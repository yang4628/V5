package com.wonder.v5.platform.data.splus;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class BaseRs {

    @SerializedName("RespCode")
    private String respCode;

    @SerializedName("Status")
    private String status;

    @SerializedName("Action")
    private String action;

    // 回傳是否為成功狀態（Status 為 success）
    public boolean isSuccess() {
        return "success".equalsIgnoreCase(status);
    }

    // ─────────────────────────────────────
    // 錯誤碼定義（依據 SPlus 文件）
    // ─────────────────────────────────────

    public static final String SC_SUCCESS = "2000001";         // 成功，有資料
    public static final String SC_NO_CONTENT = "2040001";      // 成功，不需資料
    public static final String SC_EMPTY = "2040404";           // 成功，但無資料

    public static final String SC_AGENT_RIGHT_ERROR = "4030001"; // 代理權限驗證錯誤
    public static final String SC_PARAM_SIGN_ERROR = "4000002";  // 參數簽名驗證錯誤
    public static final String SC_AGENT_IDENTITY_ERROR = "4030003"; // 代理憑別驗證錯誤
    public static final String SC_PARAM_INVALID = "4000001";    // 參數錯誤
    public static final String SC_PARAM_MISSING = "4040001";    // 找不到資料
    public static final String SC_DUPLICATE_USER = "4040099";   // 會員資料重複
    public static final String SC_NO_AGENT_DATA = "4040121";    // 無代理資料
    public static final String SC_NO_RELATED_DATA = "4040004";  // 無相關資料
    public static final String SC_INSUFFICIENT_FUNDS = "4030104"; // 金額不足
    public static final String SC_OVER_LIMIT = "4030105";       // 存款超過限制
    public static final String SC_WITHDRAW_TOO_LOW = "4030106"; // 提款餘額不足
    public static final String SC_DUPLICATE_LOGIN = "4030102";  // 重複登入
    public static final String SC_SYSTEM_ERROR = "5000001";     // 系統錯誤
    public static final String SC_MAINTENANCE = "5000002";      // 系統維護中

    // ─────────────────────────────────────
    // 錯誤碼判斷函式（常用）
    // ─────────────────────────────────────

    public boolean isNoContent() {
        return SC_NO_CONTENT.equals(respCode);
    }

    public boolean isEmptyData() {
        return SC_EMPTY.equals(respCode);
    }

    public boolean isUserDuplicate() {
        return SC_DUPLICATE_USER.equals(respCode);
    }

    public boolean isInsufficientFunds() {
        return SC_INSUFFICIENT_FUNDS.equals(respCode);
    }

    public boolean isWithdrawAmountTooLow() {
        return SC_WITHDRAW_TOO_LOW.equals(respCode);
    }

    public boolean isOverLimit() {
        return SC_OVER_LIMIT.equals(respCode);
    }

    public boolean isDuplicateLogin() {
        return SC_DUPLICATE_LOGIN.equals(respCode);
    }

    public boolean isInMaintenance() {
        return SC_MAINTENANCE.equals(respCode);
    }

    public boolean isParamError() {
        return SC_PARAM_INVALID.equals(respCode) || SC_PARAM_SIGN_ERROR.equals(respCode);
    }

    public boolean isAgentAuthError() {
        return SC_AGENT_RIGHT_ERROR.equals(respCode) || SC_AGENT_IDENTITY_ERROR.equals(respCode);
    }

    public boolean isNoAgentData() {
        return SC_NO_AGENT_DATA.equals(respCode);
    }

    public boolean isNoRelatedData() {
        return SC_NO_RELATED_DATA.equals(respCode);
    }
}
