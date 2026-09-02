package com.lodgy.app.ui.nav

import androidx.compose.ui.graphics.vector.ImageVector
import com.lodgy.app.ui.icons.strokeIcon

object NavIcons {
    val Home: ImageVector = strokeIcon(
        name = "NavHome",
        "M3,11 L12,3 L21,11",
        "M5,10 L5,21 L19,21 L19,10",
    )

    val Property: ImageVector = strokeIcon(
        name = "NavProperty",
        "M4,3 L20,3 L20,21 L4,21 Z",
        "M9,8 L10,8 M14,8 L15,8 M9,12 L10,12 M14,12 L15,12 M9,16 L10,16 M14,16 L15,16",
    )

    val Tenants: ImageVector = strokeIcon(
        name = "NavTenants",
        "M6,8 a3,3 0 1,0 6,0 a3,3 0 1,0 -6,0",
        "M3,20 C3,16.7 5.7,14 9,14 C12.3,14 15,16.7 15,20",
    )

    val Payments: ImageVector = strokeIcon(
        name = "NavPayments",
        "M3,6 L21,6 L21,19 L3,19 Z",
        "M3,10 L21,10",
        "M15.6,14 a1.4,1.4 0 1,0 2.8,0 a1.4,1.4 0 1,0 -2.8,0",
    )

    val More: ImageVector = strokeIcon(
        name = "NavMore",
        "M4.5,12 a0.9,0.9 0 1,0 1.8,0 a0.9,0.9 0 1,0 -1.8,0",
        "M11.1,12 a0.9,0.9 0 1,0 1.8,0 a0.9,0.9 0 1,0 -1.8,0",
        "M17.7,12 a0.9,0.9 0 1,0 1.8,0 a0.9,0.9 0 1,0 -1.8,0",
    )
}
