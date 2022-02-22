package hat.auth.utils

import hat.auth.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl

@Suppress("unused")
object MiHoYoAPI {

    private const val SOURCE = "webstatic.mihoyo.com"
    private const val BBSAPI = "https://bbs-api.mihoyo.com"
    private const val SDKAPI = "https://api-sdk.mihoyo.com"
    private const val HK4API = "https://hk4e-api.mihoyo.com"
    private const val WEBAPI = "https://webapi.account.mihoyo.com/Api"
    private const val RECAPI = "https://api-takumi-record.mihoyo.com/game_record/app"
    private const val TAKUMI_AUTH_API = "https://api-takumi.mihoyo.com/auth/api"
    private const val TAKUMI_BINDING_API = "https://api-takumi.mihoyo.com/binding/api"

    suspend fun createMMT() = getJson(
        url = "$WEBAPI/create_mmt",
        postBody = buildFormBody {
            add("mmt_type",1)
            add("scene_type",1)
            addTimestamp("now")
        }
    ).getJsonData().checkStatus().getAsJsonObject("mmt_data")!!

    suspend fun createMobileCaptcha(
        phoneNumber: String,
        cData: CaptchaData,
        type: String = "login",
    ) = getJson(
        url = "$WEBAPI/create_mobile_captcha",
        postBody = buildFormBody {
            add("action_type",type)
            add("mobile",phoneNumber)
            addTimestamp()
            addCaptchaData(cData)
        }
    ).getJsonData().checkStatus()

    suspend fun checkMobileRegistered(phoneNumber: String) = getJson(
        url = "$WEBAPI/is_mobile_registrable?mobile=${phoneNumber}&t=${currentTimeMills}",
    ).getJsonData().checkStatus()["is_registable"].asInt == 1

    suspend fun checkGameTokenValid(uid: String, token: String) = getJson(
        url = "https://hk4e-sdk.mihoyo.com/hk4e_cn/mdk/shield/api/verify",
        postBody = jsonBodyOf(
            "uid" to uid,
            "token" to token
        )
    )["retcode"].asInt == 0

    @Suppress("MemberVisibilityCanBePrivate")
    suspend fun getGameToken(u: MiAccount) = getJson {
        "$TAKUMI_AUTH_API/getGameToken?stoken=${u.sToken}&uid=${u.uid}"
    }.checkRetCode()["game_token"].asString!!

    suspend fun getMultiTokenByLoginTicket(u: MiAccount) = getJson {
        "$TAKUMI_AUTH_API/getMultiTokenByLoginTicket?login_ticket=${u.ticket}&token_types=3&uid=${u.uid}"
    }.checkRetCode().getAsJsonArray("list").associate {
        it.asJsonObject.let { o ->
            o["name"].asString to o["token"].asString
        }
    }.run {
        u.copy(
            lToken = getValue("ltoken"),
            sToken = getValue("stoken")
        )
    }

    // TODO: DATA CLASS
    suspend fun getUserFullInfo(u: MiAccount) = getJson(
        url = "$BBSAPI/user/api/getUserFullInfo?uid=${u.uid}",
        header = "Cookie" to "stuid=${u.uid}; stoken=${u.sToken}"
    ).checkRetCode()

    suspend fun getAvatar(u: MiAccount): String =
        getUserFullInfo(u).getAsJsonObject("user_info")["avatar_url"].asString

    suspend fun getUserGameRolesByCookie(
        u: MiAccount,
        biz: String = "hk4e_cn"
    ) = getJson(
        url = "$TAKUMI_BINDING_API/getUserGameRolesByCookie?game_biz=$biz",
        header = "Cookie" to "ltuid=${u.uid}; ltoken=${u.lToken}"
    ).checkRetCode().getAsJsonArray("list").map {
        it.toDataClass(UserGameRole::class.java)
    }

    suspend fun getCookieToken(uid: String,sToken: String) = getJson {
        "$TAKUMI_AUTH_API/getCookieAccountInfoBySToken?stoken=${sToken}&uid=${uid}"
    }.checkRetCode()["cookie_token"].asString!!

    suspend fun loginByMobileCaptcha(
        mobile: String,
        code: String
    ) = withContext(Dispatchers.IO) {
        getJson(
            url = "$WEBAPI/login_by_mobilecaptcha",
            postBody = buildFormBody {
                add("mobile", mobile)
                add("mobile_captcha", code)
                add("source", SOURCE)
                addTimestamp()
            }
        ).getJsonData().checkStatus()
    }

    suspend fun loginByPassword(
        pair: Pair<String,EncryptedPassword>,
        cData: CaptchaData
    ) = getJson(
        url = "$WEBAPI/login_by_password",
        postBody = buildFormBody {
            add("source", SOURCE)
            add("account", pair.first)
            add("password", pair.second.get())
            add("is_crypto", "true")
            addTimestamp()
            addCaptchaData(cData)
        }
    ).getJsonData().checkStatus()

    suspend fun scanQRCode(codeUrl: String) = codeUrl.parseQRCodeUrl().let { urlParams ->
        getJson(
            url = "$SDKAPI/${urlParams["biz_key"]}/combo/panda/qrcode/scan",
            postBody = jsonBodyOf(
                "app_id" to urlParams["app_id"],
                "ticket" to urlParams["ticket"],
                "device" to deviceId
            )
        ).checkRetCode()
    }

    suspend fun confirmQRCode(
        u: MiAccount,
        codeUrl: String
    ) = codeUrl.parseQRCodeUrl().let { urlParams ->
        getJson(
            url = "$SDKAPI/hk4e_cn/combo/panda/qrcode/confirm",
            postBody = jsonBodyOf(
                "app_id" to urlParams["app_id"],
                "ticket" to urlParams["ticket"],
                "device" to deviceId,
                "payload" to mapOf(
                    "proto" to "Account",
                    "raw" to mapOf(
                        "uid" to u.uid,
                        "token" to getGameToken(u)
                    ).toJson()
                )
            )
        ).checkRetCode()
    }

    private fun String.parseQRCodeUrl() = toHttpUrl().let { u ->
        mutableMapOf<String,String>().apply {
            u.queryParameterNames.forEach { k ->
                this[k] = u.queryParameter(k) ?: ""
            }
        }.toMap()
    }

    suspend fun changeDataSwitch(gid: Int, sid: Int, isPublic: Boolean = true) = getJson(
        url = "$RECAPI/card/wapi/changeDataSwitch",
        client = OkClients.SAPI,
        postBody = jsonBodyOf(
            "game_id" to gid,
            "is_public" to isPublic,
            "switch_id" to sid
        )
    ).checkRetCode()

    suspend fun getDailyNote(u: MiAccount) = getJson(
        url = "$RECAPI/genshin/api/dailyNote?server=cn_gf01&role_id=${u.guid}",
        client = OkClients.SAPI
    ).checkRetCode().toDataClass(DailyNote::class.java)

    suspend fun getGameRecord(u: MiAccount) = getJson(
        url = "$RECAPI/genshin/api/index?server=cn_gf01&role_id=${u.guid}",
        client = OkClients.SAPI
    ).checkRetCode().toDataClass(GameRecord::class.java)

    suspend fun getJournalNote(u: MiAccount,cookieToken: String,month: Int = 0) = getJson(
        url = "$HK4API/event/ys_ledger/monthInfo?month=$month&bind_uid=${u.guid}&bind_region=cn_gf01",
        header = "Cookie" to "account_id=${u.uid}; cookie_token=$cookieToken"
    ).checkRetCode().toDataClass(JourneyNotes::class.java)

}