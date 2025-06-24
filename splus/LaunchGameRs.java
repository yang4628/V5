package com.wonder.v5.platform.data.splus;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class LaunchGameRs extends BaseRs {

    @SerializedName("Data")
    private LaunchGameData data;

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class LaunchGameData {

        @SerializedName("launchgameurl")
        private String launchgameurl;

        @SerializedName("username")
        private String username;

        @SerializedName("userid")
        private String userid;

        @SerializedName("userorder")
        private String userorder;
    }
}
