package com.timetrack.app.data.repository

import com.timetrack.app.data.local.dao.CategoryDao
import com.timetrack.app.data.local.entity.CategoryEntity
import com.timetrack.app.domain.model.Category
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao,
) {
    fun getAll(): Flow<List<Category>> =
        categoryDao.getAll().map { list -> list.map { it.toDomain() } }

    suspend fun getById(id: Long): Category? =
        categoryDao.getById(id)?.toDomain()

    suspend fun insert(category: Category): Long =
        categoryDao.insert(CategoryEntity.fromDomain(category))

    suspend fun update(category: Category) =
        categoryDao.update(CategoryEntity.fromDomain(category))

    suspend fun updateAll(categories: List<Category>) =
        categoryDao.updateAll(categories.map { CategoryEntity.fromDomain(it) })

    suspend fun delete(category: Category) =
        categoryDao.delete(CategoryEntity.fromDomain(category))
}
