package io.github.virusbear.reed

class LazyRegistryValue<T>(
    factory: () -> RegistryValue<T>
): RegistryValue<T> {
    private val registryValue by lazy(factory)

    override val value: T
        get() = registryValue.value

    override fun write(key: RegistryKey, name: String) {
        registryValue.write(key, name)
    }
}