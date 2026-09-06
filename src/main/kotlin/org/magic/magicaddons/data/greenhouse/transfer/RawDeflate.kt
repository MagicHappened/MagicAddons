package org.magic.magicaddons.data.greenhouse.transfer

import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.Deflater
import java.util.zip.Inflater

/** Bytes deflated raw, with no zlib header, and written in url-safe base64 without padding. */
object RawDeflate {

    fun encode(bytes: ByteArray): String {
        val deflater = Deflater(Deflater.BEST_COMPRESSION, true)
        deflater.setInput(bytes)
        deflater.finish()

        val out = ByteArrayOutputStream()
        val buffer = ByteArray(4096)
        while (!deflater.finished()) out.write(buffer, 0, deflater.deflate(buffer))
        deflater.end()

        return Base64.getUrlEncoder().withoutPadding().encodeToString(out.toByteArray())
    }

    /** The bytes behind [text], or null when it is not base64 or not deflated data. Padding may be present or missing. */
    fun decode(text: String): ByteArray? = runCatching {
        val inflater = Inflater(true)
        inflater.setInput(Base64.getUrlDecoder().decode(text.trim()))

        val out = ByteArrayOutputStream()
        val buffer = ByteArray(4096)
        while (!inflater.finished()) {
            val count = inflater.inflate(buffer)
            if (count == 0 && inflater.needsInput()) break
            out.write(buffer, 0, count)
        }
        inflater.end()

        out.toByteArray()
    }.getOrNull()
}
