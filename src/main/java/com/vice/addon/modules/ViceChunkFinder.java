package com.vice.addon.modules;

import com.vice.addon.ViceAddon;
import com.vice.addon.utils.ChunkBlockScanner;
import it.unimi.dsi.fastutil.longs.LongIterator;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.BlockUpdateEvent;
import meteordevelopment.meteorclient.events.world.ChunkDataEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.block.Blocks;

/**
 * Highlights whole chunks (as a column outline) that contain more than a set number
 * of fully-grown amethyst clusters - i.e. chunks worth digging into for a geode farm.
 * "Fully grown" means the actual amethyst cluster block, not the smaller buds.
 */
public class ViceChunkFinder extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> minClusters = sgGeneral.add(new IntSetting.Builder()
        .name("min-clusters")
        .description("Minimum number of fully grown clusters in a chunk before it's highlighted.")
        .defaultValue(4)
        .range(1, 200)
        .sliderRange(1, 32)
        .build()
    );

    private final Setting<SettingColor> color = sgGeneral.add(new ColorSetting.Builder()
        .name("color")
        .description("Chunk outline color.")
        .defaultValue(new SettingColor(186, 85, 255, 255))
        .build()
    );

    private final ChunkBlockScanner scanner = new ChunkBlockScanner(state -> state.isOf(Blocks.AMETHYST_CLUSTER));

    public ViceChunkFinder() {
        super(ViceAddon.CATEGORY, "vice-chunk-finder", "Highlights chunks with lots of fully grown amethyst clusters.");
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
        if (mc.world == null) return;

        int minY = mc.world.getBottomY();
        int maxY = minY + mc.world.getHeight();

        LongIterator it = scanner.chunkKeys().iterator();
        while (it.hasNext()) {
            long key = it.nextLong();
            int chunkX = ChunkBlockScanner.chunkKeyX(key);
            int chunkZ = ChunkBlockScanner.chunkKeyZ(key);

            if (scanner.countInChunk(chunkX, chunkZ) < minClusters.get()) continue;

            double x1 = chunkX * 16.0;
            double z1 = chunkZ * 16.0;

            Color line = color.get();
            Color fill = new Color(line.r, line.g, line.b, 0);

            event.renderer.box(x1, minY, z1, x1 + 16, maxY, z1 + 16, fill, line, ShapeMode.Lines, 0);
        }
    }
}
