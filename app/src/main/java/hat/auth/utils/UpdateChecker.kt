package hat.auth.utils

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.gson.annotations.SerializedName
import hat.auth.BuildConfig
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

var latestUpdateInfo by mutableStateOf(UpdateInfo())

private const val base = "https://service-fdlqeyu9-1259389942.cd.apigw.tencentcs.com"

private const val appkey = "APID1rWLPEK3jhp5AU45U8Slm04Jq75PHQbvBEA"
private const val secret = "CgpzolrmwhdFwbzmCu6wkMsrfb2brP9wsfolk49c"

private val sdf by lazy {
    SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US).also {
        it.timeZone = TimeZone.getTimeZone("GMT")
    }
}
private val hmacKey by lazy { SecretKeySpec(secret.encodeToByteArray(), "HmacSHA1") }

private fun getGMTTime() = sdf.format(Calendar.getInstance().time)

private fun hmac(text: String) = Mac.getInstance("HmacSHA1").apply {
    init(hmacKey)
}.doFinal(text.encodeToByteArray())

fun checkUpdate(
    onFailure: (Throwable) -> Unit = {},
    onSuccess: (UpdateInfo) -> Unit
) {
    ioScope.launch {
        runCatching {
            val time = getGMTTime()
            val sign = Base64.getEncoder().encodeToString(hmac("x-date: $time\nGET\napplication/json\n\n\n/android-latest"))
            buildHttpRequest {
                url("$base/android-latest")
                addHeader("Accept", "application/json")
                addHeader("x-date", time)
                addHeader("Authorization", "hmac id=\"$appkey\", algorithm=\"hmac-sha1\", headers=\"x-date\", signature=\"$sign\"")
            }.execute().let { resp ->
                resp.body.use { body ->
                    if (body == null) throw IllegalStateException("检查更新失败: ${resp.message}")
                    val bstr = body.string()
                    if (bstr.startsWith("{\"message\":\"")) throw IllegalStateException("检查更新失败: ${bstr.drop(12).dropLast(2)}")
                    bstr.toDataClass<UpdateInfo>().also {
                        latestUpdateInfo = it
                    }
                }
            }
        }.onFailure(onFailure).onSuccess(onSuccess)
    }
}

data class UpdateInfo(
    @SerializedName("fn")
    val fileName: String = "null",
    @SerializedName("vc")
    val versionCode: Int = BuildConfig.VERSION_CODE,
    @SerializedName("vn")
    val versionName: String = BuildConfig.VERSION_NAME,
    @SerializedName("ds")
    val description: String = "null",
    @SerializedName("dl")
    private val fUrl: String = "",
) {
    val url get() = "https://${fUrl}/https://github.com/HolographicHat/MiHoYo-Authenticator/releases/download/$versionName/$fileName"
}
