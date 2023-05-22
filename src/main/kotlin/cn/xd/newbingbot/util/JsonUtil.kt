package cn.xd.newbingbot.util

import kotlinx.serialization.json.Json

/**
 * json格式化对象,配置了打印的格式化,理论上来说应该会好看很多(但因为很多时候打印都是jsonObject直接塞进去,所以根本没用😥)
 */
val json = Json {
    prettyPrint = true
}