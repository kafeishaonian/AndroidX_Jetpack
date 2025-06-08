package com.aj.mvvm.utils

import android.text.InputFilter
import android.text.Spanned
import android.text.TextUtils
import org.json.JSONArray
import org.json.JSONObject
import java.io.StringReader
import java.io.StringWriter
import java.util.regex.Pattern
import javax.xml.transform.OutputKeys
import javax.xml.transform.Source
import javax.xml.transform.TransformerFactory
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource

object CharacterHandler {
    //emoji过滤器
    val EMOJI_FILTER: InputFilter = object : InputFilter {
        val emoji = Pattern.compile(
            "[\ud83c\udc00-\ud83c\udfff]|[\ud83d\udc00-\ud83d\udfff]|[\u2600-\u27ff]",
            Pattern.UNICODE_CASE or Pattern.CASE_INSENSITIVE
        )

        override fun filter(
            source: CharSequence?,
            start: Int,
            end: Int,
            dest: Spanned?,
            dstart: Int,
            dend: Int
        ): CharSequence? {
            val emojiMatcher = emoji.matcher(source)
            return if (emojiMatcher.find()) "" else null
        }
    }

    /**
     * Json格式化
     */
    @JvmStatic
    fun jsonFormat(json: String): String {
        if (TextUtils.isEmpty(json)) {
            return "Empty/Null json content"
        }
        val trimmedJson = json.trim { it <= ' ' }

        return runCatching {
            when {
                trimmedJson.startsWith("{") -> JSONObject(trimmedJson).toString(4)
                trimmedJson.startsWith("[") -> JSONArray(trimmedJson).toString(4)
                else -> trimmedJson
            }
        }.recover { throwable ->
            when (throwable) {
                is OutOfMemoryError -> "Output omitted because of Object size"
                else -> trimmedJson
            }
        }.getOrElse {
            trimmedJson
        }
    }

    /**
     * xml格式化
     */
    @JvmStatic
    fun xmlFormat(xml: String?): String? {
        if (TextUtils.isEmpty(xml)) {
            return "Empty/Null xml content"
        }

        return runCatching {
            val xmlInput: Source = StreamSource(StringReader(xml))
            val xmlOutput = StreamResult(StringWriter())
            val transformer = TransformerFactory.newInstance().newTransformer()
            transformer.setOutputProperty(OutputKeys.INDENT, "yes")
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
            transformer.transform(xmlInput, xmlOutput)
            xmlOutput.writer.toString().replaceFirst(">".toRegex(), ">\n")
        }.getOrElse {
            xml
        }
    }
}