package org.samo_lego.chestrefill.forge;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import org.samo_lego.chestrefill.ChestRefill;
import org.samo_lego.chestrefill.command.ChestRefillCommand;

import java.io.File;

@Mod(ChestRefill.MOD_ID)
public class ChestRefillForge {
    public ChestRefillForge() {
        ChestRefill.init(new File(FMLPaths.CONFIGDIR.get() + "/chest_refill.json"));
    }

    @SubscribeEvent
    public void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        ChestRefillCommand.register(dispatcher, false);
    }
}
