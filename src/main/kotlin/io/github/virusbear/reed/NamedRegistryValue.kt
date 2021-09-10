package io.github.virusbear.reed

class NamedRegistryValue<T>(
    val name: String,
    private val registryValue: RegistryValue<T>
): RegistryValue<T> by registryValue