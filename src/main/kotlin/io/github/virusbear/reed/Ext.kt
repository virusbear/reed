package io.github.virusbear.reed

import com.sun.jna.platform.win32.WinReg

internal typealias HKEY = WinReg.HKEY

internal fun <T> RegistryRoot.useKey(path: String, sam: Sam, block: (HKEY) -> T): T {
    val hkey = Registry.openKey(this, path, sam)

    try {
        return block(hkey)
    } finally {
        Registry.closeKey(hkey)
    }
}