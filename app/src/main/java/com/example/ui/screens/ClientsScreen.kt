package com.example.ui.screens
import com.example.data.*
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.ui.theme.*


@Composable
fun ClientsListScreen(viewModel: AppViewModel, clients: List<Client>) {
    var searchTxt by remember { mutableStateOf("") }
    val filtered = clients.filter {
        it.name.contains(searchTxt) || it.phone.contains(searchTxt) || it.notes.contains(searchTxt)
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.navigateTo(Screen.ClientAddEdit()) },
                containerColor = LegalNavyPrimary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, "إضافة عميل")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = searchTxt,
                onValueChange = { searchTxt = it },
                placeholder = { Text("ابحث عن عملاء باسم أو رقم الهاتف...") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(Icons.Default.Search, null) }
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (filtered.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("لم يتم العثور على عملاء مسجلين.", color = Color.Gray)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filtered) { client ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clickable { viewModel.navigateTo(Screen.ClientDetails(client.id)) },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            ListItem(
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                leadingContent = {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.linearGradient(
                                                    colors = listOf(LegalNavyPrimary, LegalNavyPrimary.copy(alpha = 0.8f))
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
                                headlineContent = { Text(client.name, fontWeight = FontWeight.Bold, color = LegalNavyPrimary, fontSize = 16.sp) },
                                supportingContent = { 
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                                        Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.Gray)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(client.phone, color = Color.Gray, fontSize = 13.sp) 
                                    }
                                },
                                trailingContent = {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(LegalGrayLight),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(imageVector = Icons.Default.ArrowBackIosNew, contentDescription = null, modifier = Modifier.size(14.dp), tint = LegalNavyPrimary)
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

@Composable
fun ClientDetailsScreen(clientId: Int, viewModel: AppViewModel, clients: List<Client>, cases: List<LegalCase>) {
    val client = clients.find { it.id == clientId }
    val clientCases = cases.filter { it.clientId == clientId }
    val context = LocalContext.current

    if (client == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("العميل غير موجود")
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Client Info Card
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
                    Column {
                        Text(client.name, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(client.phone, color = LegalGoldLight, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    // Dial Phone intent
                    IconButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${client.phone}"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.background(Color.White.copy(alpha = 0.15f), CircleShape)
                    ) {
                        Icon(Icons.Default.Call, "اتصال بالعميل", tint = Color.White)
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(16.dp))
                
                client.email.takeIf { it.isNotEmpty() }?.let {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Email, null, tint = LegalGoldSecondary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(it, color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
                client.nationalId.takeIf { it.isNotEmpty() }?.let {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Fingerprint, null, tint = LegalGoldSecondary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("الرقم القومي: $it", color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null, tint = LegalGoldSecondary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(client.address.takeIf { it.isNotEmpty() } ?: "الظاهر / لا يوجد عنوان مسجل", color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp)
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
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
                Text("تعديل البيانات", fontWeight = FontWeight.Bold)
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
                Icon(Icons.Default.Delete, "حذف العميل")
                Spacer(modifier = Modifier.width(8.dp))
                Text("حذف العميل", fontWeight = FontWeight.Bold)
            }
        }

        // Notes Tab
        if (client.notes.isNotEmpty()) {
            Text("ملاحظات إضافية", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = LegalNavyPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = LegalGoldSecondary.copy(alpha = 0.1f)),
                border = BorderStroke(1.dp, LegalGoldSecondary.copy(alpha = 0.3f))
            ) {
                Text(client.notes, modifier = Modifier.padding(16.dp), fontSize = 14.sp, color = Color.DarkGray, lineHeight = 22.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Associated Cases
        Text(
            text = "القضايا المرتبطة بالعميل (${clientCases.size})",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = LegalNavyPrimary,
            modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
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
                Text("لا توجد قضايا مسجلة لهذا العميل حالياً.", color = Color.Gray, fontSize = 14.sp)
            }
        } else {
            clientCases.forEach { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clickable { viewModel.navigateTo(Screen.CaseDetails(item.id)) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(LegalGrayLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Gavel, null, tint = LegalNavyPrimary)
                            }
                        },
                        headlineContent = { Text(item.title, fontWeight = FontWeight.Bold, color = LegalNavyPrimary, fontSize = 16.sp) },
                        supportingContent = { Text("رقم ${item.caseNumber} | محكمة ${item.courtName}", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp)) },
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

@Composable
fun ClientAddEditScreen(clientId: Int?, viewModel: AppViewModel, clients: List<Client>) {
    val editingClient = clientId?.let { id -> clients.find { it.id == id } }

    var name by remember { mutableStateOf(editingClient?.name ?: "") }
    var phone by remember { mutableStateOf(editingClient?.phone ?: "") }
    var email by remember { mutableStateOf(editingClient?.email ?: "") }
    var nationalId by remember { mutableStateOf(editingClient?.nationalId ?: "") }
    var address by remember { mutableStateOf(editingClient?.address ?: "") }
    var notes by remember { mutableStateOf(editingClient?.notes ?: "") }

    Column(
        modifier = Modifier
            .fillMaxSize()
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
                        text = if (editingClient == null) "إضافة عميل جديد" else "تعديل بيانات العميل",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "يرجى تعبئة البيانات بدقة وحفظها",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("اسم العميل بالكامل (مطلوب)") },
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

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("البريد الإلكتروني للعميل") },
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
            label = { Text("العنوان السكني للعميل") },
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
            leadingIcon = { Icon(Icons.Default.Notes, tint = LegalNavyPrimary, contentDescription = null) },
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
                    notes = notes
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
            Text("حفظ بيانات العميل", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

