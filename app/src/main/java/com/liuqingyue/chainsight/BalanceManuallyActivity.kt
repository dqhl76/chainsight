package com.liuqingyue.chainsight

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.media.session.MediaSession.Token
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import coil.compose.rememberImagePainter
import coil.transform.RoundedCornersTransformation
import coingecko.CoinGeckoClient
import kotlinx.coroutines.launch
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.Instant
import java.util.*

import com.binance.connector.client.impl.SpotClientImpl
import com.google.gson.Gson
import com.liuqing.chainsight.BinanceAccountData
import kotlinx.coroutines.GlobalScope
import org.apache.commons.codec.binary.Hex
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


suspend fun updateDB(db:AccountDao,db2: ManuallyAccountDao, key :String,viewModel: BalanceViewModel,uid:String) {
    Log.e("updateDB", "updateDB")
    val coinGecko = CoinGeckoClient()
    val tokens = db2.loadByAccountIdWithoutLive(key)
    var cnt = 0
    try{
        for(token in tokens){
            val info = coinGecko.getCoinInfoByContractAddress("ethereum",token.contract)
            val price = info.marketData?.currentPrice?.get("usd")
            val percent = info.marketData?.priceChangePercentage24h
            val symbols = info.name
            if(cnt == 0 && viewModel.balance.value != 0.0) {
                viewModel.balanceSet(0.0)
            }
            viewModel.balanceAdd(price?.times(token.amount) ?: 0.0)
            Log.e("tokenData", price.toString() + " " + percent.toString() + " " + symbols.toString())
            db2.update(ManuallyAccount(uid=token.uid,contract=token.contract, amount = token.amount,price=price!!, change24h =percent!!,symbol=symbols!!, account = token.account))
            val account = db.loadById(uid)
            db.update(Account(uid=account.uid, type = account.type, name = account.name, key=account.key ,balance = viewModel.balance.value, lastUpdate =Instant.now().toString()))
            cnt += 1
        }

    }catch (e:Exception){
        Log.e("tokenData", e.toString())
    }

}

class BalanceManuallyActivity : ComponentActivity() {
    val balanceViewModel by viewModels<BalanceViewModel>()
    var key = ""
    var uid = ""
    // when return from finish() method call this function
    override fun onResume() {
        super.onResume()
        if(key != "") {
            val db = AppDatabase.getDatabase(this).getAccountDao()
            val db2 = AppDatabase2.getDatabase(this).getManuallyAccountDao()

            lifecycleScope.launch {
                updateDB(db, db2, key, balanceViewModel, uid)
            }
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_manually)

        // get data from previous one page
        val bundle: Bundle? = intent.extras
        val flag = bundle?.getBoolean("FirstTime")
        val name = bundle?.getString("name")!!

        val db = AppDatabase.getDatabase(context = this@BalanceManuallyActivity).getAccountDao()
        val db2 = AppDatabase2.getDatabase(context = this@BalanceManuallyActivity).getManuallyAccountDao()

        // connect to database
        uid = UUID.randomUUID().toString().replace("-", "")
        key = UUID.randomUUID().toString().replace("-", "")
        var balance = 0.0
        if(flag == true){
            db.insert(Account(
                uid = uid,
                type = "Man",
                name = name!!,
                key = key,
                balance = balance,
                lastUpdate = Instant.now().toString()
            ))
        }else{
            key = db.findByName(name!!).key
            uid = db.findByName(name!!).uid
            balance = db.findByName(name!!).balance
        }


        val tokens = db2.loadByAccountId(key)

        // get data from sharedPreference
        val sharePreference = getSharedPreferences("ETH",Context.MODE_PRIVATE)
        val price = sharePreference.getString("price","1500")
        val change = sharePreference.getString("change24h","1.00")

        val compose = findViewById<ComposeView>(R.id.composeManually1)
        balanceViewModel.balanceSet(balance)
        compose.setContent {
            BalanceManuallyShow(viewModel = balanceViewModel, context = this, address=key, name = name, ethPrice = price)
        }

        val compose2 = findViewById<ComposeView>(R.id.composeManually2)
        compose2.setContent {
            ManuallyTokenListShow(tokenList = tokens)
        }

        val compose3 = findViewById<ComposeView>(R.id.composeManually3)
        compose3.setContent {
            ManuallyAddTokenShow(this,key)
        }

    }
}

//
@Composable
fun ManuallyTokenListShow(tokenList: LiveData<List<ManuallyAccount>>){
    val tokens = tokenList.observeAsState(initial = listOf())
    Column(modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(top = 2.dp)
    ) {
        tokens.value?.forEach {
            ManuallyTokenShow(token = it)
        }
    }
}

@Composable
fun ManuallyTokenShow(token: ManuallyAccount){
    val percentage =if(token.change24h > 0)"+" else ""
    Row (modifier = Modifier
        .drawBehind {
            val strokeWidth = 2f
            val x = size.width - strokeWidth
            val y = size.height - strokeWidth
            drawLine(
                color = Color.LightGray,
                start = Offset(0f, 0f),
                end = Offset(x, 0f),
                strokeWidth = strokeWidth
            )
        }
        .clickable(
            onClick = {}
        )){
        Image(
            painter = rememberImagePainter(
                data = "https://ui-avatars.com/api/?size=128&background=random&name=" + token.symbol,
                builder = {
                    transformations(RoundedCornersTransformation(16.0f))
                }
            ),
            contentDescription = null,
            modifier = Modifier
                .size(70.dp)
                .padding(10.dp)
        )
        Column {
            Text(text = token.symbol, style = MaterialTheme.typography.body1,modifier = Modifier.padding(top=10.dp), color = Color.DarkGray)
            Text(text = String.format("%s %s", token.amount,token.symbol),
                fontSize = MaterialTheme.typography.body2.fontSize,
                fontWeight = FontWeight.Light,
                modifier = Modifier.padding(top=6.dp),
                color = Color.DarkGray)
        }
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth(), horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = String.format("$%.2f", token.amount * token.price),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.W400,
                    modifier = Modifier.padding(top = 10.dp, end = 8.dp),
                    color = Color.DarkGray
                )

            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text(
                    text = percentage + String.format(
                        "%.2f",
                        token.change24h
                    ) + "%",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.W400,
                    modifier = Modifier.padding(top = 6.dp, end = 8.dp),
                    color = if (token.change24h > 0
                    ) colorResource(id = R.color.up_green) else colorResource(id = R.color.down_red)
                )
            }
        }
    }
}

@Composable
fun ManuallyAddTokenShow(context: BalanceManuallyActivity,key:String){
    Row (modifier = Modifier
        .drawBehind {
            val strokeWidth = 2f
            val x = size.width - strokeWidth
            val y = size.height - strokeWidth
            drawLine(
                color = Color.LightGray,
                start = Offset(0f, 0f),
                end = Offset(x, 0f),
                strokeWidth = strokeWidth
            )
        }
        .clickable(
            onClick = {
                val intent = Intent(context, AddTokenManually::class.java)
                intent.putExtra("account", key)
                ContextCompat.startActivity(context, intent, null)
            }
        )
        .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center

    ) {
        Image( imageVector = Icons.Filled.Add, contentDescription = "Add new Token", modifier = Modifier
            .padding(top = 20.dp, bottom = 20.dp)
            .size(40.dp))
    }
}


// show the account profile (avatar, balance and name)
@Composable
fun BalanceManuallyShow(viewModel: BalanceViewModel,context: BalanceManuallyActivity,address: String?,name: String?,ethPrice:String?) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier
            .padding(bottom = 5.dp)) {
            IconButton(onClick = {
                context.startActivity(Intent(context, MainActivity::class.java))

            }) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "back",
                    modifier = Modifier.padding(top = 5.dp)
                )
            }
            Column {
                Text(
                    text = "Balance",
                    fontSize = MaterialTheme.typography.h5.fontSize,
                    modifier = Modifier.padding(5.dp, 0.dp, 0.dp, 0.dp)
                )
                Text(
                    text = name+" "+address.toString().substring(0, 5) +
                            "..." +
                            address.toString().substring(address.toString().length - 4, address.toString().length
                            ),
                    color = Color.Gray,
                    fontSize = MaterialTheme.typography.body2.fontSize,
                    modifier = Modifier.padding(5.dp, 0.dp, 0.dp, 0.dp)
                )
            }

        }
        Row(modifier = Modifier.fillMaxWidth()) {
            Image(
                modifier = Modifier
                    .size(120.dp, 120.dp)
                    .padding(start = 10.dp),
                painter = rememberImagePainter("https://api.multiavatar.com/"+address.toString()+".png"),
                contentDescription = null
            )
            Column {
                Row(modifier = Modifier.padding(10.dp, 10.dp, 0.dp, 0.dp)) {
                    Text(text = "$",fontWeight = FontWeight.Bold,
                        fontSize = MaterialTheme.typography.h4.fontSize)
                    Text(text = String.format("%.2f", viewModel.balance.value),
                        fontWeight = FontWeight.Bold,
                        fontSize = MaterialTheme.typography.h4.fontSize)
                }
                Row(modifier = Modifier.padding(10.dp, 10.dp, 0.dp, 0.dp)) {
                    Text(text = "Ξ",fontWeight = FontWeight.Bold,
                        fontSize = MaterialTheme.typography.h4.fontSize)
                    Text(text = String.format("%.3f",viewModel.balance.value/ethPrice!!.toDouble() ),
                        fontWeight = FontWeight.Bold,
                        fontSize = MaterialTheme.typography.h4.fontSize)

                    // info button to explain what is Ξ symbol mean
                    IconButton(onClick = {with(AlertDialog.Builder(context)) {
                        setTitle("What does Ξ mean?")
                        setMessage("Denominated in cryptocurrencies. It will calculate your account balance and use ETH as the denomination currency.")
                        setNeutralButton("Ok") { _, _ -> run {} }
                        create()
                    }.show()}) {
                        Icon(
                            Icons.Outlined.Info,
                            contentDescription = "what is this?",
                        )
                    }
                }
            }
        }
    }
}




