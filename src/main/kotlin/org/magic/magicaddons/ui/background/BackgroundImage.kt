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

    private class Frame(
        val x: Int,
        val y: Int,
        val width: Int,
        val height: Int,
        val paletteIndices: ByteArray,
        val palette: IntArray,
        val transparentPaletteIndex: Int,
        val disposalMethod: Int,
        val delayMs: Int
    )

    class Loaded(val image: BackgroundImage, val notes: List<String>)

    private var currentFrameIndex: Int = -1
    private var nextFrameAtMs: Long = 0

    private var canvasBeforeFrame: IntArray? = null

    fun advanceAnimation() {
        if (frames.isEmpty()) return

        val now = System.currentTimeMillis()
        if (currentFrameIndex >= 0 && now < nextFrameAtMs) return
        if (currentFrameIndex >= 0 && frames.size == 1) return

        val nextIndex = if (currentFrameIndex < 0) 0 else (currentFrameIndex + 1) % frames.size
        drawFrameOnCanvas(frames[nextIndex], first = currentFrameIndex < 0)

        currentFrameIndex = nextIndex
        nextFrameAtMs = now + frames[nextIndex].delayMs
    }

    private fun drawFrameOnCanvas(frame: Frame, first: Boolean) {
        val previousFrame = if (first) null else frames[currentFrameIndex]

        when (previousFrame?.disposalMethod) {
            DISPOSE_BACKGROUND -> clearFrameArea(previousFrame)
            DISPOSE_PREVIOUS -> canvasBeforeFrame?.let { it.copyInto(canvas) }
            else -> Unit
        }

        if (frame.disposalMethod == DISPOSE_PREVIOUS) {
            canvasBeforeFrame = canvas.copyOf()
        }

        for (row in 0 until frame.height) {
            val canvasRow = frame.y + row
            if (canvasRow !in 0 until canvasHeight) continue

            for (column in 0 until frame.width) {
                val index = frame.paletteIndices[row * frame.width + column].toInt() and 0xFF
                if (index == frame.transparentPaletteIndex) continue

                val canvasColumn = frame.x + column
                if (canvasColumn !in 0 until canvasWidth) continue

                canvas[canvasRow * canvasWidth + canvasColumn] = frame.palette[index]
            }
        }

        pushCanvasToTexture()
    }

    private fun clearFrameArea(frame: Frame) {
        for (row in 0 until frame.height) {
            val canvasRow = frame.y + row
            if (canvasRow !in 0 until canvasHeight) continue

            for (column in 0 until frame.width) {
                val canvasColumn = frame.x + column
                if (canvasColumn !in 0 until canvasWidth) continue

                canvas[canvasRow * canvasWidth + canvasColumn] = 0
            }
        }
    }

    private fun pushCanvasToTexture() {
        val pixels = texture.pixels

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

        const val MAX_WIDTH: Int = 1024

        const val MAX_FRAMES: Int = 120

        private const val DISPOSE_BACKGROUND: Int = 2
        private const val DISPOSE_PREVIOUS: Int = 3

        private const val DEFAULT_DELAY_HUNDREDTHS: Int = 10

        private var textureCounter: Int = 0

        fun load(file: File): Loaded? = runCatching {
            load(file.readBytes(), file.extension.equals("gif", ignoreCase = true))
        }.onFailure { Common.LOGGER.warn("Could not read the background image ${file.name}", it) }
            .getOrNull()

        fun load(bytes: ByteArray, gif: Boolean): Loaded? = runCatching {
            if (gif) loadGif(bytes) else loadStill(bytes)
        }.onFailure { Common.LOGGER.warn("Could not read a background image", it) }
            .getOrNull()

        private fun loadStill(bytes: ByteArray): Loaded? {
            val stillImage = bytes.inputStream().use { NativeImage.read(it) }

            val notes = mutableListOf<String>()
            val shrinkBy = shrinkFactorFor(stillImage.width)

            val width = stillImage.width / shrinkBy
            val height = stillImage.height / shrinkBy
            if (shrinkBy > 1) notes.add(scaledNote(stillImage.width, stillImage.height, width, height))

            val canvas = IntArray(stillImage.width * stillImage.height)
            for (y in 0 until stillImage.height) {
                for (x in 0 until stillImage.width) {
                    canvas[y * stillImage.width + x] = fromAbgr(stillImage.getPixel(x, y))
                }
            }
            val sourceWidth = stillImage.width
            val sourceHeight = stillImage.height
            stillImage.close()

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
                    if (frameStep > 1) notes.add("kept every ${ordinalWord(frameStep)} frame of $frameCount")

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

                    val shrinkBy = shrinkFactorFor(canvasWidth)
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
            val paletteIndices = ByteArray(image.width * image.height)
            for (y in 0 until image.height) {
                for (x in 0 until image.width) {
                    paletteIndices[y * image.width + x] = raster.getSample(x, y, 0).toByte()
                }
            }

            val tree = reader.getImageMetadata(index).getAsTree("javax_imageio_gif_image_1.0") as IIOMetadataNode
            val descriptor = tree.getElementsByTagName("ImageDescriptor").item(0) as? IIOMetadataNode
            val graphicControl = tree.getElementsByTagName("GraphicControlExtension").item(0) as? IIOMetadataNode

            val delayHundredths = graphicControl?.getAttribute("delayTime")?.toIntOrNull()?.takeIf { it > 0 } ?: DEFAULT_DELAY_HUNDREDTHS
            val transparentPaletteIndex = if (graphicControl?.getAttribute("transparentColorFlag") == "TRUE") {
                graphicControl.getAttribute("transparentColorIndex")?.toIntOrNull() ?: -1
            } else {
                -1
            }

            return Frame(
                x = descriptor?.getAttribute("imageLeftPosition")?.toIntOrNull() ?: 0,
                y = descriptor?.getAttribute("imageTopPosition")?.toIntOrNull() ?: 0,
                width = image.width,
                height = image.height,
                paletteIndices = paletteIndices,
                palette = palette,
                transparentPaletteIndex = transparentPaletteIndex,
                disposalMethod = disposalOf(graphicControl?.getAttribute("disposalMethod")),
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
            textureCounter++
            val id = Identifier.fromNamespaceAndPath(Common.MOD_ID, "background/$textureCounter")
            val texture = DynamicTexture({ id.toString() }, NativeImage(width, height, false))

            Minecraft.getInstance().textureManager.register(id, texture)

            val image = BackgroundImage(id, texture, width, height, frames, canvas, canvasWidth, canvasHeight)

            if (frames.isEmpty()) image.pushCanvasToTexture() else image.advanceAnimation()

            return image
        }

        private fun shrinkFactorFor(width: Int): Int =
            if (width <= MAX_WIDTH) 1 else (width + MAX_WIDTH - 1) / MAX_WIDTH

        private fun scaledNote(fromWidth: Int, fromHeight: Int, toWidth: Int, toHeight: Int): String =
            "scaled it from ${fromWidth}x$fromHeight down to ${toWidth}x$toHeight"

        private fun ordinalWord(frameStep: Int): String = when (frameStep) {
            2 -> "second"
            3 -> "third"
            else -> "${frameStep}th"
        }

        private fun toAbgr(argb: Int): Int =
            (argb and 0xFF00FF00.toInt()) or ((argb and 0xFF) shl 16) or ((argb ushr 16) and 0xFF)

        private fun fromAbgr(abgr: Int): Int = toAbgr(abgr)
    }
}
