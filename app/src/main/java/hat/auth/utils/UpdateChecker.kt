package hat.auth.utils

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.gson.annotations.SerializedName
import hat.auth.BuildConfig
import kotlinx.coroutines.launch

var latestUpdateInfo by mutableStateOf(UpdateInfo())

fun checkUpdate(
    onFailure: (Throwable) -> Unit = {},
    onSuccess: (UpdateInfo) -> Unit
) {
    ioScope.launch {
        runCatching {
            buildHttpRequest {
                url("https://cn-cd-1259389942.file.myqcloud.com/5a93f60a9bc11ee7/latest")
                addHeader("User-Agent", "MiHoYoAuthenticator/1.0.0")
            }.execute().let { resp ->
                resp.body.use { body ->
                    if (body == null) throw IllegalStateException("检查更新失败: ${resp.message}")
                    val bstr = body.string()
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
