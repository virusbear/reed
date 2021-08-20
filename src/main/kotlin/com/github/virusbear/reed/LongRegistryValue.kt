package com.github.virusbear.reed

import com.sun.jna.platform.win32.Advapi32Util
import com.sun.jna.platform.win32.WinNT

class LongRegistryValue(override val value: Long): RegistryValue<Long> {
    override fun write(key: RegistryKey, name: String) {
        Advapi32Util.registrySetLongValue(
            key.root.hkey, key.absolutePath, name, value, WinNT.KEY_SET_VALUE
        )
    }

    companion object: RegistryValueType<LongRegistryValue> {
        override fun read(key: RegistryKey, name: String): RegistryValue<Long> =
            Advapi32Util.registryGetLongValue(
                key.root.hkey, key.absolutePath, name, WinNT.KEY_QUERY_VALUE
            ).let(::LongRegistryValue)
    }
}