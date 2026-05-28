package com.aistudio.atelier.ui.screens

import android.app.DatePickerDialog
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aistudio.atelier.data.database.Bottle
import com.aistudio.atelier.ui.theme.*
import com.aistudio.atelier.ui.viewmodel.BottleWithLogs
import com.aistudio.atelier.ui.viewmodel.FragranceViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PerfumeDetailScreen(
    item: BottleWithLogs,
    viewModel: FragranceViewModel,
    onDismiss: () -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }

    // State holders for editing
    var house by remember { mutableStateOf(item.bottle.house) }
    var name by remember { mutableStateOf(item.bottle.name) }
    var imageUrl by remember { mutableStateOf(item.bottle.imageUrl ?: "") }
    var yearStr by remember { mutableStateOf(item.bottle.year?.toString() ?: "") }
    var olfactoryFamily by remember { mutableStateOf(item.bottle.family) }
    var priceStr by remember { mutableStateOf(item.bottle.price.toString()) }
    var topNotes by remember { mutableStateOf(item.bottle.topNotes) }
    var middleNotes by remember { mutableStateOf(item.bottle.middleNotes) }
    var baseNotes by remember { mutableStateOf(item.bottle.baseNotes) }
    var description by remember { mutableStateOf(item.bottle.description) }
    var perfumer by remember { mutableStateOf(item.bottle.perfumer ?: "") }
    var mlPerSprayStr by remember { mutableStateOf(item.bottle.mlPerSpray.toString()) }
    var personalNotes by remember { mutableStateOf(item.bottle.personalNotes) }

    // Pickers states
    var selectedConcentration by remember { mutableStateOf(item.bottle.concentration) }
    var selectedSizeMl by remember { mutableStateOf(item.bottle.sizeMl) }
    var selectedCurrency by remember { mutableStateOf(item.bottle.currency) }
    var selectedPurchaseDate by remember { mutableStateOf(item.bottle.purchaseDate) }

    // Dialog flags
    var showConcentrationPicker by remember { mutableStateOf(false) }
    var showSizePicker by remember { mutableStateOf(false) }
    var showCurrencyPicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // Observe changes from database to update viewing stats in real-time if logs are appended
    val freshItem = remember(item, viewModel.collectionDetails.collectAsState().value) {
        viewModel.collectionDetails.value.find { it.bottle.id == item.bottle.id } ?: item
    }

    Dialog(
        onDismissRequest = onDismiss
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.95f)
                .clip(RoundedCornerShape(20.dp)),
            color = BgLuxury,
            border = BorderStroke(0.5.dp, GoldAccent.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Top Header controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Page",
                            tint = CreamText
                        )
                    }

                    Text(
                        text = if (isEditing) "EDIT COUTURE" else "ATELIER ESSENTIAL",
                        color = GoldAccent,
                        fontFamily = FontFamily.Serif,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )

                    TextButton(
                        onClick = {
                            if (isEditing) {
                                // Save verification
                                if (house.isNotBlank() && name.isNotBlank()) {
                                    val finalPrice = priceStr.toDoubleOrNull() ?: 0.0
                                    val finalMlPerSpray = mlPerSprayStr.toDoubleOrNull() ?: 0.10
                                    val finalYear = yearStr.toIntOrNull()

                                    val updatedBottle = item.bottle.copy(
                                        house = house.trim(),
                                        name = name.trim(),
                                        concentration = selectedConcentration,
                                        sizeMl = selectedSizeMl,
                                        price = finalPrice,
                                        currency = selectedCurrency,
                                        purchaseDate = selectedPurchaseDate,
                                        mlPerSpray = finalMlPerSpray,
                                        imageUrl = if (imageUrl.isBlank()) null else imageUrl.trim(),
                                        topNotes = topNotes.trim(),
                                        middleNotes = middleNotes.trim(),
                                        baseNotes = baseNotes.trim(),
                                        family = olfactoryFamily.trim(),
                                        year = finalYear,
                                        description = description.trim(),
                                        perfumer = if (perfumer.isBlank()) null else perfumer.trim(),
                                        personalNotes = personalNotes.trim()
                                    )
                                    viewModel.updateBottle(updatedBottle)
                                    isEditing = false
                                } else {
                                    viewModel.showToast("House and Name are required fields!")
                                }
                            } else {
                                isEditing = true
                            }
                        },
                        modifier = Modifier.testTag("detail_edit_save_toggle")
                    ) {
                        Text(
                            text = if (isEditing) "SAVE" else "EDIT",
                            color = GoldAccent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Divider(color = GoldAccent.copy(alpha = 0.15f), thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable main body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (!isEditing) {
                        // ----------------------------------------
                        // VIEW MODE
                        // ----------------------------------------
                        // Large Portrait visual image frame
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(CardLuxury)
                                .border(
                                    width = 0.5.dp,
                                    color = GoldAccent.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(24.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!freshItem.bottle.imageUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(freshItem.bottle.imageUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Portrait",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(96.dp)
                                        .clip(CircleShape)
                                        .background(SurfaceLuxury)
                                        .border(width = 1.dp, color = GoldAccent.copy(alpha = 0.3f), shape = CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = freshItem.bottle.house.take(1).uppercase(Locale.getDefault()),
                                        color = GoldAccent,
                                        fontFamily = FontFamily.Serif,
                                        fontSize = 44.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // House & Name labels
                        Text(
                            text = freshItem.bottle.house.uppercase(Locale.getDefault()),
                            color = GoldAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = freshItem.bottle.name,
                            color = CreamText,
                            fontFamily = FontFamily.Serif,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Concentration Badge & Attributes inline
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SurfaceLuxury,
                                border = BorderStroke(0.5.dp, GoldAccent.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = freshItem.bottle.concentration.uppercase(Locale.getDefault()),
                                    color = GoldAccent,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            if (freshItem.bottle.year != null) {
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Class of ${freshItem.bottle.year}",
                                    color = MutedText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Olfactory Description Quotes block
                        if (freshItem.bottle.description.isNotBlank()) {
                            Text(
                                text = "OLFACTORY DESCRIPTION",
                                color = GoldAccent,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .drawBehind {
                                        drawLine(
                                            color = GoldAccent.copy(alpha = 0.4f),
                                            start = androidx.compose.ui.geometry.Offset(0f, 0f),
                                            end = androidx.compose.ui.geometry.Offset(0f, size.height),
                                            strokeWidth = 2.dp.toPx()
                                        )
                                    }
                                    .padding(start = 14.dp, top = 2.dp, bottom = 2.dp)
                            ) {
                                Text(
                                    text = freshItem.bottle.description,
                                    color = CreamText.copy(alpha = 0.9f),
                                    fontStyle = FontStyle.Italic,
                                    fontSize = 13.sp,
                                    lineHeight = 19.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                        }

                        // Olfactory Pyramids & Notes structure
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = CardLuxury),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(BorderStroke(0.5.dp, GoldAccent.copy(alpha = 0.15f)), RoundedCornerShape(16.dp))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "OLFACTORY PYRAMID",
                                    color = GoldAccent,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )

                                NoteSubgroup(title = "Top notes", notes = freshItem.bottle.topNotes)
                                Spacer(modifier = Modifier.height(14.dp))
                                NoteSubgroup(title = "Heart notes", notes = freshItem.bottle.middleNotes)
                                Spacer(modifier = Modifier.height(14.dp))
                                NoteSubgroup(title = "Base notes", notes = freshItem.bottle.baseNotes)
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Secondary attributes checklist (Grid)
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "ATELIER SPECIFICATIONS",
                                color = GoldAccent,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )

                            // Specs Container
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = CardLuxury),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(BorderStroke(0.5.dp, Color.White.copy(alpha = 0.03f)), RoundedCornerShape(16.dp))
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Row 1: Family & Perfumer
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column {
                                            Text("FAMILY", color = MutedText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            Text(freshItem.bottle.family, color = CreamText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                        }
                                        if (!freshItem.bottle.perfumer.isNullOrBlank()) {
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text("Nose / PERFUMER", color = MutedText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                Text(freshItem.bottle.perfumer, color = CreamText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                            }
                                        }
                                    }

                                    Divider(color = Color.White.copy(alpha = 0.05f))

                                    // Row 2: Price paid, Date of acquisition
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column {
                                            Text("PRICE COUTURE", color = MutedText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            val symbol = getCurrencySymbol(freshItem.bottle.currency)
                                            Text("$symbol${freshItem.bottle.price}", color = GoldAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("ACQUISITION DATE", color = MutedText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            val purchaseFormatted = try {
                                                val inFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
                                                val outFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
                                                outFormat.format(inFormat.parse(freshItem.bottle.purchaseDate)!!)
                                            } catch (e: Exception) {
                                                freshItem.bottle.purchaseDate
                                            }
                                            Text(purchaseFormatted, color = CreamText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                        }
                                    }

                                    Divider(color = Color.White.copy(alpha = 0.05f))

                                    // Row 3: Size & Calibration
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column {
                                            Text("BOTTLE SIZE", color = MutedText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            val sizeDisplay = when (freshItem.bottle.sizeMl) {
                                                1 -> "Decant 1ml"
                                                2 -> "Decant 2ml"
                                                5 -> "Decant 5ml"
                                                10 -> "Decant 10ml"
                                                else -> "${freshItem.bottle.sizeMl} ml"
                                            }
                                            Text(sizeDisplay, color = CreamText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("SPRAY DISCHARGE", color = MutedText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            Text("${freshItem.bottle.mlPerSpray} ml per spray", color = CreamText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                        }
                                    }
                                }
                            }
                        }

                        // Personal Memories segment
                        if (freshItem.bottle.personalNotes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = "PERSONAL JOURNAL & MEMORIES",
                                color = GoldAccent,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = CardLuxury),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(BorderStroke(0.5.dp, GoldAccent.copy(alpha = 0.15f)), RoundedCornerShape(16.dp))
                            ) {
                                Text(
                                    text = freshItem.bottle.personalNotes,
                                    color = CreamText,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // Historic logs view
                        if (freshItem.logs.isNotEmpty()) {
                            Text(
                                text = "COUTURE SPRAY REVELATION LOGS",
                                color = GoldAccent,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(bottom = 10.dp)
                            )
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = CardLuxury),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(BorderStroke(0.5.dp, Color.White.copy(alpha = 0.03f)), RoundedCornerShape(16.dp))
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    freshItem.logs.take(10).forEach { log ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                val logDateFormatted = try {
                                                    val inFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
                                                    val outFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
                                                    outFormat.format(inFormat.parse(log.date)!!)
                                                } catch (e: Exception) {
                                                    log.date
                                                }
                                                Text(text = logDateFormatted, color = CreamText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                                if (log.notes.isNotBlank()) {
                                                    Text(text = log.notes, color = MutedText, fontSize = 11.sp, fontStyle = FontStyle.Italic)
                                                }
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = SurfaceLuxury,
                                                border = BorderStroke(0.5.dp, GoldAccent.copy(alpha = 0.2f))
                                            ) {
                                                Text(
                                                    text = "${log.sprays} SPRAYS",
                                                    color = GoldAccent,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                        if (log != freshItem.logs.lastOrNull()) {
                                            Divider(color = Color.White.copy(alpha = 0.04f))
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(32.dp))
                        }

                        // Bottom Danger Area for Deletion (Safely separated inside visual block to prevent accident)
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E0C0E)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(BorderStroke(0.5.dp, RedWarning.copy(alpha = 0.3f)), RoundedCornerShape(16.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "DANGER ZONE",
                                    color = RedWarning,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "This action removes this precious fragrance from your collection forever.",
                                    color = MutedText,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { showDeleteConfirm = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = RedWarning, contentColor = Color.White),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .testTag("detail_delete_perfume_button")
                                ) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("DELETE FROM ATELIER", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        // ----------------------------------------
                        // EDIT MODE
                        // ----------------------------------------
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = house,
                                onValueChange = { house = it },
                                label = { Text("House Brand", fontSize = 11.sp, color = MutedText) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = CreamText,
                                    unfocusedTextColor = CreamText,
                                    focusedBorderColor = GoldAccent,
                                    unfocusedBorderColor = MutedText.copy(alpha = 0.4f),
                                    focusedLabelColor = GoldAccent
                                ),
                                textStyle = TextStyle(fontSize = 12.sp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("detail_input_house")
                            )

                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text("Fragrance Name", fontSize = 11.sp, color = MutedText) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = CreamText,
                                    unfocusedTextColor = CreamText,
                                    focusedBorderColor = GoldAccent,
                                    unfocusedBorderColor = MutedText.copy(alpha = 0.4f),
                                    focusedLabelColor = GoldAccent
                                ),
                                textStyle = TextStyle(fontSize = 12.sp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("detail_input_name")
                            )

                            OutlinedTextField(
                                value = imageUrl,
                                onValueChange = { imageUrl = it },
                                label = { Text("Portrait Image URL", fontSize = 11.sp, color = MutedText) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = CreamText,
                                    unfocusedTextColor = CreamText,
                                    focusedBorderColor = GoldAccent,
                                    unfocusedBorderColor = MutedText.copy(alpha = 0.4f),
                                    focusedLabelColor = GoldAccent
                                ),
                                textStyle = TextStyle(fontSize = 12.sp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("detail_input_image_url")
                            )

                            // Pickers clickable row triggers
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Concentration Picker Button
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp)
                                        .background(CardLuxury, RoundedCornerShape(8.dp))
                                        .border(BorderStroke(0.5.dp, MutedText.copy(alpha = 0.4f)), RoundedCornerShape(8.dp))
                                        .clickable { showConcentrationPicker = true }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Column {
                                        Text("CONCENTRATION", color = MutedText, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(selectedConcentration, color = CreamText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                // Size Picker Button
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp)
                                        .background(CardLuxury, RoundedCornerShape(8.dp))
                                        .border(BorderStroke(0.5.dp, MutedText.copy(alpha = 0.4f)), RoundedCornerShape(8.dp))
                                        .clickable { showSizePicker = true }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Column {
                                        Text("BOTTLE SIZE", color = MutedText, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        val sizeDisplay = when (selectedSizeMl) {
                                            1 -> "Decant 1ml"
                                            2 -> "Decant 2ml"
                                            5 -> "Decant 5ml"
                                            10 -> "Decant 10ml"
                                            else -> "${selectedSizeMl}ml"
                                        }
                                        Text(sizeDisplay, color = CreamText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // Price and currency inputs
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = priceStr,
                                    onValueChange = { priceStr = it },
                                    label = { Text("Price Paid", fontSize = 11.sp, color = MutedText) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = CreamText,
                                        unfocusedTextColor = CreamText,
                                        focusedBorderColor = GoldAccent,
                                        unfocusedBorderColor = MutedText.copy(alpha = 0.4f),
                                        focusedLabelColor = GoldAccent
                                    ),
                                    textStyle = TextStyle(fontSize = 12.sp),
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .testTag("detail_input_price")
                                )

                                Box(
                                    modifier = Modifier
                                        .weight(0.8f)
                                        .height(52.dp)
                                        .background(CardLuxury, RoundedCornerShape(8.dp))
                                        .border(BorderStroke(0.5.dp, MutedText.copy(alpha = 0.4f)), RoundedCornerShape(8.dp))
                                        .clickable { showCurrencyPicker = true }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Column {
                                        Text("CURRENCY", color = MutedText, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(selectedCurrency, color = CreamText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // Date acquired picker and year
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .height(52.dp)
                                        .background(CardLuxury, RoundedCornerShape(8.dp))
                                        .border(BorderStroke(0.5.dp, MutedText.copy(alpha = 0.4f)), RoundedCornerShape(8.dp))
                                        .clickable {
                                            val cal = Calendar.getInstance()
                                            val dateParts = selectedPurchaseDate.split("-")
                                            if (dateParts.size == 3) {
                                                cal.set(Calendar.YEAR, dateParts[0].toInt())
                                                cal.set(Calendar.MONTH, dateParts[1].toInt() - 1)
                                                cal.set(Calendar.DAY_OF_MONTH, dateParts[2].toInt())
                                            }
                                            DatePickerDialog(
                                                context,
                                                { _, y, m, d ->
                                                    selectedPurchaseDate = String.format("%04d-%02d-%02d", y, m + 1, d)
                                                },
                                                cal.get(Calendar.YEAR),
                                                cal.get(Calendar.MONTH),
                                                cal.get(Calendar.DAY_OF_MONTH)
                                            ).show()
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Column {
                                        Text("ACQUISITION DATE", color = MutedText, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(selectedPurchaseDate, color = CreamText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                OutlinedTextField(
                                    value = yearStr,
                                    onValueChange = { yearStr = it },
                                    label = { Text("Launch Year", fontSize = 11.sp, color = MutedText) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = CreamText,
                                        unfocusedTextColor = CreamText,
                                        focusedBorderColor = GoldAccent,
                                        unfocusedBorderColor = MutedText.copy(alpha = 0.4f),
                                        focusedLabelColor = GoldAccent
                                    ),
                                    textStyle = TextStyle(fontSize = 12.sp),
                                    modifier = Modifier
                                        .weight(0.8f)
                                        .testTag("detail_input_year")
                                )
                            }

                            OutlinedTextField(
                                value = olfactoryFamily,
                                onValueChange = { olfactoryFamily = it },
                                label = { Text("Olfactory Family (e.g. Woody Spicy)", fontSize = 11.sp, color = MutedText) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = CreamText,
                                    unfocusedTextColor = CreamText,
                                    focusedBorderColor = GoldAccent,
                                    unfocusedBorderColor = MutedText.copy(alpha = 0.4f),
                                    focusedLabelColor = GoldAccent
                                ),
                                textStyle = TextStyle(fontSize = 12.sp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = perfumer,
                                onValueChange = { perfumer = it },
                                label = { Text("Perfumer Name (Nose)", fontSize = 11.sp, color = MutedText) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = CreamText,
                                    unfocusedTextColor = CreamText,
                                    focusedBorderColor = GoldAccent,
                                    unfocusedBorderColor = MutedText.copy(alpha = 0.4f),
                                    focusedLabelColor = GoldAccent
                                ),
                                textStyle = TextStyle(fontSize = 13.sp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = mlPerSprayStr,
                                onValueChange = { mlPerSprayStr = it },
                                label = { Text("ml Discharge per Spray", fontSize = 11.sp, color = MutedText) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = CreamText,
                                    unfocusedTextColor = CreamText,
                                    focusedBorderColor = GoldAccent,
                                    unfocusedBorderColor = MutedText.copy(alpha = 0.4f),
                                    focusedLabelColor = GoldAccent
                                ),
                                textStyle = TextStyle(fontSize = 12.sp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = topNotes,
                                onValueChange = { topNotes = it },
                                label = { Text("Top Notes (comma-separated)", fontSize = 11.sp, color = MutedText) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = CreamText,
                                    unfocusedTextColor = CreamText,
                                    focusedBorderColor = GoldAccent,
                                    unfocusedBorderColor = MutedText.copy(alpha = 0.4f),
                                    focusedLabelColor = GoldAccent
                                ),
                                textStyle = TextStyle(fontSize = 12.sp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = middleNotes,
                                onValueChange = { middleNotes = it },
                                label = { Text("Heart Notes (comma-separated)", fontSize = 11.sp, color = MutedText) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = CreamText,
                                    unfocusedTextColor = CreamText,
                                    focusedBorderColor = GoldAccent,
                                    unfocusedBorderColor = MutedText.copy(alpha = 0.4f),
                                    focusedLabelColor = GoldAccent
                                ),
                                textStyle = TextStyle(fontSize = 12.sp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = baseNotes,
                                onValueChange = { baseNotes = it },
                                label = { Text("Base Notes (comma-separated)", fontSize = 11.sp, color = MutedText) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = CreamText,
                                    unfocusedTextColor = CreamText,
                                    focusedBorderColor = GoldAccent,
                                    unfocusedBorderColor = MutedText.copy(alpha = 0.4f),
                                    focusedLabelColor = GoldAccent
                                ),
                                textStyle = TextStyle(fontSize = 12.sp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = description,
                                onValueChange = { description = it },
                                label = { Text("Olfactory Description (multiline)", fontSize = 11.sp, color = MutedText) },
                                minLines = 3,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = CreamText,
                                    unfocusedTextColor = CreamText,
                                    focusedBorderColor = GoldAccent,
                                    unfocusedBorderColor = MutedText.copy(alpha = 0.4f),
                                    focusedLabelColor = GoldAccent
                                ),
                                textStyle = TextStyle(fontSize = 12.sp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = personalNotes,
                                onValueChange = { personalNotes = it },
                                label = { Text("Personal Memories / Notes", fontSize = 11.sp, color = MutedText) },
                                minLines = 2,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = CreamText,
                                    unfocusedTextColor = CreamText,
                                    focusedBorderColor = GoldAccent,
                                    unfocusedBorderColor = MutedText.copy(alpha = 0.4f),
                                    focusedLabelColor = GoldAccent
                                ),
                                textStyle = TextStyle(fontSize = 12.sp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }

    // Picker overlays for editing configuration
    if (showConcentrationPicker) {
        CustomPickerOverlay(
            title = "Concentration",
            options = listOf("EDT", "EDP", "Parfum", "EDC", "Cologne", "Other"),
            selectedValue = selectedConcentration,
            onSelect = {
                selectedConcentration = it
                showConcentrationPicker = false
            },
            onDismiss = { showConcentrationPicker = false }
        )
    }

    if (showSizePicker) {
        val sizeOptions = listOf(
            "Decant 1ml",
            "Decant 2ml",
            "Decant 5ml",
            "Decant 10ml",
            "30ml",
            "50ml",
            "60ml",
            "75ml",
            "100ml",
            "125ml",
            "150ml",
            "200ml"
        )
        val selectedSizeLabel = when (selectedSizeMl) {
            1 -> "Decant 1ml"
            2 -> "Decant 2ml"
            5 -> "Decant 5ml"
            10 -> "Decant 10ml"
            else -> "${selectedSizeMl}ml"
        }
        CustomPickerOverlay(
            title = "Size (ml)",
            options = sizeOptions,
            selectedValue = selectedSizeLabel,
            onSelect = {
                selectedSizeMl = it.replace("Decant", "").replace("ml", "").trim().toIntOrNull() ?: 100
                showSizePicker = false
            },
            onDismiss = { showSizePicker = false }
        )
    }

    if (showCurrencyPicker) {
        val defaultCurrency = remember {
            try {
                java.util.Currency.getInstance(Locale.getDefault()).currencyCode
            } catch (e: Exception) {
                "EUR"
            }
        }
        val currencyOptions = remember(defaultCurrency) {
            val base = mutableListOf("USD", "EUR", "GBP", "AED", "CHF")
            if (!base.contains(defaultCurrency)) {
                base.add(defaultCurrency)
            }
            base
        }
        CustomPickerOverlay(
            title = "Currency",
            options = currencyOptions,
            selectedValue = selectedCurrency,
            onSelect = {
                selectedCurrency = it
                showCurrencyPicker = false
            },
            onDismiss = { showCurrencyPicker = false }
        )
    }

    if (showDeleteConfirm) {
        Dialog(onDismissRequest = { showDeleteConfirm = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SurfaceLuxury,
                border = BorderStroke(0.5.dp, RedWarning.copy(alpha = 0.4f)),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "DELETE FRAGRANCE?",
                        color = RedWarning,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Are you absolutely stateful you wish to delete ${item.bottle.name} from your couture logs permanently?",
                        color = CreamText,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showDeleteConfirm = false },
                            border = BorderStroke(1.dp, MutedText),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CreamText),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("REFUSE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                viewModel.deleteBottle(item.bottle.id, item.bottle.name)
                                showDeleteConfirm = false
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RedWarning, contentColor = Color.White),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("CONFIRM", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
