package org.samo_lego.chestrefill;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.commands.CommandSourceStack;

public class PlatformHelper {
    @ExpectPlatform
    public static boolean hasPermission(CommandSourceStack source, String permissionNode, boolean defaultPermission) {
        throw new AssertionError();
    }
}
