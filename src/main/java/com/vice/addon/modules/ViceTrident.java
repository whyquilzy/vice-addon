package com.vice.addon.modules;

import com.vice.addon.ViceAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import static meteordevelopment.meteorclient.utils.Utils.hasEnchantments;

/**
 * Normally, riptide only fires once per click-and-release. This holds right click
 * down for you: every tick it forces the game to release-and-reuse the trident, so
 * you keep getting boosted the whole time you hold right click, instead of having
 * to spam-click it yourself.
 *
 * The server still enforces the normal riptide rules (you need Riptide on the
 * trident, and to be in water or rain unless the server itself allows otherwise) -
 * this just automates the clicking, it doesn't bypass anything server-side.
 */
public class ViceTrident extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> requireWaterOrRain = sgGeneral.add(new BoolSetting.Builder()
        .name("require-water-or-rain")
        .description("Only spams while touching water or rain (riptide won't work otherwise anyway).")
        .defaultValue(true)
        .build()
    );

    public ViceTrident() {
        super(ViceAddon.CATEGORY, "vice-trident", "Hold right click to keep moving with a riptide trident.");
    }

    @Override
    public void onDeactivate() {
        mc.options.useKey.setPressed(false);
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null) return;

        if (!mc.options.useKey.isPressed()) return;

        ItemStack stack = mc.player.getMainHandStack();
        if (!stack.isOf(Items.TRIDENT)) return;
        if (!hasEnchantments(stack, Enchantments.RIPTIDE)) return;

        if (requireWaterOrRain.get() && !mc.player.isTouchingWaterOrRain()) return;

        if (mc.player.isUsingItem()) {
            mc.interactionManager.stopUsingItem(mc.player);
        }
    }
}
