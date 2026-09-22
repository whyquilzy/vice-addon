package com.vice.addon.hud;

import com.vice.addon.ViceAddon;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * NOTE: This does NOT reproduce the exact region grid/colors/numbering from your
 * screenshot - that grid (EU-C, NA-E, ASIA, etc, numbered like #102) comes from a
 * specific server's own plugin, and I don't have that server's region data or the
 * formula it uses to turn coordinates into region numbers. Faking numbers that
 * looked right would just be wrong information dressed up to look convincing.
 *
 * What this actually does: shows your coordinates and, if you tell it how big a
 * "region" is on your server (region-size) and where region (0,0) starts
 * (origin-x/origin-z), computes a generic column/row grid reference from your
 * current position. If you can tell me the actual size/origin your server uses
 * (or a couple of known coordinate-to-region examples), I can tune the defaults.
 */
public class RegionMapHud extends HudElement {
    public static final HudElementInfo<RegionMapHud> INFO = new HudElementInfo<>(ViceAddon.HUD_GROUP, "region-map", "Shows your coordinates and a computed region grid cell.", RegionMapHud::new);

    private final Setting<Integer> regionSize = settings.getDefaultGroup().add(new IntSetting.Builder()
        .name("region-size")
        .description("Blocks per region, on one axis.")
        .defaultValue(512)
        .min(16)
        .sliderRange(16, 2000)
        .build()
    );

    private final Setting<Integer> originX = settings.getDefaultGroup().add(new IntSetting.Builder()
        .name("origin-x")
        .description("World X coordinate that region column 0 starts at.")
        .defaultValue(0)
        .build()
    );

    private final Setting<Integer> originZ = settings.getDefaultGroup().add(new IntSetting.Builder()
        .name("origin-z")
        .description("World Z coordinate that region row 0 starts at.")
        .defaultValue(0)
        .build()
    );

    private final Setting<SettingColor> textColor = settings.getDefaultGroup().add(new ColorSetting.Builder()
        .name("text-color")
        .defaultValue(new SettingColor(255, 255, 255))
        .build()
    );

    public RegionMapHud() {
        super(INFO);
    }

    @Override
    public void render(HudRenderer renderer) {
        if (mc.player == null) {
            setSize(renderer.textWidth("Region Map", true), renderer.textHeight(true));
            renderer.text("Region Map", x, y, textColor.get(), true);
            return;
        }

        int px = (int) Math.floor(mc.player.getX());
        int pz = (int) Math.floor(mc.player.getZ());

        int col = Math.floorDiv(px - originX.get(), regionSize.get());
        int row = Math.floorDiv(pz - originZ.get(), regionSize.get());

        String line1 = String.format("Region (%d, %d)", col, row);
        String line2 = String.format("X %d  Z %d", px, pz);

        double w = Math.max(renderer.textWidth(line1, true), renderer.textWidth(line2, true));
        double lineH = renderer.textHeight(true);

        setSize(w, lineH * 2);

        renderer.text(line1, x, y, textColor.get(), true);
        renderer.text(line2, x, y + lineH, textColor.get(), true);
    }
}
