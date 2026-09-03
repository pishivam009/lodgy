package com.lodgy.app.ui.icons

import androidx.compose.ui.graphics.vector.ImageVector

object CommonIcons {
    val Back: ImageVector = strokeIcon("CommonBack", "M15,18 L9,12 L15,6")
    val ChevronRight: ImageVector = strokeIcon("CommonChevronRight", "M9,18 L15,12 L9,6")
    val Plus: ImageVector = strokeIcon("CommonPlus", "M12,5 V19 M5,12 H19")
    val Trash: ImageVector = strokeIcon(
        "CommonTrash",
        "M4,7 H20",
        "M9,7 V5 A1,1 0 0,1 10,4 H14 A1,1 0 0,1 15,5 V7",
        "M6,7 L7,20 H17 L18,7",
    )
    val ArrowUp: ImageVector = strokeIcon("CommonArrowUp", "M12,19 V5 M6,11 L12,5 L18,11")
    val ArrowDown: ImageVector = strokeIcon("CommonArrowDown", "M12,5 V19 M6,13 L12,19 L18,13")
    val Edit: ImageVector = strokeIcon(
        "CommonEdit",
        "M4,20 H8 L18,10 L14,6 L4,16 Z",
        "M14,6 L18,10",
    )
    val Report: ImageVector = strokeIcon(
        "CommonReport",
        "M4,20 V10",
        "M10,20 V4",
        "M16,20 V14",
    )
    val Export: ImageVector = strokeIcon(
        "CommonExport",
        "M12,3 V15",
        "M7,8 L12,3 L17,8",
        "M4,17 V20 H20 V17",
    )
}
