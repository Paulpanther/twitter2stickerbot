import java.awt.image.BufferedImage

enum class CreationState{
    Sticker,
    Set
}

data class UserData(
    var creationState: CreationState? = null,
    var currentStickerSetName: String? = null,
    var currentStickerSetTitle: String? = null,
    var currentStickerImage: BufferedImage? = null,
    var currentStickerEmoji: String? = null
)

object UserDataStore {
    private val userData = mutableMapOf<Long, UserData>()

    operator fun get(userId: Long) = userData.getOrPut(userId, ::UserData)
}
