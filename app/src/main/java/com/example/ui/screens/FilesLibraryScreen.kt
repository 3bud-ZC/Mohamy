package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.data.AppViewModel
import com.example.data.CaseFile
import com.example.data.Client
import com.example.data.LegalCase
import com.example.data.Screen
import com.example.ui.components.FileDocumentCard
import com.example.ui.components.MohamyBadgeTone
import com.example.ui.components.MohamyButton
import com.example.ui.components.MohamyButtonStyle
import com.example.ui.components.MohamyEmptyState
import com.example.ui.components.MohamySearchBar
import com.example.ui.components.MohamyStatusBadge
import com.example.ui.openCaseFile
import com.example.ui.theme.MohamyDimens
import com.example.ui.theme.legalScreenBackground
import java.io.File

@Composable
fun FilesLibraryScreen(
    viewModel: AppViewModel,
    files: List<CaseFile>,
    cases: List<LegalCase>,
    clients: List<Client>
) {
    val context = LocalContext.current
    var searchText by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("الكل") }
    val normalizedSearch = remember(searchText) { viewModel.repository.normalizeArabic(searchText) }
    val typeOptions = remember(files) {
        listOf("الكل") + files.map { it.docType.trim() }.filter { it.isNotBlank() }.distinctBy(viewModel.repository::normalizeArabic)
    }
    val filteredFiles = remember(files, normalizedSearch, selectedType) {
        files.filter { file ->
            val haystack = viewModel.repository.normalizeArabic(
                "${file.fileName} ${file.docType} ${file.caseTitle} ${file.clientName} ${file.extractedText} ${file.normalizedSearchIndex}"
            )
            val matchesSearch = normalizedSearch.isBlank() || haystack.contains(normalizedSearch)
            val matchesType = selectedType == "الكل" ||
                viewModel.repository.normalizeArabic(file.docType) == viewModel.repository.normalizeArabic(selectedType)
            matchesSearch && matchesType
        }
    }
    val indexedCount = remember(files) { files.count { it.extractedText.isNotBlank() || it.normalizedSearchIndex.isNotBlank() } }
    val casesWithFiles = remember(files) { files.map { it.caseId }.distinct().size }
    val clientsWithFiles = remember(files, clients) { files.map { it.clientId }.distinct().size.coerceAtMost(clients.size) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .legalScreenBackground()
            .padding(horizontal = MohamyDimens.screenHorizontal, vertical = MohamyDimens.screenVertical),
        verticalArrangement = Arrangement.spacedBy(MohamyDimens.sectionGap)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(MohamyDimens.largeCardRadius),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "مكتبة الملفات",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold
                        )
                        Text(
                            "أرشيف محلي للمستندات القانونية مع فهرسة البحث وربط كل مستند بقضيته وموكله.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    MohamyStatusBadge(text = "${filteredFiles.size} نتيجة", tone = MohamyBadgeTone.Gold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FileStatCard("إجمالي الملفات", files.size.toString(), Modifier.weight(1f))
                    FileStatCard("جاهزة للبحث", indexedCount.toString(), Modifier.weight(1f))
                    FileStatCard("قضايا مرتبطة", casesWithFiles.toString(), Modifier.weight(1f))
                }
            }
        }

        MohamySearchBar(
            value = searchText,
            onValueChange = { searchText = it },
            placeholder = "ابحث باسم الملف أو نوعه أو القضية أو الموكل أو النص المفهرس..."
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            typeOptions.forEach { type ->
                FilterChip(
                    selected = selectedType == type,
                    onClick = { selectedType = type },
                    label = { Text(type) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MohamyButton(
                text = "بحث متقدم",
                icon = Icons.Default.Search,
                style = MohamyButtonStyle.Secondary,
                modifier = Modifier.weight(1f),
                onClick = { viewModel.navigateTo(Screen.Search) }
            )
            MohamyStatusBadge(
                text = "موكلون مرتبطون $clientsWithFiles",
                tone = MohamyBadgeTone.Neutral
            )
        }

        if (filteredFiles.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                MohamyEmptyState(
                    icon = Icons.Default.FolderCopy,
                    title = if (files.isEmpty()) "لا توجد مستندات محفوظة بعد" else "لا توجد نتائج مطابقة",
                    message =
                        if (files.isEmpty()) {
                            "ستظهر هنا كل الملفات المرتبطة بالقضايا بعد استيرادها من داخل ملفات القضايا."
                        } else {
                            "جرّب تغيير نوع المستند أو كلمات البحث للوصول إلى النتيجة المطلوبة."
                        },
                    actionText = if (files.isEmpty()) "عرض القضايا" else "فتح البحث",
                    onActionClick = {
                        if (files.isEmpty()) viewModel.navigateTo(Screen.CasesList) else viewModel.navigateTo(Screen.Search)
                    },
                    secondaryActionText = if (files.isEmpty() && !viewModel.hasDemoSeededForCurrentWorkspace) "بيانات تجريبية" else null,
                    onSecondaryActionClick = if (files.isEmpty() && !viewModel.hasDemoSeededForCurrentWorkspace) {
                        { viewModel.seedDemoWorkspace() }
                    } else {
                        null
                    }
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)
            ) {
                items(filteredFiles, key = { it.id }) { file ->
                    val caseExists = cases.any { it.id == file.caseId }
                    val linkedClientName = clients.firstOrNull { it.id == file.clientId }?.name ?: file.clientName
                    FileDocumentCard(
                        file = file.copy(clientName = linkedClientName),
                        onClick = { openCaseFile(context, file) }
                    ) {
                        MohamyButton(
                            text = "فتح الملف",
                            icon = Icons.AutoMirrored.Filled.OpenInNew,
                            modifier = Modifier.weight(1f),
                            onClick = { openCaseFile(context, file) }
                        )
                        OutlinedButton(
                            onClick = { viewModel.navigateTo(Screen.CaseDetails(file.caseId)) },
                            enabled = caseExists,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Gavel, contentDescription = null)
                            androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(6.dp))
                            Text("فتح القضية")
                        }
                        IconButton(
                            onClick = { shareCaseFile(context, file) },
                            modifier = Modifier
                                .size(46.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape)
                        ) {
                            Icon(Icons.Default.IosShare, contentDescription = "مشاركة", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(
                            onClick = { viewModel.deleteFile(file) },
                            modifier = Modifier
                                .size(46.dp)
                                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f), CircleShape)
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FileStatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun shareCaseFile(context: android.content.Context, file: CaseFile) {
    try {
        val target = File(file.filePath)
        if (!target.exists()) {
            Toast.makeText(context, "الملف غير موجود على التخزين المحلي.", Toast.LENGTH_SHORT).show()
            return
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", target)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = context.contentResolver.getType(uri) ?: "application/octet-stream"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "مشاركة المستند"))
    } catch (e: Exception) {
        Toast.makeText(context, "تعذر مشاركة الملف: ${e.message}", Toast.LENGTH_LONG).show()
    }
}
