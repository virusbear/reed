package com.github.virusbear.reed

import com.sun.jna.platform.win32.Advapi32Util
import com.sun.jna.platform.win32.WinNT

class IntRegistryValue(override val value: Int): RegistryValue<Int> {
    override fun write(key: RegistryKey, name: String) {
        Advapi32Util.registrySetIntValue(
            key.root.hkey, key.absolutePath, name, value, WinNT.KEY_SET_VALUE
        )
    }

    companion object: RegistryValueType<IntRegistryValue> {
        override fun read(key: RegistryKey, name: String): RegistryValue<Int> =
            Advapi32Util.registryGetIntValue(
                key.root.hkey, key.absolutePath, name, WinNT.KEY_QUERY_VALUE
            ).let(::IntRegistryValue)
    }
}