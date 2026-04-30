package com.timetrack.app.domain.usecase

import android.content.Context
import android.net.Uri
import com.timetrack.app.data.repository.SessionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class ExportCsv @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionRepository: SessionRepository,
) {
    private val iso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)

    suspend operator fun invoke(uri: Uri) {
        val sessions = sessionRepository.getAll().first()
        context.contentResolver.openOutputStream(uri)?.use { output ->
            OutputStreamWriter(output).use { writer ->
                writer.write("start_time_iso,end_time_iso,duration_minutes,category,notes\n")
                sessions.forEach { s ->
                    val start = iso.format(Date(s.startTimeMs))
                    val end = iso.format(Date(s.endTimeMs))
                    val durationMin = s.durationMs / 60_000.0
                    val category = s.category?.name ?: ""
                    val notes = s.notes?.replace(",", ";") ?: ""
                    writer.write("$start,$end,${"%.2f".format(durationMin)},$category,$notes\n")
                }
            }
        }
    }
}
