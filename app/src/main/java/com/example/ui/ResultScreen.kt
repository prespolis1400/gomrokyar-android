package com.example.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.logic.CalculationResult
import com.example.util.DateTimeHelper
import com.example.util.ExportHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    result: CalculationResult,
    applyDiscount: Boolean,
    onDiscountToggle: () -> Unit,
    onNavigateBack: () -> Unit,
    onShare: (String) -> Unit,
    currentFont: String = "Vazirmatn",
    currentScale: Float = 1.0f,
    onFontChange: (String) -> Unit = {},
    onScaleChange: (Float) -> Unit = {}
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var showFontDialog by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    
    val onCopy: (String, String) -> Unit = { value, label ->
        clipboardManager.setText(AnnotatedString(value))
        coroutineScope.launch {
            snackbarHostState.showSnackbar(
                message = "$label کپی شد",
                duration = SnackbarDuration.Short
            )
        }
    }

    if (showFontDialog) {
        FontSettingsDialog(
            currentFont = currentFont,
            currentScale = currentScale,
            onFontChange = onFontChange,
            onScaleChange = onScaleChange,
            onDismiss = { showFontDialog = false }
        )
    }

    val box042Details = if (result.box042Chips.isNotEmpty()) {
        "\nریز اقلام باکس ۰۴۲:\n" + result.box042Chips.joinToString("\n") { " - ${it.name}: ${formatNumber(it.cost)} ریال" }
    } else ""

    val dateInfo = DateTimeHelper.getDualDateStringForShareText()
    val shareText = """
        نتیجه محاسبات گمرک (${if (result.mode == "taavoni") "تعاونی مرزنشینان" else "تجاری آزاد"})
        $dateInfo
        ---
        بهای تمام‌شده هر کیلوگرم: ${formatNumber(result.costPerKg)} ریال
        جمع کل پرداختی: ${formatNumber(result.totalPayment)} ریال
        ---
        بیمه حمل: ${formatNumber(result.insurance)} ریال
        ارزش کل ریالی (سیف): ${formatNumber(result.cifValue)} ریال
        حقوق ورودی: ${formatNumber(result.tariff)} ریال
        ارزش افزوده (۱۰٪): ${formatNumber(result.vat)} ریال
        مالیات ۲٪ علی‌الحساب: ${formatNumber(result.tax4)} ریال
        عوارض استاندارد: ${formatNumber(result.standard)} ریال
        عوارض غذا و دارو: ${formatNumber(result.foodDrug)} ریال
        تجمیع عوارض باکس ۰۴۲: ${formatNumber(result.box042Total)} ریال$box042Details
        ${if (result.mode == "taavoni") "باکس ۰۴۹ (تخفیف): - ${formatNumber(result.box049)} ریال\n" else ""}
    """.trimIndent()

    // Share Options Dialog
    if (showShareDialog) {
        AlertDialog(
            onDismissRequest = { showShareDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("اشتراک‌گذاری نتایج محاسبه", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "قالب مورد نظر را برای ارسال انتخاب کنید:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Option 1: PDF Document
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showShareDialog = false
                                isExporting = true
                                coroutineScope.launch {
                                    try {
                                        val uri = withContext(Dispatchers.IO) {
                                            ExportHelper.generateResultPdf(context, result)
                                        }
                                        ExportHelper.shareContent(context, uri, "application/pdf", "گزارش رسمی محاسبات گمرک")
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar("خطا در ایجاد فایل PDF: ${e.message}")
                                    } finally {
                                        isExporting = false
                                    }
                                }
                            },
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("📄 فایل کامل و رسمی PDF", fontWeight = FontWeight.Bold)
                                Text(
                                    "شامل کلیه ردیف‌ها، ریز اقلام ۰۴۲ و جدول استاندارد",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Option 2: High-Resolution Screenshot Image
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showShareDialog = false
                                isExporting = true
                                coroutineScope.launch {
                                    try {
                                        val uri = withContext(Dispatchers.IO) {
                                            ExportHelper.generateResultBitmap(context, result)
                                        }
                                        ExportHelper.shareContent(context, uri, "image/png", "تصویر اسکرین‌شات محاسبات گمرک")
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar("خطا در ایجاد تصویر: ${e.message}")
                                    } finally {
                                        isExporting = false
                                    }
                                }
                            },
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Image,
                                contentDescription = null,
                                tint = Color(0xFF0284C7),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("🖼️ اسکرین‌شات و تصویر با کیفیت", fontWeight = FontWeight.Bold)
                                Text(
                                    "طراحی گرافیکی مدرن با وضوح بالا برای پیام‌رسان‌ها",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Option 3: Both PDF & Image
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showShareDialog = false
                                isExporting = true
                                coroutineScope.launch {
                                    try {
                                        val (pdfUri, imgUri) = withContext(Dispatchers.IO) {
                                            val p = ExportHelper.generateResultPdf(context, result)
                                            val i = ExportHelper.generateResultBitmap(context, result)
                                            Pair(p, i)
                                        }
                                        val uris = arrayListOf(pdfUri, imgUri)
                                        ExportHelper.shareMultiple(context, uris, "گزارش کامل گمرک (PDF + تصویر)")
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar("خطا در آماده‌سازی فایل‌ها: ${e.message}")
                                    } finally {
                                        isExporting = false
                                    }
                                }
                            },
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.AttachFile,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("📎 ارسال همزمان (PDF + تصویر)", fontWeight = FontWeight.Bold)
                                Text(
                                    "ارسال یکجای سند PDF و تصویر جهت بایگانی و ارائه",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Option 4: Plain Text
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showShareDialog = false
                                onShare(shareText)
                            },
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Description,
                                contentDescription = null,
                                tint = Color(0xFF8B5CF6),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("📝 متن خلاصه", fontWeight = FontWeight.Bold)
                                Text(
                                    "ارسال به صورت پیام متنی استاندارد",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showShareDialog = false }) {
                    Text("انصراف")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("نتایج محاسبه", fontWeight = FontWeight.Medium) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showFontDialog = true }) {
                        Icon(Icons.Default.FormatSize, contentDescription = "تنظیم قلم و اندازه متن")
                    }
                    IconButton(onClick = { showShareDialog = true }) {
                        Icon(Icons.Default.Share, contentDescription = "اشتراک‌گذاری")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth()
                                .clickable {
                                    onCopy(result.costPerKg.toString(), "بهای هر کیلوگرم")
                                },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("بهای تمام‌شده هر کیلوگرم خالص", color = MaterialTheme.colorScheme.onPrimary)
                            Text(
                                text = "${formatNumber(result.costPerKg)} ریال",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }

                    DetailRowCopyable("بیمه حمل", result.insurance, onCopy)
                    DetailRowCopyable("ارزش کل ریالی (سیف)", result.cifValue, onCopy)
                    DetailRowCopyable("حقوق ورودی", result.tariff, onCopy)
                    DetailRowCopyable("ارزش افزوده (۱۰٪)", result.vat, onCopy)
                    DetailRowCopyable("مالیات ۲٪ علی‌الحساب", result.tax4, onCopy)
                    DetailRowCopyable("عوارض استاندارد", result.standard, onCopy)
                    DetailRowCopyable("غذا و دارو", result.foodDrug, onCopy)

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onCopy(result.box042Total.toString(), "تجمیع باکس ۰۴۲")
                                    },
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("تجمیع عوارض باکس ۰۴۲", fontWeight = FontWeight.Bold)
                                Text("${formatNumber(result.box042Total)} ریال")
                            }
                            if (result.box042Chips.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                                    val itemFontSize = (maxWidth.value / 32f).coerceIn(10f, 14f).sp
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        result.box042Chips.forEach { chip ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        onCopy(chip.cost.toString(), chip.name)
                                                    },
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = chip.name,
                                                    fontSize = itemFontSize,
                                                    color = Color.Gray,
                                                    maxLines = 1
                                                )
                                                Text(
                                                    text = "${formatNumber(chip.cost)} ریال",
                                                    fontSize = itemFontSize
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (result.mode == "taavoni") {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth()
                                    .clickable {
                                        onCopy(result.box049.toString(), "باکس ۰۴۹")
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("باکس ۰۴۹ (تخفیف)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                    Text("- ${formatNumber(result.box049)} ریال", color = MaterialTheme.colorScheme.onSecondaryContainer)
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                                .clickable { onDiscountToggle() }
                                .padding(8.dp)
                        ) {
                            Checkbox(
                                checked = applyDiscount,
                                onCheckedChange = { onDiscountToggle() }
                            )
                            Text(
                                "اعمال تخفیف ۵۰٪ حقوق ورودی",
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onCopy(result.totalPayment.toString(), "جمع کل پرداختی")
                            },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (result.mode == "taavoni") "جمع پرداختی (تعاونی)" else "جمع کل پرداختی",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "${formatNumber(result.totalPayment)} ریال",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Quick Export & Share Action Buttons
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "اشتراک و خروجی گزارش",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    isExporting = true
                                    try {
                                        val uri = withContext(Dispatchers.IO) {
                                            ExportHelper.generateResultPdf(context, result)
                                        }
                                        ExportHelper.shareContent(context, uri, "application/pdf", "گزارش رسمی محاسبات گمرک")
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar("خطا در ایجاد PDF: ${e.message}")
                                    } finally {
                                        isExporting = false
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            enabled = !isExporting
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ارسال PDF", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    isExporting = true
                                    try {
                                        val uri = withContext(Dispatchers.IO) {
                                            ExportHelper.generateResultBitmap(context, result)
                                        }
                                        ExportHelper.shareContent(context, uri, "image/png", "تصویر نتایج گمرک")
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar("خطا در ایجاد تصویر: ${e.message}")
                                    } finally {
                                        isExporting = false
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            enabled = !isExporting
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("اسکرین‌شات", fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedButton(
                        onClick = { showShareDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isExporting
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("سایر گزینه‌ها (PDF + عکس + متن)...")
                    }

                    if (isExporting) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRowCopyable(title: String, value: Long, onCopy: (String, String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onCopy(value.toString(), title)
            }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("${formatNumber(value)} ریال", fontWeight = FontWeight.Medium)
    }
}
