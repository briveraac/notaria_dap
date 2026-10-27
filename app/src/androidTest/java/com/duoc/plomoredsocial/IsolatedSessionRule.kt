package com.duoc.plomoredsocial

import androidx.test.platform.app.InstrumentationRegistry
import com.duoc.plomoredsocial.repository.LocalSessionStore
import kotlinx.coroutines.runBlocking
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

// Cada prueba de interfaz parte sin sesión y devuelve la sesión previa al terminar.
class IsolatedSessionRule : TestRule {
    override fun apply(base: Statement, description: Description) = object : Statement() {
        override fun evaluate() {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val store = LocalSessionStore(context.noBackupFilesDir)
            val previous = runBlocking { store.readUserId().also { store.clear() } }
            try {
                base.evaluate()
            } finally {
                runBlocking {
                    if (previous == null) store.clear() else store.saveUserId(previous)
                }
            }
        }
    }
}
