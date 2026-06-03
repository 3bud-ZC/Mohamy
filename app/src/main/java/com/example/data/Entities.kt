package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "clients")
data class Client(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val phone: String,
    val email: String = "",
    val nationalId: String = "",
    val address: String = "",
    val notes: String = "",
    val status: String = "نشط", // نشط، مؤرشف
    val createdDate: Long = System.currentTimeMillis()
)

@Entity(tableName = "cases")
data class LegalCase(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val caseNumber: String,
    val caseYear: String,
    val caseType: String, // أسرة، إيجارات، جنائي، مدني، إلخ.
    val clientId: Int,
    val clientName: String,
    val opponentName: String = "",
    val courtName: String = "",
    val courtCircle: String = "",
    val startDate: String = "",
    val lastSessionDate: String = "",
    val nextSessionDate: String = "",
    val status: String = "جديدة", // جديدة، قيد العمل، متداولة، مؤجلة، إلخ.
    val priority: String = "متوسطة", // عالية، متوسطة، منخفضة
    val summary: String = "",
    val notes: String = "",
    val isArchived: Boolean = false,
    val createdDate: Long = System.currentTimeMillis()
)

@Entity(tableName = "sessions")
data class CaseSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val caseId: Int,
    val caseTitle: String,
    val clientId: Int,
    val clientName: String,
    val title: String,
    val type: String = "جلسة المرافعة", // تحقيق، مرافعة، حكم، إلخ.
    val court: String = "",
    val courtCircle: String = "",
    val date: String, // YYYY-MM-DD
    val time: String = "", // HH:MM
    val requirements: String = "",
    val result: String = "",
    val nextSessionDate: String = "",
    val notes: String = "",
    val status: String = "قادمة" // قادمة، منتهية، غيابية، ملغاة
)

@Entity(tableName = "tasks")
data class LegalTask(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String = "",
    val dueDate: String = "", // YYYY-MM-DD
    val priority: String = "متوسطة", // عاجل، متوسط، عادي
    val status: String = "مفتوحة", // مفتوحة، منتهية، متأخرة
    val caseId: Int? = null,
    val caseTitle: String? = null,
    val clientId: Int? = null,
    val clientName: String? = null
)

@Entity(tableName = "files")
data class CaseFile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val caseId: Int,
    val caseTitle: String,
    val clientId: Int,
    val clientName: String = "",
    val linkedSessionId: Int? = null,
    val fileName: String,
    val filePath: String,
    val docType: String, // عقد، توكيل، محضر، حكم، إلخ.
    val fileLength: Long = 0,
    val uploadDate: Long = System.currentTimeMillis(),
    val extractedText: String = "", // النص المستخرج ومطهر للبحث ومفهرس
    val extractionStatus: String = "نجاح", // نجاح، غير مدعوم
    val normalizedSearchIndex: String = "", // الفهرس الموحد والمطهر للبحث العربي بالكامل
    val accentColorHex: String = "#E8EEF8",
    val cardStyle: String = "rounded"
)

@Entity(tableName = "templates")
data class LegalTemplate(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val category: String,
    val caseType: String,
    val description: String,
    val requiredFieldsJson: String, // قائمة الحقول المطلوبة ممتلئة بصيغة JSON
    val templateBody: String
)

@Entity(tableName = "generated_documents")
data class GeneratedDocument(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val caseId: Int,
    val templateId: Int,
    val documentTitle: String,
    val dateCreated: Long = System.currentTimeMillis(),
    val filledFieldsJson: String, // بيانات الحقول المعبأة
    val content: String // النص النهائي المستبدل فيه المتغيرات
)

@Entity(tableName = "fee_records")
data class FeeRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val clientId: Int,
    val clientName: String,
    val caseId: Int? = null,
    val caseTitle: String? = null,
    val title: String,
    val totalAmount: Double = 0.0,
    val paidAmount: Double = 0.0,
    val currency: String = "ج.م",
    val dueDate: String = "",
    val status: String = "مستحقة",
    val paymentMethod: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "custom_case_categories",
    indices = [Index(value = ["normalizedName"], unique = true)]
)
data class CustomCaseCategory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val normalizedName: String,
    val createdDate: Long = System.currentTimeMillis()
)

@Entity(tableName = "client_interactions")
data class ClientInteraction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val clientId: Int,
    val clientName: String,
    val interactionType: String = "ملاحظة",
    val title: String,
    val details: String = "",
    val relatedCaseId: Int? = null,
    val relatedCaseTitle: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "license_cache")
data class LicenseCache(
    @PrimaryKey val id: Int = 1,
    val username: String,
    val activationCode: String,
    val activatedDeviceId: String,
    val activationDate: Long,
    val lastCheckDate: Long,
    val expiryDate: Long,
    val status: String, // "نشط", "غير نشط", "موقوف", "منتهي"
    val lawyerName: String = "المحامي",
    val officeName: String = "مكتب المحاماة الخاص",
    val phone: String = "",
    val barNumber: String = ""
)
