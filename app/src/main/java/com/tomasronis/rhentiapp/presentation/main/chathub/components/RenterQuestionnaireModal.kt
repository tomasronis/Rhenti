package com.tomasronis.rhentiapp.presentation.main.chathub.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Modal dialog to display renter questionnaire/assessment data.
 */
@Composable
fun RenterQuestionnaireModal(
    renterAssessment: Map<String, Any>?,
    bookingInfo: Map<String, Any>?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = true
        )
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Renter Questionnaire",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                HorizontalDivider()

                // Scrollable content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Contact Information Section
                    bookingInfo?.let { info ->
                        SectionHeader("Contact Information")

                        QuestionnaireItem(
                            label = "Full Name",
                            value = info["fullName"] as? String ?: "Not provided"
                        )
                        QuestionnaireItem(
                            label = "Email",
                            value = info["email"] as? String ?: "Not provided"
                        )
                        QuestionnaireItem(
                            label = "Phone",
                            value = info["phone"] as? String ?: "Not provided"
                        )
                        QuestionnaireItem(
                            label = "Working with a Realtor",
                            value = if (info["realtor"] as? Boolean == true) "Yes" else "No"
                        )
                    }

                    // Renter Assessment Section
                    renterAssessment?.let { assessment ->
                        Spacer(modifier = Modifier.height(8.dp))
                        SectionHeader("Renter Information")

                        QuestionnaireItem(
                            label = "Employer",
                            value = assessment["employerName"] as? String ?: "Not provided"
                        )
                        QuestionnaireItem(
                            label = "Job Title",
                            value = assessment["title"] as? String ?: "Not provided"
                        )
                        QuestionnaireItem(
                            label = "Employer Industry",
                            value = assessment["employer_industry"] as? String ?: "Not provided"
                        )
                        QuestionnaireItem(
                            label = "Employment Terms",
                            value = formatEmploymentTerms(assessment["employment_terms"] as? String)
                        )
                        QuestionnaireItem(
                            label = "Annual Income",
                            value = assessment["annual_income"] as? String ?: "Not provided"
                        )
                        QuestionnaireItem(
                            label = "Credit Score",
                            value = assessment["credit_score"]?.toString() ?: "Not provided"
                        )
                        QuestionnaireItem(
                            label = "Years Renting",
                            value = formatYearsRenting(assessment["years_renting"] as? String)
                        )
                        QuestionnaireItem(
                            label = "Co-applicants",
                            value = (assessment["coapps"] as? Number)?.toString() ?: "0"
                        )
                        QuestionnaireItem(
                            label = "Pets",
                            value = if (assessment["pets"] as? Boolean == true) "Yes" else "No"
                        )
                        QuestionnaireItem(
                            label = "Smoking",
                            value = if (assessment["smoking"] as? Boolean == true) "Yes" else "No"
                        )
                    }

                    if (renterAssessment == null && bookingInfo == null) {
                        // Empty state
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No questionnaire data available",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun QuestionnaireItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun formatEmploymentTerms(terms: String?): String {
    return when (terms) {
        "1-2" -> "1-2 years"
        "3-4" -> "3-4 years"
        "5+" -> "5+ years"
        else -> terms ?: "Not provided"
    }
}

private fun formatYearsRenting(years: String?): String {
    return when (years) {
        "0-1" -> "Less than 1 year"
        "2-3" -> "2-3 years"
        "4-5" -> "4-5 years"
        "6+" -> "6+ years"
        else -> years ?: "Not provided"
    }
}
