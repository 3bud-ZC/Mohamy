package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ClientDao {
    @Query("SELECT * FROM clients ORDER BY name ASC")
    fun getAllClients(): Flow<List<Client>>

    @Query("SELECT * FROM clients WHERE id = :id LIMIT 1")
    suspend fun getClientById(id: Int): Client?

    @Query("SELECT * FROM clients WHERE name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%'")
    fun searchClients(query: String): Flow<List<Client>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClient(client: Client): Long

    @Update
    suspend fun updateClient(client: Client)

    @Delete
    suspend fun deleteClient(client: Client)
}

@Dao
interface CaseDao {
    @Query("SELECT * FROM cases WHERE isArchived = 0 ORDER BY createdDate DESC")
    fun getAllActiveCases(): Flow<List<LegalCase>>

    @Query("SELECT * FROM cases WHERE isArchived = 1 ORDER BY createdDate DESC")
    fun getAllArchivedCases(): Flow<List<LegalCase>>

    @Query("SELECT * FROM cases WHERE id = :id LIMIT 1")
    suspend fun getCaseById(id: Int): LegalCase?

    @Query("SELECT * FROM cases WHERE clientId = :clientId ORDER BY createdDate DESC")
    fun getCasesForClient(clientId: Int): Flow<List<LegalCase>>

    @Query("SELECT * FROM cases WHERE title LIKE '%' || :query || '%' OR caseNumber LIKE '%' || :query || '%'")
    fun searchCases(query: String): Flow<List<LegalCase>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCase(legalCase: LegalCase): Long

    @Update
    suspend fun updateCase(legalCase: LegalCase)

    @Delete
    suspend fun deleteCase(legalCase: LegalCase)
}

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY date ASC, time ASC")
    fun getAllSessions(): Flow<List<CaseSession>>

    @Query("SELECT * FROM sessions WHERE caseId = :caseId ORDER BY date ASC")
    fun getSessionsForCase(caseId: Int): Flow<List<CaseSession>>

    @Query("SELECT * FROM sessions WHERE date = :date ORDER BY time ASC")
    fun getSessionsForDate(date: String): Flow<List<CaseSession>>

    @Query("SELECT * FROM sessions WHERE id = :id LIMIT 1")
    suspend fun getSessionById(id: Int): CaseSession?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: CaseSession): Long

    @Update
    suspend fun updateSession(session: CaseSession)

    @Delete
    suspend fun deleteSession(session: CaseSession)
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY dueDate ASC")
    fun getAllTasks(): Flow<List<LegalTask>>

    @Query("SELECT * FROM tasks WHERE caseId = :caseId ORDER BY dueDate ASC")
    fun getTasksForCase(caseId: Int): Flow<List<LegalTask>>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: Int): LegalTask?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: LegalTask): Long

    @Update
    suspend fun updateTask(task: LegalTask)

    @Delete
    suspend fun deleteTask(task: LegalTask)
}

@Dao
interface FileDao {
    @Query("SELECT * FROM files ORDER BY uploadDate DESC")
    fun getAllFiles(): Flow<List<CaseFile>>

    @Query("SELECT * FROM files WHERE caseId = :caseId ORDER BY uploadDate DESC")
    fun getFilesForCase(caseId: Int): Flow<List<CaseFile>>

    @Query("SELECT * FROM files WHERE clientId = :clientId ORDER BY uploadDate DESC")
    fun getFilesForClient(clientId: Int): Flow<List<CaseFile>>

    @Query("SELECT * FROM files WHERE id = :id LIMIT 1")
    suspend fun getFileById(id: Int): CaseFile?

    @Query("SELECT * FROM files WHERE normalizedSearchIndex LIKE '%' || :query || '%' OR fileName LIKE '%' || :query || '%' OR extractedText LIKE '%' || :query || '%'")
    fun searchFilesText(query: String): Flow<List<CaseFile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: CaseFile): Long

    @Delete
    suspend fun deleteFile(file: CaseFile)
}

@Dao
interface ClientInteractionDao {
    @Query("SELECT * FROM client_interactions ORDER BY createdAt DESC")
    fun getAll(): Flow<List<ClientInteraction>>

    @Query("SELECT * FROM client_interactions WHERE clientId = :clientId ORDER BY createdAt DESC")
    fun getForClient(clientId: Int): Flow<List<ClientInteraction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(interaction: ClientInteraction): Long

    @Delete
    suspend fun delete(interaction: ClientInteraction)
}

@Dao
interface TemplateDao {
    @Query("SELECT * FROM templates ORDER BY category ASC, title ASC")
    fun getAllTemplates(): Flow<List<LegalTemplate>>

    @Query("SELECT * FROM templates WHERE id = :id LIMIT 1")
    suspend fun getTemplateById(id: Int): LegalTemplate?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: LegalTemplate): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplates(templates: List<LegalTemplate>)
}

@Dao
interface GeneratedDocDao {
    @Query("SELECT * FROM generated_documents ORDER BY dateCreated DESC")
    fun getAllGeneratedDocuments(): Flow<List<GeneratedDocument>>

    @Query("SELECT * FROM generated_documents WHERE caseId = :caseId ORDER BY dateCreated DESC")
    fun getDocsForCase(caseId: Int): Flow<List<GeneratedDocument>>

    @Query("SELECT * FROM generated_documents WHERE id = :id LIMIT 1")
    suspend fun getDocById(id: Int): GeneratedDocument?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGeneratedDocument(doc: GeneratedDocument): Long

    @Delete
    suspend fun deleteGeneratedDocument(doc: GeneratedDocument)
}

@Dao
interface FeeDao {
    @Query("SELECT * FROM fee_records ORDER BY createdAt DESC")
    fun getAllFeeRecords(): Flow<List<FeeRecord>>

    @Query("SELECT * FROM fee_records WHERE clientId = :clientId ORDER BY createdAt DESC")
    fun getForClient(clientId: Int): Flow<List<FeeRecord>>

    @Query("SELECT * FROM fee_records WHERE caseId = :caseId ORDER BY createdAt DESC")
    fun getForCase(caseId: Int): Flow<List<FeeRecord>>

    @Query("SELECT * FROM fee_records WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): FeeRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: FeeRecord): Long

    @Update
    suspend fun update(record: FeeRecord)

    @Delete
    suspend fun delete(record: FeeRecord)
}

@Dao
interface CustomCaseCategoryDao {
    @Query("SELECT * FROM custom_case_categories ORDER BY name COLLATE NOCASE ASC")
    fun getAll(): Flow<List<CustomCaseCategory>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(category: CustomCaseCategory): Long

    @Query("DELETE FROM custom_case_categories WHERE id = :id")
    suspend fun deleteById(id: Int)
}

@Dao
interface LicenseDao {
    @Query("SELECT * FROM license_cache WHERE id = 1 LIMIT 1")
    fun getLicenseFlow(): Flow<LicenseCache?>

    @Query("SELECT * FROM license_cache WHERE id = 1 LIMIT 1")
    suspend fun getLicenseDirect(): LicenseCache?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLicense(license: LicenseCache)

    @Query("DELETE FROM license_cache WHERE id = 1")
    suspend fun deleteLicense()
}
