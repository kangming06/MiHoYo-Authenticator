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
            ia = window.setInterval(() => {
                try {
                    document.getElementsByClassName("checkbox__label").forEach((it) => { it.removeAttribute("href") })
                    document.getElementsByClassName("login-page__content__social-login")[0].style.display = "none"
                    window.clearInterval(ia)
                } catch (ignored) { }
            }, 200)
        }
        """.trimIndent()
}