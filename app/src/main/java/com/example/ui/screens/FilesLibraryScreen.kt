package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppViewModel
import com.example.data.caseFileShape
import com.example.data.CaseFile
import com.example.data.Client
import com.example.data.LegalCase
import com.example.data.Screen
import com.example.data.parseHexColorOrDefault
import com.example.ui.formatFileSize
import com.example.ui.openCaseFile
import com.example.ui.theme.LegalGoldSecondary
import com.example.ui.theme.LegalNavyPrimary
import com.example.ui.theme.legalScreenBackground
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .legalScreenBackground()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, LegalNavyPrimary.copy(alpha = 0.08f))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("مكتبة الملفات", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = LegalNavyPrimary)
                Text("إجمالي الملفات: ${files.size} | نتائج التصفية: ${filteredFiles.size}", fontSize = 12.sp, color = Color.Gray)
                Text("كل ملف مرتبط بقضيته وموكله ويمكن فتحه مباشرة أو مراجعة القضية المرتبطة به.", fontSize = 12.sp, color = Color.DarkGray)
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatMiniCard("قضايا بها ملفات", files.map { it.caseId }.distinct().size.toString(), Modifier.weight(1f))
            StatMiniCard("موكلون مرتبطون", files.map { it.clientId }.distinct().size.toString(), Modifier.weight(1f))
        }

        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            placeholder = { Text("ابحث باسم الملف أو القضية أو الموكل أو نوع المستند...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
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
                        selectedContainerColor = LegalNavyPrimary,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        if (filteredFiles.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("لا توجد ملفات مطابقة حالياً.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredFiles) { file ->
                    val caseExists = cases.any { it.id == file.caseId }
                    val linkedClientName = clients.firstOrNull { it.id == file.clientId }?.name ?: file.clientName
                    val accent = parseHexColorOrDefault(file.accentColorHex, LegalNavyPrimary)
                    val fileShape = caseFileShape(file.cardStyle)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = fileShape,
                        colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.06f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.22f))
                    ) {
                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            ListItem(
                                colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = Color.Transparent),
                                leadingContent = {
                                    Box(
                                        modifier = Modifier
                                            .background(accent.copy(alpha = 0.14f), RoundedCornerShape(12.dp))
                                            .padding(10.dp)
                                    ) {
                                        Icon(Icons.Default.Description, contentDescription = null, tint = accent)
                                    }
                                },
                                headlineContent = {
                                    Text(file.fileName, fontWeight = FontWeight.Bold, color = LegalNavyPrimary, fontSize = 15.sp)
                                },
                                supportingContent = {
                                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Text("${file.docType} | ${formatFileSize(file.fileLength)}", fontSize = 12.sp, color = Color.DarkGray)
                                        Text("القضية: ${file.caseTitle}", fontSize = 12.sp, color = Color.Gray)
                                        Text("الموكل: $linkedClientName", fontSize = 12.sp, color = Color.Gray)
                                        Text(
                                            "الحالة: ${file.extractionStatus} | الرفع: ${
                                                SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH).format(Date(file.uploadDate))
                                            }",
                                            fontSize = 11.sp,
                                            color = Color.Gray
                                        )
                                        Text("الهوية: ${file.cardStyle} | ${file.accentColorHex}", fontSize = 11.sp, color = Color.Gray)
                                    }
                                },
                                trailingContent = {
                                    Icon(Icons.Default.FolderCopy, contentDescription = null, tint = accent)
                                }
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { openCaseFile(context, file) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = LegalNavyPrimary)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("فتح الملف")
                                }
                                OutlinedButton(
                                    onClick = { viewModel.navigateTo(Screen.CaseDetails(file.caseId)) },
                                    modifier = Modifier.weight(1f),
                                    enabled = caseExists
                                ) {
                                    Icon(Icons.Default.Gavel, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("فتح القضية")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatMiniCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, LegalNavyPrimary.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = LegalNavyPrimary)
            Text(title, fontSize = 12.sp, color = Color.Gray)
        }
    }
}
