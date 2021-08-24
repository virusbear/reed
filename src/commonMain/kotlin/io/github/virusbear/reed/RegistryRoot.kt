package io.github.virusbear.reed

expect enum class RegistryRoot

fun <T> RegistryRoot.useKey(path: String, sam: Sam, block: (HKEY) -> T): T {
    val hkey = Registry.openKey(this, path, sam)

    try {
        return block(hkey)
    } finally {
        Registry.closeKey(hkey)
    }
}

expect object Registry {
    fun openKey(root: RegistryRoot, path: String, sam: Sam): HKEY
    fun closeKey(hkey: HKEY)
    fun keyExists(root: RegistryRoot, path: String): Boolean
    fun valueExists(root: RegistryRoot, path: String, name: String): Boolean
    fun getKeys(root: RegistryRoot, path: String): List<String>
    fun getValues(root: RegistryRoot, path: String): List<String>
    fun getValueType(root: RegistryRoot, path: String, name: String): RegistryValueType<*>
}

expect enum class Sam

expect class HKEY

class RegistryKey(val root: RegistryRoot, val parent: RegistryKey? = null, val path: String) {
    val absolutePath: String by lazy {
        (parent?.let { "${it.absolutePath}\\" } ?: "") + path
    }

    val exists: Boolean
        get() = Registry.keyExists(root, absolutePath)

    infix fun resolve(path: String): RegistryKey =
        RegistryKey(root, this, path)

    fun list(): List<String> =
        Registry.getKeys(root, path)

    fun listKeys(): List<RegistryKey> =
        list().map {
            RegistryKey(root, this, it)
        }

    fun valueNames(): List<String> =
        Registry.getValues(root, absolutePath)

    fun values(): List<RegistryValue<*>> =
        valueNames().mapNotNull { name ->
            this[name]
        }

    operator fun get(name: String): RegistryValue<*>? =
        if(name !in this) {
            null
        } else {
            NamedRegistryValue(
                name,
                LazyRegistryValue {
                    Registry
                        .getValueType(root, absolutePath, name)
                        .read(this, name)
                }
            )
        }

    operator fun set(name: String, value: RegistryValue<*>) {
        value.write(this, name)
    }

    operator fun contains(name: String): Boolean =
        Registry.valueExists(root, absolutePath, name)

    fun containsKey(name: String): Boolean =
        resolve(name).exists

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (this::class != other?.let { it::class }) return false

        other as RegistryKey

        if (root != other.root) return false
        if (parent != other.parent) return false
        if (path != other.path) return false

        return true
    }

    override fun hashCode(): Int {
        var result = root.hashCode()
        result = 31 * result + (parent?.hashCode() ?: 0)
        result = 31 * result + path.hashCode()
        return result
    }

    override fun toString(): String =
        "${root.name}\\$absolutePath"
}

interface RegistryValue<T> {
    val value: T
    fun write(key: RegistryKey, name: String)
}

class NamedRegistryValue<T>(
    val name: String,
    private val registryValue: RegistryValue<T>
): RegistryValue<T> by registryValue

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

interface RegistryValueType<T: RegistryValue<*>> {
    fun read(key: RegistryKey, name: String): T
}