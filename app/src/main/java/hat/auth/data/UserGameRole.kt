package hat.auth.data

import com.google.gson.annotations.SerializedName

data class UserGameRole(
    @SerializedName("game_biz")
    val biz: String,
    @SerializedName("region")
    val region: String,
    @SerializedName("game_uid")
    val uid: String,
    @SerializedName("nickname")
    val name: String,
    @SerializedName("level")
    val level: Int,
    @SerializedName("is_chosen")
    val isChosen: Boolean,
    @SerializedName("region_name")
    val regionName: String,
    @SerializedName("is_official")
    val isOfficial: Boolean
)
