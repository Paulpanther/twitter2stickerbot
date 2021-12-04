import java.awt.Font
import java.awt.Graphics2D
import java.awt.font.FontRenderContext
import java.awt.geom.AffineTransform

private data class WrappedLine(val text: String, val y: Float)

class WrappedText(font: Font, text: String, width: Int) {
    private val wrappedLines: List<WrappedLine>
    val height: Int

    init {
        val ctx = FontRenderContext(AffineTransform(), true, true)

        val words = text.split(" ")

        val lines = mutableListOf(mutableListOf<String>())
        for (word in words) {
            val currentLine = lines.last()
            val lineWithWordLength = font.getStringBounds(currentLine.joinToString(" ") + " $word", ctx).width
            if (lineWithWordLength > width) {
                if (currentLine.isEmpty()) error("Word is longer than line")
                lines += mutableListOf(word)
            } else {
                currentLine += word
            }
        }

        var currentHeight = 0f
        wrappedLines = (0 until lines.size).map { i ->
            val line = WrappedLine(lines[i].joinToString(" "), currentHeight)
            currentHeight += font.getStringBounds(line.text, ctx).height.toFloat()
            line
        }
        height = currentHeight.toInt()
    }

    fun draw(g: Graphics2D, x: Int, y: Int) {
        for (line in wrappedLines) {
            g.drawString(line.text, x, (y + line.y).toInt())
        }
    }
}

fun Graphics2D.draw(text: WrappedText, x: Int, y: Int) {
    text.draw(this, x, y)
}
