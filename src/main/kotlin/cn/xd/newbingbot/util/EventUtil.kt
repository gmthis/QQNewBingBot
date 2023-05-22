package cn.xd.newbingbot.util

import cn.xd.newbingbot.config.admin
import cn.xd.newbingbot.config.authorizedGroup
import cn.xd.newbingbot.config.blacklist
import cn.xd.newbingbot.config.owner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import net.mamoe.mirai.event.Listener
import net.mamoe.mirai.event.MessageDsl
import net.mamoe.mirai.event.MessageEventSubscribersBuilder
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.MessageSource.Key.quote
import net.mamoe.mirai.message.data.content

/**
 * 全局的协程作用域,工作在[Dispatchers.IO]调度器上.
 */
val globalCoroutineScope = CoroutineScope(Dispatchers.IO)

/**
 * 标志着用户所拥有的权限.
 *
 * @property OWNER 所有者,拥有所有指令的权限.
 * @property ADMIN 管理员,拥有部分敏感指令的权限.
 * @property ORDINARY 普通用户,可以使用绝大多数非敏感指令.
 */
enum class UserPermissions {
    /**所有者,拥有所有指令的权限.*/ OWNER, /**管理员,拥有部分敏感指令的权限.*/ ADMIN, /**普通用户,可以使用绝大多数非敏感指令.*/ ORDINARY
}

/**
 * 快速为群和好友创建有着不同的触发方式的指令.
 *
 * 该函数内会进行用户和群的双重鉴权,以保证只在正确的环境下触发指令.
 *
 * 示例:
 *
 * ```kotlin
 * instruction("info"){
 *     subject.sendMessage(it)
 * }
 * ```
 *
 * 此时在群里,只需要 @bot info 123 即可收到bot的回复 123.
 *
 * 而在好友对话中,则可以直接 -info 123 即可收到bot的回复 123.
 *
 * 暂时不支持在好友对话中自定义前缀符号.
 *
 * @param name 指令名.
 * @param permissionsRequested 指令要求的权限,默认为[UserPermissions.ORDINARY],详见[UserPermissions].
 * @param removePrefix 是否删除前缀,默认为true.示例(箭头后为[onEvent]中it的实参): true: @bot info 123 -> 123. false: @bot info 123 -> @bot info 123.
 * @param trim 是否删除指令名(也就是[name])的前导空格.注意!这不会删除尾随空格!
 * @param onEvent 想要注册的事件本身,参数中的String为根据[removePrefix]设置处理过后的实际文本.
 * @receiver [MessageEventSubscribersBuilder] 用来在mirai event dsl中构建事件链的对象.
 * @return [Listener] 泛型[MessageEvent] 通过该对象来对注册的事件进行操作,如关闭监听.
 */
@MessageDsl
fun MessageEventSubscribersBuilder.instruction(
    name: String,
    permissionsRequested: UserPermissions = UserPermissions.ORDINARY,
    removePrefix: Boolean = true,
    trim: Boolean = true,
    onEvent: @MessageDsl suspend MessageEvent.(String) -> Unit
): Listener<MessageEvent> {
    return subscriber({
//        暂时没有考虑除了正常群和好友以外的类型,所以临时会话如果没有继承自GroupMessageEvent,那么也会通过好友的处理方式触发.
        if (this is GroupMessageEvent) {
//            如果在是群消息那么需要同时满足被@和带有指令名两种要求.
            if (it.trimStart().startsWith("@${bot.id}")) {
                it.removePrefix("@${bot.id}").trimStart().startsWith(if (trim) name.trimStart() else name)
            } else false
        } else {
//            好友类型会特别判断"-"号.
            it.trimStart().startsWith(if (trim) "-${name.trimStart()}" else "-$name")
        }
    }) {
//        三层鉴权,依次为黑名单,用户权限和群权限.
        if (sender.id !in blacklist) {
            if (when (permissionsRequested) {
                    UserPermissions.OWNER -> sender.id == owner
                    UserPermissions.ADMIN -> sender.id in admin || sender.id == owner
                    UserPermissions.ORDINARY -> true
                }
            ) {
                if (this is GroupMessageEvent) {
//                    这里是第三层,只有群类型会有.
                    if (subject.id !in authorizedGroup) {
                        subject.sendMessage(message.quote() + "该群未授权,请联系管理员授权后尝试")
                    } else {
//                        根据removePrefix参数清理前缀.
                        onEvent(this.message.content.let { text ->
                            if (removePrefix)
                                text.removePrefix("@${bot.id}").trimStart().removePrefix(name.trimStart()).trim()
                            else
                                text.trim()
                        })
                    }
                } else {

//                        根据removePrefix参数清理前缀.
                    onEvent(this.message.content.let { text ->
                        if (removePrefix)
                            text.removePrefix("@${bot.id}").trimStart().removePrefix("-${name.trimStart()}").trim()
                        else
                            text.trim()
                    })
                }
            } else {
                subject.sendMessage(message.quote() + "您没有该权限,无法操作.")
            }
        } else {
            subject.sendMessage(message.quote() + "您已被置入黑名单,请联系管理员移除后尝试")
        }
    }
}