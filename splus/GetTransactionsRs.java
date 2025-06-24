package com.wonder.v5.platform.data.splus;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
public class GetTransactionsRs extends BaseRs {

    @SerializedName("Data")
    private TransactionsData data;

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class TransactionsData {

        @SerializedName("transdata")
        private List<TransactionItem> transdata;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class TransactionItem {

        @SerializedName("transrecordid")
        private String transrecordid;

        @SerializedName("transcode")
        private String transcode;

        @SerializedName("transname")
        private String transname;

        @SerializedName("transamount")
        private String transamount;

        @SerializedName("transunixtime")
        private String transunixtime;

        @SerializedName("transtype")
        private String transtype;

        @SerializedName("transstatus")
        private boolean transstatus;
    }
}
