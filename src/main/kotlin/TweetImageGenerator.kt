import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.result.Result
import java.awt.*
import java.awt.geom.AffineTransform
import java.awt.geom.RoundRectangle2D
import java.awt.image.AffineTransformOp
import java.awt.image.BufferedImage
import java.awt.image.BufferedImageOp
import java.io.ByteArrayInputStream
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.max

object TweetImageGenerator {
    private const val width = 512
    private const val minHeight = 100
    private const val profileImageSize = 60
    private const val edgeRadius = 15
    private const val contextY = 90

    private val fontBold = loadFont("Roboto-Medium.ttf", 16) ?: error("Could not load Roboto Medium")
    private val fontRegular = loadFont("Roboto-Regular.ttf", 22) ?: error("Could not load Roboto-Regular")
    private val fontThin = loadFont("Roboto-Thin.ttf", 14) ?: error("Could not load Roboto Thin")

    fun generate(name: String, username: String, text: String, profileImageUrl: String): BufferedImage? {
        val wrappedText = WrappedText(fontRegular, text, width)
        val height = max(minHeight, wrappedText.height + contextY)

        val profileImage = downloadImage(profileImageUrl) ?: return null
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics()

        g.color = Color.white
        g.fill(RoundRectangle2D.Float(0f, 0f, width.toFloat(), height.toFloat(), edgeRadius.toFloat(), edgeRadius.toFloat()))

        g.drawImage(createRoundedImage(profileImage), createScaleOperation(profileImage, profileImageSize, profileImageSize), 5, 5)

        g.color = Color.black
        g.font = fontBold
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

        g.drawString(name, 80, 30)

        g.font = fontThin
        g.drawString("@$username", 80, 50)

        g.font = fontRegular
        g.draw(wrappedText, 10, contextY)

        g.dispose()

        return image
    }

    private fun loadFont(name: String, size: Int): Font? {
        val stream = this::class.java.getResource(name)?.openStream() ?: return null
        return Font.createFont(Font.TRUETYPE_FONT, stream).deriveFont(size.toFloat())
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

}
