package com.timetrack.app.di

import android.content.Context
import androidx.room.Room
import com.timetrack.app.data.local.TimeTrackDatabase
import com.timetrack.app.data.local.dao.CategoryDao
import com.timetrack.app.data.local.dao.SessionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TimeTrackDatabase =
        Room.databaseBuilder(context, TimeTrackDatabase::class.java, "timetrack.db")
            .addMigrations(TimeTrackDatabase.MIGRATION_1_2)
            .addCallback(TimeTrackDatabase.SeedCallback())
            .build()

    @Provides
    fun provideSessionDao(db: TimeTrackDatabase): SessionDao = db.sessionDao()

    @Provides
    fun provideCategoryDao(db: TimeTrackDatabase): CategoryDao = db.categoryDao()
}
