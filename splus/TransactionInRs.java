package com.wonder.v5.platform.data.splus;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class TransactionInRs extends BaseRs {

    @SerializedName("Data")
    private DepositData data;

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class DepositData {

        // 存款回傳資料
        @SerializedName("moneydata")
        private MoneyData moneydata;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class MoneyData {

        // 玩家帳號
        @SerializedName("username")
        private String username;

        // 交易 ID（平台側）
        @SerializedName("txid")
        private String txid;

        // 支付方式代碼
        @SerializedName("payway")
        private String payway;

        // 貨幣代碼（如：CNY、VND）
        @SerializedName("currency")
        private String currency;

        // 存入金額
        @SerializedName("amount")
        private String amount;

        // 存款後餘額
        @SerializedName("balance")
        private String balance;

        // 外部交易單號
        @SerializedName("payno")
        private String payno;

        // 回應訊息null
        @SerializedName("trxMessage")
        private String trxMessage;

        // 存款成功與否
        @SerializedName("trxStatus")
        private boolean trxStatus;
    }
}
