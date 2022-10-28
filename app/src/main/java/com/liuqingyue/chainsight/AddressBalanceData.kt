package com.liuqingyue.chainsight

import com.google.gson.annotations.SerializedName

data class AddressBalanceData (

    @SerializedName("code" ) var code : String?         = null,
    @SerializedName("msg"  ) var msg  : String?         = null,
    @SerializedName("data" ) var data : ArrayList<Data> = arrayListOf()

): java.io.Serializable {
    data class Data (

        @SerializedName("page"           ) var page           : String?              = null,
        @SerializedName("limit"          ) var limit          : String?              = null,
        @SerializedName("totalPage"      ) var totalPage      : String?              = null,
        @SerializedName("chainFullName"  ) var chainFullName  : String?              = null,
        @SerializedName("chainShortName" ) var chainShortName : String?              = null,
        @SerializedName("tokenList"      ) var tokenList      : ArrayList<TokenList> = arrayListOf()

    ): java.io.Serializable {
        data class TokenList (

            @SerializedName("token"                ) var token                : String? = null,
            @SerializedName("holdingAmount"        ) var holdingAmount        : String? = null,
            @SerializedName("totalTokenValue"      ) var totalTokenValue      : String? = null,
            @SerializedName("change24h"            ) var change24h            : String? = null,
            @SerializedName("priceUsd"             ) var priceUsd             : String? = null,
            @SerializedName("valueUsd"             ) var valueUsd             : String? = null,
            @SerializedName("tokenContractAddress" ) var tokenContractAddress : String? = null

        )
    }
}

