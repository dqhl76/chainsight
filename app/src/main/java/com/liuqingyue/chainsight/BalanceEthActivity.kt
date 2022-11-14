package com.liuqingyue.chainsight


import android.app.AlertDialog
import android.content.Context
import android.content.Intent
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
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query
import java.time.Instant
import java.util.*


interface AddressApi {

    // get account's token amount
    @Headers("Ok-Access-Key: b8c93ac9-b7b4-477b-97ad-e6dc72b44907")
    @GET("address-balance-fills")
    fun getAccount(
        @Query("chainShortName") chain: String,
        @Query("address") address: String,
        @Query("protocolType") type: String
    ): Call<AddressBalanceData>

    // get a summary of an account, it contains the native coin number
    @Headers("Ok-Access-Key: b8c93ac9-b7b4-477b-97ad-e6dc72b44907")
    @GET("address-summary")
    fun getEth(
        @Query("chainShortName") chain: String,
        @Query("address") address: String
    ): Call<AddressEthData>

}


// use a observable data to store the data
// it can update the view at the same time
class BalanceViewModel: ViewModel(){
    var balance = mutableStateOf(0.0)
    fun balanceAdd(delta: Double){
        balance.value += delta
    }
    fun balanceSet(x: Double){
        balance.value = x
    }
}


class BalanceEthActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // get data from previous one page
        val bundle: Bundle? = intent.extras
        val address = bundle?.getString("address")
        val name = bundle?.getString("name")
        val flag = bundle?.getBoolean("FirstTime")

        // connect to database
        val db = AppDatabase.getDatabase(context = this@BalanceEthActivity).getAccountDao()
        var uid = UUID.randomUUID().toString().replace("-", "")
        if(flag == true){
                db.insert(Account(
                    uid = uid,
                    type = "Eth",
                    name = name!!,
                    key = address!!,
                    balance = 0.0,
                    lastUpdate = Instant.now().toString()
                ))
        }else{
            uid = db.findByName(name!!).uid
        }


        // set a basic view contains three compose view
        setContentView(R.layout.activity_balance)

        // get two compose view
        val compose = findViewById<ComposeView>(R.id.composeView)
        val compose_eth = findViewById<ComposeView>(R.id.composeETH)

        val balanceViewModel by viewModels<BalanceViewModel>()

        // get data from sharedPreference
        val sharePreference = getSharedPreferences("ETH",Context.MODE_PRIVATE)
        val price = sharePreference.getString("price","1500")
        val change = sharePreference.getString("change24h","1.00")

        // set content for compose
        compose.setContent {
            BalanceShow(viewModel = balanceViewModel, context = this, address = address,name=name,ethPrice=price)

        }

        // for debugging
        Log.d("address", address.toString())
        val retrofit = Retrofit.Builder()
            .baseUrl("https://www.oklink.com/api/v5/explorer/address/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val api = retrofit.create(AddressApi::class.java)

        // get data from API, set the ETH part
        address?.let{ itAdrress ->
            api.getEth(chain = "ETH", address = itAdrress)
                .enqueue(object : Callback<AddressEthData>{ // callback from the api data
                    override fun onResponse(
                        call: Call<AddressEthData>,
                        response: Response<AddressEthData>
                    ) {
                        response.let { itResponse ->
                            itResponse.body()?.let { itData ->
                                balanceViewModel.balanceAdd(price!!.toDouble() * (itData.data[0].balance?.toDouble() ?: 0.0))  // observer the data
                                // update the view real-time

                                // set for ETH part
                               compose_eth.setContent { TokenShow(token = AddressBalanceData.Data.TokenList(
                                   token = "ETH",
                                   holdingAmount = itData.data[0].balance,
                                   totalTokenValue = itData.data[0].balance,
                                   change24h = change,
                                   priceUsd = price,
                                   valueUsd = (price!!.toDouble() * (itData.data[0].balance?.toDouble() ?: 0.0)).toString()
                               ),true) }
                                db.update(
                                        Account(
                                            uid = uid,
                                            type = "Eth",
                                            name = name!!,
                                            key = address,
                                            balance = balanceViewModel.balance.value,
                                            lastUpdate = Instant.now().toString()
                                        )
                                )
                                    Log.e("balance_check","updated")
                                lifecycleScope.launch{
                                    Log.d("balance_check",db.getAll().toString())
                                }

                            }

                        }
                    }
                    override fun onFailure(call: Call<AddressEthData>, t: Throwable) {
                        Log.ERROR
                    }
                })
        }


        // set the ERC-20 tokens compose
        val tokens: ArrayList<AddressBalanceData.Data.TokenList> = ArrayList()
        address?.let { itAddress ->
            api.getAccount(chain = "ETH", address = itAddress, type = "token_20") // get erc-20 tokens
                .enqueue(object : Callback<AddressBalanceData> {
                    override fun onResponse(
                        call: Call<AddressBalanceData>,
                        response: Response<AddressBalanceData>
                    ) {
                        response.let { itResponse ->
                            itResponse.body()?.let { itData->
                                for (i in itData.data[0].tokenList) {
                                    if (i.valueUsd != "0") {
                                        Log.d("Token", i.toString())
                                        tokens.add(i)
                                        balanceViewModel.balanceAdd(i.valueUsd?.toDouble()!!)
                                    }
                                }
                                val compose2 = findViewById<ComposeView>(R.id.composeView2)
                                compose2.setContent {
                                    TokenListShow(tokens = tokens)
                                }

                                    db.update(
                                        Account(
                                            uid = uid,
                                            type = "Eth",
                                            name = name!!,
                                            key = address,
                                            balance = balanceViewModel.balance.value,
                                            lastUpdate = Instant.now().toString()
                                        )
                                    )
                                    Log.e("balance_check","updated")

                                lifecycleScope.launch{ // for debugging
                                    Log.d("balance_check",db.getAll().toString())
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
}


// show tokens
@Composable
fun TokenListShow(tokens: ArrayList<AddressBalanceData.Data.TokenList>) {
    Column(modifier = Modifier
        .verticalScroll(rememberScrollState())
        .padding(top = 2.dp)) {
        tokens.forEach{ token ->
            TokenShow(token,false)
        }

    }

}



// show one token, it contained by TokenListShow()
@Composable
fun TokenShow(token: AddressBalanceData.Data.TokenList,isETH:Boolean){
    val percentage =if((token.change24h?.toDouble() ?: +0.00) > 0)"+" else ""
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
        if(!isETH) {
            Image(
                painter = rememberImagePainter(
                    data = "https://ui-avatars.com/api/?size=128&background=random&name=" + token.token.toString(),
                    builder = {
                        transformations(RoundedCornersTransformation(16.0f))
                    }
                ),
                contentDescription = null,
                modifier = Modifier
                    .size(70.dp)
                    .padding(10.dp)
            )
        }else{
            Image(
                painter = rememberImagePainter(
                    data = R.drawable.ic_eth__1_,
                    builder = {
                        transformations(RoundedCornersTransformation(16.0f))
                    }
                ),
                contentDescription = null,
                modifier = Modifier
                    .size(70.dp)
                    .padding(10.dp)
            )
        }
        Column {
            Text(text = token.token.toString(), style = MaterialTheme.typography.body1,modifier = Modifier.padding(top=10.dp), color = Color.DarkGray)
            Text(text = String.format("%s %s", token.totalTokenValue.toString(),token.token.toString()),
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
                    text = String.format("$%.2f", token.valueUsd?.toDouble() ?: 0.00),
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
                        token.change24h?.toDouble() ?: 0.00
                    ) + "%",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.W400,
                    modifier = Modifier.padding(top = 6.dp, end = 8.dp),
                    color = if ((token.change24h?.toDouble()
                            ?: +0.00) > 0
                    ) colorResource(id = R.color.up_green) else colorResource(id = R.color.down_red)
                )
            }
        }
    }
}


// show the account profile (avatar, balance and name)
@Composable
fun BalanceShow(viewModel: BalanceViewModel,context: BalanceEthActivity, address: String?,name: String?,ethPrice:String?) {
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


