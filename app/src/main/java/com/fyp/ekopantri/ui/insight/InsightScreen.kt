package com.fyp.ekopantri.ui.insight

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fyp.ekopantri.ui.theme.*

@Composable
fun InsightScreen(viewModel: InsightViewModel = viewModel()) {
    val filterType by viewModel.filterType.collectAsState()
    val filterValue by viewModel.filterValue.collectAsState()
    val filterOptions by viewModel.filterOptions.collectAsState()

    val wasteVsConsumption by viewModel.wasteVsConsumption.collectAsState()
    val consumedData by viewModel.consumedByItem.collectAsState()
    val discardedData by viewModel.discardedByItem.collectAsState()

    Scaffold(
        containerColor = CreamBg
    ) { padding ->
        InsightListContent(
            modifier = Modifier.padding(padding),
            filterType = filterType,
            filterValue = filterValue,
            filterOptions = filterOptions,
            wasteVsConsumption = wasteVsConsumption,
            consumedData = consumedData,
            discardedData = discardedData,
            onFilterTypeChange = viewModel::onFilterTypeChange,
            onFilterValueChange = viewModel::onFilterValueChange
        )
    }
}

// --- MAIN CONTENT COMPONENT ---

@Composable
fun InsightListContent(
    modifier: Modifier = Modifier,
    filterType: InsightViewModel.TimeRange,
    filterValue: String,
    filterOptions: List<String>,
    wasteVsConsumption: Map<String, Int>,
    consumedData: Map<String, Int>,
    discardedData: Map<String, Int>,
    onFilterTypeChange: (InsightViewModel.TimeRange) -> Unit,
    onFilterValueChange: (String) -> Unit
) {
    val totalConsumed = wasteVsConsumption["consumed"] ?: 0
    val totalDiscarded = wasteVsConsumption["discarded"] ?: 0
    val wasteRate = calculateWasteRate(totalConsumed, totalDiscarded)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item { InsightHeader() }

        item {
            InsightFilterSection(
                filterType = filterType,
                filterValue = filterValue,
                filterOptions = filterOptions,
                onTypeChange = onFilterTypeChange,
                onValueChange = onFilterValueChange
            )
        }

        item {
            InsightStatCards(
                consumed = totalConsumed,
                discarded = totalDiscarded,
                wasteRate = wasteRate
            )
        }

        item {
            DonutChartCard(
                title = "Consumption vs Waste",
                data = wasteVsConsumption,
                colors = listOf(ForestGreen, Color.Red)
            )
        }

        item {
            HorizontalBarChartCard(
                title = "Most Consumed",
                subtitle = "Top items you eat",
                data = consumedData,
                color = ForestGreen
            )
        }

        item {
            HorizontalBarChartCard(
                title = "Most Wasted",
                subtitle = "Top items you waste",
                data = discardedData,
                color = Color.Red
            )
        }

        item { Spacer(modifier = Modifier.height(40.dp)) }
    }
}

// --- SUB-COMPOSABLES ---

@Composable
fun InsightHeader() {
    Column {
        Text(
            text = "Sustainability Insights",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = DarkForest
        )
        Text(
            text = "Track your impact on food waste reduction",
            style = MaterialTheme.typography.bodySmall,
            color = MutedGray
        )
    }
}

@Composable
fun InsightFilterSection(
    filterType: InsightViewModel.TimeRange,
    filterValue: String,
    filterOptions: List<String>,
    onTypeChange: (InsightViewModel.TimeRange) -> Unit,
    onValueChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            InsightViewModel.TimeRange.values().forEach { type ->
                val isSelected = filterType == type
                FilterChip(
                    selected = isSelected,
                    onClick = { onTypeChange(type) },
                    label = { Text(type.displayName) },
                    shape = CircleShape,
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = if (isSelected) ForestGreen else Color.White,
                        labelColor = if (isSelected) Color.White else MutedGray
                    )
                )
            }
        }

        if (filterType != InsightViewModel.TimeRange.ALL_TIME) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filterOptions) { option ->
                    val isSelected = filterValue == option
                    AssistChip(
                        onClick = { onValueChange(option) },
                        label = { Text(option) },
                        border = AssistChipDefaults.assistChipBorder(
                            enabled = true,
                            borderColor = if (isSelected) ForestGreen else Color.LightGray,
                            borderWidth = if (isSelected) 2.dp else 1.dp
                        ),
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (isSelected) ForestGreen.copy(0.1f) else Color.White,
                            labelColor = if (isSelected) ForestGreen else MutedGray
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun InsightStatCards(consumed: Int, discarded: Int, wasteRate: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatCard("Eaten", consumed.toString(), Icons.Default.Restaurant, ForestGreen, Modifier.weight(1f))
        StatCard("Wasted", discarded.toString(), Icons.Default.Delete, Color.Red, Modifier.weight(1f))
        StatCard("Waste %", "$wasteRate%",
            Icons.AutoMirrored.Filled.TrendingUp, Color.DarkGray, Modifier.weight(1f))
    }
}

@Composable
fun StatCard(label: String, value: String, icon: ImageVector, color: Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.LightGray.copy(0.3f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(color.copy(0.1f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
            }
            Text(label, style = MaterialTheme.typography.labelSmall, color = MutedGray, modifier = Modifier.padding(top = 8.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = DarkForest)
        }
    }
}

@Composable
fun HorizontalBarChartCard(title: String, subtitle: String, data: Map<String, Int>, color: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.LightGray.copy(0.3f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DarkForest)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MutedGray)

            Spacer(modifier = Modifier.height(20.dp))

            if (data.isEmpty()) {
                Text(
                    text = "No data for this period",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                    textAlign = TextAlign.Center
                )
            } else {
                val maxValue = data.values.maxOrNull() ?: 1
                data.entries.sortedByDescending { it.value }.take(5).forEach { entry ->
                    Row(
                        modifier = Modifier.padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(entry.key, modifier = Modifier.width(85.dp), style = MaterialTheme.typography.bodySmall, maxLines = 1)
                        Box(modifier = Modifier.weight(1f).height(12.dp)) {
                            Box(modifier = Modifier.fillMaxSize().background(Color.LightGray.copy(0.2f), CircleShape))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(entry.value.toFloat() / maxValue)
                                    .fillMaxHeight()
                                    .background(color, CircleShape)
                            )
                        }
                        Text("${entry.value}", modifier = Modifier.padding(start = 8.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun DonutChartCard(title: String, data: Map<String, Int>, colors: List<Color>) {
    val total = data.values.sum()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.LightGray.copy(0.3f))
    ) {
        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DarkForest)
            Spacer(Modifier.height(20.dp))

            if (total == 0) {
                Text("No activity tracked", color = MutedGray, style = MaterialTheme.typography.bodySmall)
            } else {
                Box(contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.size(150.dp)) {
                        var startAngle = -90f
                        data.values.forEachIndexed { index, count ->
                            val sweepAngle = (count.toFloat() / total) * 360f
                            drawArc(
                                color = colors[index % colors.size],
                                startAngle = startAngle,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                style = Stroke(width = 30f)
                            )
                            startAngle += sweepAngle
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Total", style = MaterialTheme.typography.labelSmall, color = MutedGray)
                        Text("$total", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Legend
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    data.keys.forEachIndexed { index, key ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 10.dp)) {
                            Box(Modifier.size(10.dp).background(colors[index % colors.size], CircleShape))
                            Spacer(Modifier.width(6.dp))
                            Text(key.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

// --- LOGIC HELPERS ---

private fun calculateWasteRate(consumed: Int, discarded: Int): Int {
    val total = consumed + discarded
    return if (total > 0) (discarded * 100 / total) else 0
}
