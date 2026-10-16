package org.magic.magicaddons.util

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import java.awt.Color
import java.awt.RenderingHints
import java.awt.geom.Path2D
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.util.concurrent.CompletableFuture
import javax.imageio.ImageIO
import kotlin.math.floor
import kotlin.math.sqrt
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.Minecraft
import net.minecraft.core.component.DataComponents
import net.minecraft.resources.Identifier
import net.minecraft.world.item.ItemStack
import org.magic.magicaddons.Common
import org.magic.magicaddons.data.handlers.ModFiles
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId.Companion.getSkyBlockId

object ItemIconExport {

    private const val ICON_SIZE: Int = 128
    private const val ICON_PADDING: Int = 4

    private const val PACK_NAMESPACE: String = "hypixel_skyblock"
    private const val MAX_MODEL_PARENTS: Int = 8

    private const val HEAD_HALF: Double = 4.0
    private const val HAT_HALF: Double = 4.25
    private const val BLOCK_HALF: Double = 4.0
    private const val FACE_PIXELS: Int = 8

    private const val TOP_SHADE: Double = 1.0
    private const val FRONT_SHADE: Double = 0.85
    private const val SIDE_SHADE: Double = 0.7

    private val COS_30: Double = sqrt(3.0) / 2
    private val SKIN_TIMEOUT: Duration = Duration.ofSeconds(10)

    private val http: HttpClient by lazy { HttpClient.newBuilder().connectTimeout(SKIN_TIMEOUT).build() }

    val iconDir: Path get() = ModFiles.modDir.resolve("icons")

    val shownIconDir: String get() = FabricLoader.getInstance().gameDir.relativize(iconDir).toString().replace('\\', '/')

    enum class Source { ResourcePack, Skull }

    class Request(val label: String, val fileName: String, val stack: ItemStack)

    sealed interface Outcome {
        data class Exported(val file: String, val source: Source) : Outcome
        data object NoIcon : Outcome
    }

    private sealed interface IconSource {
        class Texture(val image: BufferedImage) : IconSource
        class Block(val top: BufferedImage, val side: BufferedImage) : IconSource
        class Skin(val url: String) : IconSource
    }

    fun fileNameFor(name: String): String =
        name.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_').take(32).padEnd(2, '_')

    fun export(requests: List<Request>): CompletableFuture<List<Pair<Request, Outcome>>> {
        val packDefinitions = packItemDefinitions()
        val sources = requests.map { request -> request to sourceOf(request.stack, packDefinitions) }

        return CompletableFuture.supplyAsync {
            Files.createDirectories(iconDir)
            sources.map { (request, source) -> request to written(request, source) }
        }
    }

    private fun written(request: Request, source: IconSource?): Outcome {
        val icon = runCatching {
            when (source) {
                is IconSource.Texture -> scaledToIcon(source.image)
                is IconSource.Block -> cubeIcon(listOf(CubeLayer(BLOCK_HALF, source.top, source.side, source.side, isSolid = false)))
                is IconSource.Skin -> downloadedSkin(source.url)?.let(::headIcon)
                null -> null
            }
        }.onFailure { Common.LOGGER.warn("Could not draw the icon for {}", request.label, it) }.getOrNull()
            ?: return Outcome.NoIcon

        val file = "${request.fileName}.png"
        ImageIO.write(icon, "png", iconDir.resolve(file).toFile())

        return Outcome.Exported(file, if (source is IconSource.Skin) Source.Skull else Source.ResourcePack)
    }

    private fun sourceOf(stack: ItemStack, packDefinitions: Map<String, Identifier>): IconSource? {
        if (stack.isEmpty) return null
        val itemModel = stack.get(DataComponents.ITEM_MODEL)
        val definitions = listOfNotNull(
            itemModel?.takeIf { it.namespace == PACK_NAMESPACE },
            stack.getSkyBlockId()?.id?.lowercase()?.let { packDefinitions[it] },
            itemModel?.takeIf { it.namespace != PACK_NAMESPACE }
        )

        definitions.firstNotNullOfOrNull { iconSourceOf(it) }?.let { return it }

        return PlayerUtils.getSkinUrl(stack)?.let { IconSource.Skin(it) }
    }

    private fun packItemDefinitions(): Map<String, Identifier> =
        Minecraft.getInstance().resourceManager
            .listResources("items") { it.namespace == PACK_NAMESPACE && it.path.endsWith(".json") }
            .keys
            .map { file -> file.withPath { it.removePrefix("items/").removeSuffix(".json") } }
            .associateBy { it.path.substringAfterLast('/') }

    private fun iconSourceOf(definition: Identifier): IconSource? {
        val json = readJson(definition.withPath { "items/$it.json" }) ?: return null
        val model = firstModelIn(json) ?: return null
        val textures = texturesOf(model, MAX_MODEL_PARENTS)
        fun image(vararg keys: String) = keys.firstNotNullOfOrNull { textureIn(textures, it) }?.let { readTexture(it) }

        image("layer0")?.let { return IconSource.Texture(it) }
        val top = image("top", "end", "up", "all") ?: return null
        val side = image("side", "north", "all", "particle") ?: return null

        return IconSource.Block(top, side)
    }

    private fun firstModelIn(element: JsonElement): Identifier? = when {
        element.isJsonObject -> {
            val model = element.asJsonObject.get("model")
            if (model != null && model.isJsonPrimitive) Identifier.tryParse(model.asString)
            else element.asJsonObject.entrySet().firstNotNullOfOrNull { (_, child) -> firstModelIn(child) }
        }
        element.isJsonArray -> element.asJsonArray.firstNotNullOfOrNull { firstModelIn(it) }
        else -> null
    }

    private fun texturesOf(model: Identifier, parentsLeft: Int): Map<String, String> {
        val json = readJson(model.withPath { "models/$it.json" })?.takeIf { it.isJsonObject }?.asJsonObject ?: return emptyMap()
        val own = json.getAsJsonObject("textures")?.entrySet()
            ?.filter { (_, value) -> value.isJsonPrimitive }
            ?.associate { (key, value) -> key to value.asString }
            .orEmpty()
        val parent = json.get("parent")?.asString?.let(Identifier::tryParse)?.takeIf { parentsLeft > 0 }

        return parent?.let { texturesOf(it, parentsLeft - 1) }.orEmpty() + own
    }

    private fun textureIn(textures: Map<String, String>, key: String): Identifier? {
        var value = textures[key] ?: return null
        repeat(MAX_MODEL_PARENTS) {
            if (!value.startsWith("#")) return Identifier.tryParse(value)
            value = textures[value.drop(1)] ?: return null
        }
        return null
    }

    private fun readTexture(texture: Identifier): BufferedImage? {
        val image = readImage(texture.withPath { "textures/$it.png" }) ?: return null

        return if (image.height > image.width) image.getSubimage(0, 0, image.width, image.width) else image
    }

    private fun readJson(file: Identifier): JsonElement? = runCatching {
        Minecraft.getInstance().resourceManager.getResource(file).orElse(null)
            ?.openAsReader()?.use { JsonParser.parseReader(it) }
    }.getOrNull()

    private fun readImage(file: Identifier): BufferedImage? = runCatching {
        Minecraft.getInstance().resourceManager.getResource(file).orElse(null)
            ?.open()?.use { ImageIO.read(it) }
    }.getOrNull()

    private fun downloadedSkin(url: String): BufferedImage? {
        val request = HttpRequest.newBuilder(URI.create(url.replaceFirst("http://", "https://"))).timeout(SKIN_TIMEOUT).GET().build()
        val response = http.send(request, HttpResponse.BodyHandlers.ofByteArray())
        if (response.statusCode() != 200) return null

        return ImageIO.read(ByteArrayInputStream(response.body()))
    }

    private fun scaledToIcon(image: BufferedImage): BufferedImage {
        val fit = (ICON_SIZE - ICON_PADDING * 2).toDouble() / maxOf(image.width, image.height)
        val scale = if (fit >= 1) floor(fit) else fit
        val width = (image.width * scale).toInt()
        val height = (image.height * scale).toInt()

        val icon = BufferedImage(ICON_SIZE, ICON_SIZE, BufferedImage.TYPE_INT_ARGB)
        val graphics = icon.createGraphics()
        graphics.setRenderingHint(
            RenderingHints.KEY_INTERPOLATION,
            if (scale >= 1) RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR else RenderingHints.VALUE_INTERPOLATION_BILINEAR
        )
        graphics.drawImage(image, (ICON_SIZE - width) / 2, (ICON_SIZE - height) / 2, width, height, null)
        graphics.dispose()

        return icon
    }

    private class CubeLayer(val half: Double, val top: BufferedImage, val front: BufferedImage, val side: BufferedImage, val isSolid: Boolean)

    private class Face(val image: BufferedImage, val origin: DoubleArray, val uAxis: DoubleArray, val vAxis: DoubleArray, val shade: Double)

    private fun facesOf(layer: CubeLayer): List<Face> {
        val half = layer.half
        val span = half * 2

        return listOf(
            Face(layer.top, doubleArrayOf(-half, half, -half), doubleArrayOf(span, 0.0, 0.0), doubleArrayOf(0.0, 0.0, span), TOP_SHADE),
            Face(layer.front, doubleArrayOf(-half, half, half), doubleArrayOf(span, 0.0, 0.0), doubleArrayOf(0.0, -span, 0.0), FRONT_SHADE),
            Face(layer.side, doubleArrayOf(half, half, half), doubleArrayOf(0.0, 0.0, -span), doubleArrayOf(0.0, -span, 0.0), SIDE_SHADE)
        )
    }

    private fun headIcon(skin: BufferedImage): BufferedImage {
        fun region(x: Int, y: Int) = skin.getSubimage(x, y, FACE_PIXELS, FACE_PIXELS)

        return cubeIcon(
            listOf(
                CubeLayer(HEAD_HALF, region(8, 0), region(8, 8), region(16, 8), isSolid = true),
                CubeLayer(HAT_HALF, region(40, 0), region(40, 8), region(48, 8), isSolid = false)
            )
        )
    }

    private fun cubeIcon(layers: List<CubeLayer>): BufferedImage {
        val icon = BufferedImage(ICON_SIZE, ICON_SIZE, BufferedImage.TYPE_INT_ARGB)
        val graphics = icon.createGraphics()
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF)

        val unit = (ICON_SIZE - ICON_PADDING * 2) / (layers.maxOf { it.half } * 4)
        val center = ICON_SIZE / 2.0
        fun screenX(point: DoubleArray) = center + (point[0] - point[2]) * COS_30 * unit
        fun screenY(point: DoubleArray) = center + ((point[0] + point[2]) / 2 - point[1]) * unit

        for (layer in layers) {
            for (face in facesOf(layer)) {
                val pixels = face.image.width
                for (v in 0 until pixels) {
                    for (u in 0 until pixels) {
                        val argb = face.image.getRGB(u, v)
                        val alpha = argb ushr 24
                        if (alpha == 0 && !layer.isSolid) continue

                        fun corner(du: Int, dv: Int) = DoubleArray(3) { axis ->
                            face.origin[axis] + (u + du).toDouble() / pixels * face.uAxis[axis] + (v + dv).toDouble() / pixels * face.vAxis[axis]
                        }
                        val corners = listOf(corner(0, 0), corner(1, 0), corner(1, 1), corner(0, 1))
                        val pixel = Path2D.Double().apply {
                            moveTo(screenX(corners[0]), screenY(corners[0]))
                            corners.drop(1).forEach { lineTo(screenX(it), screenY(it)) }
                            closePath()
                        }

                        graphics.color = Color(
                            ((argb shr 16 and 0xFF) * face.shade).toInt(),
                            ((argb shr 8 and 0xFF) * face.shade).toInt(),
                            ((argb and 0xFF) * face.shade).toInt(),
                            if (layer.isSolid) 255 else alpha
                        )
                        graphics.fill(pixel)
                    }
                }
            }
        }
        graphics.dispose()

        return icon
    }
}
