package io.github.virusbear.reed

import kotlinx.cinterop.*
import platform.windows.*

actual typealias HKEY = HKEY__

actual enum class RegistryRoot(val hkey: CPointer<HKEY>?) {
    HKEY_CLASSES_ROOT(platform.windows.HKEY_CLASSES_ROOT),
    HKEY_CURRENT_USER(platform.windows.HKEY_CURRENT_USER),
    HKEY_LOCAL_MACHINE(platform.windows.HKEY_LOCAL_MACHINE),
    HKEY_USERS(platform.windows.HKEY_USERS),
    HKEY_PERFORMANCE_DATA(platform.windows.HKEY_PERFORMANCE_DATA),
    HKEY_PERFORMANCE_TEXT(platform.windows.HKEY_PERFORMANCE_TEXT),
    HKEY_PERFORMANCE_NLSTEXT(platform.windows.HKEY_PERFORMANCE_NLSTEXT),
    HKEY_CURRENT_CONFIG(platform.windows.HKEY_CURRENT_CONFIG),
    HKEY_DYN_DATA(platform.windows.HKEY_DYN_DATA)
}

actual enum class Sam(val sam: Int) {
    KEY_ALL_ACCESS(platform.windows.KEY_ALL_ACCESS),
    KEY_CREATE_LINK(platform.windows.KEY_CREATE_LINK),
    KEY_CREATE_SUB_KEY(platform.windows.KEY_CREATE_SUB_KEY),
    KEY_ENUMERATE_SUB_KEYS(platform.windows.KEY_ENUMERATE_SUB_KEYS),
    KEY_EXECUTE(platform.windows.KEY_EXECUTE),
    KEY_NOTIFY(platform.windows.KEY_NOTIFY),
    KEY_QUERY_VALUE(platform.windows.KEY_QUERY_VALUE),
    KEY_READ(platform.windows.KEY_READ),
    KEY_SET_VALUE(platform.windows.KEY_SET_VALUE),
    KEY_WOW64_32KEY(platform.windows.KEY_WOW64_32KEY),
    KEY_WOW64_64KEY(platform.windows.KEY_WOW64_64KEY),
    KEY_WRITE(platform.windows.KEY_WRITE)
}

actual object Registry {
    actual fun openKey(root: RegistryRoot, path: String, sam: Sam): HKEY =
        memScoped {
            val phkey = alloc<HKEYVar>()
            val status = RegOpenKeyExW(root.hkey, path, 0, sam.sam.toUInt(), phkey.ptr)

            requireSuccess(status) { "Unable to open registry key. Registry.openKey failed with error \"$it\"" }

            phkey.pointed ?: error("RegOpenKeyExW did not return handle to open registry key")
        }

    actual fun closeKey(hkey: HKEY) {
        requireSuccess(RegCloseKey(hkey.ptr)) { "RegCloseKey returned: $it" }
    }

    actual fun keyExists(root: RegistryRoot, path: String): Boolean =
        memScoped {
            val phkey = alloc<HKEYVar>()
            val status = RegOpenKeyExW(root.hkey, path, 0, Sam.KEY_READ.sam.toUInt(), phkey.ptr)

            when(status) {
                ERROR_SUCCESS -> {
                    phkey.pointed?.let(::closeKey)
                    true
                }
                ERROR_FILE_NOT_FOUND -> false
                else -> {
                    requireSuccess(status) { "Unable to check for registry key existance due to error: $it" }
                    false
                }
            }
        }

    actual fun valueExists(root: RegistryRoot, path: String, name: String): Boolean =
        root.useKey(path, Sam.KEY_READ) { hkey ->
            val status = RegQueryValueExW(hkey.ptr, name, null, null, null, null)

            when(status) {
                ERROR_SUCCESS -> true
                ERROR_FILE_NOT_FOUND -> false
                else -> {
                    requireSuccess(status) { "Registry.valueExists failed with error \"$it\"" }
                    false
                }
            }
        }

    actual fun getKeys(root: RegistryRoot, path: String): List<String> =
        root.useKey(path, Sam.KEY_READ) { hkey ->
            val keyCount = querySubKeyCount(hkey)

            (0 until keyCount).map {
                memScoped {
                    val bufferLength = alloc<UIntVar>()
                    bufferLength.value = (queryMaxSubKeyLen(hkey) + 1).toUInt()

                    val buffer: LPWSTR = allocArray(bufferLength.value.toInt())

                    val status = RegEnumKeyExW(hkey.ptr, it.toUInt(), buffer, bufferLength.ptr, null, null, null, null)
                    requireSuccess(status)

                    buffer.toKString()
                }
            }
        }

    actual fun getValues(root: RegistryRoot, path: String): List<String> =
        root.useKey(path, Sam.KEY_READ) { hkey ->
            val valueCount = queryValueCount(hkey)

            (0 until valueCount).map {
                memScoped {
                    val bufferLength = alloc<UIntVar>()
                    bufferLength.value = (queryMaxValueLen(hkey) + 1).toUInt()
                    val buffer: LPWSTR = allocArray(bufferLength.value.toInt())

                    val status = RegEnumValueW(hkey.ptr, it.toUInt(), buffer, bufferLength.ptr, null, null, null, null)
                    requireSuccess(status)

                    buffer.toKString()
                }
            }
        }

    actual fun getValueType(root: RegistryRoot, path: String, name: String): RegistryValueType<*> =
        root.useKey(path, Sam.KEY_READ) { hkey ->
            memScoped {
                val type = alloc<UIntVar>()
                val status = RegQueryValueExW(hkey.ptr, name, 0, type.ptr, null, null)
                requireSuccess(status)

                when(type.value) {
                    REG_DWORD -> IntRegistryValue
                    REG_QWORD -> LongRegistryValue
                    REG_BINARY -> BinaryRegistryValue
                    REG_SZ -> StringRegistryValue
                    REG_MULTI_SZ -> StringArrayRegistryValue
                    else -> error("No RegistryValueType instance available for type ${type.value}")
                }
            }
        }

    private fun querySubKeyCount(hkey: HKEY): Int =
        memScoped {
            val subkeyCount = alloc<UIntVar>()
            val status = RegQueryInfoKeyW(hkey.ptr, null, null, null, subkeyCount.ptr, null, null, null, null, null, null, null)
            requireSuccess(status)

            subkeyCount.value.toInt()
        }

    private fun queryValueCount(hkey: HKEY): Int =
        memScoped {
            val valueCount = alloc<UIntVar>()
            val status = RegQueryInfoKeyW(hkey.ptr, null, null, null, null, null, null, valueCount.ptr, null, null, null, null)

            requireSuccess(status)

            valueCount.value.toInt()
        }

    private fun queryMaxSubKeyLen(hkey: HKEY): Int =
        memScoped {
            val maxSubkeyLength = alloc<UIntVar>()
            val status = RegQueryInfoKeyW(hkey.ptr, null, null, null, null, maxSubkeyLength.ptr, null, null, null, null, null, null)
            requireSuccess(status)

            maxSubkeyLength.value.toInt()
        }

    private fun queryMaxValueLen(hkey: HKEY): Int =
        memScoped {
            val maxValueLength = alloc<UIntVar>()
            val status = RegQueryInfoKeyW(hkey.ptr, null, null, null, null, maxValueLength.ptr, null, null, null, null, null, null)
            requireSuccess(status)

            maxValueLength.value.toInt()
        }
}

fun requireSuccess(status: Int, block: (String) -> String = { it }) {
    check(status != ERROR_SUCCESS) {
        block(formatWin32Error(status))
    }
}

fun formatWin32Error(status: Int): String =
    memScoped {
        val buffer = alloc<COpaquePointerVar>()
        FormatMessageW(
            (FORMAT_MESSAGE_FROM_SYSTEM or FORMAT_MESSAGE_ALLOCATE_BUFFER).toUInt(),
            null,
            status.toUInt(),
            0,
            buffer.reinterpret(),
            0,
            null
        )

        val message: LPWSTR? = buffer.value?.reinterpret()

        message?.toKString() ?: "Error Code $status"
    }

class BinaryRegistryValue(override val value: ByteArray): RegistryValue<ByteArray> {
    override fun write(key: RegistryKey, name: String) {
        key.root.useKey(key.absolutePath, Sam.KEY_WRITE) { hkey ->
            value.usePinned { pinned ->
                RegSetValueExW(hkey.ptr, name, 0, REG_BINARY, pinned.addressOf(0).reinterpret(), value.size.toUInt())
            }
        }
    }

    companion object: RegistryValueType<BinaryRegistryValue> {
        override fun read(key: RegistryKey, name: String): BinaryRegistryValue =
            memScoped {
                val bufferSize = alloc<UIntVar>()
                RegGetValueW(key.root.hkey, key.absolutePath, name, (RRF_RT_REG_BINARY or RRF_ZEROONFAILURE).toUInt(), null, null, bufferSize.ptr)

                ByteArray(bufferSize.value.toInt()).apply {
                    usePinned {
                        RegGetValueW(key.root.hkey, key.absolutePath, name, (RRF_RT_REG_BINARY or RRF_ZEROONFAILURE).toUInt(), null, it.addressOf(0), bufferSize.ptr)
                    }
                }.let(::BinaryRegistryValue)
            }
    }
}