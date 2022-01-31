package hat.auth.utils

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import hat.auth.Application.Companion.context
import hat.auth.data.IAccount
import hat.auth.data.MiAccount
import hat.auth.data.TapAccount
import hat.auth.security.asEncryptedFile
import hat.auth.utils.TapAPI.getPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes

val accountList = mutableStateListOf<IAccount>()

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
            with(TapAPI.getCode().getPage(old)) {
                val p = second.getBasicProfile()
                old.copy(name = p.name, avatar = p.avatar).takeIf { old != it }?.let {
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
        't' -> {
            gson.fromJson(this,TapAccount::class.java)
        }
        else -> throw IllegalArgumentException()
    }
}

private fun IAccount.getFile() = run {
    when (this) {
        is MiAccount  -> File(dataDir,"m_$uid")
        is TapAccount -> File(dataDir,"t_$uid")
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
        File(dataDir,"decrypted_${it.name}").run {
            Log.d("DataManager", "File ${it.name} decrypt.")
            createNewFile()
            writeText(it.asEncryptedFile().readText())
        }
        it.delete()
    }
}

private fun migrate() {
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
