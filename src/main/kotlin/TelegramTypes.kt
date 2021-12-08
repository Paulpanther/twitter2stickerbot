import com.google.gson.annotations.SerializedName
import java.io.InputStream
import javax.management.monitor.StringMonitor

data class TResponse<T>(
    val ok: Boolean,
    val result: T)

data class TUser(
    val id: Long,
    val isBot: Boolean,
    val firstName: String,
    val username: String,
    val canJoinGroups: Boolean,
    val canReadAllGroupMessages: Boolean,
    val supportsInlineQueries: Boolean)

data class TChat(val id: Long)

enum class TMessageEntityType {
    @SerializedName("bot_command")
    BotCommand,
}

data class TMessageEntity(
    val type: TMessageEntityType,
    val offset: Int,
    val length: Int)

data class TPhoto(val fileId: String)

data class TMessage(
    val messageId: Long,
    val from: TUser?,
    val chat: TChat,
    val date: Int,
    val text: String?,
    val entities: List<TMessageEntity>?,
    val photo: List<TPhoto>?,
    val sticker: TSticker?)

data class TUpdate(
    val updateId: Long,
    val message: TMessage?)

data class TUpdatesParams(
    val offset: Long? = null,
    val limit: Long? = null,
    val timeout: Long? = null,
    val allowedUpdates: List<String>? = null)

data class TCreateNewStickerSetParams(
    val userId: Long,
    val name: String,
    val title: String,
    val pngSticker: InputStream,
    val emojis: String)

data class TAddStickerToSetParams(
    val userId: Long,
    val name: String,
    val pngSticker: InputStream,
    val emojis: String)

data class TBotCommand(
    val command: String,
    val description: String)

data class TSetCommandsParams(val commands: List<TBotCommand>)

data class TSendMessageParams(
    val chatId: Long,
    val text: String)

data class TGetStickerSetParams(val name: String)

data class TSticker(val fileId: String, val setName: String?)

data class TStickerSet(
    val name: String,
    val title: String,
    val stickers: List<TSticker>)

data class TSendStickerParams(
    val chatId: Long,
    val sticker: String)
