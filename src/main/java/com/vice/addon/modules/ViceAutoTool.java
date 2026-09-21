package com.vice.addon.modules;

import com.vice.addon.ViceAddon;
import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;

/**
 * When you start breaking a block, switches to whichever hotbar item mines it the fastest.
 * Named "vice-auto-tool" so it doesn't clash with Meteor's built-in "auto-tool".
 * Don't run both at the same time.
 */
public class ViceAutoTool extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> switchBack = sgGeneral.add(new BoolSetting.Builder()
        .name("switch-back")
        .description("Switches back to the slot you were on when you stop breaking.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> antiBreak = sgGeneral.add(new BoolSetting.Builder()
        .name("anti-break")
        .description("Doesn't switch to tools that are almost broken.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> antiBreakPercent = sgGeneral.add(new IntSetting.Builder()
        .name("anti-break-percentage")
        .description("Tools with this much durability or less left are skipped.")
        .defaultValue(10)
        .range(1, 100)
        .sliderRange(1, 100)
        .visible(antiBreak::get)
        .build()
    );

    private boolean swapped;

    public ViceAutoTool() {
        super(ViceAddon.CATEGORY, "vice-auto-tool", "Switches to the fastest hotbar tool for the block you're breaking.");
    }

    @Override
    public void onDeactivate() {
        if (swapped && switchBack.get()) InvUtils.swapBack();
        swapped = false;
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onStartBreakingBlock(StartBreakingBlockEvent event) {
        if (mc.player == null || mc.world == null || mc.player.isCreative()) return;

        BlockState state = mc.world.getBlockState(event.blockPos);
        if (!BlockUtils.canBreak(event.blockPos, state)) return;

        int currentSlot = mc.player.getInventory().getSelectedSlot();
        int bestSlot = currentSlot;
        double bestScore = score(mc.player.getInventory().getStack(currentSlot), state);

        for (int i = 0; i < 9; i++) {
            if (i == currentSlot) continue;

            double score = score(mc.player.getInventory().getStack(i), state);
            if (score > bestScore) {
                bestScore = score;
                bestSlot = i;
            }
        }

        if (bestSlot != currentSlot) {
            InvUtils.swap(bestSlot, switchBack.get());
            swapped = true;
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!swapped || mc.player == null) return;
        if (mc.options.attackKey.isPressed()) return;

        if (switchBack.get()) InvUtils.swapBack();
        swapped = false;
    }

    /**
     * Higher is better. Returns -1 for items that must not be used
     * (wrong tool for a block that needs one, or nearly broken with anti-break on).
     */
    private double score(ItemStack stack, BlockState state) {
        if (antiBreak.get() && stack.isDamageable()) {
            int left = stack.getMaxDamage() - stack.getDamage();
            if (left <= stack.getMaxDamage() * antiBreakPercent.get() / 100) return -1;
        }

        // Blocks like ores need the right tool tier to drop anything.
        if (state.isToolRequired() && !stack.isSuitableFor(state)) return -1;

        double speed = stack.getMiningSpeedMultiplier(state);

        // Efficiency only applies when the tool is actually faster than bare hands.
        if (speed > 1.0) {
            int efficiency = Utils.getEnchantmentLevel(stack, Enchantments.EFFICIENCY);
            if (efficiency > 0) speed += efficiency * efficiency + 1;
        }

        return speed;
    }
}
