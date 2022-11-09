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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import coil.compose.rememberImagePainter
import coil.transform.RoundedCornersTransformation
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


interface BinanceApi {

    // get account's token amount
    @GET("24hr")
    fun getAccount(
        @Query("symbols") symbols: String
    ): Call<TokenPriceData>

}



fun createSignature( data: String, key: String): String {
    val sha256Hmac = Mac.getInstance("HmacSHA256")
    val secretKey = SecretKeySpec(key.toByteArray(), "HmacSHA256")
    sha256Hmac.init(secretKey)

    return Hex.encodeHexString(sha256Hmac.doFinal(data.toByteArray()))

}

//
//// use a observable data to store the data
//// it can update the view at the same time
//class BalanceViewModel: ViewModel(){
//    var balance = mutableStateOf(0.0)
//    fun balanceAdd(delta: Double){
//        balance.value += delta
//    }
//}
//
//
class BalanceBinanceActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // get data from previous one page
        val bundle: Bundle? = intent.extras
        val apiKey = bundle?.getString("apiKey")
        val apiSecret = bundle?.getString("apiSecret")
        val flag = bundle?.getBoolean("FirstTime")
        val name = bundle?.getString("name")

        val sharePreference = getSharedPreferences("ETH",Context.MODE_PRIVATE)
        val ethPrice = sharePreference.getString("price","1500")

        // connect to database
        val db = AppDatabase.getDatabase(context = this@BalanceBinanceActivity).getAccountDao()
        var uid = UUID.randomUUID().toString().replace("-", "")
        if(flag == true){
            db.insert(Account(
                uid = uid,
                type = "Cex",
                name = name!!,
                key = "$apiKey@@$apiSecret",
                balance = 0.0,
                lastUpdate = Instant.now().toString()
            ))
        }else{
            uid = db.findByName(name!!).uid
        }


//         use retrofit2 to send request for Binance API
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.binance.com/api/v3/ticker/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val service = retrofit.create(BinanceApi::class.java)


        val client = SpotClientImpl(
            apiKey!!,
            apiSecret!!,
            "https://testnet.binance.vision"
        )
        setContentView(R.layout.activity_binance)
        val compose = findViewById<ComposeView>(R.id.composeBinance1)
        val compose2 = findViewById<ComposeView>(R.id.composeBinance2)
        val thread = Thread {
            try {
                val parameters = LinkedHashMap<String, Any>()
                parameters["recvWindow"] = 5000
                val result = client.createTrade().account(parameters)
                val resultJson = Gson().fromJson(result,BinanceAccountData::class.java)
                var tokens = ArrayList<String>()
                for(token in resultJson.balances){
                    if(token.free!!.toDouble() > 0.0){
                        if(token.asset != "USDT" && token.asset != "BUSD"){
                            tokens.add(token.asset + "USDT")
                        }
                    }
                }

                val parameters2 = LinkedHashMap<String, Any>()
                parameters2["symbols"] = tokens
                var result2 = client.createMarket().ticker24H(parameters2)
                var tokenPrice = ArrayList<TokenPriceData>()
                Log.e("Token",result2)
                val tokensPrice = HashMap<String,Double>()
                val tokensChange = HashMap<String,Double>()
                if(result2.startsWith("[")){
                    result2 = result2.substring(1,result2.length-2)
                    val tokens = result2.split("},")
                    for(token in tokens){
                        val tokenJson = Gson().fromJson("$token}",TokenPriceData::class.java)
                        tokenPrice.add(tokenJson)
                        tokensPrice[tokenJson.symbol!!.replace("USDT","")] = tokenJson.lastPrice!!.toDouble()
                        tokensChange[tokenJson.symbol!!.replace("USDT","")] = tokenJson.priceChangePercent!!.toDouble()
                    }
                }else{
                    val tokenJson = Gson().fromJson(result2,TokenPriceData::class.java)
                    tokenPrice.add(tokenJson)
                }



                val tokenInfo = ArrayList<TokenInfoData>()
                var total = 0.0
                for(token in resultJson.balances){
                    var tokenPrice = tokensPrice[token.asset!!]
                    if(tokenPrice == null){
                        tokenPrice = 1.0
                    }
                    var tokenPriceChange = tokensChange[token.asset!!]
                    if(tokenPriceChange == null){
                        tokenPriceChange = 0.0
                    }
                    tokenInfo.add(TokenInfoData(
                        symbol = token.asset!!,
                        amount = token.free!!.toDouble() + token.locked!!.toDouble(),
                        price = tokenPrice,
                        change24h = tokenPriceChange
                    ))
                }
                Log.e("Token",tokenInfo.toString())
                for(token in tokenInfo){
                    total += token.amount * token.price!!
                }
                compose.setContent {
                    BinanceBalanceShow(name=name!!, context = this@BalanceBinanceActivity, address = apiKey, total=total, ethPrice = ethPrice!!.toDouble())
                }
                compose2.setContent { 
                    BinanceTokenListShow(tokens = tokenInfo)
                }
                db.update(Account(uid=uid, type = "Cex", name = name!!, key = "$apiKey@@$apiSecret", balance = total, lastUpdate = Instant.now().toString()))
            } catch (e: Exception) {
                Log.e("Binance", e.toString())
            }
        }
        thread.start()


    }
}




// show tokens
@Composable
fun BinanceTokenListShow(tokens: kotlin.collections.ArrayList<TokenInfoData>) {
    Column(modifier = Modifier
        .verticalScroll(rememberScrollState())
        .padding(top = 2.dp)) {
        tokens.forEach{ token ->
            BinanceTokenShow(token)
        }


    }

}



// show one token, it contained by TokenListShow()
@Composable
fun BinanceTokenShow(token: TokenInfoData){
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


// show the account profile (avatar, balance and name)
@Composable
fun BinanceBalanceShow(name:String,context:BalanceBinanceActivity,address:String,total:Double,ethPrice:Double) {
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
                    Text(text = String.format("%.2f", total),
                        fontWeight = FontWeight.Bold,
                        fontSize = MaterialTheme.typography.h4.fontSize)
                }
                Row(modifier = Modifier.padding(10.dp, 10.dp, 0.dp, 0.dp)) {
                    Text(text = "Ξ",fontWeight = FontWeight.Bold,
                        fontSize = MaterialTheme.typography.h4.fontSize)
                    Text(text = String.format("%.3f",total/ethPrice ),
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