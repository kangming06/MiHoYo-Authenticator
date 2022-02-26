package hat.auth.activities.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.text.trimmedLength
import com.microsoft.appcenter.analytics.Analytics
import hat.auth.activities.MainActivity
import hat.auth.data.EncryptedPassword
import hat.auth.utils.*
import hat.auth.utils.GT3.dismissGeetestDialog
import hat.auth.utils.GT3.start
import hat.auth.utils.ui.SingleTextField
import kotlinx.coroutines.launch

private var tabPageV by mutableStateOf(true)
private var isDialogShowing by mutableStateOf(false)

fun showMiHuYoLoginDialog() { isDialogShowing = true }

fun hideMiHoYoLoginDialog() { isDialogShowing = false }

@Composable
@ExperimentalComposeUiApi
@ExperimentalAnimationApi
fun MainActivity.MiHoYoLoginDialog() {
    if (isDialogShowing) MLD()
}

@Composable
@ExperimentalComposeUiApi
@ExperimentalAnimationApi
private fun MainActivity.MLD() = Dialog(
    onDismissRequest = {
        isDialogShowing = false
    }
) {
    Column(
        modifier = Modifier.background(
            color = Color.White,
            shape = RoundedCornerShape(10.dp)
        )
    ) {
        TabRow(
            selectedTabIndex = if (tabPageV) 0 else 1,
            backgroundColor = Color.Transparent,
            contentColor = Color(0xFF4ea4dc)
        ) {
            TabItem(
                text = "验证码登录",
                page = true
            )
            TabItem(
                text = "密码登录",
                page = false
            )
        }
        Row(
            modifier = Modifier.padding(start = 15.dp, end = 15.dp, bottom = 15.dp, top = 10.dp)
        ) {
            AnimatedVisibility(
                visible = tabPageV,
                enter = expandHorizontally(),
                exit = shrinkHorizontally()
            ) {
                Column {
                    CodeLoginLayout(focusToCodeTextField)
                }
            }
            AnimatedVisibility(
                visible = !tabPageV,
                enter = expandHorizontally(),
                exit = shrinkHorizontally()
            ) {
                Column {
                    PasswordLoginLayout()
                }
            }
        }
    }
}

@Composable
private fun TabItem(
    text: String,
    page: Boolean
) = Tab(
    selected = tabPageV == page,
    onClick = {
        tabPageV = page
    }
) {
    Text(
        text = text,
        fontSize = 16.sp,
        modifier = Modifier.padding(12.5.dp)
    )
}

var focusToCodeTextField by mutableStateOf(false)

private object CL {

    var code  by mutableStateOf("")
    var phone by mutableStateOf("")
    var isCodeError  by mutableStateOf(false)
    var isPhoneError by mutableStateOf(false)

    val CodeRegex = Regex("[0-9]{6}")

    fun checkPhone() = (!phone.matches(PhoneNumRegex)).apply {
        isPhoneError = this
    }
    fun checkCode() = (!code.matches(CodeRegex)).apply {
        isCodeError = this
    }

    fun resetCodeLayout() {
        code = ""
        phone = ""
    }

    fun MainActivity.onNext() {
        if (checkPhone()) return
        GT3.init(
            ctx = this,
            afterTest = { params ->
                showLoadingDialog()
                runCatching {
                    check(!MiHoYoAPI.checkMobileRegistered(phone)) { "该手机号尚未注册" }
                    with(parseCaptchaData(params[0].toString())) {
                        MiHoYoAPI.createMobileCaptcha(phone,this)
                    }
                    focusToCodeTextField = true
                    cfr.requestFocus()
                    toast("发送成功.")
                }.onFailure {
                    processException(it)
                }
                hideLoadingDialog()
            },
            onDialogResultL = {
                dismissGeetestDialog()
                after.execute(it)
            }
        ).start()
    }

    fun MainActivity.onGo() {
        if (checkPhone() || checkCode()) return
        ioScope.launch {
            showLoadingDialog("正在登录")
            runCatching {
                MiHoYoAuth.login(phone, code).also {
                    check(!it.exists()) { "已经存在相同UID的账户了" }
                } addTo accountList
                Analytics.trackEvent("AddAccount-MiHiYo")
                resetCodeLayout()
                hideMiHoYoLoginDialog()
            }.onFailure {
                processException(it)
            }
            hideLoadingDialog()
        }
    }
}

lateinit var cfr: FocusRequester

@ExperimentalComposeUiApi
@Composable
private fun MainActivity.CodeLoginLayout(focusState: Boolean) = with(CL) {
    cfr = FocusRequester()
    val phoneFocusRequester = FocusRequester()
    SingleTextField(
        label = if (tabPageV) "手机号码" else "",
        value = phone,
        isError = isPhoneError,
        onValueChange = ovc@{
            if (it.length > 11) return@ovc
            phone = it
            if (isPhoneError) isPhoneError = checkPhone()
            if (focusState) focusToCodeTextField = false
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Phone,
            imeAction = ImeAction.Next
        ),
        keyboardActions = KeyboardActions(
            onNext = { onNext() }
        ),
        modifier = Modifier.focusRequester(phoneFocusRequester)
    )
    val skc = LocalSoftwareKeyboardController.current
    SingleTextField(
        label = if (tabPageV) "验证码" else "",
        value = code,
        isError = isCodeError,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Go
        ),
        keyboardActions = KeyboardActions(
            onGo = { onGo() }
        ),
        onValueChange = ovc@{
            if (it.length > 6) return@ovc
            code = it
            if (isCodeError) isCodeError = checkCode()
        },
        modifier = Modifier.focusRequester(cfr).onFocusEvent {
            if (it.isFocused) {
                skc!!.show()
            }
        }
    )
    LaunchedEffect(Unit) {
        if (focusState) {
            cfr.requestFocus()
        } else {
            phoneFocusRequester.requestFocus()
        }
    }
}

private object PL {

    var name     by mutableStateOf("")
    var password by mutableStateOf("")
    var isUsernameError by mutableStateOf(false)
    var isPasswordError by mutableStateOf(false)

    fun checkUsername() = (name.matches(PhoneNumRegex) || name.contains('@')).not().apply {
        isUsernameError = this
    }
    fun checkPassword() = (password.trimmedLength() !in 8..15).apply {
        isPasswordError = this
    }

    fun resetPasswordLayout() {
        name = ""
        password = ""
    }

    fun MainActivity.onGo()  {
        if (checkUsername() || checkPassword()) return
        GT3.init(
            ctx = this,
            afterTest = { params ->
                showLoadingDialog("正在登录")
                runCatching {
                    with(parseCaptchaData(params[0].toString())) {
                        MiHoYoAuth.login(name, EncryptedPassword(password),this).also {
                            check(!it.exists()) { "已经存在相同UID的账户了" }
                        } addTo accountList
                        Analytics.trackEvent("AddAccount-MiHiYo")
                    }
                    resetPasswordLayout()
                    hideMiHoYoLoginDialog()
                }.onFailure {
                    processException(it)
                }
                hideLoadingDialog()
            },
            onDialogResultL = {
                dismissGeetestDialog()
                after.execute(it)
            }
        ).start()
    }
}

@Composable
private fun MainActivity.PasswordLoginLayout() = with(PL) {
    val ufc = FocusRequester()
    val pfc = FocusRequester()
    SingleTextField(
        label = if (!tabPageV) "用户名/邮箱" else "",
        isError = isUsernameError,
        value = name,
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(ufc),
        onValueChange = {
            name = it
            if (isUsernameError) isUsernameError = checkUsername()
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next
        ),
        keyboardActions = KeyboardActions(
            onNext = { pfc.requestFocus() }
        )
    )
    SingleTextField(
        label = if (!tabPageV) "密码" else "",
        isError = isPasswordError,
        value = password,
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(pfc),
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Go
        ),
        keyboardActions = KeyboardActions(
            onGo = { onGo() }
        ),
        onValueChange = ovc@{
            if (it.length > 15) return@ovc
            password = it
            if (isPasswordError) isPasswordError = checkPassword()
        }
    )
    LaunchedEffect(Unit) {
        ufc.requestFocus()
    }
}
