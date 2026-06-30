package com.example.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.AppDatabase
import com.example.data.Repository
import com.example.data.AppNotificationManager
import kotlinx.coroutines.flow.firstOrNull
import java.text.SimpleDateFormat
import java.util.*

class NotificationWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val prefs = appContext.getSharedPreferences("mohamy_phone_prefs", Context.MODE_PRIVATE)
        val notificationsEnabled = prefs.getBoolean("reminders_enabled", true)
        
        if (!notificationsEnabled) {
            return Result.success()
        }

        try {
            val db = AppDatabase.getDatabase(appContext)
            val repository = Repository(db, appContext)
            
            checkSessions(repository)
            checkTasks(repository)
            checkFees(repository)
            postDailySummary(repository)

            return Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.retry()
        }
    }

    private suspend fun checkSessions(repository: Repository) {
        val sessions = repository.sessionDao.getAllSessions().firstOrNull().orEmpty()
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        cal.timeInMillis = now
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfToday = cal.timeInMillis

        val msInDay = 24L * 60 * 60 * 1000
        val endOfToday = startOfToday + msInDay - 1
        val startOfTomorrow = startOfToday + msInDay
        val endOfTomorrow = startOfTomorrow + msInDay - 1
        val endOf3Days = startOfToday + (3 * msInDay) - 1

        var tomorrowCount = 0
        var todayCount = 0
        var upcomingCount = 0

        sessions.forEach { session ->
            if (session.status == "منتهية" || session.status == "ملغاة") return@forEach
            val time = parseDate(session.date, session.time) ?: return@forEach

            when (time) {
                in startOfToday..endOfToday -> {
                    todayCount++
                    AppNotificationManager.notifyReminder(
                        appContext,
                        "جلسة اليوم",
                        "لديك جلسة اليوم في محكمة ${session.court.ifBlank { "غير محددة" }}${
                            session.time.takeIf { it.isNotBlank() }?.let { " الساعة $it" } ?: ""
                        }: ${session.requirements.ifBlank { "لا يوجد متطلبات مسجلة" }}",
                        idOffset = session.id + 3000
                    )
                }
                in startOfTomorrow..endOfTomorrow -> {
                    tomorrowCount++
                    AppNotificationManager.notifyReminder(
                        appContext,
                        "تذكير بجلسة غداً",
                        "جلسة غداً في محكمة ${session.court.ifBlank { "غير محددة" }}${
                            session.time.takeIf { it.isNotBlank() }?.let { " الساعة $it" } ?: ""
                        }: ${session.requirements.ifBlank { "لا يوجد متطلبات مسجلة" }}",
                        idOffset = session.id
                    )
                }
                in (endOfTomorrow + 1)..endOf3Days -> {
                    upcomingCount++
                }
            }
        }

        if (upcomingCount > 0 && tomorrowCount == 0 && todayCount == 0) {
            AppNotificationManager.notifyReminder(
                appContext,
                "تذكير بجلسات قريبة",
                "لديك $upcomingCount جلسات خلال الأيام الثلاثة القادمة، تأكد من جاهزية ملفاتها.",
                idOffset = 9999
            )
        }
    }

    private suspend fun checkTasks(repository: Repository) {
        val tasks = repository.taskDao.getAllTasks().firstOrNull().orEmpty()
        val now = System.currentTimeMillis()
        var overdueCount = 0

        tasks.forEach { task ->
            if (task.status == "منتهية") return@forEach
            val due = parseOnlyDate(task.dueDate) ?: return@forEach
            if (due < now) {
                overdueCount++
            }
        }

        if (overdueCount > 0) {
            AppNotificationManager.notifyReminder(
                appContext,
                "تنبيه مهام متأخرة",
                "يوجد لديك $overdueCount مهام تجاوزت تاريخ الاستحقاق ولم تكتمل بعد. افتح قائمة المهام لمراجعة الأولويات.",
                idOffset = 8888
            )
        }
    }

    private suspend fun checkFees(repository: Repository) {
        val fees = repository.feeDao.getAllFeeRecords().firstOrNull().orEmpty()
        val now = System.currentTimeMillis()
        var overdueAmount = 0.0
        var overdueCount = 0

        fees.forEach { fee ->
            val outstanding = (fee.totalAmount - fee.paidAmount).coerceAtLeast(0.0)
            if (outstanding <= 0) return@forEach
            val dueDate = fee.dueDate.takeIf { it.isNotBlank() }?.let { parseOnlyDate(it) }
            if (dueDate == null || dueDate < now) {
                overdueCount++
                overdueAmount += outstanding
            }
        }

        if (overdueCount > 0) {
            AppNotificationManager.notifyReminder(
                appContext,
                "تنبيه أتعاب مستحقة",
                "يوجد $overdueCount سجل أتعاب متأخر استحقاقه بإجمالي ${"%.2f".format(overdueAmount)} ج.م. راجع شاشة الأتعاب لمتابعة السداد.",
                idOffset = 7777
            )
        }
    }

    private suspend fun postDailySummary(repository: Repository) {
        val sessions = repository.sessionDao.getAllSessions().firstOrNull().orEmpty()
        val tasks = repository.taskDao.getAllTasks().firstOrNull().orEmpty()
        val fees = repository.feeDao.getAllFeeRecords().firstOrNull().orEmpty()
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        cal.timeInMillis = now
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfToday = cal.timeInMillis
        val endOfToday = startOfToday + 24L * 60 * 60 * 1000 - 1
        val tomorrow = startOfToday + 24L * 60 * 60 * 1000
        val endOfTomorrow = tomorrow + 24L * 60 * 60 * 1000 - 1

        val todaySessions = sessions.filter { parseDate(it.date, it.time) in startOfToday..endOfToday }
        val tomorrowSessions = sessions.filter { parseDate(it.date, it.time) in tomorrow..endOfTomorrow }
        val overdueTasks = tasks.filter { it.status != "منتهية" && parseOnlyDate(it.dueDate)?.let { it < now } == true }
        val overdueFees = fees.filter { (it.totalAmount - it.paidAmount).coerceAtLeast(0.0) > 0 && it.dueDate.takeIf { it.isNotBlank() }?.let { parseOnlyDate(it) }?.let { it < now } == true }

        val summary = buildString {
            append("ملخص المكتب اليوم: ")
            append("${todaySessions.size} جلسة، ")
            append("${tomorrowSessions.size} غداً، ")
            append("${overdueTasks.size} مهام متأخرة، ")
            append("${overdueFees.size} سجل أتعاب متأخر.")
        }

        AppNotificationManager.notifyReminder(
            appContext,
            "ملخص يومي - محامي فون",
            summary,
            idOffset = 5555
        )
    }

    private fun parseDate(dateStr: String, timeStr: String): Long? {
        var d = dateStr.toEnglishDigits().trim()
        var t = timeStr.toEnglishDigits().trim()
        if (d.isBlank()) return null
        val full = if (t.isBlank()) "$d 00:00" else "$d $t"
        
        val formats = listOf(
            "yyyy-MM-dd HH:mm", "yyyy-MM-dd H:mm",
            "dd-MM-yyyy HH:mm", "dd-MM-yyyy H:mm",
            "dd/MM/yyyy HH:mm", "dd/MM/yyyy H:mm",
            "yyyy/MM/dd HH:mm", "yyyy/MM/dd H:mm"
        )
        for (pattern in formats) {
            try {
                return SimpleDateFormat(pattern, Locale.ENGLISH).parse(full)?.time
            } catch (_: Exception) {}
        }
        return null
    }

    private fun parseOnlyDate(dateStr: String): Long? {
        var d = dateStr.toEnglishDigits().trim()
        if (d.isBlank()) return null
        val formats = listOf("yyyy-MM-dd", "dd-MM-yyyy", "dd/MM/yyyy", "yyyy/MM/dd")
        for (pattern in formats) {
            try {
                return SimpleDateFormat(pattern, Locale.ENGLISH).parse(d)?.time
            } catch (_: Exception) {}
        }
        return null
    }

    private fun String.toEnglishDigits(): String {
        var res = this
        val arabicDigits = arrayOf("٠","١","٢","٣","٤","٥","٦","٧","٨","٩")
        for (i in 0..9) { res = res.replace(arabicDigits[i], i.toString()) }
        return res
    }
}
