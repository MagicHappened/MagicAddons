package org.magic.magicaddons.ui.background

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.renderer.RenderPipelines
import org.magic.magicaddons.Common
import org.magic.magicaddons.data.config.ActionSetting
import org.magic.magicaddons.data.handlers.DataHandler
import org.magic.magicaddons.features.customization.Customization
import org.magic.magicaddons.util.ChatUtils
import org.lwjgl.util.tinyfd.TinyFileDialogs
import net.minecraft.util.Util
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import kotlin.io.path.createDirectories

/** The picture behind the config screen: picking one, following a link, and drawing whichever is set. */
object ConfigBackground {

    /** How much of a link the mod will download before giving up on it. */
    private const val MAX_DOWNLOAD_BYTES: Int = 32 * 1024 * 1024

    /** How often a link is asked again while the config screen is open, for one that changes. */
    private const val RECHECK_MS: Long = 2 * 60 * 1000

    /** How long a link is given to answer before it is given up on. */
    private const val TIMEOUT_MS: Int = 10 * 1000

    /** A link is followed only over the web, never to a file on this computer. */
    private val ALLOWED_SCHEMES: Set<String> = setOf("http", "https")

    private val KNOWN_ENDINGS: Set<String> = setOf("png", "jpg", "jpeg", "gif")

    private val folder: File get() = DataHandler.modDir.resolve("backgrounds").toFile()

    private var image: BackgroundImage? = null

    /** The file the loaded picture came from, so it is only read again when the choice changes. */
    private var loadedFrom: String? = null

    /** The link the loaded picture came from, and the bytes it arrived as. */
    private var loadedUrl: String? = null
    private var linkBytes: ByteArray? = null
    private var checkedUrlAt: Long = 0
    private var fetching: Boolean = false

    /** Draws the background inside a rectangle, cropped to fill it rather than squashed. */
    fun draw(graphics: GuiGraphicsExtractor, left: Int, top: Int, right: Int, bottom: Int) {
        val picture = currentPicture() ?: return
        picture.advance()

        val width = right - left
        val height = bottom - top
        if (width <= 0 || height <= 0) return

        when (Customization.backgroundFit) {
            BackgroundFit.Cover -> drawCover(graphics, picture, left, top, width, height)
            BackgroundFit.Contain -> drawContain(graphics, picture, left, top, width, height)
            BackgroundFit.Stretch -> drawWhole(graphics, picture, left, top, width, height)
            BackgroundFit.Tile -> drawTiled(graphics, picture, left, top, right, bottom)
        }

        // over the picture and under everything else, so writing keeps its own colour
        val dim = Customization.backgroundDim
        if (dim != 0) graphics.fill(left, top, right, bottom, dim)
    }

    /** Deletes a picture from the mod's folder, keeping the one being drawn. */
    fun deletePictureFile(name: String) {
        if (name.isBlank()) {
            ChatUtils.sendWithPrefix("Pick a picture first.")
            return
        }

        if (name == loadedFrom) {
            ChatUtils.sendWithPrefix("$name is the background being shown; pick another one first.")
            return
        }

        val file = File(folder, name)
        if (!file.isFile) return

        if (file.delete()) {
            ChatUtils.sendWithPrefix("Deleted $name.")
        } else {
            ChatUtils.sendWithPrefix("Could not delete $name.")
        }
    }

    /** Opens the folder the pictures are kept in, in whatever this computer shows folders with. */
    fun openPictureFolder() {
        runCatching {
            folder.toPath().createDirectories()
            Util.getPlatform().openPath(folder.toPath())
        }.onFailure {
            Common.LOGGER.warn("Could not open the backgrounds folder", it)
            ChatUtils.sendWithPrefix("Could not open that folder.")
        }
    }

    /** The part of the picture shaped like the space, taken from its middle and filling it. */
    private fun drawCover(
        graphics: GuiGraphicsExtractor,
        picture: BackgroundImage,
        left: Int,
        top: Int,
        width: Int,
        height: Int
    ) {
        val fillScale = maxOf(width.toFloat() / picture.width, height.toFloat() / picture.height)
        val sourceWidth = (width / fillScale).toInt().coerceIn(1, picture.width)
        val sourceHeight = (height / fillScale).toInt().coerceIn(1, picture.height)

        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            picture.id,
            left, top,
            (picture.width - sourceWidth) / 2f,
            (picture.height - sourceHeight) / 2f,
            width, height,
            sourceWidth, sourceHeight,
            picture.width, picture.height
        )
    }

    /** The whole picture, as large as it goes without changing shape, centred in the space. */
    private fun drawContain(
        graphics: GuiGraphicsExtractor,
        picture: BackgroundImage,
        left: Int,
        top: Int,
        width: Int,
        height: Int
    ) {
        val fitScale = minOf(width.toFloat() / picture.width, height.toFloat() / picture.height)
        val drawnWidth = (picture.width * fitScale).toInt().coerceAtLeast(1)
        val drawnHeight = (picture.height * fitScale).toInt().coerceAtLeast(1)

        drawWhole(
            graphics,
            picture,
            left + (width - drawnWidth) / 2,
            top + (height - drawnHeight) / 2,
            drawnWidth,
            drawnHeight
        )
    }

    /** The whole picture pulled to the size given, which changes its shape unless they match. */
    private fun drawWhole(
        graphics: GuiGraphicsExtractor,
        picture: BackgroundImage,
        left: Int,
        top: Int,
        width: Int,
        height: Int
    ) {
        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            picture.id,
            left, top,
            0f, 0f,
            width, height,
            picture.width, picture.height,
            picture.width, picture.height
        )
    }

    /** The picture at its own size, laid out across the space until it is covered. */
    private fun drawTiled(
        graphics: GuiGraphicsExtractor,
        picture: BackgroundImage,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int
    ) {
        var y = top
        while (y < bottom) {
            val tileHeight = minOf(picture.height, bottom - y)

            var x = left
            while (x < right) {
                val tileWidth = minOf(picture.width, right - x)

                graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    picture.id,
                    x, y,
                    0f, 0f,
                    tileWidth, tileHeight,
                    tileWidth, tileHeight,
                    picture.width, picture.height
                )
                x += picture.width
            }
            y += picture.height
        }
    }

    /** The picture the settings ask for: a saved file, or whatever the link last gave. */
    private fun currentPicture(): BackgroundImage? = when (Customization.backgroundSource) {
        BackgroundSource.None -> null
        BackgroundSource.LocalFile -> savedPicture()
        BackgroundSource.Url -> linkPicture()
    }

    private fun savedPicture(): BackgroundImage? {
        val wanted = Customization.backgroundFile.takeIf { it.isNotBlank() }

        if (wanted == loadedFrom) return image

        forgetLoadedPicture()
        loadedFrom = wanted

        val file = wanted?.let { File(folder, it) } ?: return null
        if (!file.isFile) return null

        return showPicture(BackgroundImage.load(file), file.name)
    }

    /**
     * The picture a link gives. It is never saved: the link is asked again when it changes and every
     * couple of minutes the screen stays open, so a link that serves a different picture follows.
     */
    private fun linkPicture(): BackgroundImage? {
        val url = Customization.backgroundUrl.takeIf { it.isNotBlank() } ?: run {
            forgetLoadedPicture()
            return null
        }

        val urlChanged = url != loadedUrl
        val recheckDue = System.currentTimeMillis() - checkedUrlAt > RECHECK_MS

        if (urlChanged || recheckDue) fetchLink(url, announce = urlChanged)

        return image
    }

    /** Asks the link for its bytes, and rebuilds the picture only when they came back different. */
    private fun fetchLink(url: String, announce: Boolean) {
        if (fetching) return

        fetching = true
        checkedUrlAt = System.currentTimeMillis()

        Thread({
            val bytes = runCatching { readPicture(url) }
                .onFailure { Common.LOGGER.warn("Could not read the background link $url", it) }
                .getOrNull()

            if (bytes == null && announce) {
                Minecraft.getInstance().execute {
                    ChatUtils.sendWithPrefix("That link did not give a png, jpg or gif.")
                }
            }

            Minecraft.getInstance().execute {
                fetching = false
                if (bytes == null) return@execute

                // the same bytes as last time means the same picture, so the one on screen stays
                if (!announce && bytes.contentEquals(linkBytes)) return@execute

                forgetLoadedPicture()
                linkBytes = bytes
                loadedUrl = url

                showPicture(BackgroundImage.load(bytes, gifBytes(bytes)), "the link")
            }
        }, "MagicAddons background link").start()
    }

    /**
     * Reads a picture from a link, refusing anything that is not plainly one. Only http and https
     * are followed, so a link cannot be pointed at a file on this computer, the reply has to call
     * itself an image, it is never read past [MAX_DOWNLOAD_BYTES], and the bytes themselves have to
     * begin as a png, jpg or gif whatever the link or the server claims.
     */
    private fun readPicture(url: String): ByteArray? {
        val address = URI(url)
        if (address.scheme?.lowercase() !in ALLOWED_SCHEMES) return null

        val connection = address.toURL().openConnection() as? HttpURLConnection ?: return null

        connection.apply {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            instanceFollowRedirects = true
            setRequestProperty("Accept", "image/*")
        }

        try {
            // a redirect may leave http or https, and java stops following when it does
            if (connection.url.protocol?.lowercase() !in ALLOWED_SCHEMES) return null
            if (connection.responseCode !in 200..299) return null

            val contentType = connection.contentType?.substringBefore(';')?.trim()?.lowercase()
            if (contentType != null && !contentType.startsWith("image/")) return null

            if (connection.contentLengthLong > MAX_DOWNLOAD_BYTES) return null

            val bytes = connection.inputStream.use { it.readNBytes(MAX_DOWNLOAD_BYTES) }

            return bytes.takeIf { looksLikePicture(it) }
        } finally {
            connection.disconnect()
        }
    }

    /** Whether the bytes begin the way a png, jpg or gif does. */
    private fun looksLikePicture(bytes: ByteArray): Boolean {
        if (bytes.size < 4) return false

        fun byteAt(index: Int): Int = bytes[index].toInt() and 0xFF

        val startsAsPng = byteAt(0) == 0x89 && byteAt(1) == 0x50 && byteAt(2) == 0x4E && byteAt(3) == 0x47
        val startsAsJpg = byteAt(0) == 0xFF && byteAt(1) == 0xD8 && byteAt(2) == 0xFF

        return startsAsPng || startsAsJpg || gifBytes(bytes)
    }

    /** Puts a freshly read picture on screen, saying what had to be done to fit it. */
    private fun showPicture(loaded: BackgroundImage.Loaded?, name: String): BackgroundImage? {
        if (loaded == null) {
            ChatUtils.sendWithPrefix("Could not read $name as a picture.")
            return null
        }

        loaded.notes.forEach { ChatUtils.sendWithPrefix("Background $name: ${it}.") }
        image = loaded.image

        return image
    }

    /** Opens the system's own file dialog and takes a copy of whatever is picked. */
    fun choosePictureFile(setting: ActionSetting) {
        Thread({
            val picked = TinyFileDialogs.tinyfd_openFileDialog(
                "Choose a background image",
                "",
                null,
                "png, jpg, gif",
                false
            ) ?: return@Thread

            Minecraft.getInstance().execute { savePictureFile(File(picked), setting) }
        }, "MagicAddons background picker").start()
    }

    /** Writes what the link last gave into the mod's folder, so it joins the saved pictures. */
    fun saveLinkPicture() {
        val bytes = linkBytes ?: run {
            ChatUtils.sendWithPrefix("Nothing has been read from that link yet.")
            return
        }

        val fileName = fileNameFor(Customization.backgroundUrl, bytes)

        runCatching {
            folder.toPath().createDirectories()
            File(folder, fileName).writeBytes(bytes)
            ChatUtils.sendWithPrefix("Saved it as $fileName.")
        }.onFailure {
            Common.LOGGER.warn("Could not save the picture from the link", it)
            ChatUtils.sendWithPrefix("Could not save that picture.")
        }
    }

    /** Copies a chosen file into the mod's own folder, so moving the original does not break it. */
    private fun savePictureFile(file: File, setting: ActionSetting) {
        runCatching {
            folder.toPath().createDirectories()
            file.copyTo(File(folder, file.name), overwrite = true)
            setting.value = file.name
            Customization.useSavedPicture(file.name)
            forgetLoadedPicture()
        }.onFailure {
            Common.LOGGER.warn("Could not copy the background ${file.name}", it)
            ChatUtils.sendWithPrefix("Could not copy that file.")
        }
    }

    /** Every picture in the mod's folder, which is everything ever picked or saved from a link. */
    fun savedPictureFiles(): List<String> = folder.listFiles()
        ?.filter { it.isFile && it.extension.lowercase() in KNOWN_ENDINGS }
        ?.map { it.name }
        ?.sorted()
        .orEmpty()

    /** The picture is read again the next time the screen draws. */
    fun forgetLoadedPicture() {
        image?.close()
        image = null
        loadedFrom = null
        loadedUrl = null
    }

    /** Gif files open with GIF87a or GIF89a, which is how downloaded bytes are told apart. */
    private fun gifBytes(bytes: ByteArray): Boolean =
        bytes.size > 3 && bytes[0] == 'G'.code.toByte() && bytes[1] == 'I'.code.toByte() &&
                bytes[2] == 'F'.code.toByte()

    /** A file name for a link, keeping the ending its bytes say it has. */
    private fun fileNameFor(url: String, bytes: ByteArray): String {
        val fileEnding = if (gifBytes(bytes)) {
            "gif"
        } else {
            url.substringAfterLast('.', "").substringBefore('?').lowercase()
                .takeIf { it in KNOWN_ENDINGS } ?: "png"
        }

        return "link-${url.hashCode().toUInt()}.$fileEnding"
    }
}
