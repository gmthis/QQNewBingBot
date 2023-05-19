package cn.xd.newbingbot.network.entity

import cn.xd.newbingbot.network.NewBingModel

data class NewBingChatConversation(
    val clientId: String = "",
    val conversationId: String = "",
    val conversationSignature: String = "",
    val uuid: String = "",
    var index: Int,
    var module: NewBingModel = NewBingModel.Creative,
    var conversationChain: MutableList<String> = mutableListOf()
)