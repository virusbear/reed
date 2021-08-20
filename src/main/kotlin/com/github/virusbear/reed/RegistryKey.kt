package com.github.virusbear.reed

import com.sun.jna.platform.win32.Advapi32Util
import com.sun.jna.platform.win32.WinNT
import com.sun.jna.platform.win32.WinReg
import com.sun.jna.platform.win32.WinReg.HKEY
import java.rmi.registry.RegistryHandler

class RegistryKey(val root: Root, val parent: RegistryKey? = null, val path: String) {
    val absolutePath: String
        get() = (parent?.let { "${it.absolutePath}\\" } ?: "") + path

    val exists: Boolean
        get() = Advapi32Util.registryKeyExists(root.hkey, absolutePath, WinNT.KEY_READ)

    infix fun resolve(path: String): RegistryKey =
        RegistryKey(root, this, path)

    fun list(): List<String> =
        root.useKey(absolutePath) { hkey ->
            Advapi32Util.registryGetKeys(hkey).toList()
        }

    fun listKeys(): List<RegistryKey> =
        list().map {
            RegistryKey(root, this, it)
        }

    operator fun get(name: String): RegistryValue<*>? {
        TODO()
    }

    operator fun set(name: String, value: RegistryValue<*>) {
        TODO()
    }

    operator fun contains(name: String): Boolean =
        Advapi32Util.registryValueExists(root.hkey, absolutePath, name)

    fun containsKey(name: String): Boolean =
        resolve(name).exists

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RegistryKey

        if (root != other.root) return false
        if (parent != other.parent) return false
        if (path != other.path) return false

        return true
    }

    override fun hashCode(): Int {
        var result = root.hashCode()
        result = 31 * result + (parent?.hashCode() ?: 0)
        result = 31 * result + path.hashCode()
        return result
    }

    override fun toString(): String =
        "${root.name}\\$absolutePath"

    enum class Root(val hkey: HKEY) {
        HKEY_CLASSES_ROOT(WinReg.HKEY_CLASSES_ROOT),
        HKEY_CURRENT_USER(WinReg.HKEY_CURRENT_USER),
        HKEY_LOCAL_MACHINE(WinReg.HKEY_LOCAL_MACHINE),
        HKEY_USERS(WinReg.HKEY_USERS),
        HKEY_PERFORMANCE_DATA(WinReg.HKEY_PERFORMANCE_DATA),
        HKEY_PERFORMANCE_TEXT(WinReg.HKEY_PERFORMANCE_TEXT),
        HKEY_PERFORMANCE_NLSTEXT(WinReg.HKEY_PERFORMANCE_NLSTEXT),
        HKEY_CURRENT_CONFIG(WinReg.HKEY_CURRENT_CONFIG),
        HKEY_DYN_DATA(WinReg.HKEY_DYN_DATA);

        fun <T> useKey(path: String, sam: Int = WinNT.KEY_READ, block: (HKEY) -> T): T {
            val hkey = Advapi32Util.registryGetKey(hkey, path, sam)

            try {
                return block(hkey.value)
            } finally {
                Advapi32Util.registryCloseKey(hkey.value)
            }
        }
    }
}

fun main() {
    val data = RegistryKey(RegistryKey.Root.HKEY_CURRENT_USER, null, "SOFTWARE\\JavaSoft\\Prefs\\google")["device_id"]
    println(data?.javaClass)
}

interface RegistryValue<T> {
    val value: T
    fun write(key: RegistryKey, name: String)
    fun read(key: RegistryKey, name: String): RegistryValue<T>?
}

class StringRegistryValue(override val value: String): RegistryValue<String> {
    override fun write(key: RegistryKey, name: String) {
        Advapi32Util.registrySetStringValue(
            key.root.hkey,
            key.absolutePath,
            name,
            value,
            WinNT.KEY_SET_VALUE
        )
    }

    override fun read(key: RegistryKey, name: String): RegistryValue<String>? =
        Advapi32Util.registryGetStringValue(
            key.root.hkey,
            key.absolutePath,
            name,
            WinNT.KEY_QUERY_VALUE
        )?.let {
            NamedRegistryValue(name, StringRegistryValue(it))
        }
}
class BinaryRegistryValue(override val value: ByteArray): RegistryValue<ByteArray> {
    override fun write(key: RegistryKey, name: String) {
        Advapi32Util.registrySetBinaryValue(
            key.root.hkey,
            key.absolutePath,
            name,
            value,
            WinNT.KEY_SET_VALUE
        )
    }

    override fun read(key: RegistryKey, name: String): RegistryValue<ByteArray>? =
        Advapi32Util.registryGetBinaryValue(
            key.root.hkey,
            key.absolutePath,
            name,
            WinNT.KEY_QUERY_VALUE
        )?.let {
            NamedRegistryValue(name, BinaryRegistryValue(value))
        }
}
class IntRegistryValue(override val value: Int): RegistryValue<Int> {
    override fun write(key: RegistryKey, name: String) {
        Advapi32Util.registrySetIntValue(
            key.root.hkey,
            key.absolutePath,
            name,
            value,
            WinNT.KEY_SET_VALUE
        )
    }

    override fun read(key: RegistryKey, name: String): RegistryValue<Int> =
        Advapi32Util.registryGetIntValue(
            key.root.hkey,
            key.absolutePath,
            name,
            WinNT.KEY_QUERY_VALUE
        ).let {
            NamedRegistryValue(name, IntRegistryValue(it))
        }
}
class LongRegistryValue(override val value: Long): RegistryValue<Long> {
    override fun write(key: RegistryKey, name: String) {
        Advapi32Util.registrySetLongValue(
            key.root.hkey,
            key.absolutePath,
            name,
            value,
            WinNT.KEY_SET_VALUE
        )
    }

    override fun read(key: RegistryKey, name: String): RegistryValue<Long> =
        Advapi32Util.registryGetLongValue(
            key.root.hkey,
            key.absolutePath,
            name,
            WinNT.KEY_SET_VALUE
        ).let {
            NamedRegistryValue(name, LongRegistryValue(it))
        }
}
class StringArrayRegistryValue(override val value: Array<String>): RegistryValue<Array<String>> {
    override fun write(key: RegistryKey, name: String) {
        Advapi32Util.registrySetStringArray(
            key.root.hkey,
            key.absolutePath,
            name,
            value,
            WinNT.KEY_SET_VALUE
        )
    }

    override fun read(key: RegistryKey, name: String): RegistryValue<Array<String>>? =
        Advapi32Util.registryGetStringArray(
            key.root.hkey,
            key.absolutePath,
            name,
            WinNT.KEY_SET_VALUE
        )?.let {
            NamedRegistryValue(name, StringArrayRegistryValue(it))
        }
}

class NamedRegistryValue<T>(
    val name: String,
    private val registryValue: RegistryValue<T>
): RegistryValue<T> by registryValue