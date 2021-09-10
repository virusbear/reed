package io.github.virusbear.reed

interface RegistryValueType<T: RegistryValue<*>> {
    fun read(key: RegistryKey, name: String): T
}