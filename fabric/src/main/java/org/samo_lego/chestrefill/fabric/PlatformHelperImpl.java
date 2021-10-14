package org.samo_lego.chestrefill.fabric;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;

public class PlatformHelperImpl {
    private static final boolean LUCKPERMS_LOADED = FabricLoader.getInstance().isModLoaded("fabric-permissions-api-v0");

    public static boolean hasPermission(CommandSourceStack source, String permissionNode, boolean defaultPermission) {
        return LUCKPERMS_LOADED ? PermissionHelper.hasPermission(source, permissionNode, defaultPermission) : defaultPermission;
    }
}
