package hat.auth.activities.tap

import android.graphics.Bitmap
import android.webkit.*
import java.net.URL

class TapWebViewClient(
    private val onAuthFinished: (String) -> Unit
) : WebViewClient() {

    private var shouldStopLoading = false

    override fun onPageStarted(v: WebView, u: String, f: Bitmap?) {
        val p = URL(u).path
        if (p == "/login") {
            v.evaluateJavascript(script) {}
        } else {
            v.stopLoading()
            shouldStopLoading = true
            onAuthFinished(CookieManager.getInstance().getCookie("https://accounts.taptap.com")!!)
        }
    }

    override fun shouldInterceptRequest(v: WebView, r: WebResourceRequest): WebResourceResponse? {
        return if (shouldStopLoading) null else super.shouldInterceptRequest(v, r)
    }

    private val script = """
        let ia = null
        const ib = document.location.pathname
        if (ib === "/login") {
            ta = window.setInterval(() => {
                try {
                    document.getElementsByClassName("checkbox__label").forEach((it) => { it.removeAttribute("href") })
                    window.clearInterval(ta)
                } catch (ignored) { }
            }, 50)
            tb = window.setInterval(() => {
                try {
                    document.getElementsByClassName("login-page__content__social-login")[0].style.display = "none"
                    window.clearInterval(tb)
                } catch (ignored) { }
            }, 50)
        }
        """.trimIndent()
}