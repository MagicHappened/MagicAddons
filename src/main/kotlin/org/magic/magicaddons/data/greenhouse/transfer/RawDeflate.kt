package org.magic.magicaddons.data.greenhouse.transfer

import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.Deflater
import java.util.zip.Inflater

object RawDeflate {

    private const val MAX_DECODED_BYTES: Int = 64 * 1024

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

    fun decode(text: String): ByteArray? = runCatching {
        Inflater(true).use { inflater ->
            inflater.setInput(Base64.getUrlDecoder().decode(text.trim()))

            val out = ByteArrayOutputStream()
            val buffer = ByteArray(4096)
            while (!inflater.finished()) {
                val inflatedByteCount = inflater.inflate(buffer)
                if (inflater.needsDictionary()) return null
                if (inflatedByteCount == 0 && inflater.needsInput()) break
                if (out.size() + inflatedByteCount > MAX_DECODED_BYTES) return null
                out.write(buffer, 0, inflatedByteCount)
            }

            out.toByteArray()
        }
    }.getOrNull()
}
