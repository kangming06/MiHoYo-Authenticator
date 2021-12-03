package hat.auth.utils

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import hat.auth.BuildConfig
import kotlinx.coroutines.launch

var currentReleaseInfo by mutableStateOf(ReleaseInfo())

private const val UPDATE_URL = "https://hoyoauth-android-update.holographichat.workers.dev/"

fun checkUpdate(
    onFailure: (Throwable) -> Unit = {},
    onSuccess: (ReleaseInfo) -> Unit
) {
    ioScope.launch {
        runCatching {
            getJson(UPDATE_URL).let {
                val pkg = it["assets"].asJsonArray[0].asJsonObject
                ReleaseInfo(
                    name = it["tag_name"].asString,
                    pkgName = pkg["name"].asString,
                    url = pkg["browser_download_url"].asString,
                    body = it["body"].asString
                ).also { r ->
                    currentReleaseInfo = r
                }
            }
        }.onFailure(onFailure).onSuccess(onSuccess)
    }
}

data class ReleaseInfo(
    val name: String = BuildConfig.VERSION_NAME,
    val pkgName: String = "null",
    val url: String = "null",
    val body: String = "null"
)
