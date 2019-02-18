package com.bermudalocket.nerdydragon;

import com.bermudalocket.nerdydragon.tasks.AbsorbProjectileTask;
import com.bermudalocket.nerdydragon.tasks.LeavePortalTask;
import com.bermudalocket.nerdydragon.tasks.RainFireTask;
import com.bermudalocket.nerdydragon.tasks.ReinforcementSpawnTask;
import com.destroystokyo.paper.event.entity.EnderDragonFireballHitEvent;
import com.destroystokyo.paper.event.entity.EnderDragonFlameEvent;
import com.destroystokyo.paper.event.entity.EnderDragonShootFireballEvent;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
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
import org.bukkit.entity.LivingEntity;
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
import org.bukkit.event.entity.EntityUnleashEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.UUID;
import java.util.stream.Collectors;

public class EnderDragonFight implements Listener {

    private final UUID _id;

    private EnderDragon _dragon;

    private FightStage _stage = FightStage.FIRST;

    private CrystalRunnable _crystalRunnable;

    private BossBar _bossBar;

    private final World _world;

    private Location _center;

    private UUID _lastDamagedBy;

    private long _commencedTimestamp;

    private final HashMap<UUID, Double> _attackedBy = new HashMap<>();

    private final HashMap<UUID, PlayerData> _playerData = new HashMap<>();

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
        _bossBar = dragon.getBossBar(); // paper
        _crystalRunnable = new CrystalRunnable(this);

        commonInit();
        Util.tagEntityWithMetadata(_dragon);
        DragonHelper.modifyAttribute(_dragon, Attribute.GENERIC_MAX_HEALTH, 0.75);
        announceStage(FightStage.FIRST);
        _commencedTimestamp = System.currentTimeMillis();
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
        config.set("started-at", _commencedTimestamp);
        config.set("restart-at", System.currentTimeMillis());
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
     * {@link net.minecraft.server.v1_13_R2.EnderDragonBattle}, so we will do
     * likewise.
     *
     * @param newDragon the new dragon.
     */
    void updateDragon(EnderDragon newDragon) {
        _dragon = newDragon;
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

    public ImmutableSet<OfflinePlayer> getAttackers() {
        return ImmutableSet.copyOf(_attackedBy.keySet()
                                              .stream()
                                              .map(Bukkit::getOfflinePlayer)
                                              .collect(Collectors.toSet()));
    }

    // TODO
    // ------------------------------------------------------------------------
    /**
     * Returns the given player's PlayerData.
     *
     * @param player the player.
     * @return the player's PlayerData.
     */
    public PlayerData getPlayerData(Player player) {
        UUID uuid = player.getUniqueId();
        PlayerData data = _playerData.get(uuid);
        return data == null ? _playerData.put(uuid, new PlayerData(player)) : data;
    }

    // ------------------------------------------------------------------------
    /**
     * Stops the current fight.
     *
     * @param forced if the fight's end is forced (e.g. by command).
     */
    public void endFight(boolean forced) {
        if (_crystalRunnable != null) {
            _crystalRunnable.stop(); // just in case they didn't get past stage 1
        }
        removeReinforcements(forced);
        _stage = FightStage.FINISHED;
        Thread.newThread(5, () -> {
            HandlerList.unregisterAll(this);
            Bukkit.getScheduler().cancelTasks(NerdyDragon.PLUGIN);
            NerdyDragon.clearCurrentFight();
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
            if (Util.isReinforcement(entity)) {
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
    }

    // ------------------------------------------------------------------------
    /**
     * Returns true if the player is online, in the same world as the dragon,
     * and within 150 blocks of the world spawn (i.e. dragon fight "arena").
     *
     * @param player the player.
     * @return true if the player is within range.
     */
    public boolean inRange(Player player) {
        return player != null && player.isOnline()
                              && player.getWorld() == _world
                              && player.getLocation().distanceSquared(_center) < 180*180*180;
    }

    // ------------------------------------------------------------------------
    /**
     * Returns a set of EnderCrystals currently being tracked by this plugin.
     *
     * @return a set of currently-active crystals.
     */
    public ImmutableSet<EnderCrystal> getCrystals() {
        return _crystalRunnable != null ? _crystalRunnable.getCrystals()
                                        : ImmutableSet.of();
    }

    // ------------------------------------------------------------------------
    /**
     * Returns true if the crystal runnable is running.
     *
     * @return true if the crystal runnable is running.
     */
    public LinkedHashSet<String> getRunnableStates() {
        boolean crystals = _crystalRunnable != null && _crystalRunnable.isRunning();
        return new LinkedHashSet<>(Collections.singletonList(
            "1. Crystal runnable is " + ((crystals) ? "running" : "stopped")
        ));
    }

    // ------------------------------------------------------------------------
    /**
     * Returns all players within 180 blocks of the End Portal.
     *
     * @return all players within 180 blocks of the End Portal.
     */
    public HashSet<Player> getNearbyPlayers() {
        return new HashSet<>(_world.getNearbyPlayers(_center, 180));
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
    public void setStage(FightStage stage) {
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
        _world.playSound(Util.END_SPAWN, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 10, 1);
        getNearbyPlayers().forEach(player -> player.sendTitle(stage.DISPLAY_NAME, "", 15, 20 * 4, 25));
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
            for (ItemStack loot : Configuration.getLoot()) {
                if (getsLoot.getInventory().addItem(loot).size() == 0) {
                    NerdyDragon.message(getsLoot, "Check your inventory for your loot! (" + loot.getAmount() + "x " + loot.getType().toString() + ")");
                } else {
                    NerdyDragon.message(getsLoot, "There's no room in your inventory for your loot so it's been dropped at your feet. " + Util.locationToOrderedTriple(getsLoot.getLocation()) + " (" + loot.getAmount() + "x " + loot.getType().toString() + ")");
                    _world.dropItemNaturally(getsLoot.getLocation(), loot);
                }
            }
        } else {
            NerdyDragon.log("Dragon death: no specific killer found or they're offline or dead. Dropping loot naturally.");
            e.getDrops().clear();
            e.getDrops().addAll(Configuration.getLoot());
        }

        // play death sound in mirrored worlds
        for (World world : Configuration.getMirrorWorlds()) {
            world.playSound(new Location(world, 0, 65, 0), Sound.ENTITY_ENDER_DRAGON_DEATH, 2500, 0.9f);
        }

        String fightDuration = DurationFormatUtils.formatDuration(System.currentTimeMillis() - _commencedTimestamp, "H:mm:ss");

        //String attackers = _playerData.values()
        String attackers = _attackedBy.keySet()
                                      .stream()
                                      .map(Bukkit::getOfflinePlayer)
                                      .map(p -> ChatColor.DARK_PURPLE + p.getName() + ChatColor.GRAY + " (" + getDamageRatio(_attackedBy.get(p.getUniqueId())) + "%)")
                                      //.map(playerData -> ChatColor.DARK_PURPLE + playerData.getName() + ChatColor.GRAY + " (" + getDamageRatio(playerData.getDamageInflicted()) + "%, " + playerData.getDeaths() + " deaths)")
                                      .collect(Collectors.joining(", "));
        for (Player player : Bukkit.getOnlinePlayers()) {
            String adjective = (getAttackers().size() == 1) ? "warrior" : "warriors";
            NerdyDragon.message(player, "The dragon has been slain! The valiant " + adjective + " " + attackers + ChatColor.GRAY + " prevailed in " + ChatColor.DARK_PURPLE + fightDuration);
        }

        // debug
        _attackedBy.forEach((uuid, dmg) -> {
            NerdyDragon.log("[DAMAGE] " + Bukkit.getOfflinePlayer(uuid).getName() + " --> " + dmg);
        });

        // clean up
        endFight(false);
    }

    private double getDamageRatio(double damage) {
        if (damage == 0) {
            return 0;
        }
        double ratio = 100 * (damage / DragonHelper.getMaxHealth(_dragon));
        return ratio >= 100 ? 100 : Math.round(ratio*100.0)/100.0;
    }

    // ------------------------------------------------------------------------
    /**
     * Don't rain leads because of reinforcement phantoms.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    protected void onEntityUnleash(EntityUnleashEvent e) {
        if (Util.isReinforcement(e.getEntity())) {
            if (e.getReason() == EntityUnleashEvent.UnleashReason.HOLDER_GONE) {
                final Location pos = e.getEntity().getLocation().clone();
                Thread.newThread(() -> {
                    for (Entity entity : pos.getNearbyEntities(10, 10, 10)) { // 5x5x5 box
                        if (entity instanceof Item) {
                            Item item = (Item) entity;
                            if (item.getItemStack().getType() == Material.LEAD) {
                                entity.remove();
                            }
                        }
                    }
                });
            }
        }
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
        for (LivingEntity victim : event.getAffectedEntities()) {
            if (Util.isReinforcement(victim)) {
                victim.getActivePotionEffects()
                      .stream()
                      .map(PotionEffect::getType)
                      .forEach(victim::removePotionEffect);
            }
        }
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
                } else if (next == FightStage.FINISHED) {
                    endFight(false);
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

        if (e.getEntityType() == EntityType.ENDER_CRYSTAL) {
            EnderCrystal crystal = (EnderCrystal) e.getEntity();
            if (crystal != _crystalRunnable.getMasterCrystal()) {
                e.setCancelled(true);
            } else {
                if (Util.isFlying(player)) {
                    player.sendActionBar(ChatColor.RED.toString() + "The crystal absorbed the impact!");
                    e.setCancelled(true);
                    return;
                }
                _crystalRunnable.handleCrystalDeath(crystal, player);
                alertPlayers(player.getName() + " destroyed a crystal");
            }
        } else if (e.getEntityType() == EntityType.ENDER_DRAGON) {
            // announce damage in title bar, record info about damager
            Entity damager = e.getDamager();

            double finalDamage = e.getFinalDamage();
            if (_dragon.getHealth() - finalDamage < 0) {
                finalDamage -= Math.abs(_dragon.getHealth() - finalDamage);
            }
            finalDamage = MathUtil.round(finalDamage * 100.0) / 100.0;
            if (damager instanceof Player) {
                _lastDamagedBy = damager.getUniqueId();
                recordDamage((Player) damager, e.getFinalDamage());
                alertPlayers(damager.getName() + " inflicted " + finalDamage + " damage");
            } else if (damager instanceof Projectile) {
                ProjectileSource shooter = ((Projectile) damager).getShooter();
                if (shooter instanceof Player) {
                    Player playerShooter = (Player) shooter;
                    recordDamage(playerShooter, e.getFinalDamage());
                    _lastDamagedBy = playerShooter.getUniqueId();
                    alertPlayers(playerShooter.getName() + "'s " + damager.getType().toString() + " inflicted " + finalDamage + " damage");
                }
            } else {
                alertPlayers(damager.getType().toString() + " inflicted " + finalDamage + " damage");
            }

            // if the dragon is dead or about to die, don't do any extra stuff
            if (_dragon.getHealth() - e.getFinalDamage() <= 10) {
                return;
            }

            // nothing else happens in the first stage
            if (_stage == FightStage.FIRST) {
                e.setCancelled(true);
                return;
            }

            // in stage 4, randomly absorb projectiles and/or rain fireballs
            if (_stage == FightStage.FOURTH) {
                if (damager instanceof Projectile && MathUtil.cdf(Configuration.DRAGON_ABSORB_PROJECTILE_CHANCE)) {
                    new AbsorbProjectileTask(this, e);
                }
                if (MathUtil.cdf(0.10)) {
                    new RainFireTask(this);
                }
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

    EnderDragonFight(ConfigurationSection config) {
        final long loadedAt = System.currentTimeMillis();
        _commencedTimestamp = config.getLong("started-at", 0);
        _commencedTimestamp += loadedAt - config.getLong("restart-at", 0);

        String uuidAsString = config.getString("id");
        _id = UUID.fromString(uuidAsString);

        NerdyDragon.log("Instantiating serialized fight with UUID " + _id.toString());

        // find world
        String worldName = config.getString("world-name", "world_the_end");
        World world = Bukkit.getWorld(worldName);
        _world = (world != null) ? world : Util.WORLD_THE_END;

        NerdyDragon.log("--> world = " + _world);

        int radius = 6;
        for (int i = -1*radius; i <= radius; i++) {
            for (int j = -1*radius; j <= radius; j++) {
                Chunk chunk = _world.getChunkAt(i, j);
                if (!chunk.isLoaded()) {
                    _world.loadChunk(i, j);
                    _world.setChunkForceLoaded(i, j, true);
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
            endFight(false);
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
