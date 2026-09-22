package com.vice.addon.modules;

import com.vice.addon.ViceAddon;
import com.vice.addon.utils.ChunkBlockScanner;
import it.unimi.dsi.fastutil.longs.LongIterator;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.BlockUpdateEvent;
import meteordevelopment.meteorclient.events.world.ChunkDataEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;

/**
 * Highlights amethyst geode blocks through walls. Only scans chunks you've already
 * loaded (and keeps itself updated as you explore/mine), it doesn't reveal anything
 * your client hasn't actually received from the server.
 */
public class AmethystBypass extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<SettingColor> color = sgGeneral.add(new ColorSetting.Builder()
        .name("color")
        .description("Highlight color.")
        .defaultValue(new SettingColor(186, 85, 255, 100))
        .build()
    );

    private final Setting<SettingColor> lineColor = sgGeneral.add(new ColorSetting.Builder()
        .name("line-color")
        .description("Outline color.")
        .defaultValue(new SettingColor(186, 85, 255, 255))
        .build()
    );

    private final Setting<Boolean> includeShell = sgGeneral.add(new BoolSetting.Builder()
        .name("include-geode-shell")
        .description("Also highlights the calcite/smooth basalt shell around geodes, not just the amethyst itself.")
        .defaultValue(false)
        .build()
    );

    private final ChunkBlockScanner scanner = new ChunkBlockScanner(this::isAmethyst);

    public AmethystBypass() {
        super(ViceAddon.CATEGORY, "amethyst-bypass", "Highlights amethyst geode blocks in light purple.");
    }

    private boolean isAmethyst(BlockState state) {
        if (state.isOf(Blocks.AMETHYST_BLOCK)
            || state.isOf(Blocks.BUDDING_AMETHYST)
            || state.isOf(Blocks.AMETHYST_CLUSTER)
            || state.isOf(Blocks.LARGE_AMETHYST_BUD)
            || state.isOf(Blocks.MEDIUM_AMETHYST_BUD)
            || state.isOf(Blocks.SMALL_AMETHYST_BUD)) return true;

        return includeShell.get() && (state.isOf(Blocks.CALCITE) || state.isOf(Blocks.SMOOTH_BASALT));
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
