package org.magic.magicaddons.ui.background

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.renderer.RenderPipelines
import org.magic.magicaddons.Common
import org.magic.magicaddons.data.config.ActionSetting
import org.magic.magicaddons.data.handlers.ModFiles
import org.magic.magicaddons.features.customization.Customization
import org.magic.magicaddons.util.ChatUtils
import org.lwjgl.util.tinyfd.TinyFileDialogs
import net.minecraft.util.Util
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import kotlin.io.path.createDirectories

object ScreenBackground {

    private const val MAX_DOWNLOAD_BYTES: Int = 32 * 1024 * 1024

    private const val LINK_RECHECK_MS: Long = 2 * 60 * 1000

    private const val LINK_TIMEOUT_MS: Int = 10 * 1000

    private val ALLOWED_LINK_SCHEMES: Set<String> = setOf("http", "https")

    private val PICTURE_EXTENSIONS: Set<String> = setOf("png", "jpg", "jpeg", "gif")

    private val picturesFolder: File get() = ModFiles.modDir.resolve("backgrounds").toFile()

    private var loadedPicture: BackgroundImage? = null

    private var loadedFileName: String? = null

    private var loadedUrl: String? = null
    private var loadedLinkBytes: ByteArray? = null
    private var linkCheckedAtMs: Long = 0
    private var isFetchingLink: Boolean = false

    fun drawBackground(graphics: GuiGraphicsExtractor, left: Int, top: Int, right: Int, bottom: Int) {
        val picture = pictureToShow() ?: return
        picture.advanceAnimation()

        val width = right - left
        val height = bottom - top
        if (width <= 0 || height <= 0) return

        when (Customization.backgroundFit) {
            BackgroundFit.Cover -> drawCovering(graphics, picture, left, top, width, height)
            BackgroundFit.Contain -> drawContained(graphics, picture, left, top, width, height)
            BackgroundFit.Stretch -> drawStretched(graphics, picture, left, top, width, height)
            BackgroundFit.Tile -> drawTiled(graphics, picture, left, top, right, bottom)
        }

        val dim = Customization.backgroundDim
        if (dim != 0) graphics.fill(left, top, right, bottom, dim)
    }

    fun deletePictureFile(name: String) {
        if (name.isBlank()) {
            ChatUtils.sendWithPrefix("Pick a picture first.")
            return
        }

        if (name == loadedFileName) {
            ChatUtils.sendWithPrefix("$name is the background being shown; pick another one first.")
            return
        }

        val file = File(picturesFolder, name)
        if (!file.isFile) return

        if (file.delete()) {
            ChatUtils.sendWithPrefix("Deleted $name.")
        } else {
            ChatUtils.sendWithPrefix("Could not delete $name.")
        }
    }

    fun openPictureFolder() {
        runCatching {
            picturesFolder.toPath().createDirectories()
            Util.getPlatform().openPath(picturesFolder.toPath())
        }.onFailure {
            Common.LOGGER.warn("Could not open the backgrounds folder", it)
            ChatUtils.sendWithPrefix("Could not open that folder.")
        }
    }

    private fun drawCovering(
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

    private fun drawContained(
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

        drawStretched(
            graphics,
            picture,
            left + (width - drawnWidth) / 2,
            top + (height - drawnHeight) / 2,
            drawnWidth,
            drawnHeight
        )
    }

    private fun drawStretched(
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

    private fun pictureToShow(): BackgroundImage? = when (Customization.backgroundSource) {
        BackgroundSource.None -> null
        BackgroundSource.LocalFile -> pictureFromSavedFile()
        BackgroundSource.Url -> pictureFromLink()
    }

    private fun pictureFromSavedFile(): BackgroundImage? {
        val wantedFileName = Customization.backgroundFile.takeIf { it.isNotBlank() }

        if (wantedFileName == loadedFileName) return loadedPicture

        forgetLoadedPicture()
        loadedFileName = wantedFileName

        val file = wantedFileName?.let { File(picturesFolder, it) } ?: return null
        if (!file.isFile) return null

        return showLoadedPicture(BackgroundImage.load(file), file.name)
    }

    private fun pictureFromLink(): BackgroundImage? {
        val url = Customization.backgroundUrl.takeIf { it.isNotBlank() } ?: run {
            forgetLoadedPicture()
            return null
        }

        val isNewLink = url != loadedUrl
        val isRecheckDue = System.currentTimeMillis() - linkCheckedAtMs > LINK_RECHECK_MS

        if (isNewLink || isRecheckDue) fetchLinkPicture(url, isNewLink)

        return loadedPicture
    }

    private fun fetchLinkPicture(url: String, isNewLink: Boolean) {
        if (isFetchingLink) return

        isFetchingLink = true
        linkCheckedAtMs = System.currentTimeMillis()

        Thread({
            val bytes = runCatching { downloadPictureBytes(url) }
                .onFailure { Common.LOGGER.warn("Could not read the background link $url", it) }
                .getOrNull()

            Minecraft.getInstance().execute {
                isFetchingLink = false

                if (bytes == null) {
                    if (isNewLink) {
                        forgetLoadedPicture()
                        loadedUrl = url
                        ChatUtils.sendWithPrefix("That link did not give a png, jpg or gif.")
                    }
                    return@execute
                }

                if (!isNewLink && bytes.contentEquals(loadedLinkBytes)) return@execute

                forgetLoadedPicture()
                loadedLinkBytes = bytes
                loadedUrl = url

                showLoadedPicture(BackgroundImage.load(bytes, isGifBytes(bytes)), "the link")
            }
        }, "MagicAddons background link").start()
    }

    private fun downloadPictureBytes(url: String): ByteArray? {
        val address = URI(url)
        if (address.scheme?.lowercase() !in ALLOWED_LINK_SCHEMES) return null

        val connection = address.toURL().openConnection() as? HttpURLConnection ?: return null

        connection.apply {
            connectTimeout = LINK_TIMEOUT_MS
            readTimeout = LINK_TIMEOUT_MS
            instanceFollowRedirects = true
            setRequestProperty("Accept", "image/*")
        }

        try {
            if (connection.url.protocol?.lowercase() !in ALLOWED_LINK_SCHEMES) return null
            if (connection.responseCode !in 200..299) return null

            val contentType = connection.contentType?.substringBefore(';')?.trim()?.lowercase()
            if (contentType != null && !contentType.startsWith("image/")) return null

            if (connection.contentLengthLong > MAX_DOWNLOAD_BYTES) return null

            val bytes = connection.inputStream.use { it.readNBytes(MAX_DOWNLOAD_BYTES) }

            return bytes.takeIf { hasPictureSignature(it) }
        } finally {
            connection.disconnect()
        }
    }

    private fun hasPictureSignature(bytes: ByteArray): Boolean {
        if (bytes.size < 4) return false

        fun byteAt(index: Int): Int = bytes[index].toInt() and 0xFF

        val startsAsPng = byteAt(0) == 0x89 && byteAt(1) == 0x50 && byteAt(2) == 0x4E && byteAt(3) == 0x47
        val startsAsJpg = byteAt(0) == 0xFF && byteAt(1) == 0xD8 && byteAt(2) == 0xFF

        return startsAsPng || startsAsJpg || isGifBytes(bytes)
    }

    private fun showLoadedPicture(loaded: BackgroundImage.Loaded?, name: String): BackgroundImage? {
        if (loaded == null) {
            ChatUtils.sendWithPrefix("Could not read $name as a picture.")
            return null
        }

        loaded.notes.forEach { ChatUtils.sendWithPrefix("Background $name: ${it}.") }
        loadedPicture = loaded.image

        return loadedPicture
    }

    fun choosePictureFile(setting: ActionSetting) {
        Thread({
            val picked = TinyFileDialogs.tinyfd_openFileDialog(
                "Choose a background image",
                "",
                null,
                "png, jpg, gif",
                false
            ) ?: return@Thread

            Minecraft.getInstance().execute { copyPictureIntoFolder(File(picked), setting) }
        }, "MagicAddons background picker").start()
    }

    fun saveLinkPicture() {
        val bytes = loadedLinkBytes ?: run {
            ChatUtils.sendWithPrefix("Nothing has been read from that link yet.")
            return
        }

        val fileName = fileNameForLink(Customization.backgroundUrl, bytes)

        runCatching {
            ModFiles.writeBytesAtomically(picturesFolder.toPath().resolve(fileName), bytes)
            ChatUtils.sendWithPrefix("Saved it as $fileName.")
        }.onFailure {
            Common.LOGGER.warn("Could not save the picture from the link", it)
            ChatUtils.sendWithPrefix("Could not save that picture.")
        }
    }

    private fun copyPictureIntoFolder(file: File, setting: ActionSetting) {
        runCatching {
            ModFiles.copyAtomically(file.toPath(), picturesFolder.toPath().resolve(file.name))
            setting.value = file.name
            Customization.useSavedPicture(file.name)
            forgetLoadedPicture()
        }.onFailure {
            Common.LOGGER.warn("Could not copy the background ${file.name}", it)
            ChatUtils.sendWithPrefix("Could not copy that file.")
        }
    }

    fun savedPictureFiles(): List<String> = picturesFolder.listFiles()
        ?.filter { it.isFile && it.extension.lowercase() in PICTURE_EXTENSIONS }
        ?.map { it.name }
        ?.sorted()
        .orEmpty()

    fun forgetLoadedPicture() {
        loadedPicture?.close()
        loadedPicture = null
        loadedFileName = null
        loadedUrl = null
    }

    private fun isGifBytes(bytes: ByteArray): Boolean =
        bytes.size > 3 && bytes[0] == 'G'.code.toByte() && bytes[1] == 'I'.code.toByte() &&
                bytes[2] == 'F'.code.toByte()

    private fun fileNameForLink(url: String, bytes: ByteArray): String {
        val fileEnding = if (isGifBytes(bytes)) {
            "gif"
        } else {
            url.substringAfterLast('.', "").substringBefore('?').lowercase()
                .takeIf { it in PICTURE_EXTENSIONS } ?: "png"
        }

        return "link-${url.hashCode().toUInt()}.$fileEnding"
    }
}
