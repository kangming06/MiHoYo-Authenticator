package hat.auth.activities.main

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
import hat.auth.BuildConfig
import hat.auth.activities.MainActivity
import hat.auth.utils.*

var lastVer = ReleaseInfo()

private var versionName = BuildConfig.VERSION_NAME

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
        Component(
            title = if (lastVer.name != versionName) {
                buildAnnotatedString {
                    pushStyle(SpanStyle(Color(0xFFEF5350)))
                    append("有可用更新，点击跳转至浏览器下载\n")
                    pop()
                    pushStyle(SpanStyle(fontSize = 14.sp))
                    append("新版本更新内容:\n${lastVer.body}")
                    pop()
                }
            } else {
                AnnotatedString("已经是最新版本", SpanStyle(Color(0xFF66BB6A)))
            },
            text = if (lastVer.name != versionName) null else versionName
        ) {
            if (currentReleaseInfo.name != versionName) {
                openWebPage(currentReleaseInfo.url)
            }
        }
        Component(
            title = "项目地址",
            text = REPO_URL
        ) {
            openWebPage(REPO_URL)
        }
        Component(
            title = "反馈",
            text = "./MiHoYo-Authenticator/issues"
        ) {
            openWebPage("$REPO_URL/issues")
        }
        Component(
            title = "支持",
            text = "./MiHoYo-Authenticator/pulls"
        ) {
            openWebPage("$REPO_URL/pulls")
        }
    }
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
