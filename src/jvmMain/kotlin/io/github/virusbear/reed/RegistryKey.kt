package io.github.virusbear.reed

import com.sun.jna.Pointer
import com.sun.jna.platform.win32.*
import com.sun.jna.platform.win32.WinReg.HKEY
import com.sun.jna.ptr.IntByReference

/**
 * Represents a Windows Registry Key.
 * Registry keys are very lightweight as no operations are executed upon construction of an instance.
 * [Path][path] and [Parent][parent] parameters are not validated.
 *
 * @since 1.0
 * @author virusbear
 * @property root Registry [root][Root] this key is located at
 * @property parent parent of this key. May be null in the case of a direct child of a registry [root][Root]
 * @property path path relative to the [root][Root]/[parent] of this key.
 */
class RegistryKey(val root: Root, val parent: RegistryKey? = null, val path: String) {
    /**
     * Represents the absolute path of this [key][RegistryKey] relative to the [root][Root].
     * Example: SOFTWARE\JavaSoft\Prefs
     *
     * @since 1.0
     * @author virusbear
     */
    val absolutePath: String
        get() = (parent?.let { "${it.absolutePath}\\" } ?: "") + path

    /**
     * Checks for existence of the [key][RegistryKey] in the Windows Registry.
     *
     * @since 1.0
     * @author virusbear
     */
    val exists: Boolean
        get() = Advapi32Util.registryKeyExists(root.hkey, absolutePath, WinNT.KEY_READ)

    /**
     * Combines this [key][RegistryKey] with the give [path].
     * The [root] of the returned instance is the same as for this instance.
     * Parent will be this instance.
     * It is not validated if the path is correct or exists in this Windows Registry
     *
     * @since 1.0
     * @author virusbear
     * @param path Path to resolve against this key
     * @return combined [key][RegistryKey]
     */
    infix fun resolve(path: String): RegistryKey =
        RegistryKey(root, this, path)

    /**
     * Lists all subkey names that are currently available as direct children to this key.
     * To get a list of instances for each available subkey use [listKeys].
     *
     * @since 1.0
     * @author virusbear
     * @see listKeys
     * @return List of names of available subkeys for this key.
     */
    fun list(): List<String> =
        root.useKey(absolutePath) { hkey ->
            Advapi32Util.registryGetKeys(hkey).toList()
        }

    /**
     * Lists all direct subkeys for this key.
     * To get a list of the names of each subkey use [list].
     *
     * @since 1.0
     * @author virusbear
     * @see list
     * @return List of subkeys for this key.
     */
    fun listKeys(): List<RegistryKey> =
        list().map {
            RegistryKey(root, this, it)
        }

    /**
     * Queries the Windows Registry to return all [values][RegistryValue] available for this key.
     *
     * @since 1.0
     * @author virusbear
     * @see get
     * @return List of available [values][RegistryValue] for this key
     */
    fun values(): List<RegistryValue<*>> =
        Advapi32Util
            .registryGetValues(root.hkey, absolutePath)
            .keys.mapNotNull {
                this[it]
            }

    /**
     *
     * @since 1.0
     * @author virusbear
     * @see values
     * @return Value for the requested name. null if the value is not available.
     */
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

    /**
     *
     */
    operator fun set(name: String, value: RegistryValue<*>) {
        value.write(this, name)
    }

    /**
     *
     */
    operator fun contains(name: String): Boolean =
        Advapi32Util.registryValueExists(root.hkey, absolutePath, name)

    /**
     *
     */
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

    /**
     *
     */
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

        /**
         *
         */
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