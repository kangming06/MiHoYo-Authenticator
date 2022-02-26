package hat.auth.activities.main

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import hat.auth.Application.Companion.context
import hat.auth.BuildConfig
import hat.auth.activities.MainActivity
import hat.auth.utils.*
import java.net.URLEncoder

private const val versionCode = BuildConfig.VERSION_CODE

private var isDialogShowing by mutableStateOf(false)

private const val REPO_URL = "https://github.com/HolographicHat/MiHoYo-Authenticator"

fun showAboutDialog() {
    isDialogShowing = true
}

@ExperimentalMaterialApi
@Composable
fun MainActivity.AboutDialog() {
    if (isDialogShowing) AD()
}

@ExperimentalMaterialApi
@Composable
private fun MainActivity.AD() = Dialog(
    onDismissRequest = { isDialogShowing = false }
) {
    Column(
        modifier = Modifier
            .background(
                color = Color.White,
                shape = RoundedCornerShape(10.dp)
            )
    ) {
        val newVersion = latestUpdateInfo.versionCode != versionCode
        Component(
            title = if (newVersion) {
                buildAnnotatedString {
                    pushStyle(SpanStyle(Color(0xFFEF5350)))
                    append("有可用更新，点击跳转至浏览器下载\n")
                    pop()
                    pushStyle(SpanStyle(fontSize = 14.sp))
                    append("新版本更新内容:\n- ${latestUpdateInfo.description.replace("\n", "\n- ")}")
                    pop()
                }
            } else {
                AnnotatedString("已经是最新版本", SpanStyle(Color(0xFF66BB6A)))
            },
            text = if (newVersion) null else BuildConfig.VERSION_NAME
        ) {
            if (newVersion) {
                openWebPage(latestUpdateInfo.url)
            }
        }
        Component(
            title = "交流/反馈QQ群",
            text = "913777414"
        ) {
            if (!joinQQGroup("V_g0dAkBnXw_dsNbZprmQAcsmMw5Cu6J")) {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("913777414", "913777414")
                clipboard.setPrimaryClip(clip)
                toast("调用失败，群号已复制到剪贴板")
            }
        }
        Component(
            title = "项目地址",
            text = REPO_URL
        ) {
            openWebPage(REPO_URL)
        }
    }
}

/****************
 *
 * 发起添加群流程。群号：HuYoAuth(913777414) 的 key 为： V_g0dAkBnXw_dsNbZprmQAcsmMw5Cu6J
 * 调用 joinQQGroup(V_g0dAkBnXw_dsNbZprmQAcsmMw5Cu6J) 即可发起手Q客户端申请加群 HuYoAuth(913777414)
 *
 * @param key 由官网生成的key
 * @return 返回true表示呼起手Q成功，返回false表示呼起失败
 ******************/
private fun MainActivity.joinQQGroup(key: String) = runCatching {
    val url = URLEncoder.encode("http://qm.qq.com/cgi-bin/qm/qr?from=app&p=android&jump_from=webapi&k=", "utf-8")
    Intent().apply {
        // 此Flag可根据具体产品需要自定义，如设置，则在加群界面按返回，返回手Q主界面，不设置，按返回会返回到呼起产品界面
        // intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        data = Uri.parse("mqqopensdkapi://bizAgent/qm/qr?url=$url$key")
    }.launch()
}.isSuccess

private fun Intent.launch() {
    context.startActivity(this)
}

@Composable
private fun Component(
    title: String,
    text: String,
    onClick:() -> Unit
) = Component(AnnotatedString(title),text,onClick)

@Composable
private fun Component(
    title: AnnotatedString,
    text: String?,
    onClick:() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(15.dp)
        ) {
            Text(title,style = MaterialTheme.typography.subtitle1)
            text?.let { Text(it,style = MaterialTheme.typography.body2) }
        }
    }
}
