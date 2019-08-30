/*
 * Copyright (c) 2019 bermudalocket. All rights reserved.
 * Unauthorized copying or distribution of this item without permission of the author is prohibited.
 * Proprietary and Confidential
 * Created by bermudalocket on 8/29/2019 at 9:40:21.
 * Last modified 8/29/19, 9:30 PM.
 */
package com.bermudalocket.nerdydragon;

import com.bermudalocket.nerdydragon.tasks.AbsorbProjectileTask;
import com.bermudalocket.nerdydragon.tasks.LeavePortalTask;
import com.bermudalocket.nerdydragon.tasks.RainFireTask;
import com.bermudalocket.nerdydragon.tasks.ReinforcementSpawnTask;
import com.destroystokyo.paper.event.entity.EnderDragonFireballHitEvent;
import com.destroystokyo.paper.event.entity.EnderDragonFlameEvent;
import com.destroystokyo.paper.event.entity.EnderDragonShootFireballEvent;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.DragonFireball;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Shulker;
import org.bukkit.entity.ShulkerBullet;
import org.bukkit.entity.Vex;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EnderDragonChangePhaseEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityUnleashEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;
import java.util.stream.Collectors;

// ------------------------------------------------------------------------
/**
 * Represents an instance of an EnderDragon fight.
 */
public class EnderDragonFight implements Listener {

    /**
     * This fight's unique identifier.
     */
    private final UUID _id;

    /**
     * A reference to the summoned dragon.
     */
    private EnderDragon _dragon;

    /**
     * This fight's current stage.
     */
    private FightStage _stage = FightStage.FIRST;

    /**
     * A timer runnable which facilitates the crystal stage. May be null if
     * the crystal stage is not in progress.
     */
    private CrystalRunnable _crystalRunnable;

    /**
     * The dragon's boss bar.
     */
    private BossBar _bossBar;

    /**
     * The world in which the fight is occurring.
     */
    private final World _world;

    /**
     * The topmost block at the center of the exit portal.
     */
    private Location _center;

    /**
     * The UUID of the last player to damage the dragon. Used for awarding
     * drops if the dragon's killer is null (e.g. if killed with an arrow).
     */
    private UUID _lastDamagedBy;

    /**
     * The unix timestamp recorded at the very beginning of this fight. Used
     * to time the fight.
     */
    final long _timeStarted;

    /**
     * A record mapping from player UUID to the amount of damage they have
     * inflicted on the dragon during this fight.
     */
    private final HashMap<UUID, Double> _attackedBy = new HashMap<>();

    // ------------------------------------------------------------------------
    /**
     * Constructs a new Ender Dragon fight object/instance.
     *
     * @param dragon the newly-spawned dragon which started this fight.
     */
    EnderDragonFight(EnderDragon dragon) {
        _id = UUID.randomUUID();
        NerdyDragon.log("Instantiating new fight with UUID " + _id.toString());
        _dragon = dragon;
        NerdyDragon.log("The dragon has UUID " + dragon.getUniqueId().toString());
        _world = dragon.getWorld();
        _bossBar = dragon.getBossBar();
        _crystalRunnable = new CrystalRunnable(this);

        commonInit();
        Util.tagEntityWithMetadata(_dragon);
        DragonHelper.modifyAttribute(_dragon, Attribute.GENERIC_MAX_HEALTH, 0.75);
        announceStage(FightStage.FIRST);
        _timeStarted = System.currentTimeMillis();
    }

    // ------------------------------------------------------------------------
    /**
     * A series of startup tasks common to both a newly-instantiated fight and
     * one being deserialized.
     */
    private void commonInit() {
        // find portal center
        int y = _world.getHighestBlockYAt(0, 0);
        if (_world.getBlockAt(0, y, 0).getType() == Material.BEDROCK) {
            // found it
            _center = new Location(_world, 0, y, 0);
        } else {
            // iterate down
            for (int newY = y-1; newY >= 6; newY--) {
                if (_world.getBlockAt(0, newY, 0).getType() == Material.BEDROCK) {
                    _center = new Location(_world, 0, newY, 0);
                    break;
                }
            }
        }
        NerdyDragon.log("center = " + _center);
        Bukkit.getPluginManager().registerEvents(this, NerdyDragon.PLUGIN);
        _bossBar.setColor(_stage.BOSS_BAR_COLOR);
        _bossBar.setStyle(BarStyle.SEGMENTED_20);
        setChunkStates(true);
    }

    // ------------------------------------------------------------------------
    /**
     * Set chunks to force-load at the start of a fight, and then undo this
     * at the end of the fight. Prevents the dragon from unloading.
     *
     * @param loaded true to force-load; false to release.
     */
    private void setChunkStates(boolean loaded) {
        for (int i = -4; i <= 4; i++) {
            for (int j = -4; j <= 4; j++) {
                _world.getChunkAt(i, j).setForceLoaded(loaded);
            }
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Saves this fight to config.
     *
     * @param config config.
     */
    void save(ConfigurationSection config) {
        NerdyDragon.log("Serializing fight...");
        config.set("id", _id.toString());
        config.set("world-name", _world.getName());
        config.set("stage", _stage.toString());
        config.set("started-at", _timeStarted);
        config.set("restart-at", System.currentTimeMillis());
        Chunk chunk = _dragon.getLocation().getChunk();
        config.set("dragon-chunk", chunk.getX() + "," + chunk.getZ());
        for (UUID uuid : _attackedBy.keySet()) {
            String uuidString = uuid.toString();
            config.set("attacked-by." + uuidString, _attackedBy.get(uuid));
        }
        if (_stage == FightStage.FIRST) {
            _crystalRunnable.save(config);
        }
        NerdyDragon.PLUGIN.saveConfig();
        NerdyDragon.log("... done!");
    }

    // ------------------------------------------------------------------------
    /**
     * Returns this fight's unique identifier.
     *
     * @return this fight's unique identifier.
     */
    public UUID getUUID() {
        return _id;
    }

    // ------------------------------------------------------------------------
    /**
     * Returns the Ender Dragon.
     *
     * @return the Ender Dragon.
     */
    public EnderDragon getDragon() {
        return _dragon;
    }

    // ------------------------------------------------------------------------
    /**
     * Updates the dragon instance this fight should follow. This is necessary
     * due to some balderdash in the vanilla dragon code which occasionally causes
     * the dragon to completely disappear. The vanilla code handles this by
     * summoning a new dragon and re-assigning it in
     * {@link net.minecraft.server.v1_14_R1.EnderDragonBattle}, so we will do
     * likewise.
     *
     * @param newDragon the new dragon.
     */
    void updateDragon(EnderDragon newDragon) {
        NerdyDragon.log("Dragon switcheroo called.");
        NerdyDragon.log("Old UUID: " + _dragon.getUniqueId());
        NerdyDragon.log("New UUID: " + newDragon.getUniqueId());
        DragonHelper.mergeDragons(newDragon, _dragon);
        _dragon = newDragon;
        Util.tagEntityWithMetadata(newDragon);
        _bossBar = newDragon.getBossBar();
        _bossBar.setColor(_stage.BOSS_BAR_COLOR);
        _bossBar.setStyle(BarStyle.SEGMENTED_20);
    }

    // ------------------------------------------------------------------------
    /**
     * Returns the world in which this fight is taking place.
     *
     * @return the world in which this fight is taking place.
     */
    public World getWorld() {
        return _world;
    }

    // ------------------------------------------------------------------------
    /**
     * Returns the center of the world, i.e. the block above the center
     * portal bedrock.
     *
     * @return the center of the world.
     */
    public Location getCenter() {
        return _center;
    }

    // ------------------------------------------------------------------------
    /**
     * Returns a reference to the crystal runnable, or null if it doesn't exist.
     *
     * @return a reference to the crystal runnable, or null if it doesn't exist.
     */
    public CrystalRunnable getCrystalRunnable() {
        return _crystalRunnable;
    }

    // ------------------------------------------------------------------------
    /**
     * Stops the current fight.
     *
     * @param forced if the fight's end is forced (e.g. by command).
     */
    public void endFight(boolean forced) {
        if (_crystalRunnable != null) {
            _crystalRunnable.stop();
        }
        removeReinforcements(forced);
        _stage = FightStage.FINISHED;
        Thread.newThread(5, () -> {
            setChunkStates(false);
            HandlerList.unregisterAll(this);
            Bukkit.getScheduler().cancelTasks(NerdyDragon.PLUGIN);
            NerdyDragon.FIGHT = null;
        });
    }

    // ------------------------------------------------------------------------
    /**
     * Spawns an entity at the given location and tags it with reinforcement
     * metadata.
     *
     * @param type the entity type.
     * @param location the location.
     */
    public Entity spawnReinforcement(Location location, EntityType type) {
        Entity entity = _world.spawnEntity(location, type);
        Util.tagEntityWithMetadata(entity);
        return entity;
    }

    // ------------------------------------------------------------------------
    /**
     * Removes all reinforcements spawned during this fight.
     *
     * @param forced if the fight's end is being forced (e.g. by command).
     */
    public void removeReinforcements(boolean forced) {
        for (Entity entity : _world.getEntities()) {
            if (!Util.isReinforcement(entity)) {
                continue;
            }
            if (entity instanceof EnderCrystal) {
                if (_crystalRunnable != null) {
                    _crystalRunnable.handleCrystalDeath((EnderCrystal) entity, null);
                }
                entity.remove();
            } else if (entity instanceof EnderDragon) {
                if (forced) {
                    Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "execute in minecraft:the_end run kill @e[type=minecraft:ender_dragon]");
                }
            } else {
                entity.remove();
            }
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Returns true if the player is online, in the same world as the dragon,
     * and within 150 blocks of the world spawn (i.e. dragon fight "arena").
     *
     * @param player the player.
     * @return true if the player is within range.
     */
    boolean inRange(Player player) {
        return player != null && player.isOnline()
                              && player.getWorld() == _world
                              && player.getLocation().distanceSquared(_center) < 180*180*180;
    }

    // ------------------------------------------------------------------------
    /**
     * Returns all players within 80 blocks of the End Portal.
     *
     * @return all players within 80 blocks of the End Portal.
     */
    public HashSet<Player> getNearbyPlayers() {
        return new HashSet<>(_world.getNearbyPlayers(_center, 80));
    }

    // ------------------------------------------------------------------------
    /**
     * Plays a sound at each player's location.
     *
     * @param sound the sound.
     * @param pitch the pitch.
     */
    public void playSound(Sound sound, float pitch) {
        for (Player player : getNearbyPlayers()) {
            player.playSound(player.getLocation(), sound, 3, pitch);
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Returns the current stage.
     *
     * @return the current stage.
     */
    public FightStage getStage() {
        return _stage;
    }

    // ------------------------------------------------------------------------
    /**
     * Sets the current stage.
     */
    void setStage(FightStage stage) {
        if (_stage != stage) {
            _stage = stage;
            if (stage != FightStage.FINISHED) {
                _bossBar.setColor(stage.BOSS_BAR_COLOR);
                announceStage(stage);
            }
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Skips the current stage and immediately progresses to the next.
     */
    public void skipStage() {
        switch (_stage) {
            case FIRST:
                for (EnderCrystal crystal : _crystalRunnable.getCrystals()) {
                    crystal.remove();
                    _crystalRunnable.handleCrystalDeath(crystal, null);
                }
                break;

            case SECOND:
            case THIRD:
                _dragon.setHealth(_stage.DRAGON_HP_LOW_BOUND * DragonHelper.getMaxHealth(_dragon));
                setStage(FightStage.getNext(_stage));
                break;

            case FOURTH:
                endFight(false);
                break;

            default: break;
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Announces the given stage to the players in the fight.
     *
     * @param stage the next stage.
     */
    private void announceStage(FightStage stage) {
        playSound(Sound.ENTITY_ENDER_DRAGON_AMBIENT, 1);
        getNearbyPlayers().forEach(player -> player.sendTitle(stage.DISPLAY_NAME, "", 15, 20 * 4, 25));
    }

    // ------------------------------------------------------------------------
    /**
     * Sends an action bar alert to all nearby players.
     *
     * @param msg the message to send via the action bar.
     */
    private void alertPlayers(String msg) {
        for (Player player : getNearbyPlayers()) {
            player.sendActionBar(ChatColor.WHITE.toString() + msg);
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Records damage dealt by the given player.
     *
     * @param player the player.
     * @param damage the damage.
     */
    private void recordDamage(Player player, double damage) {
        UUID uuid = player.getUniqueId();
        if (_attackedBy.containsKey(uuid)) {
            _attackedBy.put(uuid, damage + _attackedBy.get(uuid));
        } else {
            _attackedBy.put(uuid, damage);
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Prevent reinforcements from picking up items. This will prevent
     * headaches re: picking up player-dropped loot.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityPickupItem(EntityPickupItemEvent e) {
        if (Util.isReinforcement(e.getEntity())) {
            e.setCancelled(true);
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Vexes summoned by reinforcement Evokers get a random admin head, an
     * attack damage nerf, and will be removed automatically after 20-30 seconds
     * to prevent a build up.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent e) {
        if (e.getEntityType() == EntityType.VEX) {
            Vex vex = (Vex) e.getEntity();
            if (Util.isReinforcement(vex.getSummoner())) {
                vex.getEquipment().setHelmet(Util.getRandomAdminHead());
                vex.setHealth(5.0);
                vex.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).addModifier(new AttributeModifier("weak", -0.5, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
                Thread.newThread(20, 30, vex::remove);
            }
        }
    }

    // ------------------------------------------------------------------------
    /**
     * When the dragon dies: reward loot, play sound in mirror worlds, and clean
     * up the fight.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent e) {
        // ignore non-reinforcements
        if (!Util.isReinforcement(e.getEntity())) {
            if (e.getEntityType() == EntityType.ENDER_DRAGON) {
                NerdyDragon.log("Dragon death detected, but this dragon has no fight metadata...");
            }
            return;
        }

        // nerf drops to prevent farming
        if (e.getEntityType() != EntityType.ENDER_DRAGON) {
            if (MathUtil.cdf(0.60)) {
                e.getDrops().clear();
            }
            return;
        }

        // dragon dead, award loot
        e.getDrops().clear();
        Player killer = e.getEntity().getKiller();
        NerdyDragon.log("killer = " + killer);
        Player getsLoot;
        if (killer != null && killer.isOnline() && !killer.isDead()) {
            getsLoot = killer;
        } else {
            Player lastDamager = Bukkit.getPlayer(_lastDamagedBy);
            if (lastDamager != null && lastDamager.isOnline() && !lastDamager.isDead()) {
                getsLoot = lastDamager;
                NerdyDragon.log("getsLoot = last damager " + lastDamager);
            } else {
                getsLoot = null;
                NerdyDragon.log("getsLoot = null, no last damager " + lastDamager);
            }
        }
        if (getsLoot != null) {
            for (ItemStack loot : NerdyDragon.CONFIG.getLoot()) {
                if (getsLoot.getInventory().addItem(loot).size() == 0) {
                    NerdyDragon.message(getsLoot, "Check your inventory for your loot! (" + loot.getAmount() + "x " + loot.getType().toString() + ")");
                } else {
                    String msg = String.format("There's no room in your inventory for your loot so it's been dropped at your feet. %s (%dx %s)",
                        Util.locationToOrderedTriple(getsLoot.getLocation()),
                        loot.getAmount(),
                        loot.getType().toString()
                    );
                    NerdyDragon.message(getsLoot, msg);
                    _world.dropItemNaturally(getsLoot.getLocation(), loot);
                }
            }
        } else {
            NerdyDragon.log("Dragon death: no specific killer found or they're offline or dead. Dropping loot naturally.");
            e.getDrops().clear();
            e.getDrops().addAll(NerdyDragon.CONFIG.getLoot());
        }

        // play death sound in mirrored worlds
        for (World world : NerdyDragon.CONFIG.getMirrorWorlds()) {
            world.playSound(new Location(world, 0, 65, 0), Sound.ENTITY_ENDER_DRAGON_DEATH, 2500, 0.9f);
        }

        // compute and normalize damage ratios
        final HashMap<UUID, Double> damagePercents = new HashMap<>();
        double damageSum = _attackedBy.values()
                                      .stream()
                                      .reduce(Double::sum)
                                      .orElse(DragonHelper.getMaxHealth(_dragon));
        for (UUID uuid : _attackedBy.keySet()) {
            double damage = _attackedBy.get(uuid);
            double ratio = damage / damageSum; // normalize
            damagePercents.put(uuid, ratio);
        }

        // calculate duration and build victory message
        long absoluteDuration = System.currentTimeMillis() - _timeStarted;
        String fightDuration = DurationFormatUtils.formatDuration(absoluteDuration, Util.getHMSFormat(absoluteDuration));
        String adjective = (_attackedBy.size() == 1) ? "warrior" : "warriors";
        String attackers = _attackedBy.keySet().stream()
            .map(Bukkit::getOfflinePlayer)
            .map(p -> String.format("%s%s%s (%.2f%%)", ChatColor.DARK_PURPLE, p.getName(), ChatColor.GRAY, damagePercents.get(p.getUniqueId())))
            .collect(Collectors.joining(", "));
        for (Player player : Bukkit.getOnlinePlayers()) {
            NerdyDragon.message(player, "The dragon has been slain! The valiant " + adjective + " " + attackers + ChatColor.GRAY + " prevailed in " + ChatColor.DARK_PURPLE + fightDuration);
        }

        // record this fight into history
        NerdyDragon.LEADERBOARD.addRecord(this, absoluteDuration, damagePercents);

        // debug
        _attackedBy.forEach((uuid, dmg) -> {
            NerdyDragon.log("[DAMAGE] " + Bukkit.getOfflinePlayer(uuid).getName() + " --> " + dmg);
        });

        // clean up
        endFight(false);
    }

    // ------------------------------------------------------------------------
    /**
     * Don't rain leads because of reinforcement phantoms.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    protected void onEntityUnleash(EntityUnleashEvent e) {
        if (!Util.isReinforcement(e.getEntity()) || e.getReason() != EntityUnleashEvent.UnleashReason.HOLDER_GONE) {
            return;
        }
        final Location pos = e.getEntity().getLocation().clone();
        Thread.newThread(() -> {
            for (Entity entity : pos.getNearbyEntities(3, 6, 3)) {
                if (entity instanceof Item) {
                    Item item = (Item) entity;
                    if (item.getItemStack().getType() == Material.LEAD) {
                        entity.remove();
                    }
                }
            }
        });
    }

    // ------------------------------------------------------------------------
    /**
     * Shulkers fire extra bullets.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onShulkerShoot(ProjectileLaunchEvent e) {
        if (_stage != FightStage.THIRD && _stage != FightStage.FOURTH) {
            return;
        }
        if (e.getEntityType() == EntityType.SHULKER_BULLET && e.getEntity().getShooter() instanceof Shulker) {
            Shulker shulker = (Shulker) e.getEntity().getShooter();
            if (!Util.isReinforcement(shulker)) {
                return;
            }
            // fire 1-3 more shulker bullets
            if (shulker.getTarget() instanceof Player && inRange((Player) shulker.getTarget())) {
                final Location loc = e.getEntity().getLocation().clone();
                Thread.newRepeatedThread(1, 3, 10, () -> {
                    ShulkerBullet bullet = (ShulkerBullet) _world.spawnEntity(loc.add(0, 1, 0), EntityType.SHULKER_BULLET);
                    Util.tagEntityWithMetadata(bullet);
                    bullet.setTarget(shulker.getTarget());
                    Thread.newThread(7, 10, () -> { if (!bullet.isDead()) bullet.remove(); });
                });
            }
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Modify the dragon's breath and spawn a flood of endermites. Only called
     * when the dragon is roosting.
     *
     * @apiNote Requires Paper.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDragonFlame(EnderDragonFlameEvent e) {
        PotionEffectHelper.modifyDragonBreath(e.getAreaEffectCloud(), _stage);
        Thread.newRepeatedThread(12, 18, 3, () -> {
            spawnReinforcement(e.getEntity().getEyeLocation(), EntityType.ENDERMITE);
        });
    }

    // ------------------------------------------------------------------------
    /**
     * Modify the dragon fireball's impact cloud and summon a random number of
     * endermites.
     *
     * @apiNote Requires Paper.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDragonFireballImpact(EnderDragonFireballHitEvent e) {
        AreaEffectCloud dragonBreath = e.getAreaEffectCloud();
        Util.tagEntityWithMetadata(dragonBreath);

        AreaEffectCloud effectCloud = (AreaEffectCloud) spawnReinforcement(dragonBreath.getLocation(), EntityType.AREA_EFFECT_CLOUD);
        PotionEffectHelper.modifyDragonBreath(effectCloud, _stage);
        dragonBreath.setDuration(effectCloud.getDuration());

        if (MathUtil.cdf(0.30)) {
            Thread.newRepeatedThread(1, _stage.MAX_ENDERMITES, 1, () -> {
                this.spawnReinforcement(dragonBreath.getLocation(), EntityType.ENDERMITE);
            });
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Prevent area effect clouds from affecting reinforcements.
     */
    @EventHandler
    public void onAreaEffectCloudApplyEvent(AreaEffectCloudApplyEvent event) {
        event.getAffectedEntities().stream()
            .filter(Util::isReinforcement)
            .forEach(PotionEffectHelper::removePotionEffects);
    }

    // ------------------------------------------------------------------------
    /**
     * @see EnderDragonShootFireballEvent
     * @apiNote Requires Paper.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDragonFireball(EnderDragonShootFireballEvent e) {
        if (_stage == FightStage.FIRST) {
            e.setCancelled(true);
            return;
        }
        Player target = DragonHelper.findRandomTarget(this);
        if (target == null) {
            return;
        }
        Thread.newRepeatedThread(1, _stage.MAX_EXTRA_FIREBALLS, _stage.FIREBALL_TICK_INCREMENT, () -> {
            Location targetLoc = target.getLocation();
            Location dragonLoc = _dragon.getEyeLocation();
            Vector farEnoughAhead = dragonLoc.toVector().clone()
                .add(dragonLoc.getDirection()
                .multiply(4));
            Vector dP = targetLoc.toVector().clone()
                .subtract(farEnoughAhead)
                .normalize()
                .multiply(0.75);
            DragonFireball fireball = _dragon.launchProjectile(DragonFireball.class, dP);
            fireball.setDirection(dP);
        });
    }

    // ------------------------------------------------------------------------
    /**
     * @see EnderDragonChangePhaseEvent
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDragonPhaseChange(EnderDragonChangePhaseEvent e) {
        EnderDragon.Phase phase = e.getNewPhase();
        double rand = MathUtil.nextDouble();
        switch (_stage) {
            case FIRST:
                if (phase != EnderDragon.Phase.CHARGE_PLAYER && phase != EnderDragon.Phase.CIRCLING) {
                    e.setCancelled(true);
                    _dragon.setPhase(EnderDragon.Phase.CIRCLING);
                }
                break;

            case SECOND:
                if (phase == EnderDragon.Phase.FLY_TO_PORTAL) {
                    e.setCancelled(true);
                    if (rand <= 0.30) {
                        _dragon.setPhase(EnderDragon.Phase.CIRCLING);
                    } else if (rand <= 0.80) {
                        DragonHelper.fireballRandomPlayer(this);
                    } else {
                        DragonHelper.chargeRandomPlayer(this);
                    }
                }
                break;

            case THIRD:
                if (phase == EnderDragon.Phase.FLY_TO_PORTAL) {
                    if (rand <= 0.15) {
                        break;
                    }
                    e.setCancelled(true);
                    if (rand <= 0.30) {
                        DragonHelper.chargeRandomPlayer(this);
                    } else {
                        DragonHelper.fireballRandomPlayer(this);
                    }
                }
                break;

            case FOURTH:
                if (phase == EnderDragon.Phase.FLY_TO_PORTAL) {
                    e.setCancelled(true);
                    if (rand <= 0.20) {
                        new RainFireTask(this);
                    } else if (rand <= 0.40) {
                        DragonHelper.chargeRandomPlayer(this);
                    } else if (rand <= 0.90) {
                        DragonHelper.fireballRandomPlayer(this);
                    }
                }
                break;

            default: break;
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent e) {
        if (e.getEntityType() == EntityType.ENDER_DRAGON) {
            EnderDragon dragon = (EnderDragon) e.getEntity();
            double health = dragon.getHealth() - e.getFinalDamage();
            double nextStageAt = _stage.DRAGON_HP_LOW_BOUND * dragon.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();

            NerdyDragon.log("Dragon's health is " + health + ". Next stage at " + nextStageAt);

            if (health <= nextStageAt) {
                FightStage next = FightStage.getNext(_stage);
                setStage(next);
                if (next == FightStage.THIRD) {
                    DragonHelper.modifyAttribute(getDragon(), Attribute.GENERIC_MOVEMENT_SPEED, 0.35);
                    DragonHelper.modifyAttribute(getDragon(), Attribute.GENERIC_ARMOR, 0.35);
                } else if (next == FightStage.FOURTH) {
                    DragonHelper.modifyAttribute(getDragon(), Attribute.GENERIC_MOVEMENT_SPEED, 0.75);
                    DragonHelper.modifyAttribute(getDragon(), Attribute.GENERIC_ARMOR, 0.50);
                }
            }
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Handles reactions and side-effects from damaging the dragon or a crystal.
     * Blocks damage dealt by players not involved in this fight.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
        // don't let the dragon hurt reinforcements
        switch (e.getDamager().getType()) {
            case ENDER_DRAGON:
            case AREA_EFFECT_CLOUD:
            case DRAGON_FIREBALL:
            case FIREBALL:
                if (Util.isReinforcement(e.getEntity())) {
                    e.setCancelled(true);
                    return;
                }
                break;

            default: break;
        }

        // don't bother continuing if the entity is not a crystal or the dragon
        if (e.getEntityType() != EntityType.ENDER_CRYSTAL && e.getEntityType() != EntityType.ENDER_DRAGON) {
            return;
        }

        // find the player involved in the event
        Player player = Util.getPlayerFromDamageEvent(e);
        if (player == null) {
            e.setCancelled(true);
            return;
        }

        // crystals
        if (e.getEntityType() == EntityType.ENDER_CRYSTAL) {
            EnderCrystal crystal = (EnderCrystal) e.getEntity();

            // if it's not the master crystal, don't blow up
            if (crystal != _crystalRunnable.getMasterCrystal()) {
                e.setCancelled(true);

            // otherwise go for it
            } else {
                if (Util.isFlying(player)) {
                    player.sendActionBar(ChatColor.RED.toString() + "The crystal absorbed the impact!");
                    e.setCancelled(true);
                    return;
                }
                _crystalRunnable.handleCrystalDeath(crystal, player);
                alertPlayers(player.getName() + " destroyed a crystal");
            }

        // dragon
        } else if (e.getEntityType() == EntityType.ENDER_DRAGON) {
            // announce damage in title bar, record info about damager
            Entity damager = e.getDamager();

            // nothing else happens in the first stage
            if (_stage == FightStage.FIRST) {
                e.setCancelled(true);
                return;
            }

            // in stage 4, randomly absorb projectiles and/or rain fireballs
            if (_stage == FightStage.FOURTH) {
                if (_dragon.getHealth() >= 10) {
                    if (MathUtil.cdf(0.10)) {
                        new RainFireTask(this);
                    }
                    if (damager instanceof Projectile && MathUtil.cdf(0.33)) {
                        if (new AbsorbProjectileTask(this, e).isAbsorbed()) {
                            return;
                        }
                    }
                }
            }

            double finalDamage = e.getFinalDamage();
            if (_dragon.getHealth() - finalDamage < 0) {
                finalDamage -= Math.abs(_dragon.getHealth() - finalDamage);
            }
            double displayFinalDamage = Math.round(finalDamage * 100.0) / 100.0;
            if (damager instanceof Player) {
                _lastDamagedBy = damager.getUniqueId();
                recordDamage((Player) damager, finalDamage);
                alertPlayers(damager.getName() + " inflicted " + displayFinalDamage + " damage");
            } else if (damager instanceof Projectile) {
                ProjectileSource shooter = ((Projectile) damager).getShooter();
                if (shooter instanceof Player) {
                    Player playerShooter = (Player) shooter;
                    recordDamage(playerShooter, finalDamage);
                    _lastDamagedBy = playerShooter.getUniqueId();
                    alertPlayers(playerShooter.getName() + "'s " + damager.getType().toString() + " inflicted " + displayFinalDamage + " damage");
                }
            } else {
                alertPlayers(damager.getType().toString() + " inflicted " + displayFinalDamage + " damage");
            }

            // if the dragon is dead or about to die, don't do any extra stuff
            if (_dragon.getHealth() - e.getFinalDamage() <= 10) {
                return;
            }

            // if at portal, try to leave early
            if (MathUtil.cdf(_stage.LEAVE_PORTAL_CHANCE)) {
                new LeavePortalTask(this);
            }

            // apply random potion effects
            PotionEffectHelper.applyRandomEffects(player, _stage);

            // try to spawn some reinforcements
            if (MathUtil.cdf(_stage.REINFORCEMENT_CHANCE)) {
                new ReinforcementSpawnTask(this);
            }
        }
    } // onEntityDamageByEntity

    // ------------------------------------------------------------------------
    /**
     * Deserializing constructor.
     */
    EnderDragonFight(ConfigurationSection config) {
        _timeStarted = config.getLong("started-at", 0) + (System.currentTimeMillis() - config.getLong("restart-at", 0));

        String uuidAsString = config.getString("id");
        _id = UUID.fromString(uuidAsString);
        NerdyDragon.log("Instantiating serialized fight with UUID " + _id.toString());

        // find world
        String worldName = config.getString("world-name", "world_the_end");
        World world = Bukkit.getWorld(worldName);
        _world = (world != null) ? world : Util.WORLD_THE_END;
        NerdyDragon.log("--> world = " + _world);

        try {
            String[] dragonLastChunkCoords = config.getString("dragon-chunk","1,1").split(",");
            int chunkX = Integer.valueOf(dragonLastChunkCoords[0]);
            int chunkZ = Integer.valueOf(dragonLastChunkCoords[1]);
            Chunk dragonChunk = _world.getChunkAt(chunkX, chunkZ);
            if (!dragonChunk.isLoaded()) {
                _world.loadChunk(dragonChunk);
                NerdyDragon.log("Loaded last dragon chunk at (" + chunkX + ", " + chunkZ + ")");
            }
        } catch (Exception e) { }

        int radius = 6;
        for (int i = -1*radius; i <= radius; i++) {
            for (int j = -1*radius; j <= radius; j++) {
                Chunk chunk = _world.getChunkAt(i, j);
                if (!chunk.isLoaded()) {
                    _world.loadChunk(i, j);
                    NerdyDragon.log("Loaded chunk at (" + i + ", " + j + ")");
                }
            }
        }

        // find dragon
        NerdyDragon.log("Finding the dragon...");
        boolean foundDragon = false;
        for (Entity entity : _world.getEntities()) {
            if (entity.getType() == EntityType.ENDER_DRAGON) {
                _dragon = (EnderDragon) entity;
                Util.tagEntityWithMetadata(_dragon);
                foundDragon = true;
                break;
            }
        }
        if (!foundDragon) {
            NerdyDragon.log("Couldn't find the dragon :(");
            endFight(true);
            return;
        }

        NerdyDragon.log("The dragon has UUID " + _dragon.getUniqueId().toString());

        // stage
        String stageName = config.getString("stage");
        try {
            _stage = FightStage.valueOf(stageName);
            NerdyDragon.log("We are in stage " + stageName);
        } catch (Exception e) {
            NerdyDragon.log("Couldn't figure out what stage we're in :(");
            endFight(false);
            return;
        }

        // load attacked-by players
        ConfigurationSection attackedBySection = config.getConfigurationSection("attacked-by");
        if (attackedBySection != null) {
            for (String uuidKey : attackedBySection.getKeys(false)) {
                UUID uuid = UUID.fromString(uuidKey);
                double damage = attackedBySection.getDouble(uuidKey, 0);
                _attackedBy.put(uuid, damage);
            }
        }

        _bossBar = _dragon.getBossBar();

        commonInit();

        if (_stage == FightStage.FIRST) {
            NerdyDragon.log("Starting crystal runnable...");
            _crystalRunnable = new CrystalRunnable(this, config.getConfigurationSection("crystal-runnable"));
        }

        NerdyDragon.PLUGIN.getConfig().set("saved-fight", null);
        NerdyDragon.PLUGIN.saveConfig();
    }

}
