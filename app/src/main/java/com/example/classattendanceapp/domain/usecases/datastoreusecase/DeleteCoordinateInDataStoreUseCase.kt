package com.example.classattendanceapp.domain.usecases.datastoreusecase

import androidx.datastore.preferences.core.Preferences
import com.example.classattendanceapp.domain.repository.ClassAttendanceRepository

class DeleteCoordinateInDataStoreUseCase(
    private val classAttendanceRepository: ClassAttendanceRepository
) {

    suspend operator fun invoke(key: Preferences.Key<Double>){
        classAttendanceRepository.deleteCoordinateInDataStore(key)
    }

}