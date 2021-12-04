import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.*
import com.github.kittinunf.result.Result
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.awt.*
import java.awt.geom.AffineTransform
import java.awt.geom.RoundRectangle2D
import java.awt.image.AffineTransformOp
import java.awt.image.BufferedImage
import java.awt.image.BufferedImageOp
import java.io.*
import javax.imageio.ImageIO

object TwitterApi {
    private val bearerToken = System.getenv("TWITTER_BEARER_TOKEN") ?: error("TWITTER_BEARER_TOKEN not set")
    private const val base = "https://api.twitter.com/2/"

    private val gson = GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create()

    fun imageFromLink(link: String): BufferedImage? {
        val tweetId = link.split("/").lastOrNull() ?: return null
        val tweet = sendRequest<Tweet>("tweets/$tweetId", listOf(
            "expansions" to "author_id",
            "user.fields" to "profile_image_url,verified"
        )) ?: return null

        val user = tweet.includes?.users?.firstOrNull()
        val profileImageUrl = user?.profileImageUrl ?: return null
        val text = tweet.data?.text ?: return null
        val name = user.name ?: return null
        val username = user.username ?: return null

        return TweetImageGenerator.generate(name, username, text, profileImageUrl)
    }

    private inline fun <reified ResultType: Any> sendRequest(route: String, params: List<Pair<String, String>>): ResultType? {
        val request = Fuel.get(base + route, params).header("Authorization", "Bearer $bearerToken")

        val result = getResponse<ResultType>(request)
        if (result is Result.Failure) {
            println(result.error)
            return null
        }
        return result.get()
    }

    /** Fuel does implement this in Fuel.gson, but for some reason it does not work there for me */
    private inline fun <reified T: Any> getResponse(request: Request): Result<T, FuelError> {
        val (_, _, result) = request.response(object: ResponseDeserializable<T> {
            override fun deserialize(reader: Reader) = gson.fromJson<T>(reader, object: TypeToken<T>() {}.type)
        })
        return result
    }
}

data class TweetData(val text: String?)

data class TweetUser(
    val name: String?,
    val username: String?,
    val verified: Boolean?,
    val profileImageUrl: String?)

data class TweetUsers(val users: List<TweetUser>?)

data class Tweet(
    val data: TweetData?,
    val includes: TweetUsers?)


fun BufferedImage.toInputStream(): InputStream {
    val os = ByteArrayOutputStream()
    ImageIO.write(this, "png", os)
    return ByteArrayInputStream(os.toByteArray())
}
