package com.liuqingyue.chainsight

import android.content.Context
import android.util.Log
import com.binance.connector.client.impl.SpotClientImpl
import com.google.gson.Gson
import com.liuqing.chainsight.BinanceAccountData
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*

fun updateEth(context: MainActivity){
    val db = AppDatabase.getDatabase(context = context).getAccountDao()
    val accounts = db.getAll()
    val sharePreference = context.getSharedPreferences("ETH",Context.MODE_PRIVATE)
    val ethPrice = sharePreference.getString("price","1500")
    for(account in accounts){
        if(account.type == "Eth"){

               val retrofit = Retrofit.Builder()
                   .baseUrl("https://www.oklink.com/api/v5/explorer/address/")
                   .addConverterFactory(GsonConverterFactory.create())
                   .build()
               val api = retrofit.create(AddressApi::class.java)
               val address = account.key

              var balance = 0.0
               api.getEth(chain = "ETH", address=address)
                   .enqueue(object : Callback<AddressEthData> {
                       override fun onResponse(
                           call: Call<AddressEthData>,
                           response: Response<AddressEthData>
                       ) {
                           response.let { itResponse ->
                               balance += ethPrice!!.toDouble() * (itResponse.body()?.data?.get(0)?.balance?.toDouble() ?: 0.0)
                               db.update(Account(account.uid, account.type, account.name, account.key, balance = balance, Date().toString()))
                               Log.e("update1",balance.toString())
                           }

                       }
                       override fun onFailure(call: Call<AddressEthData>, t: Throwable) {
                           Log.ERROR
                       }
                   })
            api.getAccount(chain = "ETH",address=address, type = "token_20")
                  .enqueue(object : Callback<AddressBalanceData> {
                      override fun onResponse(
                          call: Call<AddressBalanceData>,
                          response: Response<AddressBalanceData>
                      ) {
                            response.let { itResponse ->
                                itResponse.body()?.let { itData->
                                    for (token in itData.data[0].tokenList){
                                        if (token.valueUsd != "0") {
                                            balance += token.valueUsd!!.toDouble()
                                        }
                                    }
                                    db.update(Account(account.uid, account.type, account.name, account.key, balance = balance, Date().toString()))
                                    Log.e("update2",balance.toString())

                                } }

                            }

                      override fun onFailure(call: Call<AddressBalanceData>, t: Throwable) {
                          TODO("Not yet implemented")
                      }



                  })
        }
    }
}

fun updateBinance(context: MainActivity){
    val db = AppDatabase.getDatabase(context = context).getAccountDao()
    val accounts = db.getAll()

    for(account in accounts){
        if(account.type == "Cex") {
            val apiKey = account.key.split("@@")[0]
            val apiSecret = account.key.split("@@")[1]
            val client = SpotClientImpl(
                apiKey!!,
                apiSecret!!,
                "https://testnet.binance.vision"
            )
            val parameters = LinkedHashMap<String, Any>()
            parameters["recvWindow"] = 5000
            val result = client.createTrade().account(parameters)
            val resultJson = Gson().fromJson(result, BinanceAccountData::class.java)
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
            db.update(Account(account.uid, account.type, account.name, account.key, balance = total, Date().toString()))

        }
    }
}