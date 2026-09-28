package org.magic.magicaddons.ui.fonts

import net.fabricmc.fabric.api.resource.v1.ResourceLoader
import net.minecraft.SharedConstants
import net.minecraft.client.Minecraft
import net.minecraft.resources.Identifier
import net.minecraft.server.packs.PackType
import net.minecraft.server.packs.resources.ResourceManagerReloadListener
import org.magic.magicaddons.Common
import org.magic.magicaddons.data.handlers.ModFiles
import org.magic.magicaddons.util.ChatUtils
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.nameWithoutExtension

object SystemFonts {

    private val BUILT_IN_FONT_ID_BY_NAME: Map<String, Identifier> = linkedMapOf(
        "Minecraft" to Identifier.withDefaultNamespace("default"),
        "Uniform" to Identifier.withDefaultNamespace("uniform"),
        "Enchanting" to Identifier.withDefaultNamespace("alt"),
        "Illager" to Identifier.withDefaultNamespace("illageralt")
    )

    val DEFAULT_FONT_NAME: String = BUILT_IN_FONT_ID_BY_NAME.keys.first()

    private val SYSTEM_FONT_ID: Identifier = Identifier.fromNamespaceAndPath(Common.MOD_ID, "system")

    private const val FONT_PACK_NAME: String = "magicaddons-fonts"

    private const val FONT_PACK_ID: String = "file/$FONT_PACK_NAME"

    private const val COVERAGE_SAMPLE_TEXT: String =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789 .,:;!?%/()[]-+…"

    const val LOW_COVERAGE_THRESHOLD: Float = 0.2f

    private const val FONT_FOLDER_SCAN_DEPTH: Int = 4

    private val SYSTEM_FONT_FOLDERS: List<Path> = listOf(
        System.getenv("WINDIR")?.let { Path.of(it, "Fonts") },
        System.getenv("LOCALAPPDATA")?.let { Path.of(it, "Microsoft", "Windows", "Fonts") },
        Path.of("/usr/share/fonts"),
        Path.of("/usr/local/share/fonts"),
        Path.of(System.getProperty("user.home"), ".fonts"),
        Path.of(System.getProperty("user.home"), ".local", "share", "fonts"),
        Path.of("/Library/Fonts"),
        Path.of("/System/Library/Fonts"),
        Path.of(System.getProperty("user.home"), "Library", "Fonts")
    ).mapNotNull { it }

    private val installedFontPathByName: Map<String, Path> by lazy {
        val fontPathByName = sortedMapOf<String, Path>(String.CASE_INSENSITIVE_ORDER)

        SYSTEM_FONT_FOLDERS.filter { it.exists() }.forEach { folder ->
            runCatching {
                Files.walk(folder, FONT_FOLDER_SCAN_DEPTH).use { paths ->
                    paths.filter { it.isRegularFile() && it.extension.lowercase() == "ttf" }
                        .forEach { path -> fontPathByName.putIfAbsent(fontNameInFile(path), path) }
                }
            }.onFailure { Common.LOGGER.warn("Could not read the fonts in $folder", it) }
        }

        fontPathByName.keys.removeAll { it in BUILT_IN_FONT_ID_BY_NAME }
        fontPathByName
    }

    private var cachedIsPackWritten: Boolean? = null

    private var cachedIsPackSelected: Boolean? = null

    fun init() {
        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(
            Identifier.fromNamespaceAndPath(Common.MOD_ID, "system_font_pack"),
            ResourceManagerReloadListener { forgetPackState() }
        )
    }

    private fun forgetPackState() {
        cachedIsPackWritten = null
        cachedIsPackSelected = null
    }

    private fun fontNameInFile(path: Path): String =
        runCatching { java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, path.toFile()).fontName }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: path.nameWithoutExtension

    fun sampleCoverageOf(name: String): Float {
        val fontFile = installedFontPathByName[name] ?: return 1f
        val font = runCatching { java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, fontFile.toFile()) }
            .getOrNull() ?: return 0f

        val drawableCount = COVERAGE_SAMPLE_TEXT.count { font.canDisplay(it) }

        return drawableCount.toFloat() / COVERAGE_SAMPLE_TEXT.length
    }

    fun fontChoices(): List<String> = BUILT_IN_FONT_ID_BY_NAME.keys.toList() + installedFontPathByName.keys

    fun isBuiltInFont(name: String): Boolean = name in BUILT_IN_FONT_ID_BY_NAME

    private fun isFontPackActive(): Boolean {
        val isPackWritten = cachedIsPackWritten
            ?: fontPackFolder().resolve("pack.mcmeta").exists().also { cachedIsPackWritten = it }
        val isPackSelected = cachedIsPackSelected
            ?: (FONT_PACK_ID in Minecraft.getInstance().resourcePackRepository.selectedIds).also { cachedIsPackSelected = it }

        return isPackWritten && isPackSelected
    }

    fun fontIdFor(name: String): Identifier? {
        BUILT_IN_FONT_ID_BY_NAME[name]?.let { return if (name == DEFAULT_FONT_NAME) null else it }
        return if (isFontPackActive()) SYSTEM_FONT_ID else null
    }

    private fun fontPackFolder(): Path =
        Minecraft.getInstance().resourcePackDirectory.resolve(FONT_PACK_NAME)

    fun installSystemFont(name: String) {
        val fontFile = installedFontPathByName[name] ?: return
        val minecraft = Minecraft.getInstance()

        forgetPackState()

        runCatching {
            writeFontPack(fontFile)

            val repository = minecraft.resourcePackRepository
            repository.reload()
            if (repository.getPack(FONT_PACK_ID) == null) error("The game did not find the pack $FONT_PACK_ID")
            repository.addPack(FONT_PACK_ID)
            minecraft.options.updateResourcePacks(repository)
        }.onFailure {
            Common.LOGGER.warn("Could not set up the font pack for $name", it)
            ChatUtils.sendWithPrefix("Could not set up that font.")
            return
        }

        minecraft.reloadResourcePacks()
    }

    private fun writeFontPack(fontFile: Path) {
        val fontFolder = fontPackFolder().resolve("assets").resolve(Common.MOD_ID).resolve("font")
        val format = SharedConstants.getCurrentVersion().packVersion(PackType.CLIENT_RESOURCES)
        ModFiles.writeTextAtomically(fontPackFolder().resolve("pack.mcmeta"), 
            """
            {
              "pack": {
                "pack_format": ${format.major()},
                "min_format": [${format.major()}, ${format.minor()}],
                "max_format": [${format.major()}, ${format.minor()}],
                "description": "MagicAddons fonts"
              }
            }
            """.trimIndent()
        )

        ModFiles.copyAtomically(fontFile, fontFolder.resolve("system.ttf"))

        ModFiles.writeTextAtomically(fontFolder.resolve("system.json"), 
            """
            {
              "providers": [
                {
                  "type": "ttf",
                  "file": "${Common.MOD_ID}:system.ttf",
                  "size": 11.0,
                  "oversample": 2.0,
                  "shift": [0.0, 0.0],
                  "skip": ""
                }
              ]
            }
            """.trimIndent()
        )
    }
}
