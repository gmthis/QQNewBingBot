package cn.xd.newbingbot.network.entity

import cn.xd.newbingbot.config.globalLockIsNotLocking
import cn.xd.newbingbot.network.NewBingChatRequester
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.MessageSource.Key.quote
import org.slf4j.LoggerFactory
import java.io.PrintWriter
import java.io.StringWriter
import cn.xd.newbingbot.config.locking as GLocking
import cn.xd.newbingbot.config.unlock as GUnlock

/**
 * 用户与new bing的聊天状态.
 *
 * @property currentChatConversation [NewBingChatConversation] 当前选中的会话,发送消息时会发送到该会话中.
 * @property chatConversationList [MutableList]<[NewBingChatConversation]> 用户与new bing所有的聊天记录.
 * @property nextChatIsNewChat [Boolean] 下一次会话是否是新聊天,默认是false.一定不要再外部修改该值.
 * @property chatIsNotLock [Boolean] 聊天是否被锁定,这与全局锁不同,即使全局锁关闭,用户仍然只能同时进行一次对话.
 */
data class UserChatInfo(
    var currentChatConversation: NewBingChatConversation,
    val chatConversationList: MutableList<NewBingChatConversation>,
    var nextChatIsNewChat: Boolean = false,
    var chatIsNotLock: Boolean = true
) {
    companion object {
        /**
         * 创建一个新的[UserChatInfo].
         *
         * @param requester 使用的请求器.
         * @return [UserChatInfo?][UserChatInfo] 如果返回为空,则表示内部出现了异常或者全局锁正在生效.
         */
        fun creatNewUserChatInfo(requester: NewBingChatRequester): UserChatInfo? {
            if (globalLockIsNotLocking) {
                GLocking()
                val bingChat = try {
                    requester.creatNewChat()
                } catch (e: Exception) {
                    logger.warn("链接出现了读写错误,堆栈信息:\n${
                        StringWriter().also {
                            PrintWriter(it).use { pw ->
                                e.printStackTrace(pw)
                            }
                        }
                    }")
                    null
                }
                GUnlock()
                return bingChat?.let { UserChatInfo(it, mutableListOf(bingChat)) }
            }
            return null
        }

        /**
         * logger.
         */
        private val logger = LoggerFactory.getLogger(NewBingChatRequester::class.java)
    }

    /**
     * 为当前的会话发送一条新的消息,消息的响应会被丢到[requester]的[channel][cn.xd.newbingbot.network.NewBingChatRequester.channel]中,监听该[channel][cn.xd.newbingbot.network.NewBingChatRequester.channel]来处理响应.
     *
     * @param message 消息内容.
     * @param requester 使用的请求器.
     * @param messageEvent 消息事件,在函数最大的的用处就是塞给[requester],让[requester]塞到[cn.xd.newbingbot.network.NewBingChatRequester.channel]中.
     */
    suspend fun sendNewBingChatMessage(message: String, requester: NewBingChatRequester, messageEvent: MessageEvent) {
        if (chatIsNotLock && globalLockIsNotLocking) {
            locking()
//            如果这次需要创建新的会话.
            if (nextChatIsNewChat) {
                val bingChat = try {
                    requester.creatNewChat()
                } catch (e: Exception) {
                    logger.warn("链接出现了读写错误,堆栈信息:\n${
                        StringWriter().also {
                            PrintWriter(it).use { pw ->
                                e.printStackTrace(pw)
                            }
                        }
                    }")
                    unlock()
                    messageEvent.subject.sendMessage(messageEvent.message.quote() + "出现了错误,已释放锁,请通知所有者查看控制台")
                    return
                }
//                将新会话的标识位拨回.
                nextChatIsNewChat = false
                chatConversationList.add(bingChat)
                currentChatConversation = bingChat
            }
            currentChatConversation.conversationChain.add(message)
            requester.sendMessage(currentChatConversation, message, messageEvent)
            unlock()
        } else {
            messageEvent.subject.sendMessage(messageEvent.message.quote() + "聊天已锁定,请等待上个聊天的结束")
        }
    }

    /**
     * 重新选择[currentChatConversation]的值.
     *
     * @param target 要选的值在[chatConversationList]中的下标.
     * @return [Boolean] true则成功, false则说明找不到下标指定的对象,可能是下标不存在或者为负数.
     */
    fun switchChatContext(target: Int): Boolean {
        currentChatConversation = chatConversationList.getOrNull(target) ?: return false
        return true
    }

    /**
     * 下次发消息创建新会话.
     */
    fun newChat() {
        nextChatIsNewChat = true
    }

    /**
     * 加锁.
     */
    private fun locking() {
        GLocking()
        chatIsNotLock = false
    }

    /**
     * 解锁.
     */
    private fun unlock() {
        GUnlock()
        chatIsNotLock = true
    }
}