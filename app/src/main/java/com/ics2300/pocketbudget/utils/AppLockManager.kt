package com.ics2300.pocketbudget.utils

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

object AppLockManager {

    private val isCurrentSessionUnlocked = AtomicBoolean(false)
    private val lastBackgroundTime = AtomicLong(0L)

    fun unlockSession() {
        isCurrentSessionUnlocked.set(true)
        lastBackgroundTime.set(0L)
    }

    fun lockSession() {
        isCurrentSessionUnlocked.set(false)
        lastBackgroundTime.set(System.currentTimeMillis())
    }

    fun markAppBackgrounded() {
        isCurrentSessionUnlocked.set(false)
        lastBackgroundTime.set(System.currentTimeMillis())
    }

    fun shouldLock(): Boolean {
        return !isCurrentSessionUnlocked.get()
    }

    fun shouldLockAfterTimeout(timeoutMs: Long): Boolean {
        if (!isCurrentSessionUnlocked.get()) return true

        val backgroundedAt = lastBackgroundTime.get()
        if (backgroundedAt <= 0L) return false

        val elapsed = System.currentTimeMillis() - backgroundedAt
        return elapsed >= timeoutMs
    }

    fun isSessionUnlocked(): Boolean {
        return isCurrentSessionUnlocked.get()
    }
}