package org.samo_lego.chestrefill.fabric;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.commands.CommandSourceStack;

public class PlatformHelperImpl {

    public static boolean hasPermission(CommandSourceStack source, String permissionNode, boolean defaultPermission) {
        return Permissions.check(source, permissionNode, defaultPermission);
    }
}
