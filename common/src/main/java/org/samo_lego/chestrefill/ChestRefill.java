package org.samo_lego.chestrefill;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.samo_lego.chestrefill.storage.LootConfig;

import java.io.File;

public class ChestRefill {
    public static final String MOD_ID = "chestrefill";
    public static final Logger LOGGER = LogManager.getLogger();
    public static final LootConfig config = LootConfig.load(new File(",/config/chest_refill.json"));
    public static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .serializeNulls()
            .create();

    public static void init() {
    }
}
