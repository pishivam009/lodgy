package com.lodgy.app.ui.nav

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavBackStackEntry

private const val SLIDE_MILLIS = 280
private const val FADE_MILLIS = 150

/** The outgoing screen only moves a quarter width, so it reads as sitting underneath the
 *  incoming one rather than being shoved off the edge at the same speed. */
private const val UNDERLAP = 4

private val slideSpec = tween<androidx.compose.ui.unit.IntOffset>(SLIDE_MILLIS)

val pushEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    slideInHorizontally(slideSpec) { width -> width }
}

val pushExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    slideOutHorizontally(slideSpec) { width -> -width / UNDERLAP }
}

val popEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    slideInHorizontally(slideSpec) { width -> -width / UNDERLAP }
}

val popExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    slideOutHorizontally(slideSpec) { width -> width }
}

/** Bottom-nav tabs are siblings, not a stack, so they cross-fade instead of sliding. */
val tabEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    fadeIn(tween(FADE_MILLIS))
}

val tabExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    fadeOut(tween(FADE_MILLIS))
}
