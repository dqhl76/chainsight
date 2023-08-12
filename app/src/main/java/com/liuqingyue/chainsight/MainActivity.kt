package com.liuqingyue.chainsight

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LiveData
import coil.compose.rememberImagePainter
import coil.transform.RoundedCornersTransformation
import com.patrykandpatryk.vico.compose.axis.horizontal.bottomAxis
import com.patrykandpatryk.vico.compose.chart.Chart
import com.patrykandpatryk.vico.compose.chart.column.columnChart
import com.patrykandpatryk.vico.compose.chart.line.lineChart
import com.patrykandpatryk.vico.compose.component.shape.roundedCornerShape
import com.patrykandpatryk.vico.compose.component.shape.textComponent
import com.patrykandpatryk.vico.compose.component.shapeComponent
import com.patrykandpatryk.vico.compose.dimensions.dimensionsOf
import com.patrykandpatryk.vico.compose.style.ProvideChartStyle
import com.patrykandpatryk.vico.compose.style.ChartStyle
import com.patrykandpatryk.vico.core.axis.AxisPosition
import com.patrykandpatryk.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatryk.vico.core.chart.column.ColumnChart
import com.patrykandpatryk.vico.core.chart.decoration.ThresholdLine
import com.patrykandpatryk.vico.core.component.shape.ShapeComponent
import com.patrykandpatryk.vico.core.component.shape.Shapes
import com.patrykandpatryk.vico.core.component.shape.cornered.Corner
import com.patrykandpatryk.vico.core.component.shape.cornered.CorneredShape
import com.patrykandpatryk.vico.core.component.shape.cornered.CutCornerTreatment
import com.patrykandpatryk.vico.core.component.shape.cornered.RoundedCornerTreatment
import com.patrykandpatryk.vico.core.entry.ChartEntry
import com.patrykandpatryk.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatryk.vico.core.extension.copyColor
import com.zj.refreshlayout.SwipeRefreshLayout
import com.zj.refreshlayout.SwipeRefreshState
import de.charlex.compose.BottomAppBarSpeedDialFloatingActionButton
import de.charlex.compose.FloatingActionButtonItem
import de.charlex.compose.SubSpeedDialFloatingActionButtons
import de.charlex.compose.rememberSpeedDialFloatingActionButtonState
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDate
import java.time.format.DateTimeFormatter


/* Why I import material3 and material2 in the same file?
 * Because, I use a third-party lib to generate a floating button, it use material3
 * And, other files in my project use material2. No material3 env polluted to other file
 */

class Entry(
    val localDate: LocalDate,
    override val x: Float,
    override val y: Float,
) : ChartEntry{
    override fun withY(y: Float) =  Entry (
        localDate = this.localDate,
        x = this.x,
        y = y)
}
var performances = emptyList<Performance>()

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // I use a mix layout, which means it has layout file and using Jetpack compose
        // it can help me divide the layout into 2 parts. one of them need wait database check



        // connect to database
        val db = AppDatabase.getDatabase(this@MainActivity).getAccountDao()

        // use a observer model to update the balance for view model when database changed
        val accountsLiveData = db.getObserverAll()
        val dbPerformance = AppDatabase3.getDatabase(this@MainActivity).getPerformanceDao()
        performances = dbPerformance.getAll()
        Log.e("performance", performances.toString())
        if(performances.size == 0) {
            performances = listOf(Performance(LocalDate.now().toString(), 0.0f))
        }
        performances = performances.sortedBy { it.time }
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

        setContent{
            ShowPullAndRefresh(context = this@MainActivity, accountsLiveData = accountsLiveData, chartEntryModelProducer = chartEntryModelProducer, maxPerformance = maxPerformance, minPerformance = minPerformance)
        }

        // To get eth price and eth change24h, it will be used in the BalanceEthActivity
        val retrofit = Retrofit.Builder()
            .baseUrl("https://www.oklink.com/api/v5/explorer/address/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val api = retrofit.create(AddressApi::class.java)
        // this address have 0.001 weth, I use it to get the eth price and 24h change
        // ETH is the native coin on Ethereum, I cannot get it price
        // So I use weth to instead of it. (1 weth = 1 eth, weth means wrapped eth)
        val aAddressHaveWeth = "0xA4625bbF48Cc1936A4E4613D7514523B05A7e79F"
        api.getAccount(chain = "ETH", address = aAddressHaveWeth, type = "token_20")
            .enqueue(object : Callback<AddressBalanceData> {
                override fun onResponse(
                    call: Call<AddressBalanceData>,
                    response: Response<AddressBalanceData>
                ) {
                    response.let { itResponse ->
                        itResponse.body()?.let { itData ->
                            if(itData.data.size == 0){
                                val sharedPreference = getSharedPreferences("ETH", Context.MODE_PRIVATE)
                                val editor = sharedPreference.edit()
                                editor.putString("price","1513")
                                editor.putString("change24h","1.2")
                                editor.commit()
                                return;
                            }
                            for (i in itData.data[0].tokenList) {
                                if (i.valueUsd != "0") {
                                    // store the info into sharedPreference
                                    Log.d("WETH", i.toString())
                                    val sharedPreference = getSharedPreferences("ETH", Context.MODE_PRIVATE)
                                    val editor = sharedPreference.edit()
                                    editor.putString("price",i.priceUsd)
                                    editor.putString("change24h",i.change24h)
                                    editor.commit()
                                }
                            }
                        }
                    }
                }

                override fun onFailure(call: Call<AddressBalanceData>, t: Throwable) {
                    Log.ERROR
                }
            })


    }
}

fun updatePortfolio(res1: Unit, res2:Unit, res3: Unit, context: MainActivity){
    Log.d("updatePortfolio", "updatePortfolio")
    val db = AppDatabase.getDatabase(context = context).getAccountDao()
    val accounts = db.getAll()
    var totalBalance = 0.0
    for (i in accounts){
        totalBalance += i.balance
    }
    Log.d(LocalDate.now().toString(), totalBalance.toString())
    val dao = AppDatabase3.getDatabase(context = context).getPerformanceDao()
    if(dao.loadByTime(LocalDate.now().toString()) == null) {
        dao.insert(Performance(LocalDate.now().toString(), totalBalance.toFloat()))
    }else{
        dao.update(Performance(LocalDate.now().toString(), totalBalance.toFloat()))
    }
}

@Composable
fun ShowPullAndRefresh(context: MainActivity, accountsLiveData: LiveData<List<Account>>, chartEntryModelProducer: ChartEntryModelProducer, maxPerformance: Performance, minPerformance: Performance) {
    var refreshing by remember { mutableStateOf(false) }
    var deleteView by remember { mutableStateOf(false) }
    LaunchedEffect(refreshing) {
        if (refreshing){
            GlobalScope.launch {
                val cex = async { updateBinance(context) }
                val eth = async { updateEth(context) }
                val man = async { updateManu(context) }
                updatePortfolio(cex.await(),eth.await(),man.await(),context)
            }
            delay(3000)
            refreshing = false
        }
    }
    SwipeRefreshLayout(isRefreshing = refreshing, onRefresh = { refreshing = true },indicator = {
        BallRefreshHeader(state = it)
    }) {
        Column {
            ShowMainTopBar(title = "Board",onDeleteViewChange = { deleteView = it },deleteView = deleteView)
            ShowBalance(accountsLiveData)
            Surface(modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    val intent = Intent(context, PerformanceActivity::class.java)
                    context.startActivity(intent)
                }
                .padding(start = 10.dp, end = 10.dp, top = 5.dp, bottom = 10.dp)) {
                ShowPerformance(chartEntryModelProducer,maxPerformance,minPerformance)
            }
            ShowSurface(context = context, accountsLive = accountsLiveData,deleteView)

        }
    }
}

@Composable
fun ShowBalance(accountsLiveData: LiveData<List<Account>>){
    val accounts = accountsLiveData.observeAsState(initial = emptyList())
    var totalBalance = 0.0
    for (i in accounts.value!!){
        totalBalance += i.balance
    }
    Row(modifier = Modifier
        .fillMaxWidth()
        .padding(start = 18.dp, top = 0.dp, bottom = 0.dp)) {
        Text(text = "$" + String.format("%.0f",totalBalance.toFloat()), fontSize = 40.sp, fontWeight = FontWeight.Bold)
        Text(String.format("%.2f",totalBalance.toFloat()-totalBalance.toInt()).substring(1,4), fontSize = 40.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
    }
}

@Composable
fun ShowPerformance(chartEntryModelProducer: ChartEntryModelProducer, maxPerformance: Performance, minPerformance: Performance) {
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
        color = MaterialTheme.colorScheme.surface,
        padding = dimensionsOf(all = 4.dp),
        margins = dimensionsOf(all = 4.dp),
        background = ShapeComponent(
            shape = Shapes.roundedCornerShape(4.dp),
            color = 0xFF68A7AD.toInt(),
            strokeWidthDp = 0f,
        ),
    )
    val line= ThresholdLine(
        thresholdRange = minPerformance.total .. maxPerformance.total,
        labelComponent = labelComponent,
        lineComponent = ShapeComponent(
            color = 0xFF68A7AD.toInt()
                .toInt()
                .copyColor(alpha = 0.16f),
        ),
    )

    ProvideChartStyle(chartStyle = chartStyle) {
        Chart(
            chart = columnChart(decorations = listOf(line)),
            chartModelProducer = chartEntryModelProducer,
            bottomAxis = bottomAxis(valueFormatter = rememberGroupedColumnChartAxisValueFormatter()),
        )
    }
}

@Composable
internal fun rememberGroupedColumnChartAxisValueFormatter(): AxisValueFormatter<AxisPosition.Horizontal.Bottom> =
    AxisValueFormatter { x, _ -> LocalDate.parse(performances[x.toInt()].time).format(
        DateTimeFormatter.ofPattern("MM/dd")) }

@Composable
fun BallRefreshHeader(state: SwipeRefreshState) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp), contentAlignment = Alignment.Center
    ) {
        val radius = LocalDensity.current.run {
            8.dp.toPx()
        }
        val mCircleSpacing = LocalDensity.current.run {
            8.dp.toPx()
        }
        val mInterpolator = AccelerateDecelerateInterpolator()
        val ballColor = if (state.isRefreshing) Color(0xff33aaff) else Color(0xffeeeeee)
        val mStartTime = if (state.isRefreshing) System.currentTimeMillis() else 0
        var now by remember {
            mutableStateOf(0L)
        }
        var mScale by remember {
            mutableStateOf(1f)
        }
        LaunchedEffect(key1 = state.isRefreshing) {
            if (state.isRefreshing) {
                while (true) {
                    now = System.currentTimeMillis()
                    delay(60)
                }
            }
        }
        Canvas(modifier = Modifier.wrapContentSize()) {
            for (i in 0 until 3) {
                val time: Long = now - mStartTime - 120 * (i + 1)
                var percent: Float = if (time > 0) time % 750 / 750f else 0f
                percent = mInterpolator.getInterpolation(percent)
                mScale = if (state.isRefreshing) {
                    if (percent < 0.5f) {
                        1 - percent * 2 * 0.7f
                    } else {
                        percent * 2 * 0.7f - 0.4f
                    }
                } else {
                    1f
                }
                scale(mScale, Offset(x = (i - 1) * (mCircleSpacing + radius * 2), y = 0f)) {
                    drawCircle(
                        ballColor,
                        radius,
                        center = Offset(x = (i - 1) * (mCircleSpacing + radius * 2), y = 0f),
                        1f
                    )
                }
            }
        }
    }
}


// Accounts part contain many account
@Composable
fun ShowAccounts(accounts: List<Account>, context:MainActivity, deleteView: Boolean) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        accounts.forEach{ account ->
            ShowAccount(account,context,deleteView)
        }
    }

}

// It contained by ShowAccounts()
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ShowAccount(account: Account,context:MainActivity, deleteView: Boolean) {
    var pic = ""
    if(account.type == "Cex"){
        pic = account.key.split("@@")[0]
    }else if(account.type == "Eth"){
        pic = account.key
    }else if(account.type == "Man"){
        pic = account.key
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 12.dp, start = 16.dp, end = 16.dp),
        elevation = 8.dp,
        shape = androidx.compose.material.MaterialTheme.shapes.medium,
        backgroundColor = androidx.compose.material.MaterialTheme.colors.surface,
        contentColor = androidx.compose.material.MaterialTheme.colors.onSurface,
        onClick = {
            // jump into the detail page
            if(account.type == "Eth"){
                val intent = Intent(context, BalanceEthActivity::class.java)
                intent.putExtra("address",account.key)
                intent.putExtra("name",account.name)
                intent.putExtra("FirstTime",false)
                context.startActivity(intent)
            }else if (account.type == "Cex"){
                val intent = Intent(context, BalanceBinanceActivity::class.java)
                intent.putExtra("apiKey",account.key.split("@@")[0])
                intent.putExtra("apiSecret",account.key.split("@@")[1])
                intent.putExtra("name",account.name)
                intent.putExtra("FirstTime",false)
                context.startActivity(intent)
            }else if(account.type == "Man"){
                val intent = Intent(context, BalanceManuallyActivity::class.java)
                intent.putExtra("name",account.name)
                intent.putExtra("FirstTime",false)
                context.startActivity(intent)
            }
        }
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
        ) {
            Image(
                painter = rememberImagePainter( // avatar for the account
                    data = "https://api.multiavatar.com/$pic.png",
                    builder = {
                        transformations(RoundedCornersTransformation(4.0f)) // round shape
                    }
                ),
                contentDescription = "avatar",
                modifier = Modifier
                    .size(80.dp)
                    .padding(5.dp)
            )
            Column() {
                var nameShort = account.name
                if(nameShort.length>=8){
                    nameShort = nameShort.substring(0,3)+"..."+nameShort.substring(nameShort.length-3,nameShort.length)
                } // If the name is too long, I will use a short one
                val keyShort = pic.toString().substring(0, 5) + "..." + account.key.toString().substring(account.key.toString().length - 4, account.key.toString().length)
                androidx.compose.material.Text(text = account.name, fontSize = androidx.compose.material.MaterialTheme.typography.h5.fontSize, modifier = Modifier.padding(5.dp))
                androidx.compose.material.Text(text = keyShort,fontSize = androidx.compose.material.MaterialTheme.typography.body2.fontSize, modifier = Modifier.padding(5.dp))
            }
            Column() {
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    androidx.compose.material.Text(text = String.format("$%.2f",account.balance),fontSize = androidx.compose.material.MaterialTheme.typography.h5.fontSize)
                }

                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                if(deleteView){
                        androidx.compose.material.IconButton(onClick = {
                            val thread = Thread(Runnable {
                                val db = AppDatabase.getDatabase(context = context).getAccountDao()
                                db.delete(db.findByName(account.name))
                            })
                            thread.start()
                        }) {
                            Icon(
                                imageVector = Icons.Filled.Clear,
                                contentDescription = "delete",
                                tint = androidx.compose.material.MaterialTheme.colors.onSurface
                            )
                        }
                    }
                }
            }

        }
    }
}


// Top Bar
@Composable
fun ShowMainTopBar(title:String,onDeleteViewChange:(Boolean)->Unit,deleteView:Boolean) {
    Row() {
        androidx.compose.material.Text(text = title,
            modifier = Modifier
                .padding(top = 15.dp, bottom = 15.dp, start = 10.dp),
            fontSize = androidx.compose.material.MaterialTheme.typography.h5.fontSize,
        )
        // a button to delete the account

       Row(modifier = Modifier.fillMaxWidth(),horizontalArrangement = Arrangement.End) {
           androidx.compose.material.IconButton(onClick = {
               if (deleteView) {
                   onDeleteViewChange(false)
               } else {
                   onDeleteViewChange(true)
               }
           }) {
               if(deleteView) {
                   Icon(
                       imageVector = Icons.Filled.Delete,
                       contentDescription = "delete",
                       tint = androidx.compose.material.MaterialTheme.colors.onSurface
                   )
               }else{
                     Icon(
                          imageVector = Icons.Outlined.Delete,
                          contentDescription = "delete",
                          tint = androidx.compose.material.MaterialTheme.colors.onSurface
                     )
               }
           }
       }
    }
}


// Top Bar
@Composable
fun ShowTopBar(title:String) {
    androidx.compose.material.Text(text = title,
        modifier = Modifier
            .padding(top = 15.dp, bottom = 15.dp, start = 10.dp),
        fontSize = androidx.compose.material.MaterialTheme.typography.h5.fontSize,
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowSurface(context: MainActivity,accountsLive: LiveData<List<Account>>,deleteView:Boolean){
    val accounts = accountsLive.observeAsState(arrayListOf())
    val fabState = rememberSpeedDialFloatingActionButtonState()
    // For a floating action button
    // scaffold provide a bottom place
    Scaffold(
        bottomBar = {
            BottomAppBar(
                containerColor = Color.Transparent, // I don't need a bottom app bar, so transparent
                actions = {

                },
                floatingActionButton = {
                    BottomAppBarSpeedDialFloatingActionButton(
                        state = fabState
                    ) {
                        androidx.compose.material3.Icon(Icons.Default.Add, contentDescription = "Add")
                    }
                }
            )
        },
        floatingActionButton = {
            SubSpeedDialFloatingActionButtons(
                state = fabState,
                items = listOf(
                    FloatingActionButtonItem(
                        icon = ImageVector.vectorResource(id = R.drawable.ic_eth),
                        label = "Ethereum",
                        onFabItemClicked = {
                            Log.d("FAB", "ShowNewButton: ")
                            val intent = Intent(context, AddEthActivity::class.java)
                            context.startActivity(intent)
                        },
                    ),
                    FloatingActionButtonItem(
                        icon = ImageVector.vectorResource(id = R.drawable.ic_binance),
                        label = "Binance",
                        onFabItemClicked = {
                            Log.d("FAB", "ShowNewButton: ")
                            val intent = Intent(context, AddCexActivity::class.java)
                            context.startActivity(intent)
                        },
                    ),
                    FloatingActionButtonItem(
                        icon = Icons.Filled.Edit,
                        label = "Manual",
                        onFabItemClicked = {
                            Log.d("FAB", "ShowNewButton: ")
                            val intent = Intent(context, AddManualActivity::class.java)
                            context.startActivity(intent)
                        },
                    )

                )
            )
        }
    ) {
        Surface(
            modifier = Modifier
                .padding(it)
                .fillMaxSize(),
            color = androidx.compose.material3.MaterialTheme.colorScheme.background
        ) {
            if(accounts!=null)
                ShowAccounts(accounts = accounts.value, context = context,deleteView = deleteView)
        }
    }
}

