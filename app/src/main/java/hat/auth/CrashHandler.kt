package hat.auth

import android.content.Context
import android.os.Build
import hat.auth.utils.Log
import java.io.File
import java.io.Serializable
import java.lang.Thread.UncaughtExceptionHandler
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.LinkedHashMap
import kotlin.system.exitProcess
import kotlin.system.measureTimeMillis

class CrashHandler(
    private val context: Context
) : UncaughtExceptionHandler {

    init {
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        throwable.printStackTrace()
        val t = measureTimeMillis {
            saveCrashLog(throwable,collectDeviceInfo())
        }
        Log.i("CrashHandler","Collect error logs in ${t}ms.")
        exitProcess(1)
    }

    private fun collectDeviceInfo() = context.run{
        val i = LinkedHashMap<String, Serializable>()
        i["OBC"] = Build.VERSION.SDK_INT
        i["ABT"] = BuildConfig.BUILD_TYPE
        i["AVN"] = BuildConfig.VERSION_NAME
        i["AVC"] = BuildConfig.VERSION_CODE
        Build::class.java.declaredFields.forEach { f ->
            f.isAccessible = true
            val c = f.get(null)?.toString() ?: "null"
            if (c != "unknown" && c != "") {
                i[f.name] = when (f.name) {
                    "SUPPORTED_ABIS","SUPPORTED_32_BIT_ABIS","SUPPORTED_64_BIT_ABIS" ->
                        (f.get(null) as Array<*>).joinToString(",")
                    else -> c
                }
            }
        }
        return@run i
    }

    private fun saveCrashLog(ex: Throwable,m: LinkedHashMap<String,Serializable>) {
        val crashInfo = ex.stackTraceToString()
        val deviceInfo = m.entries.joinToString("\n") { "${it.key}: ${it.value}" }
        val time = SimpleDateFormat("yyyy-MM-dd_HH:mm:ss", Locale.CHINA).format(Date())
        val logFile = File(context.getDir("logs",Context.MODE_PRIVATE),"$time.crash")
        logFile.writeText("$crashInfo\n$deviceInfo")
    }
}