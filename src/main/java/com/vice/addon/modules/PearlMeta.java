package com.vice.addon.modules;

import com.vice.addon.ViceAddon;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;

import java.util.HashSet;
import java.util.Set;

/**
 * When one of YOUR ender pearls goes under the configured Y level, sends /rtp in chat.
 */
public class PearlMeta extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<TriggerMode> mode = sgGeneral.add(new EnumSetting.Builder<TriggerMode>()
        .name("trigger")
        .description("PearlBelowY: fires when your pearl in the air goes below the Y level. PlayerBelowY: fires when you throw a pearl while you are below the Y level.")
        .defaultValue(TriggerMode.PearlBelowY)
        .build()
    );

    private final Setting<Double> triggerY = sgGeneral.add(new DoubleSetting.Builder()
        .name("y-level")
        .description("The Y level to trigger under.")
        .defaultValue(-4.0)
        .range(-2048, 2048)
        .sliderRange(-64, 320)
        .build()
    );

    private final Setting<String> message = sgGeneral.add(new StringSetting.Builder()
        .name("message")
        .description("What gets sent in chat.")
        .defaultValue("/rtp")
        .build()
    );

    private final Setting<Integer> cooldown = sgGeneral.add(new IntSetting.Builder()
        .name("cooldown")
        .description("Minimum ticks between messages (20 ticks = 1 second).")
        .defaultValue(100)
        .range(0, 1200)
        .sliderRange(0, 400)
        .build()
    );

    // Pearls we have already looked at / already fired for (by entity id).
    private final Set<Integer> seen = new HashSet<>();
    private final Set<Integer> triggered = new HashSet<>();
    private int cooldownTicks;

    public PearlMeta() {
        super(ViceAddon.CATEGORY, "pearl-meta", "Sends /rtp automatically when you throw a pearl below a set Y level.");
    }

    @Override
    public void onActivate() {
        reset();
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        reset();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.world == null) return;

        if (cooldownTicks > 0) cooldownTicks--;

        Set<Integer> present = new HashSet<>();

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof EnderPearlEntity pearl)) continue;
            if (pearl.getOwner() != mc.player) continue;

            int id = pearl.getId();
            present.add(id);
            boolean isNew = seen.add(id);

            boolean hit = switch (mode.get()) {
                case PearlBelowY -> pearl.getY() < triggerY.get();
                case PlayerBelowY -> isNew && mc.player.getY() < triggerY.get();
            };

            if (hit && cooldownTicks <= 0 && triggered.add(id)) {
                ChatUtils.sendPlayerMsg(message.get());
                cooldownTicks = cooldown.get();
            }
        }

        // Forget pearls that no longer exist.
        seen.retainAll(present);
        triggered.retainAll(present);
    }

    private void reset() {
        seen.clear();
        triggered.clear();
        cooldownTicks = 0;
    }

    public enum TriggerMode {
        PearlBelowY,
        PlayerBelowY
    }
}
