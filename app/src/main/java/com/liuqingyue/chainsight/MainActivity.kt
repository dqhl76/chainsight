package com.liuqingyue.chainsight

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import coil.compose.rememberImagePainter
import coil.transform.RoundedCornersTransformation
import de.charlex.compose.BottomAppBarSpeedDialFloatingActionButton
import de.charlex.compose.FloatingActionButtonItem
import de.charlex.compose.SubSpeedDialFloatingActionButtons
import de.charlex.compose.rememberSpeedDialFloatingActionButtonState
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


/* Why I import material3 and material2 in the same file?
 * Because, I use a third-party lib to generate a floating button, it use material3
 * And, other files in my project use material2. No material3 env polluted to other file
 */


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // I use a mix layout, which means it has layout file and using Jetpack compose
        // it can help me divide the layout into 2 parts. one of them need wait database check

        setContentView(R.layout.activity_main)

        // top bar compose part
        val top = findViewById<ComposeView>(R.id.topBar)
        top.setContent {
            ShowTopBar("Board")
        }


        // connect to database
        val db = AppDatabase.getDatabase(this@MainActivity).getAccountDao()

        // use a observer model to update the balance for view model when database changed
        val accountsLiveData = db.getObserverAll()

        // accounts compose part
        findViewById<ComposeView>(R.id.accounts)
            .setContent {
                ShowSurface(context = this@MainActivity, accountsLive = accountsLiveData)
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


// Accounts part contain many account
@Composable
fun ShowAccounts(accounts: List<Account>, context:MainActivity) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        accounts.forEach{ account ->
            ShowAccount(account,context)
        }
    }

}

// It contained by ShowAccounts()
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ShowAccount(account: Account,context:MainActivity) {
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
            }
        }
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
        ) {
            Image(
                painter = rememberImagePainter( // avatar for the account
                    data = "https://api.multiavatar.com/"+account.key.toString()+".png",
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
                val keyShort = account.key.toString().substring(0, 5) + "..." + account.key.toString().substring(account.key.toString().length - 4, account.key.toString().length)
                androidx.compose.material.Text(text = account.name, fontSize = androidx.compose.material.MaterialTheme.typography.h5.fontSize, modifier = Modifier.padding(5.dp))
                androidx.compose.material.Text(text = keyShort,fontSize = androidx.compose.material.MaterialTheme.typography.body2.fontSize, modifier = Modifier.padding(5.dp))
            }
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxSize()) {
                androidx.compose.material.Text(text = String.format("$%.2f",account.balance),fontSize = androidx.compose.material.MaterialTheme.typography.h5.fontSize, modifier = Modifier.padding(5.dp))

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
fun ShowSurface(context: MainActivity,accountsLive: LiveData<List<Account>>){
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
                ShowAccounts(accounts = accounts.value, context = context)
        }
    }
}

