package com.aistudio.atelier.ui.screens

import android.app.DatePickerDialog
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aistudio.atelier.data.api.FragranceInfo
import com.aistudio.atelier.data.database.Bottle
import com.aistudio.atelier.data.database.LogEntity
import com.aistudio.atelier.ui.theme.*
import com.aistudio.atelier.ui.viewmodel.AutoFillState
import com.aistudio.atelier.ui.viewmodel.BottleWithLogs
import com.aistudio.atelier.ui.viewmodel.FragranceViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.*

// Helper to get currency symbol based on locale and fallback mapping
fun getCurrencySymbol(currencyCode: String): String {
    val currencySymbols = mapOf(
        "USD" to "$", "EUR" to "€", "GBP" to "£", "AED" to "د.إ", "CHF" to "Fr",
        "JPY" to "¥", "CNY" to "¥", "INR" to "₹", "CAD" to "CA$", "AUD" to "A$"
    )
    val upper = currencyCode.uppercase(Locale.getDefault())
    return currencySymbols[upper] ?: try {
        java.util.Currency.getInstance(upper).getSymbol(Locale.getDefault())
    } catch (e: Exception) {
        currencyCode
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainScreen(viewModel: FragranceViewModel) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val bottleDetails by viewModel.collectionDetails.collectAsStateWithLifecycle()
    val showAddModal by viewModel.showAddBottleModal.collectAsStateWithLifecycle()
    val showLogModalForBottle by viewModel.showLogSprayModal.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var selectedBottleForDetail by remember { mutableStateOf<BottleWithLogs?>(null) }

    // Handle toast state
    var activeToastMessage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        viewModel.toastMessage.collectLatest { message ->
            activeToastMessage = message
            delay(3000)
            activeToastMessage = null
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = BgLuxury,
        bottomBar = {
            BottomTabBar(
                currentTab = currentTab,
                onTabSelect = { viewModel.currentTab.value = it },
                onAddClick = { viewModel.showAddBottleModal.value = true }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main content based on selection
            Crossfade(
                targetState = currentTab,
                modifier = Modifier.fillMaxSize(),
                label = "TabSwitcher"
            ) { tab ->
                when (tab) {
                    "collection" -> CollectionTab(
                        bottles = bottleDetails,
                        onLogSprayClick = { viewModel.showLogSprayModal.value = it.bottle },
                        onPerfumeClick = { selectedBottleForDetail = it }
                    )
                    "analytics" -> AnalyticsTab(
                        bottles = bottleDetails
                    )
                }
            }

            // Slide up toast overlay
            AnimatedVisibility(
                visible = activeToastMessage != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
            ) {
                activeToastMessage?.let { msg ->
                    SuccessToast(message = msg)
                }
            }
        }
    }

    // Modal Sheet Overlay: Add Bottle
    if (showAddModal) {
        AddBottleModal(
            viewModel = viewModel,
            onDismiss = {
                viewModel.showAddBottleModal.value = false
                viewModel.resetAutoFillState()
            }
        )
    }

    // Modal Sheet Overlay: Log Spray
    showLogModalForBottle?.let { bottle ->
        LogSprayModal(
            bottle = bottle,
            onDismiss = { viewModel.showLogSprayModal.value = null },
            onConfirmLog = { sprays, date, notes ->
                viewModel.logSprays(bottle.id, sprays, date, notes)
            }
        )
    }

    // Detail & Editing Sheet Overlay
    selectedBottleForDetail?.let { detailItem ->
        PerfumeDetailScreen(
            item = detailItem,
            viewModel = viewModel,
            onDismiss = { selectedBottleForDetail = null }
        )
    }
}

// Custom Bottom Tab Bar with circular FAB in center
@Composable
fun BottomTabBar(
    currentTab: String,
    onTabSelect: (String) -> Unit,
    onAddClick: () -> Unit
) {
    Surface(
        color = SurfaceLuxury,
        tonalElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 0.5.dp, color = GoldAccent.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .height(72.dp)
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Collection Tab (Left)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .testTag("tab_collection")
                    .clickable { onTabSelect("collection") }
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Collections,
                    contentDescription = "Collection",
                    tint = if (currentTab == "collection") GoldAccent else MutedText,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "COLLECTION",
                    color = if (currentTab == "collection") GoldAccent else MutedText,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
            }

            // FAB in Center
            Box(
                modifier = Modifier
                    .size(64.dp),
                contentAlignment = Alignment.Center
            ) {
                FloatingActionButton(
                    onClick = onAddClick,
                    containerColor = GoldAccent,
                    contentColor = BgLuxury,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(52.dp)
                        .border(width = 3.dp, color = BgLuxury, shape = CircleShape)
                        .testTag("add_bottle_fab")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Fragrance",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Analytics Tab (Right)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .testTag("tab_analytics")
                    .clickable { onTabSelect("analytics") }
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Analytics,
                    contentDescription = "Analytics",
                    tint = if (currentTab == "analytics") GoldAccent else MutedText,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "ANALYTICS",
                    color = if (currentTab == "analytics") GoldAccent else MutedText,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
            }
        }
    }
}

// Slide up Success Toast
@Composable
fun SuccessToast(message: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = GoldAccent),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.LocalFireDepartment,
                contentDescription = "Success",
                tint = BgLuxury,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = message,
                color = BgLuxury,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// Collection View Tab
@Composable
fun CollectionTab(
    bottles: List<BottleWithLogs>,
    onLogSprayClick: (BottleWithLogs) -> Unit,
    onPerfumeClick: (BottleWithLogs) -> Unit
) {
    if (bottles.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(CardLuxury)
                        .border(width = 0.5.dp, color = GoldAccent.copy(alpha = 0.15f), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = GoldAccent.copy(alpha = 0.6f),
                        modifier = Modifier.size(40.dp)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "No Fragrances Yet",
                    color = CreamText,
                    fontFamily = FontFamily.Serif,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Tap the gorgeous gold button below to start your personal high-end olfactory collection.",
                    color = MutedText,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 19.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp, top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ATELIER",
                        color = GoldAccent,
                        fontFamily = FontFamily.Serif,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 4.sp,
                        modifier = Modifier.testTag("app_header_title")
                    )
                    
                    // Decorative high-contrast luxury circle
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(CardLuxury)
                            .border(width = 0.5.dp, color = GoldAccent.copy(alpha = 0.2f), shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = GoldAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            items(bottles) { item ->
                CollectionCard(
                    item = item,
                    onLogSprayClick = { onLogSprayClick(item) },
                    onCardClick = { onPerfumeClick(item) }
                )
            }
        }
    }
}

// Single Fragrance Display Card (Collection)
@Composable
fun CollectionCard(
    item: BottleWithLogs,
    onLogSprayClick: () -> Unit,
    onCardClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardLuxury),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("fragrance_card_${item.bottle.id}")
            .border(width = 0.5.dp, color = GoldAccent.copy(alpha = 0.10f), shape = RoundedCornerShape(24.dp))
            .clickable { onCardClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Warning Banner if < 20%
            if (item.percentRemaining < 20.0 && item.percentRemaining > 0.0) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = RedWarning.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "Warning",
                            tint = RedWarning,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Collection alert: This bottle is below 20% limit!",
                            color = RedWarning,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Top Row: Image, Details, Fill Tube
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Photo or Fallback Frame with custom tall premium bottle aspect ratio
                Box(
                    modifier = Modifier
                        .width(96.dp)
                        .height(132.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceLuxury)
                        .border(
                            width = 0.5.dp,
                            color = GoldAccent.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (!item.bottle.imageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(item.bottle.imageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "${item.bottle.name} Image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        // Initials fallback
                        Text(
                            text = item.bottle.house.take(1).uppercase(Locale.getDefault()),
                            color = GoldAccent.copy(alpha = 0.4f),
                            fontFamily = FontFamily.Serif,
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Details Middle Area
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = item.bottle.house.uppercase(Locale.getDefault()),
                        color = MutedText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.bottle.name,
                        color = Color.White,
                        fontFamily = FontFamily.Serif,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 24.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Badges row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Concentration Badge
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = GoldAccent.copy(alpha = 0.1f),
                            border = BorderStroke(0.5.dp, GoldAccent.copy(alpha = 0.2f))
                        ) {
                            Text(
                                text = item.bottle.concentration.uppercase(Locale.getDefault()),
                                color = GoldAccent,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }

                        // Year Badge if any
                        item.bottle.year?.let { y ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MutedText.copy(alpha = 0.1f),
                                border = BorderStroke(0.5.dp, MutedText.copy(alpha = 0.2f))
                            ) {
                                Text(
                                    text = y.toString(),
                                    color = MutedText,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    val sizeDisplay = when (item.bottle.sizeMl) {
                        1 -> "Decant 1ml"
                        2 -> "Decant 2ml"
                        5 -> "Decant 5ml"
                        10 -> "Decant 10ml"
                        else -> "${item.bottle.sizeMl}ml"
                    }
                    Text(
                        text = "${item.bottle.family}  •  $sizeDisplay",
                        color = MutedText,
                        fontSize = 11.sp,
                        fontStyle = FontStyle.Italic
                    )
                }

                // Visual Fill Tube Indicator on Right
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.width(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(12.dp)
                            .height(132.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(SurfaceLuxury)
                            .border(width = 0.5.dp, color = Color.White.copy(alpha = 0.05f), shape = RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        val tubeHeight = 132.dp * (item.percentRemaining / 100.0).toFloat()
                        Box(
                            modifier = Modifier
                                .width(12.dp)
                                .height(tubeHeight)
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = if (item.percentRemaining >= 20.0) {
                                            listOf(Color(0xFF81C784), Color(0xFF388E3C))
                                        } else {
                                            listOf(Color(0xFFE57373), Color(0xFFD32F2F))
                                        }
                                    )
                                )
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${item.percentRemaining.toInt()}%",
                        color = if (item.percentRemaining < 20.0) RedWarning else GoldAccent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Percentage & Quantities bar
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    val remainingMlFormatted = String.format("%.1f", item.mlRemaining)
                    Row(
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = "${item.percentRemaining.toInt()}%",
                            color = Color.White,
                            fontFamily = FontFamily.Serif,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${remainingMlFormatted}ml / ${item.spraysRemaining} sprays left",
                            color = MutedText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }

                    val symbol = getCurrencySymbol(item.bottle.currency)
                    val costFormatted = String.format("%.2f", item.costPerSpray)
                    Text(
                        text = "$symbol$costFormatted / SPRAY",
                        color = GoldAccent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))

                // Linear horizontal progress bar with Gold outline/fill
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(SurfaceLuxury)
                ) {
                    val progressWidthPercent = (item.percentRemaining / 100.0).toFloat()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progressWidthPercent)
                            .height(6.dp)
                            .background(GoldAccent)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Three Stat Boxes Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Stat 1: Price
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(SurfaceLuxury, RoundedCornerShape(12.dp))
                        .border(width = 0.5.dp, color = Color.White.copy(alpha = 0.05f), shape = RoundedCornerShape(12.dp))
                        .padding(vertical = 10.dp, horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "PAID", color = MutedText, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    val symbol = getCurrencySymbol(item.bottle.currency)
                    Text(text = "$symbol${item.bottle.price.toInt()}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // Stat 2: Sessions Logged
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(SurfaceLuxury, RoundedCornerShape(12.dp))
                        .border(width = 0.5.dp, color = Color.White.copy(alpha = 0.05f), shape = RoundedCornerShape(12.dp))
                        .padding(vertical = 10.dp, horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "LOGS", color = MutedText, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = item.sessionsCount.toString(), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // Stat 3: Last Used Date
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(SurfaceLuxury, RoundedCornerShape(12.dp))
                        .border(width = 0.5.dp, color = Color.White.copy(alpha = 0.05f), shape = RoundedCornerShape(12.dp))
                        .padding(vertical = 10.dp, horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "LAST", color = MutedText, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    val lastUsedStr = item.lastUsedDate?.let { date ->
                        try {
                            val inFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
                            val outFormat = SimpleDateFormat("MMM d", Locale.getDefault())
                            outFormat.format(inFormat.parse(date)!!)
                        } catch (e: Exception) {
                            date
                        }
                    } ?: "Never"
                    Text(
                        text = lastUsedStr,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Primary Action Bottom Row (Log Spray taking full-width fraction)
            val isEmpty = item.spraysRemaining <= 0
            Button(
                onClick = onLogSprayClick,
                enabled = !isEmpty,
                colors = ButtonDefaults.buttonColors(
                    containerColor = GoldAccent,
                    contentColor = BgLuxury,
                    disabledContainerColor = SurfaceLuxury,
                    disabledContentColor = MutedText
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("log_spray_button_${item.bottle.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isEmpty) "EMPTY" else "LOG SPRAY",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

// Small helper for Note Tag Pills
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NoteSubgroup(title: String, notes: String) {
    if (notes.isBlank()) return

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = title.uppercase(Locale.ROOT), color = MutedText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            notes.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { note ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Transparent,
                    border = BorderStroke(0.5.dp, GoldAccent.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = note,
                        color = CreamText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

// Add Bottle Dialog overlay
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddBottleModal(
    viewModel: FragranceViewModel,
    onDismiss: () -> Unit
) {
    var house by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }
    var yearStr by remember { mutableStateOf("") }
    var olfactoryFamily by remember { mutableStateOf("") }
    var priceStr by remember { mutableStateOf("") }
    var topNotes by remember { mutableStateOf("") }
    var middleNotes by remember { mutableStateOf("") } // Heart notes
    var baseNotes by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var perfumer by remember { mutableStateOf("") }
    var mlPerSprayStr by remember { mutableStateOf("0.10") }
    var personalNotes by remember { mutableStateOf("") }

    // Picker Selection states (dialog anchors)
    var selectedConcentration by remember { mutableStateOf("EDP") }
    var selectedSizeMl by remember { mutableStateOf(100) }
    var selectedCurrency by remember {
        val defaultCurrency = try {
            java.util.Currency.getInstance(Locale.getDefault()).currencyCode
        } catch (e: Exception) {
            "EUR"
        }
        mutableStateOf(defaultCurrency)
    }
    var selectedPurchaseDate by remember {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        mutableStateOf(format.format(Date()))
    }

    // Picker dialog switches
    var showConcentrationPicker by remember { mutableStateOf(false) }
    var showSizePicker by remember { mutableStateOf(false) }
    var showCurrencyPicker by remember { mutableStateOf(false) }

    val autoFillState by viewModel.autoFillState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Observe Auto-fill successes to populate fields automatically
    LaunchedEffect(autoFillState) {
        if (autoFillState is AutoFillState.Success) {
            val data = (autoFillState as AutoFillState.Success).info
            selectedConcentration = data.concentration ?: "EDP"
            topNotes = data.topNotes ?: ""
            middleNotes = data.middleNotes ?: ""
            baseNotes = data.baseNotes ?: ""
            olfactoryFamily = data.family ?: ""
            yearStr = data.year?.toString() ?: ""
            description = data.description ?: ""
            perfumer = data.perfumer ?: ""
        }
    }

    Dialog(
        onDismissRequest = onDismiss
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.95f)
                .clip(RoundedCornerShape(16.dp)),
            color = SurfaceLuxury,
            border = BorderStroke(0.5.dp, GoldAccent.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Title and Close Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add Fragrance",
                        color = CreamText,
                        fontFamily = FontFamily.Serif,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = CreamText)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Auto-fill header panel
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CardLuxury),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(BorderStroke(0.5.dp, GoldAccent.copy(alpha = 0.2f)), RoundedCornerShape(12.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "GEMINI INTELLIGENT AUTO-FILL",
                            color = GoldAccent,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = house,
                                onValueChange = { house = it },
                                label = { Text("House", fontSize = 11.sp, color = MutedText) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GoldAccent,
                                    unfocusedBorderColor = MutedText.copy(alpha = 0.4f),
                                    focusedLabelColor = GoldAccent,
                                    unfocusedLabelColor = MutedText,
                                    focusedTextColor = CreamText,
                                    unfocusedTextColor = CreamText
                                ),
                                textStyle = TextStyle(fontSize = 12.sp, color = CreamText),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("input_house")
                            )

                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text("Name", fontSize = 11.sp, color = MutedText) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GoldAccent,
                                    unfocusedBorderColor = MutedText.copy(alpha = 0.4f),
                                    focusedLabelColor = GoldAccent,
                                    unfocusedLabelColor = MutedText,
                                    focusedTextColor = CreamText,
                                    unfocusedTextColor = CreamText
                                ),
                                textStyle = TextStyle(fontSize = 12.sp, color = CreamText),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("input_name")
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Trigger auto-fill call button
                        Button(
                            onClick = { viewModel.fetchFragranceInfo(house, name) },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = BgLuxury),
                            shape = RoundedCornerShape(8.dp),
                            enabled = autoFillState !is AutoFillState.Loading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .testTag("btn_autofill")
                        ) {
                            if (autoFillState is AutoFillState.Loading) {
                                CircularProgressIndicator(
                                    color = BgLuxury,
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.LocalFireDepartment,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Auto-fill from web", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Auto-fill Feedback
                        when (autoFillState) {
                            is AutoFillState.Loading -> {
                                Text(
                                    text = "Searching fragrance notes and data with Gemini API...",
                                    color = GoldAccent,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                            is AutoFillState.Success -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocalFireDepartment,
                                        contentDescription = "Success",
                                        tint = GreenNormal,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Auto-fill successful!",
                                        color = GreenNormal,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            is AutoFillState.Error -> {
                                Text(
                                    text = (autoFillState as AutoFillState.Error).message,
                                    color = RedWarning,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                            else -> {}
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Save and Cancel buttons moved here (right under the auto-fill card)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        border = BorderStroke(1.dp, GoldAccent),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldAccent),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("Cancel", fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            val p = priceStr.toDoubleOrNull() ?: 0.0
                            val ml = mlPerSprayStr.toDoubleOrNull() ?: 0.10
                            val yr = yearStr.toIntOrNull()
                            if (house.isNotBlank() && name.isNotBlank()) {
                                viewModel.saveBottle(
                                    house = house,
                                    name = name,
                                    concentration = selectedConcentration,
                                    sizeMl = selectedSizeMl,
                                    price = p,
                                    currency = selectedCurrency,
                                    purchaseDate = selectedPurchaseDate,
                                    mlPerSpray = ml,
                                    imageUrl = imageUrl,
                                    topNotes = topNotes,
                                    middleNotes = middleNotes,
                                    baseNotes = baseNotes,
                                    family = olfactoryFamily,
                                    year = yr,
                                    description = description,
                                    perfumer = perfumer,
                                    personalNotes = personalNotes
                                )
                            } else {
                                viewModel.showToast("House and Name are required fields!")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = BgLuxury),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1.2f)
                            .height(48.dp)
                            .testTag("btn_save_bottle")
                    ) {
                        Text("Save Fragrance", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = GoldAccent.copy(alpha = 0.15f))
                Spacer(modifier = Modifier.height(16.dp))

                // Core details entry fields
                // Image URL Preview Box
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CardLuxury)
                            .border(BorderStroke(0.5.dp, GoldAccent.copy(alpha = 0.3f)), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (imageUrl.isNotBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(imageUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Preview Image",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(imageVector = Icons.Default.LocalFireDepartment, contentDescription = null, tint = MutedText.copy(alpha = 0.5f))
                        }
                    }

                    OutlinedTextField(
                        value = imageUrl,
                        onValueChange = { imageUrl = it },
                        label = { Text("Image URL", fontSize = 11.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = CreamText,
                            unfocusedTextColor = CreamText,
                            focusedBorderColor = GoldAccent,
                            unfocusedBorderColor = MutedText.copy(alpha = 0.4f),
                            focusedLabelColor = GoldAccent,
                            unfocusedLabelColor = MutedText
                        ),
                        textStyle = TextStyle(fontSize = 11.sp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_image_url")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Selectors row: 1. Concentration, 2. Size ML
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Concentration custom modal opener
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "CONCENTRATION", color = MutedText, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .background(CardLuxury, RoundedCornerShape(8.dp))
                                .border(BorderStroke(0.5.dp, MutedText.copy(alpha = 0.4f)), RoundedCornerShape(8.dp))
                                .clickable { showConcentrationPicker = true }
                                .padding(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = selectedConcentration, color = CreamText, fontSize = 13.sp)
                            Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(16.dp))
                        }
                    }

                    // Size custom modal opener
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "SIZE (ML)", color = MutedText, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .background(CardLuxury, RoundedCornerShape(8.dp))
                                .border(BorderStroke(0.5.dp, MutedText.copy(alpha = 0.4f)), RoundedCornerShape(8.dp))
                                .clickable { showSizePicker = true }
                                .padding(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val sizeDisplay = when (selectedSizeMl) {
                                1 -> "Decant 1ml"
                                2 -> "Decant 2ml"
                                5 -> "Decant 5ml"
                                10 -> "Decant 10ml"
                                else -> "${selectedSizeMl}ml"
                            }
                            Text(text = sizeDisplay, color = CreamText, fontSize = 13.sp)
                            Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Price and Currency selectors
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = priceStr,
                        onValueChange = { priceStr = it },
                        label = { Text("Price", fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = CreamText,
                            unfocusedTextColor = CreamText,
                            focusedBorderColor = GoldAccent,
                            unfocusedBorderColor = MutedText.copy(alpha = 0.4f),
                            focusedLabelColor = GoldAccent,
                            unfocusedLabelColor = MutedText
                        ),
                        textStyle = TextStyle(fontSize = 12.sp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_price")
                    )

                    // Currency selector
                    Column(modifier = Modifier.width(100.dp)) {
                        Text(text = "CURRENCY", color = MutedText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .background(CardLuxury, RoundedCornerShape(8.dp))
                                .border(BorderStroke(0.5.dp, MutedText.copy(alpha = 0.4f)), RoundedCornerShape(8.dp))
                                .clickable { showCurrencyPicker = true }
                                .padding(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = selectedCurrency, color = CreamText, fontSize = 13.sp)
                            Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Year & Olfactory Family
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = yearStr,
                        onValueChange = { yearStr = it },
                        label = { Text("Year Of Launch", fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = CreamText,
                            unfocusedTextColor = CreamText,
                            focusedBorderColor = GoldAccent,
                            unfocusedBorderColor = MutedText.copy(alpha = 0.4f),
                            focusedLabelColor = GoldAccent,
                            unfocusedLabelColor = MutedText
                        ),
                        textStyle = TextStyle(fontSize = 12.sp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_year")
                    )

                    OutlinedTextField(
                        value = olfactoryFamily,
                        onValueChange = { olfactoryFamily = it },
                        label = { Text("Olfactory Family", fontSize = 11.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = CreamText,
                            unfocusedTextColor = CreamText,
                            focusedBorderColor = GoldAccent,
                            unfocusedBorderColor = MutedText.copy(alpha = 0.4f),
                            focusedLabelColor = GoldAccent,
                            unfocusedLabelColor = MutedText
                        ),
                        textStyle = TextStyle(fontSize = 12.sp),
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("input_family")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Native Date Picker Selector for Purchase Date
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(text = "PURCHASE DATE", color = MutedText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
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
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = selectedPurchaseDate, color = CreamText, fontSize = 13.sp)
                        Icon(imageVector = Icons.Default.DateRange, contentDescription = "Date Picker", tint = GoldAccent, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = GoldAccent.copy(alpha = 0.15f))
                Spacer(modifier = Modifier.height(16.dp))

                // Notes Subsets: Top, Heart, Base Notes
                OutlinedTextField(
                    value = topNotes,
                    onValueChange = { topNotes = it },
                    label = { Text("Top Notes (comma-separated)", fontSize = 11.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = CreamText,
                        unfocusedTextColor = CreamText,
                        focusedBorderColor = GoldAccent,
                        unfocusedBorderColor = MutedText.copy(alpha = 0.4f),
                        focusedLabelColor = GoldAccent,
                        unfocusedLabelColor = MutedText
                    ),
                    textStyle = TextStyle(fontSize = 12.sp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("input_top_notes")
                )

                OutlinedTextField(
                    value = middleNotes,
                    onValueChange = { middleNotes = it },
                    label = { Text("Heart / Middle Notes (comma-separated)", fontSize = 11.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = CreamText,
                        unfocusedTextColor = CreamText,
                        focusedBorderColor = GoldAccent,
                        unfocusedBorderColor = MutedText.copy(alpha = 0.4f),
                        focusedLabelColor = GoldAccent,
                        unfocusedLabelColor = MutedText
                    ),
                    textStyle = TextStyle(fontSize = 12.sp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("input_middle_notes")
                )

                OutlinedTextField(
                    value = baseNotes,
                    onValueChange = { baseNotes = it },
                    label = { Text("Base Notes (comma-separated)", fontSize = 11.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = CreamText,
                        unfocusedTextColor = CreamText,
                        focusedBorderColor = GoldAccent,
                        unfocusedBorderColor = MutedText.copy(alpha = 0.4f),
                        focusedLabelColor = GoldAccent,
                        unfocusedLabelColor = MutedText
                    ),
                    textStyle = TextStyle(fontSize = 12.sp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("input_base_notes")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Perfumer and Custom ML Per Spray
                OutlinedTextField(
                    value = perfumer,
                    onValueChange = { perfumer = it },
                    label = { Text("Perfumer Name", fontSize = 11.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = CreamText,
                        unfocusedTextColor = CreamText,
                        focusedBorderColor = GoldAccent,
                        unfocusedBorderColor = MutedText.copy(alpha = 0.4f),
                        focusedLabelColor = GoldAccent,
                        unfocusedLabelColor = MutedText
                    ),
                    textStyle = TextStyle(fontSize = 12.sp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("input_perfumer")
                )

                OutlinedTextField(
                    value = mlPerSprayStr,
                    onValueChange = { mlPerSprayStr = it },
                    label = { Text("ml Per Spray", fontSize = 11.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = CreamText,
                        unfocusedTextColor = CreamText,
                        focusedBorderColor = GoldAccent,
                        unfocusedBorderColor = MutedText.copy(alpha = 0.4f),
                        focusedLabelColor = GoldAccent,
                        unfocusedLabelColor = MutedText
                    ),
                    textStyle = TextStyle(fontSize = 12.sp),
                    supportingText = { Text("Default high-end standard is 0.10ml per spray", color = MutedText) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("input_ml_per_spray")
                )

                // Descriptions & Personal notes
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Olfactory Description (multiline)", fontSize = 11.sp) },
                    minLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = CreamText,
                        unfocusedTextColor = CreamText,
                        focusedBorderColor = GoldAccent,
                        unfocusedBorderColor = MutedText.copy(alpha = 0.4f),
                        focusedLabelColor = GoldAccent,
                        unfocusedLabelColor = MutedText
                    ),
                    textStyle = TextStyle(fontSize = 12.sp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("input_description")
                )

                OutlinedTextField(
                    value = personalNotes,
                    onValueChange = { personalNotes = it },
                    label = { Text("Personal Memories / Notes", fontSize = 11.sp) },
                    minLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = CreamText,
                        unfocusedTextColor = CreamText,
                        focusedBorderColor = GoldAccent,
                        unfocusedBorderColor = MutedText.copy(alpha = 0.4f),
                        focusedLabelColor = GoldAccent,
                        unfocusedLabelColor = MutedText
                    ),
                    textStyle = TextStyle(fontSize = 12.sp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                        .testTag("input_personal_notes")
                )
            }
        }
    }

    // Picker dialogues
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
}

// Custom select modal overlay
@Composable
fun CustomPickerOverlay(
    title: String,
    options: List<String>,
    selectedValue: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceLuxury),
            border = BorderStroke(0.5.dp, GoldAccent.copy(alpha = 0.3f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Select $title",
                    fontFamily = FontFamily.Serif,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = CreamText,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Divider(color = GoldAccent.copy(alpha = 0.2f))

                // Scroll selection items
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    options.forEach { option ->
                        val isSelected = option == selectedValue
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(option) }
                                .padding(vertical = 12.dp, horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = option,
                                color = if (isSelected) GoldAccent else CreamText,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.LocalFireDepartment,
                                    contentDescription = "Selected",
                                    tint = GoldAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = CardLuxury, contentColor = MutedText),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close", fontSize = 12.sp)
                }
            }
        }
    }
}

// Log Spray dialog overlay
@Composable
fun LogSprayModal(
    bottle: Bottle,
    onDismiss: () -> Unit,
    onConfirmLog: (Int, String, String) -> Unit
) {
    var sprayCount by remember { mutableStateOf(3) }
    var notes by remember { mutableStateOf("") }
    val context = LocalContext.current

    var selectedDate by remember {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
        mutableStateOf(format.format(Date()))
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceLuxury),
            border = BorderStroke(0.5.dp, GoldAccent.copy(alpha = 0.3f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Text(
                    text = "Log Spray Usage",
                    color = MutedText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = bottle.name,
                    color = CreamText,
                    fontFamily = FontFamily.Serif,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = bottle.house.uppercase(Locale.ROOT),
                    color = GoldAccent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Spray Quantity Selector with big text
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = { if (sprayCount > 1) sprayCount-- },
                        modifier = Modifier
                            .size(44.dp)
                            .background(CardLuxury, CircleShape)
                            .border(BorderStroke(0.5.dp, GoldAccent.copy(alpha = 0.4f)), CircleShape)
                    ) {
                        Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = "Decrease", tint = GoldAccent)
                    }

                    Text(
                        text = sprayCount.toString(),
                        color = CreamText,
                        fontFamily = FontFamily.Serif,
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )

                    IconButton(
                        onClick = { sprayCount++ },
                        modifier = Modifier
                            .size(44.dp)
                            .background(CardLuxury, CircleShape)
                            .border(BorderStroke(0.5.dp, GoldAccent.copy(alpha = 0.4f)), CircleShape)
                    ) {
                        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Increase", tint = GoldAccent)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Calculated values: ML of usage and Cost
                val mlUsed = sprayCount * bottle.mlPerSpray
                val costOfUsage = (bottle.price / (bottle.sizeMl / bottle.mlPerSpray)) * sprayCount
                val currencySymbols = mapOf("USD" to "$", "EUR" to "€", "GBP" to "£", "AED" to "د.إ", "CHF" to "Fr")
                val symbol = currencySymbols[bottle.currency] ?: bottle.currency

                Text(
                    text = "${String.format("%.2f", mlUsed)}ml used  •  Cost of usage: $symbol${String.format("%.2f", costOfUsage)}",
                    color = GoldAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(20.dp))
                Divider(color = GoldAccent.copy(alpha = 0.15f))
                Spacer(modifier = Modifier.height(16.dp))

                // Date Picker trigger button / row style
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(text = "DATE", color = MutedText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .background(CardLuxury, RoundedCornerShape(8.dp))
                            .border(BorderStroke(0.5.dp, MutedText.copy(alpha = 0.4f)), RoundedCornerShape(8.dp))
                            .clickable {
                                val cal = Calendar.getInstance()
                                val dateParts = selectedDate.split("-")
                                if (dateParts.size == 3) {
                                    cal.set(Calendar.YEAR, dateParts[0].toInt())
                                    cal.set(Calendar.MONTH, dateParts[1].toInt() - 1)
                                    cal.set(Calendar.DAY_OF_MONTH, dateParts[2].toInt())
                                }
                                DatePickerDialog(
                                    context,
                                    { _, y, m, d ->
                                        selectedDate = String.format("%04d-%02d-%02d", y, m + 1, d)
                                    },
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            }
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = selectedDate, color = CreamText, fontSize = 13.sp)
                        Icon(imageVector = Icons.Default.DateRange, contentDescription = "Date", tint = GoldAccent, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Log notes (Where did you wear it?)", fontSize = 11.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = CreamText,
                        unfocusedTextColor = CreamText,
                        focusedBorderColor = GoldAccent,
                        unfocusedBorderColor = MutedText.copy(alpha = 0.4f),
                        focusedLabelColor = GoldAccent,
                        unfocusedLabelColor = MutedText
                    ),
                    textStyle = TextStyle(fontSize = 12.sp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Action buttons row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        border = BorderStroke(1.dp, GoldAccent),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldAccent),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Text("Cancel", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            onConfirmLog(sprayCount, selectedDate, notes)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = BgLuxury),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1.2f)
                            .height(44.dp)
                            .testTag("btn_log_sprays_submit")
                    ) {
                        Text("Log $sprayCount Sprays", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Analytics View Tab containing KPI blocks and Plain Views Bar Chart
@Composable
fun AnalyticsTab(
    bottles: List<BottleWithLogs>
) {
    if (bottles.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                Icon(imageVector = Icons.Default.Analytics, contentDescription = null, tint = MutedText.copy(alpha = 0.3f), modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Insufficient Data", color = CreamText, fontFamily = FontFamily.Serif, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Add bottles to your collection and record spray logs to generate beautiful visual analytics diagrams.", color = MutedText, fontSize = 12.sp, textAlign = TextAlign.Center)
            }
        }
    } else {
        // Compute calculated parameters
        val totalBottles = bottles.size
        val totalSpraysLogged = bottles.sumOf { it.spraysUsed }
        val totalSessions = bottles.sumOf { it.sessionsCount }
        val avgSpraysPerSession = if (totalSessions > 0) totalSpraysLogged.toDouble() / totalSessions else 0.0

        // Investment calculations (with multi-currency summary support!)
        val investmentMap = bottles.groupBy { it.bottle.currency }.mapValues { entry ->
            entry.value.sumOf { it.bottle.price }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Text(
                    text = "Atelier Analytics",
                    color = CreamText,
                    fontFamily = FontFamily.Serif,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Cards KPI grids
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        KpiCard(title = "BOTTLES", value = totalBottles.toString(), subtext = "Olfactory assets", modifier = Modifier.weight(1f))
                        KpiCard(title = "SPRAYS LOGGED", value = totalSpraysLogged.toString(), subtext = "Total sprays used", modifier = Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        KpiCard(title = "SESSIONS", value = totalSessions.toString(), subtext = "Active wearings", modifier = Modifier.weight(1f))
                        KpiCard(title = "SPRAYS / SESSION", value = String.format("%.1f", avgSpraysPerSession), subtext = "Average dosage", modifier = Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    // Investment block (multicuries summarized beautifully)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CardLuxury),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(text = "TOTAL INVESTMENT", color = MutedText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            if (investmentMap.isEmpty()) {
                                Text(text = "$0.00", color = GoldAccent, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    val formattedList = investmentMap.map { "${it.key} ${it.value.toInt()}" }
                                    Text(
                                        text = formattedList.joinToString("  |  "),
                                        color = GoldAccent,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Monthly progress usage chart (Vertical bar styling using pure view elements)
            item {
                MonthlyUsageChart(bottles = bottles)
            }

            // Fragrance usage intensity horizontals
            item {
                UsageIntensityHorizontals(bottles = bottles)
            }

            // Cost analysis table
            item {
                CostAnalysisTable(bottles = bottles)
            }
        }
    }
}

// Small KPI Grid Box
@Composable
fun KpiCard(
    title: String,
    value: String,
    subtext: String,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardLuxury),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Text(text = title, color = MutedText, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = value, color = CreamText, fontFamily = FontFamily.Serif, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtext, color = MutedText, fontSize = 10.sp)
        }
    }
}

// 6-Month Usage chart built with plain views beautifully
@Composable
fun MonthlyUsageChart(
    bottles: List<BottleWithLogs>
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardLuxury),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "MONTHLY USAGE LOG (LAST 6 MONTHS)",
                color = MutedText,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(18.dp))

            // Obtain list of last 6 months in chronological order
            val calendar = Calendar.getInstance()
            val format = SimpleDateFormat("yyyy-MM", Locale.ROOT)
            val monthLabelFormat = SimpleDateFormat("MMM", Locale.getDefault())

            val last6MonthsData = (0..5).reversed().map { offset ->
                val cal = Calendar.getInstance()
                cal.add(Calendar.MONTH, -offset)
                val monthYm = format.format(cal.time)
                val monthLabel = monthLabelFormat.format(cal.time)

                // Sum sprays logged during this month key
                val sumSprays = bottles.flatMap { it.logs }
                    .filter { it.date.startsWith(monthYm) }
                    .sumOf { it.sprays }

                monthLabel to sumSprays
            }

            val maxSprays = last6MonthsData.maxOfOrNull { it.second } ?: 1
            val chartMax = if (maxSprays <= 0) 10 else maxSprays // non-zero baseline

            // Chart display grid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                last6MonthsData.forEach { (label, sprays) ->
                    val progressFraction = (sprays.toFloat() / chartMax.toFloat()).coerceIn(0.01f, 1f)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (sprays > 0) sprays.toString() else "0",
                            color = if (sprays > 0) GoldAccent else MutedText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        // Tall view column representing the volume block
                        Box(
                            modifier = Modifier
                                .width(16.dp)
                                .fillMaxHeight(progressFraction)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            GoldAccent,
                                            GoldAccent.copy(alpha = 0.5f)
                                        )
                                    )
                                )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = GoldAccent.copy(alpha = 0.15f), thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(6.dp))

            // Bottom Month labels Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                last6MonthsData.forEach { (label, _) ->
                    Text(
                        text = label.uppercase(Locale.ROOT),
                        color = CreamText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

// Usage intensity lines for fragrances
@Composable
fun UsageIntensityHorizontals(
    bottles: List<BottleWithLogs>
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardLuxury),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "USAGE INTENSITY BY FRAGRANCE (SPRAYS)",
                color = MutedText,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(16.dp))

            val sortedList = bottles.filter { it.spraysUsed > 0 }.sortedByDescending { it.spraysUsed }.take(5)

            if (sortedList.isEmpty()) {
                Text(
                    text = "No sprays logged yet.",
                    color = MutedText,
                    fontSize = 11.sp,
                    fontStyle = FontStyle.Italic,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                val topSprays = sortedList.first().spraysUsed

                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    sortedList.forEach { item ->
                        val ratio = if (topSprays > 0) item.spraysUsed.toFloat() / topSprays.toFloat() else 0f
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${item.bottle.house} ${item.bottle.name}",
                                    color = CreamText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${item.spraysUsed} sprays",
                                    color = GoldAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))

                            // Horizontal Bar View representation
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(SurfaceLuxury)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(ratio.coerceIn(0.01f, 1f))
                                        .height(8.dp)
                                        .background(GoldAccent)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Cost Analysis Scrollable table
@Composable
fun CostAnalysisTable(
    bottles: List<BottleWithLogs>
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardLuxury),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "COST ANALYSIS DATABASE",
                color = MutedText,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(14.dp))

            // Headers row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "FRAGRANCE", color = MutedText, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.8f))
                Text(
                    text = "QTY LEFT",
                    color = MutedText,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1.2f)
                )
                Text(
                    text = "COST/SPRAY",
                    color = MutedText,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f)
                )
            }
            Divider(color = GoldAccent.copy(alpha = 0.15f), thickness = 0.5.dp)

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                bottles.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Thumbnail + Title area
                        Row(
                            modifier = Modifier.weight(1.8f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(SurfaceLuxury)
                                    .border(BorderStroke(0.5.dp, GoldAccent.copy(alpha = 0.2f)), RoundedCornerShape(4.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (item.bottle.imageUrl?.isNotBlank() == true) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(item.bottle.imageUrl)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Text(
                                        text = item.bottle.house.take(1).uppercase(Locale.ROOT),
                                        color = GoldAccent,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.bottle.name,
                                    color = CreamText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${item.bottle.house} • ${item.bottle.concentration}",
                                    color = MutedText,
                                    fontSize = 9.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Progress indicator bar + stats ML left
                        Column(
                            modifier = Modifier.weight(1.2f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${item.percentRemaining.toInt()}% remaining",
                                color = if (item.percentRemaining < 20.0) RedWarning else CreamText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            // Small progress bar indicator
                            Box(
                                modifier = Modifier
                                    .width(64.dp)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(SurfaceLuxury)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth((item.percentRemaining / 100.0).toFloat())
                                        .height(4.dp)
                                        .background(if (item.percentRemaining < 20) RedWarning else GoldAccent)
                                )
                            }
                        }

                        // Cost sprays
                        val currencySymbols = mapOf("USD" to "$", "EUR" to "€", "GBP" to "£", "AED" to "د.إ", "CHF" to "Fr")
                        val symbol = currencySymbols[item.bottle.currency] ?: item.bottle.currency
                        val costFormatted = String.format("%.2f", item.costPerSpray)
                        Text(
                            text = "$symbol$costFormatted",
                            color = GoldAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}
