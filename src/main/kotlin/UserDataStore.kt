import java.awt.image.BufferedImage

data class UserData(
    var currentStickerSetName: String? = null,
    var currentStickerSetTitle: String? = null,
    var currentImage: BufferedImage? = null,
    var currentStickerEmoji: String? = null
)

object UserDataStore {
    private val userData = mutableMapOf<Long, UserData>()

    operator fun get(userId: Long) = userData.getOrPut(userId, ::UserData)
}
