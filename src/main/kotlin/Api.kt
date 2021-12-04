import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.*
import com.github.kittinunf.result.Result
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.InputStream
import java.io.Reader

object Api {
    private val token = System.getenv("TELEGRAM_TOKEN") ?: error("TELEGRAM_TOKEN not set")
    private val base = "https://api.telegram.org/bot$token/"
    const val botname = "twitter2stickerbot"

    private val gson = GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create()

    fun getMe(): TUser? = sendRequest("getMe")

    fun getUpdates(params: TUpdatesParams): List<TUpdate>? = sendRequest("getUpdates", params)

    fun addStickerToSet(params: TAddStickerToSetParams): Boolean? = sendRequest("addStickerToSet", params)

    fun setMyCommands(params: TSetCommandsParams): Boolean? = sendRequest("setMyCommands", params)

    fun sendMessage(params: TSendMessageParams): TMessage? = sendRequest("sendMessage", params)

    fun getStickerSet(params: TGetStickerSetParams): TStickerSet? = sendRequest("getStickerSet", params)

    fun sendSticker(params: TSendStickerParams): TMessage? = sendRequest("sendSticker", params)

    fun createNewStickerSet(params: TCreateNewStickerSetParams): Boolean? {
        val request = Fuel.upload(base + "createNewStickerSet", Method.POST, listOf(
            "user_id" to params.userId,
            "name" to params.name,
            "title" to params.title,
            "emojis" to params.emojis))
            .add(BlobDataPart(params.pngSticker, "png_sticker", "image.png", contentType = "image/png"))

        val (_, result) = getResponse<Boolean>(request)
        if (result is Result.Failure) {
            println(result.error)
            return null
        }
        val response = result.get()
        if (!response.ok) return null
        return response.result
    }

    fun sendPhoto(chatId: Long, imageStream: InputStream): TMessage? {
        val request = Fuel.upload(base + "sendPhoto", Method.POST, listOf("chat_id" to chatId))
            .add(BlobDataPart(imageStream, "photo", "image.png", contentType = "image/png"))

        val (_, result) = getResponse<TMessage>(request)
        if (result is Result.Failure) {
            println(result.error)
            return null
        }
        val response = result.get()
        if (!response.ok) return null
        return response.result
    }

    private inline fun <reified ResultType: Any> sendRequest(route: String, params: Any? = null, image: BlobDataPart? = null): ResultType? {
        val serializedParams = gson.toJson(params)
        var request = Fuel.post(base + route)
        if (params != null) {
            request = request
                .header("Content-Type", "application/json")
                .body(serializedParams)
        }

        val (res, result) = getResponse<ResultType>(request)
        if (result is Result.Failure) {
            println(res.body().asString("text/plain"))
            println(result.error)
            return null
        }
        val response = result.get()
        if (!response.ok) return null
        return response.result
    }

    /** Fuel does implement this in Fuel.gson, but for some reason it does not work there for me */
    private inline fun <reified T: Any> getResponse(request: Request): Pair<Response, Result<TResponse<T>, FuelError>> {
        val (ra, re, result) = request.response(object: ResponseDeserializable<TResponse<T>> {
            override fun deserialize(reader: Reader) = gson.fromJson<TResponse<T>>(reader, object: TypeToken<TResponse<T>>() {}.type)
        })
        return re to result
    }
}

