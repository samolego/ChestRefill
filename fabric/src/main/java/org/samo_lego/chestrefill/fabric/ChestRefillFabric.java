package org.samo_lego.chestrefill.fabric;

import net.fabricmc.api.ModInitializer;
import org.samo_lego.chestrefill.ChestRefill;

public class ChestRefillFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        //UseBlockCallback.EVENT.register(BlockInteractionEventImpl::onInteractBlock);
        ChestRefill.init();
    }
}
