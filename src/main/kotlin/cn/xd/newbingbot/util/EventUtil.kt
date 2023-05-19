package cn.xd.newbingbot.util

import cn.xd.newbingbot.config.authorizedGroup
import cn.xd.newbingbot.config.blacklist
import net.mamoe.mirai.event.Listener
import net.mamoe.mirai.event.MessageDsl
import net.mamoe.mirai.event.MessageEventSubscribersBuilder
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.MessageSource.Key.quote
import net.mamoe.mirai.message.data.content

@MessageDsl
fun MessageEventSubscribersBuilder.instruction(
    name: String,
    removePrefix: Boolean = true,
    trim: Boolean = true,
    onEvent: @MessageDsl suspend MessageEvent.(String) -> Unit
): Listener<MessageEvent> {
    return subscriber({
        if (this is GroupMessageEvent && it.trimStart().startsWith("@${bot.id}")){
            it.removePrefix("@${bot.id}").trimStart().startsWith(if (trim) name.trim() else name)
        }else{
            it.trimStart().startsWith(if (trim) "-${name.trim()}" else "-$name")
        }
    }){
        if (sender.id !in blacklist){
            if (this is GroupMessageEvent && subject.id in authorizedGroup && message.content.trim().startsWith("@${bot.id}")){
                onEvent(this.message.content.let { text ->
                    if (removePrefix)
                        text.removePrefix("@${bot.id}").trimStart().removePrefix(name.trimStart()).trim()
                    else
                        text.trim()
                })
            }else{
                onEvent(this.message.content.let { text ->
                    if (removePrefix)
                        text.removePrefix("@${bot.id}").trimStart().removePrefix(name.trimStart()).trim()
                    else
                        text.trim()
                })
            }
        }else{
            subject.sendMessage(message.quote() + "您已被置入黑名单,请联系管理员移除后尝试")
        }
    }
}