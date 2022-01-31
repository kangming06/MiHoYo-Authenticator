package hat.auth.data

import com.google.gson.annotations.SerializedName

data class DailyNote(
    /** 树脂 **/
    @SerializedName("current_resin")
    val currentResin: Int = 0,
    @SerializedName("max_resin")
    val maxResin: Int = 0,
    @SerializedName("resin_recovery_time")
    val resinRecoveryTime: String = "",
    /** 周本 **/
    @SerializedName("resin_discount_num_limit")
    val resinDiscountNumLimit: Int = 0,
    @SerializedName("remain_resin_discount_num")
    val remainResinDiscountNum: Int = 0,
    /** 洞天宝钱 **/
    @SerializedName("current_home_coin")
    val curHomeCoin: Int = 0,
    @SerializedName("max_home_coin")
    val maxHomeCoin: Int = 0,
    @SerializedName("home_coin_recovery_time")
    val recHomeCoin: String = "",
    /** 派遣 **/
    @SerializedName("current_expedition_num")
    val currentExpeditionNum: Int = 0,
    @SerializedName("max_expedition_num")
    val maxExpeditionNum: Int = 0,
    @SerializedName("expeditions")
    val expeditions: List<Expedition> = listOf(),
    /** 每日 **/
    @SerializedName("finished_task_num")
    val finishedTaskNum: Int = 0,
    @SerializedName("total_task_num")
    val totalTaskNum: Int = 0,
    @SerializedName("is_extra_task_reward_received")
    val isExtraTaskRewardReceived: Boolean = false
) {
    data class Expedition(
        @SerializedName("avatar_side_icon")
        val avatarSideIcon: String = "",
        @SerializedName("remained_time")
        val remainedTime: String = "",
        @SerializedName("status")
        val status: String = ""
    )
}