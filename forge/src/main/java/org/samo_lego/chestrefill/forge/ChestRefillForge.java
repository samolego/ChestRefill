package org.samo_lego.chestrefill.forge;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import org.samo_lego.chestrefill.ChestRefill;

import java.io.File;

@Mod(ChestRefill.MOD_ID)
public class ChestRefillForge {
    public ChestRefillForge() {
        ChestRefill.init(new File(FMLPaths.CONFIGDIR.get() + "/chest_refill.json"));
    }
}
