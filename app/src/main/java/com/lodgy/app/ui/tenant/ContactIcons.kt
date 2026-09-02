package com.lodgy.app.ui.tenant

import androidx.compose.ui.graphics.vector.ImageVector
import com.lodgy.app.ui.icons.strokeIcon

object ContactIcons {
    val Call: ImageVector = strokeIcon(
        "ContactCall",
        "M6,3 H9 L10.5,7 L8,9 C9,12 11,14 14,15 L16,12.5 L20,14 V17 " +
            "C20,18 19,19 18,19 C11,19 5,13 3,6 C3,5 4,3 6,3 Z",
    )

    val WhatsApp: ImageVector = strokeIcon(
        "ContactWhatsApp",
        "M12,3 A9,9 0 0,0 4.2,16.5 L3,21 L7.6,19.8 A9,9 0 1,0 12,3 Z",
        "M8.5,8.7 C8.7,8.2 8.9,8.2 9.2,8.2 H9.7 C9.9,8.2 10.1,8.2 10.3,8.6 " +
            "C10.5,9.1 10.9,10.1 10.9,10.2 C10.9,10.3 11,10.5 10.9,10.7 " +
            "C10.8,10.9 10.7,11 10.6,11.2 C10.4,11.4 10.3,11.5 10.5,11.8 " +
            "C11,12.7 11.5,13.1 12.4,13.6 C12.6,13.7 12.8,13.7 12.9,13.5 " +
            "C13.1,13.3 13.5,12.8 13.7,12.6 C13.9,12.4 14,12.4 14.3,12.5 " +
            "C14.5,12.6 15.8,13.2 16,13.3 C16.2,13.4 16.3,13.5 16.4,13.6 " +
            "C16.4,13.8 16.4,14.5 16.1,14.9 C15.7,15.4 14.9,15.8 14.4,15.8 " +
            "C14,15.8 12.7,15.4 11.2,13.9 C8.9,11.9 8.3,10 8.2,9.7 C8.1,9.4 7.8,8.8 8.5,8.7 Z",
    )

    val Sms: ImageVector = strokeIcon(
        "ContactSms",
        "M3,5 H21 V17 H7 L4,21 Z",
    )
}
