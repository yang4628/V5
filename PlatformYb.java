package com.wonder.v5.platform.game;

import static com.wonder.lib.util.ConstUtils.GSON;
import static com.wonder.v5.common.constant.RedisSubscribeInfo.REDIS_ACTION_GAME_DATA_UPDATE;
import static com.wonder.v5.common.constant.RedisSubscribeInfo.REDIS_TOPIC_GAME_INFO;
import static com.wonder.v5.platform.enumeration.PlatformResponseEnum.INTERNAL_SERVER_ERROR;
import static com.wonder.v5.platform.enumeration.PlatformResponseEnum.NETWORK_ERROR;
import static com.wonder.v5.platform.enumeration.PlatformResponseEnum.PLATFORM_ERROR;
import static com.wonder.v5.platform.enumeration.PlatformResponseEnum.SUCCESS;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import javax.annotation.PostConstruct;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Base64;
import org.redisson.api.listener.MessageListener;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import com.wonder.lib.data.ResponseData;
import com.wonder.lib.util.RestUtils;
import com.wonder.lib.util.RestUtils.HttpRequestData;
import com.wonder.v5.common.data.RedisMsgData;
import com.wonder.v5.common.enumeration.game.PlatformEnum;
import com.wonder.v5.common.persistence.model.common.SiteModel;
import com.wonder.v5.common.persistence.model.member.UserModel;
import com.wonder.v5.platform.data.ApiResponseData;
import com.wonder.v5.platform.data.ExtraData;
import com.wonder.v5.platform.data.PlatformCommonApiSetting;
import com.wonder.v5.platform.data.PlatformMultiWalletTransactionStatusData;
import com.wonder.v5.platform.data.PlatformTransactionResultData;
import com.wonder.v5.platform.data.yb.CheckTransferRq;
import com.wonder.v5.platform.data.yb.CheckTransferRs;
import com.wonder.v5.platform.data.yb.CreateUserRq;
import com.wonder.v5.platform.data.yb.CreateUserRs;
import com.wonder.v5.platform.data.yb.GetAcctInfoRq;
import com.wonder.v5.platform.data.yb.GetAcctInfoRs;
import com.wonder.v5.platform.data.yb.LaunchRq;
import com.wonder.v5.platform.data.yb.LaunchRs;
import com.wonder.v5.platform.data.yb.TransferRq;
import com.wonder.v5.platform.data.yb.TransferRs;
import com.wonder.v5.platform.enumeration.PlatformGameDisplayModeEnum;
import com.wonder.v5.platform.enumeration.PlatformMultiWalletTransactionStatusEnum;
import com.wonder.v5.platform.persistence.model.PlatformApiSettingModel;
import com.wonder.v5.platform.persistence.model.PlatformMultiWalletTransactionLogModel;
import com.wonder.v5.platform.utils.PlatformUtils;
import lombok.Data;
import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class PlatformYb extends AbstractPlatform {

  private static final DateTimeFormatter transactionOrderTimeForm =
      DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

  private Map<String, PlatformApiSettingModel> apiSettingMap;

  private PlatformCommonApiSetting commonApiSetting;

  protected PlatformYb() {
    super(PlatformEnum.PLATFORM_YB);
  }

  @PostConstruct
  public void init() {

    initInfo();

    mRedisSubscribeService.subscribe(REDIS_TOPIC_GAME_INFO, RedisMsgData.class,
        new PlatformRedisMsgListener());
  }

  public void initInfo() {

    var model = mPlatformMainInfoRepo.getPlatformMainInfo(platformEnum.getId());

    commonApiSetting = GSON.fromJson(model.getCommonApiSetting(), PlatformCommonApiSetting.class);

    List<PlatformApiSettingModel> settingList =
        mPlatformApiSettingRepo.getApiSetting(this.platformEnum.getId());
    apiSettingMap = PlatformUtils.generateApiSettingMapV2(settingList);
  }

  @Override
  public ResponseData launchGame(String gameCode, int kind, String gameLang, String currency,
      UserModel user, SiteModel site, Locale locale, PlatformGameDisplayModeEnum displayType,
      ExtraData extraData) {

    try {

      String settingKey = PlatformUtils.getInfoKey(mSystemEnvironment, currency, site.getId());
      String platformAccount = getPlatformAccount(user, currency, site);

      PlatformApiSettingModel apiSetting = apiSettingMap.get(settingKey);
      YbAgentConfig apiConfig = new YbAgentConfig(apiSetting);

      String convertLang = commonApiSetting.getLangMap().getOrDefault(gameLang.toLowerCase(), "en");

      String apiUrl = apiSetting.getApiUrl();

      LaunchRq request = new LaunchRq();

      request.setUid(platformAccount.toLowerCase());
      request.setGType(gameCode);
      request.setMType(gameCode);
      request.setLang(convertLang);

      ApiResponseData apiResponse = doPostApi(apiUrl, apiConfig, request);

      if (apiResponse.getStatus() == NETWORK_ERROR
          || apiResponse.getStatus() == INTERNAL_SERVER_ERROR) {
        return ResponseData.error(locale, NETWORK_ERROR.getCode(), NETWORK_ERROR.getMsg());
      }

      LaunchRs response = GSON.fromJson(apiResponse.getResponse().getBody(), LaunchRs.class);

      if (response.isRsOK()) {
        if (response.getPath() != null) {

          return ResponseData.of(response.getPath());
        } else {
          log.error("[launchGame] no enable launch domain, code: {}, msg: {} ", apiUrl,
              response.getCode(), response.getMessage());
          return ResponseData.error(locale, PLATFORM_ERROR.getCode(), PLATFORM_ERROR.getMsg());
        }

      } else {
        log.error("[launchGame] Failed, url:{}, message:{}", apiUrl, response.getMessage());
        return ResponseData.error(locale, PLATFORM_ERROR.getCode(), PLATFORM_ERROR.getMsg());
      }

    } catch (Exception e) {
      log.error("[launchGame]", e);
      return ResponseData.error(locale, INTERNAL_SERVER_ERROR.getCode(),
          INTERNAL_SERVER_ERROR.getMsg());
    }
  }

  public PlatformTransactionResultData deposit(UserModel user, String currency, BigDecimal amount,
      SiteModel site, String transactionOrderNo) {

    String platformAccount = getPlatformAccount(user, currency, site);
    PlatformTransactionResultData result = new PlatformTransactionResultData();
    result.setPlatformAccount(platformAccount);
    result.setPlatformBalance(BigDecimal.ZERO);
    result.setTransactionOrderNo(transactionOrderNo);

    try {

      String settingKey = PlatformUtils.getInfoKey(mSystemEnvironment, currency, site.getId());
      PlatformApiSettingModel apiSetting = apiSettingMap.get(settingKey);
      YbAgentConfig apiConfig = new YbAgentConfig(apiSetting);
      String apiUrl = apiSetting.getApiUrl();

      BigDecimal exchangeRate =
          commonApiSetting.getExchangeRateMap().getOrDefault(currency, BigDecimal.ONE);
      BigDecimal convertedAmount = amount.multiply(exchangeRate).setScale(2, RoundingMode.DOWN);

      String convertedCurrency = commonApiSetting.getCurrencyMap().get(currency);

      // 組裝請求

      TransferRq request = new TransferRq();

      if (platformAccount == null || convertedCurrency == null || convertedAmount == null
          || transactionOrderNo == null) {

        log.error(
            "[deposit] platformAccount: {}, convertedCurrency: {}, convertedAmount: {}, transactionOrderNo: {}",
            platformAccount, convertedCurrency, convertedAmount, transactionOrderNo);

        result.setResponseEnum(INTERNAL_SERVER_ERROR);
        return result;
      }

      request.setUid(platformAccount);
      request.setAmount(convertedAmount);
      request.setSerialNo(transactionOrderNo);
      request.setParent(apiConfig.getAgentName()); // 這家Agent本身帶有貨幣資訊,不用輸入幣別 (1 agent to 1 currency
                                                   // )

      // 呼叫 API
      ApiResponseData apiResponse = doPostApi(apiUrl, apiConfig, request);
      result.setResponseEnum(apiResponse.getStatus());

      if (apiResponse.getStatus() == NETWORK_ERROR
          || apiResponse.getStatus() == INTERNAL_SERVER_ERROR) {
        log.error("[deposit] Response: {}, status: {}, apiUrl: {}, rq: {}",
            apiResponse.getResponse(), apiResponse.getStatus(), apiUrl, request);
        return result;
      }

      TransferRs response = GSON.fromJson(apiResponse.getResponse().getBody(), TransferRs.class);

      if (response.isRsOK()) {
        result.setResponseEnum(SUCCESS);
        BigDecimal platformAmount =
            response.getUserBalance().divide(exchangeRate, 2, RoundingMode.DOWN);
        result.setPlatformBalance(platformAmount);
        result.setTransactionAmount(amount);

      } else if (response.isUserNotExist() && createUser(currency, user, site)) {
        return deposit(user, currency, amount, site, transactionOrderNo);
      }

      else {

        log.error("[deposit] code:{},  msg: {}, apiUrl: {}, rq: {}", response.getCode(),
            response.getMessage(), apiUrl, request);
        result.setNeedRollback(true);
        result.setResponseEnum(PLATFORM_ERROR);
      }

    } catch (Exception e) {
      log.error("[deposit]", e);
      result.setResponseEnum(INTERNAL_SERVER_ERROR);
    }

    return result;
  }

  public PlatformTransactionResultData withdraw(UserModel user, String currency, BigDecimal amount,
      SiteModel site, String transactionOrderNo) {

    String platformAccount = getPlatformAccount(user, currency, site);

    PlatformTransactionResultData result = new PlatformTransactionResultData();
    result.setPlatformAccount(platformAccount);
    result.setPlatformBalance(BigDecimal.ZERO);
    result.setTransactionOrderNo(transactionOrderNo);

    try {
      String settingKey = PlatformUtils.getInfoKey(mSystemEnvironment, currency, site.getId());
      PlatformApiSettingModel apiSetting = apiSettingMap.get(settingKey);
      YbAgentConfig apiConfig = new YbAgentConfig(apiSetting);
      String apiUrl = apiSetting.getApiUrl();

      BigDecimal exchangeRate =
          commonApiSetting.getExchangeRateMap().getOrDefault(currency, BigDecimal.ONE);
      BigDecimal convertedAmount = amount.multiply(exchangeRate).setScale(2, RoundingMode.DOWN);

      String convertedCurrency = commonApiSetting.getCurrencyMap().get(currency);

      // 組裝請求

      TransferRq request = new TransferRq();

      if (platformAccount == null || convertedCurrency == null || convertedAmount == null
          || transactionOrderNo == null) {

        log.error(
            "[withdraw] platformAccount: {}, convertedCurrency: {}, convertedAmount: {}, transactionOrderNo: {}",
            platformAccount, convertedCurrency, convertedAmount, transactionOrderNo);

        result.setResponseEnum(INTERNAL_SERVER_ERROR);
        return result;
      }

      request.setUid(platformAccount);
      request.setAmount(convertedAmount.negate()); // 存取共用一支 取時為負值
      request.setSerialNo(transactionOrderNo);
      request.setParent(apiConfig.getAgentName()); // 這家Agent本身帶有貨幣資訊,不用輸入幣別 (1 agent to 1 currency
                                                   // )

      // 呼叫 API
      ApiResponseData apiResponse = doPostApi(apiUrl, apiConfig, request);
      result.setResponseEnum(apiResponse.getStatus());

      if (apiResponse.getStatus() == NETWORK_ERROR
          || apiResponse.getStatus() == INTERNAL_SERVER_ERROR) {
        log.error("[withdraw] Response: {}, status: {}, apiUrl: {}, rq: {}",
            apiResponse.getResponse(), apiResponse.getStatus(), apiUrl, request);
        return result;
      }

      TransferRs response = GSON.fromJson(apiResponse.getResponse().getBody(), TransferRs.class);

      if (response.isRsOK()) {
        result.setResponseEnum(SUCCESS);
        BigDecimal platformAmount =
            response.getUserBalance().divide(exchangeRate, 2, RoundingMode.DOWN);
        result.setPlatformBalance(platformAmount);
        result.setTransactionAmount(amount);
      } else if (response.isUserNotExist()) {
        result.setResponseEnum(SUCCESS);
      } else {
        log.error("[withdraw] code:{} , msg: {}, apiUrl: {}, rq: {}", response.getCode(),
            response.getMessage(), apiUrl, request);

        result.setResponseEnum(PLATFORM_ERROR);
      }

    } catch (Exception e) {
      log.error("[withdraw]", e);
      result.setResponseEnum(INTERNAL_SERVER_ERROR);
    }

    return result;
  }

  public PlatformTransactionResultData getBalance(UserModel user, String currency, SiteModel site) {
    String platformAccount = getPlatformAccount(user, currency, site);
    PlatformTransactionResultData result = new PlatformTransactionResultData();

    result.setPlatformAccount(platformAccount);
    result.setPlatformBalance(BigDecimal.ZERO);

    try {
      String settingKey = PlatformUtils.getInfoKey(mSystemEnvironment, currency, site.getId());
      PlatformApiSettingModel apiSetting = apiSettingMap.get(settingKey);

      BigDecimal exchangeRate =
          commonApiSetting.getExchangeRateMap().getOrDefault(currency, BigDecimal.ONE);

      String convertedCurrency = commonApiSetting.getCurrencyMap().get(currency);

      YbAgentConfig apiConfig = new YbAgentConfig(apiSetting);

      // 組裝請求

      String apiUrl = apiSetting.getApiUrl();

      if (platformAccount == null || convertedCurrency == null) {
        result.setResponseEnum(INTERNAL_SERVER_ERROR);
        return result;
      }

      GetAcctInfoRq request = new GetAcctInfoRq();
      request.setParent(apiConfig.getAgentName());
      request.setUid(platformAccount);

      // 呼叫 API
      ApiResponseData apiResponse = doPostApi(apiUrl, apiConfig, request);
      result.setResponseEnum(apiResponse.getStatus());

      if (apiResponse.getStatus() == NETWORK_ERROR
          || apiResponse.getStatus() == INTERNAL_SERVER_ERROR) {
        return result;
      }

      GetAcctInfoRs response =
          GSON.fromJson(apiResponse.getResponse().getBody(), GetAcctInfoRs.class);

      if (response.isRsOK()) {
        List<GetAcctInfoRs.AcctInfo> acctInfoList = response.getAcctInfoList();
        if (acctInfoList != null && !acctInfoList.isEmpty()) {
          result.setResponseEnum(SUCCESS);

          BigDecimal rawBalance = response.getAcctInfoList().get(0).getBalance();
          BigDecimal converted = rawBalance.divide(exchangeRate, 2, RoundingMode.DOWN);

          result.setPlatformBalance(converted);
        }

        else {
          result.setResponseEnum(SUCCESS);
        }
      } else {
        if (response.isUserNotExist()) {
          result.setResponseEnum(SUCCESS);

        }

        else {
          log.error("[getBalance]  code: {} , msg: {}, apiUrl: {} , rs:{} ", response.getCode(),
              response.getMessage(), apiUrl, response);
          result.setResponseEnum(PLATFORM_ERROR);
        }
      }

    } catch (Exception e) {
      log.error("[getBalance]", e);
      result.setResponseEnum(INTERNAL_SERVER_ERROR);
    }
    return result;
  }

  public PlatformMultiWalletTransactionStatusData checkTransactionStatus(
      PlatformMultiWalletTransactionLogModel logData, long siteId, Locale locale) {

    PlatformMultiWalletTransactionStatusData result =
        new PlatformMultiWalletTransactionStatusData();
    result.setTransactionStatus(PlatformMultiWalletTransactionStatusEnum.UNCONFIRMED, locale);

    try {
      String settingKey =
          PlatformUtils.getInfoKey(mSystemEnvironment, logData.getCurrency(), siteId);
      PlatformApiSettingModel apiSetting = apiSettingMap.get(settingKey);

      // 組裝請求
      YbAgentConfig apiConfig = new YbAgentConfig(apiSetting);

      String apiUrl = apiSetting.getApiUrl();

      CheckTransferRq request = new CheckTransferRq();

      request.setParent(apiConfig.getAgentName());
      request.setSerialNo(logData.getPlatformOrderNo());

      // 呼叫 API
      ApiResponseData apiResponse = doPostApi(apiUrl, apiConfig, request);
      result.setResponseEnum(apiResponse.getStatus());

      if (apiResponse.getStatus() == NETWORK_ERROR
          || apiResponse.getStatus() == INTERNAL_SERVER_ERROR) {
        result.setResponseEnum(INTERNAL_SERVER_ERROR);
        return result;
      }

      // 解析回應
      CheckTransferRs response =
          GSON.fromJson(apiResponse.getResponse().getBody(), CheckTransferRs.class);

      if (response.isRsOK()) {
        result.setResponseEnum(SUCCESS);
        result.setTransactionStatus(PlatformMultiWalletTransactionStatusEnum.SUCCESS, locale);
      } else {
        switch (response.getCode()) {
          case CheckTransferRs.SC_DATA_NOT_EXISTED: {
            result.setTransactionStatus(PlatformMultiWalletTransactionStatusEnum.ORDER_NOT_EXIST,
                locale);
            result.setResponseEnum(SUCCESS);
            break;
          }
          default: {
            result.setResponseEnum(PLATFORM_ERROR);
            log.error(
                "[checkTransactionStatus] code: {} , msg: {} , serialNo: {}, apiUrl: {} , PlatformOrderNo: {}",
                response.getCode(), response.getMessage(), request.getSerialNo(), apiUrl,
                logData.getPlatformOrderNo());
            break;
          }
        }

      }
      return result;

    } catch (Exception e) {
      log.error("[checkTransactionStatus]", e);
      result.setResponseEnum(INTERNAL_SERVER_ERROR);
      return result;
    }
  }

  @Override
  public boolean checkPlatformSupportCurrency(long siteId, String currency) {
    String settingKey = PlatformUtils.getInfoKey(mSystemEnvironment, currency, siteId);
    PlatformApiSettingModel apiSetting = apiSettingMap.get(settingKey);

    return apiSetting != null;
  }

  public String getPlatformAccount(UserModel user, String currency, SiteModel site) {
    return PlatformUtils.getPlatformAccount(site.getPlatformPrefix(), currency, user.getId());
  }

  private ApiResponseData doPostApi(String url, YbAgentConfig ApiConfig, Object rq) {

    String rawJsonString = GSON.toJson(rq);

    ApiResponseData result = new ApiResponseData();

    String encryptedData = null;
    try {
      encryptedData = encrypt(rawJsonString, ApiConfig.getSecretKey(), ApiConfig.getAesIv());

    } catch (Exception e) {
      log.error("[doPostApi] encrypt Error!! ApiConfig:{},  rawbody:{} ", ApiConfig, rawJsonString);
      result.setStatus(INTERNAL_SERVER_ERROR);
      return result;
    }

    Map<String, String> bodyMap = new HashMap<>();
    bodyMap.put("dc", ApiConfig.getDc());
    bodyMap.put("x", encryptedData);

    HttpRequestData req = new HttpRequestData().setMediaType(MediaType.APPLICATION_FORM_URLENCODED)
        .setTimeout(3000).setUrl(url).setHttpMethod(HttpMethod.POST).setBypassResponseError(true)
        .setLogError(false).setBypassException(false).setBody(bodyMap).setDebug(false);

    try {
      ResponseEntity<String> response = new RestUtils<>(String.class, req).exchange();

      if (HttpStatus.OK.value() == response.getStatusCodeValue()
          || HttpStatus.CREATED.value() == response.getStatusCodeValue()) {

        result.setStatus(SUCCESS);
        result.setResponse(response);

      } else if (HttpStatus.BAD_REQUEST.value() == response.getStatusCodeValue()
          || HttpStatus.FORBIDDEN.value() == response.getStatusCodeValue()
          || HttpStatus.INTERNAL_SERVER_ERROR.value() == response.getStatusCodeValue()) {
        result.setStatus(PLATFORM_ERROR);
        result.setResponse(response);

      } else {
        log.error("[doPostApi] HttpError, status:{}, req:{} , rawbody:{} ", response,
            GSON.toJson(req), rawJsonString);
        result.setStatus(NETWORK_ERROR);
      }
      return result;

    } catch (Exception e) {
      log.error("[doPostFormJsonApi] call api failed, req:{}, rawbody:{}, exception:{}",
          GSON.toJson(req), rawJsonString, e.getMessage());
      result.setStatus(INTERNAL_SERVER_ERROR);
      return result;
    }
  }

  public BigDecimal scaleTransactionAmount(BigDecimal amount, String currency) {

    BigDecimal resultAmt = amount.setScale(2, RoundingMode.DOWN);
    BigDecimal exchangeRate = commonApiSetting.getExchangeRateMap().get(currency.toLowerCase());

    if (exchangeRate == null) {
      return resultAmt;
    }

    BigDecimal reverseExchangeRate = BigDecimal.ONE.divide(exchangeRate, 2, RoundingMode.HALF_UP);
    BigDecimal transferAmt = amount.multiply(exchangeRate).setScale(2, RoundingMode.UP);
    BigDecimal testAmt =
        transferAmt.multiply(reverseExchangeRate).setScale(2, RoundingMode.HALF_UP);

    if (testAmt.compareTo(amount) > 0) {
      resultAmt = transferAmt.subtract(amountScaleAccuracy).multiply(reverseExchangeRate)
          .setScale(0, RoundingMode.DOWN);
    }

    return resultAmt;
  }

  public String generateUUID() {

    return UUID.randomUUID().toString();
  }

  public String getPlatformTransactionOrderNo(UserModel user, String currency, SiteModel site) {

    String platformAccount = getPlatformAccount(user, currency, site);

    String settingKey = PlatformUtils.getInfoKey(mSystemEnvironment, currency, site.getId());
    PlatformApiSettingModel apiSetting = apiSettingMap.get(settingKey);

    platformAccount = platformAccount.replaceAll("_", "");

    String orderNo = LocalDateTime.now().format(transactionOrderTimeForm) + platformAccount;

    if (!orderNo.matches("^[A-Za-z0-9]{1,50}$")) {
      log.error(
          "[getPlatformTransactionOrderNo] OrderNo not valid: regex:(^[A-Za-z0-9]{1,50}$) OrderNo:{}",
          orderNo);
    }

    return orderNo;
  }

  public static String encrypt(String data, String key, String iv) throws Exception {
    Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
    int blockSize = cipher.getBlockSize();
    byte[] dataBytes = data.getBytes();
    int plainTextLength = dataBytes.length;

    if (plainTextLength % blockSize != 0) {
      plainTextLength = plainTextLength + (blockSize - plainTextLength % blockSize);
    }

    byte[] plaintext = new byte[plainTextLength];
    System.arraycopy(dataBytes, 0, plaintext, 0, dataBytes.length);

    SecretKeySpec keyspec = new SecretKeySpec(key.getBytes(), "AES");
    IvParameterSpec ivspec = new IvParameterSpec(iv.getBytes());

    cipher.init(Cipher.ENCRYPT_MODE, keyspec, ivspec);
    byte[] encrypted = cipher.doFinal(plaintext);
    return Base64.encodeBase64URLSafeString(encrypted);
  }

  private boolean createUser(String currency, UserModel user, SiteModel site) {

    try {
      String settingKey = PlatformUtils.getInfoKey(mSystemEnvironment, currency, site.getId());
      PlatformApiSettingModel apiSetting = apiSettingMap.get(settingKey);
      String platformAccount = getPlatformAccount(user, currency, site);

      YbAgentConfig apiConfig = new YbAgentConfig(apiSetting);
      String apiUrl = apiSetting.getApiUrl();

      // 構建請求物件
      CreateUserRq request = new CreateUserRq();
      request.setUid(platformAccount);
      request.setParent(apiConfig.getAgentName());

      // 呼叫 API
      ApiResponseData apiResponse = doPostApi(apiUrl, apiConfig, request);

      if (apiResponse.getStatus() == NETWORK_ERROR
          || apiResponse.getStatus() == INTERNAL_SERVER_ERROR) {
        return false;
      }

      CreateUserRs response =
          GSON.fromJson(apiResponse.getResponse().getBody(), CreateUserRs.class);

      if (response.isRsOK()) {

        return true;

      } else {
        log.error("[createUser] Failed, url:{}, rq: {}, msg:{}", apiUrl, request,
            response.getMessage());
        return false;
      }

    } catch (Exception e) {
      log.error("[createUser]", e);
      return false;
    }
  }

  private class PlatformRedisMsgListener implements MessageListener<RedisMsgData> {

    @Override
    public void onMessage(CharSequence channel, RedisMsgData message) {
      if (REDIS_TOPIC_GAME_INFO.equals(channel.toString())) {
        String action = message.getAction();

        if (REDIS_ACTION_GAME_DATA_UPDATE.equals(action)) {
          initInfo();
        }
      }
    }
  }

  static @Data private class YbAgentConfig {

    String apiUrl;

    String agentName;

    String secretKey;

    String aesIv;

    String dc;

    private YbAgentConfig(PlatformApiSettingModel apiSetting) {

      this.apiUrl = apiSetting.getApiUrl();
      this.agentName = apiSetting.getApiAccount();
      this.secretKey = apiSetting.getSecretKey();
      this.aesIv = apiSetting.getAesKey();
      this.dc = apiSetting.getExtraParam1();

    }
  }

}
