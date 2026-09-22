package com.vice.addon.modules;

import com.vice.addon.ViceAddon;
import com.vice.addon.utils.ChunkBlockScanner;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.BlockUpdateEvent;
import meteordevelopment.meteorclient.events.world.ChunkDataEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;

/**
 * Highlights spawners with a box and a vertical line ("beam"), and sends one chat
 * notification the first time each spawner is found (not every render frame).
 */
public class SpawnerFinder extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<SettingColor> color = sgGeneral.add(new ColorSetting.Builder()
        .name("color")
        .description("Highlight and beam color.")
        .defaultValue(new SettingColor(255, 0, 0, 255))
        .build()
    );

    private final Setting<Boolean> notify = sgGeneral.add(new BoolSetting.Builder()
        .name("notify")
        .description("Sends a chat message the first time a spawner is found.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> beamHeight = sgGeneral.add(new IntSetting.Builder()
        .name("beam-height")
        .description("How tall the beam above the spawner is, in blocks.")
        .defaultValue(60)
        .range(0, 320)
        .sliderRange(0, 150)
        .build()
    );

    private final ChunkBlockScanner scanner = new ChunkBlockScanner(state -> state.isOf(Blocks.SPAWNER));
    private final LongSet notified = new LongOpenHashSet();

    public SpawnerFinder() {
        super(ViceAddon.CATEGORY, "spawner-finder", "Highlights spawners and notifies you when one is found.");
    }

    @Override
    public void onActivate() {
        notified.clear();
        scanner.rescanAll();
        checkForNew();
    }

    @Override
    public void onDeactivate() {
        scanner.clear();
        notified.clear();
    }

    @EventHandler
    private void onChunkData(ChunkDataEvent event) {
        scanner.scanChunk(event.chunk());
        checkForNew();
    }

    @EventHandler
    private void onBlockUpdate(BlockUpdateEvent event) {
        scanner.onBlockUpdate(event.pos, event.newState);
        checkForNew();
    }

    private void checkForNew() {
        if (!notify.get()) return;

        LongIterator it = scanner.allPositions().iterator();
        while (it.hasNext()) {
            long key = it.nextLong();
            if (notified.add(key)) {
                BlockPos pos = BlockPos.fromLong(key);
                info("Found a spawner at (highlight)%d, %d, %d(default).", pos.getX(), pos.getY(), pos.getZ());
            }
        }
    }

    @EventHandler
    private void onRender3d(Render3DEvent event) {
        BlockPos.Mutable pos = new BlockPos.Mutable();
        LongIterator it = scanner.allPositions().iterator();

        while (it.hasNext()) {
            pos.set(it.nextLong());
            event.renderer.box(pos, color.get(), color.get(), ShapeMode.Both, 0);

            if (beamHeight.get() > 0) {
                double cx = pos.getX() + 0.5;
                double cz = pos.getZ() + 0.5;
                event.renderer.line(cx, pos.getY() + 1, cz, cx, pos.getY() + 1 + beamHeight.get(), cz, color.get());
            }
        }
    }
}
