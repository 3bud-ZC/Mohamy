package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppViewModel
import com.example.data.CaseFile
import com.example.data.CaseSession
import com.example.data.Client
import com.example.data.ClientInteraction
import com.example.data.LegalCase
import com.example.data.Screen
import com.example.ui.theme.LegalGoldLight
import com.example.ui.theme.LegalGoldSecondary
import com.example.ui.theme.LegalGrayLight
import com.example.ui.theme.LegalNavyPrimary
import com.example.ui.theme.legalScreenBackground
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun clientStatusPalette(status: String): Pair<Color, Color> = when (status.trim()) {
    "نشط" -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
    "موقوف" -> Color(0xFFFFEBEE) to Color(0xFFB00020)
    "منتهي التعامل" -> Color(0xFFF3E5F5) to Color(0xFF6A1B9A)
    else -> Color(0xFFF3F4F6) to Color(0xFF475467)
}

private fun clientStatusOptions(): List<String> = listOf("نشط", "مؤرشف", "موقوف", "منتهي التعامل")

@Composable
fun ClientsListScreen(viewModel: AppViewModel, clients: List<Client>) {
    var searchTxt by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf("الكل") }
    val normalizedSearch = remember(searchTxt) { viewModel.repository.normalizeArabic(searchTxt) }
    val filtered = remember(clients, normalizedSearch, selectedStatus) {
        clients.filter { client ->
            val haystack = viewModel.repository.normalizeArabic("${client.name} ${client.phone} ${client.notes} ${client.status}")
            val matchesSearch = normalizedSearch.isBlank() || haystack.contains(normalizedSearch)
            val matchesStatus = selectedStatus == "الكل" || client.status == selectedStatus
            matchesSearch && matchesStatus
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.navigateTo(Screen.ClientAddEdit()) },
                containerColor = LegalNavyPrimary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, "إضافة موكل")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .legalScreenBackground()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, LegalNavyPrimary.copy(alpha = 0.08f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("سجل الموكلين", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = LegalNavyPrimary)
                    Text("إجمالي الموكلين: ${clients.size} | النتائج الحالية: ${filtered.size}", fontSize = 12.sp, color = Color.Gray)
                }
            }

            OutlinedTextField(
                value = searchTxt,
                onValueChange = { searchTxt = it },
                placeholder = { Text("ابحث عن موكل بالاسم أو الهاتف أو الحالة...") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(Icons.Default.Search, null) }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                (listOf("الكل") + clientStatusOptions()).forEach { status ->
                    FilterChip(
                        selected = selectedStatus == status,
                        onClick = { selectedStatus = status },
                        label = { Text(status) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LegalNavyPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            if (filtered.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("لم يتم العثور على موكلين مسجلين.", color = Color.Gray)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    filtered.forEach { client ->
                        val (statusBg, statusColor) = clientStatusPalette(client.status)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.navigateTo(Screen.ClientDetails(client.id)) },
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            ListItem(
                                colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = Color.Transparent),
                                leadingContent = {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.linearGradient(
                                                    colors = listOf(LegalNavyPrimary, LegalNavyPrimary.copy(alpha = 0.78f))
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = client.name.take(1),
                                            color = Color.White,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 18.sp
                                        )
                                    }
                                },
                                headlineContent = {
                                    Text(client.name, fontWeight = FontWeight.Bold, color = LegalNavyPrimary, fontSize = 16.sp)
                                },
                                supportingContent = {
                                    Column(modifier = Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.Gray)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(client.phone, color = Color.Gray, fontSize = 13.sp)
                                        }
                                        if (client.notes.isNotBlank()) {
                                            Text(client.notes, color = Color.DarkGray, fontSize = 12.sp, maxLines = 1)
                                        }
                                    }
                                },
                                trailingContent = {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(statusBg)
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(client.status, color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientDetailsScreen(
    clientId: Int,
    viewModel: AppViewModel,
    clients: List<Client>,
    cases: List<LegalCase>,
    files: List<CaseFile>,
    sessions: List<CaseSession>,
    interactions: List<ClientInteraction>
) {
    val client = clients.find { it.id == clientId }
    val clientCases = cases.filter { it.clientId == clientId }
    val clientFiles = files.filter { it.clientId == clientId }
    val clientSessions = sessions.filter { it.clientId == clientId }
    val clientInteractions = interactions.filter { it.clientId == clientId }.sortedByDescending { it.createdAt }
    val context = LocalContext.current

    if (client == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("الموكل غير موجود")
        }
        return
    }

    val averageReadiness = if (clientCases.isEmpty()) {
        0
    } else {
        clientCases.sumOf { viewModel.caseReadinessScore(it) } / clientCases.size
    }
    val latestSession = clientSessions.maxByOrNull { it.date + it.time }
    val latestFile = clientFiles.maxByOrNull { it.uploadDate }
    val (statusBg, statusColor) = clientStatusPalette(client.status)

    var interactionType by remember { mutableStateOf("مكالمة") }
    var interactionTitle by remember { mutableStateOf("") }
    var interactionDetails by remember { mutableStateOf("") }
    var selectedCaseId by remember { mutableStateOf<Int?>(clientCases.firstOrNull()?.id) }
    var interactionCaseExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .legalScreenBackground()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = LegalNavyPrimary),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(LegalGoldSecondary.copy(alpha = 0.2f))
                            .border(2.dp, LegalGoldSecondary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = client.name.take(1),
                            color = LegalGoldLight,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(client.name, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(client.phone, color = LegalGoldLight, fontSize = 14.sp)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(statusBg)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(client.status, color = statusColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ClientMetricCard("قضايا نشطة", clientCases.size.toString(), Icons.Default.AccountTree, Modifier.weight(1f))
                    ClientMetricCard("ملفات", clientFiles.size.toString(), Icons.Default.FileCopy, Modifier.weight(1f))
                    ClientMetricCard("جاهزية", "$averageReadiness%", Icons.Default.Shield, Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(12.dp))
                latestSession?.let {
                    Text("آخر جلسة/أقرب جلسة مسجلة: ${it.title} - ${it.date} ${it.time}", color = Color.White.copy(alpha = 0.88f), fontSize = 12.sp)
                }
                latestFile?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("آخر ملف مرفوع: ${it.fileName}", color = Color.White.copy(alpha = 0.88f), fontSize = 12.sp)
                }
            }
        }

        val feeRecords by viewModel.allFeeRecords.collectAsState(initial = emptyList())

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { viewModel.navigateTo(Screen.ClientAddEdit(client.id)) },
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LegalGoldSecondary)
            ) {
                Icon(Icons.Default.Edit, "تعديل البيانات")
                Spacer(modifier = Modifier.width(8.dp))
                Text("تعديل البيانات", fontWeight = FontWeight.Bold, color = LegalNavyPrimary)
            }
            OutlinedButton(
                onClick = {
                    viewModel.deleteClient(client) {
                        viewModel.navigateTo(Screen.ClientsList)
                    }
                },
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f))
            ) {
                Icon(Icons.Default.Delete, "حذف الموكل")
                Spacer(modifier = Modifier.width(8.dp))
                Text("حذف الموكل", fontWeight = FontWeight.Bold)
            }
        }

        Button(
            onClick = {
                val uri = com.example.util.PdfExporter.exportClientFinancials(
                    context, client, clientCases, feeRecords
                )
                if (uri != null) {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "مشاركة كشف حساب الموكل"))
                } else {
                    android.widget.Toast.makeText(context, "فشل تصدير الـ PDF", android.widget.Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = LegalNavyPrimary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Description, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("تصدير كشف حساب (PDF)", fontWeight = FontWeight.Bold)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlineInfoCard("الجلسات", clientSessions.size.toString(), Icons.Default.CalendarToday, Modifier.weight(1f))
            OutlineInfoCard("سجلات التواصل", clientInteractions.size.toString(), Icons.Default.HistoryEdu, Modifier.weight(1f))
            OutlineInfoCard("المستندات", clientFiles.size.toString(), Icons.Default.Description, Modifier.weight(1f))
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, LegalNavyPrimary.copy(alpha = 0.08f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("بيانات الموكل", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = LegalNavyPrimary)
                InfoLine(Icons.Default.Email, client.email.ifBlank { "لا يوجد بريد مسجل" })
                InfoLine(Icons.Default.Fingerprint, if (client.nationalId.isBlank()) "لا يوجد رقم قومي مسجل" else "الرقم القومي: ${client.nationalId}")
                InfoLine(Icons.Default.LocationOn, client.address.ifBlank { "لا يوجد عنوان مسجل" })
                if (client.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("ملاحظات", fontWeight = FontWeight.Bold, color = LegalNavyPrimary, fontSize = 13.sp)
                    Text(client.notes, color = Color.DarkGray, fontSize = 13.sp, lineHeight = 20.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${client.phone}"))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Call, "اتصال")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("اتصال سريع بالموكل")
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, LegalNavyPrimary.copy(alpha = 0.08f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("سجل التواصل", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = LegalNavyPrimary)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("مكالمة", "زيارة", "واتساب", "ملاحظة", "اتفاق").forEach { option ->
                        FilterChip(
                            selected = interactionType == option,
                            onClick = { interactionType = option },
                            label = { Text(option) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LegalNavyPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
                OutlinedTextField(
                    value = interactionTitle,
                    onValueChange = { interactionTitle = it },
                    label = { Text("عنوان سجل التواصل") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = interactionDetails,
                    onValueChange = { interactionDetails = it },
                    label = { Text("تفاصيل المكالمة أو الزيارة أو الملاحظة") },
                    modifier = Modifier.fillMaxWidth().height(110.dp)
                )
                if (clientCases.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = interactionCaseExpanded,
                        onExpandedChange = { interactionCaseExpanded = !interactionCaseExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedCaseId?.let { selectedId -> clientCases.find { it.id == selectedId }?.title } ?: "بدون ربط بقضية",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("ربط بقضية") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = interactionCaseExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = interactionCaseExpanded,
                            onDismissRequest = { interactionCaseExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("بدون ربط بقضية") },
                                onClick = {
                                    selectedCaseId = null
                                    interactionCaseExpanded = false
                                }
                            )
                            clientCases.forEach { legalCase ->
                                DropdownMenuItem(
                                    text = { Text(legalCase.title) },
                                    onClick = {
                                        selectedCaseId = legalCase.id
                                        interactionCaseExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                Button(
                    onClick = {
                        viewModel.addClientInteraction(
                            client = client,
                            interactionType = interactionType,
                            title = interactionTitle,
                            details = interactionDetails,
                            relatedCase = clientCases.find { it.id == selectedCaseId }
                        )
                        interactionTitle = ""
                        interactionDetails = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = LegalNavyPrimary)
                ) {
                    Icon(Icons.Default.Save, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("حفظ سجل التواصل", fontWeight = FontWeight.Bold)
                }

                if (clientInteractions.isEmpty()) {
                    Text("لا توجد سجلات تواصل محفوظة لهذا الموكل.", color = Color.Gray, fontSize = 12.sp)
                } else {
                    clientInteractions.forEach { interaction ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = LegalGrayLight),
                            border = BorderStroke(1.dp, LegalNavyPrimary.copy(alpha = 0.05f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(interaction.title, fontWeight = FontWeight.Bold, color = LegalNavyPrimary, modifier = Modifier.weight(1f))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(LegalGoldSecondary.copy(alpha = 0.18f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(interaction.interactionType, color = LegalNavyPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Text(interaction.details.ifBlank { "بدون تفاصيل إضافية." }, fontSize = 12.sp, color = Color.DarkGray)
                                if (interaction.relatedCaseTitle.isNotBlank()) {
                                    Text("القضية المرتبطة: ${interaction.relatedCaseTitle}", fontSize = 11.sp, color = Color.Gray)
                                }
                                Text(
                                    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH).format(Date(interaction.createdAt)),
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    OutlinedButton(onClick = { viewModel.deleteClientInteraction(interaction) }) {
                                        Text("حذف السجل", color = Color.Red)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Text(
            text = "القضايا المرتبطة بالموكل (${clientCases.size})",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = LegalNavyPrimary
        )
        if (clientCases.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(LegalGrayLight)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("لا توجد قضايا مسجلة لهذا الموكل حالياً.", color = Color.Gray, fontSize = 14.sp)
            }
        } else {
            clientCases.forEach { item ->
                val readiness = viewModel.caseReadinessScore(item)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.navigateTo(Screen.CaseDetails(item.id)) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    ListItem(
                        colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = Color.Transparent),
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(LegalGrayLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.AccountTree, null, tint = LegalNavyPrimary)
                            }
                        },
                        headlineContent = { Text(item.title, fontWeight = FontWeight.Bold, color = LegalNavyPrimary, fontSize = 16.sp) },
                        supportingContent = {
                            Column(modifier = Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text("رقم ${item.caseNumber} | محكمة ${item.courtName}", color = Color.Gray, fontSize = 12.sp)
                                Text("جاهزية الملف: $readiness% - ${viewModel.caseReadinessLabel(item)}", color = Color.DarkGray, fontSize = 12.sp)
                            }
                        },
                        trailingContent = {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(LegalGoldSecondary.copy(alpha = 0.15f))
                                    .border(1.dp, LegalGoldSecondary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(item.status, color = LegalNavyPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientAddEditScreen(clientId: Int?, viewModel: AppViewModel, clients: List<Client>) {
    val editingClient = clientId?.let { id -> clients.find { it.id == id } }

    var name by remember { mutableStateOf(editingClient?.name ?: "") }
    var phone by remember { mutableStateOf(editingClient?.phone ?: "") }
    var email by remember { mutableStateOf(editingClient?.email ?: "") }
    var nationalId by remember { mutableStateOf(editingClient?.nationalId ?: "") }
    var address by remember { mutableStateOf(editingClient?.address ?: "") }
    var notes by remember { mutableStateOf(editingClient?.notes ?: "") }
    var status by remember { mutableStateOf(editingClient?.status ?: "نشط") }
    var statusExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .legalScreenBackground()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = LegalNavyPrimary)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(LegalGoldSecondary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (editingClient == null) Icons.Default.PersonAdd else Icons.Default.Edit,
                        contentDescription = null,
                        tint = LegalGoldLight,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = if (editingClient == null) "إضافة موكل جديد" else "تعديل بيانات الموكل",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "بطاقة الموكل تشمل الحالة وسجل التعامل والمعلومات الأساسية",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("اسم الموكل بالكامل (مطلوب)") },
            leadingIcon = { Icon(Icons.Default.Person, tint = LegalNavyPrimary, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = LegalGoldSecondary,
                focusedLabelColor = LegalNavyPrimary,
                cursorColor = LegalNavyPrimary
            )
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("رقم الهاتف (مطلوب)") },
            leadingIcon = { Icon(Icons.Default.Phone, tint = LegalNavyPrimary, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = LegalGoldSecondary,
                focusedLabelColor = LegalNavyPrimary,
                cursorColor = LegalNavyPrimary
            )
        )
        Spacer(modifier = Modifier.height(16.dp))

        ExposedDropdownMenuBox(
            expanded = statusExpanded,
            onExpandedChange = { statusExpanded = !statusExpanded }
        ) {
            OutlinedTextField(
                value = status,
                onValueChange = {},
                readOnly = true,
                label = { Text("حالة الموكل") },
                leadingIcon = { Icon(Icons.Default.Warning, tint = LegalNavyPrimary, contentDescription = null) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                shape = RoundedCornerShape(12.dp)
            )
            ExposedDropdownMenu(
                expanded = statusExpanded,
                onDismissRequest = { statusExpanded = false }
            ) {
                clientStatusOptions().forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            status = option
                            statusExpanded = false
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("البريد الإلكتروني للموكل") },
            leadingIcon = { Icon(Icons.Default.Email, tint = LegalNavyPrimary, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = LegalGoldSecondary,
                focusedLabelColor = LegalNavyPrimary,
                cursorColor = LegalNavyPrimary
            )
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = nationalId,
            onValueChange = { nationalId = it },
            label = { Text("رقم البطاقة (الرقم القومي)") },
            leadingIcon = { Icon(Icons.Default.Fingerprint, tint = LegalNavyPrimary, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = LegalGoldSecondary,
                focusedLabelColor = LegalNavyPrimary,
                cursorColor = LegalNavyPrimary
            )
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text("العنوان السكني للموكل") },
            leadingIcon = { Icon(Icons.Default.LocationOn, tint = LegalNavyPrimary, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = LegalGoldSecondary,
                focusedLabelColor = LegalNavyPrimary,
                cursorColor = LegalNavyPrimary
            )
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("ملاحظات إضافية") },
            leadingIcon = { Icon(Icons.AutoMirrored.Filled.Notes, tint = LegalNavyPrimary, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = LegalGoldSecondary,
                focusedLabelColor = LegalNavyPrimary,
                cursorColor = LegalNavyPrimary
            )
        )
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                val toSave = Client(
                    id = editingClient?.id ?: 0,
                    name = name,
                    phone = phone,
                    email = email,
                    nationalId = nationalId,
                    address = address,
                    notes = notes,
                    status = status,
                    createdDate = editingClient?.createdDate ?: System.currentTimeMillis()
                )
                viewModel.saveClient(toSave) {
                    viewModel.navigateTo(Screen.ClientsList)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = LegalNavyPrimary,
                disabledContainerColor = LegalNavyPrimary.copy(alpha = 0.5f)
            ),
            enabled = name.isNotEmpty() && phone.isNotEmpty()
        ) {
            Icon(Icons.Default.Save, contentDescription = null, tint = LegalGoldLight)
            Spacer(modifier = Modifier.width(8.dp))
            Text("حفظ بيانات الموكل", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
private fun ClientMetricCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, contentDescription = null, tint = LegalGoldLight, modifier = Modifier.size(18.dp))
            Text(value, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            Text(title, color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
        }
    }
}

@Composable
private fun OutlineInfoCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, LegalNavyPrimary.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = LegalNavyPrimary, modifier = Modifier.size(18.dp))
            Text(value, fontWeight = FontWeight.ExtraBold, color = LegalNavyPrimary, fontSize = 18.sp)
            Text(title, color = Color.Gray, fontSize = 11.sp)
        }
    }
}

@Composable
private fun InfoLine(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = LegalGoldSecondary, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, color = Color.DarkGray, fontSize = 14.sp)
    }
}
