package com.github.virusbear.reed

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.Closeable
import java.io.File
import java.io.PrintWriter
import java.util.concurrent.Executors

class PowerShell(val root: File? = null): Closeable {
    private val process = ProcessBuilder("powershell", "-nologo", "-noexit").apply {
        root?.let {
            directory(root)
        }
    }.start()

    private val writer = PrintWriter(process.outputStream.bufferedWriter(), true)
    private val reader = process.inputStream.bufferedReader()
    private val lock = Mutex()
    var closed = false
        private set
    var pid = process.pid()
        private set

    suspend fun exec(cmd: String): String =
        withContext(Dispatchers.PowerShell) {
            lock.withLock {
                if(closed)
                    error("PowerShell session terminated. Unable to execute command")

                val command = "$cmd; Write-Host \"`0\""
                writer.println(command)

                reader
                    .lineSequence()
                    .drop(command.lines().size)
                    .takeWhile {
                        it != "\u0000"
                    }.joinToString(System.lineSeparator())
            }
        }

    override fun close() {
        runBlocking(Dispatchers.PowerShell) {
            lock.withLock {
                writer.close()
                reader.close()
                process.destroy()
                closed = true
                pid = -1
            }
        }
    }
}

val Dispatchers.PowerShell: CoroutineDispatcher by lazy {
    Executors.newWorkStealingPool().asCoroutineDispatcher()
}