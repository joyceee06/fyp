package com.fyp.ekopantri.ui.insight

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fyp.ekopantri.ui.inventory.InventoryViewModel

@Composable
fun InsightScreen(viewModel: InventoryViewModel = viewModel()) {
    LaunchedEffect(Unit) {
        viewModel.startListening()
    }

    val consumedData by viewModel.consumedByCategory.collectAsState()
    val discardedData by viewModel.discardedByCategory.collectAsState()
    val wasteVsConsumption by viewModel.wasteVsConsumption.collectAsState()

    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val availableMonths by viewModel.availableMonths.collectAsState()
    var expanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Insights",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                // Month Filter Dropdown
                Box {
                    TextButton(
                        onClick = { expanded = true },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(selectedMonth, fontWeight = FontWeight.Medium)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        availableMonths.forEach { month ->
                            DropdownMenuItem(
                                text = { Text(month) },
                                onClick = {
                                    viewModel.setSelectedMonth(month)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // --- Pie Charts ---
        item {
            CategoryPieChart(title = "Total Waste vs Consumption", data = wasteVsConsumption)
        }
        item {
            CategoryPieChart(title = "Consumed by Category", data = consumedData)
        }
        item {
            CategoryPieChart(title = "Discarded by Category", data = discardedData)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryPieChart(
    title: String,
    data: Map<String, Int>,
    modifier: Modifier = Modifier
) {
    val totalCount = data.values.sum()

    val colors = listOf(
        Color(0xFF66BB6A),
        Color(0xFF42A5F5),
        Color(0xFFFFCA28),
        Color(0xFFFF7043),
        Color(0xFFAB47BC),
        Color(0xFF26C6DA)
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(6.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Title
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            if (totalCount == 0) {
                Box(
                    Modifier
                        .height(160.dp)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No data available",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Spacer(Modifier.height(20.dp))

                // Donut Chart
                Box(contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.size(180.dp)) {
                        var startAngle = -90f

                        data.values.forEachIndexed { index, count ->
                            val sweepAngle = (count.toFloat() / totalCount) * 360f

                            drawArc(
                                color = colors[index % colors.size],
                                startAngle = startAngle,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(
                                    width = 40f
                                )
                            )
                            startAngle += sweepAngle
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Legend (cleaner)
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    data.entries.forEachIndexed { index, entry ->
                        val percent = (entry.value * 100 / totalCount)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier
                                        .size(12.dp)
                                        .background(
                                            colors[index % colors.size],
                                            CircleShape
                                        )
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(entry.key)
                            }

                            Text(
                                "${entry.value} ($percent%)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}