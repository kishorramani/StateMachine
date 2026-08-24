package com.kishorramani.statemachine.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * SQLite Database implementation matching Room specs.
 * Guarantees zero crashes from annotation processors while persisting state across process restarts.
 */
class AppDatabase private constructor(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    private val appStateFlows = ConcurrentHashMap<String, MutableStateFlow<ApplicationEntity?>>()
    private val auditLogsFlows = ConcurrentHashMap<String, MutableStateFlow<List<AuditLogEntity>>>()

    private val dao = object : ApplicationDao {
        override fun getApplicationFlow(id: String): Flow<ApplicationEntity?> {
            val stateFlow = appStateFlows.getOrPut(id) {
                MutableStateFlow(queryApplicationSync(id))
            }
            return stateFlow.asStateFlow()
        }

        override suspend fun getApplication(id: String): ApplicationEntity? {
            return queryApplicationSync(id)
        }

        override suspend fun insertOrUpdateApplication(application: ApplicationEntity) {
            val db = writableDatabase
            val cv = ContentValues().apply {
                put("id", application.id)
                put("applicantName", application.applicantName)
                put("amount", application.amount)
                put("state", application.state)
                put("paymentId", application.paymentId)
                put("idempotencyKey", application.idempotencyKey)
                put("failureReason", application.failureReason)
                put("retryCount", application.retryCount)
                put("updatedAt", application.updatedAt)
            }
            db.insertWithOnConflict("applications", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            appStateFlows.getOrPut(application.id) { MutableStateFlow(null) }.value = application
        }

        override fun getAuditLogsFlow(appId: String): Flow<List<AuditLogEntity>> {
            val stateFlow = auditLogsFlows.getOrPut(appId) {
                MutableStateFlow(queryAuditLogsSync(appId))
            }
            return stateFlow.asStateFlow()
        }

        override suspend fun insertAuditLog(log: AuditLogEntity) {
            val db = writableDatabase
            val cv = ContentValues().apply {
                put("applicationId", log.applicationId)
                put("fromState", log.fromState)
                put("event", log.event)
                put("toState", log.toState)
                put("isValid", if (log.isValid) 1 else 0)
                put("details", log.details)
                put("timestamp", log.timestamp)
            }
            db.insert("audit_logs", null, cv)
            auditLogsFlows.getOrPut(log.applicationId) { MutableStateFlow(emptyList()) }.value = queryAuditLogsSync(log.applicationId)
        }

        override suspend fun clearAuditLogs(appId: String) {
            val db = writableDatabase
            db.delete("audit_logs", "applicationId = ?", arrayOf(appId))
            auditLogsFlows.getOrPut(appId) { MutableStateFlow(emptyList()) }.value = emptyList()
        }
    }

    fun applicationDao(): ApplicationDao = dao

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS applications (
                id TEXT PRIMARY KEY,
                applicantName TEXT NOT NULL,
                amount REAL NOT NULL,
                state TEXT NOT NULL,
                paymentId TEXT,
                idempotencyKey TEXT,
                failureReason TEXT,
                retryCount INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS audit_logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                applicationId TEXT NOT NULL,
                fromState TEXT NOT NULL,
                event TEXT NOT NULL,
                toState TEXT NOT NULL,
                isValid INTEGER NOT NULL,
                details TEXT NOT NULL,
                timestamp INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS applications")
        db.execSQL("DROP TABLE IF EXISTS audit_logs")
        onCreate(db)
    }

    private fun queryApplicationSync(id: String): ApplicationEntity? {
        val db = readableDatabase
        val cursor = db.query("applications", null, "id = ?", arrayOf(id), null, null, null)
        return cursor.use { c ->
            if (c.moveToFirst()) {
                ApplicationEntity(
                    id = c.getString(c.getColumnIndexOrThrow("id")),
                    applicantName = c.getString(c.getColumnIndexOrThrow("applicantName")),
                    amount = c.getDouble(c.getColumnIndexOrThrow("amount")),
                    state = c.getString(c.getColumnIndexOrThrow("state")),
                    paymentId = c.getString(c.getColumnIndexOrThrow("paymentId")),
                    idempotencyKey = c.getString(c.getColumnIndexOrThrow("idempotencyKey")),
                    failureReason = c.getString(c.getColumnIndexOrThrow("failureReason")),
                    retryCount = c.getInt(c.getColumnIndexOrThrow("retryCount")),
                    updatedAt = c.getLong(c.getColumnIndexOrThrow("updatedAt"))
                )
            } else null
        }
    }

    private fun queryAuditLogsSync(appId: String): List<AuditLogEntity> {
        val db = readableDatabase
        val cursor = db.query("audit_logs", null, "applicationId = ?", arrayOf(appId), null, null, "timestamp DESC")
        val list = mutableListOf<AuditLogEntity>()
        cursor.use { c ->
            while (c.moveToNext()) {
                list.add(
                    AuditLogEntity(
                        id = c.getLong(c.getColumnIndexOrThrow("id")),
                        applicationId = c.getString(c.getColumnIndexOrThrow("applicationId")),
                        fromState = c.getString(c.getColumnIndexOrThrow("fromState")),
                        event = c.getString(c.getColumnIndexOrThrow("event")),
                        toState = c.getString(c.getColumnIndexOrThrow("toState")),
                        isValid = c.getInt(c.getColumnIndexOrThrow("isValid")) == 1,
                        details = c.getString(c.getColumnIndexOrThrow("details")),
                        timestamp = c.getLong(c.getColumnIndexOrThrow("timestamp"))
                    )
                )
            }
        }
        return list
    }

    companion object {
        private const val DATABASE_NAME = "financial_app.db"
        private const val DATABASE_VERSION = 1

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = AppDatabase(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
