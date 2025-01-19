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
    private final Set<String> lootedUUIDs = new HashSet<>();

    @Unique
    private ResourceLocation savedLootTable;

    @Unique
    private long savedLootTableSeed, lastRefillTime, minWaitTime;

    @Unique
    private boolean allowRelootByDefault, randomizeLootSeed, refillFull, hadCustomData;

    @Unique
    private int refillCounter, maxRefills;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        this.maxRefills = config.defaultProperties.maxRefills;
        this.refillFull = config.defaultProperties.refillFull;
        this.randomizeLootSeed = config.defaultProperties.randomizeLootSeed;
        this.allowRelootByDefault = config.defaultProperties.allowRelootByDefault;
        this.minWaitTime = config.defaultProperties.minWaitTime;

        this.refillCounter = 0;
        this.lastRefillTime = 0;
        this.savedLootTableSeed = 0L;
        this.hadCustomData = false;
    }

    @Inject(method = "unpackLootTable", at = @At("HEAD"))
    private void refillLootTable(@Nullable Player player, CallbackInfo ci) {
        if (player != null) {
            if (this.lootTable == null && this.savedLootTable != null) {
                boolean empty = this.getItems().stream().allMatch(ItemStack::isEmpty) || this.refillFull;
                if (empty && this.canRefillFor(player)) {
                    this.lootedUUIDs.add(player.getStringUUID());
                    // Refilling for player
                    this.setLootTable(this.savedLootTable, this.randomizeLootSeed ? player.getRandom().nextLong() : this.savedLootTableSeed);
                    this.lastRefillTime = System.currentTimeMillis();
                    ++refillCounter;
                }
            } else {
                // Original loot
                this.lastRefillTime = System.currentTimeMillis();
                this.lootedUUIDs.add(player.getStringUUID());

                if (this.lootTable != null) {
                    this.savedLootTable = this.lootTable;
                    this.savedLootTableSeed = this.lootTableSeed;

                }
            }
        }

    }

    @Inject(method = "tryLoadLootTable", at = @At("RETURN"))
    private void onLootTableLoad(CompoundTag compoundTag, CallbackInfoReturnable<Boolean> cir) {
        CompoundTag refillTag = compoundTag.getCompound("ChestRefill");
        if (!refillTag.isEmpty()) {
            // Has been looted already but has saved loot table
            this.savedLootTable = new ResourceLocation(refillTag.getString("SavedLootTable"));
            this.savedLootTableSeed = refillTag.getLong("SavedLootTableSeed");

            this.refillCounter = refillTag.getInt("RefillCounter");
            this.lastRefillTime = refillTag.getLong("LastRefillTime");


            ListTag lootedUUIDsTag = (ListTag) refillTag.get("LootedUUIDs");
            if(lootedUUIDsTag != null) {
                lootedUUIDsTag.forEach(tag -> this.lootedUUIDs.add(tag.getAsString()));
            }

            // Per loot table customization
            var modifiers = config.lootModifierMap.get(this.savedLootTable.toString());
            if (modifiers == null) {
                modifiers = config.lootModifierMap.get(this.savedLootTable.getPath());
            }

            if(modifiers != null) {
                // This loot table has special values set
                this.randomizeLootSeed = modifiers.randomizeLootSeed;
                this.refillFull = modifiers.refillFull;
                this.allowRelootByDefault = modifiers.allowRelootByDefault;
                this.maxRefills = modifiers.maxRefills;
                this.minWaitTime = modifiers.minWaitTime;
            }

            // Per-chest customization
            CompoundTag customValues = refillTag.getCompound("CustomValues");
            if(!customValues.isEmpty()) {
                this.hadCustomData = true;
                this.randomizeLootSeed = customValues.getBoolean("RandomizeLootSeed");
                this.refillFull = customValues.getBoolean("RefillNonEmpty");
                this.allowRelootByDefault = customValues.getBoolean("AllowReloot");
                this.maxRefills = customValues.getInt("MaxRefills");
                this.minWaitTime = customValues.getLong("MinWaitTime");
            }
        } else if (this.lootTable != null) {
            this.savedLootTable = this.lootTable;
            this.savedLootTableSeed = this.lootTableSeed;
        }
    }

    @Inject(method = "trySaveLootTable", at = @At("HEAD"))
    private void onLootTableSave(CompoundTag compoundTag, CallbackInfoReturnable<Boolean> cir) {
        if (this.lootTable == null && this.savedLootTable != null) {
            // Save only if chest was looted (if there's no more original loot table)
            CompoundTag refillTag = new CompoundTag();

            refillTag.putString("SavedLootTable", this.savedLootTable.toString());
            refillTag.putLong("SavedLootTableSeed", this.savedLootTableSeed);
            refillTag.putInt("RefillCounter", this.refillCounter);
            refillTag.putLong("LastRefillTime", this.lastRefillTime);

            ListTag lootedUUIDsTag = new ListTag();
            this.lootedUUIDs.forEach(uuid -> lootedUUIDsTag.add(StringTag.valueOf(uuid)));
            refillTag.put("LootedUUIDs", lootedUUIDsTag);

            // Allows per-chest customization
            if (this.hadCustomData) {
                CompoundTag customValues = new CompoundTag();

                customValues.putBoolean("RandomizeLootSeed", this.randomizeLootSeed);
                customValues.putBoolean("RefillNonEmpty", this.refillFull);
                customValues.putBoolean("AllowReloot", this.allowRelootByDefault);
                customValues.putInt("MaxRefills", this.maxRefills);
                customValues.putLong("MinWaitTime", this.minWaitTime);
                refillTag.put("CustomValues", customValues);
            }

            compoundTag.put("ChestRefill", refillTag);
        }
    }

    /**
     * Whether container can be refilled for given player.
     * @param player player to check refilling for.
     * @return true if refilling can happen, otherwise false.
     */
    @Unique
    private boolean canRefillFor(Player player) {
        boolean relootPermission = hasPermission(player.createCommandSourceStack(), "chestrefill.allowReloot", this.allowRelootByDefault) || !this.lootedUUIDs.contains(player.getStringUUID());
        return this.canStillRefill() && this.hasEnoughTimePassed() && relootPermission;
    }


    /**
     * Whether this container hasn't reached max refills yet.
     * @return true if container can still be refilled, false if refills is more than max refills.
     */
    @Unique
    private boolean canStillRefill() {
        return this.refillCounter < this.maxRefills || this.maxRefills == -1;
    }

    /**
     * Tells whether enough time has passed since previous refill.
     * @return true if container can already be refilled, otherwise false.
     */
    @Unique
    private boolean hasEnoughTimePassed() {
        // * 1000 as seconds are used in config.
        return System.currentTimeMillis() - this.lastRefillTime > this.minWaitTime * 1000;
    }
}
