package org.magic.magicaddons.util

import tech.thatgravyboat.skyblockapi.api.location.LocationAPI
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland
import tech.thatgravyboat.skyblockapi.api.profile.garden.PlotAPI

enum class SBLocation(val inside: () -> Boolean) {
    SkyBlock({ LocationAPI.isOnSkyBlock }),
    Garden({ SkyBlockIsland.GARDEN.inIsland() }),
    OwnGarden({ Garden.inside() && !LocationAPI.isGuest }),

    // cant detect someone elses greenhouse plot without some like weird block detection so its left out
    OwnGreenhouse({ OwnGarden.inside() && PlotAPI.getCurrentPlot()?.data?.isGreenhouse == true }),
    MiningIslands({
        SkyBlockIsland.inAnyIsland(SkyBlockIsland.DWARVEN_MINES, SkyBlockIsland.CRYSTAL_HOLLOWS, SkyBlockIsland.MINESHAFT)
    }),
    Mineshaft({ SkyBlockIsland.MINESHAFT.inIsland() }),
    Safari({ SkyBlockIsland.SAFARI.inIsland() }),
    Kuudra({ SkyBlockIsland.KUUDRA.inIsland() })
}
