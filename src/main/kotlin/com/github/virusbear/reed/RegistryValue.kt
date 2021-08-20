package com.github.virusbear.reed

interface RegistryValue<T> {
    val value: T
    fun write(key: RegistryKey, name: String)
}