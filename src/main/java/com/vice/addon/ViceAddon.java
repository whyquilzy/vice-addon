package com.vice.addon;

import com.mojang.logging.LogUtils;
import com.vice.addon.modules.PearlMeta;
import com.vice.addon.modules.ViceAutoTool;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;

public class ViceAddon extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("Vice");

    @Override
    public void onInitialize() {
        LOG.info("Initializing Vice Addon");

        Modules.get().add(new ViceAutoTool());
        Modules.get().add(new PearlMeta());
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
