package io.github.virusbear.reed

import com.sun.jna.Pointer
import com.sun.jna.platform.win32.Advapi32
import com.sun.jna.platform.win32.Advapi32Util
import com.sun.jna.platform.win32.WinNT
import com.sun.jna.platform.win32.WinReg
import com.sun.jna.ptr.IntByReference

actual typealias HKEY = WinReg.HKEY

actual enum class RegistryRoot(val hkey: HKEY) {
    HKEY_CLASSES_ROOT(WinReg.HKEY_CLASSES_ROOT),
    HKEY_CURRENT_USER(WinReg.HKEY_CURRENT_USER),
    HKEY_LOCAL_MACHINE(WinReg.HKEY_LOCAL_MACHINE),
    HKEY_USERS(WinReg.HKEY_USERS),
    HKEY_PERFORMANCE_DATA(WinReg.HKEY_PERFORMANCE_DATA),
    HKEY_PERFORMANCE_TEXT(WinReg.HKEY_PERFORMANCE_TEXT),
    HKEY_PERFORMANCE_NLSTEXT(WinReg.HKEY_PERFORMANCE_NLSTEXT),
    HKEY_CURRENT_CONFIG(WinReg.HKEY_CURRENT_CONFIG),
    HKEY_DYN_DATA(WinReg.HKEY_DYN_DATA)
}

actual enum class Sam(val sam: Int) {
    KEY_ALL_ACCESS(WinNT.KEY_ALL_ACCESS),
    KEY_CREATE_LINK(WinNT.KEY_CREATE_LINK),
    KEY_CREATE_SUB_KEY(WinNT.KEY_CREATE_SUB_KEY),
    KEY_ENUMERATE_SUB_KEYS(WinNT.KEY_ENUMERATE_SUB_KEYS),
    KEY_EXECUTE(WinNT.KEY_EXECUTE),
    KEY_NOTIFY(WinNT.KEY_NOTIFY),
    KEY_QUERY_VALUE(WinNT.KEY_QUERY_VALUE),
    KEY_READ(WinNT.KEY_READ),
    KEY_SET_VALUE(WinNT.KEY_SET_VALUE),
    KEY_WOW64_32KEY(WinNT.KEY_WOW64_32KEY),
    KEY_WOW64_64KEY(WinNT.KEY_WOW64_64KEY),
    KEY_WRITE(WinNT.KEY_WRITE)
}

actual object Registry {
    actual fun openKey(root: RegistryRoot, path: String, sam: Sam): HKEY =
        Advapi32Util.registryGetKey(root.hkey, path, sam.sam).value

    actual fun closeKey(hkey: HKEY) {
        Advapi32Util.registryCloseKey(hkey)
    }

    actual fun keyExists(root: RegistryRoot, path: String): Boolean =
        Advapi32Util.registryKeyExists(root.hkey, path)

    actual fun valueExists(root: RegistryRoot, path: String, name: String): Boolean =
        Advapi32Util.registryValueExists(root.hkey, path, name)

    actual fun getKeys(root: RegistryRoot, path: String): List<String> =
        Advapi32Util.registryGetKeys(root.hkey, path, Sam.KEY_READ.sam).toList()

    actual fun getValues(root: RegistryRoot, path: String): List<String> =
        root.useKey(path, Sam.KEY_READ) { hkey ->
            val keyInfo = Advapi32Util.registryQueryInfoKey(hkey, 0)

            (0 until keyInfo.lpcValues.value).mapNotNull {
                val bufferLength = IntByReference(keyInfo.lpcMaxValueLen.value + 1)
                val buffer = CharArray(bufferLength.value)
                val status = Advapi32.INSTANCE.RegEnumValue(hkey, it, buffer, bufferLength, null, null, null as? Pointer, null)

                if(status != WinNT.ERROR_SUCCESS) {
                    null
                } else {
                    buffer.concatToString()
                }
            }
        }

    actual fun getValueType(root: RegistryRoot, path: String, name: String): RegistryValueType<*> =
        root.useKey(path, Sam.KEY_READ) { hkey ->
            val type = IntByReference()
            val status = Advapi32.INSTANCE.RegQueryValueEx(hkey, name, 0, type, null as? Pointer, null)

            if(status != WinNT.ERROR_SUCCESS) {
                error("Registry.getValueType returned status $status")
            }

            when(type.value) {
                WinNT.REG_DWORD -> IntRegistryValue
                WinNT.REG_QWORD -> LongRegistryValue
                WinNT.REG_BINARY -> BinaryRegistryValue
                WinNT.REG_SZ, WinNT.REG_EXPAND_SZ -> StringRegistryValue
                WinNT.REG_MULTI_SZ -> StringArrayRegistryValue
                else -> error("No RegistryValueType instance available for type ${type.value}")
            }
        }
}