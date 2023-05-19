package cn.xd.newbingbot.config

import cn.xd.newbingbot.util.json
import kotlinx.serialization.json.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

val logger: Logger = LoggerFactory.getLogger("GlobalLogger")

val config = json.parseToJsonElement(
    File("config.json").takeIf { it.exists() && it.isFile }?.readText()
        ?: throw Exception("配置文件不存在,请在工作目录创建你配置文件config.json")
).jsonObject.also {
    logger.info("配置已加载")
}

val authorizedGroup = mutableListOf<Long>().also { list ->
    config["authorized_group"]?.jsonArray?.forEach { number ->
        list.add(number.jsonPrimitive.long)
    }
}.also {
    logger.info("授权使用的群: ${it.joinToString { number -> number.toString() }}")
}

val blacklist = mutableListOf<Long>().also { list ->
    config["blacklist"]?.jsonArray?.forEach { number ->
        list.add(number.jsonPrimitive.long)
    }
}.also {
    logger.info("黑名单(用户,非群): ${it.joinToString { number -> number.toString() }}")
}

private val globalLockConfig = (config["global_lock"]?.jsonPrimitive?.boolean ?: true).also {
    logger.info("全局锁状态: $it")
}

private var globalLock = true

val globalLockIsNotLocking: Boolean
    get() = if (globalLockConfig){
        globalLock
    }else{
        true
    }

fun locking(){
    if (globalLockConfig){
        globalLock = false
    }
}

fun unlock(){
    if (globalLockConfig){
        globalLock = true
    }
}