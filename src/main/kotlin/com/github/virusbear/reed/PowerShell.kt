package com.github.virusbear.reed

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.Closeable
import java.io.File
import java.io.PrintWriter
import java.util.*
import java.util.concurrent.Executors

class PowerShell(val root: File? = null): Closeable {
    private val process = ProcessBuilder("powershell", "-nologo", "-noexit", "-noninteractive", "-noprofile", "-OutputFormat XML").apply {
        root?.let {
            directory(root)
        }
    }.start().also {
        GlobalScope.launch(Dispatchers.IO) {
            it.inputStream.copyTo(System.out)
        }
    }

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

                writer.println(cmd)

                ""
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

fun main() {
    runBlocking {
        PowerShell().use { ps ->
            println(ps.exec("Write-Host 'Hello World';Write-Host 'PS t> b';Write-Host 'Hello World'"))
            println(ps.exec("Write-Host 'Big Issue'"))
            Thread.sleep(10000)
        }
    }
}