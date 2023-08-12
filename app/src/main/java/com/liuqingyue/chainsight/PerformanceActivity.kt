package com.liuqingyue.chainsight

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.liuqingyue.chainsight.ui.theme.ChainsightTheme
import com.patrykandpatryk.vico.compose.axis.horizontal.bottomAxis
import com.patrykandpatryk.vico.compose.chart.Chart
import com.patrykandpatryk.vico.compose.chart.column.columnChart
import com.patrykandpatryk.vico.compose.chart.line.lineChart
import com.patrykandpatryk.vico.compose.component.shape.roundedCornerShape
import com.patrykandpatryk.vico.compose.component.shape.textComponent
import com.patrykandpatryk.vico.compose.dimensions.dimensionsOf
import com.patrykandpatryk.vico.compose.style.ChartStyle
import com.patrykandpatryk.vico.compose.style.ProvideChartStyle
import com.patrykandpatryk.vico.core.axis.AxisPosition
import com.patrykandpatryk.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatryk.vico.core.chart.decoration.ThresholdLine
import com.patrykandpatryk.vico.core.component.shape.ShapeComponent
import com.patrykandpatryk.vico.core.component.shape.Shapes
import com.patrykandpatryk.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatryk.vico.core.entry.FloatEntry
import com.patrykandpatryk.vico.core.extension.copyColor
import java.time.LocalDate
import java.time.format.DateTimeFormatter

var accoutsName = emptyArray<String>()
class PerformanceActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = AppDatabase.getDatabase(this).getAccountDao()
        val accounts = db.getAll()

        var accountList = mutableListOf<Pair<String,Float>>()

        var tempName = mutableListOf<String>()
        for(account in accounts){
            accountList.add(Pair(account.name,account.balance.toFloat()))
            tempName.add (account.name)
        }
        if(tempName.size == 0){
            accoutsName = arrayOf("No Account")
        }
        accoutsName = tempName.toTypedArray()
        val chartEntryModelProducer2 = accountList.toList().mapIndexed { index, (dateString, y) ->
            FloatEntry(
                x = index.toFloat(),
                y = y,
            )
        }.let { entryCollection -> ChartEntryModelProducer(entryCollection) }

        val maxPerformance = performances.maxBy { it.total }
        val minPerformance = performances.minBy { it.total }
        var performanceList = mutableListOf<Pair<String,Float>>()
        for (performance in performances){
            performanceList.add( Pair(performance.time, performance.total.toFloat()))
        }

        val chartEntryModelProducer = performanceList.toList().mapIndexed { index, (dateString, y) ->
            Entry(
                localDate = LocalDate.parse(dateString),
                x = index.toFloat(),
                y = y,
            )
        }.let { entryCollection -> ChartEntryModelProducer(entryCollection) }

        setContent {
            Column(
                Modifier
                    .background(Color(0xFFF1F1F1))
                    .fillMaxSize()) {
                // A surface container using the 'background' color from the theme
                Row(modifier = Modifier.fillMaxWidth()) {
                    ShowTopBar("Portfolio Performance") // top bar part
                    Row(
                        horizontalArrangement = Arrangement.End, // close button at the right place
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(onClick = {
                            finish() // return to the main page
                        }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "close",
                            )
                        }
                    }

                }
                ShowChart(minPerformance,maxPerformance,chartEntryModelProducer)
                ShowChart2(chartEntryModelProducer2)
            }
        }
    }
}


@Composable
fun ShowChart(minPerformance: Performance, maxPerformance: Performance, chartEntryModelProducer: ChartEntryModelProducer) {
    Card(Modifier.padding(10.dp)) {
        Column(Modifier.padding(5.dp)) {
            Row() {
                Image( painter = painterResource(id = R.drawable.line), contentDescription = "line chart", modifier = Modifier.size(35.dp))

                Text(
                    text = "Performance Net Worth",
                    style = MaterialTheme.typography.h5,
                    modifier = Modifier.padding(top = 5.dp)
                )
            }
            Text(text = "Best Performance: $${maxPerformance.total} at ${maxPerformance.time}",
                modifier = Modifier.padding(0.dp, 6.dp, 4.dp, 12.dp),
                style = MaterialTheme.typography.body1,
                color = Color.DarkGray)
            ShowPerformance2(chartEntryModelProducer,maxPerformance,minPerformance)

        }}
}

@Composable
fun ShowChart2(chartEntryModelProducer: ChartEntryModelProducer) {
    Card(Modifier.padding(10.dp)) {
        Column(Modifier.padding(5.dp)) {
            Row() {
                Image( painter = painterResource(id = R.drawable.bag), contentDescription = "column chart", modifier = Modifier.size(35.dp))

                Text(
                    text = "Portfolio Distribution",
                    style = MaterialTheme.typography.h5,
                    modifier = Modifier.padding(top = 5.dp)
                )
            }
            ShowDistributed(chartEntryModelProducer)
        }}
}

@Composable
fun ShowDistributed(chartEntryModelProducer: ChartEntryModelProducer){
    // create a List contain two colors
    val colors = listOf(
        Color(0xFF00BFA5),
        Color(0xFF00BFA5)
    )
    val chartStyle = ChartStyle.fromColors(
        Color.Black,
        Color.White,
        Color.LightGray,
        colors,
        Color.LightGray,
    )


    ProvideChartStyle(chartStyle = chartStyle) {
        Chart(
            chart = columnChart(),
            chartModelProducer = chartEntryModelProducer,
            bottomAxis = bottomAxis(valueFormatter = rememberGroupedColumnChartAxisValueFormatter2()),
        )
    }
}


@Composable
fun ShowPerformance2(chartEntryModelProducer: ChartEntryModelProducer, maxPerformance: Performance, minPerformance: Performance) {
    // create a List contain two colors
    val colors = listOf(
        Color(0xFF00BFA5),
        Color(0xFF00BFA5)
    )
    val chartStyle = ChartStyle.fromColors(
        Color.Black,
        Color.White,
        Color.LightGray,
        colors,
        Color.LightGray,
    )
    val labelComponent = textComponent(
        color = androidx.compose.material3.MaterialTheme.colorScheme.surface,
        padding = dimensionsOf(all = 4.dp),
        margins = dimensionsOf(all = 4.dp),
        background = ShapeComponent(
            shape = Shapes.roundedCornerShape(4.dp),
            color = 0xFF68A7AD.toInt(),
            strokeWidthDp = 0f,
        ),
    )
    val line= ThresholdLine(
        thresholdRange = minPerformance.total .. maxPerformance.total.toFloat(),
        labelComponent = labelComponent,
        lineComponent = ShapeComponent(
            color = 0xFF68A7AD.toInt()
                .toInt()
                .copyColor(alpha = 0.16f),
        ),
    )

    ProvideChartStyle(chartStyle = chartStyle) {
        Chart(
            chart = lineChart(decorations = listOf(line)),
            chartModelProducer = chartEntryModelProducer,
            bottomAxis = bottomAxis(valueFormatter = rememberGroupedColumnChartAxisValueFormatter()),
        )
    }
}

@Composable
internal fun rememberGroupedColumnChartAxisValueFormatter2(): AxisValueFormatter<AxisPosition.Horizontal.Bottom> =
    AxisValueFormatter { x, _ -> accoutsName[x.toInt()] }

