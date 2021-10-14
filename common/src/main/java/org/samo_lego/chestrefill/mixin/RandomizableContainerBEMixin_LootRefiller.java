package org.samo_lego.chestrefill.mixin;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashSet;
import java.util.Set;

import static org.samo_lego.chestrefill.ChestRefill.config;
import static org.samo_lego.chestrefill.PlatformHelper.hasPermission;

@Mixin(RandomizableContainerBlockEntity.class)
public abstract class RandomizableContainerBEMixin_LootRefiller {

    @Shadow @Nullable protected ResourceLocation lootTable;

    @Shadow public abstract void setLootTable(ResourceLocation resourceLocation, long l);

    @Shadow protected abstract NonNullList<ItemStack> getItems();

    @Shadow protected long lootTableSeed;

    @Unique
    private ResourceLocation savedLootTable;
    @Unique
    private long savedLootTableSeed = 0L;
    @Unique
    private final Set<String> lootedUUIDs = new HashSet<>();
    @Unique
    private long lastRefillTime = 0L;

    @Unique
    private int refillCounter = 0;

    @Inject(method = "unpackLootTable", at = @At("HEAD"))
    private void refillLootTable(@Nullable Player player, CallbackInfo ci) {
        boolean empty = this.getItems().stream().allMatch(ItemStack::isEmpty) || config.refillFull;
        if (player != null && this.savedLootTable != null) {
            if(this.lootTable == null) {
                if (empty && this.canRefillFor(player)) {
                    // Refilling for player
                    this.setLootTable(this.savedLootTable, config.randomizeLootSeed ? player.getRandom().nextLong() : this.savedLootTableSeed);
                    this.lastRefillTime = System.currentTimeMillis();
                    ++refillCounter;
                }
            } else {
                // Original loot
                this.lastRefillTime = System.currentTimeMillis();
            }
            this.lootedUUIDs.add(player.getStringUUID());
        }

    }


    @Inject(method = "tryLoadLootTable", at = @At("RETURN"))
    private void onLootTableLoad(CompoundTag compoundTag, CallbackInfoReturnable<Boolean> cir) {
        CompoundTag refillTag = compoundTag.getCompound("ChestRefill");
        if(!refillTag.isEmpty()) {
            // Has been looted already but has saved loot table
            this.lootTable = new ResourceLocation(refillTag.getString("SavedLootTable"));
            this.lootTableSeed = refillTag.getLong("SavedLootTableSeed");

            this.savedLootTable = this.lootTable;
            this.savedLootTableSeed = this.lootTableSeed;
            this.refillCounter = refillTag.getInt("RefillCounter");
            this.lastRefillTime = refillTag.getLong("LastRefillTime");


            ListTag lootedUUIDsTag = (ListTag) refillTag.get("LootedUUIDs");
            if(lootedUUIDsTag != null) {
                lootedUUIDsTag.forEach(tag -> {
                    this.lootedUUIDs.add(tag.getAsString());
                });
            }
            refillTag.put("LootedUUIDs", lootedUUIDsTag);
        } else if(this.lootTable != null) {
            this.savedLootTable = this.lootTable;
            this.savedLootTableSeed = this.lootTableSeed;
        }
    }



    @Inject(method = "trySaveLootTable", at = @At("HEAD"))
    private void onLootTableSave(CompoundTag compoundTag, CallbackInfoReturnable<Boolean> cir) {
        if(this.lootTable == null && this.savedLootTable != null && canStillRefill()) {
            // Save only if chest was looted and can still be refilled
            CompoundTag refillTag = new CompoundTag();

            refillTag.putString("SavedLootTable", this.savedLootTable.toString());
            refillTag.putLong("SavedLootTableSeed", this.savedLootTableSeed);
            refillTag.putInt("RefillCounter", this.refillCounter);
            refillTag.putLong("LastRefillTime", this.lastRefillTime);

            ListTag lootedUUIDsTag = new ListTag();
            this.lootedUUIDs.forEach(uuid -> lootedUUIDsTag.add(StringTag.valueOf(uuid)));
            refillTag.put("LootedUUIDs", lootedUUIDsTag);

            compoundTag.put("ChestRefill", refillTag);
        }
    }

    @Unique
    private boolean canRefillFor(Player player) {
        boolean canReloot = hasPermission(player.createCommandSourceStack(), "chestrefill.allowReloot", config.allowRelootByDefault) || !this.lootedUUIDs.contains(player.getStringUUID());
        System.out.println(player.getName().getString() + " can reloot: " + canReloot);
        return this.canStillRefill() && this.hasEnoughTimePassed() && canReloot;
    }


    /**
     * Whether this container hasn't reached max refills yet.
     * @return true if container can still be refilled, false if refills is more than max refills.
     */
    @Unique
    private boolean canStillRefill() {
        System.out.println(refillCounter + " < " + config.maxRefills);
        return refillCounter < config.maxRefills || config.maxRefills != -1;
    }

    /**
     * Tells whether enough time has passed since previous refill.
     * @return true if container can already be refilled, otherwise false.
     */
    @Unique
    private boolean hasEnoughTimePassed() {
        // * 1000 as seconds are used in config.
        return this.lastRefillTime == 0L || System.currentTimeMillis() - this.lastRefillTime > config.minWaitTime * 1000;
    }
}
