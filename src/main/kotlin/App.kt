private fun sendError(chatId: Long, error: String) {
    Api.sendMessage(TSendMessageParams(chatId, error))
}

private fun onCreateSticker(msg: TMessage) {
    val chatId = msg.chat.id
    val userId = msg.from?.id ?: return sendError(chatId, "Error: Could not get user_id")
    val data = UserDataStore[userId]
    data.currentStickerSetTitle = null
    data.currentStickerSetName = null
    data.currentImage = null
    Api.sendMessage(TSendMessageParams(chatId, "Please send me the title of the new Sticker-Set"))
}

private fun onTextMessage(msg: TMessage) {
    val chatId = msg.chat.id
    val userId = msg.from?.id ?: return sendError(chatId, "Error: Could not get user_id")
    val data = UserDataStore[userId]

    if (data.currentStickerSetTitle == null) {
        val text = msg.text ?: return sendError(chatId, "Please send me text")
        data.currentStickerSetTitle = text
        Api.sendMessage(TSendMessageParams(chatId, "Cool title. Now send me the name of the Sticker Set"))
    } else if (data.currentStickerSetName == null) {
        val name = msg.text ?: return sendError(chatId, "Please send me text")
        data.currentStickerSetName = "${name}_by_${Api.botname}"
        Api.sendMessage(TSendMessageParams(chatId, "Noice. Lets create the first Sticker! Send me a twitter link"))
    } else if (data.currentImage == null) {
        val link = msg.text ?: return sendError(chatId, "Please send me text")
        val image = TwitterApi.imageFromLink(link) ?: return sendError(chatId, "Could not create Image from Link")

        Api.sendMessage(TSendMessageParams(chatId, "Awesome. Here's a preview of the generated Image"))
        val photoMsg = Api.sendPhoto(chatId, image.toInputStream())
        val photoId = photoMsg?.photo?.firstOrNull()?.fileId ?: return sendError(chatId, "Could not get Photo Id")
        data.currentImage = image
        Api.sendMessage(TSendMessageParams(chatId, "Please send me the emoji for this Sticker next"))
    } else if (data.currentStickerEmoji == null) {
        val emoji = msg.text ?: return sendError(chatId, "Please send me text")
        data.currentStickerEmoji = emoji
        val result = Api.createNewStickerSet(TCreateNewStickerSetParams(userId, data.currentStickerSetName!!, data.currentStickerSetTitle!!, data.currentImage!!.toInputStream(), emoji))
        if (result == null || !result) return sendError(chatId, "Could not create Sticker Set")
        Api.sendMessage(TSendMessageParams(chatId, "Successfully created the Sticker Set"))

        val stickerSet = Api.getStickerSet(TGetStickerSetParams(data.currentStickerSetName!!)) ?: return sendError(chatId, "Could not get StickerSet")
        val sticker = stickerSet.stickers.firstOrNull() ?: return sendError(chatId, "StickerSet is empty")
        Api.sendSticker(TSendStickerParams(chatId, sticker.fileId))
    }
}

fun main() {
    println("Starting Telegram Bot")
    MessageHandler {
        onCommand("create_sticker_set", "Creates a new Sticker Set", ::onCreateSticker)
        onTextMessage(::onTextMessage)
    }
}
