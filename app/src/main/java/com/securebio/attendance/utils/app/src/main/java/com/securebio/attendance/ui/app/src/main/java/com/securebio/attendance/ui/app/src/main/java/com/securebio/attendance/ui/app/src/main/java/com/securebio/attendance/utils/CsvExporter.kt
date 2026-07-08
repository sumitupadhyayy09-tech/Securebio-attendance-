package com.securebio.attendance.utils

import android.content.Context
import android.net.Uri
import com.securebio.attendance.data.AttendanceEntity
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExporter {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    fun export(context: Context, uri: Uri, records: List<AttendanceEntity>) {
        context.contentResolver.openOutputStream(uri)?.use { out ->
            out.write("id,personId,personName,timestamp,status,verifiedByBiometric,synced\n".toByteArray())
            for (r in records) {
                val line = "${r.id},${r.personId},${escapeCsv(r.personName)}," +
                        "${dateFormat.format(Date(r.timestampMillis))},${r.status}," +
                        "${r.verifiedByBiometric},${r.synced}\n"
                out.write(line.toByteArray())
            }
        }
    }

    fun importRoster(context: Context, uri: Uri): List<Pair<String, String>> {
        val result = mutableListOf<Pair<String, String>>()
        context.contentResolver.openInputStream(uri)?.use { input ->
            BufferedReader(InputStreamReader(input)).useLines { lines ->
                lines.drop(1).forEach { line ->
                    val parts = line.split(",")
                    if (parts.size >= 2) result.add(parts[0].trim() to parts[1].trim())
                }
            }
        }
        return result
    }

    private fun escapeCsv(value: String): String =
        if (value.contains(",")) "\"$value\"" else value
}
