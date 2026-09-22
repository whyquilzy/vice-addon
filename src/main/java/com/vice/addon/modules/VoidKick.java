package com.vice.addon.modules;

import com.vice.addon.ViceAddon;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.ObjectObjectImmutablePair;
import meteordevelopment.meteorclient.events.world.ServerConnectBeginEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.text.Text;

/**
 * When you drop below a set Y level, disconnects you and reconnects to the same
 * server a moment later - handy for resetting your position if you're stuck
 * somewhere below the world instead of taking void damage.
 *
 * Needs a moment on the server list / at least one successful join to know which
 * server to reconnect to; it remembers this itself, it doesn't need Meteor's
 * built-in Auto Reconnect module to be enabled.
 */
public class VoidKick extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> triggerY = sgGeneral.add(new DoubleSetting.Builder()
        .name("y-level")
        .description("Disconnects when you go below this Y.")
        .defaultValue(-4.0)
        .range(-2048, 2048)
        .sliderRange(-64, 64)
        .build()
    );

    private final Setting<Integer> reconnectDelay = sgGeneral.add(new IntSetting.Builder()
        .name("reconnect-delay")
        .description("Ticks to wait before reconnecting (20 ticks = 1 second).")
        .defaultValue(60)
        .range(0, 1200)
        .sliderRange(0, 200)
        .build()
    );

    private Pair<ServerAddress, ServerInfo> lastServer;
    private boolean pending;
    private int ticksLeft;

    public VoidKick() {
        super(ViceAddon.CATEGORY, "void-kick", "Disconnects and reconnects when you drop below a Y level.");
    }

    @Override
    public void onDeactivate() {
        pending = false;
    }

    @EventHandler
    private void onServerConnectBegin(ServerConnectBeginEvent event) {
        lastServer = new ObjectObjectImmutablePair<>(event.address, event.info);
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (pending) {
            if (ticksLeft <= 0) {
                reconnect();
            } else {
                ticksLeft--;
            }
            return;
        }

        if (mc.player == null || mc.world == null) return;
        if (mc.player.getY() >= triggerY.get()) return;
        if (lastServer == null) {
            warning("Can't void-kick yet, no known server to reconnect to.");
            return;
        }

        info("Below y=%.0f, disconnecting...", triggerY.get());
        pending = true;
        ticksLeft = reconnectDelay.get();

        mc.player.networkHandler.onDisconnect(new DisconnectS2CPacket(Text.literal("[Vice] void-kick")));
    }

    private void reconnect() {
        pending = false;
        if (lastServer == null) return;

        ConnectScreen.connect(new TitleScreen(), mc, lastServer.left(), lastServer.right(), false, null);
    }
}
