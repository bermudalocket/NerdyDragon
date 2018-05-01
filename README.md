# NerdyDragon

A plugin to facilitate handling of custom Ender Dragon drops.

## Configuration

* `default-world` - the world in which the dragon fight occurs, typically `WORLD_THE_END`.
* `upon-fail-drop-in-portal` - if for some reason the drops cannot be put directly into the player's inventory (i.e. full inventory, or the player logged out), drop the drops into the portal instead.
* `default-drop-search-radius` - if the portal dropping is not enabled, the plugin will search around the world's spawn point for a safe block on which to drop the drops. This is the radius of that search.

Example custom drop:

```
custom-drops:
  - name: 'Essence of Flight'
    material: GHAST_TEAR
    enchant: ARROW_INFINITE
    drop-rate: 1.0
    drop-qty: 1
    lore:
      - '&rTrade in with &6Fragments of Amber&r at the &3Spawn Museum&r!'
```

Note that `name` must be in single quotes (Minecraft color codes allowed!); `material` must be a Material*; `enchant` must be an Enchantment*, `drop-rate` must be a double in the interval `[0.0, 1.0]`; `drop-qty` must be a positive integer; and `lore` must be a list of strings (Minecraft color codes allowed here too!).

`*` denotes a Bukkit/Spigot/Paper object -- see respective javadocs

## Commands

* `nerdydragon-reload` - reloads the configuration file (no restart necessary!)
* `nerdydragon-list` - prints a list of all currently active custom drops into your chat window
* `nerdydragon-peek` - places in your inventory one of each currently active custom drop (i.e. for color code/formatting verification)
* `nerdydragon-toggle` - toggles the plugin's state to either enabled or soft disabled (i.e. the plugin is not actually disabled and can be re-enabled by running `nerdydragon-toggle` once again, instead of having to restart the server)

## Permissions

* `nerdydragon-admin` - the only permission, gives access to all commands (default group: op)
