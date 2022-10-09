package hat.auth.data

import com.google.gson.annotations.SerializedName

data class GameRecord(
    /*@SerializedName("homes")
    val homes: List<Home> = emptyList(),*/
    @SerializedName("stats")
    val stats: Stats = Stats(),
    /*@SerializedName("world_explorations")
    val worldExplorations: List<WorldExploration> = emptyList()*/
) {

    data class Stats(
        /*@SerializedName("achievement_number")
        val achievementNumber: Int = -1,
        @SerializedName("active_day_number")
        val activeDayNumber: Int = -1,
        @SerializedName("avatar_number")
        val avatarNumber: Int = -1,
        @SerializedName("spiral_abyss")
        val spiralAbyss: String = "null",
        @SerializedName("way_point_number")
        val wayPointNumber: Int = -1,
        @SerializedName("win_rate")
        val winRate: Int = -1,
        @SerializedName("domain_number")
        val domainNumber: Int = -1,*/
        @SerializedName("anemoculus_number")
        val anemoculusNumber: Int = -1,
        @SerializedName("geoculus_number")
        val geoculusNumber: Int = -1,
        @SerializedName("electroculus_number")
        val electroculusNumber: Int = -1,
        @SerializedName("dendroculus_number")
        val dendroculusNumber: Int = -1,
        @SerializedName("common_chest_number")
        val commonChestNumber: Int = -1,
        @SerializedName("exquisite_chest_number")
        val exquisiteChestNumber: Int = -1,
        @SerializedName("precious_chest_number")
        val preciousChestNumber: Int = -1,
        @SerializedName("luxurious_chest_number")
        val luxuriousChestNumber: Int = -1,
        @SerializedName("magic_chest_number")
        val magicChestNumber: Int = -1
    )

    /*@Keep
    data class WorldExploration(
        @SerializedName("exploration_percentage")
        val explorationPercentage: Int,
        @SerializedName("icon")
        val icon: String,
        @SerializedName("id")
        val id: Int,
        @SerializedName("level")
        val level: Int,
        @SerializedName("name")
        val name: String,
        @SerializedName("offerings")
        val offerings: List<Offering>,
        @SerializedName("type")
        val type: String
    ) {
        @Keep
        data class Offering(
            @SerializedName("level")
            val level: Int,
            @SerializedName("name")
            val name: String
        )
    }*/

    /*@Keep
    data class Home(
        @SerializedName("comfort_level_icon")
        val comfortLevelIcon: String,
        @SerializedName("comfort_level_name")
        val comfortLevelName: String,
        @SerializedName("comfort_num")
        val comfortNum: Int,
        @SerializedName("icon")
        val icon: String,
        @SerializedName("item_num")
        val itemNum: Int,
        @SerializedName("level")
        val level: Int,
        @SerializedName("name")
        val name: String,
        @SerializedName("visit_num")
        val visitNum: Int
    )*/
}