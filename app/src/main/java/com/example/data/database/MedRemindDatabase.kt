package com.example.data.database

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// -------------------------------------------------------------------------
// 1. Database Entities
// -------------------------------------------------------------------------

@Entity(tableName = "medicines")
data class MedicineEntity(
    @PrimaryKey val id: String,
    val name: String,
    val dosage: String,
    val form: String,
    val pillCount: Int,
    val refillThreshold: Int,
    val isActive: Boolean
)

@Entity(tableName = "schedules")
data class ScheduleEntity(
    @PrimaryKey val id: String,
    val medicineId: String,
    val scheduledTime: String // e.g. "08:00 AM"
)

@Entity(tableName = "dose_logs")
data class DoseLogEntity(
    @PrimaryKey val id: String,
    val medicineId: String,
    val scheduledAt: String, // e.g. "2026-05-24 08:00 AM"
    val takenAt: String?,    // e.g. "2026-05-24 08:15 AM" or null
    val status: String       // "TAKEN", "MISSED", "SKIPPED", "SCHEDULED"
)

// -------------------------------------------------------------------------
// 2. Helper & Result Data Classes for Queries
// -------------------------------------------------------------------------

data class DailyAdherence(
    val date: String,
    val totalCount: Int,
    val takenCount: Int
)

data class DoseLogWithMedicine(
    val id: String,
    val medicineId: String,
    val scheduledAt: String,
    val takenAt: String?,
    val status: String,
    val medicineName: String,
    val medicineDosage: String
)

// -------------------------------------------------------------------------
// 3. DAOs (Data Access Objects)
// -------------------------------------------------------------------------

@Dao
interface MedicineDao {
    @Query("SELECT * FROM medicines ORDER BY name ASC")
    fun getAllMedicinesFlow(): Flow<List<MedicineEntity>>

    @Query("SELECT * FROM medicines ORDER BY name ASC")
    suspend fun getAllMedicines(): List<MedicineEntity>

    @Query("SELECT * FROM medicines WHERE pillCount <= refillThreshold")
    fun getLowStockMedicinesFlow(): Flow<List<MedicineEntity>>

    @Query("SELECT COUNT(*) FROM medicines WHERE pillCount <= refillThreshold")
    fun getLowStockCountFlow(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedicine(medicine: MedicineEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedicines(medicines: List<MedicineEntity>)

    @Update
    suspend fun updateMedicine(medicine: MedicineEntity)

    @Query("UPDATE medicines SET pillCount = :newCount WHERE id = :medId")
    suspend fun updatePillCount(medId: String, newCount: Int)

    @Query("DELETE FROM medicines WHERE id = :id")
    suspend fun deleteMedicine(id: String)
}

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedules")
    fun getAllSchedulesFlow(): Flow<List<ScheduleEntity>>

    @Query("SELECT * FROM schedules")
    suspend fun getAllSchedules(): List<ScheduleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: ScheduleEntity)

    @Query("DELETE FROM schedules WHERE id = :id")
    suspend fun deleteSchedule(id: String)
}

@Dao
interface DoseLogDao {
    @Query("SELECT * FROM dose_logs ORDER BY scheduledAt ASC")
    fun getAllLogsFlow(): Flow<List<DoseLogEntity>>

    // Query for today's logs using a matching prefix. E.g. "2026-05-24"
    @Query("""
        SELECT 
            d.id AS id,
            d.medicineId AS medicineId,
            d.scheduledAt AS scheduledAt,
            d.takenAt AS takenAt,
            d.status AS status,
            m.name AS medicineName,
            m.dosage AS medicineDosage
        FROM dose_logs d
        INNER JOIN medicines m ON d.medicineId = m.id
        WHERE d.scheduledAt LIKE :datePrefix || '%'
        ORDER BY d.scheduledAt ASC
    """)
    fun getLogsForDate(datePrefix: String): Flow<List<DoseLogWithMedicine>>

    // 30-Day adherence grouped by date. Filters based on trailing 30 days. E.g. "2026-04-24"
    @Query("""
        SELECT 
            substr(scheduledAt, 1, 10) AS date,
            COUNT(*) AS totalCount,
            SUM(CASE WHEN status = 'TAKEN' THEN 1 ELSE 0 END) AS takenCount
        FROM dose_logs
        WHERE substr(scheduledAt, 1, 10) >= :dateThirtyDaysAgo
        GROUP BY substr(scheduledAt, 1, 10)
        ORDER BY date ASC
    """)
    fun getAdherenceLast30Days(dateThirtyDaysAgo: String): Flow<List<DailyAdherence>>

    @Query("""
        SELECT 
            d.id AS id,
            d.medicineId AS medicineId,
            d.scheduledAt AS scheduledAt,
            d.takenAt AS takenAt,
            d.status AS status,
            m.name AS medicineName,
            m.dosage AS medicineDosage
        FROM dose_logs d
        INNER JOIN medicines m ON d.medicineId = m.id
        WHERE substr(d.scheduledAt, 1, 10) >= :dateThirtyDaysAgo
        ORDER BY d.scheduledAt DESC
    """)
    suspend fun getLogsWithMedicineLast30Days(dateThirtyDaysAgo: String): List<DoseLogWithMedicine>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: DoseLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogs(logs: List<DoseLogEntity>)

    @Query("UPDATE dose_logs SET status = :status, takenAt = :takenAt WHERE id = :logId")
    suspend fun updateLogStatus(logId: String, status: String, takenAt: String?)
}

@Entity(tableName = "family_members")
data class FamilyMemberEntity(
    @PrimaryKey val id: String,
    val caregiverId: String,
    val memberId: String,
    val relationship: String,
    val permission: String, // VIEW or MANAGE
    val notifyMissed: Boolean,
    val name: String,
    val adherence: Float = 0.85f,
    val hasMissedDose: Boolean = false
)

@Entity(tableName = "family_invitations")
data class FamilyInvitationEntity(
    @PrimaryKey val code: String, // 6-digit code
    val relationshipProposed: String,
    val permissionProposed: String,
    val notifyMissed: Boolean,
    val name: String,
    val isUsed: Boolean = false
)

@Dao
interface FamilyDao {
    @Query("SELECT * FROM family_members ORDER BY name ASC")
    fun getAllFamilyMembersFlow(): Flow<List<FamilyMemberEntity>>

    @Query("SELECT * FROM family_members ORDER BY name ASC")
    suspend fun getAllFamilyMembers(): List<FamilyMemberEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFamilyMember(member: FamilyMemberEntity)

    @Update
    suspend fun updateFamilyMember(member: FamilyMemberEntity)

    @Query("DELETE FROM family_members WHERE id = :id")
    suspend fun deleteFamilyMember(id: String)

    @Query("SELECT * FROM family_invitations")
    fun getAllInvitationsFlow(): Flow<List<FamilyInvitationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvitation(invitation: FamilyInvitationEntity)

    @Query("SELECT * FROM family_invitations WHERE code = :code LIMIT 1")
    suspend fun getInvitationByCode(code: String): FamilyInvitationEntity?

    @Query("DELETE FROM family_invitations WHERE code = :code")
    suspend fun deleteInvitationByCode(code: String)
}

// -------------------------------------------------------------------------
// 4. Room Database
// -------------------------------------------------------------------------

@Database(
    entities = [MedicineEntity::class, ScheduleEntity::class, DoseLogEntity::class, FamilyMemberEntity::class, FamilyInvitationEntity::class],
    version = 3,
    exportSchema = false
)
abstract class MedRemindDatabase : RoomDatabase() {
    abstract fun medicineDao(): MedicineDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun doseLogDao(): DoseLogDao
    abstract fun familyDao(): FamilyDao

    companion object {
        @Volatile
        private var INSTANCE: MedRemindDatabase? = null

        fun getDatabase(context: Context): MedRemindDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MedRemindDatabase::class.java,
                    "med_remind_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
