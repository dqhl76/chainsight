package com.liuqingyue.chainsight

import com.google.gson.annotations.SerializedName


data class AddressEthData (

    @SerializedName("code" ) var code : String?         = null,
    @SerializedName("msg"  ) var msg  : String?         = null,
    @SerializedName("data" ) var data : ArrayList<Data> = arrayListOf()

){
    data class Data (

        @SerializedName("chainFullName"                 ) var chainFullName                 : String? = null,
        @SerializedName("chainShortName"                ) var chainShortName                : String? = null,
        @SerializedName("address"                       ) var address                       : String? = null,
        @SerializedName("contractAddress"               ) var contractAddress               : String? = null,
        @SerializedName("balance"                       ) var balance                       : String? = null,
        @SerializedName("balanceSymbol"                 ) var balanceSymbol                 : String? = null,
        @SerializedName("transactionCount"              ) var transactionCount              : String? = null,
        @SerializedName("verifying"                     ) var verifying                     : String? = null,
        @SerializedName("sendAmount"                    ) var sendAmount                    : String? = null,
        @SerializedName("receiveAmount"                 ) var receiveAmount                 : String? = null,
        @SerializedName("tokenAmount"                   ) var tokenAmount                   : String? = null,
        @SerializedName("totalTokenValue"               ) var totalTokenValue               : String? = null,
        @SerializedName("createContractAddress"         ) var createContractAddress         : String? = null,
        @SerializedName("createContractTransactionHash" ) var createContractTransactionHash : String? = null,
        @SerializedName("firstTransactionTime"          ) var firstTransactionTime          : String? = null,
        @SerializedName("lastTransactionTime"           ) var lastTransactionTime           : String? = null,
        @SerializedName("token"                         ) var token                         : String? = null,
        @SerializedName("bandwidth"                     ) var bandwidth                     : String? = null,
        @SerializedName("energy"                        ) var energy                        : String? = null,
        @SerializedName("votingRights"                  ) var votingRights                  : String? = null,
        @SerializedName("unclaimedVotingRewards"        ) var unclaimedVotingRewards        : String? = null

    )
}