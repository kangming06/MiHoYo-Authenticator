package hat.auth.utils

import android.os.Build
import com.google.gson.annotations.SerializedName
import hat.auth.data.TapAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.RequestBody
import okhttp3.Response
import java.net.URLEncoder
import java.util.*

val TapUrlRegex = Regex("https://www\\.taptap\\.com/qrcode/to\\?url=https%3A%2F%2Fwww\\.taptap\\.com%2Fdevice%3Fqrcode%3D1%26user_code%3D\\w{5}")

object TapAPI {

    suspend fun getCode() = getJson(
        url = "https://www.taptap.com/oauth2/v1/device/code",
        headers = mapOf(
            "User-Agent" to "TapTapUnitySDK/1.0 UnityPlayer/2017.4.30f1",
            "Content-Type" to "application/x-www-form-urlencoded",
            "X-Unity-Version" to "2017.4.30f1"
        ),
        postBody = buildFormBody {
            add("version","1.0.1")
            add("platform","unity")
            add("scope","public_profile")
            add("response_type","device_code")
            add("client_id","WT6NfH8PsSmZtyXNFb")
            add("info","{\"device_id\":\"Windows PC\"}")
        }
    ).also {
        if (!it.get("success").asBoolean) {
            throw IllegalStateException(it.getAsJsonObject("data").get("msg").asString)
        }
    }.getAsJsonObject("data").toDataClass(TapOAuthCode::class.java)

    private suspend inline fun <reified R : Any> TapAccount.request(url: String) = request(url) { toDataClass<R>() }

    private suspend inline fun <reified R : Any> TapAccount.post(
        url: String,
        formBuilder: FormBody.Builder.() -> Unit
    ) = FormBody.Builder().apply(formBuilder).build().let { body -> request(url, body) { toDataClass<R>() } }

    private suspend fun <R> TapAccount.request(
        url: String,
        body: RequestBody? = null,
        scope: Response.() -> R
    ) = withContext(Dispatchers.IO) {
        buildHttpRequest {
            val acc = this@request
            val uuid = UUID.randomUUID().toString()
            url(url)
            addHeader("Cookie", acc.toString())
            addHeader("X-UA", "V=1&PN=Accounts&LANG=${acc.locale}&VN_CODE=1&UID=$uuid&VID=${acc.uid}")
            addHeader("X-CLIENT-XUA", "V=1&PN=TapTap&VN_CODE=223001000&LOC=CN&LANG=${acc.locale}}&CH=default&UID=$uuid&VID=${acc.uid}&$deviceXUA")
            body?.let { post(body) }
        }.execute(OkClients.NO_REDIRECT).use(scope)
    }

    private const val base = "https://accounts.taptap.com/api"

    // NT: Network type
    //     UNKNOWN  0
    //     WIFI     1
    //     NET_2G   2
    //     NET_3G   3
    //     NET_4G   4
    //     NET_5G   5
    private val deviceXUA by lazy {
        "NT=1&SR=${getScreenResolution()}&DEB=${Build.MANUFACTURER}&DEM=${Build.MODEL}&OSV=${Build.VERSION.RELEASE}"
    }

    suspend fun TapAccount.profile() = request<TapProfile>("$base/user/profile")

    suspend fun TapAccount.confirm(qrcodeUrl: String) {
        val parameters = checkNotNull(qrcodeUrl.toHttpUrl().queryParameterValue(0)) {
            "无效的二维码"
        }.toHttpUrl().query.let {
            checkNotNull(it) { "无效的二维码" }
        }
        val info = request<TapOAuth>("$base/oauth2/auth?parameters=${parameters.urlEncoded()}")
        post<TapOAuthResult>("$base/oauth2/approve") {
            add("grant_type", info.type)
            add("client_id", info.client.id)
            add("scopes", "public_profile")
            add("continuation_code", info.code)
        }
    }

    data class TapOAuthCode(
        @SerializedName("device_code")
        val deviceCode: String = "",
        @SerializedName("user_code")
        val user: String = "",
        @SerializedName("verification_url")
        val verificationUrl: String = "",
        @SerializedName("qrcode_url")
        val qrcodeUrl: String = ""
    )

    private data class TapOAuthResult(
        @SerializedName("redirect_uri")
        val redirectTo: String
    )

    data class TapProfile(
        @SerializedName("avatar")
        val avatar: String = "",
        @SerializedName("nickname")
        val nickname: String = "",
        @SerializedName("user_id")
        val uid: Int = 0
    )

    private data class TapOAuth(
        @SerializedName("client")
        val client: Client,
        @SerializedName("continuation_code")
        val code: String = "",
        @SerializedName("grant_type")
        val type: String = ""
    ) {
        data class Client(
            @SerializedName("icon_url")
            val iconUrl: String = "",
            @SerializedName("id")
            val id: String = "",
            @SerializedName("name")
            val name: String = "",
            @SerializedName("status")
            val status: String = ""
        )
    }

    private fun Response.getStringBody() = notNullBody.string()

    @Suppress("DEPRECATION")
    private fun getScreenResolution() = "1080x2340"

    private fun String.urlEncoded() = URLEncoder.encode(this, "utf-8")

    private inline fun <reified T : Any> Response.toDataClass() = getStringBody().toJsonObject().let { obj ->
        if (obj["code"].asString != "OK") {
            throw IllegalStateException(obj.getAsJsonObject("error")["reason"].asString)
        } else obj["data"].toDataClass(T::class.java)
    }
}
