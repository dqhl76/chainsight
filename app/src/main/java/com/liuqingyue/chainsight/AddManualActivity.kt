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

class AddManualActivity : ComponentActivity() {
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
                            ShowTopBar("Add Manually Record Account") // top bar part
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
                        ShowAddNewManualAccount(this@AddManualActivity)

                    }
                }
            }
        }
    }
}


@Composable
fun ShowAddNewManualAccount(context: AddManualActivity) {


    var name by remember {
        mutableStateOf("")
    }

    Column(modifier = Modifier.padding(start = 15.dp, end = 15.dp)) {
        Text(
            text = "Please input an account name. You can record your asserts manually.",
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


        Button(
            onClick = {
                val intent = Intent(context, BalanceManuallyActivity::class.java)
                intent.putExtra("name", name)
                intent.putExtra("FirstTime", true)
                ContextCompat.startActivity(context, intent, null)
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

    }
}



