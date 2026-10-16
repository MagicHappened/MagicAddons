package org.magic.magicaddons.commands.farming

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import org.magic.magicaddons.commands.AbstractCommand
import org.magic.magicaddons.features.farming.fortune.FarmingFortune
import org.magic.magicaddons.features.farming.fortune.FortuneSnapshot
import org.magic.magicaddons.features.farming.greenhousePresets.lookups.StatsWidget
import org.magic.magicaddons.util.ChatUtils

object FortuneCommand : AbstractCommand() {
    override val argument: String = "fortune"

    override fun build(): LiteralArgumentBuilder<FabricClientCommandSource> =
        LiteralArgumentBuilder.literal<FabricClientCommandSource>(argument).executes {
            report(FarmingFortune.snapshot())
            1
        }

    private fun report(snapshot: FortuneSnapshot) {
        val message = ChatUtils.buildWithPrefix("Farming Fortune: ${format(snapshot.total)}")

        snapshot.sources.groupBy { it.name }.forEach { (name, sources) ->
            val details = sources.mapNotNull { it.detail }.joinToString(", ")
            message.append(line(name, sources.sumOf { it.value }, details.takeIf { it.isNotEmpty() }, ChatFormatting.GREEN))
        }

        val tab = StatsWidget.value(StatsWidget.FARMING_FORTUNE)
        if (tab != null) {
            message.append(line("Tab list", tab.toDouble(), null, ChatFormatting.AQUA))
            message.append(line("Unexplained", tab - snapshot.total, null, ChatFormatting.YELLOW))
        }

        if (snapshot.unknown.isNotEmpty()) {
            message.append(Component.literal("\nNot counted yet:").withStyle(ChatFormatting.GRAY))
            snapshot.unknown.forEach { message.append(Component.literal("\n  $it").withStyle(ChatFormatting.DARK_GRAY)) }
        }

        ChatUtils.send(message)
    }

    private fun line(name: String, value: Double, detail: String?, color: ChatFormatting): Component {
        val text = Component.literal("\n  $name: ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal(format(value)).withStyle(color))
        detail?.let { text.append(Component.literal(" ($it)").withStyle(ChatFormatting.DARK_GRAY)) }
        return text
    }

    private fun format(value: Double): String =
        if (value % 1.0 == 0.0) "%+,d".format(value.toLong()) else "%+,.1f".format(value)
}
