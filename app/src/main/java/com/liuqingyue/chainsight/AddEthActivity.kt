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

/*
* Add a new ethereum account page
* */

class AddEthActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                            ShowTopBar("Add Ethereum Account") // top bar part
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
                        ShowAddNewAccount(this@AddEthActivity)

                    }
                }
            }
        }



    }
}

// get balance will jump into the detail page
fun getBalance(address: String, name: String, context: AddEthActivity) {
    val intent = Intent(context, BalanceEthActivity::class.java)
    intent.putExtra("address", address)
    intent.putExtra("name", name)
    intent.putExtra("FirstTime", true)
    ContextCompat.startActivity(context, intent, null)
}


// address regex to check if a ERC20 address is valid
val addressRegex = Regex("^0x[a-fA-F0-9]{40}\$")

@Composable
fun ShowAddNewAccount(context: AddEthActivity) {

    // address and name need to remember and pass into detailed page
    var address by remember {
        mutableStateOf("")
    }
    var name by remember {
        mutableStateOf("")
    }

    Column(modifier = Modifier.padding(start = 15.dp, end = 15.dp)) {
        Text(
            text = "Please input your Ethereum address and the account name. Address started from 0x. We will support more network in the future.",
            modifier = Modifier.padding(4.dp, 6.dp, 4.dp, 8.dp),
            style = MaterialTheme.typography.body1,
            color = Color.DarkGray
        )
        TextField(
            value = address,
            onValueChange = { address = it },
            label = { Text("Address", color = colorResource(id = R.color.black)) },
            //        leadingIcon = @Composable {
            //            Image(imageVector = Icons.Filled.Search, contentDescription = "Search Icon")
            //        },
            trailingIcon = @Composable {
                Image(imageVector = Icons.Filled.Clear, contentDescription = "Clear Search Icon",
                    modifier = Modifier.clickable {
                        address = ""
                    })

            },
            colors = TextFieldDefaults.textFieldColors(
                focusedIndicatorColor = colorResource(id = R.color.primary),
                unfocusedIndicatorColor = colorResource(id = R.color.primary),
                errorIndicatorColor = colorResource(id = R.color.primary),
            ),
            shape = RoundedCornerShape(8.dp, 8.dp, 8.dp, 8.dp),
            placeholder = { Text("Enter your ethereum address") },
            isError = addressRegex.matches(address).not(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 10.dp)
        )

        TextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name", color = colorResource(id = R.color.black)) },
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
            placeholder = { Text("Enter your account name") },
            isError = addressRegex.matches(address).not(),
            modifier = Modifier
                .fillMaxWidth()
        )


        Button(
            onClick = {
                if (addressRegex.matches(address)) {
                    getBalance(address, name, context)
                } else {
                    Toast.makeText(context, "Invalid address", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .height(48.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = colorResource(id = R.color.primary)),

            ) {
            Text("Finish", color = Color.Black, style = MaterialTheme.typography.button)
        }
        SelectionContainer() {

            Text(
                text = "If you are testing this app and don't have a address, you can use this address as example: 0xA47F68826Abe8d3A20EE1B1458F3156e6C7b5277",
                modifier = Modifier.padding(4.dp, 16.dp, 4.dp, 8.dp),
                style = MaterialTheme.typography.body1,
                color = Color.DarkGray
            )
        }
    }
}



