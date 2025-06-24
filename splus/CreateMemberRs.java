package com.wonder.v5.platform.data.splus;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class CreateMemberRs extends BaseRs {

    @SerializedName("Data")
    private CreateMemberData data;

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class CreateMemberData {

        @SerializedName("userid")
        private String userid;

        @SerializedName("username")
        private String username;

        @SerializedName("userenable")
        private String userenable;

        @SerializedName("balance")
        private String balance;

        @SerializedName("iat")
        private long iat;
    }
}
