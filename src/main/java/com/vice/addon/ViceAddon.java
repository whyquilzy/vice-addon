package com.vice.addon;

import com.mojang.logging.LogUtils;
import com.vice.addon.hud.RegionMapHud;
import com.vice.addon.hud.SpotifyHud;
import com.vice.addon.modules.*;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;

public class ViceAddon extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("Vice");
    public static final HudGroup HUD_GROUP = new HudGroup("Vice");

    @Override
    public void onInitialize() {
        LOG.info("Initializing Vice Addon");

        // Modules
        Modules.get().add(new ViceAutoTool());
        Modules.get().add(new PearlMeta());
        Modules.get().add(new AmethystBypass());
        Modules.get().add(new ViceChunkFinder());
        Modules.get().add(new VoidKick());
        Modules.get().add(new ViceTrident());
        Modules.get().add(new RotatedDeepslateHighlight());
        Modules.get().add(new BetterFreecam());
        Modules.get().add(new AutoFirework());
        Modules.get().add(new SpawnerFinder());
        Modules.get().add(new ViceEsp());
        Modules.get().add(new ViceNameProtect());

        // HUD
        Hud.get().register(SpotifyHud.INFO);
        Hud.get().register(RegionMapHud.INFO);
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    // Must match the root package of the addon, otherwise modules' event handlers won't work.
    @Override
    public String getPackage() {
        return "com.vice.addon";
    }
}
