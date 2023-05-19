package cn.xd.newbingbot.network

import cn.xd.newbingbot.config.config
import cn.xd.newbingbot.network.entity.NewBingChatConversation
import cn.xd.newbingbot.util.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import okhttp3.*
import okio.ByteString
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class NewBingChatRequester @Throws(Exception::class) constructor() {

    companion object{
        val logger = LoggerFactory.getLogger(NewBingChatRequester::class.java) ?: throw Exception("意料之外的异常,logger构造失败")
        const val RS = "\u001e"
        const val NEW_MESSAGE = "{\"protocol\":\"json\",\"version\":1}$RS"
        const val OK = "{\"type\":6}$RS"
    }

    private val httpClient = OkHttpClient.Builder()
        .cookieJar(object: CookieJar{
//            自动从指定的目录下读取cookie
            override fun loadForRequest(url: HttpUrl): List<Cookie> {
                val cookieDir = config["cookie_dir"] ?: throw Exception("没有找到cookieDir配置,请在config.json中添加该配置")
                val cookieFile = File("${cookieDir.jsonPrimitive.content}/${url.host}.cookie")
                if (!cookieFile.exists()){
                    if (!cookieFile.parentFile.exists()) cookieFile.parentFile.mkdirs()
                    cookieFile.createNewFile()
                    return emptyList()
                }
                val cookieSource = cookieFile.readText()
                if (cookieSource.trim().isEmpty()) return emptyList()
                val cookieList = cookieSource.split(";")
                val result = mutableListOf<Cookie>()
                for (cookieItemSource in cookieList) {
                    val cookieItemSourceTrim = cookieItemSource.trim()
                    val separator = cookieItemSourceTrim.indexOf("=")
                    val key = cookieItemSourceTrim.substring(0, separator)
                    val value = cookieItemSourceTrim.substring(separator + 1)
                    result.add(Cookie.Builder().name(key).value(value).hostOnlyDomain(url.host).build())
                }
                return result
            }

//            但并不会保存,保存的话下次请求就会type 7,原因不明
            override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
//                val cookieDir = config["cookieDir"] ?: throw Exception("没有找到cookieDir配置,请在config.json中添加该配置")
//                val cookieFile = File("${cookieDir.jsonPrimitive.content}/${url.host}.cookie")
//                if (!cookieFile.exists()){
//                    if (!cookieFile.parentFile.exists()) cookieFile.parentFile.mkdirs()
//                    cookieFile.createNewFile()
//                }
//                val result = StringBuilder()
//                cookies.forEachIndexed { index, cookie ->
//                    val item = "${cookie.name}=${cookie.value}"
//                    result.append(item)
//                    if (index != cookies.lastIndex){
//                        result.append("; ")
//                    }
//                }
//                cookieFile.writeText(result.toString())
            }

        })
//        上代理,没代理反正用不了
        .proxy(Proxy(Proxy.Type.HTTP, InetSocketAddress(
            config["proxy_host"]?.jsonPrimitive?.content ?: throw Exception("没有找到proxyHost配置或该配置配置错误,请在config.json中添加或该配置"),
            config["proxy_port"]?.jsonPrimitive?.int ?: throw Exception("没有找到proxyPort配置或该配置配置错误,请在config.json中添加或修改该配置")
        )))
//        意义不明,理论上不需要这个心跳包
        .pingInterval(2, TimeUnit.SECONDS)
        .build()

    /**
     * 事件通道,当响应完成后会把结果塞到通道里
     */
    val channel = Channel<JsonObject>(2)
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    /**
     * 请求创建一个新的会话
     * @return [NewBingChatConversation] 保存了会话相关的数据
     */
    @Throws(IOException::class)
    suspend fun creatNewChat(): NewBingChatConversation {
        val uuid = UUID.randomUUID()
        val ip = Random(uuid.hashCode()).run {
            "13.${nextInt(104, 107)}.${nextInt(0, 255)}.${nextInt(0, 255)}"
        }
        val request = Request.Builder()
            .get()
            .url("https://www.bing.com/turing/conversation/create")
            .header("x-ms-client-request-id", uuid.toString())
            .header("x-ms-useragent", "azsdk-js-api-client-factory/1.0.0-beta.1 core-rest-pipeline/1.10.0 OS/MacIntel")
            .header("accept-language", "zh-CN,zh;q=0.9")
            .header("x-forwarded-for", ip)
            .build()
        val call = httpClient.newCall(request)
        val response = call.execute()
        val body = response.body?.string() ?: throw IOException("服务器未正确返回,请确认代理配置")
        val jsonObject = json.decodeFromString<JsonObject>(body)
        val clientId = jsonObject["clientId"]?.jsonPrimitive?.content ?: throw IOException("未在响应体中找到clientId参数")
        val conversationId = jsonObject["conversationId"]?.jsonPrimitive?.content ?: throw IOException("未在响应体中找到conversationId参数")
        val conversationSignature = jsonObject["conversationSignature"]?.jsonPrimitive?.content ?: throw IOException("未在响应体中找到conversationSignature参数")
        if ((jsonObject["result"]?.jsonObject?.get("value")?.jsonPrimitive?.content) != "Success"){
            throw IOException("出现了预料之外的错误,响应体为:\n${jsonObject}")
        }
        return NewBingChatConversation(
            clientId = clientId,
            conversationId = conversationId,
            conversationSignature = conversationSignature,
            uuid = uuid.toString(),
            index = 0
        )
    }


    /**
     * 用指定的会话发送一个新的消息
     */
    suspend fun sendMessage(
        conversation: NewBingChatConversation,
        text: String
    ){
        val webSocket = creatWebSocket { webSocket, messageSource ->
            for (messageItemContentSource in messageSource.split("\u001e")) {
                if (messageItemContentSource.trim().isEmpty()) break
                val messageContent = json.decodeFromString<JsonObject>(messageItemContentSource)
                when (messageContent["type"]?.jsonPrimitive?.int) {
                    1 -> {
                        logger.debug(messageContent.toString())
                    }
                    2 -> {
                        logger.debug(messageContent.toString())
                        logger.info("该次会话所有讯息均已接收")
                        coroutineScope.launch {
                            logger.debug("发送至通道中")
                            channel.send(messageContent)
                            logger.debug("发送完毕")
                        }
                    }
                    3 -> {
                        logger.debug(messageContent.toString())
                        logger.info("该次会话结束,正在尝试关闭会话")
                        webSocket.close(1000, null)
                    }
                    6 -> {
                        logger.debug("接收到了心跳包,已返回心跳包")
                        webSocket.send(OK)
                    }

                    7 -> {
                        logger.debug(messageContent.toString())
                        logger.warn("上一次会话没有正确关闭! 接收到的消息:$messageContent")
                    }
                    else -> {
                        logger.debug(messageContent.toString())
                        if (messageContent.isNotEmpty())
                            logger.info("出现了预料之外的异常,接收到的消息:$messageContent")
                    }
                }
            }
        }
        webSocket.send(NEW_MESSAGE)
        webSocket.send(OK)
        webSocket.send(buildJsonObject {
            put("type", 4)
            putJsonArray("arguments") {
                addJsonObject {
                    put("source", "cib")
                    putJsonArray("optionsSets") {
                        for (option in NewBingChatConfig.options) {
                            add(option)
                        }
                        add(conversation.module.value)
                    }
                    putJsonArray("allowedMessageTypes") {
                        for (type in NewBingChatConfig.allowed) {
                            add(type)
                        }
                    }
                    putJsonArray("sliceIds") {
                        for (id in NewBingChatConfig.sliceIds) {
                            add(id)
                        }
                    }
                    put("isStartOfSession", conversation.index == 0)
                    put("conversationId", conversation.conversationId)
                    put("conversationSignature", conversation.conversationSignature)
                    putJsonObject("participant") {
                        put("id", conversation.clientId)
                    }

                    putJsonObject("message") {
                        put("author", "user")
                        put("inputMethod", "Keyboard")
                        put("text", text)
                        put("messageType", "Chat")
                    }
                }
            }
            put("invocationId", "${conversation.index++}")
            put("target", "chat")
        }.toString() + RS)
    }

    /**
     * 用于创建websocket
     */
    private fun creatWebSocket(messageHandle: (webSocket: WebSocket, text: String) -> Unit): WebSocket {
        val request = Request.Builder()
            .url("wss://sydney.bing.com/sydney/ChatHub")
            .get()
            .addHeader("accept-language", "zh-CN,zh;q=0.9")
            .build()
        return httpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                super.onClosed(webSocket, code, reason)
                logger.info("关闭了WebSocket链接, code: $code, reason(正常应为空或null): $reason")
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                super.onClosing(webSocket, code, reason)
                logger.info("远程服务器请求关闭链接,原因: code: $code, reason: $reason. 正在尝试关闭.")
                webSocket.close(1000, null)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                super.onFailure(webSocket, t, response)
                logger.warn("链接出现了读写错误,堆栈信息:\n${t.cause}")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                super.onMessage(webSocket, text)
                messageHandle(webSocket, text)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                super.onMessage(webSocket, bytes)
                logger.info("服务端返回了bytes信息,暂未实现对该信息的处理")
            }

            override fun onOpen(webSocket: WebSocket, response: Response) {
                super.onOpen(webSocket, response)
                logger.info("WebSocket链接已被正常打开")
            }
        })
    }

}