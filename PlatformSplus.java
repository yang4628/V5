package com.wonder.v5.platform.game;

import com.wonder.v5.common.enumeration.game.PlatformEnum;
import com.wonder.v5.platform.core.wallet.dto.PlatformTransactionContext;
import com.wonder.v5.platform.core.wallet.dto.PlatformTransactionResultData;
import com.wonder.v5.platform.core.wallet.entity.*;
import com.wonder.v5.platform.core.wallet.platform.AbstractMultiWalletPlatform;
import com.wonder.v5.platform.data.splus.GetBalanceRs;
import com.wonder.v5.platform.data.splus.GetTransactionsRs;
import com.wonder.v5.platform.data.splus.SplusApiUtil;
import com.wonder.v5.platform.data.splus.TransactionInRs;
import com.wonder.v5.platform.data.splus.TransactionOutRs;
import com.wonder.v5.platform.data.splus.CreateMemberRs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class PlatformSplus extends AbstractMultiWalletPlatform {

    private static final Logger log = LoggerFactory.getLogger(PlatformSplus.class);
    private static final DateTimeFormatter transactionOrderTimeForm =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    @Override
    public PlatformEnum getPlatformCode() {
        return PlatformEnum.SPLUS;
    }

    @Override
    public PlatformTransactionResultData deposit(PlatformTransactionContext context) {
        String username = context.getUsername();
        BigDecimal amount = context.getAmount();
        String payNo = context.getOrderNo();
        String transferId = "SPLUS" + LocalDateTime.now().format(transactionOrderTimeForm);

        try {
            log.info("[SPLUS][DEPOSIT] username={}, amount={}, payNo={}, transferId={}", username, amount, payNo, transferId);

            // 確認帳號是否存在（部分三方需先建立用戶）
            CreateMemberRs createRs = SplusApiUtil.createMember(username);
            if (createRs != null && !createRs.isSuccess()) {
                log.warn("[SPLUS][CREATE_MEMBER] 帳號建立失敗: {}", createRs);
                return PlatformTransactionResultData.fail("Create member failed");
            }

            TransactionInRs response = SplusApiUtil.deposit(username, amount, payNo, transferId);
            log.info("[SPLUS][DEPOSIT][RESPONSE] {}", response);
            if (response == null || !response.isSuccess()) {
                return PlatformTransactionResultData.fail("SPLUS deposit failed");
            }
            return PlatformTransactionResultData.success(new BigDecimal(response.getMoneydata().getBalance()));
        } catch (Exception e) {
            log.error("[SPLUS][DEPOSIT][ERROR] Exception occurred", e);
            return PlatformTransactionResultData.fail("SPLUS deposit exception: " + e.getMessage());
        }
    }

    @Override
    public PlatformTransactionResultData withdraw(PlatformTransactionContext context) {
        String username = context.getUsername();
        BigDecimal amount = context.getAmount();
        String payNo = context.getOrderNo();
        String transferId = "SPLUS" + LocalDateTime.now().format(transactionOrderTimeForm);

        try {
            log.info("[SPLUS][WITHDRAW] username={}, amount={}, payNo={}, transferId={}", username, amount, payNo, transferId);
            TransactionOutRs response = SplusApiUtil.withdraw(username, amount, payNo, transferId);
            log.info("[SPLUS][WITHDRAW][RESPONSE] {}", response);
            if (response == null || !response.isSuccess()) {
                return PlatformTransactionResultData.fail("SPLUS withdraw failed");
            }
            return PlatformTransactionResultData.success(new BigDecimal(response.getMoneydata().getBalance()));
        } catch (Exception e) {
            log.error("[SPLUS][WITHDRAW][ERROR] Exception occurred", e);
            return PlatformTransactionResultData.fail("SPLUS withdraw exception: " + e.getMessage());
        }
    }

    @Override
    public PlatformTransactionResultData checkTransactionStatus(PlatformTransactionContext context) {
        String payNo = context.getOrderNo();

        try {
            log.info("[SPLUS][CHECK_STATUS] payNo={}", payNo);
            GetTransactionsRs response = SplusApiUtil.getTransactionDetail(payNo);
            log.info("[SPLUS][CHECK_STATUS][RESPONSE] {}", response);

            if (response == null || !response.isSuccess() || response.getTransdata() == null || response.getTransdata().isEmpty()) {
                return PlatformTransactionResultData.fail("Check transaction failed");
            }

            var data = response.getTransdata().get(0);
            boolean success = Boolean.TRUE.equals(data.getTransstatus());
            BigDecimal amount = new BigDecimal(data.getTransamount());
            return success ? PlatformTransactionResultData.success(amount) : PlatformTransactionResultData.fail("交易失敗");
        } catch (Exception e) {
            log.error("[SPLUS][CHECK_STATUS][ERROR] Exception occurred", e);
            return PlatformTransactionResultData.fail("Check transaction exception: " + e.getMessage());
        }
    }

    @Override
    public BigDecimal getBalance(String username) {
        try {
            log.info("[SPLUS][GET_BALANCE] username={}", username);
            GetBalanceRs response = SplusApiUtil.getBalance(username);
            log.info("[SPLUS][GET_BALANCE][RESPONSE] {}", response);
            if (response == null || !response.isSuccess()) {
                return BigDecimal.ZERO;
            }
            return new BigDecimal(response.getBalance());
        } catch (Exception e) {
            log.error("[SPLUS][GET_BALANCE][ERROR] Exception occurred", e);
            return BigDecimal.ZERO;
        }
    }
} 
