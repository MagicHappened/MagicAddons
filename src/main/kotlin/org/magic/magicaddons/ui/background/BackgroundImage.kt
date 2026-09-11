package org.magic.magicaddons.ui.background

import com.mojang.blaze3d.platform.NativeImage
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.resources.Identifier
import org.magic.magicaddons.Common
import java.awt.image.IndexColorModel
import java.io.File
import javax.imageio.ImageIO
import javax.imageio.metadata.IIOMetadataNode

/**
 * A picture drawn behind a screen. A gif keeps its frames as the bytes the file stores them in and
 * paints the one that is due onto a single texture, so only one frame is ever on the graphics card.
 */
class BackgroundImage private constructor(
    val id: Identifier,
    private val texture: DynamicTexture,
    val width: Int,
    val height: Int,
    private val frames: List<Frame>,
    private val canvas: IntArray,
    private val canvasWidth: Int,
    private val canvasHeight: Int
) {

    /** One gif frame: the rectangle it covers, its own colours, and what to do with it afterwards. */
    private class Frame(
        val x: Int,
        val y: Int,
        val width: Int,
        val height: Int,
        /** A colour of [palette] per pixel. */
        val indices: ByteArray,
        val palette: IntArray,
        val transparentIndex: Int,
        val disposal: Int,
        val delayMs: Int
    )

    /** What had to be done to fit the file, so the player can be told why it looks different. */
    class Loaded(val image: BackgroundImage, val notes: List<String>)

    private var frameIndex: Int = -1
    private var nextFrameAt: Long = 0

    /** The canvas as it was before a frame that has to be undone afterwards. */
    private var savedCanvas: IntArray? = null

    val animated: Boolean get() = frames.size > 1

    /** Paints the frame that is due, when one is. Cheap to call every time the screen draws. */
    fun advance() {
        if (frames.isEmpty()) return

        val now = System.currentTimeMillis()
        if (frameIndex >= 0 && now < nextFrameAt) return
        if (frameIndex >= 0 && frames.size == 1) return

        val nextIndex = if (frameIndex < 0) 0 else (frameIndex + 1) % frames.size
        paint(frames[nextIndex], first = frameIndex < 0)

        frameIndex = nextIndex
        nextFrameAt = now + frames[nextIndex].delayMs
    }

    /** Lays a frame onto the canvas, dealing with what the frame before it asked for. */
    private fun paint(frame: Frame, first: Boolean) {
        val previousFrame = if (first) null else frames[frameIndex]

        when (previousFrame?.disposal) {
            DISPOSE_BACKGROUND -> fillRect(previousFrame, 0)
            DISPOSE_PREVIOUS -> savedCanvas?.let { it.copyInto(canvas) }
            else -> Unit
        }

        if (frame.disposal == DISPOSE_PREVIOUS) {
            savedCanvas = canvas.copyOf()
        }

        for (row in 0 until frame.height) {
            val canvasRow = frame.y + row
            if (canvasRow !in 0 until canvasHeight) continue

            for (column in 0 until frame.width) {
                val index = frame.indices[row * frame.width + column].toInt() and 0xFF
                if (index == frame.transparentIndex) continue

                val canvasColumn = frame.x + column
                if (canvasColumn !in 0 until canvasWidth) continue

                canvas[canvasRow * canvasWidth + canvasColumn] = frame.palette[index]
            }
        }

        upload()
    }

    private fun fillRect(frame: Frame, color: Int) {
        for (row in 0 until frame.height) {
            val canvasRow = frame.y + row
            if (canvasRow !in 0 until canvasHeight) continue

            for (column in 0 until frame.width) {
                val canvasColumn = frame.x + column
                if (canvasColumn !in 0 until canvasWidth) continue

                canvas[canvasRow * canvasWidth + canvasColumn] = color
            }
        }
    }

    /** Writes the canvas onto the texture, scaled down on the way when the gif is larger than we draw. */
    private fun upload() {
        val pixels = texture.pixels ?: return

        for (y in 0 until height) {
            val sourceY = y * canvasHeight / height
            for (x in 0 until width) {
                val sourceX = x * canvasWidth / width
                pixels.setPixelABGR(x, y, toAbgr(canvas[sourceY * canvasWidth + sourceX]))
            }
        }

        texture.upload()
    }

    fun close() {
        Minecraft.getInstance().textureManager.release(id)
        texture.close()
    }

    companion object {

        /** How wide a background is allowed to be; anything larger is scaled down to it. */
        const val MAX_WIDTH: Int = 1024

        /** How many frames of a gif are kept; past this every other frame is dropped. */
        const val MAX_FRAMES: Int = 120

        private const val DISPOSE_BACKGROUND: Int = 2
        private const val DISPOSE_PREVIOUS: Int = 3

        /** What a gif frame says to wait when it says nothing sensible, in hundredths of a second. */
        private const val DEFAULT_DELAY: Int = 10

        private var counter: Int = 0

        /** Reads a picture off disk. Null when nothing in the file could be read as one. */
        fun load(file: File): Loaded? = runCatching {
            load(file.readBytes(), file.extension.equals("gif", ignoreCase = true))
        }.onFailure { Common.LOGGER.warn("Could not read the background image ${file.name}", it) }
            .getOrNull()

        /** Reads a picture already in memory, which is how a downloaded one arrives. */
        fun load(bytes: ByteArray, gif: Boolean): Loaded? = runCatching {
            if (gif) loadGif(bytes) else loadStill(bytes)
        }.onFailure { Common.LOGGER.warn("Could not read a background image", it) }
            .getOrNull()

        private fun loadStill(bytes: ByteArray): Loaded? {
            val source = bytes.inputStream().use { NativeImage.read(it) } ?: return null

            val notes = mutableListOf<String>()
            val shrinkBy = shrinkForWidth(source.width)

            val width = source.width / shrinkBy
            val height = source.height / shrinkBy
            if (shrinkBy > 1) notes.add(scaledNote(source.width, source.height, width, height))

            val canvas = IntArray(source.width * source.height)
            for (y in 0 until source.height) {
                for (x in 0 until source.width) {
                    canvas[y * source.width + x] = fromAbgr(source.getPixel(x, y))
                }
            }
            val sourceWidth = source.width
            val sourceHeight = source.height
            source.close()

            return Loaded(buildImage(width, height, emptyList(), canvas, sourceWidth, sourceHeight), notes)
        }

        private fun loadGif(bytes: ByteArray): Loaded? {
            val reader = ImageIO.getImageReadersByFormatName("gif").let { if (it.hasNext()) it.next() else null }
                ?: return null

            val notes = mutableListOf<String>()

            try {
                ImageIO.createImageInputStream(bytes.inputStream()).use { stream ->
                    reader.setInput(stream)

                    val frameCount = reader.getNumImages(true)
                    if (frameCount <= 0) return null

                    val frameStep = if (frameCount > MAX_FRAMES) {
                        (frameCount + MAX_FRAMES - 1) / MAX_FRAMES
                    } else {
                        1
                    }
                    if (frameStep > 1) notes.add("kept every ${stepWord(frameStep)} frame of $frameCount")

                    val frames = mutableListOf<Frame>()
                    var canvasWidth = 0
                    var canvasHeight = 0

                    for (index in 0 until frameCount step frameStep) {
                        val frame = readFrame(reader, index) ?: continue
                        frames.add(frame)
                        canvasWidth = maxOf(canvasWidth, frame.x + frame.width)
                        canvasHeight = maxOf(canvasHeight, frame.y + frame.height)
                    }

                    if (frames.isEmpty() || canvasWidth == 0 || canvasHeight == 0) return null

                    val shrinkBy = shrinkForWidth(canvasWidth)
                    val width = canvasWidth / shrinkBy
                    val height = canvasHeight / shrinkBy
                    if (shrinkBy > 1) notes.add(scaledNote(canvasWidth, canvasHeight, width, height))

                    return Loaded(
                        buildImage(
                            width, height, frames,
                            IntArray(canvasWidth * canvasHeight), canvasWidth, canvasHeight
                        ),
                        notes
                    )
                }
            } finally {
                reader.dispose()
            }
        }

        private fun readFrame(reader: javax.imageio.ImageReader, index: Int): Frame? {
            val image = reader.read(index)
            val colors = image.colorModel as? IndexColorModel ?: return null

            val palette = IntArray(colors.mapSize)
            colors.getRGBs(palette)

            val raster = image.raster
            val indices = ByteArray(image.width * image.height)
            for (y in 0 until image.height) {
                for (x in 0 until image.width) {
                    indices[y * image.width + x] = raster.getSample(x, y, 0).toByte()
                }
            }

            val tree = reader.getImageMetadata(index).getAsTree("javax_imageio_gif_image_1.0") as IIOMetadataNode
            val descriptor = tree.getElementsByTagName("ImageDescriptor").item(0) as? IIOMetadataNode
            val control = tree.getElementsByTagName("GraphicControlExtension").item(0) as? IIOMetadataNode

            val delayHundredths = control?.getAttribute("delayTime")?.toIntOrNull()?.takeIf { it > 0 } ?: DEFAULT_DELAY
            val transparent = if (control?.getAttribute("transparentColorFlag") == "TRUE") {
                control.getAttribute("transparentColorIndex")?.toIntOrNull() ?: -1
            } else {
                -1
            }

            return Frame(
                x = descriptor?.getAttribute("imageLeftPosition")?.toIntOrNull() ?: 0,
                y = descriptor?.getAttribute("imageTopPosition")?.toIntOrNull() ?: 0,
                width = image.width,
                height = image.height,
                indices = indices,
                palette = palette,
                transparentIndex = transparent,
                disposal = disposalOf(control?.getAttribute("disposalMethod")),
                delayMs = delayHundredths * 10
            )
        }

        private fun disposalOf(name: String?): Int = when (name) {
            "restoreToBackgroundColor" -> DISPOSE_BACKGROUND
            "restoreToPrevious" -> DISPOSE_PREVIOUS
            else -> 0
        }

        private fun buildImage(
            width: Int,
            height: Int,
            frames: List<Frame>,
            canvas: IntArray,
            canvasWidth: Int,
            canvasHeight: Int
        ): BackgroundImage {
            counter++
            val id = Identifier.fromNamespaceAndPath(Common.MOD_ID, "background/$counter")
            val texture = DynamicTexture({ id.toString() }, NativeImage(width, height, false))

            Minecraft.getInstance().textureManager.register(id, texture)

            val image = BackgroundImage(id, texture, width, height, frames, canvas, canvasWidth, canvasHeight)

            // a still has no frame to wait for, so its one canvas goes on straight away
            if (frames.isEmpty()) image.upload() else image.advance()

            return image
        }

        /** How many times over the picture has to shrink to fit [MAX_WIDTH]. */
        private fun shrinkForWidth(width: Int): Int =
            if (width <= MAX_WIDTH) 1 else (width + MAX_WIDTH - 1) / MAX_WIDTH

        private fun scaledNote(fromWidth: Int, fromHeight: Int, toWidth: Int, toHeight: Int): String =
            "scaled it from ${fromWidth}x$fromHeight down to ${toWidth}x$toHeight"

        private fun stepWord(frameStep: Int): String = when (frameStep) {
            2 -> "second"
            3 -> "third"
            else -> "${frameStep}th"
        }

        private fun toAbgr(argb: Int): Int =
            (argb and 0xFF00FF00.toInt()) or ((argb and 0xFF) shl 16) or ((argb ushr 16) and 0xFF)

        private fun fromAbgr(abgr: Int): Int = toAbgr(abgr)
    }
}
