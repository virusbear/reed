package com.github.virusbear.reed

import java.io.Closeable

interface Pool<T>: Closeable {
    fun borrow(): T
    fun recycle(instance: T)
}

fun <T, R> Pool<T>.useInstance(block: (T) -> R): R {
    val instance = borrow()

    try {
        return block(instance)
    } finally {
        recycle(instance)
    }
}