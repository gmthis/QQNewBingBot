package cn.xd.newbingbot

import cn.xd.newbingbot.config.config
import cn.xd.newbingbot.config.logger
import cn.xd.newbingbot.network.NewBingChatConfig
import cn.xd.newbingbot.network.NewBingChatRequester
import cn.xd.newbingbot.network.NewBingModel
import cn.xd.newbingbot.network.entity.UserChatInfo
import cn.xd.newbingbot.util.UserPermissions
import cn.xd.newbingbot.util.instruction
import kotlinx.coroutines.*
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import net.mamoe.mirai.BotFactory
import net.mamoe.mirai.auth.BotAuthorization
import net.mamoe.mirai.event.subscribeMessages
import net.mamoe.mirai.message.data.MessageSource.Key.quote
import net.mamoe.mirai.utils.BotConfiguration
import java.io.File

fun main() = runBlocking {
//    记录开机时间
    val startTimeMillis = System.currentTimeMillis()
//    映射着用户聊天信息的map,没有持久化,关了就没了
    val userNewBingChatMap = mutableMapOf<Long, UserChatInfo>()
    val workdir = config["mirai_workdir"]?.jsonPrimitive?.content ?: "."
    val cacheDir = config["mirai_cache"]?.jsonPrimitive?.content ?: "cache"
//    创建bot,根据配置文件中设置的登陆方式决定,默认是扫码
    val bot = if ((config["qq_login_method"]?.jsonPrimitive?.content ?: "qrCode") == "qrCode") {
        BotFactory.newBot(
            config["qq"]?.jsonPrimitive?.long
                ?: throw Exception("没有找到qq配置或该配置配置错误,请在config.json中添加或该配置"),
            BotAuthorization.byQRCode()
        ) {
            protocol = BotConfiguration.MiraiProtocol.ANDROID_WATCH
            workingDir = File(workdir)
            this.cacheDir = File(cacheDir)
            fileBasedDeviceInfo("$cacheDir/device.json")
        }
    } else {
        BotFactory.newBot(
            config["qq"]?.jsonPrimitive?.long
                ?: throw Exception("没有找到qq配置或该配置配置错误,请在config.json中添加或该配置"),
            config["qq_password"]?.jsonPrimitive?.content
                ?: throw Exception("没有找到qq配置或该配置配置错误,请在config.json中添加或该配置")
        ) {
            protocol = when (config["login_protocol"]?.jsonPrimitive?.content ?: "ANDROID_WATCH") {
                "ANDROID_PHONE" -> BotConfiguration.MiraiProtocol.ANDROID_PHONE
                "ANDROID_WATCH" -> BotConfiguration.MiraiProtocol.ANDROID_WATCH
                "ANDROID_PAD" -> BotConfiguration.MiraiProtocol.ANDROID_PAD
                "IPAD" -> BotConfiguration.MiraiProtocol.IPAD
                "MACOS" -> BotConfiguration.MiraiProtocol.MACOS
                else -> BotConfiguration.MiraiProtocol.ANDROID_WATCH
            }
            fileBasedDeviceInfo("")
        }
    }
    val requester = NewBingChatRequester()
    logger.info("NewBing请求客户端创建完成")
    bot.login()
    logger.info("QQ登陆成功")

    val replyJob = launch(
        Dispatchers.IO
    ) {
        try {
            while (isActive) {
                val (event,jsonObject, currentChatConversation) = requester.channel.receive()
                val array = jsonObject["item"]?.jsonObject?.get("messages")?.jsonArray ?: continue
                val response = array.first { element ->
                    element.jsonObject["suggestedResponses"] != null
                }.jsonObject
                val result = response["text"]?.jsonPrimitive?.content ?: "接收到了错误的内容"
                currentChatConversation.conversationChain.add(result)
                event.subject.sendMessage(event.message.quote() + result)
                logger.info("在主题: ${event.subject}中向用户: ${event.sender}发送消息: $result")
            }
        }catch (e: CancellationException){
            requester.channel.cancel()
            logger.info("消息回复事件处理器已关闭")
        }
    }
    logger.info("消息回复事件处理器已启动")

    bot.eventChannel.subscribeMessages {
        instruction("chat") {
            val info = userNewBingChatMap[sender.id] ?: run {
                val info = UserChatInfo.creatNewUserChatInfo(requester) ?: run {
                    subject.sendMessage(message.quote() + "聊天已锁定,请等待上个聊天的结束")
                    return@instruction
                }
                userNewBingChatMap[sender.id] = info
                info
            }
            info.sendNewBingChatMessage(it, requester, this)
        }
        instruction("new") {
            val info = userNewBingChatMap[sender.id] ?: run {
                subject.sendMessage(message.quote() + "没有找到聊天信息")
                return@instruction
            }
            info.newChat()
            subject.sendMessage(message.quote() + NewBingChatConfig.newChatReceipt.random())
        }
        instruction("model") {
            val info = userNewBingChatMap[sender.id] ?: run {
                subject.sendMessage(message.quote() + "没有找到聊天信息")
                return@instruction
            }
            info.currentChatConversation.module = when (it) {
                "创意" -> NewBingModel.Creative
                "平衡" -> NewBingModel.Balanced
                "严谨" -> NewBingModel.Precise
                else -> NewBingModel.Creative
            }.also { model ->
                subject.sendMessage(message.quote() + "已设置模式为${model.chinese}")
            }
        }
        instruction("cinfo") {
            val info = userNewBingChatMap[sender.id] ?: run {
                subject.sendMessage(message.quote() + "没有找到聊天信息")
                return@instruction
            }
            if (info.nextChatIsNewChat) {
                subject.sendMessage(message.quote() + "没有找到聊天信息")
                return@instruction
            }
            subject.sendMessage(
                message.quote() + "第一句:${
                    info.currentChatConversation.conversationChain.first().let { title ->
                        if (title.length > 30) {
                            title.substring(0, 25) + "..."
                        } else {
                            title
                        }
                    }
                }\n" +
                "已聊天次数: ${info.currentChatConversation.index}\n" +
                "模式: ${info.currentChatConversation.module.chinese}"
            )
        }
        instruction("list") {
            val info = userNewBingChatMap[sender.id] ?: run {
                subject.sendMessage(message.quote() + "没有找到聊天信息")
                return@instruction
            }
            if (info.chatConversationList.isEmpty()) {
                subject.sendMessage(message.quote() + "没有找到聊天信息")
                return@instruction
            }
            var result = "找到以下聊天\n"
            info.chatConversationList.forEachIndexed() { index, element ->
                result =
                    "$result$index. ${element.conversationChain.first()}${if (index != info.chatConversationList.lastIndex) "\n" else ""}"
            }
            subject.sendMessage(message.quote() + result)
        }
        instruction("choose") {
            val index = it.toIntOrNull()
            if (index == null || index < 0) {
                subject.sendMessage(message.quote() + "错误的参数,应为阿拉伯数字组成的非负整数")
                return@instruction
            }
            val info = userNewBingChatMap[sender.id] ?: run {
                subject.sendMessage(message.quote() + "没有找到聊天信息")
                return@instruction
            }
            if (index in info.chatConversationList.indices) {
                info.switchChatContext(index)
                subject.sendMessage(
                    message.quote() + "已切换话题至${
                        info.chatConversationList[index].conversationChain.first().let { title ->
                            if (title.length > 30) {
                                title.substring(0, 25) + "..."
                            } else {
                                title
                            }
                        }
                    }"
                )
            } else {
                subject.sendMessage(message.quote() + "请求切换的条目不存在")
                return@instruction
            }
        }
        instruction("clear") {
            val info = userNewBingChatMap[sender.id] ?: run {
                subject.sendMessage(message.quote() + "没有找到聊天信息")
                return@instruction
            }
            info.chatConversationList.clear()
            info.newChat()
            subject.sendMessage(message.quote() + "已清理所有保存的会话,下次会话会重新开始")
        }
        instruction("help") {
            subject.sendMessage(
                message.quote() + "chat *: 向NewBing发一条消息,如果没有会话会创建一个新的会话(默认创意模式)\n" +
                        "new: 下次会话会创建一个新的会话(默认为创意模式)\n" +
                        "model *: 切换选中会话的模式,共三个取值: 创意,平衡,严谨\n" +
                        "cinfo: 查看已选中会话的信息\n" +
                        "list: 查看保存的所有会话(过久的会话应该会被服务器删除,不太清楚机制)\n" +
                        "choose *: 切换会话上下文,*为任意在取值范围内的由阿拉伯数字组成的正整数\n" +
                        "clear: 清除所有保存的会话(暂不支持删除一个,如果发送了该命令,会直接清空,谨慎使用)\n" +
                        "help: 查看帮助\n" +
                        "info: 查看bot信息\n" +
                        "shutdown: 关闭bot,这需要最高权限,所以你就别试啦~"
            )
        }
        instruction("info") {
            subject.sendMessage(
                message.quote() + "该bot依托于Mirai(项目地址:https://github.com/mamoe/mirai)实现.\n" +
                        "Mirai version: 2.15.0-M\n" +
                        "application version: 1.0-simple\n" +
                        "已运行: ${(System.currentTimeMillis() - startTimeMillis) / 3600000} 小时"
            )
        }
        instruction("shutdown", UserPermissions.OWNER){
            subject.sendMessage(message.quote() + "正在尝试关闭程序")
            bot.close()
            logger.info("机器人已关闭")
            replyJob.cancel()
            logger.info("已关闭手头能关闭的所有资源,请等待其余线程的关闭")
        }
        logger.info("bot消息事件监听挂载完成")
    }
}