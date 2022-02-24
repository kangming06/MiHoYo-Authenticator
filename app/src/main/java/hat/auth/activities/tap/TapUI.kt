@file:Suppress("unused")

package hat.auth.activities.tap

import android.annotation.SuppressLint
import android.content.Intent
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import hat.auth.activities.TapAuthActivity

@SuppressLint("StaticFieldLeak")
private var cWebView: WebView? = null

@Composable
fun TapAuthActivity.UI() = run {
    Column(Modifier.fillMaxSize().background(Color.Cyan)) {
        WebView(
            modifier = Modifier.fillMaxSize().background(Color.Transparent)
        )
    }
}

fun TapAuthActivity.destroyWebView() {
    cWebView?.destroy()
    cWebView = null
}

@Composable
private fun TapAuthActivity.WebView(
    modifier: Modifier = Modifier
) = AndroidView(
    factory = { ctx ->
        WebView(ctx).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            webViewClient = TapWebViewClient {
                setResult(1001, Intent().putExtra("s", it))
                finishAfterTransition()
            }
            @SuppressLint("SetJavaScriptEnabled")
            settings.javaScriptEnabled = true
            CookieManager.getInstance().removeAllCookies(null)
            loadUrl("https://accounts.taptap.com/login")
        }
    },
    modifier = modifier
)
