package com.example.data.repository

import com.example.data.database.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class MedRemindRepository(private val database: MedRemindDatabase) {

    private val medicineDao = database.medicineDao()
    private val scheduleDao = database.scheduleDao()
    private val doseLogDao = database.doseLogDao()
    private val familyDao = database.familyDao()

    val allMedicinesFlow: Flow<List<MedicineEntity>> = medicineDao.getAllMedicinesFlow()
    val lowStockMedicinesFlow: Flow<List<MedicineEntity>> = medicineDao.getLowStockMedicinesFlow()
    val lowStockCountFlow: Flow<Int> = medicineDao.getLowStockCountFlow()
    val allFamilyMembersFlow: Flow<List<FamilyMemberEntity>> = familyDao.getAllFamilyMembersFlow()
    val allInvitationsFlow: Flow<List<FamilyInvitationEntity>> = familyDao.getAllInvitationsFlow()

    fun getLogsForDate(datePrefix: String): Flow<List<DoseLogWithMedicine>> {
        return doseLogDao.getLogsForDate(datePrefix)
    }

    fun getAdherenceLast30Days(dateThirtyDaysAgo: String): Flow<List<DailyAdherence>> {
        return doseLogDao.getAdherenceLast30Days(dateThirtyDaysAgo)
    }

    suspend fun getLogsWithMedicineLast30Days(dateThirtyDaysAgo: String): List<DoseLogWithMedicine> = withContext(Dispatchers.IO) {
        doseLogDao.getLogsWithMedicineLast30Days(dateThirtyDaysAgo)
    }

    suspend fun getAllFamilyMembers(): List<FamilyMemberEntity> = withContext(Dispatchers.IO) {
        familyDao.getAllFamilyMembers()
    }

    suspend fun insertFamilyMember(member: FamilyMemberEntity) = withContext(Dispatchers.IO) {
        familyDao.insertFamilyMember(member)
    }

    suspend fun updateFamilyMember(member: FamilyMemberEntity) = withContext(Dispatchers.IO) {
        familyDao.updateFamilyMember(member)
    }

    suspend fun deleteFamilyMember(id: String) = withContext(Dispatchers.IO) {
        familyDao.deleteFamilyMember(id)
    }

    suspend fun insertInvitation(invitation: FamilyInvitationEntity) = withContext(Dispatchers.IO) {
        familyDao.insertInvitation(invitation)
    }

    suspend fun getInvitationByCode(code: String): FamilyInvitationEntity? = withContext(Dispatchers.IO) {
        familyDao.getInvitationByCode(code)
    }

    suspend fun deleteInvitationByCode(code: String) = withContext(Dispatchers.IO) {
        familyDao.deleteInvitationByCode(code)
    }

    suspend fun insertMedicine(medicine: MedicineEntity) = withContext(Dispatchers.IO) {
        medicineDao.insertMedicine(medicine)
    }

    suspend fun updatePillCount(medId: String, newCount: Int) = withContext(Dispatchers.IO) {
        medicineDao.updatePillCount(medId, newCount)
    }

    suspend fun updateMedicineActiveStatus(medId: String, isActive: Boolean) = withContext(Dispatchers.IO) {
        val all = medicineDao.getAllMedicines()
        val match = all.firstOrNull { it.id == medId }
        if (match != null) {
            medicineDao.insertMedicine(match.copy(isActive = isActive))
        }
    }

    suspend fun updateLogStatus(logId: String, status: String, takenAt: String?) = withContext(Dispatchers.IO) {
        doseLogDao.updateLogStatus(logId, status, takenAt)
    }

    suspend fun insertLog(log: DoseLogEntity) = withContext(Dispatchers.IO) {
        doseLogDao.insertLog(log)
    }

    suspend fun insertSchedule(schedule: ScheduleEntity) = withContext(Dispatchers.IO) {
        scheduleDao.insertSchedule(schedule)
    }

    suspend fun preSeedDatabaseIfEmpty() = withContext(Dispatchers.IO) {
        if (medicineDao.getAllMedicines().isEmpty()) {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val todayStr = sdf.format(Date())

            val initialMeds = listOf(
                MedicineEntity("med_1", "Lisinopril", "10mg", "Tablet", 6, 10, true),
                MedicineEntity("med_2", "Atorvastatin", "20mg", "Tablet", 35, 15, true),
                MedicineEntity("med_3", "Metformin", "500mg", "Tablet", 8, 12, true),
                MedicineEntity("med_4", "Amlodipine", "5mg", "Tablet", 45, 10, true)
            )
            medicineDao.insertMedicines(initialMeds)

            val initialFamily = listOf(
                FamilyMemberEntity("fam_1", "caregiver_local", "member_sarah", "Daughter", "VIEW", true, "Sarah Connor", 0.95f, false),
                FamilyMemberEntity("fam_2", "caregiver_local", "member_john", "Father", "MANAGE", true, "John Connor", 0.68f, true)
            )
            for (fam in initialFamily) {
                familyDao.insertFamilyMember(fam)
            }

            val initialLogs = listOf(
                DoseLogEntity("log_1", "med_1", "$todayStr 08:00 AM", "$todayStr 08:05 AM", "TAKEN"),
                DoseLogEntity("log_2", "med_3", "$todayStr 08:00 AM", "$todayStr 08:10 AM", "TAKEN"),
                DoseLogEntity("log_3", "med_2", "$todayStr 01:00 PM", "$todayStr 01:02 PM", "TAKEN"),
                DoseLogEntity("log_4", "med_1", "$todayStr 08:00 PM", null, "SCHEDULED"),
                DoseLogEntity("log_5", "med_4", "$todayStr 09:00 PM", null, "SCHEDULED")
            )
            doseLogDao.insertLogs(initialLogs)

            // Seed initial Schedule Entity records to match initialLogs times
            val initialSchedules = listOf(
                ScheduleEntity("sched_1_am", "med_1", "08:00 AM"),
                ScheduleEntity("sched_1_pm", "med_1", "08:00 PM"),
                ScheduleEntity("sched_2", "med_2", "01:00 PM"),
                ScheduleEntity("sched_3", "med_3", "08:00 AM"),
                ScheduleEntity("sched_4", "med_4", "09:00 PM")
            )
            for (sch in initialSchedules) {
                scheduleDao.insertSchedule(sch)
            }

            // Seed historical data back-dated for 30 days
            val pastLogs = mutableListOf<DoseLogEntity>()
            val cal = Calendar.getInstance()
            for (i in 1..29) {
                cal.time = Date()
                cal.add(Calendar.DAY_OF_YEAR, -i)
                val pastDateStr = sdf.format(cal.time)

                // Alternate adherence rates dynamically to simulate actual therapy progression
                val isMissed = (i % 7 == 0)
                pastLogs.add(DoseLogEntity("past_log_a_$i", "med_1", "$pastDateStr 08:00 AM", if (isMissed) null else "$pastDateStr 08:05 AM", if (isMissed) "MISSED" else "TAKEN"))
                pastLogs.add(DoseLogEntity("past_log_b_$i", "med_2", "$pastDateStr 01:00 PM", "$pastDateStr 01:02 PM", "TAKEN"))
                if (i % 3 != 0) {
                    pastLogs.add(DoseLogEntity("past_log_c_$i", "med_3", "$pastDateStr 08:00 PM", "$pastDateStr 08:10 PM", "TAKEN"))
                }
            }
            doseLogDao.insertLogs(pastLogs)
        }
    }
}
