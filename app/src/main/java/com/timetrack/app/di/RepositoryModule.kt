package com.timetrack.app.di

import com.timetrack.app.data.repository.CategoryRepository
import com.timetrack.app.data.repository.SessionRepository
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

// Repositories are @Singleton and use @Inject constructor — Hilt auto-provides them.
// This module is kept as a placeholder for any future binding overrides.
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule
