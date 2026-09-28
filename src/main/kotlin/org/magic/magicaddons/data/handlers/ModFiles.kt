package org.magic.magicaddons.data.handlers

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import net.fabricmc.loader.api.FabricLoader
import org.magic.magicaddons.Common

object ModFiles {

    val configDir: Path = FabricLoader.getInstance().configDir
    val modDir: Path = configDir.resolve(Common.MOD_ID)
    val dataDir: Path = modDir.resolve("data")

    sealed interface LoadResult<out T> {
        data object NoFile : LoadResult<Nothing>
        class Loaded<T>(val value: T, val restoredFromBackup: Boolean, val brokenPath: Path?) : LoadResult<T>
        class Unreadable(val cause: Throwable) : LoadResult<Nothing>
    }

    fun init() {
        Files.createDirectories(dataDir)
    }

    fun writeTextAtomically(path: Path, text: String) = writeBytesAtomically(path, text.toByteArray())

    fun writeBytesAtomically(path: Path, bytes: ByteArray) {
        val temporaryPath = temporaryPathOf(path)
        Files.write(temporaryPath, bytes)
        Files.move(temporaryPath, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }

    fun copyAtomically(source: Path, target: Path) {
        val temporaryPath = temporaryPathOf(target)
        Files.copy(source, temporaryPath, StandardCopyOption.REPLACE_EXISTING)
        Files.move(temporaryPath, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }

    fun saveTextWithBackup(path: Path, text: String, checkReadable: (String) -> Unit) {
        val temporaryPath = temporaryPathOf(path)
        Files.writeString(temporaryPath, text)
        runCatching { checkReadable(Files.readString(temporaryPath)) }.onFailure {
            Files.deleteIfExists(temporaryPath)
            throw it
        }

        if (Files.exists(path)) Files.copy(path, backupPathOf(path), StandardCopyOption.REPLACE_EXISTING)
        Files.move(temporaryPath, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }

    fun <T> loadTextWithBackup(path: Path, read: (String) -> T): LoadResult<T> {
        val backupPath = backupPathOf(path)
        if (!Files.exists(path) && !Files.exists(backupPath)) return LoadResult.NoFile

        val fromFile = runCatching { read(Files.readString(path)) }
        fromFile.onSuccess { return LoadResult.Loaded(it, restoredFromBackup = false, brokenPath = null) }

        val brokenPath = if (Files.exists(path)) siblingOf(path, ".broken") else null
        brokenPath?.let { runCatching { Files.copy(path, it, StandardCopyOption.REPLACE_EXISTING) } }

        val fromBackup = runCatching { read(Files.readString(backupPath)) }
        fromBackup.onSuccess {
            Files.copy(backupPath, path, StandardCopyOption.REPLACE_EXISTING)
            return LoadResult.Loaded(it, restoredFromBackup = true, brokenPath = brokenPath)
        }

        val cause = fromFile.exceptionOrNull()!!
        fromBackup.exceptionOrNull()?.let { cause.addSuppressed(it) }
        return LoadResult.Unreadable(cause)
    }

    private fun temporaryPathOf(path: Path): Path {
        path.parent?.let { Files.createDirectories(it) }
        return siblingOf(path, ".tmp")
    }

    private fun backupPathOf(path: Path): Path = siblingOf(path, ".bak")

    private fun siblingOf(path: Path, suffix: String): Path = path.resolveSibling(path.fileName.toString() + suffix)
}
