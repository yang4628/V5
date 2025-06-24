package com.wonder.v5.platform.data.splus;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)  
public class AGLoginRs extends BaseRs {

    @SerializedName("Data")
    private AGLoginData data;

    @Data
    @EqualsAndHashCode(callSuper = false)  
    public static class AGLoginData {

        @SerializedName("accessToken")
        private String accessToken;
    }
}
