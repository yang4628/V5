package com.wonder.v5.platform.data.splus;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
public class CheckWagerRecordsRs extends BaseRs {

    @SerializedName("Data")
    private WagerData data;

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class WagerData {

        // 總頁數
        @SerializedName("totalpage")
        private String totalpage;

        // 當前頁碼
        @SerializedName("page")
        private String page;

        // 下注紀錄列表
        @SerializedName("wagerdata")
        private List<WagerRecord> wagerdata;
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static class WagerRecord {

        // 注單編號
        @SerializedName("wagerid")
        private String wagerid;

        // 遊戲紀錄編號
        @SerializedName("gamerecordid")
        private String gamerecordid;

        // 玩家帳號
        @SerializedName("username")
        private String username;

        // 玩家 ID
        @SerializedName("userid")
        private String userid;

        // 平台網站代碼
        @SerializedName("ecsiteid")
        private String ecsiteid;

        // 遊戲 ID
        @SerializedName("gameid")
        private String gameid;

        // 下注前餘額
        @SerializedName("beforebetbalance")
        private String beforebetbalance;

        // 下注金額
        @SerializedName("betamount")
        private String betamount;

        // 有效投注金額
        @SerializedName("validamount")
        private String validamount;

        // 派彩金額
        @SerializedName("winamount")
        private String winamount;

        // 結果金額（派彩 - 下注）
        @SerializedName("resultamount")
        private String resultamount;

        // 是否結算：1=已結算
        @SerializedName("settle")
        private String settle;

        // 結果：W=贏、L=輸
        @SerializedName("result")
        private String result;

        // 下注後餘額
        @SerializedName("afterbetbalance")
        private String afterbetbalance;

        // 注單時間（Unix 毫秒）
        @SerializedName("wagerdate")
        private String wagerdate;

        // 裝置類型：S
        @SerializedName("device")
        private String device;

        // 遊戲類型代碼
        @SerializedName("gametype")
        private String gametype;

        // KK 模式下注前餘額（可能為複製欄位）
        @SerializedName("kkBeforBalance")
        private String kkBeforBalance;

        // KK 模式下注後餘額
        @SerializedName("kkAfterBalance")
        private String kkAfterBalance;

        // 紀錄類型，例如 Game
        @SerializedName("recordtype")
        private String recordtype;

        // 是否購買免費遊戲（buy free spin）
        @SerializedName("buyFreeGame")
        private boolean buyFreeGame;

        // 贏分比
        @SerializedName("winRatio")
        private double winRatio;

        // 回合 ID
        @SerializedName("RoundId")
        private String roundId;

        // 投注內容（可能為 JSON 或文字描述）
        @SerializedName("BetInfo")
        private String betInfo;

        // 投注賠率
        @SerializedName("BetOdds")
        private double betOdds;

        // 預期贏分
        @SerializedName("ExpectedWinAmount")
        private double expectedWinAmount;

        // 贏分結果
        @SerializedName("WinResult")
        private int winResult;

        // 遊戲結果文字
        @SerializedName("GameResult")
        private String gameResult;

        // 道具類型（如果有用道具）
        @SerializedName("propsType")
        private int propsType;

        // 名義投注（如有免費遊戲）
        @SerializedName("nominalBet")
        private int nominalBet;

        // 是否使用道具
        @SerializedName("useProps")
        private boolean useProps;

        // JP（彩金）編號
        @SerializedName("jpxid")
        private String jpxid;

        // JP 發生時間（Unix）
        @SerializedName("jptimestamp")
        private long jptimestamp;

        // JP 等級（例如 Grand, Major）
        @SerializedName("jPLevel")
        private int jPLevel;

        // JP 類型
        @SerializedName("jptype")
        private int jptype;

        // 是否中彩金
        @SerializedName("jpisHit")
        private boolean jpisHit;

        // 中彩金金額
        @SerializedName("jPHitValue")
        private int jPHitValue;
    }
}
