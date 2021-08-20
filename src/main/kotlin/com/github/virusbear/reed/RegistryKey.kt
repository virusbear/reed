package com.github.virusbear.reed

import com.sun.jna.Pointer
import com.sun.jna.platform.win32.*
import com.sun.jna.platform.win32.WinError.ERROR_SUCCESS
import com.sun.jna.platform.win32.WinReg.HKEY
import com.sun.jna.ptr.IntByReference
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

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

    fun values(): List<RegistryValue<*>> =
        root.useKey(absolutePath) { hkey ->
            Advapi32Util.registryGetValues(hkey)
        }.keys.mapNotNull {
            this[it]
        }

    operator fun get(name: String): RegistryValue<*>? {
        if(name !in this) {
            return null
        }

        val type = root.useKey(absolutePath) { hkey ->
            val typeRef = IntByReference()
            val result = Advapi32.INSTANCE.RegQueryValueEx(hkey, name, 0, typeRef, null as Pointer?, null)
            if(result != WinNT.ERROR_SUCCESS) {
                throw Win32Exception(result)
            }

            typeRef.value
        }

        return RegistryValueType.forType(type)?.read(this, name)?.let {
            NamedRegistryValue(name, it)
        }
    }

    operator fun set(name: String, value: RegistryValue<*>) {
        value.write(this, name)
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