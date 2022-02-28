package hat.auth.activities.main

import android.content.Intent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.AlertDialog
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.zIndex
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.SwipeRefreshIndicator
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.crashes.Crashes
import hat.auth.activities.MainActivity
import hat.auth.activities.TapAuthActivity
import hat.auth.data.IAccount
import hat.auth.data.MiAccount
import hat.auth.data.TapAccount
import hat.auth.utils.*
import hat.auth.utils.ui.CircularProgressDialog
import hat.auth.utils.ui.TextButton
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

var currentAccount by mutableStateOf(IAccount("","",""))

fun MainActivity.processException(source: String, e: Throwable) {
    Log.e(tr = e)
    Crashes.trackError(e, mapOf("source" to source), null)
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
            add("米哈游账号登录") {
                showMiHuYoLoginDialog()
            }
            add("Taptap登录") {
                launcher.launch(Intent(this@UI,TapAuthActivity::class.java))
            }
            add("解密账号数据") {
                isLoadingDialogShowing = true
                ioScope.launch {
                    decryptAll()
                    isLoadingDialogShowing = false
                    showAlertDialog("解密完毕", "你现在可以进行数据迁移了！下一次打开应用时，所有的账号信息将会被重新加密")
                }
            }
        },
        debugDropdownItems = buildDropdownMenuItems {
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
                                    val jn = async {
                                        with(MiHoYoAPI.getCookieToken(mA.uid,mA.sToken)) {
                                            MiHoYoAPI.getJournalNote(mA,this)
                                        }
                                    }
                                    showInfoDialog(dn.await(),gr.await(),jn.await())
                                }
                            }.onFailure {
                                showAlertDialog("请求失败", it.message ?: "未知错误")
                            }
                            isLoadingDialogShowing = false
                        }
                    },
                    onTestClick = {
                        ioScope.launch {
                            isLoadingDialogShowing = true
                            runCatching {
                                (currentAccount as? TapAccount)?.let { account ->
                                    with(TapAPI) {
                                        val curl = getCode().qrcodeUrl
                                        Log.d("TapV2", curl)
                                        account.confirm(curl)
                                    }
                                    toast("Success.")
                                }
                            }.onFailure {
                                processException("TapScanTest", it)
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
    NormalAlertDialog()
    MiHoYoLoginDialog()
    DeleteAccountDialog()
    QRCodeScannerDialog()
    if (removedTapAccounts.isNotEmpty()) {
        val accountsText = removedTapAccounts.joinToString("\n") { "${it.name} (${it.uid})"}
        showAlertDialog("注意", "由于Taptap相关API变动，你需要重新登录以下账号：\n\n${accountsText}")
        removedTapAccounts.clear()
    }
    LaunchedEffect(refreshing) {
        if (refreshing) {
            runCatching {
                refreshAccount()
            }.onFailure {
                processException("AccountRefresh", it)
            }
            refreshing = false
        }
    }
}

fun MainActivity.onCookieReceived(s: String) {
    ioScope.launch {
        isLoadingDialogShowing = true
        runCatching {
            val account = with(cookieStringToMap(s)) {
                TapAccount(
                    locale = getOrDefault("locale", "zh_CN"),
                    session = getValue("ACCOUNTS_SESS")
                )
            }
            check(!account.exists()) { "已经存在相同UID的账户了" }
            with(TapAPI) {
                val p = account.profile()
                account.copy(
                    name = p.nickname,
                    avatar = p.avatar,
                    uid = p.uid.toString()
                )
            } addTo accountList
            Analytics.trackEvent("AddAccount-Taptap")
        }.onFailure {
            processException("TapAccountLogin", it)
        }
        isLoadingDialogShowing = false
    }
}

fun MainActivity.registerScanCallback() = registerScanCallback { result ->
    ioScope.launch {
        val u = result.text
        isLoadingDialogShowing = true
        runCatching {
            when (val account = currentAccount) { // smart cast
                is MiAccount -> {
                    MiHoYoAPI.scanQRCode(u)
                    MiHoYoAPI.confirmQRCode(account, u)
                    Analytics.trackEvent("LoginConfirm-MiHoYo")
                }
                is TapAccount -> {
                    with(TapAPI) {
                        account.confirm(u)
                        Analytics.trackEvent("LoginConfirm-Taptap")
                    }
                }
                else -> throw IllegalArgumentException("Unknown account type.")
            }
            toast("登录成功")
        }.onFailure {
            processException("LoginConfirm", it)
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

private var alertDialogText by mutableStateOf("")
private var alertDialogTitle by mutableStateOf("")
private var isAlertDialogShowing by mutableStateOf(false)

fun showAlertDialog(title: String = "", msg: String = "") {
    if (msg.isNotEmpty()) alertDialogText = msg
    if (title.isNotEmpty()) alertDialogTitle = title
    isAlertDialogShowing = true
}

fun hideAlertDialog() {
    alertDialogText = ""
    alertDialogTitle = ""
    isAlertDialogShowing = false
}

@Composable
fun MainActivity.NormalAlertDialog() = run {
    if (isAlertDialogShowing) AlertDialog(
        onDismissRequest = ::hideAlertDialog,
        title = {
            Text(alertDialogTitle)
        },
        text = {
            Text(alertDialogText)
        },
        confirmButton = {
            TextButton("确定") {
                hideAlertDialog()
            }
        }
    )
}
