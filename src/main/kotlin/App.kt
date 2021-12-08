import java.io.File
import javax.imageio.ImageIO

//
// Disclaimer: This Code was written mainly in under 3 Hours. Please don't judge me for it ;)
//

private fun sendError(chatId: Long, error: String) {
    Api.sendMessage(TSendMessageParams(chatId, error))
}

private fun onCreateStickerSet(msg: TMessage) {
    val chatId = msg.chat.id
    val userId = msg.from?.id ?: return sendError(chatId, "Error: Could not get user_id")
    val data = UserDataStore[userId]
    data.currentStickerSetName = null
    data.currentStickerSetTitle = null
    data.currentStickerImage = null
    data.currentStickerEmoji = null
    data.creationState = CreationState.Set
    Api.sendMessage(TSendMessageParams(chatId, "Please send me the title of the new Sticker Set"))
}

private fun onCreateSticker(msg: TMessage) {
    val chatId = msg.chat.id
    val userId = msg.from?.id ?: return sendError(chatId, "Error: Could not get user_id")
    val data = UserDataStore[userId]
    data.currentStickerSetName = null
    data.currentStickerImage = null
    data.currentStickerEmoji = null
    data.creationState = CreationState.Sticker
    Api.sendMessage(TSendMessageParams(chatId, "Please send me a Sticker of the Sticker-Pack you want to add a new Sticker too. (The pack has to been created by me and not another bot!)"))
}

private fun onTextMessage(msg: TMessage) {
    val chatId = msg.chat.id
    val userId = msg.from?.id ?: return sendError(chatId, "Error: Could not get user_id")
    val data = UserDataStore[userId]

    if (data.creationState == CreationState.Set) {
        if (data.currentStickerSetTitle == null) {
            val text = msg.text ?: return sendError(chatId, "Please send me text")
            data.currentStickerSetTitle = text
            Api.sendMessage(TSendMessageParams(chatId, "Cool title. Now send me the name of the Sticker Set"))
        } else if (data.currentStickerSetName == null) {
            val name = msg.text ?: return sendError(chatId, "Please send me text")
            data.currentStickerSetName = "${name}_by_${Api.botname}"
            Api.sendMessage(TSendMessageParams(chatId, "Noice. Lets create the first Sticker! Send me a twitter link"))
        } else if (data.currentStickerImage == null) {
            val link = msg.text ?: return sendError(chatId, "Please send me text")
            val image = TwitterApi.imageFromLink(link) ?: return sendError(chatId, "Could not create Image from Link")

            Api.sendMessage(TSendMessageParams(chatId, "Awesome. Here's a preview of the generated Image"))
            val photoMsg = Api.sendPhoto(chatId, image.toInputStream())
            val photoId = photoMsg?.photo?.firstOrNull()?.fileId ?: return sendError(chatId, "Could not get Photo Id")
            data.currentStickerImage = image
            Api.sendMessage(TSendMessageParams(chatId, "Please send me the emoji for this Sticker next"))
        } else if (data.currentStickerEmoji == null) {
            val emoji = msg.text ?: return sendError(chatId, "Please send me text")
            data.currentStickerEmoji = emoji
            val result = Api.createNewStickerSet(TCreateNewStickerSetParams(userId, data.currentStickerSetName!!, data.currentStickerSetTitle!!, data.currentStickerImage!!.toInputStream(), emoji))
            if (result == null || !result) return sendError(chatId, "Could not create Sticker Set")
            Api.sendMessage(TSendMessageParams(chatId, "Successfully created the Sticker Set"))

            val stickerSet = Api.getStickerSet(TGetStickerSetParams(data.currentStickerSetName!!)) ?: return sendError(chatId, "Could not get StickerSet")
            val sticker = stickerSet.stickers.firstOrNull() ?: return sendError(chatId, "StickerSet is empty")
            Api.sendSticker(TSendStickerParams(chatId, sticker.fileId))
            data.creationState = null
        }
    } else if (data.creationState == CreationState.Sticker) {
        if (data.currentStickerSetName == null) {
            val sticker = msg.sticker ?: return sendError(chatId, "You need to send me a sticker")
            data.currentStickerSetName = sticker.setName ?: return sendError(chatId, "Could not get name of Sticker-Set, try a different Sticker")
            val stickerSet = Api.getStickerSet(TGetStickerSetParams(data.currentStickerSetName!!))
            Api.sendMessage(TSendMessageParams(chatId, if (stickerSet != null) "Now send me the Tweet-Link to add to ${stickerSet.title}" else "Now send me the Tweet-Link"))
        } else if (data.currentStickerImage == null) {
            val link = msg.text ?: return sendError(chatId, "Please send me text")
            val image = TwitterApi.imageFromLink(link) ?: return sendError(chatId, "Could not create Image from Link")

            Api.sendMessage(TSendMessageParams(chatId, "Awesome. Here's a preview of the generated Image"))
            Api.sendPhoto(chatId, image.toInputStream())
            data.currentStickerImage = image
            Api.sendMessage(TSendMessageParams(chatId, "Please send me the emoji for this Sticker next"))
        } else if (data.currentStickerEmoji == null) {
            val emoji = msg.text ?: return sendError(chatId, "Please send me text")
            data.currentStickerEmoji = emoji
            val result = Api.addStickerToSet(TAddStickerToSetParams(userId, data.currentStickerSetName!!, data.currentStickerImage!!.toInputStream(), emoji))
            if (result == null || !result) return sendError(chatId, "Could not create Sticker Set")
            Api.sendMessage(TSendMessageParams(chatId, "Successfully created the Sticker"))

            val stickerSet = Api.getStickerSet(TGetStickerSetParams(data.currentStickerSetName!!)) ?: return sendError(chatId, "Could not get StickerSet")
            val sticker = stickerSet.stickers.lastOrNull() ?: return sendError(chatId, "StickerSet is empty")
            Api.sendSticker(TSendStickerParams(chatId, sticker.fileId))
            data.creationState = null
        }
    }
}

fun main() {
    println("Starting Telegram Bot")
    MessageHandler {
        onCommand("create_sticker_set", "Creates a new Sticker Set", ::onCreateStickerSet)
        onCommand("create_sticker", "Creates a new Sticker for the current Set", ::onCreateSticker)
        onTextMessage(::onTextMessage)
    }
//    val img = TwitterApi.imageFromLink("https://twitter.com/NaumannAntonius/status/1465716238496243714")
//    ImageIO.write(img, "png", File("out.png"))
}
