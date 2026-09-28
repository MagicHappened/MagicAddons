package org.magic.magicaddons.data.greenhouse.transfer

import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.Deflater
import java.util.zip.Inflater

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
