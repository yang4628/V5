package com.wonder.v5.platform.data.splus;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class TransactionOutRs extends BaseRs {

    @SerializedName("Message")
    private String message;  

    @SerializedName("Data")
    private WithdrawData data;

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class WithdrawData {

        @SerializedName("moneydata")
        private MoneyData moneydata;  // 出款回傳內容
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class MoneyData {

        // 玩家帳號
        @SerializedName("username")
        private String username;

        // 交易流水號（平台內部）
        @SerializedName("txid")
        private String txid;

        // 支付方式（-1 代表提款）
        @SerializedName("payway")
        private String payway;

        // 貨幣（如 CNY）
        @SerializedName("currency")
        private String currency;

        // 提領金額
        @SerializedName("reqamount")
        private String reqamount;

        // 提領後餘額
        @SerializedName("balance")
        private String balance;

        // 外部交易編號
        @SerializedName("payno")
        private String payno;

        // 回應訊息null
        @SerializedName("trxMessage")
        private String trxMessage;

        // 是否成功
        @SerializedName("trxStatus")
        private boolean trxStatus;
    }
}
