package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.DoseLogEntity
import com.example.data.database.DoseLogWithMedicine
import com.example.data.database.MedRemindDatabase
import com.example.data.database.MedicineEntity
import com.example.data.repository.MedRemindRepository
import com.example.ui.dashboard.DoseLog
import com.example.ui.dashboard.DoseStatus
import com.example.ui.dashboard.Medicine
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class MedRemindViewModel(application: Application) : AndroidViewModel(application) {

    private val onboardingDataStore = com.example.data.datastore.OnboardingDataStore(application)
    val isOnboardingComplete: StateFlow<Boolean> = onboardingDataStore.isOnboardingComplete
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun completeOnboarding() {
        viewModelScope.launch {
            onboardingDataStore.setOnboardingComplete(true)
        }
    }

    private val repository: MedRemindRepository

    val userSettingsStore = com.example.data.datastore.UserSettingsStore(application)

    val userName: StateFlow<String> = userSettingsStore.userName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Margaret Vance")

    val userPhone: StateFlow<String> = userSettingsStore.userPhone
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "+1 (555) 234-5678")

    val soundEnabled: StateFlow<Boolean> = userSettingsStore.soundEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val vibrationEnabled: StateFlow<Boolean> = userSettingsStore.vibrationEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val reminderAdvanceTime: StateFlow<Int> = userSettingsStore.reminderAdvanceTime
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 5)

    val isCaregiverModeGlobal: StateFlow<Boolean> = userSettingsStore.caregiverModeEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // Current Date Prefix (e.g., "2026-05-24")
    private val _currentDatePrefix = MutableStateFlow("")
    val currentDatePrefix: StateFlow<String> = _currentDatePrefix.asStateFlow()

    init {
        val database = MedRemindDatabase.getDatabase(application)
        repository = MedRemindRepository(database)

        // Set current date prefix
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        _currentDatePrefix.value = sdf.format(Date())

        // Run pre-seeding and schedule alarms
        viewModelScope.launch {
            try {
                repository.preSeedDatabaseIfEmpty()
                com.example.notification.DoseAlarmScheduler.scheduleAllFromDb(application)
            } catch (e: Exception) {
                android.util.Log.e("MedRemindViewModel", "Failed to pre-seed database or schedule alarms: ${e.message}", e)
            }
        }
    }

    // Expose Medicines list as StateFlow of UI models
    val medicines: StateFlow<List<Medicine>> = repository.allMedicinesFlow
        .map { list ->
            list.map { entity ->
                Medicine(
                    id = entity.id,
                    name = entity.name,
                    dosage = entity.dosage,
                    form = entity.form,
                    pillCount = entity.pillCount,
                    refillThreshold = entity.refillThreshold,
                    isActive = entity.isActive
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Expose Today's Dose Logs as StateFlow of UI models
    val todayDoseLogs: StateFlow<List<DoseLog>> = _currentDatePrefix
        .flatMapLatest { datePrefix ->
            repository.getLogsForDate(datePrefix)
        }
        .map { list ->
            list.map { it.toUiModel() }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val todayAdherencePercent: StateFlow<Float> = todayDoseLogs
        .map { logs ->
            if (logs.isEmpty()) 1f
            else {
                val taken = logs.count { it.status == DoseStatus.TAKEN }
                taken.toFloat() / logs.size
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1.0f)

    val currentStreak: StateFlow<Int> = todayAdherencePercent
        .map { percent ->
            // Simulating streak based on compliance
            if (percent >= 0.99f) 14 else 13
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 13)

    val lowStockCount: StateFlow<Int> = repository.lowStockCountFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun addMedicine(newMed: Medicine) {
        viewModelScope.launch {
            val entity = MedicineEntity(
                id = newMed.id,
                name = newMed.name,
                dosage = newMed.dosage,
                form = newMed.form,
                pillCount = newMed.pillCount,
                refillThreshold = newMed.refillThreshold,
                isActive = newMed.isActive
            )
            repository.insertMedicine(entity)

            // Auto-insert a default 08:00 AM schedule for this medicine
            val scheduleId = "sched_${newMed.id}"
            val scheduleEntity = com.example.data.database.ScheduleEntity(
                id = scheduleId,
                medicineId = newMed.id,
                scheduledTime = "08:00 AM"
            )
            repository.insertSchedule(scheduleEntity)

            // Auto schedule today's dose log if it's active
            if (newMed.isActive) {
                val todayStr = _currentDatePrefix.value
                val logEntity = DoseLogEntity(
                    id = "log_${System.currentTimeMillis()}",
                    medicineId = newMed.id,
                    scheduledAt = "$todayStr 08:00 AM",
                    takenAt = null,
                    status = "SCHEDULED"
                )
                repository.insertLog(logEntity)

                // Enqueue exact alarm via AlarmManager
                com.example.notification.DoseAlarmScheduler.scheduleAlarm(
                    context = getApplication(),
                    scheduleId = scheduleId,
                    medicineName = newMed.name,
                    medicineDosage = newMed.dosage,
                    timeStr = "08:00 AM"
                )
            }
        }
    }

    fun addMedicineWithSchedules(newMed: Medicine, times: List<String>) {
        viewModelScope.launch {
            val entity = MedicineEntity(
                id = newMed.id,
                name = newMed.name,
                dosage = newMed.dosage,
                form = newMed.form,
                pillCount = newMed.pillCount,
                refillThreshold = newMed.refillThreshold,
                isActive = newMed.isActive
            )
            repository.insertMedicine(entity)

            times.forEachIndexed { index, timeStr ->
                val scheduleId = "sched_${newMed.id}_$index"
                val scheduleEntity = com.example.data.database.ScheduleEntity(
                    id = scheduleId,
                    medicineId = newMed.id,
                    scheduledTime = timeStr
                )
                repository.insertSchedule(scheduleEntity)

                // If active, insert a today's dose log
                if (newMed.isActive) {
                    val todayStr = _currentDatePrefix.value
                    val logEntity = DoseLogEntity(
                        id = "log_${System.currentTimeMillis()}_${index}_${System.currentTimeMillis() % 1000}",
                        medicineId = newMed.id,
                        scheduledAt = "$todayStr $timeStr",
                        takenAt = null,
                        status = "SCHEDULED"
                    )
                    repository.insertLog(logEntity)

                    // Enqueue exact alarm via AlarmManager
                    com.example.notification.DoseAlarmScheduler.scheduleAlarm(
                        context = getApplication(),
                        scheduleId = scheduleId,
                        medicineName = newMed.name,
                        medicineDosage = newMed.dosage,
                        timeStr = timeStr
                    )
                }
            }
        }
    }

    fun adjustStock(medId: String, delta: Int) {
        viewModelScope.launch {
            val list = medicines.value
            val match = list.find { it.id == medId }
            if (match != null) {
                val newCount = (match.pillCount + delta).coerceAtLeast(0)
                repository.updatePillCount(medId, newCount)
            }
        }
    }

    fun refillMedicine(medId: String) {
        viewModelScope.launch {
            val list = medicines.value
            val match = list.find { it.id == medId }
            if (match != null) {
                val newCount = match.pillCount + 30
                repository.updatePillCount(medId, newCount)
            }
        }
    }

    fun toggleMedicineActive(medId: String) {
        viewModelScope.launch {
            val list = medicines.value
            val match = list.find { it.id == medId }
            if (match != null) {
                repository.updateMedicineActiveStatus(medId, !match.isActive)
                // Batch-update associated alarms of active states
                com.example.notification.DoseAlarmScheduler.scheduleAllFromDb(getApplication())
            }
        }
    }

    fun updateDoseStatus(logId: String, newStatus: DoseStatus) {
        viewModelScope.launch {
            val logs = todayDoseLogs.value
            val log = logs.find { it.id == logId } ?: return@launch
            val oldStatus = log.status

            // Adjust stock on transitions to TAKEN or from TAKEN
            if (newStatus == DoseStatus.TAKEN && oldStatus != DoseStatus.TAKEN) {
                val list = medicines.value
                val med = list.find { it.name == log.medicineName }
                if (med != null && med.pillCount > 0) {
                    repository.updatePillCount(med.id, med.pillCount - 1)
                }
                
                // If in caregiver mode, notify successful on-behalf log
                if (isCaregiverModeGlobal.value) {
                    android.widget.Toast.makeText(
                        getApplication(),
                        "Caregiver Charles Vance logged ${log.medicineName} as TAKEN on behalf of Margaret Vance",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            } else if (oldStatus == DoseStatus.TAKEN && newStatus != DoseStatus.TAKEN) {
                val list = medicines.value
                val med = list.find { it.name == log.medicineName }
                if (med != null) {
                    repository.updatePillCount(med.id, med.pillCount + 1)
                }
            }

            // Trigger local Caregiver notification if a dose is missed and caregiver has notification enabled
            if (newStatus == DoseStatus.MISSED) {
                triggerPartnerMissedDoseNotification(log.medicineName)
            }

            val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
            val takenAt = if (newStatus == DoseStatus.TAKEN) format.format(Date()) else null

            repository.updateLogStatus(logId, newStatus.name, takenAt)
        }
    }

    val familyMembers: StateFlow<List<com.example.data.database.FamilyMemberEntity>> = repository.allFamilyMembersFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val activeInvitations: StateFlow<List<com.example.data.database.FamilyInvitationEntity>> = repository.allInvitationsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val generatedInviteCode = MutableStateFlow<String?>(null)

    fun generateInviteCode(relationship: String, permission: String, notifyMissed: Boolean, name: String) {
        val code = (100000..999999).random().toString()
        viewModelScope.launch {
            val invitation = com.example.data.database.FamilyInvitationEntity(
                code = code,
                relationshipProposed = relationship,
                permissionProposed = permission,
                notifyMissed = notifyMissed,
                name = name,
                isUsed = false
            )
            repository.insertInvitation(invitation)
            generatedInviteCode.value = code
        }
    }

    fun deleteFamilyMember(id: String) {
        viewModelScope.launch {
            repository.deleteFamilyMember(id)
        }
    }

    fun acceptInviteCode(code: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val invitation = repository.getInvitationByCode(code.trim())
            if (invitation != null) {
                val newId = "fam_${System.currentTimeMillis()}"
                val member = com.example.data.database.FamilyMemberEntity(
                    id = newId,
                    caregiverId = "caregiver_local",
                    memberId = "member_${invitation.name.lowercase(Locale.US).replace(" ", "_")}",
                    relationship = invitation.relationshipProposed,
                    permission = invitation.permissionProposed,
                    notifyMissed = invitation.notifyMissed,
                    name = invitation.name,
                    adherence = 1.0f,
                    hasMissedDose = false
                )
                repository.insertFamilyMember(member)
                repository.deleteInvitationByCode(code.trim())
                onResult(true, "Successfully linked with ${invitation.name}")
            } else {
                onResult(false, "Invalid or expired 6-digit invite code")
            }
        }
    }

    private fun triggerPartnerMissedDoseNotification(medicineName: String) {
        viewModelScope.launch {
            val hasCaregiverNotifier = familyMembers.value.any { it.notifyMissed }
            if (hasCaregiverNotifier) {
                val context = getApplication<Application>()
                val notificationManager = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager ?: return@launch
                val channelId = "med_remind_caregiver"
                
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    val channel = android.app.NotificationChannel(
                        channelId,
                        "Caregiver Alerts",
                        android.app.NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "Alerts for missed family doses"
                        enableLights(true)
                        enableVibration(true)
                    }
                    notificationManager.createNotificationChannel(channel)
                }
                
                val activityIntent = android.content.Intent(context, com.example.MainActivity::class.java).apply {
                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                val pendingIntent = android.app.PendingIntent.getActivity(
                    context,
                    99,
                    activityIntent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                )
                
                val notification = androidx.core.app.NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(android.R.drawable.stat_notify_chat)
                    .setContentTitle("Caregiver Alert: Missed Dose")
                    .setContentText("Your family member missed their scheduled dose of $medicineName.")
                    .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .build()
                
                notificationManager.notify(101, notification)
            }
        }
    }

    fun toggleCaregiverMode(enabled: Boolean) {
        viewModelScope.launch {
            userSettingsStore.saveCaregiverModeEnabled(enabled)
        }
    }

    fun saveUserName(name: String) {
        viewModelScope.launch {
            userSettingsStore.saveUserName(name)
        }
    }

    fun saveUserPhone(phone: String) {
        viewModelScope.launch {
            userSettingsStore.saveUserPhone(phone)
        }
    }

    fun saveSoundEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userSettingsStore.saveSoundEnabled(enabled)
        }
    }

    fun saveVibrationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userSettingsStore.saveVibrationEnabled(enabled)
        }
    }

    fun saveReminderAdvanceTime(timeMinutes: Int) {
        viewModelScope.launch {
            userSettingsStore.saveReminderAdvanceTime(timeMinutes)
        }
    }

    // -------------------------------------------------------------------------
    // Pharmacy Finder State & Logic
    // -------------------------------------------------------------------------
    private val pharmacyRepository = com.example.data.repository.PharmacyRepository(application)

    val pharmacies = MutableStateFlow<List<com.example.data.api.PharmacyUiModel>>(emptyList())
    val isPharmacyLoading = MutableStateFlow(false)
    val pharmacyError = MutableStateFlow<String?>(null)
    val isPharmacySimulated = MutableStateFlow(false)

    fun searchNearbyPharmacies(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            isPharmacyLoading.value = true
            pharmacyError.value = null
            isPharmacySimulated.value = false
            try {
                // Fetch directly from OpenStreetMap Overpass using local repository
                val results = pharmacyRepository.fetchNearbyPharmacies(latitude, longitude)
                pharmacies.value = results
                if (results.isEmpty()) {
                    pharmacyError.value = "No pharmacies found within 3km of your location in OpenStreetMap database."
                }
            } catch (e: java.lang.Exception) {
                // Safe simulated fallback on failure
                loadMockPharmacies(latitude, longitude)
                isPharmacySimulated.value = true
                pharmacyError.value = "OSM connection failed. Loaded offline simulated pharmacies nearby."
            } finally {
                isPharmacyLoading.value = false
            }
        }
    }

    fun loadMockPharmacies(lat: Double, lng: Double) {
        val mockData = listOf(
            Pair("CVS Pharmacy", Pair(0.003, -0.002)),
            Pair("Walgreens Pharmacy", Pair(-0.005, 0.006)),
            Pair("Rite Aid Pharmacy", Pair(0.008, 0.011)),
            Pair("MedRemind Care Pharmacy", Pair(-0.002, -0.003)),
            Pair("Community Health Pharmacy", Pair(0.012, -0.012)),
            Pair("Super Rx Pharmacy", Pair(-0.009, -0.008))
        )
        val list = mockData.mapIndexed { index, data ->
            val pId = "mock_pharm_id_$index"
            val pName = data.first
            val dLat = lat + data.second.first
            val dLng = lng + data.second.second

            val results = FloatArray(1)
            android.location.Location.distanceBetween(
                lat, lng,
                dLat, dLng,
                results
            )
            val distKm = results[0].toDouble() / 1000.0

            com.example.data.api.PharmacyUiModel(
                id = pId,
                name = pName,
                distanceKm = distKm,
                isOpenStr = if (index % 2 == 0) "Hours: 08:00 AM - 10:00 PM" else null,
                vicinity = "${300 + index * 52} Medical Center Dr, Suite ${index * 10 + 1}",
                phoneNumber = "+1-555-${100 + index}-${2002 + index}",
                latitude = dLat,
                longitude = dLng
            )
        }.sortedBy { it.distanceKm }
        pharmacies.value = list
    }

    private fun DoseLogWithMedicine.toUiModel(): DoseLog {
        // From "2026-05-24 08:00 AM" -> "08:00 AM"
        val timePart = if (scheduledAt.length > 10) scheduledAt.substring(11) else scheduledAt
        return DoseLog(
            id = id,
            medicineName = medicineName,
            dosage = medicineDosage,
            scheduledTime = timePart,
            status = when (status) {
                "TAKEN" -> DoseStatus.TAKEN
                "MISSED" -> DoseStatus.MISSED
                "SKIPPED" -> DoseStatus.SKIPPED
                else -> DoseStatus.SCHEDULED
            }
        )
    }

    suspend fun getLogsWithMedicineLast30Days(): List<DoseLogWithMedicine> {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -30)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dateThirtyDaysAgo = sdf.format(cal.time)
        return repository.getLogsWithMedicineLast30Days(dateThirtyDaysAgo)
    }
}

class MedRemindViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MedRemindViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MedRemindViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
