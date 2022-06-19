package com.example.classattendanceapp.presenter.viewmodel

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.classattendanceapp.data.models.Logs
import com.example.classattendanceapp.data.models.Subject
import com.example.classattendanceapp.data.models.TimeTable
import com.example.classattendanceapp.domain.models.ModifiedLogs
import com.example.classattendanceapp.domain.models.ModifiedSubjects
import com.example.classattendanceapp.domain.models.ModifiedTimeTable
import com.example.classattendanceapp.domain.usecases.usecase.ClassAttendanceUseCase
import com.example.classattendanceapp.domain.utils.alarms.ClassAlarmBroadcastReceiver
import com.example.classattendanceapp.domain.utils.alarms.ClassAlarmManager
import com.example.classattendanceapp.presenter.utils.DateToSimpleFormat
import com.example.classattendanceapp.presenter.utils.Days
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ClassAttendanceViewModel @Inject constructor(
    private val classAttendanceUseCase: ClassAttendanceUseCase
): ViewModel() {

    private val longitudeDataStoreKey = doublePreferencesKey("userLongitude")
    private val latitudeDataStoreKey = doublePreferencesKey("userLatitude")
    private val rangeDataStoreKey = doublePreferencesKey("userRange")

    private var _floatingButtonClicked = MutableStateFlow(false)
    val floatingButtonClicked: StateFlow<Boolean> get() = _floatingButtonClicked

    fun changeFloatingButtonClickedState(state: Boolean){
        _floatingButtonClicked.value = state
    }

    private var _showAddLocationCoordinateDialog = MutableStateFlow(false)
    val showAddLocationCoordinateDialog:StateFlow<Boolean> get() = _showAddLocationCoordinateDialog

    fun changeAddLocationCoordinateState(state: Boolean){
        _showAddLocationCoordinateDialog.value = state
    }

    private var _showOverFlowMenu = MutableStateFlow(false)
    val showOverFlowMenu : StateFlow<Boolean> get() = _showOverFlowMenu

    fun changeOverFlowMenuState(state: Boolean){
        _showOverFlowMenu.value = state
    }

    suspend fun getAllLogs() = flow{
        classAttendanceUseCase.getAllLogsUseCase().collect{
            val tempLogList = mutableListOf<ModifiedLogs>()
            it.forEach {
                val tempLog = ModifiedLogs(
                    _id = it._id,
                    subjectName = it.subjectName,
                    subjectId = it.subjectId,
                    date = DateToSimpleFormat.getDay(it.timestamp),
                    day = DateToSimpleFormat.getDayOfTheWeek(it.timestamp),
                    month = DateToSimpleFormat.getMonthStringFromNumber(it.timestamp),
                    monthNumber = DateToSimpleFormat.getMonthNumber(it.timestamp),
                    year = DateToSimpleFormat.getYear(it.timestamp),
                    wasPresent = it.wasPresent
                )
                tempLogList.add(tempLog)
            }
            emit(tempLogList.toList())
        }
    }

    fun getAllLogsAdvanced() :Flow<List<ModifiedLogs>>{
        return classAttendanceUseCase.getAllLogsUseCase().map{
            val tempLogList = mutableListOf<ModifiedLogs>()
            it.forEach {
                val tempLog = ModifiedLogs(
                    _id = it._id,
                    subjectName = it.subjectName,
                    subjectId = it.subjectId,
                    date = DateToSimpleFormat.getDay(it.timestamp),
                    day = DateToSimpleFormat.getDayOfTheWeek(it.timestamp),
                    month = DateToSimpleFormat.getMonthStringFromNumber(it.timestamp),
                    monthNumber = DateToSimpleFormat.getMonthNumber(it.timestamp),
                    year = DateToSimpleFormat.getYear(it.timestamp),
                    wasPresent = it.wasPresent
                )
                tempLogList.add(tempLog)
            }
            tempLogList
        }
    }

    suspend fun getSubjects() = flow {
        classAttendanceUseCase.getSubjectsUseCase().collect {
            val tempSubjectList = mutableListOf<ModifiedSubjects>()
            it.forEach{
                val tempLogOfSubject = classAttendanceUseCase.getLogOfSubjectIdUseCase(it._id).first()
                val percentage = if(tempLogOfSubject.isEmpty()) 0.toDouble() else String.format("%.2f",((tempLogOfSubject.filter{ it.wasPresent }.size.toDouble())/tempLogOfSubject.size.toDouble())*100).toDouble()

                tempSubjectList.add(
                    ModifiedSubjects(
                        it._id,
                        it.subjectName,
                        percentage
                    )
                )
            }
            emit(tempSubjectList.toList())
        }
    }

    fun getSubjectsAdvanced() : Flow<List<ModifiedSubjects>>{
        return classAttendanceUseCase.getSubjectsUseCase().map {
            val tempSubjectList = mutableListOf<ModifiedSubjects>()
            it.forEach{
                val tempLogOfSubject = classAttendanceUseCase.getLogOfSubjectIdUseCase(it._id).first()
                val percentage = if(tempLogOfSubject.isEmpty()) 0.toDouble() else String.format("%.2f",((tempLogOfSubject.filter{ it.wasPresent }.size.toDouble())/tempLogOfSubject.size.toDouble())*100).toDouble()

                tempSubjectList.add(
                    ModifiedSubjects(
                        it._id,
                        it.subjectName,
                        percentage
                    )
                )
            }
            tempSubjectList
        }
    }

    suspend fun getTimeTable() = flow<Map<String, List<TimeTable>>>{
        val resultant = mutableMapOf<String, List<TimeTable>>()
        classAttendanceUseCase.getTimeTableUseCase().collect{
            for(day in Days.values()){
                resultant[day.day] =
                    // Faulty line
                    // Instead of collecting another flow
                    // Just filter out the already got list from the initial flow
                    // assign the result to resultant
                    classAttendanceUseCase.getTimeTableOfDayUseCase(day.value).first().ifEmpty { emptyList() }
            }
            emit(resultant)
        }
    }

    fun getTimeTableAdvanced(): Flow<Map<String, List<TimeTable>>>{
        return classAttendanceUseCase.getTimeTableUseCase().map{
            val resultant = mutableMapOf<String, List<TimeTable>>()
            for(day in Days.values()){
                resultant[day.day] = it.filter{
                    it.dayOfTheWeek == day.value
                }
            }
            resultant
        }
    }

    suspend fun insertLogs(logs: Logs){
        classAttendanceUseCase.insertLogsUseCase(logs)
    }

    suspend fun insertSubject(subject: Subject){
        classAttendanceUseCase.insertSubjectUseCase(
            Subject(
                subject._id,
                subject.subjectName.trim()
            )
        )
    }

    suspend fun insertTimeTable(
        timeTable: TimeTable,
        context: Context,

    ){
        val id = classAttendanceUseCase.insertTimeTableUseCase(timeTable)
        ClassAlarmManager.registerAlarm(
            context = context,
            timeTableId = id.toInt(),
            timeTable = timeTable
        )
    }

    suspend fun deleteLogs(id: Int){
        classAttendanceUseCase.deleteLogsUseCase(id)
    }

    suspend fun deleteSubject(id: Int){
        classAttendanceUseCase.deleteLogsWithSubjectIdUseCase(id)
        classAttendanceUseCase.deleteSubjectUseCase(id)
    }

    suspend fun deleteTimeTable(id: Int, context: Context){
        val tempTimeTable = classAttendanceUseCase.getTimeTableWithIdUseCase(id)
        Log.d("tempTimeTable", "tempTimeTable is $tempTimeTable")
        classAttendanceUseCase.deleteTimeTableUseCase(id)
        ClassAlarmManager.cancelAlarm(
            context = context,
            timeTable = tempTimeTable
        )
    }

    suspend fun deleteLogsWithSubject(subjectName: String){
        classAttendanceUseCase.deleteLogsWithSubjectUseCase(subjectName)
    }

    suspend fun deleteLogsWithSubjectId(subjectId: Int){
        classAttendanceUseCase.deleteLogsWithSubjectIdUseCase(subjectId)
    }

    private var _currentLatitudeInDataStore = MutableStateFlow<Double?>(null)
    private var _currentLongitudeInDataStore = MutableStateFlow<Double?>(null)
    private var _currentRangeInDataStore = MutableStateFlow<Double?>(null)

    val currentLatitudeInDataStore : StateFlow<Double?> get() = _currentLatitudeInDataStore
    val currentLongitudeInDataStore: StateFlow<Double?> get() = _currentLongitudeInDataStore
    val currentRangeInDataStore : StateFlow<Double?> get() = _currentRangeInDataStore

    fun changeUserLatitude(latitude: Double?){
        _currentLatitudeInDataStore.value = latitude
    }
    fun changeUserLongitude(longitude: Double?){
        _currentLongitudeInDataStore.value = longitude
    }
    fun changeUserRange(range: Double?){
        _currentRangeInDataStore.value = range
    }

    fun getCoordinateInDataStore(
        coroutineScope: CoroutineScope
    ){
        coroutineScope.launch{
            val latitudeDataStoreFlow = classAttendanceUseCase.getCoordinateInDataStoreUseCase(latitudeDataStoreKey)
            val longitudeDataStoreFlow = classAttendanceUseCase.getCoordinateInDataStoreUseCase(longitudeDataStoreKey)
            val rangeDataStoreFlow = classAttendanceUseCase.getCoordinateInDataStoreUseCase(rangeDataStoreKey)
            combine(latitudeDataStoreFlow,longitudeDataStoreFlow,rangeDataStoreFlow){ latitude, longitude, range ->
                Triple(latitude, longitude, range)
            }.collectLatest { coordinates ->
                changeUserLatitude(coordinates.first)
                changeUserLongitude(coordinates.second)
                changeUserRange(coordinates.third)
            }
        }
    }

    suspend fun writeOrUpdateCoordinateInDataStore(latitude: Double, longitude: Double, range: Double){
        classAttendanceUseCase.writeOrUpdateCoordinateInDataStoreUseCase(latitudeDataStoreKey, latitude)
        classAttendanceUseCase.writeOrUpdateCoordinateInDataStoreUseCase(longitudeDataStoreKey, longitude)
        classAttendanceUseCase.writeOrUpdateCoordinateInDataStoreUseCase(rangeDataStoreKey, range)
    }

    fun deleteCoordinateInDataStore(){
        viewModelScope.launch{
            classAttendanceUseCase.deleteCoordinateInDataStoreUseCase(latitudeDataStoreKey)
            classAttendanceUseCase.deleteCoordinateInDataStoreUseCase(longitudeDataStoreKey)
        }
    }
}