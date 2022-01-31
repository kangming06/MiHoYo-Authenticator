package hat.auth

import android.app.Application
import android.os.Build
import android.os.StrictMode
import hat.auth.utils.apply
import hat.auth.utils.buildThreadPolicy
import hat.auth.utils.buildVmPolicy

@Suppress("unused")
class Application : Application() {

    override fun onCreate() {
        super.onCreate()
        context = this
        if (BuildConfig.DEBUG) {
            buildVmPolicy {
                detectActivityLeaks()
                detectFileUriExposure()
                detectLeakedSqlLiteObjects()
                detectLeakedClosableObjects()
                detectLeakedRegistrationObjects()
                detectContentUriWithoutPermission()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) { // API 28
                    detectNonSdkApiUsage()
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // API 29
                    detectImplicitDirectBoot()
                    detectCredentialProtectedWhileLocked()
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // API 30
                    detectUnsafeIntentLaunch()
                    detectIncorrectContextUse()
                }
                penaltyLog()
            }.apply()
            buildThreadPolicy {
                detectAll()
                penaltyLog()
            }.apply()
            StrictMode.noteSlowCall("SlowOperation")
            CrashHandler(this)
        }
    }

    companion object {
        lateinit var context: Application
    }

}