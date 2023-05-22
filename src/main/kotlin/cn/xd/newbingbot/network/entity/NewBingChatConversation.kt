package cn.xd.newbingbot.network.entity

import cn.xd.newbingbot.network.NewBingModel

/**
 * 用来存储一次与NewBing的对话.
 *
 * @param clientId 客户端id,由new bing服务器响应获得.
 * @param conversationId 会话id,由new bing服务器响应获得.
 * @param conversationSignature 会话签名,由new bing服务器响应获得.
 * @param uuid 请求时塞到请求头"x-ms-client-request-id"里的id,由[UUID][java.util.UUID]随机生成.
 * @param index 聊天次数,目前new bing限制上线为20,但是目前代码里还没有做限制,肯定会出问题的.
 * @param module 模式,默认为创意模式,详见[cn.xd.newbingbot.network.NewBingModel].
 * @param conversationChain 用于存储会话中用户和new bing的对话信息.
 */
data class NewBingChatConversation(
    val clientId: String = "",
    val conversationId: String = "",
    val conversationSignature: String = "",
    val uuid: String = "",
//    聊天的次数
    var index: Int,
    var module: NewBingModel = NewBingModel.Creative,
    var conversationChain: MutableList<String> = mutableListOf()
)