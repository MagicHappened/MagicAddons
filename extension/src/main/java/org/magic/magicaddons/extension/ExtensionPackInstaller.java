package org.magic.magicaddons.extension;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExtensionPackInstaller implements ClientModInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger("magicaddons-extension");

    private static final String UNLOCK_CLASS = "org.magic.magicaddons.ExtensionPack";
    private static final String UNLOCK_METHOD = "install";

    @Override
    public void onInitializeClient() {
        try {
            Class.forName(UNLOCK_CLASS).getMethod(UNLOCK_METHOD).invoke(null);
        } catch (ReflectiveOperationException e) {
            LOGGER.error("MagicAddons is too old for this extension pack, nothing was enabled", e);
        }
    }
}
