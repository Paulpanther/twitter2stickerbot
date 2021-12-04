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
        val profileImage = downloadImage(profileImageUrl) ?: return null
        val text = tweet.data?.text ?: return null
        val name = user.name ?: return null
        val username = user.username ?: return null

        val width = 512
        val height = 200
        val profileImageSize = 60
        val edgeRadius = 10

        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics()

        g.color = Color.white
        g.fill(RoundRectangle2D.Float(0f, 0f, width.toFloat(), height.toFloat(), edgeRadius.toFloat(), edgeRadius.toFloat()))

        g.drawImage(createRoundedImage(profileImage), createScaleOperation(profileImage, profileImageSize, profileImageSize), 5, 5)

        g.color = Color.black
        g.font = g.font.deriveFont(16f).deriveFont(Font.BOLD)
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

        g.drawString(name, 80, 30)

        g.font = g.font.deriveFont(16f).deriveFont(Font.PLAIN)
        g.drawString("@$username", 80, 50)


        g.drawWrappedText(text, 10, 100, 492)

        g.dispose()

        return image
    }

    private fun createScaleOperation(image: BufferedImage, newWidth: Int, newHeight: Int): BufferedImageOp {
        val oldWidth = image.width
        val oldHeight = image.height
        val scaleX = newWidth / oldWidth.toDouble()
        val scaleY = newHeight / oldHeight.toDouble()

        return AffineTransformOp(AffineTransform().apply { scale(scaleX, scaleY) }, AffineTransformOp.TYPE_BILINEAR)
    }

    private fun createRoundedImage(image: BufferedImage): BufferedImage {
        val w = image.width
        val h = image.height
        val out = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
        val g = out.createGraphics()
        g.composite = AlphaComposite.Src
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.color = Color.white
        g.fill(RoundRectangle2D.Float(0f, 0f, w.toFloat(), h.toFloat(), w.toFloat(), h.toFloat()))
        g.composite = AlphaComposite.SrcAtop
        g.drawImage(image, 0, 0, null)
        g.dispose()
        return out
    }

    private fun downloadImage(url: String): BufferedImage? {
        val (_, _, result) = Fuel.download(url).fileDestination { response, request ->
            File.createTempFile("temp", ".tmp")
        }.response()
        if (result is Result.Failure) {
            println(result.error)
            return null
        }
        val bytes = result.get()
        return ImageIO.read(ByteArrayInputStream(bytes))
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

private fun Graphics2D.drawWrappedText(text: String, x: Int, y: Int, width: Int) {
    val words = text.split(" ")
    val lineHeight = fontMetrics.height

    val lines = mutableListOf(mutableListOf<String>())
    for (word in words) {
        val currentLine = lines.last()
        val lineWithWordLength = fontMetrics.stringWidth(currentLine.joinToString(" ") + " $word")
        if (lineWithWordLength > width) {
            if (currentLine.isEmpty()) error("Word is longer than line")
            lines += mutableListOf(word)
        } else {
            currentLine += word
        }
    }

    for (i in 0 until lines.size) {
        val actualY = i * lineHeight + y
        drawString(lines[i].joinToString(" "), x, actualY)
    }
}

fun BufferedImage.toInputStream(): InputStream {
    val os = ByteArrayOutputStream()
    ImageIO.write(this, "png", os)
    return ByteArrayInputStream(os.toByteArray())
}
