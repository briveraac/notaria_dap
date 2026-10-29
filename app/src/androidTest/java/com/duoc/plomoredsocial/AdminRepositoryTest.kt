package com.duoc.plomoredsocial

import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.duoc.plomoredsocial.model.*
import com.duoc.plomoredsocial.repository.*
import com.duoc.plomoredsocial.viewmodel.LoginViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*

// Solo fixtures en memoria. Ninguna cuenta de prueba se agrega a la base del emulador.
class AdminRepositoryTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private lateinit var db: MygosDatabase
    private lateinit var auth: RoomAuthRepository
    private lateinit var session: AdminTestSession
    private lateinit var admin: AdminRepository

    @Before fun prepare() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(context, MygosDatabase::class.java).build()
        auth = RoomAuthRepository(db, context.assets)
        assertTrue(requireNotNull(auth.getUserById(PRIMARY_DEMO_ID)).isAdmin)
        session = AdminTestSession(PRIMARY_DEMO_ID)
        admin = AdminRepository(db, session)
    }
    @After fun close() { db.close() }
    private suspend fun user(email: String = "normal@example.com") = auth.register("Normal", email, "Contraseña de prueba larga")
    private fun event(id: String, owner: String) = ActivityEntity(id, "Actividad $id", "Maipú", "Plaza",
        "31-12-2099, 18:00", null, null, owner)
    private suspend fun denied(operation: suspend () -> Unit) {
        try { operation(); fail("La operación debía rechazar permisos") } catch (_: SecurityException) { }
    }

    @Test fun rolesAndDemoEditsSurviveFreshRepositoriesWithoutEmailBasedPromotion() = runBlocking {
        val normal = user()
        assertEquals(UserRole.USER, normal.role)
        admin.updateUser(PRIMARY_DEMO_ID, "Administradora editada", "admin.changed@example.com")
        admin.updateUser(normal.id, "Normal", "demo@mygos.cl")
        val fresh = RoomAuthRepository(db, context.assets)
        val principal = requireNotNull(fresh.getUserById(PRIMARY_DEMO_ID))
        assertTrue(principal.isAdmin)
        assertEquals("Administradora editada", principal.name)
        assertEquals("admin.changed@example.com", principal.email)
        val emailMatch = requireNotNull(fresh.getUserById(normal.id))
        assertFalse(emailMatch.isAdmin)
        assertEquals(UserRole.USER, emailMatch.role)
        assertEquals(normal.id, fresh.authenticate("demo@mygos.cl", "Contraseña de prueba larga")?.id)
        assertTrue(admin.observePanel().first().users.none { it.id != PRIMARY_DEMO_ID && it.role == "ADMIN" })
    }

    @Test fun editValidatesNormalizesAndRejectsDuplicateEmails() = runBlocking {
        val a = user()
        val b = user("second@example.com")
        admin.updateUser(a.id, " Nombre nuevo ", " NUEVO@Example.com ")
        val changed = requireNotNull(db.userDao().findById(a.id))
        assertEquals("Nombre nuevo", changed.name)
        assertEquals("NUEVO@Example.com", changed.email)
        assertEquals("nuevo@example.com", changed.normalizedEmail)
        try { admin.updateUser(b.id, "Otra", " nuevo@EXAMPLE.com "); fail("Duplicado aceptado") }
        catch (_: DuplicateEmailException) { }
        for ((name, email) in listOf("" to "valid@example.com", "Válido" to "no-es-correo")) {
            try { admin.updateUser(a.id, name, email); fail("Dato inválido aceptado") }
            catch (_: AdminOperationException) { }
        }
        assertEquals("second@example.com", db.userDao().findById(b.id)?.email)
    }

    @Test fun resetUsesSha256FreshSaltAndNewPasswordAuthenticates() = runBlocking {
        val a = user()
        val before = requireNotNull(db.userDao().findById(a.id))
        admin.resetPassword(a.id, "Nueva contraseña muy larga")
        val after = requireNotNull(db.userDao().findById(a.id))
        assertEquals(PasswordDeriver.ALGORITHM, after.algorithm)
        assertEquals(600_000, after.iterations)
        assertTrue(before.salt != after.salt)
        assertEquals(a.id, auth.authenticate(a.email, "Nueva contraseña muy larga")?.id)
        assertNull(auth.authenticate(a.email, "Contraseña de prueba larga"))
        admin.resetPassword(a.id, "Nueva contraseña muy larga")
        assertTrue(after.salt != db.userDao().findById(a.id)?.salt)
        try { admin.resetPassword(a.id, "corta"); fail("Contraseña débil aceptada") }
        catch (_: AdminOperationException) { }
    }

    @Test fun everyReadAndMutationRejectsNormalForgedMissingAndLoggedOutSessions() = runBlocking {
        val normal = user()
        db.activityDao().create(event("event", normal.id))
        // Ni siquiera un rol adulterado en una cuenta de otro origen basta para administrar.
        db.openHelper.writableDatabase.execSQL("UPDATE users SET role='ADMIN' WHERE id=?", arrayOf(normal.id))
        for (actor in listOf(normal.id, "missing", null)) {
            session.id = actor
            denied { admin.observePanel().first() }
            denied { admin.updateUser(normal.id, "Intruso", "intruso@example.com") }
            denied { admin.resetPassword(normal.id, "Una contraseña no autorizada") }
            denied { admin.deleteUser(normal.id) }
            denied { admin.deleteActivity("event") }
        }
        assertNotNull(db.userDao().findById(normal.id))
        assertNotNull(db.activityDao().find("event"))
    }

    @Test fun organizersAreBlockedUntilActivitiesAreIndividuallyResolvedAndLastAdminIsProtected() = runBlocking {
        val target = user()
        db.activityDao().create(event("first", target.id))
        db.activityDao().create(event("second", target.id))
        db.activityDao().join(ActivityMember("first", PRIMARY_DEMO_ID))
        try { admin.deleteUser(target.id); fail("Organizador eliminado") }
        catch (error: AdminOperationException) { assertTrue(error.message.orEmpty().contains("2 actividad")) }
        assertEquals(2, db.activityDao().countOrganized(target.id))
        admin.deleteActivity("first")
        assertNull(db.activityDao().find("first"))
        assertTrue(db.activityDao().observeMembers().first().none { it.activityId == "first" })
        try { admin.deleteUser(target.id); fail("Organizador eliminado") }
        catch (error: AdminOperationException) { assertTrue(error.message.orEmpty().contains("1 actividad")) }
        admin.deleteActivity("second")
        admin.deleteUser(target.id)
        assertNull(db.userDao().findById(target.id))
        try { admin.deleteUser(PRIMARY_DEMO_ID); fail("Último administrador eliminado") }
        catch (error: AdminOperationException) { assertTrue(error.message.orEmpty().contains("último administrador")) }
        assertNotNull(db.userDao().findById(PRIMARY_DEMO_ID))
    }

    @Test fun deletingMemberClearsLiveAndRestoredSessionsAndPreservesOtherData() = runBlocking {
        val target = user()
        val victimSession = AdminTestSession(target.id)
        val stores = ViewModelStore()
        lateinit var login: LoginViewModel
        db.activityDao().create(event("kept", PRIMARY_DEMO_ID))
        db.activityDao().join(ActivityMember("kept", target.id))
        instrumentation.runOnMainSync { login = LoginViewModel(auth, victimSession); stores.put("login", login) }
        try {
            withTimeout(10000) { while (login.uiState.restoringSession) delay(20) }
            admin.updateUser(target.id, "Nombre sincronizado", target.email)
            withTimeout(10000) { while (login.uiState.user?.name != "Nombre sincronizado") delay(20) }
            admin.deleteUser(target.id)
            withTimeout(10000) { while (login.uiState.user != null || victimSession.id != null) delay(20) }
            assertNull(auth.getUserById(target.id))
            assertNotNull(db.activityDao().find("kept"))
            assertTrue(db.activityDao().observeMembers().first().isEmpty())
            // Una pantalla antigua no puede volver a crear asociaciones de la cuenta borrada.
            assertFalse(db.activityDao().joinForAccount(ActivityMember("kept", target.id)))
            try { db.activityDao().createForAccount(event("stale", target.id)); fail("Cuenta borrada creó actividad") }
            catch (_: IllegalStateException) { }
            assertNull(db.activityDao().find("stale"))
            victimSession.id = target.id
            lateinit var restored: LoginViewModel
            instrumentation.runOnMainSync { restored = LoginViewModel(auth, victimSession); stores.put("restored", restored) }
            withTimeout(10000) { while (restored.uiState.restoringSession) delay(20) }
            assertNull(restored.uiState.user)
            assertNull(victimSession.id)
        } finally { instrumentation.runOnMainSync { stores.clear() } }
    }

    @Test fun userDeletionRollsBackMembershipCleanupOnFailure() = runBlocking {
        val target = user()
        db.activityDao().create(event("kept", PRIMARY_DEMO_ID))
        db.activityDao().join(ActivityMember("kept", target.id))
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER reject_user_delete BEFORE DELETE ON users BEGIN SELECT RAISE(ABORT, 'test rollback'); END")
        try { admin.deleteUser(target.id); fail("Se esperaba rollback") } catch (_: android.database.sqlite.SQLiteException) { }
        assertNotNull(db.userDao().findById(target.id))
        assertEquals(listOf(ActivityMember("kept", target.id)), db.activityDao().observeMembers().first())
    }
    @Test fun concurrentDeletionCannotLeaveNewActivitiesOrMembershipsOfRemovedAccounts() = runBlocking {
        repeat(4) { index ->
            val target = user("race-create-$index@example.com")
            val activity = event("race-$index", target.id)
            coroutineScope {
                val creation = async(Dispatchers.IO) { runCatching { db.activityDao().createForAccount(activity) } }
                val deletion = async(Dispatchers.IO) { runCatching { admin.deleteUser(target.id) } }
                creation.await(); deletion.await()
            }
            // Solo son válidos dos resultados: cuenta con actividad, o ambas ausentes.
            val accountExists = db.userDao().findById(target.id) != null
            assertEquals(accountExists, db.activityDao().find(activity.id) != null)
        }
        db.activityDao().create(event("shared", PRIMARY_DEMO_ID))
        repeat(4) { index ->
            val target = user("race-join-$index@example.com")
            coroutineScope {
                val joining = async(Dispatchers.IO) { db.activityDao().joinForAccount(ActivityMember("shared", target.id)) }
                val deletion = async(Dispatchers.IO) { admin.deleteUser(target.id) }
                joining.await(); deletion.await()
            }
            assertNull(db.userDao().findById(target.id))
            assertTrue(db.activityDao().observeMembers().first().none { it.userId == target.id })
        }
    }
}

internal class AdminTestSession(@Volatile var id: String?) : SessionStore {
    override suspend fun readUserId() = id
    override suspend fun saveUserId(userId: String) { id = userId }
    override suspend fun clear() { id = null }
}
