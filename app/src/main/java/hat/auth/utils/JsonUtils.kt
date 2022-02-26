package hat.auth.utils

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import org.json.JSONObject

suspend fun getJson(url: () -> String) = getJson(url())

suspend fun getJson(
    url: String,
    client: OkHttpClient = OkClients.NORMAL,
    header: Pair<String,String>,
    postBody: RequestBody? = null
) = getJson(url,client,mapOf(header),postBody)

suspend fun getJson(
    url: String,
    client: OkHttpClient = OkClients.NORMAL,
    headers: Map<String,String> = emptyMap(),
    postBody: RequestBody? = null
) = withContext(Dispatchers.IO) {
    getText(url,client,headers,postBody).toJsonObject()
}

inline fun <reified T: Any> String.toDataClass(): T = Gson().fromJson(this, T::class.java)

fun <T: Any> JsonElement.toDataClass(clazz: Class<T>): T = Gson().fromJson(this,clazz)

fun JsonObject.toOrgJson() = JSONObject(Gson().fromJson(this,Map::class.java))

fun JsonObject.getJsonData() = getAsJsonObject("data")!!

fun JsonObject.checkStatus() = checkImpl("status","msg",1)

fun JsonObject.checkRetCode() = checkImpl("retcode","message",0).getJsonData()

fun <K,V> jsonBodyOf(vararg pairs: Pair<K,V>) = mapOf(*pairs).toJson().asJsonBody()

fun <K,V> Map<K,V>.toJson() = Gson().toJson(this)!!

fun String.toJsonObject() = Gson().fromJson(this,JsonObject::class.java)!!

private fun JsonObject.checkImpl(b: String, c: String, d: Int) = apply {
    if (this[b].asInt != d) {
        throw IllegalStateException(this[c].asString)
    }
}
