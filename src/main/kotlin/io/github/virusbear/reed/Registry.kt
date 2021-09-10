package io.github.virusbear.reed

import com.sun.jna.Pointer
import com.sun.jna.platform.win32.Advapi32
import com.sun.jna.platform.win32.Advapi32Util
import com.sun.jna.platform.win32.WinNT
import com.sun.jna.ptr.IntByReference

internal object Registry {
    fun openKey(root: RegistryRoot, path: String, sam: Sam): HKEY =
        Advapi32Util.registryGetKey(root.hkey, path, sam.sam).value

    fun closeKey(hkey: HKEY) {
        Advapi32Util.registryCloseKey(hkey)
    }

    fun keyExists(root: RegistryRoot, path: String): Boolean = Advapi32Util.registryKeyExists(root.hkey, path)

    fun valueExists(root: RegistryRoot, path: String, name: String): Boolean =
        Advapi32Util.registryValueExists(root.hkey, path, name)

    fun getKeys(root: RegistryRoot, path: String): List<String> =
        Advapi32Util.registryGetKeys(root.hkey, path, Sam.KEY_READ.sam).toList()

    fun getValues(root: RegistryRoot, path: String): List<String> = root.useKey(path, Sam.KEY_READ) { hkey ->
        val keyInfo = Advapi32Util.registryQueryInfoKey(hkey, 0)

        (0 until keyInfo.lpcValues.value).mapNotNull {
            val bufferLength = IntByReference(keyInfo.lpcMaxValueLen.value + 1)
            val buffer = CharArray(bufferLength.value)
            val status =
                Advapi32.INSTANCE.RegEnumValue(hkey, it, buffer, bufferLength, null, null, null as? Pointer, null)

            if(status != WinNT.ERROR_SUCCESS) {
                null
            } else {
                buffer.concatToString()
            }
        }
    }

    fun getValueType(root: RegistryRoot, path: String, name: String): RegistryValueType<*> =
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