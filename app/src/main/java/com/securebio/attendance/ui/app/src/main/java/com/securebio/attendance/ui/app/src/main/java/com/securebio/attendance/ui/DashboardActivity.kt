package com.securebio.attendance.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.securebio.attendance.data.AppDatabase
import com.securebio.attendance.data.FirebaseRepository
import com.securebio.attendance.databinding.ActivityDashboardBinding
import com.securebio.attendance.utils.CsvExporter
import kotlinx.coroutines.launch
import java.util.Calendar

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private val db by lazy { AppDatabase.getInstance(applicationContext) }
    private val firebaseRepo by lazy { FirebaseRepository() }

    private val createCsvLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            lifecycleScope.launch {
                val records = db.attendanceDao().getAllForExport()
                CsvExporter.export(this@DashboardActivity, uri, records)
                binding.tvSyncStatus.text = "CSV exported (${records.size} records)"
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnScan.setOnClickListener {
            startActivity(Intent(this, ScanActivity::class.java))
        }

        binding.btnExportCsv.setOnClickListener {
            createCsvLauncher.launch("attendance_export.csv")
        }

        binding.btnSyncNow.setOnClickListener { syncToCloud() }

        loadTodayStats()

        lifecycleScope.launch {
            db.attendanceDao().observeAll().collect {
                loadTodayStats()
            }
        }
    }

    private fun loadTodayStats() {
        lifecycleScope.launch {
            val (start, end) = todayRange()
            val present = db.attendanceDao().countPresent(start, end)
            val absent = db.attendanceDao().countAbsent(start, end)
            updateChart(present, absent)
            binding.tvPresentCount.text = "Present: $present"
            binding.tvAbsentCount.text = "Absent: $absent"
        }
    }

    private fun updateChart(present: Int, absent: Int) {
        val entries = listOf(
            PieEntry(present.toFloat(), "Present"),
            PieEntry(absent.toFloat(), "Absent")
        )
        val dataSet = PieDataSet(entries, "Today's Attendance").apply {
            colors = listOf(Color.parseColor("#2ECC71"), Color.parseColor("#E74C3C"))
            valueTextSize = 14f
        }
        binding.pieChart.data = PieData(dataSet)
        binding.pieChart.description.isEnabled = false
        binding.pieChart.animateY(500)
        binding.pieChart.invalidate()
    }

    private fun syncToCloud() {
        binding.tvSyncStatus.text = "Syncing..."
        lifecycleScope.launch {
            firebaseRepo.syncPending(db.attendanceDao())
            binding.tvSyncStatus.text = "Cloud backup up to date"
        }
    }

    private fun todayRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.add(Calendar.DAY_OF_MONTH, 1)
        val end = cal.timeInMillis - 1
        return start to end
    }
}
