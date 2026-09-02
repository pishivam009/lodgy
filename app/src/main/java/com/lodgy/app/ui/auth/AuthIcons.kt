package com.lodgy.app.ui.auth

import androidx.compose.ui.graphics.vector.ImageVector
import com.lodgy.app.ui.icons.strokeIcon

object AuthIcons {
    val Lock: ImageVector = strokeIcon(
        name = "AuthLock",
        "M5,11 L19,11 L19,20 L5,20 Z",
        "M8,11 V8 A4,4 0 0,1 16,8 V11",
    )

    val Fingerprint: ImageVector = strokeIcon(
        name = "AuthFingerprint",
        "M12,3 A7,7 0 0,0 5,10 C5,14 7,16 7,18",
        "M12,3 A7,7 0 0,1 19,10 C19,12 18.7,13.5 18,15",
        "M9,20 C8,18 7,16 7,13 A5,5 0 0,1 17,13 C17,14 17,15 16.7,16",
        "M12,8 A3,3 0 0,0 9,11 C9,14 10,16 11,18",
    )

    val Backspace: ImageVector = strokeIcon(
        name = "AuthBackspace",
        "M21,4 L8,4 L2,12 L8,20 L21,20 A2,2 0 0,0 23,18 V6 A2,2 0 0,0 21,4 Z",
        "M14,10 L18,14 M18,10 L14,14",
    )
}
