package com.vice.addon.modules;

import com.vice.addon.ViceAddon;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Simple box ESP for other players, through walls. Meteor's built-in "esp" module
 * already does this (with more render modes, shaders, per-entity-type colors etc.)
 * - this is a much smaller version scoped to just players, kept in this addon so
 * it's grouped with the other Vice modules.
 */
public class ViceEsp extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<SettingColor> color = sgGeneral.add(new ColorSetting.Builder()
        .name("color")
        .description("Box color.")
        .defaultValue(new SettingColor(255, 255, 255, 75))
        .build()
    );

    private final Setting<SettingColor> lineColor = sgGeneral.add(new ColorSetting.Builder()
        .name("line-color")
        .description("Outline color.")
        .defaultValue(new SettingColor(255, 255, 255, 255))
        .build()
    );

    private final Setting<Boolean> ignoreSelf = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-self")
        .description("Doesn't draw a box around you.")
        .defaultValue(true)
        .build()
    );

    public ViceEsp() {
        super(ViceAddon.CATEGORY, "vice-esp", "Highlights other players through walls.");
    }

    @EventHandler
    private void onRender3d(Render3DEvent event) {
        if (mc.world == null) return;

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (ignoreSelf.get() && player == mc.player) continue;

            event.renderer.box(player.getBoundingBox(), color.get(), lineColor.get(), ShapeMode.Both, 0);
        }
    }
}
