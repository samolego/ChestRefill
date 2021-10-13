package org.samo_lego.chestrefill.forge;

import net.minecraftforge.fml.common.Mod;
import org.samo_lego.chestrefill.ChestRefill;

@Mod(ChestRefill.MOD_ID)
public class ChestRefillForge {
    public ChestRefillForge() {
        ChestRefill.init();
    }
}
