package hat.auth.utils

import android.app.Activity
import com.geetest.sdk.GT3ConfigBean
import com.geetest.sdk.GT3ErrorBean
import com.geetest.sdk.GT3GeetestUtils
import com.geetest.sdk.GT3Listener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private var mmtKey = ""

object GT3 {

    private var utils: GT3GeetestUtils? = null

    private val config = GT3ConfigBean().apply {
        pattern = 1
        isCanceledOnTouchOutside = true
    }

    fun getConfig() = config

    fun getGeetest() = utils!!.getGeetest()

    fun onDestroy() {
        utils?.destory()
    }

    fun Activity.initGeetest() = ioScope.launch {
        utils = GT3GeetestUtils(this@initGeetest)
    }

    fun init(
        ctx: Activity,
        beforeTest: SuspendLambda = {
            runCatching {
                MiHoYoAPI.createMMT().also {
                    mmtKey = it["mmt_key"].asString
                    config.api1Json = it.toOrgJson()
                }
                utils!!.getGeetest()
            }.onFailure {
                Log.e("Exception","h.a.u.GT3:L34 ",it)
                ctx.toast(it.message ?: "未知错误")
                dismissGeetestDialog()
            }
        },
        afterTest: SuspendLambda,
        onClosedL: IntLambda = {},
        onButtonClickL: Lambda = {
            before.execute()
        },
        onSuccessL: StringLambda = {},
        onStatisticsL: StringLambda = {},
        onDialogReadyL: StringLambda = {},
        onDialogResultL: StringLambda = {},
        onReceiveCaptchaCodeL: IntLambda = l0@{
            if (it != 1) return@l0
            dismissGeetestDialog()
        },
        onFailedL: (GT3ErrorBean) -> Unit = {},
    ) = object : GT3ListenerEx(before = blk(beforeTest),after = blk(afterTest)) {
        override fun onClosed(i: Int) { onClosedL(i) }
        override fun onButtonClick() { onButtonClickL() }
        override fun onSuccess(s: String) { onSuccessL(s) }
        override fun onFailed(g: GT3ErrorBean) { onFailedL(g) }
        override fun onStatistics(i: String) { onStatisticsL(i) }
        override fun onDialogReady(p0: String) { onDialogReadyL(p0) }
        override fun onDialogResult(p0: String) { onDialogResultL(p0) }
        override fun onReceiveCaptchaCode(i: Int) { onReceiveCaptchaCodeL(i) }
    }

    fun GT3ListenerEx.start() {
        config.listener = this
        utils!!.init(config)
        utils!!.startCustomFlow()
    }

    fun dismissGeetestDialog() = utils!!.dismissGeetestDialog()

}

open class GT3ListenerEx(
    val before: Runnable,
    val after: Runnable
) : GT3Listener() {

    override fun onButtonClick() {}

    override fun onClosed(i: Int) {}

    override fun onSuccess(s: String) {}

    override fun onStatistics(i: String) {}

    override fun onFailed(g: GT3ErrorBean) {}

    override fun onReceiveCaptchaCode(i: Int) {}

}

data class CaptchaData(
    val mmtKey: String,
    val secCode: String,
    val validate: String,
    val challenge: String
)

fun parseCaptchaData(json: String) = with(json.toJsonObject()) {
    CaptchaData(
        mmtKey = mmtKey,
        secCode = get("geetest_seccode").asString,
        validate = get("geetest_validate").asString,
        challenge = get("geetest_challenge").asString
    )
}

abstract class Runnable {
    abstract fun execute(vararg params: Any): Job
}

private fun blk(func: SuspendLambda) = object: Runnable() {
    override fun execute(vararg params: Any) = ioScope.launch {
        func(params)
    }
}

private typealias SuspendLambda = suspend CoroutineScope.(Array<out Any>) -> Unit
private typealias Lambda = GT3ListenerEx.() -> Unit
private typealias IntLambda = GT3ListenerEx.(Int) -> Unit
private typealias StringLambda = GT3ListenerEx.(String) -> Unit
