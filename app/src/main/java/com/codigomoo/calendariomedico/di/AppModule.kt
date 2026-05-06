package com.codigomoo.calendariomedico.di

import android.content.Context
import androidx.room.Room
import com.codigomoo.calendariomedico.data.local.AppDatabase
import com.codigomoo.calendariomedico.data.local.dao.IntakeDao
import com.codigomoo.calendariomedico.data.local.dao.MedicationDao
import com.codigomoo.calendariomedico.data.local.dao.ScheduleDao
import com.codigomoo.calendariomedico.data.local.dao.TreatmentDao
import com.codigomoo.calendariomedico.data.preferences.ReminderPreferences
import com.codigomoo.calendariomedico.data.repository.IntakeRepository
import com.codigomoo.calendariomedico.data.repository.MedicationRepository
import com.codigomoo.calendariomedico.data.repository.TreatmentRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "calendario_medico.db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides fun provideTreatmentDao(db: AppDatabase): TreatmentDao = db.treatmentDao()
    @Provides fun provideMedicationDao(db: AppDatabase): MedicationDao = db.medicationDao()
    @Provides fun provideScheduleDao(db: AppDatabase): ScheduleDao = db.scheduleDao()
    @Provides fun provideIntakeDao(db: AppDatabase): IntakeDao = db.intakeDao()

    @Provides
    @Singleton
    fun provideTreatmentRepository(dao: TreatmentDao): TreatmentRepository =
        TreatmentRepository(dao)

    @Provides
    @Singleton
    fun provideMedicationRepository(
        medicationDao: MedicationDao,
        scheduleDao: ScheduleDao
    ): MedicationRepository = MedicationRepository(medicationDao, scheduleDao)

    @Provides
    @Singleton
    fun provideIntakeRepository(dao: IntakeDao): IntakeRepository = IntakeRepository(dao)

    @Provides
    @Singleton
    fun provideReminderPreferences(@ApplicationContext context: Context): ReminderPreferences =
        ReminderPreferences(context)
}
