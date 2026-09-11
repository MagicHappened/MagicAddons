package org.magic.magicaddons.ui.fonts

import net.minecraft.SharedConstants
import net.minecraft.client.Minecraft
import net.minecraft.resources.Identifier
import net.minecraft.server.packs.PackType
import org.magic.magicaddons.Common
import org.magic.magicaddons.util.ChatUtils
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.nameWithoutExtension

/**
 * The fonts the mod's screens can write in: the game's own, and any TrueType font installed on this
 * computer. A system font reaches the game through a small resource pack holding just that one file.
 */
object SystemFonts {

    /** The game's fonts by the name the picker shows, the first being what the mod ships with. */
    private val builtIn: Map<String, Identifier> = linkedMapOf(
        "Minecraft" to Identifier.withDefaultNamespace("default"),
        "Uniform" to Identifier.withDefaultNamespace("uniform"),
        "Enchanting" to Identifier.withDefaultNamespace("alt"),
        "Illager" to Identifier.withDefaultNamespace("illageralt")
    )

    val defaultName: String = builtIn.keys.first()

    /** The one font id the pack ever defines; whichever file is picked is written under it. */
    private val systemFontId: Identifier = Identifier.fromNamespaceAndPath(Common.MOD_ID, "system")

    private const val PACK_NAME: String = "magicaddons-fonts"

    /** The writing a font is tried against: letters, digits and the punctuation the screens use. */
    private const val SAMPLE: String =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789 .,:;!?%/()[]-+…"

    /** Below this share of [SAMPLE] a font is warned about before it is used. */
    const val WARN_BELOW: Float = 0.2f

    /** How the game names a folder pack in the options, so it can be turned on by name. */
    private const val PACK_ID: String = "file/$PACK_NAME"

    /** Where each system puts its fonts. Folders that do not exist are skipped. */
    private val fontFolders: List<Path> = listOf(
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

    /**
     * Every TrueType file found, by the name the font calls itself rather than its file name, so
     * "comicbd.ttf" is listed as Comic Sans MS Bold. Read once on the first look.
     */
    private val installed: Map<String, Path> by lazy {
        val found = sortedMapOf<String, Path>(String.CASE_INSENSITIVE_ORDER)

        fontFolders.filter { it.exists() }.forEach { folder ->
            runCatching {
                Files.walk(folder, 2).use { paths ->
                    paths.filter { it.isRegularFile() && it.extension.lowercase() == "ttf" }
                        .forEach { path -> found.putIfAbsent(nameOf(path), path) }
                }
            }.onFailure { Common.LOGGER.warn("Could not read the fonts in $folder", it) }
        }

        found
    }

    /** The name written inside the font file, or the file's own name when that cannot be read. */
    private fun nameOf(path: Path): String =
        runCatching { java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, path.toFile()).fontName }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: path.nameWithoutExtension

    /**
     * How much of ordinary writing a font can draw, from none to all. A symbol font such as Wingdings
     * has pictures where the letters should be and comes out as boxes in the game.
     */
    fun coverage(name: String): Float {
        val file = installed[name] ?: return 1f
        val font = runCatching { java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, file.toFile()) }
            .getOrNull() ?: return 0f

        val drawable = SAMPLE.count { font.canDisplay(it) }

        return drawable.toFloat() / SAMPLE.length
    }

    /** What the picker offers: the game's fonts first, then everything installed. */
    fun choices(): List<String> = builtIn.keys.toList() + installed.keys

    fun isBuiltIn(name: String): Boolean = name in builtIn

    /** Whether the pack for a system font is in place and switched on, so its id resolves. */
    private fun packReady(): Boolean {
        val repository = Minecraft.getInstance().resourcePackRepository
        return packFolder().resolve("pack.mcmeta").exists() && PACK_ID in repository.selectedIds
    }

    /** The font id to write in, or null for the game's own default, which needs no style at all. */
    fun fontIdFor(name: String): Identifier? {
        builtIn[name]?.let { return if (name == defaultName) null else it }
        return if (name in installed && packReady()) systemFontId else null
    }

    private fun packFolder(): Path =
        Minecraft.getInstance().resourcePackDirectory.resolve(PACK_NAME)

    /**
     * Copies the chosen file into the mod's pack, turns the pack on, and reloads so the game reads
     * it. A font that is the game's own needs none of this.
     */
    fun install(name: String) {
        val file = installed[name] ?: return
        val minecraft = Minecraft.getInstance()

        runCatching {
            writePack(file)

            // the repository decides what a reload loads and rewrites the options from itself, so the
            // pack is turned on there: rescanned first, since the folder may have just been made
            val repository = minecraft.resourcePackRepository
            repository.reload()
            if (!repository.addPack(PACK_ID)) error("The game did not find the pack $PACK_ID")
            minecraft.options.updateResourcePacks(repository)
        }.onFailure {
            Common.LOGGER.warn("Could not set up the font pack for $name", it)
            ChatUtils.sendWithPrefix("Could not set up that font.")
            return
        }

        minecraft.reloadResourcePacks()
    }

    private fun writePack(font: Path) {
        val fontFolder = packFolder().resolve("assets").resolve(Common.MOD_ID).resolve("font")
        Files.createDirectories(fontFolder)

        val format = SharedConstants.getCurrentVersion().packVersion(PackType.CLIENT_RESOURCES)
        packFolder().resolve("pack.mcmeta").toFile().writeText(
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

        Files.copy(font, fontFolder.resolve("system.ttf"), java.nio.file.StandardCopyOption.REPLACE_EXISTING)

        fontFolder.resolve("system.json").toFile().writeText(
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

    /** Whether a name the picker holds still has a file behind it, for one deleted since. */
    fun exists(name: String): Boolean = isBuiltIn(name) || installed[name]?.toFile()?.let(File::isFile) == true
}
