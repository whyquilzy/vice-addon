# Vice Addon

Meteor Client addon for Minecraft 1.21.11 (Fabric, Yarn mappings).

## Modules (category: Vice)

- **vice-auto-tool** - when you start breaking a block, switches to the hotbar item that mines it fastest.
  Settings: switch-back, anti-break (+ percentage). Turn off Meteor's built-in `auto-tool` while using it.
- **pearl-meta** - sends `/rtp` in chat when your ender pearl goes under a Y level.
  Settings: trigger mode, y-level (default -4), message (default `/rtp`), cooldown.

## Build

Requires JDK 21.

    ./gradlew build          (Windows: gradlew.bat build)

The jar ends up in `build/libs/` - use `vice-addon-0.1.0.jar` (not the `-sources` one)
and put it in your `mods` folder next to Meteor Client 1.21.11.
