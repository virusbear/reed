package com.github.virusbear.reed

import com.sun.jna.platform.win32.Advapi32Util
import com.sun.jna.platform.win32.WinNT

class StringArrayRegistryValue(override val value: Array<String>): RegistryValue<Array<String>> {
    override fun write(key: RegistryKey, name: String) {
        Advapi32Util.registrySetStringArray(
            key.root.hkey, key.absolutePath, name, value, WinNT.KEY_SET_VALUE
        )
    }

    companion object: RegistryValueType<StringArrayRegistryValue> {
        override fun read(key: RegistryKey, name: String): RegistryValue<Array<String>>? =
            Advapi32Util.registryGetStringArray(
                key.root.hkey, key.absolutePath, name, WinNT.KEY_QUERY_VALUE
            )?.let(::StringArrayRegistryValue)
    }
}