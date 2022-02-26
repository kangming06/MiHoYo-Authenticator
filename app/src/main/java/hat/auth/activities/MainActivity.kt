package hat.auth.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.ui.ExperimentalComposeUiApi
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.microsoft.appcenter.analytics.Analytics
import hat.auth.BuildConfig
import hat.auth.activities.main.*
import hat.auth.utils.*
import hat.auth.utils.GT3.initGeetest
import hat.auth.utils.ui.ComposeActivity

class MainActivity : ComposeActivity() {

    lateinit var launcher: ActivityResultLauncher<Intent>

    @OptIn(
        ExperimentalMaterialApi::class,
        ExperimentalAnimationApi::class,
        ExperimentalComposeUiApi::class,
        ExperimentalPermissionsApi::class
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setMainTheme()
        init { UI() }
        initGeetest()
        registerLauncher()
        if (!loaded) {
            loadAccountList(this)
            startAnalytics()
            Analytics.trackEvent("AppStartup", buildMap {
                put("version", BuildConfig.VERSION_NAME)
            })
            registerScanCallback()
            loaded = true
        }
    }

    override fun onResume() {
        super.onResume()
        captureManager?.onResume()
    }

    override fun onPause() {
        super.onPause()
        captureManager?.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        GT3.onDestroy()
        captureManager?.onDestroy()
    }

    private fun registerLauncher() {
        launcher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) callback@{
            if (it.resultCode == 1001) {
                val s = checkNotNull(it.data?.getStringExtra("s"))
                onCookieReceived(s)
            }
        }
    }

}