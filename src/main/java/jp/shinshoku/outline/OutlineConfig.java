// SPDX-License-Identifier: MIT
package jp.shinshoku.outline;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;

public final class OutlineConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("shinshoku-outline.json");
    public boolean enabled = true;
    public boolean throughWalls = true;
    public boolean hud = true;
    public int guideDistance = 32;
    public int differenceDecimals = 2;
    public MiningMode mode = MiningMode.TEN;
    public double baseX = Geometry.DEFAULT_BASE_X;

    public static OutlineConfig load() {
        if (Files.exists(FILE)) {
            try (var reader = Files.newBufferedReader(FILE)) {
                OutlineConfig c = GSON.fromJson(reader, OutlineConfig.class);
                if (c != null) {
                    c.guideDistance = Math.max(8, Math.min(128, c.guideDistance));
                    c.differenceDecimals = Math.max(0, Math.min(6, c.differenceDecimals));
                    if (c.mode==null) c.mode=MiningMode.TEN;
                    c.baseX=Geometry.validBaseX(c.baseX)?Geometry.blockCenter(c.baseX):Geometry.DEFAULT_BASE_X;
                    return c;
                }
            } catch (IOException | RuntimeException e) {
                LogManager.getLogger("shinshoku_outline").warn("Cannot load outline settings; using defaults", e);
            }
        }
        return new OutlineConfig();
    }
    public boolean save() {
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(this));
            return true;
        } catch (IOException e) {
            LogManager.getLogger("shinshoku_outline").warn("Cannot save outline settings", e);
            return false;
        }
    }
}
