package com.liuqingyue.chainsight

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.liuqingyue.chainsight.ui.theme.ChainsightTheme
import java.util.UUID


class AddTokenManually : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bundle = intent.extras
        val account = bundle?.getString("account")
        setContent {
            ChainsightTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier
                        .fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    Column {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            ShowTopBar("Add Token") // top bar part
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
                        ShowAddNewToken(this@AddTokenManually, account = account!!)

                    }
                }
            }
        }
    }
}



@Composable
fun ShowAddNewToken(context: AddTokenManually,account: String) {

    // address and name need to remember and pass into detailed page
    var name by remember {
        mutableStateOf("")
    }
    var contract by remember {
        mutableStateOf("")
    }
    var amount by remember {
        mutableStateOf("")
    }

    Column(modifier = Modifier.padding(start = 15.dp, end = 15.dp)) {
        Text(
            text = "Please input the token's Ethereum contract address and the amount. Address should start from 0x.",
            modifier = Modifier.padding(4.dp, 6.dp, 4.dp, 8.dp),
            style = MaterialTheme.typography.body1,
            color = Color.DarkGray
        )
        TextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Token Name", color = colorResource(id = R.color.black)) },
            //        leadingIcon = @Composable {
            //            Image(imageVector = Icons.Filled.Search, contentDescription = "Search Icon")
            //        },
            trailingIcon = @Composable {
                Image(imageVector = Icons.Filled.Clear, contentDescription = "Clear Search Icon",
                    modifier = Modifier.clickable {
                        name = ""
                    })

            },
            colors = TextFieldDefaults.textFieldColors(
                focusedIndicatorColor = colorResource(id = R.color.primary),
                unfocusedIndicatorColor = colorResource(id = R.color.primary),
                errorIndicatorColor = colorResource(id = R.color.primary),
            ),
            shape = RoundedCornerShape(8.dp, 8.dp, 8.dp, 8.dp),
            placeholder = { Text("Enter your ethereum address") },
            isError = addressRegex.matches(contract).not(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 10.dp)
        )
        TextField(
            value = contract,
            onValueChange = { contract = it },
            label = { Text("Contract Address", color = colorResource(id = R.color.black)) },
            //        leadingIcon = @Composable {
            //            Image(imageVector = Icons.Filled.Search, contentDescription = "Search Icon")
            //        },
            trailingIcon = @Composable {
                Image(imageVector = Icons.Filled.Clear, contentDescription = "Clear Search Icon",
                    modifier = Modifier.clickable {
                        contract = ""
                    })

            },
            colors = TextFieldDefaults.textFieldColors(
                focusedIndicatorColor = colorResource(id = R.color.primary),
                unfocusedIndicatorColor = colorResource(id = R.color.primary),
                errorIndicatorColor = colorResource(id = R.color.primary),
            ),
            shape = RoundedCornerShape(8.dp, 8.dp, 8.dp, 8.dp),
            placeholder = { Text("Enter your ethereum address") },
            isError = addressRegex.matches(contract).not(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 10.dp)
        )

        TextField(
            value = amount,
            onValueChange = { amount = it },
            label = { Text("Amount", color = colorResource(id = R.color.black)) },
            //        leadingIcon = @Composable {
            //            Image(imageVector = Icons.Filled.Search, contentDescription = "Search Icon")
            //        },
            trailingIcon = @Composable {
                Image(imageVector = Icons.Filled.Clear, contentDescription = "Clear Search Icon",
                    modifier = Modifier.clickable {
                        amount = ""
                    })

            },
            colors = TextFieldDefaults.textFieldColors(
                focusedIndicatorColor = colorResource(id = R.color.primary),
                unfocusedIndicatorColor = colorResource(id = R.color.primary),
                errorIndicatorColor = colorResource(id = R.color.primary),
            ),
            shape = RoundedCornerShape(8.dp, 8.dp, 8.dp, 8.dp),
            placeholder = { Text("Enter your account name") },
            modifier = Modifier
                .fillMaxWidth()
        )


        Button(
            onClick = {
                if (addressRegex.matches(contract) && amount.isNotEmpty()) {
                    val db = AppDatabase2.getDatabase(context).getManuallyAccountDao()
                    db.insert(ManuallyAccount(
                        uid = UUID.randomUUID().toString().replace("-", ""),
                        contract = contract,
                        amount = amount.toDouble(),
                        account = account,
                        symbol = name,
                        price = 0.0,
                        change24h = 0.0
                    ))
                    context.finish()
                } else {
                    Toast.makeText(context, "Please input correct contrast address and amount", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .height(48.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = colorResource(id = R.color.primary)),

            ) {
            Text("Add", color = Color.Black, style = MaterialTheme.typography.button)
        }
        SelectionContainer() {

            Text(
                text = "If you are testing this app and don't know how to get a token's contract, you can use this contract as example: 0x7fc66500c84a76ad7e9c93437bfc5ac33e2ddae9",
                modifier = Modifier.padding(4.dp, 16.dp, 4.dp, 8.dp),
                style = MaterialTheme.typography.body1,
                color = Color.DarkGray
            )
        }
    }
}



