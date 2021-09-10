package io.github.virusbear.reed

import com.sun.jna.platform.win32.WinNT

internal enum class Sam(val sam: Int) {
    KEY_ALL_ACCESS(WinNT.KEY_ALL_ACCESS),
    KEY_CREATE_LINK(WinNT.KEY_CREATE_LINK),
    KEY_CREATE_SUB_KEY(WinNT.KEY_CREATE_SUB_KEY),
    KEY_ENUMERATE_SUB_KEYS(WinNT.KEY_ENUMERATE_SUB_KEYS),
    KEY_EXECUTE(WinNT.KEY_EXECUTE),
    KEY_NOTIFY(WinNT.KEY_NOTIFY),
    KEY_QUERY_VALUE(WinNT.KEY_QUERY_VALUE),
    KEY_READ(WinNT.KEY_READ),
    KEY_SET_VALUE(WinNT.KEY_SET_VALUE),
    KEY_WOW64_32KEY(WinNT.KEY_WOW64_32KEY),
    KEY_WOW64_64KEY(WinNT.KEY_WOW64_64KEY),
    KEY_WRITE(WinNT.KEY_WRITE)
}