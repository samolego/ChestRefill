package org.samo_lego.chestrefill.mixin;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
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

import java.util.Date;
import java.util.List;

import static org.samo_lego.chestrefill.ChestRefill.config;

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
    private List<String> lastLootedUUID;
    @Unique
    private long lastRefillTime = 0L;

    @Unique
    private int refillCounter = 0;

    @Inject(method = "unpackLootTable", at = @At("HEAD"))
    private void refillLootTable(@Nullable Player player, CallbackInfo ci) {
        boolean empty = this.getItems().stream().allMatch(ItemStack::isEmpty) || config.refillFull;
        if(this.lootTable == null && empty && player != null && canRefill()) {
            System.out.println("REFILING " + this.getItems().stream().allMatch(ItemStack::isEmpty));
            // Refilling
            this.setLootTable(this.savedLootTable, config.randomizeLootSeed ? player.getRandom().nextLong() : this.savedLootTableSeed);
            this.lastRefillTime = new Date().getTime();
            ++refillCounter;
        }
    }


    @Inject(method = "tryLoadLootTable", at = @At("RETURN"))
    private void onLootTableLoad(CompoundTag compoundTag, CallbackInfoReturnable<Boolean> cir) {
        CompoundTag refillTag = compoundTag.getCompound("ChestRefill");
        if(!refillTag.isEmpty()) {
            System.out.println("Looted but has saved table");
            // Has been looted already but has saved loot table
            this.lootTable = new ResourceLocation(refillTag.getString("SavedLootTable"));
            this.lootTableSeed = refillTag.getLong("SavedLootTableSeed");

            this.savedLootTable = this.lootTable;
            this.savedLootTableSeed = this.lootTableSeed;
            this.refillCounter = refillTag.getInt("RefillCounter");
            this.lastRefillTime = refillTag.getLong("LastRefillTime");
        } else if(this.lootTable != null) {
            this.savedLootTable = this.lootTable;
            this.savedLootTableSeed = this.lootTableSeed;
        }
    }



    @Inject(method = "trySaveLootTable", at = @At("HEAD"))
    private void onLootTableSave(CompoundTag compoundTag, CallbackInfoReturnable<Boolean> cir) {
        if(this.lootTable == null && this.savedLootTable != null && canRefill()) {
            System.out.println("Saving as it has been looted.");
            // Save only if chest was looted and can still be refilled
            CompoundTag refillTag = new CompoundTag();

            refillTag.putString("SavedLootTable", this.savedLootTable.toString());
            refillTag.putLong("SavedLootTableSeed", this.savedLootTableSeed);
            refillTag.putInt("RefillCounter", this.refillCounter);
            refillTag.putLong("LastRefillTime", this.lastRefillTime);

            compoundTag.put("ChestRefill", refillTag);
        }
    }

    @Unique
    private boolean canRefill() {
        Date now = new Date();
        // * 1000 as seconds are used in config.
        boolean timeDiff = this.lastRefillTime == 0L || now.getTime() - this.lastRefillTime > config.minWaitTime * 1000;

        return (refillCounter < config.maxRefills || config.maxRefills == -1) && timeDiff;
    }
}
