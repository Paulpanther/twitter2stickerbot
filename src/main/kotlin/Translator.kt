import com.google.gson.JsonElement
import com.google.gson.JsonParser

class Language(private val mapping: Map<String, String>): Map<String, String> by mapping

object Translator {
    private val languages: Map<String, Language>
    private val first: String

    init {
        val path = this::class.java.getResource("lang.json")?.openStream() ?: error("No lang.json")
        val json = JsonParser.parseReader(path.reader())
        languages = json.asMap { Language(it.asMap { v -> v.toString() }) }
        first = json.asJsonObject.keySet().first()
    }

    operator fun get(code: String, key: String): String {
        val lang = languages.getOrDefault(code, languages[first]) ?: error("Could not get Language with code $code")
        return lang.getOrDefault(key, languages[first]?.get(key)) ?: error("Could not get Translation for key $key")
    }
}

private fun <T> JsonElement.asMap(valueMapping: (JsonElement) -> T): Map<String, T> {
    val obj = asJsonObject
    return obj.keySet().associateWith { valueMapping(obj.get(it)) }
}
