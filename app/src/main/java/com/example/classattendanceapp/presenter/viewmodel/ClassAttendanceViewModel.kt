package com.example.classattendanceapp.presenter.viewmodel

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.classattendanceapp.data.models.Logs
import com.example.classattendanceapp.data.models.Subject
import com.example.classattendanceapp.data.models.TimeTable
import com.example.classattendanceapp.domain.models.ModifiedLogs
import com.example.classattendanceapp.domain.models.ModifiedSubjects
import com.example.classattendanceapp.domain.usecases.usecase.ClassAttendanceUseCase
import com.example.classattendanceapp.domain.utils.alarms.ClassAlarmManager
import com.example.classattendanceapp.presenter.utils.DateToSimpleFormat
import com.example.classattendanceapp.presenter.utils.Days
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ClassAttendanceViewModel @Inject constructor(
    private val classAttendanceUseCase: ClassAttendanceUseCase
): ViewModel() {

    init{
        viewModelScope.launch {
            Log.d("viewmodel", "Subjects retrieval started in viewmodel")
            getSubjectsAdvanced().collectLatest { retrievedSubjectsList ->
                if(!_isInitialSubjectDataRetrievalDone.value){
                    _isInitialSubjectDataRetrievalDone.value = true
                }
                _subjectsList.value = retrievedSubjectsList
            }
        }
        viewModelScope.launch {
            Log.d("viewmodel", "Logs retrieval started in viewmodel")
            getAllLogsAdvanced().collectLatest { retrievedLogsList ->
                if(!_isInitialLogDataRetrievalDone.value){
                    _isInitialLogDataRetrievalDone.value = true
                }
                _logsList.value = retrievedLogsList
            }
        }
    }

    private var _currentHour = MutableStateFlow(0)
    val currentHour : StateFlow<Int> get() = _currentHour

    private var _currentMinute = MutableStateFlow(0)
    val currentMinute : StateFlow<Int> get() = _currentMinute

    private var _currentYear = MutableStateFlow(0)
    val currentYear : StateFlow<Int> get() = _currentYear

    private var _currentMonth = MutableStateFlow(0)
    val currentMonth : StateFlow<Int> get() = _currentMonth

    private var _currentDay = MutableStateFlow(0)
    val currentDay : StateFlow<Int> get() = _currentDay

    fun changeCurrentHour(hour: Int){
        _currentHour.value = hour
    }

    fun changeCurrentMinute(minute: Int){
        _currentMinute.value = minute
    }

    fun changeCurrentYear(year: Int){
        _currentYear.value = year
    }

    fun changeCurrentMonth(month: Int){
        _currentMonth.value = month
    }

    fun changeCurrentDay(day: Int){
        _currentDay.value = day
    }

    private var _isInitialSubjectDataRetrievalDone = MutableStateFlow(false)
    val isInitialSubjectDataRetrievalDone : StateFlow<Boolean> get() = _isInitialSubjectDataRetrievalDone

    private var _isInitialLogDataRetrievalDone = MutableStateFlow(false)
    val isInitialLogDataRetrievalDone : StateFlow<Boolean> get() = _isInitialLogDataRetrievalDone

    private var _subjectsList = MutableStateFlow<List<ModifiedSubjects>>(emptyList())
    val subjectsList : StateFlow<List<ModifiedSubjects>> get() = _subjectsList

    private var _logsList = MutableStateFlow<List<ModifiedLogs>>(emptyList())
    val logsList : StateFlow<List<ModifiedLogs>> get() = _logsList


    private val longitudeDataStoreKey = doublePreferencesKey("userLongitude")
    private val latitudeDataStoreKey = doublePreferencesKey("userLatitude")
    private val rangeDataStoreKey = doublePreferencesKey("userRange")

    private var _floatingButtonClicked = MutableStateFlow(false)
    val floatingButtonClicked: StateFlow<Boolean> get() = _floatingButtonClicked

    fun changeFloatingButtonClickedState(
        state: Boolean,
        doNotMakeChangesToTime: Boolean? = null
    ){
        if(doNotMakeChangesToTime==null && state){
            updateHourMinuteYearMonthDay()
        }
        _floatingButtonClicked.value = state
    }

    private fun updateHourMinuteYearMonthDay(){
        _currentHour.value = getCurrentHour()
        _currentMinute.value = getCurrentMinute()
        _currentYear.value = getCurrentYear()
        _currentMonth.value = getCurrentMonth()
        _currentDay.value = getCurrentDay()
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

    suspend fun updateSubject(subject: Subject){
        classAttendanceUseCase.updateSubjectUseCase(subject)
    }

    suspend fun updateLog(log: Logs){
        classAttendanceUseCase.updateLogUseCase(log)
    }

    private fun getAllLogsAdvanced() :Flow<List<ModifiedLogs>>{
        return classAttendanceUseCase.getAllLogsUseCase().map{
            val tempLogList = mutableListOf<ModifiedLogs>()
            it.forEach {

                val tempLog = ModifiedLogs(
                    _id = it._id,
                    subjectName = it.subjectName,
                    subjectId = it.subjectId,
                    hour = DateToSimpleFormat.getHours(it.timestamp),
                    minute = DateToSimpleFormat.getMinutes(it.timestamp),
                    date = DateToSimpleFormat.getDay(it.timestamp),
                    day = DateToSimpleFormat.getDayOfTheWeek(it.timestamp),
                    month = DateToSimpleFormat.getMonthStringFromNumber(it.timestamp),
                    monthNumber = DateToSimpleFormat.getConventionalMonthNumber(it.timestamp),
                    year = DateToSimpleFormat.getYear(it.timestamp),
                    wasPresent = it.wasPresent
                )
                Log.d("datetime", "Date is ${DateToSimpleFormat.getDay(it.timestamp)}")
                Log.d("datetime", "day is ${DateToSimpleFormat.getDayOfTheWeek(it.timestamp)}")
                Log.d("datetime", "month is ${DateToSimpleFormat.getMonthStringFromNumber(it.timestamp)}")
                Log.d("datetime", "monthNumber is ${DateToSimpleFormat.getMonthNumber(it.timestamp)}")
                Log.d("datetime", "Year is ${DateToSimpleFormat.getYear(it.timestamp)}")

                tempLogList.add(tempLog)
            }
            tempLogList
        }
    }

    fun getSubjectsAdvanced() : Flow<List<ModifiedSubjects>>{
        return classAttendanceUseCase.getSubjectsUseCase().map {
            val tempSubjectList = mutableListOf<ModifiedSubjects>()
            it.forEach{
                val percentage = if((it.daysPresent+it.daysAbsent)!=0.toLong()){
                    (it.daysPresent.toDouble()/(it.daysPresent+it.daysAbsent).toDouble())*100
                }else{
                    0.00
                }
                tempSubjectList.add(
                    ModifiedSubjects(
                        it._id,
                        it.subjectName,
                        percentage,
                        it.daysPresent,
                        it.daysAbsent
                    )
                )
            }
            tempSubjectList
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

    fun getTimeTableWithSubjectId(subjectId: Int): Flow<List<TimeTable>>{
        return classAttendanceUseCase.getTimeTableWithSubjectIdUseCase(subjectId)
    }

    suspend fun insertLogs(logs: Logs){
        val subjectWithId = classAttendanceUseCase.getSubjectWithIdWithUseCase(logs.subjectId)
            ?: return
        if(logs.wasPresent){
            subjectWithId.daysPresent++
        }else{
            subjectWithId.daysAbsent++
        }
        classAttendanceUseCase.insertSubjectUseCase(subjectWithId)
        classAttendanceUseCase.insertLogsUseCase(logs)
    }

    suspend fun insertSubject(subject: Subject){
        classAttendanceUseCase.insertSubjectUseCase(
            Subject(
                subject._id,
                subject.subjectName.trim(),
                subject.daysPresent,
                subject.daysAbsent
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
        val logWithId = classAttendanceUseCase.getLogsWithIdUseCase(id) ?: return

        val subjectWithId = classAttendanceUseCase.getSubjectWithIdWithUseCase(logWithId.subjectId) ?: return
        if(logWithId.wasPresent){
            subjectWithId.daysPresent--
        }else{
            subjectWithId.daysAbsent--
        }
        classAttendanceUseCase.insertSubjectUseCase(
            subjectWithId
        )
        classAttendanceUseCase.deleteLogsUseCase(id)
    }

    suspend fun deleteSubject(id: Int, context: Context){
        val timeTableWithSubjectId = classAttendanceUseCase.getTimeTableWithSubjectIdUseCase(id).first()
        for(timeTable in timeTableWithSubjectId){
            deleteTimeTable(
                timeTable._id,
                context
            )
        }
        classAttendanceUseCase.deleteLogsWithSubjectIdUseCase(id)
        classAttendanceUseCase.deleteSubjectUseCase(id)
    }

    suspend fun deleteTimeTable(id: Int, context: Context){
        val tempTimeTable = classAttendanceUseCase.getTimeTableWithIdUseCase(id) ?: return
        classAttendanceUseCase.deleteTimeTableUseCase(id)
        ClassAlarmManager.cancelAlarm(
            context = context,
            timeTable = tempTimeTable
        )
    }

    private var _currentLatitudeInDataStore = MutableStateFlow<Double?>(null)
    private var _currentLongitudeInDataStore = MutableStateFlow<Double?>(null)
    private var _currentRangeInDataStore = MutableStateFlow<Double?>(null)

    val currentLatitudeInDataStore : StateFlow<Double?> get() = _currentLatitudeInDataStore
    val currentLongitudeInDataStore: StateFlow<Double?> get() = _currentLongitudeInDataStore
    val currentRangeInDataStore : StateFlow<Double?> get() = _currentRangeInDataStore

    private fun changeUserLatitude(latitude: Double?){
        _currentLatitudeInDataStore.value = latitude
    }
    private fun changeUserLongitude(longitude: Double?){
        _currentLongitudeInDataStore.value = longitude
    }
    private fun changeUserRange(range: Double?){
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

    private fun getCurrentYear():Int{
        val cal = Calendar.getInstance()
        return cal.get(Calendar.YEAR)
    }
    private fun getCurrentMonth():Int{
        val cal = Calendar.getInstance()
        return cal.get(Calendar.MONTH)
    }
    private fun getCurrentDay():Int{
        val cal = Calendar.getInstance()
        return cal.get(Calendar.DAY_OF_MONTH)
    }
    private fun getCurrentHour():Int{
        val cal = Calendar.getInstance()
        return cal.get(Calendar.HOUR_OF_DAY)
    }
    private fun getCurrentMinute():Int{
        val cal = Calendar.getInstance()
        return cal.get(Calendar.MINUTE)
    }
}