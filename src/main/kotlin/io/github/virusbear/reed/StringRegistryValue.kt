package io.github.virusbear.reed

import com.sun.jna.platform.win32.Advapi32Util
import com.sun.jna.platform.win32.WinNT

class StringRegistryValue(override val value: String): RegistryValue<String> {
    override fun write(key: RegistryKey, name: String) {
        Advapi32Util.registrySetStringValue(
            key.root.hkey, key.absolutePath, name, value, WinNT.KEY_SET_VALUE
        )
    }

    companion object: RegistryValueType<StringRegistryValue> {
        override fun read(key: RegistryKey, name: String): StringRegistryValue =
            Advapi32Util.registryGetStringValue(
                key.root.hkey, key.absolutePath, name, WinNT.KEY_QUERY_VALUE
            )?.let(::StringRegistryValue) ?: error("Unable to read value $name from registry key $key")
    }
}