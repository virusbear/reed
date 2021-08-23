package io.github.virusbear.reed

import com.sun.jna.platform.win32.Advapi32Util
import com.sun.jna.platform.win32.WinNT

class BinaryRegistryValue(override val value: ByteArray): RegistryValue<ByteArray> {
    override fun write(key: RegistryKey, name: String) {
        Advapi32Util.registrySetBinaryValue(
            key.root.hkey, key.absolutePath, name, value, WinNT.KEY_SET_VALUE
        )
    }

    companion object: RegistryValueType<BinaryRegistryValue> {
        override fun read(key: RegistryKey, name: String): RegistryValue<ByteArray>? =
            Advapi32Util.registryGetBinaryValue(
                key.root.hkey, key.absolutePath, name, WinNT.KEY_QUERY_VALUE
            )?.let(::BinaryRegistryValue)
    }
}