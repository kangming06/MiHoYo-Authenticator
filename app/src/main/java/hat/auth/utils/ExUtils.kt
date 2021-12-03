package hat.auth.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.StrictMode
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import com.google.gson.ExclusionStrategy
import com.google.gson.FieldAttributes
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.microsoft.appcenter.AppCenter
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.crashes.Crashes
import hat.auth.Application.Companion.context
import hat.auth.BuildConfig
import hat.auth.data.Ignore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.io.File
import java.math.BigInteger
import java.security.MessageDigest
import java.util.*
import kotlin.random.Random

val ioScope = CoroutineScope(Dispatchers.IO)
val MiHoYoUrlRegex = Regex("https://user\\.mihoyo\\.com/qr_code_in_game\\.html\\?app_id=\\d&app_name=[%A-Z0-9]+&bbs=(?:true|false)&biz_key=\\w+&expire=\\d{10}&ticket=[a-f0-9]{24}")
val PhoneNumRegex = Regex("^1(?:3\\d{3}|5[^4\\D]\\d{2}|8\\d{3}|7(?:[0-35-9]\\d{2}|4(?:0\\d|1[0-2]|9\\d))|9[0-35-9]\\d{2}|6[2567]\\d{2}|4(?:(?:10|4[01])\\d{3}|[68]\\d{4}|[579]\\d{2}))\\d{6}\$")

val currentTimeSeconds get() = currentTimeMills / 1000
val currentTimeMills get() = System.currentTimeMillis()

val gson: Gson by lazy {
    GsonBuilder().addSerializationExclusionStrategy(CustomExclusionStrategy).create()
}

val deviceId by lazy {
    context.getDataFile("uuid").getText {
        UUID.randomUUID().toString()
    }
}

var loaded = false

fun buildThreadPolicy(block: StrictMode.ThreadPolicy.Builder.() -> Unit): StrictMode.ThreadPolicy =
    StrictMode.ThreadPolicy.Builder().apply(block).build()

fun StrictMode.ThreadPolicy.apply() = StrictMode.setThreadPolicy(this)

fun buildVmPolicy(block: StrictMode.VmPolicy.Builder.() -> Unit): StrictMode.VmPolicy =
    StrictMode.VmPolicy.Builder().apply(block).build()

fun StrictMode.VmPolicy.apply() = StrictMode.setVmPolicy(this)

fun getDrawableAsBitmap(@DrawableRes resId: Int) =
    AppCompatResources.getDrawable(context,resId)!!.toBitmap()

fun getDrawableAsImageBitmap(@DrawableRes resId: Int) = getDrawableAsBitmap(resId).asImageBitmap()

fun Activity.startAnalytics() {
    AppCenter.start(application,BuildConfig.APP_CENTER_KEY,Analytics::class.java,Crashes::class.java)
}

fun Activity.toast(
    msg: String,
    length: Int = Toast.LENGTH_SHORT
) = runOnUiThread {
    Toast.makeText(this,msg,length).show()
}

fun Activity.openWebPage(url: String) {
    val intent = Intent(Intent.ACTION_VIEW,Uri.parse(url))
    startActivity(intent)
}

fun Context.getDataFile(name: String) = File(dataDir.absolutePath,name)

fun File.getText(init: () -> String) = if (exists()) {
    readText()
} else {
    createNewFile()
    init().apply {
        writeText(this)
    }
}

fun Random.nextString(length: Int): String {
    val charArray = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
    var rString = ""
    (1..length).forEach { _ ->
        rString += charArray.random(this)
    }
    return rString
}

fun String.digest(algorithm: String): String = MessageDigest.getInstance(algorithm).run {
    BigInteger(1,digest(toByteArray())).toString(16).padStart(32,'0')
}

private val CustomExclusionStrategy = object : ExclusionStrategy {
    override fun shouldSkipClass(clazz: Class<*>) = false
    override fun shouldSkipField(f: FieldAttributes) = f.getAnnotation(Ignore::class.java) != null
}
