package cn.xd.newbingbot.network

object NewBingChatConfig {
    val options: List<String> = listOf(
        "deepleo",
        "enable_debug_commands",
        "disable_emoji_spoken_text",
        "enablemm"
    )
    val allowed: List<String> = listOf(
        "Chat"
    )
    val sliceIds: List<String> = emptyList()

    val newChatReceipt = listOf(
        "谢谢你帮我理清头绪! 我现在能帮你做什么?",
        "谢谢你! 知道你什么时候准备好继续前进总是很有帮助的。我现在能为你回答什么问题?",
        "重新开始总是很棒。问我任何问题!",
        "当然，我很乐意重新开始。我现在可以为你提供哪些帮助?",
        "好了，我已经为新的对话重置了我的大脑。你现在想聊些什么?",
        "没问题，很高兴你喜欢上一次对话。让我们转到一个新主题。你想要了解有关哪些内容的详细信息?",
        "当然，我已准备好进行新的挑战。我现在可以为你做什么?",
        "好的，我已清理好板子，可以重新开始了。我可以帮助你探索什么?",
        "明白了，我已经抹去了过去，专注于现在。我们现在应该探索什么?",
        "很好，让我们来更改主题。你在想什么?",
        "好了，我已经为新的对话擦拭干净板子了。现在我可以和你聊些什么呢?",
        "不用担心，我很高兴尝试一些新内容。我现在可以为你回答什么问题?"
    )
}

enum class NewBingModel(
    val value: String,
    val chinese: String
){
    Balanced("galileo", "平衡"),
    Creative("h3imaginative", "创意"),
    Precise("h3precise", "严谨")
}