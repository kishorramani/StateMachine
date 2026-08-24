package com.kishorramani.statemachine.data.local

import kotlinx.coroutines.flow.Flow

interface ApplicationDao {
    fun getApplicationFlow(id: String): Flow<ApplicationEntity?>
    suspend fun getApplication(id: String): ApplicationEntity?
    suspend fun insertOrUpdateApplication(application: ApplicationEntity)
    fun getAuditLogsFlow(appId: String): Flow<List<AuditLogEntity>>
    suspend fun insertAuditLog(log: AuditLogEntity)
    suspend fun clearAuditLogs(appId: String)
}
