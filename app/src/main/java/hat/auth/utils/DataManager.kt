package hat.auth.utils

import android.app.Activity
import android.content.Context
import androidx.annotation.Keep
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.google.gson.annotations.SerializedName
import hat.auth.Application.Companion.context
import hat.auth.data.IAccount
import hat.auth.data.MiAccount
import hat.auth.data.TapAccount
import hat.auth.security.asEncryptedFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes

val accountList = mutableStateListOf<IAccount>()
val removedTapAccounts = mutableStateListOf<DeprecatedTapAccount>()

private val dataDir by lazy {
    context.getDir("accounts",Context.MODE_PRIVATE)
}

fun loadAccountList(activity: Activity) = ioScope.launch {
    try {
        migrate()
    } catch (e: IOException) {
        activity.toast("账号数据加载失败: ${e.message}")
    }
}

suspend fun refreshAccount() {
    val cl = hashMapOf<Int,IAccount>()
    accountList.forEachIndexed { i, old ->
        if (old is MiAccount) {
            val avatar = MiHoYoAPI.getAvatar(old)
            val name = MiHoYoAPI.getUserGameRolesByCookie(old)[0].name
            old.copy(name = name, avatar = avatar).takeIf { old != it }?.let {
                cl[i] = it
            }
        } else if (old is TapAccount) {
            with(TapAPI) {
                val p = old.profile()
                old.copy(name = p.nickname, avatar = p.avatar).takeIf { old != it }?.let {
                    cl[i] = it
                }
            }
        }
    }
    cl.forEach { (i, a) ->
        accountList[i] = a
        a.save()
    }
}

fun File.getCreationTime() =
    Files.readAttributes(toPath(),BasicFileAttributes::class.java).creationTime().toMillis()

private fun File.g() = with(asEncryptedFile().readText()) {
    when (this@g.nameWithoutExtension[0]) {
        'm' -> {
            gson.fromJson(this,MiAccount::class.java)
        }
        'p' -> {
            gson.fromJson(this,TapAccount::class.java)
        }
        else -> throw IllegalArgumentException()
    }
}

private fun IAccount.getFile() = run {
    when (this) {
        is MiAccount  -> File(dataDir,"m_$uid")
        is TapAccount -> File(dataDir,"p_$uid")
        else -> throw IllegalArgumentException()
    }
}

fun IAccount.exists() = getFile().exists()

suspend infix fun IAccount.addTo(list: SnapshotStateList<IAccount>) {
    list.add(this)
    save()
}

private suspend fun IAccount.save() = withContext(Dispatchers.IO) {
    val json = gson.toJson(this@save)
    getFile().also {
        it.delete()
    }.asEncryptedFile().writeText(json)
}

infix fun IAccount.removeFrom(list: SnapshotStateList<IAccount>) = getFile().delete().also {
    if (it) list.remove(this)
}

suspend fun decryptAll() = withContext(Dispatchers.IO) {
    dataDir.listFiles()?.filterNot { it.nameWithoutExtension.startsWith("decrypted_") }?.forEach {
        File(dataDir, "decrypted_${it.name}").run {
            Log.d("DataManager", "File ${it.name} decrypt.")
            createNewFile()
            writeText(it.asEncryptedFile().readText())
        }
        it.delete()
    }
}

@Keep
data class DeprecatedTapAccount(
    @SerializedName("c")
    override val uid: String = "",
    @SerializedName("i")
    override val name: String = "null"
): IAccount(uid, name, "")

private fun migrate() {
    val oldTapAccounts = dataDir.listFiles()?.filter { f -> f.nameWithoutExtension.startsWith("t_") }
    oldTapAccounts?.forEach {
        removedTapAccounts.add(gson.fromJson(it.asEncryptedFile().readText(), DeprecatedTapAccount::class.java))
        it.delete()
    }
    val df = dataDir.listFiles()?.filter { f -> f.nameWithoutExtension.startsWith("decrypted_") }
    if (df?.isNotEmpty() == true) {
        df.forEach {
            Log.d("DataManager", "File ${it.name} re-encrypt.")
            File(dataDir, it.nameWithoutExtension.removePrefix("decrypted_")).asEncryptedFile().writeText(it.readText())
            it.delete()
        }
    }
    dataDir.listFiles()?.also {
        it.sortBy { f -> f.getCreationTime() }
    }?.forEach {
        accountList.add(it.g())
    }
}
