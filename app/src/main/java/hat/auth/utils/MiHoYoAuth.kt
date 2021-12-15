package hat.auth.utils

import hat.auth.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext

object MiHoYoAuth {

    suspend fun login(
        username:String,
        password: EncryptedPassword,
        captchaData: CaptchaData
    ) = withContext(Dispatchers.IO) {
        /* 使用密码登录，获取用户ID和登录凭据 */
        var user = with(MiHoYoAPI.loginByPassword(username to password,captchaData)) {
            val i = getAsJsonObject("account_info")
            MiAccount(
                uid = i["account_id"].asString,
                ticket = i["weblogin_token"].asString
            )
        }
        /* 通过登录凭据获取两份令牌(ltoken,stoken) */
        user = MiHoYoAPI.getMultiTokenByLoginTicket(user)
        /* 获取米游社个人信息(头像Url) */
        val a = async {
            runCatching {
                MiHoYoAPI.getAvatar(user)
            }.getOrNull() ?: "https://img-static.mihoyo.com/avatar/avatar1.png"
        }
        /* 获取玩家信息(UID,昵称) */
        val b = async { MiHoYoAPI.getUserGameRolesByCookie(user).getOrNull(0) }
        val (profile, avatar) = b.await() to a.await()
        checkNotNull(profile) { "空账号" }
        user.copy(
            guid = profile.uid,
            name = profile.name,
            avatar = avatar
        )
    }

    suspend fun login(
        phone: String,
        code: String
    ) = withContext(Dispatchers.IO) {
        /* 使用验证码登录，获取用户ID和登录凭据 */
        var user = with(MiHoYoAPI.loginByMobileCaptcha(phone,code)) {
            val i = getAsJsonObject("account_info")
            MiAccount(
                uid = i["account_id"].asString,
                ticket = i["weblogin_token"].asString
            )
        }
        /* 通过登录凭据获取两份令牌(ltoken,stoken) */
        user = MiHoYoAPI.getMultiTokenByLoginTicket(user)
        /* 获取米游社个人信息(头像Url) */
        val a = async {
            runCatching {
                MiHoYoAPI.getAvatar(user)
            }.getOrNull() ?: "https://img-static.mihoyo.com/avatar/avatar1.png"
        }
        /* 获取玩家信息(UID,昵称) */
        val b = async { MiHoYoAPI.getUserGameRolesByCookie(user).getOrNull(0) }
        val (profile, avatar) = b.await() to a.await()
        checkNotNull(profile) { "空账号" }
        /* 创建Account对象并返回 */
        user.copy(
            guid = profile.uid,
            name = profile.name,
            avatar = avatar
        )
    }
}
