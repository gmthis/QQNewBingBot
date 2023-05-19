创建聊天

`"/turing/conversation/create"`

请求方式是get

响应格式

```json
{
    "conversationId": "",
    "clientId": "",
    "conversationSignature": "",
    "result": {
        "value": "Success",
        "message": null
    }
}
```

收发信息(WebSocket)

`wss://sydney.bing.com/sydney/ChatHub`

发送格式

```json
{
  "protocol": "json",
  "version": 1
}\u001e
```
```json
{"type":6}\u001e
```
```json
{
  "arguments": [
    {
      "source": "cib",
      "optionsSets": [
        "nlu_direct_response_filter",
        "deepleo",
        "disable_emoji_spoken_text",
        "responsible_ai_policy_235",
        "enablemm",
        "h3imaginative",
        "clgalileo",
        "gencontentv3",
        "osbsdusgrec",
        "cricketansgnd",
        "dagslnv1",
        "sportsansgnd",
        "dv3sugg",
        "autosave"
      ],
      "allowedMessageTypes": [
        "ActionRequest",
        "Chat",
        "Context",
        "InternalSearchQuery",
        "InternalSearchResult",
        "Disengaged",
        "InternalLoaderMessage",
        "Progress",
        "RenderCardRequest",
        "AdsQuery",
        "SemanticSerp",
        "GenerateContentQuery",
        "SearchQuery"
      ],
      "sliceIds": [
        "winmuid1tf",
        "516ajcome",
        "512suptone",
        "convtnknbsupp",
        "osbsdusgrec",
        "noaddsyreq",
        "ttstmoutcf",
        "winstmsg2tf",
        "norespwcf",
        "tempcacheread",
        "temptacache",
        "505iccric",
        "505suggs0",
        "508jbcar",
        "425b2pctrl",
        "430rai267s0",
        "0516conv1",
        "515vaoprvs",
        "424dagslnv1sp",
        "427startpms0",
        "427vserps0",
        "512bicp1"
      ],
      "verbosity": "verbose",
      "traceId": "",
      "isStartOfSession": true,
      "message": {
        "locale": "zh-CN",
        "market": "zh-CN",
        "region": "HK",
        "location": "lat:47.639557;long:-122.128159;re=1000m;",
        "locationHints": [
          {
            "country": "Hong Kong",
            "state": "Hong Kong",
            "city": "Aberdeen",
            "timezoneoffset": 8,
            "countryConfidence": 8,
            "Center": {
              "Latitude": 2.2,
              "Longitude": 1.1
            },
            "RegionType": 2,
            "SourceType": 1
          }
        ],
        "timestamp": "",
        "author": "user",
        "inputMethod": "Keyboard",
        "text": "",
        "messageType": "Chat",
        "requestId": "",
        "messageId": ""
      },
      "tone": "Creative",
      "requestId": "",
      "conversationSignature": "",
      "participant": {
        "id": ""
      },
      "conversationId": ""
    }
  ],
  "invocationId": "0",
  "target": "chat",
  "type": 4
}\u001e
```
连续发送上面三条
这其中有一部分是不需要的,具体请看[NewBingChatRequester.kt](src/main/kotlin/cn/xd/newbingbot/network/NewBingChatRequester.kt)中的
sendMessage函数.

建立websocket连接之后,服务端会不时的发送
```json
{"type":6}\u001e
```
此时需要回复该json来保活

之后会通过该websocket接收到服务器的响应,当相应的type为2时,对话结束,同时会接收到
```json
{"type":3,"invocationId":"0"}\u001e
```
此时应关闭websocket链接
响应的内容直接去F12看一眼


# 以下还没有尝试过
获取会话的历史

`/sydney/GetConversation`

参数是conversationId=*和source=cib ,请求方式是get, 相应格式存的是个数组,格式基本和正常聊天里的那个数组相同

获取所有保存的会话

`/turing/conversation/chats`

请求方式为get,不需要额外带参数

响应格式
```json
{
    "chats": [
        {
            "conversationId": "",
            "chatName": "",
            "conversationSignature": "",
            "tone": "Creative",
            "createTimeUtc": 1,
            "updateTimeUtc": 1
        }
    ],
    "result": {
        "value": "Success",
        "message": "1 chats found.",
        "serviceVersion": "20230518.47"
    },
    "clientId": ""
}
```

删除最近的聊天

`sydney.bing.com/sydney/DeleteSingleConversation`

POST

参数

```json
{
  "conversationId": "",
  "conversationSignature": "",
  "participant": {
    "id": ""
  },
  "source": "cib",
  "optionsSets": [
    "autosave"
  ]
}
```

响应

```json
{
  "result": {
    "value": "Success",
    "message": "Conversation * and its chat data deleted.",
    "serviceVersion": "20230518.108"
  }
}
```