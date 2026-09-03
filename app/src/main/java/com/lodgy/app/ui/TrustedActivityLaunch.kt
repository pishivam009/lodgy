package com.lodgy.app.ui

/**
 * Lets a screen exempt the *next* whole-app backgrounding from AppRootViewModel's PIN
 * re-lock, for launches the app itself initiated and expects a result back from (the
 * system Camera, in particular - a heavyweight separate-app activity that genuinely
 * backgrounds us, unlike the Photo Picker or a share-sheet overlay which don't).
 * Without this, returning from the camera mid-form drops the warden on the lock screen
 * and silently abandons whatever wasn't saved yet.
 */
object TrustedActivityLaunch {
    @Volatile
    private var expected = false

    fun expectOne() {
        expected = true
    }

    fun consumeIfExpected(): Boolean {
        val wasExpected = expected
        expected = false
        return wasExpected
    }
}
