package com.github.virusbear.reed

import com.sun.jna.platform.win32.WinNT

interface RegistryValueType<T> {
    fun read(key: RegistryKey, name: String): RegistryValue<*>?

    companion object {
        fun forType(type: Int): RegistryValueType<*>? =
            when(type) {
                WinNT.REG_DWORD -> IntRegistryValue
                WinNT.REG_QWORD -> LongRegistryValue
                WinNT.REG_BINARY -> BinaryRegistryValue
                WinNT.REG_SZ, WinNT.REG_EXPAND_SZ -> StringRegistryValue
                WinNT.REG_MULTI_SZ -> StringArrayRegistryValue
                else -> null
            }
    }
}