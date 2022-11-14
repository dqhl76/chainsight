package com.liuqingyue.chainsight


import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.liuqingyue.chainsight.ui.theme.ChainsightTheme


class AddCexActivity : ComponentActivity() {
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
                            ShowTopBar("Add Binance Account") // top bar part
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
                        ShowAddNewCexAccount(this@AddCexActivity)

                    }
                }
            }
        }
    }
}


@Composable
fun ShowAddNewCexAccount(context: AddCexActivity) {

    // address and name need to remember and pass into detailed page
    var name by remember {
        mutableStateOf("")
    }
    var apiKey by remember {
        mutableStateOf("")
    }
    var apiSecret by remember {
        mutableStateOf("")
    }

    Column(modifier = Modifier.padding(start = 15.dp, end = 15.dp)) {
        Text(
            text = "Please input your Binance API. See the instruction on Binance official website about how to get your API key and secret. https://www.binance.com/en/binance-api",
            modifier = Modifier.padding(4.dp, 6.dp, 4.dp, 8.dp),
            style = MaterialTheme.typography.body1,
            color = Color.DarkGray
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
            modifier = Modifier
                .fillMaxWidth()
        )

        TextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            label = { Text("API Key", color = colorResource(id = R.color.black)) },
            //        leadingIcon = @Composable {
            //            Image(imageVector = Icons.Filled.Search, contentDescription = "Search Icon")
            //        },
            trailingIcon = @Composable {
                Image(imageVector = Icons.Filled.Clear, contentDescription = "Clear Search Icon",
                    modifier = Modifier.clickable {
                        apiKey = ""
                    })

            },
            colors = TextFieldDefaults.textFieldColors(
                focusedIndicatorColor = colorResource(id = R.color.primary),
                unfocusedIndicatorColor = colorResource(id = R.color.primary),
                errorIndicatorColor = colorResource(id = R.color.primary),
            ),
            shape = RoundedCornerShape(8.dp, 8.dp, 8.dp, 8.dp),
            placeholder = { Text("Enter your Binance API Key") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 10.dp)
        )

        TextField(
            value = apiSecret,
            onValueChange = { apiSecret = it },
            label = { Text("API Secret", color = colorResource(id = R.color.black)) },
            //        leadingIcon = @Composable {
            //            Image(imageVector = Icons.Filled.Search, contentDescription = "Search Icon")
            //        },
            trailingIcon = @Composable {
                Image(imageVector = Icons.Filled.Clear, contentDescription = "Clear Search Icon",
                    modifier = Modifier.clickable {
                        apiSecret = ""
                    })

            },
            colors = TextFieldDefaults.textFieldColors(
                focusedIndicatorColor = colorResource(id = R.color.primary),
                unfocusedIndicatorColor = colorResource(id = R.color.primary),
                errorIndicatorColor = colorResource(id = R.color.primary),
            ),
            shape = RoundedCornerShape(8.dp, 8.dp, 8.dp, 8.dp),
            placeholder = { Text("Enter your API Secret") },
            modifier = Modifier
                .fillMaxWidth()
        )


        Button(
            onClick = {
                if (apiKey == "" || apiSecret == "") {
                    Toast.makeText(context, "Please input your API Key and API Secret", Toast.LENGTH_SHORT).show()
                } else {
                    val intent = Intent(context,BalanceBinanceActivity::class.java )
                    intent.putExtra("apiKey", apiKey)
                    intent.putExtra("apiSecret", apiSecret)
                    intent.putExtra("FirstTime", true)
                    intent.putExtra("name", name)
                    ContextCompat.startActivity(context, intent, null)
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
                text = "If you want to test this app, please get a test API from https://testnet.binance.vision/. I also provide a test API, you can use it test this function.",
                modifier = Modifier.padding(4.dp, 16.dp, 4.dp, 0.dp),
                style = MaterialTheme.typography.body1,
                color = Color.DarkGray
            )
        }
        SelectionContainer() {
            Text(
                text = "Test API Key: 7nhvMub7uAW2CwCDE8B45nBZOUkzCBCxnrN8cPvR3K2nFIsT6oFhkHVXDUOwziiG \nTest API Secret: uZT7BeJWF34WMTopgIgYYqq7pfZ8tzcRVfvyimxXrkhR31F41bChfkqcxbIFW4pW",
                modifier = Modifier.padding(4.dp, 12.dp, 4.dp, 8.dp),
                style = MaterialTheme.typography.body1,
                color = Color.Gray
            )
        }


    }
}


