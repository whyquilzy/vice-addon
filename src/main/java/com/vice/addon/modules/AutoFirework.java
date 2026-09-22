package com.vice.addon.modules;

import com.vice.addon.ViceAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.FireworkRocketItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;

/**
 * While gliding with an elytra, automatically uses a firework rocket from your
 * hotbar/inventory once your fall speed drops below a threshold (i.e. you're
 * about to stall/drop), so you don't have to manually rocket-boost yourself.
 */
public class AutoFirework extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> velocityThreshold = sgGeneral.add(new DoubleSetting.Builder()
        .name("velocity-y-threshold")
        .description("Uses a firework when your vertical velocity drops below this (more negative = falling faster).")
        .defaultValue(-0.6)
        .sliderRange(-2, 0)
        .build()
    );

    private final Setting<Integer> cooldown = sgGeneral.add(new IntSetting.Builder()
        .name("cooldown")
        .description("Minimum ticks between uses.")
        .defaultValue(30)
        .range(0, 200)
        .sliderRange(0, 100)
        .build()
    );

    private final Setting<Boolean> searchInventory = sgGeneral.add(new BoolSetting.Builder()
        .name("search-inventory")
        .description("Also looks in your inventory, not just the hotbar, and quick-swaps a firework in.")
        .defaultValue(true)
        .build()
    );

    private int ticksLeft;

    public AutoFirework() {
        super(ViceAddon.CATEGORY, "auto-firework", "Automatically uses a firework rocket while gliding.");
    }

    @Override
    public void onActivate() {
        ticksLeft = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null) return;
        if (ticksLeft > 0) {
            ticksLeft--;
            return;
        }

        if (!mc.player.isFallFlying()) return;
        if (mc.player.getVelocity().y >= velocityThreshold.get()) return;

        FindItemResult firework = searchInventory.get()
            ? InvUtils.find(this::isFirework)
            : InvUtils.find(this::isFirework, 0, 8);

        if (!firework.found()) return;

        int slot = firework.slot();
        if (!firework.isHotbar()) {
            FindItemResult emptyOrFirework = InvUtils.find(stack -> stack.isEmpty() || isFirework(stack), 0, 8);
            if (!emptyOrFirework.found()) return;

            InvUtils.quickSwap().fromId(emptyOrFirework.slot()).to(firework.slot());
            slot = emptyOrFirework.slot();
        }

        InvUtils.swap(slot, true);
        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        InvUtils.swapBack();

        ticksLeft = cooldown.get();
    }

    private boolean isFirework(ItemStack stack) {
        return stack.getItem() instanceof FireworkRocketItem;
    }
}
