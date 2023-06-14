package org.samo_lego.chestrefill.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import org.samo_lego.chestrefill.PlatformHelper;
import org.samo_lego.chestrefill.storage.LootConfig;

import java.io.File;

import static net.minecraft.commands.Commands.literal;
import static org.samo_lego.chestrefill.ChestRefill.MOD_ID;
import static org.samo_lego.chestrefill.ChestRefill.config;

public class ChestRefillCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralCommandNode<CommandSourceStack> root = dispatcher.register(literal(MOD_ID)
                .requires(src -> PlatformHelper.hasPermission(src, "chestrefill.config", src.hasPermission(4)))
                .then(literal("reload")
                        .requires(src -> PlatformHelper.hasPermission(src, "chestrefill.config.reload", src.hasPermission(4)))
                        .executes(ChestRefillCommand::reloadConfig)
                )
        );
        LiteralCommandNode<CommandSourceStack> edit = literal("edit")
                .requires(src -> PlatformHelper.hasPermission(src, "chestrefill.config.edit", src.hasPermission(4)))
                .build();

        config.generateCommand(edit);
        root.addChild(edit);
    }

    private static int reloadConfig(CommandContext<CommandSourceStack> context) {
        LootConfig newConfig = LootConfig.load(new File(config.fileLocation));
        config.reload(newConfig);
        context.getSource().sendSuccess(() -> Component.translatable("gui.done").append(".").withStyle(ChatFormatting.GREEN), false);
        return 1;
    }
}
