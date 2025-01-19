package org.samo_lego.chestrefill.storage;

import com.google.gson.annotations.SerializedName;
import org.samo_lego.config2brigadier.IBrigadierConfigurator;
import org.samo_lego.config2brigadier.annotation.BrigadierDescription;
import org.samo_lego.config2brigadier.annotation.BrigadierExcluded;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.samo_lego.chestrefill.ChestRefill.*;

public class LootConfig implements IBrigadierConfigurator {


    public DefaultProperties defaultProperties = new DefaultProperties();
    public static class DefaultProperties {
        @BrigadierDescription(
                value = "Whether to randomize loot table seed.\nThis ensures that regenerated loot is different each time",
                defaultOption = "true"
        )
        @SerializedName("randomize_loot_seed")
        public boolean randomizeLootSeed = true;

        @BrigadierDescription(
                value = "Whether to allow players to reloot containers,\neven if they don't have `chestrefill.allowReloot` permission.",
                defaultOption = "false"
        )
        @SerializedName("allow_reloot_without_permission")
        public boolean allowRelootByDefault = false;

        @BrigadierDescription(
                value = "Max refills per container, inclusive. -1 for unlimited.",
                defaultOption = "5"
        )
        @SerializedName("max_refills")
        public int maxRefills = 5;

        @BrigadierDescription(
                value = "Whether to add loot even if container has some items already.",
                defaultOption = "false"
        )
        @SerializedName("refill_non_empty")
        public boolean refillFull = false;

        @BrigadierDescription(
                value = "Minimum wait time to refill the loot, in seconds.",
                defaultOption = "14400 ( = 4 hours)"
        )
        @SerializedName("min_wait_time")
        public long minWaitTime = 14400;
    }

    @SerializedName("// Map to override above config for certain loot tables only.")
    public final String _comment_lootModifierMap = "";
    public Map<String, DefaultProperties> lootModifierMap = Stream.of(new Object[][] {
                    { "//minecraft:chests/igloo_chest", new DefaultProperties() },
                    { "sample_mod:chests/custom_loot_table", new DefaultProperties() }
            }).collect(Collectors.toMap(data -> (String) data[0], data -> (DefaultProperties) data[1]));

    @BrigadierExcluded
    public transient String fileLocation;

    public static LootConfig load(File file) {
        LootConfig config = null;
        if (file.exists()) {
            try (BufferedReader fileReader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)
            )) {
                config = GSON.fromJson(fileReader, LootConfig.class);
            } catch (IOException e) {
                throw new RuntimeException(MOD_ID + " Problem occurred when trying to load config: ", e);
            }
        }
        if(config == null)
            config = new LootConfig();

        config.fileLocation = file.getAbsolutePath();

        config.save();

        return config;

    }

    /**
     * Saves the config to the given file.
     */
    @Override
    public void save() {
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(this.fileLocation), StandardCharsets.UTF_8)) {
            GSON.toJson(this, writer);
        } catch (IOException e) {
            LOGGER.error("Problem occurred when saving config: " + e.getMessage());
        }
    }
}
