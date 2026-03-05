package com.ics2300.pocketbudget.utils

object AppLockManager {
    var isCurrentSessionUnlocked = false
        private set

    fun unlockSession() {
        isCurrentSessionUnlocked = true
    }

    fun lockSession() {
        isCurrentSessionUnlocked = false
    }
    
    fun shouldLock(): Boolean {
        return !isCurrentSessionUnlocked
    }
}
