package org.samo_lego.chestrefill.storage;

import com.google.gson.annotations.SerializedName;

import java.io.*;
import java.nio.charset.StandardCharsets;

import static org.samo_lego.chestrefill.ChestRefill.*;

public class LootConfig {

    @SerializedName("// Whether to randomize loot table seed.")
    public final String _comment_randomizeLootSeed0 = "";
    @SerializedName("// This ensures that regenerated loot is different each time.")
    public final String _comment_randomizeLootSeed1 = "(default: true)";
    @SerializedName("randomize_loot_seed")
    public boolean randomizeLootSeed = true;

    @SerializedName("// Max refills per container, inclusive. -1 for unlimited")
    public final String _comment_maxRefills = "(default: 5)";
    @SerializedName("max_refills")
    public int maxRefills = 5;

    @SerializedName("// Whether to add loot even if container has some items already.")
    public final String _comment_refillFull = "(default: false)";
    @SerializedName("refill_non_empty")
    public boolean refillFull = false;

    public final String _comment_minWaitTime = "(default: 14400 (=4 hours))";
    @SerializedName("min_wait_time") // in seconds
    public long minWaitTime = 5;

    private File fileLocation;

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

        config.fileLocation = file;

        config.save();

        return config;

    }

    /**
     * Saves the config to the given file.
     */
    public void save() {
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(this.fileLocation), StandardCharsets.UTF_8)) {
            GSON.toJson(this, writer);
        } catch (IOException e) {
            LOGGER.error("Problem occurred when saving config: " + e.getMessage());
        }
    }
}
