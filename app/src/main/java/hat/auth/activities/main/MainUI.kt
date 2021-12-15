package hat.auth.activities.main

import android.content.Intent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.zIndex
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.SwipeRefreshIndicator
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import hat.auth.activities.MainActivity
import hat.auth.activities.TapAuthActivity
import hat.auth.data.IAccount
import hat.auth.data.MiAccount
import hat.auth.data.TapAccount
import hat.auth.utils.*
import hat.auth.utils.TapAPI.confirm
import hat.auth.utils.TapAPI.getPage
import hat.auth.utils.ui.CircularProgressDialog
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrl

var currentAccount by mutableStateOf(IAccount("","",""))

fun MainActivity.processException(e: Throwable) {
    Log.e(tr = e)
    toast(e.message ?: "未知错误")
}

/** =========================================== Main =========================================== **/

@Composable
@ExperimentalMaterialApi
@ExperimentalAnimationApi
@ExperimentalComposeUiApi
@ExperimentalPermissionsApi
fun MainActivity.UI() {
    val lst = remember { accountList }
    TopAppBar(
        a = lst.size,
        normalDropdownItems = buildDropdownMenuItems {
            add("米哈游登录") {
                showMiHuYoLoginDialog()
            }
            add("Taptap登录(Beta)") {
                launcher.launch(Intent(this@UI,TapAuthActivity::class.java))
            }
        },
        debugDropdownItems = buildDropdownMenuItems {
            add("decrypt") {
                ioScope.launch {
                    decryptAll()
                    toast("done.")
                }
            }
        }
    )
    var refreshing by remember { mutableStateOf(false) }
    SwipeRefresh(
        state = rememberSwipeRefreshState(refreshing),
        onRefresh = { refreshing = true },
        modifier = Modifier.zIndex(-233F),
        indicator = { state, trigger ->
            SwipeRefreshIndicator(
                state = state,
                refreshTriggerDistance = trigger,
                contentColor = Color(0xFF2196F3)
            )
        }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            items(lst) { a ->
                AccountItem(
                    ia = a,
                    onInfoClick = {
                        ioScope.launch {
                            isLoadingDialogShowing = true
                            runCatching {
                                val mA = currentAccount as MiAccount
                                coroutineScope {
                                    val dn = async { MiHoYoAPI.getDailyNote(mA) }
                                    val gr = async { MiHoYoAPI.getGameRecord(mA) }
                                    val jn = async { with(MiHoYoAPI.getCookieToken(mA.uid,mA.sToken)) {
                                        MiHoYoAPI.getJournalNote(mA,this)
                                    } }
                                    showInfoDialog(dn.await(),gr.await(),jn.await())
                                }
                            }.onFailure {
                                processException(it)
                            }
                            isLoadingDialogShowing = false
                        }
                    },
                    onTestClick = {
                        ioScope.launch {
                            isLoadingDialogShowing = true
                            runCatching {
                                (currentAccount as? TapAccount)?.let {
                                    val c = TapAPI.getCode()
                                    val s = with(c.getPage(it)) {
                                        second.confirm(first,c.verificationUrl)
                                    }
                                    toast("Success: $s")
                                }
                            }.onFailure {
                                processException(it)
                            }
                            isLoadingDialogShowing = false
                        }
                    }
                ) {
                    showQRCodeScannerDialog()
                }
            }
        }
    }
    InfoDialog()
    AboutDialog()
    LoadingDialog()
    MiHoYoLoginDialog()
    DeleteAccountDialog()
    QRCodeScannerDialog()
    LaunchedEffect(refreshing) {
        if (refreshing) {
            runCatching {
                refreshAccount()
            }.onFailure {
                processException(it)
            }
            refreshing = false
        }
    }
}

fun MainActivity.onCookieReceived(s: String) {
    ioScope.launch {
        isLoadingDialogShowing = true
        runCatching {
            val a0 = with(cookieStringToMap(s)) {
                TapAccount(
                    acw = getValue("acw_tc"),
                    locale = getValue("locale"),
                    uid = getValue("user_id"),
                    logFrom = getValue("ACCOUNT_LOGGED_USER_FROM_WWW"),
                    tokenFrom = getValue("CONSOLES_TOKEN_FROM_WWW"),
                    xToken = getValue("XSRF-TOKEN"),
                    session = getValue("tap_sess")
                )
            }
            check(!a0.exists()) { "已经存在相同UID的账户了" }
            with(TapAPI.getCode().getPage(a0)) {
                val p = second.getBasicProfile()
                first.copy(
                    name = p.name,
                    avatar = p.avatar
                )
            } addTo accountList
        }.onFailure {
            processException(it)
        }
        isLoadingDialogShowing = false
    }
}

fun MainActivity.registerScanCallback() = registerScanCallback { result ->
    ioScope.launch {
        val u = result.text
        isLoadingDialogShowing = true
        runCatching {
            when (currentAccount) {
                is MiAccount -> {
                    MiHoYoAPI.scanQRCode(u)
                    MiHoYoAPI.confirmQRCode(currentAccount as MiAccount,u)
                }
                is TapAccount -> {
                    val au = checkNotNull(u.toHttpUrl().queryParameterValue(0))
                    with(getPage(au,currentAccount as TapAccount)) {
                        second.confirm(first)
                    }
                }
                else -> throw IllegalArgumentException("Unknown account type.")
            }
            toast("登录成功")
        }.onFailure {
            processException(it)
        }
        isLoadingDialogShowing = false
    }
}

private var loadingDialogText by mutableStateOf("正在处理请求")
private var isLoadingDialogShowing by mutableStateOf(false)

fun showLoadingDialog(msg: String = "") {
    if (msg.isNotEmpty()) loadingDialogText = msg
    isLoadingDialogShowing = true
}

fun hideLoadingDialog() {
    loadingDialogText = "正在处理请求"
    isLoadingDialogShowing = false
}

@Composable
fun MainActivity.LoadingDialog() = run {
    if (isLoadingDialogShowing) CircularProgressDialog(loadingDialogText)
}
