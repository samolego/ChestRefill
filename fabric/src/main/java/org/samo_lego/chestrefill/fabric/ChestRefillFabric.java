package org.samo_lego.chestrefill.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import org.samo_lego.chestrefill.ChestRefill;
import org.samo_lego.chestrefill.command.ChestRefillCommand;

import java.io.File;

public class ChestRefillFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        ChestRefill.init(new File(FabricLoader.getInstance().getConfigDir() + "/chest_refill.json"));
        CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) -> ChestRefillCommand.register(dispatcher));
    }
}
