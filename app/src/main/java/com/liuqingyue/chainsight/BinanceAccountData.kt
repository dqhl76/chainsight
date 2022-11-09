package com.liuqing.chainsight

import com.google.gson.annotations.SerializedName


data class BinanceAccountData (

  @SerializedName("makerCommission"  ) var makerCommission  : Int?                = null,
  @SerializedName("takerCommission"  ) var takerCommission  : Int?                = null,
  @SerializedName("buyerCommission"  ) var buyerCommission  : Int?                = null,
  @SerializedName("sellerCommission" ) var sellerCommission : Int?                = null,
  @SerializedName("canTrade"         ) var canTrade         : Boolean?            = null,
  @SerializedName("canWithdraw"      ) var canWithdraw      : Boolean?            = null,
  @SerializedName("canDeposit"       ) var canDeposit       : Boolean?            = null,
  @SerializedName("brokered"         ) var brokered         : Boolean?            = null,
  @SerializedName("updateTime"       ) var updateTime       : Long?                = null,
  @SerializedName("accountType"      ) var accountType      : String?             = null,
  @SerializedName("balances"         ) var balances         : ArrayList<Balances> = arrayListOf(),
  @SerializedName("permissions"      ) var permissions      : ArrayList<String>   = arrayListOf()

){
  data class Balances (

    @SerializedName("asset"  ) var asset  : String? = null,
    @SerializedName("free"   ) var free   : String? = null,
    @SerializedName("locked" ) var locked : String? = null

  )
}