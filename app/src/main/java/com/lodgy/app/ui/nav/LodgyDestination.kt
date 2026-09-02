package com.lodgy.app.ui.nav

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import com.lodgy.app.R

enum class LodgyDestination(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    Home("home", R.string.nav_home, NavIcons.Home),
    Property("property", R.string.nav_property, NavIcons.Property),
    Tenants("tenants", R.string.nav_tenants, NavIcons.Tenants),
    Payments("payments", R.string.nav_payments, NavIcons.Payments),
    More("more", R.string.nav_more, NavIcons.More),
}
