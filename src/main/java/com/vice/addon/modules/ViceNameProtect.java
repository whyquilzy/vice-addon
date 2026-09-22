package com.vice.addon.modules;

import com.vice.addon.ViceAddon;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.player.NameProtect;

/**
 * Meteor Client already has a full "name-protect" module (Player category) that
 * hides your real username client-side wherever it appears - tab list, F3, chat,
 * etc - by hooking into the game's text rendering directly with its own mixins.
 * That's real, low-level plumbing; writing a second, separate set of mixins into
 * the exact same vanilla classes from this addon would be redundant at best and
 * could conflict with the one Meteor already installs at worst.
 *
 * So rather than faking a second copy that doesn't actually do anything (or
 * risking one that fights the real one), this module lives in the Vice category
 * for convenience but just configures and toggles Meteor's real name-protect
 * module for you. Turning this on/off, or changing the name here, does the exact
 * same thing as opening the real "name-protect" module directly - you're not
 * missing out on anything by using this one instead.
 */
public class ViceNameProtect extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<String> name = sgGeneral.add(new StringSetting.Builder()
        .name("name")
        .description("The name to show instead of your real username.")
        .defaultValue("seasnail")
        .onChanged(this::push)
        .build()
    );

    private final Setting<Boolean> hideSkin = sgGeneral.add(new BoolSetting.Builder()
        .name("hide-skin")
        .description("Also turns other players into Steve skins (mirrors name-protect's skin-protect).")
        .defaultValue(true)
        .onChanged(this::push)
        .build()
    );

    public ViceNameProtect() {
        super(ViceAddon.CATEGORY, "vice-name-protect", "Choose your protected name. Configures Meteor's built-in name-protect module.");
    }

    @Override
    public void onActivate() {
        push();
        real().enable();
    }

    @Override
    public void onDeactivate() {
        NameProtect real = real();
        if (real.isActive()) real.disable();
    }

    private void push() {
        NameProtect real = real();

        Setting<Boolean> realEnabled = real.settings.get("name-protect", Boolean.class);
        Setting<String> realName = real.settings.get("name", String.class);
        Setting<Boolean> realSkin = real.settings.get("skin-protect", Boolean.class);

        if (realEnabled != null) realEnabled.set(true);
        if (realName != null) realName.set(name.get());
        if (realSkin != null) realSkin.set(hideSkin.get());
    }

    private NameProtect real() {
        return Modules.get().get(NameProtect.class);
    }
}
