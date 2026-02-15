package com.example.gothere.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gothere.data.DestinationConfig

/**
 * Screen displaying downloadable visa pack resources organized by category.
 * Resources are bundled as HTML files in assets/visa_packs/{destination}/.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisaPackScreen(
    destinationId: String = DestinationConfig.SPAIN
) {
    val context = LocalContext.current
    val destination = remember(destinationId) {
        DestinationConfig.getDestination(destinationId)
    }

    // Define visa pack categories and files
    val categories = remember(destinationId) {
        getVisaPackCategories(destinationId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Visa Pack")
                        destination?.let {
                            Text(
                                text = "${it.flagEmoji} ${it.name}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Essential Resources",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Downloadable checklists, templates, and guides for your relocation.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
            }

            items(categories) { category ->
                VisaPackCategoryCard(
                    category = category,
                    onItemClick = { item ->
                        // Open in browser or internal viewer
                        val assetPath = "visa_packs/$destinationId/${item.fileName}"
                        // For now, show a message - in production, open a WebView or PDF viewer
                        // context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("file:///android_asset/$assetPath")))
                    }
                )
            }

            item {
                Spacer(Modifier.height(16.dp))
                
                // Official resources card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Public,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = "Official Government Resources",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        
                        destination?.officialImmigrationUrl?.let { url ->
                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Outlined.OpenInNew, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Official Immigration Portal")
                            }
                        }
                    }
                }
                
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun VisaPackCategoryCard(
    category: VisaPackCategory,
    onItemClick: (VisaPackItem) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    category.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = category.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = category.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))

            category.items.forEach { item ->
                VisaPackItemRow(
                    item = item,
                    onClick = { onItemClick(item) }
                )
            }
        }
    }
}

@Composable
private fun VisaPackItemRow(
    item: VisaPackItem,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    when (item.fileType) {
                        FileType.PDF -> Icons.Outlined.PictureAsPdf
                        FileType.HTML -> Icons.Outlined.Article
                        FileType.CHECKLIST -> Icons.Outlined.Checklist
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    if (item.subtitle != null) {
                        Text(
                            text = item.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Open"
            )
        }
    }
    Spacer(Modifier.height(8.dp))
}

// ==================== Data Models ====================

data class VisaPackCategory(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val items: List<VisaPackItem>
)

data class VisaPackItem(
    val title: String,
    val subtitle: String? = null,
    val fileName: String,
    val fileType: FileType = FileType.HTML
)

enum class FileType {
    PDF, HTML, CHECKLIST
}

// ==================== Category Definitions ====================

private fun getVisaPackCategories(destinationId: String): List<VisaPackCategory> {
    return when (destinationId) {
        DestinationConfig.SPAIN -> getSpainVisaPack()
        DestinationConfig.PORTUGAL -> getPortugalVisaPack()
        DestinationConfig.MEXICO -> getMexicoVisaPack()
        else -> getSpainVisaPack()
    }
}

private fun getSpainVisaPack(): List<VisaPackCategory> = listOf(
    VisaPackCategory(
        title = "Requirements Checklist",
        description = "Complete document checklist for your visa",
        icon = Icons.Outlined.Checklist,
        items = listOf(
            VisaPackItem("Non-Lucrative Visa Checklist", "All required documents", "requirements_nlv.html", FileType.CHECKLIST),
            VisaPackItem("Digital Nomad Visa Checklist", "DNV specific requirements", "requirements_dnv.html", FileType.CHECKLIST),
            VisaPackItem("Financial Requirements", "Income & savings thresholds", "financial_requirements.html")
        )
    ),
    VisaPackCategory(
        title = "Document Templates",
        description = "Sample letters and forms",
        icon = Icons.Outlined.Description,
        items = listOf(
            VisaPackItem("Cover Letter Template", "Visa application cover letter", "cover_letter.html"),
            VisaPackItem("Proof of Funds Letter", "Bank letter template", "bank_letter.html"),
            VisaPackItem("Accommodation Letter", "Host invitation template", "accommodation_letter.html")
        )
    ),
    VisaPackCategory(
        title = "Appointment Prep",
        description = "What to expect at your appointments",
        icon = Icons.Outlined.CalendarMonth,
        items = listOf(
            VisaPackItem("Consulate Appointment Guide", "What to bring & expect", "consulate_appointment.html"),
            VisaPackItem("TIE Appointment Guide", "Cita previa preparation", "tie_appointment.html"),
            VisaPackItem("Common Interview Questions", "Prepare your answers", "interview_questions.html")
        )
    ),
    VisaPackCategory(
        title = "Arrival Setup",
        description = "First steps after landing",
        icon = Icons.Outlined.FlightLand,
        items = listOf(
            VisaPackItem("First Week Checklist", "Priority tasks on arrival", "first_week.html", FileType.CHECKLIST),
            VisaPackItem("Padrón Registration Guide", "Town hall registration", "padron_guide.html"),
            VisaPackItem("Banking Guide", "Opening a Spanish bank account", "banking_guide.html")
        )
    )
)

private fun getPortugalVisaPack(): List<VisaPackCategory> = listOf(
    VisaPackCategory(
        title = "Requirements Checklist",
        description = "Complete document checklist for your visa",
        icon = Icons.Outlined.Checklist,
        items = listOf(
            VisaPackItem("D7 Visa Checklist", "Passive income visa requirements", "requirements_d7.html", FileType.CHECKLIST),
            VisaPackItem("D8 Visa Checklist", "Digital nomad visa requirements", "requirements_d8.html", FileType.CHECKLIST),
            VisaPackItem("Financial Requirements", "Income thresholds by visa type", "financial_requirements.html")
        )
    ),
    VisaPackCategory(
        title = "Document Templates",
        description = "Sample letters and forms",
        icon = Icons.Outlined.Description,
        items = listOf(
            VisaPackItem("Motivation Letter Template", "Why you want to live in Portugal", "motivation_letter.html"),
            VisaPackItem("Income Declaration", "Self-certification of income", "income_declaration.html"),
            VisaPackItem("Accommodation Proof", "Rental agreement requirements", "accommodation_proof.html")
        )
    ),
    VisaPackCategory(
        title = "Appointment Prep",
        description = "What to expect at your appointments",
        icon = Icons.Outlined.CalendarMonth,
        items = listOf(
            VisaPackItem("VFS Appointment Guide", "Biometrics & document submission", "vfs_appointment.html"),
            VisaPackItem("AIMA/SEF Appointment Guide", "Residence permit appointment", "aima_appointment.html")
        )
    ),
    VisaPackCategory(
        title = "Arrival Setup",
        description = "First steps after landing",
        icon = Icons.Outlined.FlightLand,
        items = listOf(
            VisaPackItem("First Week Checklist", "Priority tasks on arrival", "first_week.html", FileType.CHECKLIST),
            VisaPackItem("NIF Guide", "Getting your tax number", "nif_guide.html"),
            VisaPackItem("Banking Guide", "Opening a Portuguese bank account", "banking_guide.html"),
            VisaPackItem("Healthcare Registration", "SNS enrollment guide", "healthcare_guide.html")
        )
    )
)

private fun getMexicoVisaPack(): List<VisaPackCategory> = listOf(
    VisaPackCategory(
        title = "Requirements Checklist",
        description = "Complete document checklist for your visa",
        icon = Icons.Outlined.Checklist,
        items = listOf(
            VisaPackItem("Temporary Resident Checklist", "Economic solvency requirements", "requirements_temp_resident.html", FileType.CHECKLIST),
            VisaPackItem("Financial Requirements", "Bank balance vs income methods", "financial_requirements.html")
        )
    ),
    VisaPackCategory(
        title = "Document Templates",
        description = "Sample letters and forms",
        icon = Icons.Outlined.Description,
        items = listOf(
            VisaPackItem("Employment Verification Letter", "For income method applicants", "employment_letter.html"),
            VisaPackItem("Bank Statement Guidelines", "What consulates look for", "bank_statement_guide.html")
        )
    ),
    VisaPackCategory(
        title = "Appointment Prep",
        description = "What to expect at your appointments",
        icon = Icons.Outlined.CalendarMonth,
        items = listOf(
            VisaPackItem("Consulate Appointment Guide", "What to bring & expect", "consulate_appointment.html"),
            VisaPackItem("INM Canje Guide", "Exchanging visa for resident card", "inm_canje.html")
        )
    ),
    VisaPackCategory(
        title = "Arrival Setup",
        description = "First steps after landing",
        icon = Icons.Outlined.FlightLand,
        items = listOf(
            VisaPackItem("First Week Checklist", "30-day INM deadline tasks", "first_week.html", FileType.CHECKLIST),
            VisaPackItem("CURP Guide", "Getting your Mexican ID number", "curp_guide.html"),
            VisaPackItem("Banking Guide", "Opening a Mexican bank account", "banking_guide.html"),
            VisaPackItem("RFC Guide", "Tax ID for those who need it", "rfc_guide.html")
        )
    )
)
