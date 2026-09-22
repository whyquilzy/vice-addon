package com.vice.addon.modules;

import com.vice.addon.ViceAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Freecam;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

/**
 * Meteor's own Freecam just leaves your character standing still while your camera
 * flies off. This module remembers whatever block you were looking at the instant
 * before Freecam turns on, and keeps mining that block the whole time Freecam is
 * active - so your body isn't just standing there doing nothing.
 *
 * You still need Meteor's built-in "freecam" module enabled for the camera part;
 * this only handles the mining. Enable both.
 */
public class BetterFreecam extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> swingHand = sgGeneral.add(new BoolSetting.Builder()
        .name("swing-hand")
        .description("Shows the arm swing animation while mining.")
        .defaultValue(true)
        .build()
    );

    private BlockPos target;
    private boolean wasFreecamActive;

    public BetterFreecam() {
        super(ViceAddon.CATEGORY, "better-freecam", "Keeps mining the block you were looking at when you entered freecam.");
    }

    @Override
    public void onDeactivate() {
        target = null;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        Freecam freecam = Modules.get().get(Freecam.class);
        boolean active = freecam.isActive();

        if (active && !wasFreecamActive) {
            // Freecam was just turned on this tick - grab whatever we were looking at right before.
            target = null;
            if (mc.crosshairTarget instanceof BlockHitResult bhr && mc.crosshairTarget.getType() == HitResult.Type.BLOCK) {
                target = bhr.getBlockPos();
            }
        } else if (!active) {
            target = null;
        }

        wasFreecamActive = active;

        if (active && target != null) {
            if (mc.world.getBlockState(target).isAir()) {
                target = null;
                return;
            }

            BlockUtils.breakBlock(target, swingHand.get());
        }
    }
}
