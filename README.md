# NewBingBot

## 简介

一个可以登录qq的机器人,用来和new bing聊天,和朋友聊天开玩笑然后就搓了,很简陋,仅限于能用(而且不好用,有很多问题都没修),
别的别指望了.如果你正在学习kotlin或者mirai,可以考虑看一下,不过项目还有写注释,但很简单,代码量也不多.

项目依赖于[mirai](https://github.com/mamoe/mirai)实现.

mirai项目是用kotlin编写的qq协议的实现,用于实现一些qq的机器人操作.

kotlin由喷气脑(jetbrains)开发的jvm平台语言,与java有着近乎100%的完美互操作性,且拥有着现代的语法,大量的语法糖,十分适合自己
闲的没事摸摸代码的时候使用.

## 配置
```json
{
  "cookie_dir": "",
  "proxy_host": "",
  "proxy_port": 0,
  "qq": 0,
  "qq_password": "",
  "qq_login_method": "qrCode",
  "login_protocol": "ANDROID_WATCH",
  "global_lock": true,
  "authorized_group": [0],
  "mirai_workdir": ".",
  "mirai_cache": "cache",
  "blacklist": [0]
}
```
目前只有这些配置文件,有些配置是必须比如cookie_dir,proxy_hose,proxy_port,qq等,有些则不是,有这些差异主要是因为写着
写着风格就变了,害

下面是用一些配置的取值

qq_login_method(可不存在): qrCode, paasword(实际上不是qrCode就是密码登录)
login_protocol(可不存在,默认是ANDROID_WATCH): ANDROID_PHONE, ANDROID_PAD, ANDROID_WATCH, IPAD, MACOS

记得在cookie_dir指定的目录下面创建文件www.bing.com.cookies然后将new bing的cookie粘进去
具体操作方法是,登录new bing,F12,控制台直接打印document.cookie,从中找到_U,key和值直接粘到文件里面即可,分号带不带都行

## 启动
jar和配置文件放在一个目录下然后`java -jar fileName.jar`就完事了.

## 使用
在群中直接@bot,然后通过指令触发事件,详细列表如下
```text
chat *: 向NewBing发一条消息,如果没有会话会创建一个新的会话(默认创意模式)
new: 下次会话会创建一个新的会话(默认为创意模式)
model *: 切换选中会话的模式,共三个取值: 创意,平衡,严谨
cinfo: 查看已选中会话的信息
list: 查看保存的所有会话(过久的会话应该会被服务器删除,不太清楚机制)
choose *: 切换会话上下文,*为任意在取值范围内的由阿拉伯数字组成的正整数
clear: 清除所有保存的会话(暂不支持删除一个,如果发送了该命令,会直接清空,谨慎使用)
help: 查看帮助
info: 查看bot信息
```
如果私聊则不需要@(也@不了),直接在命令前添加'-'即可,如'-help'

## api
连接用到的api都在[api](api.md)里面了,操作流程我印象里也写了.

## 参考
因为有很多东西都不会,所以实际上参考了很多内容,十分感谢各位大佬的无私贡献让我这种菜鸡可以参考.

[在Node终端实现NewBing对话功能](https://juejin.cn/post/7223563681878655037#heading-11)

[微软new bing chatgpt 逆向爬虫实战](https://zhuanlan.zhihu.com/p/609240938)

[mirai-new-bing](https://github.com/cssxsh/mirai-new-bing/tree/main) 尤其是该项目,由于是同一个语言且均依托于mirai
,所以我很多代码都有借鉴与参考该项目,十分感谢.

## 依赖
okhttp3

slf4j

slf4j-simple

kotlinx-serialization

kotlinx-coroutines

mirai

详见[build.gradle.kts](build.gradle.kts)