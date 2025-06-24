package com.wonder.v5.platform.data.splus;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class GetBalanceRs extends BaseRs {

    @SerializedName("Data")
    private BalanceData data;

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class BalanceData {

        @SerializedName("balance")
        private String balance;

        @SerializedName("currency")
        private String currency;

        @SerializedName("username")
        private String username;

        @SerializedName("playerId")
        private String playerId;

        @SerializedName("iat")
        private long iat;
    }
}
