package cn.xd.newbingbot.config

import cn.xd.newbingbot.util.json
import kotlinx.serialization.json.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

/**
 * 全局logger对象,用来在顶层函数中打印日志,为了和普通class中的logger统一使用体验,未添加global前缀.
 */
val logger: Logger = LoggerFactory.getLogger("GlobalLogger")

/**
 * 配置对象,通过该对象来访问config.json,需要注意的是这是只读的,暂时没有写配置的功能.
 */
val config = json.parseToJsonElement(
    File("config.json").takeIf { it.exists() && it.isFile }?.readText()
        ?: throw Exception("配置文件不存在,请在工作目录创建配置文件config.json")
).jsonObject.also {
    logger.info("配置已加载")
}

/**
 * 授权的群,预设以方便访问.
 */
val authorizedGroup: List<Long> = mutableListOf<Long>().also { list ->
    config["authorized_group"]?.jsonArray?.forEach { number ->
        list.add(number.jsonPrimitive.long)
    }
}.also {
    logger.info("授权使用的群: ${it.joinToString { number -> number.toString() }}")
}

/**
 * 黑名单,预设以方便访问.
 */
val blacklist: List<Long> = mutableListOf<Long>().also { list ->
    config["blacklist"]?.jsonArray?.forEach { number ->
        list.add(number.jsonPrimitive.long)
    }
}.also {
    logger.info("黑名单(用户,非群): ${it.joinToString { number -> number.toString() }}")
}

/**
 * 所有者,也就是[UserPermissions.OWNER][cn.xd.newbingbot.util.UserPermissions.OWNER]权限的所有者,如果没有找到这项配置,那么则不会存在该权限的所有者.预设以方便访问.
 */
val owner = (config["owner"]?.jsonPrimitive?.long ?: -1).also {
    logger.info("持有人: ${
        if (it != -1L && it > 0) {
            it
        }else{
            "未找到"
        }
    }")
}

/**
 * 管理员,预设以方便访问.
 */
val admin = mutableListOf<Long>().also { list ->
    config["admin"]?.jsonArray?.forEach { number ->
        list.add(number.jsonPrimitive.long)
    }
}.also {
    logger.info("权限狗: ${it.joinToString { number -> number.toString() }}")
}

/**
 * 全局锁是否启用,预设以方便访问.
 */
private val globalLockConfig = (config["global_lock"]?.jsonPrimitive?.boolean ?: true).also {
    logger.info("全局锁状态: $it")
}

/**
 * 全局锁对象,用来实际进行锁定.
 */
private var globalLock = true

/**
 * 可供外部访问的锁对象,有更加语义化的名字.
 */
val globalLockIsNotLocking: Boolean
    get() = if (globalLockConfig){
        globalLock
    }else{
        true
    }

/**
 * 全局锁锁定.
 */
fun locking(){
    if (globalLockConfig){
        globalLock = false
    }
}

/**
 * 全局锁解锁.
 */
fun unlock(){
    if (globalLockConfig){
        globalLock = true
    }
}