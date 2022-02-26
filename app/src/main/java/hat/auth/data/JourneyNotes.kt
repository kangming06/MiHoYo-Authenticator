package hat.auth.data

import com.google.gson.annotations.SerializedName

data class JourneyNotes(
    @SerializedName("data_last_month")
    val dataLastMonth: Int = 0,
    @SerializedName("data_month")
    val dataMonth: Int = 0,
    @SerializedName("date")
    val date: String = "null",
    @SerializedName("day_data")
    val dayData: DayData = DayData(),
    @SerializedName("lantern")
    val lantern: Boolean = false,
    @SerializedName("month")
    val month: Int = 0,
    @SerializedName("month_data")
    val monthData: MonthData = MonthData(),
    @SerializedName("optional_month")
    val optionalMonth: List<Int> = emptyList(),
) {
    data class DayData(
        @SerializedName("current_mora")
        val currentMora: Int = 0,
        @SerializedName("current_primogems")
        val currentPrimogems: Int = 0,
        @SerializedName("last_mora")
        val lastMora: Int = 0,
        @SerializedName("last_primogems")
        val lastPrimogems: Int = 0
    )

    data class MonthData(
        @SerializedName("current_mora")
        val currentMora: Int = 0,
        @SerializedName("current_primogems")
        val currentPrimogems: Int = 0,
        @SerializedName("current_primogems_level")
        val currentPrimogemsLevel: Int = 0,
        @SerializedName("group_by")
        val groupBy: List<GroupBy> = listOf(),
        @SerializedName("last_mora")
        val lastMora: Int = 0,
        @SerializedName("last_primogems")
        val lastPrimogems: Int = 0,
        @SerializedName("mora_rate")
        val moraRate: Int = 0,
        @SerializedName("primogems_rate")
        val primogemsRate: Int = 0
    ) {
        data class GroupBy(
            @SerializedName("action")
            val action: String = "",
            @SerializedName("action_id")
            val actionId: Int = 0,
            @SerializedName("num")
            val num: Int = 0,
            @SerializedName("percent")
            val percent: Int = 0
        )
    }
}