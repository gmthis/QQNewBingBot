package cn.xd.newbingbot.network.entity

import cn.xd.newbingbot.config.globalLockIsNotLocking
import cn.xd.newbingbot.network.NewBingChatRequester
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.MessageSource.Key.quote
import org.slf4j.LoggerFactory
import cn.xd.newbingbot.config.locking as GLocking
import cn.xd.newbingbot.config.unlock as GUnlock

data class UserChatInfo(
    var currentChatConversation: NewBingChatConversation,
    val chatConversationList: MutableList<NewBingChatConversation>,
    var nextChatIsNewChat: Boolean = false,
    var chatIsNotLock: Boolean = true
){

    companion object{
        suspend fun creatNewUserChatInfo(requester: NewBingChatRequester): UserChatInfo? {
            if (globalLockIsNotLocking){
                GLocking()
                val bingChat = requester.creatNewChat()
                GUnlock()
                return UserChatInfo(bingChat, mutableListOf(bingChat))
            }
            return null
        }
        val logger = LoggerFactory.getLogger(NewBingChatRequester::class.java) ?: throw Exception("意料之外的异常,logger构造失败")
    }

    /**
     * 为当前的会话发送一条新的消息,同时会监听消息通道来获取bing的回应.
     *
     * 该方案十分十分十分十分十分不合理,但是我懒得改🥱
     */
    suspend fun sendNewBingChatMessage(message: String, requester: NewBingChatRequester, messageEvent: MessageEvent){
        if (chatIsNotLock && globalLockIsNotLocking){
            locking()
            if (nextChatIsNewChat){
                val bingChat = requester.creatNewChat()
                nextChatIsNewChat = false
                chatConversationList.add(bingChat)
                currentChatConversation = bingChat
            }
            currentChatConversation.conversationChain.add(message)
            requester.sendMessage(currentChatConversation, message)
            val jsonObject = requester.channel.receive()
            val array = jsonObject["item"]?.jsonObject?.get("messages")?.jsonArray ?: return
            val response = array.first {element ->
                element.jsonObject["suggestedResponses"] != null
            }.jsonObject
            val result = response["text"]?.jsonPrimitive?.content ?: "接收到了错误的内容"
            currentChatConversation.conversationChain.add(result)
            messageEvent.subject.sendMessage(messageEvent.message.quote() + result)
            logger.info("在主题: ${messageEvent.subject}中向用户: ${messageEvent.sender}发送消息: $result")
            unlock()
        }else{
            messageEvent.subject.sendMessage(messageEvent.message.quote() + "聊天已锁定,请等待上个聊天的结束")
        }
    }

    fun switchChatContext(target: Int): Boolean{
        currentChatConversation = chatConversationList.getOrNull(target) ?: return false
        return true
    }

    fun newChat(){
        nextChatIsNewChat = true
    }

    fun locking(){
        GLocking()
        chatIsNotLock = false
    }

    fun unlock(){
        GUnlock()
        chatIsNotLock = true
    }
}