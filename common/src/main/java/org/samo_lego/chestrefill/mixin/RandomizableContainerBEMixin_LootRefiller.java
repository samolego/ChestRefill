package org.samo_lego.chestrefill.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.samo_lego.chestrefill.PlatformHelper;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;
import java.util.Set;

import static org.samo_lego.chestrefill.ChestRefill.config;


/**
 * <b>RandomizableContainerBEMixin_LootRefiller</b> is a mixin class that extends {@link BaseContainerBlockEntity} and
 * implements the {@link RandomizableContainer} interface for
 * BlockEntities (BEs) that can have loot tables and can be refilled with loot.
 * It provides methods to refill the container's loot table, save and load the loot table, and modify the refill behavior of the container.
 * The class relies on a configuration file to customize the refill behavior of each specific loot table and each container.
 * This class should not be instantiated directly, instead, it should be mixed into specific BE classes that implement the RandomizableContainer interface.
 * <p>
 * This class uses mixin annotations to redirect and inject methods from the original class.
 */
@SuppressWarnings("AddedMixinMembersNamePattern")
@Mixin(value = RandomizableContainerBlockEntity.class, remap = false)
public abstract class RandomizableContainerBEMixin_LootRefiller extends BaseContainerBlockEntity implements RandomizableContainer {
    protected RandomizableContainerBEMixin_LootRefiller(
            BlockEntityType<?> type,
            @NotNull BlockPos pos,
            BlockState blockState
           ) {
        super(type, pos, blockState);
    }



// =-=-=-=-= Shadows =-=-=-=-=
    @Shadow
    @Nullable
    protected ResourceKey<LootTable> lootTable;

    @Shadow
    protected long lootTableSeed;

    @Shadow
    public abstract void setLootTable(ResourceKey<LootTable> resourceKey);

    @Shadow
    public abstract void setLootTableSeed(long l);



// =-=-=-=-= Unique Vars =-=-=-=-=
    @Unique
    private ResourceKey<LootTable> savedLootTable;

    @Unique
    private final Set<String> lootedUUIDs = new HashSet<>();

    @Unique
    private long savedLootTableSeed, lastRefillTime, minWaitTime;

    @Unique
    private boolean allowRelootByDefault, randomizeLootSeed, refillFull, hadCustomData;

    @Unique
    private int refillCounter, maxRefills;



// =-=-=-=-= Injections/Overrides =-=-=-=-=
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

    public void unpackLootTable(@Nullable Player player) {
        if (player != null) {
            if (this.lootTable == null && this.savedLootTable != null) {
                this.refillLootTable(player);
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

        RandomizableContainer.super.unpackLootTable(player);
    }

    public boolean tryLoadLootTable(@NotNull CompoundTag compoundTag) {
        CompoundTag refillTag = compoundTag.getCompound("ChestRefill");

        if (!refillTag.isEmpty()) {
            this.loadRefillTags(refillTag);
        } else if (this.lootTable != null) {
            this.savedLootTable = this.lootTable;
            this.savedLootTableSeed = this.lootTableSeed;
        }

        return RandomizableContainer.super.tryLoadLootTable(compoundTag);
    }

    public boolean trySaveLootTable(@NotNull CompoundTag compoundTag) {
        if (this.lootTable == null && this.savedLootTable != null) {
            this.saveRefillTags(compoundTag);
        }

        return RandomizableContainer.super.trySaveLootTable(compoundTag);
    }



// =-=-=-=-= Unique Checker Methods =-=-=-=-=
    /**
     * Whether container can be refilled for given player.
     * <p>
     * Checks for Player permissions,
     * wether the storage can be refilled
     * and whether enough time has passed since last refill.
     *
     * @param player player to check refilling for.
     * @return <code>true</code> if all checks succeed, otherwise <code>false</code>.
     * @see RandomizableContainerBEMixin_LootRefiller#canStillRefill()
     * @see RandomizableContainerBEMixin_LootRefiller#hasPermission(Player)
     * @see RandomizableContainerBEMixin_LootRefiller#hasEnoughTimePassed()
     * @see RandomizableContainerBEMixin_LootRefiller#refillLootTable(Player)
     */
    @Unique
    private boolean canRefillFor(@NotNull Player player) {
        return this.canStillRefill() && this.hasEnoughTimePassed() && this.hasPermission(player);
    }

    /**
     * Whether a player has permission to reloot from this storage.
     * @param player Player opening the storage
     * @return <code>true</code> if the player has the <code>chestrefill.allowReloot</code> permission node or hasn't looted this storage. otherwise <code>false</code>.
     */
    @Unique
    private boolean hasPermission(@NotNull Player player) {
        return PlatformHelper.hasPermission(
                player.createCommandSourceStack(),
                "chestrefill.allowReloot",
                this.allowRelootByDefault) || !this.lootedUUIDs.contains(player.getStringUUID()
        );
    }

    /**
     * Whether this container hasn't reached max refills yet.
     * @return <code>true</code> if container can still be refilled, <code>false</code> if refills is more than max refills.
     */
    @Unique
    private boolean canStillRefill() {
        return this.refillCounter < this.maxRefills || this.maxRefills == -1;
    }

    /**
     * Tells whether enough time has passed since previous refill.
     * @return <code>true</code> if container can already be refilled, otherwise <code>false</code>.
     */
    @Unique
    private boolean hasEnoughTimePassed() {
        // * 1000 as seconds are used in config.
        return System.currentTimeMillis() - this.lastRefillTime > this.minWaitTime * 1000;
    }



// =-=-=-=-= Unique ChestRefill Methods =-=-=-=-=
    /**
     * Refills the loot table of the container.
     * <p>
     * Only refills if: <ul>
     *     <li> The container is empty or can be refilled while full </li>
     *     <li> The container can be filled for the player </li>
     * </ul>
     *
     *
     * @param player The player opening the container.
     * @see RandomizableContainerBEMixin_LootRefiller#canRefillFor(Player)
     * @see RandomizableContainerBEMixin_LootRefiller#unpackLootTable(Player)
     */
    @Unique
    private void refillLootTable(@NotNull Player player) {
        boolean empty = super.isEmpty() || this.refillFull;
        if (empty && this.canRefillFor(player)) {
            this.lootedUUIDs.add(player.getStringUUID());
            // Refilling for player
            this.setLootTable(this.savedLootTable);
            this.setLootTableSeed(this.randomizeLootSeed ? player.getRandom().nextLong() : this.savedLootTableSeed);
            this.lastRefillTime = System.currentTimeMillis();
            ++refillCounter;
        }
    }

    /**
     * Loads the refilling options from the given compound tag and performs various operations based on the tag contents and configs.
     *
     * @param refillTag The compound tag containing the refilling options.
     */
    @Unique
    private void loadRefillTags(@NotNull CompoundTag refillTag) {
        // Has been looted already but has saved loot table
        this.savedLootTable = ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.parse(refillTag.getString("SavedLootTable")));
        this.savedLootTableSeed = refillTag.getLong("SavedLootTableSeed");

        this.refillCounter = refillTag.getInt("RefillCounter");
        this.lastRefillTime = refillTag.getLong("LastRefillTime");

        ListTag lootedUUIDsTag = (ListTag) refillTag.get("LootedUUIDs");
        if(lootedUUIDsTag != null) {
            lootedUUIDsTag.forEach(tag -> this.lootedUUIDs.add(tag.getAsString()));
        }

        // Per loot table customization
        var modifiers = config.lootModifierMap.get(this.savedLootTable.location().toString());
        if (modifiers == null) {
            modifiers = config.lootModifierMap.get(this.savedLootTable.registry().getPath());
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
    }

    /**
     * Saves the refill options to a CompoundTag.
     *
     * @param compoundTag The CompoundTag to save the refill options to.
     */
    @Unique
    private void saveRefillTags(@NotNull CompoundTag compoundTag) {
        // Save only if chest was looted (if there's no more original loot table)
        CompoundTag refillTag = new CompoundTag();

        refillTag.putString("SavedLootTable", this.savedLootTable.location().toString());
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
