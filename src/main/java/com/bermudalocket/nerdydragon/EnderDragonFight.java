package com.bermudalocket.nerdydragon;

import com.bermudalocket.nerdydragon.tasks.RainFireTask;
import com.destroystokyo.paper.event.entity.EnderDragonFireballHitEvent;
import com.destroystokyo.paper.event.entity.EnderDragonFlameEvent;
import com.destroystokyo.paper.event.entity.EnderDragonShootFireballEvent;
import org.bukkit.Bukkit;
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
import org.bukkit.event.entity.EntityUnleashEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;
import java.util.function.Consumer;

// ------------------------------------------------------------------------
/**
 * Represents an instance of an EnderDragon fight.
 */
public class EnderDragonFight implements Listener {

    private FightRecord record;

    private UUID lastDamager;

    private final UUID id;

    private EnderDragon dragon;

    private FightStage stage;

    private BossBar _bossBar;

    private final World world;

    final long _timeStarted;

    private static final HashMap<UUID, Double> PARTICIPANTS = new HashMap<>();

    // ------------------------------------------------------------------------
    /**
     * Constructs a new Ender Dragon fight instance.
     *
     * @param dragon the newly-spawned dragon which started this fight.
     */
    public EnderDragonFight(EnderDragon dragon) {
        this.id = UUID.randomUUID();
        this.dragon = dragon;
        this.world = dragon.getWorld();
        _bossBar = dragon.getBossBar();

        NerdyDragon.log("Instantiating new fight with UUID " + this.id.toString());
        NerdyDragon.log("The dragon has UUID " + dragon.getUniqueId().toString());

        commonInit();
        Util.tagEntityWithMetadata(this.dragon);
        DragonController.modifyAttribute(this.dragon, Attribute.GENERIC_MAX_HEALTH, 0.75);
        announceStage(FightStage.FIRST);
        _timeStarted = System.currentTimeMillis();

        this.record = new FightRecord(this);
    }

    /**
     * Sends a subtitle alert to all nearby players.
     *
     * @param msg the message to send via a subtitle.
     */
    public void alertPlayer(Player player, String msg) {
        player.sendTitle("", msg, 10, 50, 10);
    }

    public void alertNearbyPlayers(String msg) {
        for (Player player : getNearbyPlayers()) {
            alertPlayer(player, msg);
        }
    }

    // ------------------------------------------------------------------------
    /**
     * A series of startup tasks common to both a newly-instantiated fight and
     * one being deserialized.
     */
    private void commonInit() {
        Bukkit.getPluginManager().registerEvents(this, NerdyDragon.PLUGIN);
        _bossBar.setColor(this.stage.BOSS_BAR_COLOR);
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
        config.set("id", this.id.toString());
        config.set("world-name", this.world.getName());
        config.set("stage", this.stage.toString());
        config.set("started-at", _timeStarted);
        config.set("restart-at", System.currentTimeMillis());
        Chunk chunk = this.dragon.getLocation().getChunk();
        config.set("dragon-chunk", chunk.getX() + "," + chunk.getZ());
        for (UUID uuid : PARTICIPANTS.keySet()) {
            String uuidString = uuid.toString();
            config.set("participants." + uuidString, PARTICIPANTS.get(uuid));
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
        return this.id;
    }

    // ------------------------------------------------------------------------
    /**
     * Returns the Ender Dragon.
     *
     * @return the Ender Dragon.
     */
    public EnderDragon getDragon() {
        return this.dragon;
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
        NerdyDragon.log("Dragon switcheroo called.");
        NerdyDragon.log("Old UUID: " + this.dragon.getUniqueId());
        NerdyDragon.log("New UUID: " + newDragon.getUniqueId());
        DragonController.mergeDragons(newDragon, this.dragon);
        this.dragon = newDragon;
        Util.tagEntityWithMetadata(newDragon);
        _bossBar = newDragon.getBossBar();
        _bossBar.setColor(this.stage.BOSS_BAR_COLOR);
        _bossBar.setStyle(BarStyle.SEGMENTED_20);
    }

    // ------------------------------------------------------------------------
    /**
     * Returns the world in which this fight is taking place.
     *
     * @return the world in which this fight is taking place.
     */
    public World getWorld() {
        return this.world;
    }

    // ------------------------------------------------------------------------
    /**
     * Stops the current fight.
     *
     * @param forced if the fight's end is forced (e.g. by command).
     */
    public void endFight(boolean forced) {
        removeReinforcements(forced);
        this.stage = FightStage.FINISHED;
        Thread.newThread(5, () -> {
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
        Entity entity = this.world.spawnEntity(location, type);
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
        for (Entity entity : this.world.getEntities()) {
            if (!Util.isReinforcement(entity)) {
                continue;
            }
            if (entity instanceof EnderDragon) {
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
                              && player.getWorld() == this.world
                              && this.getNearbyPlayers().contains(player);
    }

    // ------------------------------------------------------------------------
    /**
     * Returns all players within 160 blocks of the End Portal.
     *
     * @return all players within 160 blocks of the End Portal.
     */
    public HashSet<Player> getNearbyPlayers() {
        return new HashSet<>(this.world.getNearbyPlayers(new Location(this.world, 0, 65, 0), 160));
    }

    // ------------------------------------------------------------------------
    /**
     * Returns the current stage.
     *
     * @return the current stage.
     */
    public FightStage getStage() {
        return this.stage;
    }

    // ------------------------------------------------------------------------
    /**
     * Sets the current stage.
     */
    void setStage(FightStage stage) {
        if (this.stage != stage) {
            this.stage = stage;
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
        switch (this.stage) {
            case SECOND:
            case THIRD:
                this.dragon.setHealth(this.stage.DRAGON_HP_LOW_BOUND * DragonController.getMaxHealth(this.dragon));
                setStage(FightStage.getNext(this.stage));
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
        for (Player nearbyPlayer : getNearbyPlayers()) {
            nearbyPlayer.sendTitle(stage.DISPLAY_NAME, "", 20, 20 * 4, 20);
            nearbyPlayer.playSound(nearbyPlayer.getLocation(), Sound.ENTITY_ENDER_DRAGON_AMBIENT, 3, 1);
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
        EntityType entityType = e.getEntityType();

        // ignore non-reinforcements
        if (!Util.isReinforcement(e.getEntity())) {
            return;
        }

        // nerf drops to prevent farming
        if (Configuration.NERF_DROPS && entityType != EntityType.ENDER_DRAGON) {
            if (Util.tryWithChance(Configuration.DROP_NERF_RATE)) {
                e.getDrops().clear();
            }
            return;
        }

        // go no further if we're not dealing with a dragon
        if (entityType != EntityType.ENDER_DRAGON) {
            return;
        }

        // clear the dragon's drops. we're going to award our own loot and drop it elsewhere
        e.getDrops().clear();

        // figure out who should get the loot. we need someone alive and online
        Player killer = e.getEntity().getKiller();
        if (NerdyDragon.PLUGIN.isValidLootRecipient(killer)) {
            NerdyDragon.PLUGIN.awardLoot(killer);
        } else {
            // sometimes Entity#getKiller is null. we'll check the dragon's last damager for more info
            Player lastDamager = Bukkit.getPlayer(this.lastDamager);
            if (NerdyDragon.PLUGIN.isValidLootRecipient(lastDamager)) {
                NerdyDragon.PLUGIN.awardLoot(lastDamager);
            } else {
                // the Entity#getKiller is null and the last damager is unavailable too, so let the dragon
                // drop the loot
                NerdyDragon.log("Dragon death: no specific killer found or they're offline or dead. Dropping loot naturally.");
                e.getDrops().addAll(NerdyDragon.CONFIG.getLoot());
            }
        }

        // finalize record, send out win message, and locally play dragon death sound for everyone
        // except those who have it muted
        this.record.setDuration(System.currentTimeMillis() - _timeStarted);
        String winMessage = this.record.getWinMessage();
        for (Player player : Bukkit.getOnlinePlayers()) {
            NerdyDragon.message(player, winMessage);
            if (!NerdyDragon.PLUGIN.getPlayerState(player).didMuteDragon()) {
                player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_DEATH, 1f, 0.82f);
            }
        }

        // record this fight into history
        NerdyDragon.LEADERBOARD.add(this.record);

        // clean up
        this.endFight(false);
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
        if (this.stage != FightStage.THIRD && this.stage != FightStage.FOURTH) {
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
                    ShulkerBullet bullet = (ShulkerBullet) this.world.spawnEntity(loc.add(0, 1, 0), EntityType.SHULKER_BULLET);
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
        PotionEffectHelper.modifyDragonBreath(e.getAreaEffectCloud(), this.stage);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDragonFireballImpact(EnderDragonFireballHitEvent e) {
        DragonActionContext context = new DragonActionContext(this.dragon, e.getAreaEffectCloud(), this.stage);
        for (DragonControllerListener listener : NerdyDragon.LISTENERS) {
            Consumer<EnderDragonFight> consumer = listener.onFireballImpact(context);
            Thread.newThread(() -> consumer.accept(this));
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Prevent area effect clouds from affecting reinforcements.
     */
    @EventHandler
    public void onAreaEffectCloudApplyEvent(AreaEffectCloudApplyEvent event) {
        event.getAffectedEntities().removeIf(Util::isReinforcement);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDragonFireball(EnderDragonShootFireballEvent e) {
        DragonActionContext context = new DragonActionContext(this.dragon, DragonController.findRandomTarget(this), this.stage);
        for (DragonControllerListener listener : NerdyDragon.LISTENERS) {
            Consumer<EnderDragonFight> consumer = listener.onDragonFireball(context);
            Thread.newThread(() -> consumer.accept(this));
        }
    }

    // ------------------------------------------------------------------------
    /**
     * @see EnderDragonChangePhaseEvent
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDragonPhaseChange(EnderDragonChangePhaseEvent e) {
        EnderDragon.Phase phase = e.getNewPhase();
        double rand = Util.nextDouble();
        switch (this.stage) {
            case FIRST:
                if (phase != EnderDragon.Phase.CHARGE_PLAYER && phase != EnderDragon.Phase.CIRCLING) {
                    e.setCancelled(true);
                    this.dragon.setPhase(EnderDragon.Phase.CIRCLING);
                }
                break;

            case SECOND:
                if (phase == EnderDragon.Phase.FLY_TO_PORTAL) {
                    e.setCancelled(true);
                    if (rand <= 0.30) {
                        this.dragon.setPhase(EnderDragon.Phase.CIRCLING);
                    } else if (rand <= 0.80) {
                        DragonController.fireballRandomPlayer(this);
                    } else {
                        DragonController.chargeRandomPlayer(this);
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
                        DragonController.chargeRandomPlayer(this);
                    } else {
                        DragonController.fireballRandomPlayer(this);
                    }
                }
                break;

            case FOURTH:
                if (phase == EnderDragon.Phase.FLY_TO_PORTAL) {
                    e.setCancelled(true);
                    if (rand <= 0.20) {
                        new RainFireTask(this);
                    } else if (rand <= 0.40) {
                        DragonController.chargeRandomPlayer(this);
                    } else if (rand <= 0.90) {
                        DragonController.fireballRandomPlayer(this);
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
            double nextStageAt = this.stage.DRAGON_HP_LOW_BOUND * dragon.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
            if (health <= nextStageAt) {
                FightStage next = FightStage.getNext(this.stage);
                setStage(next);
                if (next == FightStage.THIRD) {
                    DragonController.modifyAttribute(getDragon(), Attribute.GENERIC_MOVEMENT_SPEED, 0.35);
                    DragonController.modifyAttribute(getDragon(), Attribute.GENERIC_ARMOR, 0.35);
                } else if (next == FightStage.FOURTH) {
                    DragonController.modifyAttribute(getDragon(), Attribute.GENERIC_MOVEMENT_SPEED, 0.75);
                    DragonController.modifyAttribute(getDragon(), Attribute.GENERIC_ARMOR, 0.50);
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

        if (e.getEntityType() == EntityType.ENDER_DRAGON) {
            double finalDamage = e.getFinalDamage();
            if (this.dragon.getHealth() - finalDamage < 0) {
                finalDamage -= Math.abs(this.dragon.getHealth() - finalDamage);
            }

            Entity damager = e.getDamager();
            if (damager instanceof Player) {
                this.record.getParticipant((Player) damager).addDamage(finalDamage);
                this.alertNearbyPlayers(damager.getName() + " inflicted " + finalDamage + " damage");
            } else if (damager instanceof Projectile) {
                ProjectileSource shooter = ((Projectile) damager).getShooter();
                if (shooter instanceof Player) {
                    Player playerShooter = (Player) shooter;
                    //recordDamage(playerShooter, finalDamage);
                    this.lastDamager = playerShooter.getUniqueId();
                    this.alertNearbyPlayers(playerShooter.getName() + "'s " + damager.getType().toString() + " inflicted " + finalDamage + " damage");
                }
            } else {
                this.alertNearbyPlayers(damager.getType().toString() + " inflicted " + finalDamage + " damage");
            }

            // if the dragon is dead or about to die, don't do any extra stuff
            if (this.dragon.getHealth() - e.getFinalDamage() <= 1) {
                return;
            }

            // TODO
/*
            // if at portal, try to leave early
            if (Util.tryWithChance(this.stage.LEAVE_PORTAL_CHANCE)) {
                new LeavePortalTask(this);
            }

            // apply random potion effects
            PotionEffectHelper.applyRandomEffects(player, this.stage);

            // try to spawn some reinforcements
            if (Util.tryWithChance(this.stage.REINFORCEMENT_CHANCE)) {
                new ReinforcementSpawnTask(this);
            }

 */
        }
    } // onEntityDamageByEntity

    // ------------------------------------------------------------------------
    /**
     * Deserializing constructor.
     */
    EnderDragonFight(ConfigurationSection config) {
        _timeStarted = config.getLong("started-at", 0) + (System.currentTimeMillis() - config.getLong("restart-at", 0));

        String uuidAsString = config.getString("id");
        if (uuidAsString == null) {
            NerdyDragon.log("Error loading instantiated fight, bad UUID: " + uuidAsString);
            NerdyDragon.log("Attempting to proceed with new UUID");
            this.id = UUID.randomUUID();
        } else {
            this.id = UUID.fromString(uuidAsString);
        }
        NerdyDragon.log("Instantiating serialized fight with UUID " + this.id.toString());

        // find world
        String worldName = config.getString("world-name");
        if (worldName == null) {
            NerdyDragon.log("Error loading instantiated fight, bad world: null. Trying world_the_end");
            worldName = "world_the_end";
        }
        World world = Bukkit.getWorld(worldName);
        this.world = (world != null) ? world : Util.WORLD_THE_END;

        try {
            String[] dragonLastChunkCoords = config.getString("dragon-chunk","1,1").split(",");
            int chunkX = Integer.parseInt(dragonLastChunkCoords[0]);
            int chunkZ = Integer.parseInt(dragonLastChunkCoords[1]);
            Chunk dragonChunk = this.world.getChunkAt(chunkX, chunkZ);
            if (!dragonChunk.isLoaded()) {
                this.world.loadChunk(dragonChunk);
                NerdyDragon.log("Loaded last dragon chunk at (" + chunkX + ", " + chunkZ + ")");
            }
        } catch (Exception e) {
            NerdyDragon.log("Error loading dragon's last location.");
        }

        int radius = 6;
        for (int i = -1*radius; i <= radius; i++) {
            for (int j = -1*radius; j <= radius; j++) {
                Chunk chunk = this.world.getChunkAt(i, j);
                if (!chunk.isLoaded()) {
                    this.world.loadChunk(i, j);
                    NerdyDragon.log("Loaded chunk at (" + i + ", " + j + ")");
                }
            }
        }

        // find dragon
        NerdyDragon.log("Finding the dragon...");
        boolean foundDragon = false;
        for (Entity entity : this.world.getEntities()) {
            if (entity.getType() == EntityType.ENDER_DRAGON) {
                this.dragon = (EnderDragon) entity;
                Util.tagEntityWithMetadata(this.dragon);
                foundDragon = true;
                break;
            }
        }
        if (!foundDragon) {
            NerdyDragon.log("Couldn't find the dragon :(");
            endFight(true);
            return;
        }

        NerdyDragon.log("The dragon has UUID " + this.dragon.getUniqueId().toString());

        // stage
        String stageName = config.getString("stage");
        try {
            this.stage = FightStage.valueOf(stageName);
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
                PARTICIPANTS.put(uuid, damage);
            }
        }

        _bossBar = this.dragon.getBossBar();

        commonInit();

        NerdyDragon.PLUGIN.getConfig().set("saved-fight", null);
        NerdyDragon.PLUGIN.saveConfig();
    }

}
