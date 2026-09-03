package com.lodgy.app.ui.icons

import androidx.compose.ui.graphics.vector.ImageVector

/** One symbol per status, so a warden reads the state the same way in Hindi or English. */
object StatusIcons {
    val BedVacant: ImageVector = strokeIcon(
        "StatusBedVacant",
        "M3,18 V8",
        "M3,12 H21 V18",
        "M3,15 H21",
    )
    val BedOccupied: ImageVector = strokeIcon(
        "StatusBedOccupied",
        "M3,18 V8",
        "M3,12 H21 V18",
        "M3,15 H21",
        "M7,9 A2,2 0 1,0 7,5 A2,2 0 1,0 7,9 Z",
    )
    val Check: ImageVector = strokeIcon("StatusCheck", "M5,13 L10,18 L19,6")
    val Half: ImageVector = strokeIcon(
        "StatusHalf",
        "M12,3 A9,9 0 1,0 12,21 A9,9 0 1,0 12,3 Z",
        "M12,3 V21",
        "M12,6 L15,9 M12,10 L18,16 M12,14 L16,18",
    )
    val Alert: ImageVector = strokeIcon(
        "StatusAlert",
        "M12,3 A9,9 0 1,0 12,21 A9,9 0 1,0 12,3 Z",
        "M12,7 V13",
        "M12,16 V16.5",
    )
    val Exit: ImageVector = strokeIcon(
        "StatusExit",
        "M14,4 H19 V20 H14",
        "M4,12 H14",
        "M9,7 L4,12 L9,17",
    )
}
