package com.vice.addon.modules;

import com.vice.addon.ViceAddon;
import com.vice.addon.utils.ChunkBlockScanner;
import it.unimi.dsi.fastutil.longs.LongIterator;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.BlockUpdateEvent;
import meteordevelopment.meteorclient.events.world.ChunkDataEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Deepslate is a pillar block: natural/vanilla-generated and player-placed deepslate
 * always ends up with its axis set to Y (the default). Deepslate you find with its
 * axis set to X or Z is not something normal survival play produces, so it's a
 * reasonable "something unusual happened here" marker. This just highlights it -
 * it doesn't tell you *why* it's rotated.
 */
public class RotatedDeepslateHighlight extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<SettingColor> color = sgGeneral.add(new ColorSetting.Builder()
        .name("color")
        .description("Highlight color.")
        .defaultValue(new SettingColor(255, 140, 0, 100))
        .build()
    );

    private final Setting<SettingColor> lineColor = sgGeneral.add(new ColorSetting.Builder()
        .name("line-color")
        .description("Outline color.")
        .defaultValue(new SettingColor(255, 140, 0, 255))
        .build()
    );

    private final ChunkBlockScanner scanner = new ChunkBlockScanner(this::isRotated);

    public RotatedDeepslateHighlight() {
        super(ViceAddon.CATEGORY, "rotated-deepslate-highlight", "Highlights deepslate with a non-default axis.");
    }

    private boolean isRotated(BlockState state) {
        if (!state.isOf(Blocks.DEEPSLATE)) return false;
        if (!state.contains(Properties.AXIS)) return false;

        return state.get(Properties.AXIS) != Direction.Axis.Y;
    }

    @Override
    public void onActivate() {
        scanner.rescanAll();
    }

    @Override
    public void onDeactivate() {
        scanner.clear();
    }

    @EventHandler
    private void onChunkData(ChunkDataEvent event) {
        scanner.scanChunk(event.chunk());
    }

    @EventHandler
    private void onBlockUpdate(BlockUpdateEvent event) {
        scanner.onBlockUpdate(event.pos, event.newState);
    }

    @EventHandler
    private void onRender3d(Render3DEvent event) {
        BlockPos.Mutable pos = new BlockPos.Mutable();
        LongIterator it = scanner.allPositions().iterator();

        while (it.hasNext()) {
            pos.set(it.nextLong());
            event.renderer.box(pos, color.get(), lineColor.get(), ShapeMode.Both, 0);
        }
    }
}
