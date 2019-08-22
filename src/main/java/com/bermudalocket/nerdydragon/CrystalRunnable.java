/*
 * Copyright (c) 2019 bermudalocket. All rights reserved.
 * Unauthorized copying or distribution of this item without permission of the author is prohibited.
 * Proprietary and Confidential
 * Written by bermudalocket, 2019.
 */
package com.bermudalocket.nerdydragon;

import com.google.common.collect.ImmutableSet;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.UUID;

// ------------------------------------------------------------------------
/**
 * This is essentially a ticking clock with two distinct divisons: a main
 * "thread" which ticks every tick, and a "sub-thread" which ticks only when
 * its counter is positively nonzero.
 *
 * The main thread is a countdown until the next major action. For example,
 * let _mainDelay = 30. Then every time the clock ticks the -- operator is
 * applied, eventually reaching zero and firing off a major task (usually
 * choosing a new Master Crystal).
 *
 * The sub-thread is a countdown between minor actions, such as the delay
 * between each cycle of the "searching for master" process, i.e. when the
 * crystals randomly point their beams every 10 ticks.
 */
public class CrystalRunnable implements Runnable {

    private int _mainDelay;

    private int _subDelay = 1;

    /**
     * The current state of the clock.
     */
    private CrystalState _state = CrystalState.SEARCHING_FOR_MASTER;

    /**
     * The player currently being targeted by the crystals. Usually null.
     */
    private Player _target;

    /**
     * The set of active crystals.
     */
    private final HashSet<EnderCrystal> ENDER_CRYSTALS = new HashSet<>();

    /**
     * The current Master Crystal.
     */
    private EnderCrystal _masterCrystal;

    /**
     * The associated fight. Injected during construction.
     */
    private final EnderDragonFight _fight;

    /**
     * A reference to the underlying clock/BukkitTask.
     */
    private BukkitTask _task;

    /**
     * Possible states.
     */
    public enum CrystalState {
        SEARCHING_FOR_MASTER,
        FOCUS_ON_MASTER,
        ATTACK_PLAYER
    }

    // ------------------------------------------------------------------------
    /**
     * Constructor. Schedules and begins this runnable with Bukkit.
     */
    CrystalRunnable(EnderDragonFight fight) {
        _fight = fight;
        initCrystals();
        _mainDelay = 20 * MathUtil.random(9, 14);
        _task = Bukkit.getScheduler().runTaskTimer(NerdyDragon.PLUGIN, this, 1, 1);
    }

    // ------------------------------------------------------------------------
    /**
     * Deserializing constructor.
     */
    CrystalRunnable(EnderDragonFight fight, ConfigurationSection config) {
        _fight = fight;
        initCrystalsByMetadata();
        try {
            _state = CrystalState.valueOf(config.getString("state"));
            _target = Bukkit.getPlayer(UUID.fromString(config.getString("target")));
        } catch (Exception e) {
            _state = CrystalState.SEARCHING_FOR_MASTER;
            _target = null;
        }
        _mainDelay = config.getInt("current-delay", 20 * MathUtil.random(9, 14));
        _subDelay = config.getInt("action-delay", 0);
        _task = Bukkit.getScheduler().runTaskTimer(NerdyDragon.PLUGIN, this, 1, 1);
    }

    // ------------------------------------------------------------------------
    /**
     * Serializes the current state for loading after restart.
     *
     * @param config the parent config section.
     */
    void save(ConfigurationSection config) {
        ConfigurationSection crystalSection = config.getConfigurationSection("crystal-runnable");
        if (crystalSection == null) {
            crystalSection = config.createSection("crystal-runnable");
        }
        crystalSection.set("state", _state.toString());
        crystalSection.set("target", (_target != null) ? _target.getUniqueId().toString() : null);
        crystalSection.set("current-delay", _mainDelay);
        crystalSection.set("action-delay", _subDelay);
        NerdyDragon.PLUGIN.saveConfig();
    }

    // ------------------------------------------------------------------------
    /**
     * Returns true if this runnable is still running.
     *
     * @return true if this runnable is still running.
     */
    public boolean isRunning() {
        return !_task.isCancelled();
    }

    // ------------------------------------------------------------------------
    /**
     * Initializes this runnable by locating and storing all crystals in the
     * fight world by their physical location.
     */
    private void initCrystals() {
        if (NerdyDragon.CONFIG.ENDER_CRYSTAL_PILLAR_LOCATIONS.size() == 10) {
            for (Location location : NerdyDragon.CONFIG.ENDER_CRYSTAL_PILLAR_LOCATIONS) {
                boolean matched = false;
                for (EnderCrystal crystal : _fight.getWorld().getEntitiesByClass(EnderCrystal.class)) {
                    if (Util.weaklyCompareLocations(location, crystal.getLocation())) {
                        Util.tagEntityWithMetadata(crystal);
                        ENDER_CRYSTALS.add(crystal);
                        NerdyDragon.log("Init crystal: " + crystal);
                        matched = true;
                        break;
                    }
                }
                if (!matched) {
                    EnderCrystal crystal = (EnderCrystal) _fight.spawnReinforcement(location, EntityType.ENDER_CRYSTAL);
                    ENDER_CRYSTALS.add(crystal);
                    NerdyDragon.log("Force init crystal: " + crystal + ". Was it missing?");
                }
            }
        } else {
            for (EnderCrystal crystal : _fight.getWorld().getEntitiesByClass(EnderCrystal.class)) {
                Location loc = crystal.getLocation().clone();
                Block underneath = loc.getBlock().getRelative(BlockFace.DOWN);
                if (underneath.getType() == Material.BEDROCK && crystal.getLocation().getBlockY() >= 65) {
                    Util.tagEntityWithMetadata(crystal);
                    ENDER_CRYSTALS.add(crystal);
                    NerdyDragon.log("Init crystal: " + crystal);
                }
            }
            if (NerdyDragon.CONFIG.ENDER_CRYSTAL_PILLAR_LOCATIONS.size() < 10) {
                NerdyDragon.CONFIG.saveEnderCrystalPillarLocations(new HashSet<>(ENDER_CRYSTALS));
            }
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Initializes this runnable by locating and storing all crystals in the
     * fight world by their metadata.
     */
    private void initCrystalsByMetadata() {
        _fight.getWorld().getEntitiesByClass(EnderCrystal.class)
                         .stream()
                         .filter(Util::isReinforcement)
                         .forEach(ENDER_CRYSTALS::add);
    }

    // ------------------------------------------------------------------------
    /**
     * Regenerates a crystal.
     */
    private void regenerateCrystal() {
        if (ENDER_CRYSTALS.size() >= 10) {
            return;
        }
        HashSet<Location> generationSpots = new HashSet<>(NerdyDragon.CONFIG.ENDER_CRYSTAL_PILLAR_LOCATIONS);
        for (EnderCrystal crystal : ENDER_CRYSTALS) {
            Location loc = crystal.getLocation();
            generationSpots.removeIf(regenSpot -> loc.getBlockX() == regenSpot.getBlockX()
                                               && loc.getBlockY() == regenSpot.getBlockY()
                                               && loc.getBlockZ() == regenSpot.getBlockZ());
        }
        if (generationSpots.size() == 0) {
            return;
        }
        Location regenLoc = MathUtil.getRandomObject(generationSpots);
        EnderCrystal newCrystal = (EnderCrystal) _fight.spawnReinforcement(regenLoc, EntityType.ENDER_CRYSTAL);
        ENDER_CRYSTALS.add(newCrystal);
        _fight.playSound(Sound.ENTITY_WITHER_SPAWN, 0.2f);
    }

    // ------------------------------------------------------------------------
    /**
     * Returns an immutable set of the current crystals.
     *
     * @return an immutable set of the current crystals.
     */
    public ImmutableSet<EnderCrystal> getCrystals() {
        return ImmutableSet.copyOf(ENDER_CRYSTALS);
    }

    // ------------------------------------------------------------------------
    /**
     * Returns the current master crystal.
     *
     * @return the current master crystal.
     */
    public EnderCrystal getMasterCrystal() {
        return _masterCrystal;
    }

    // ------------------------------------------------------------------------
    /**
     * Stops this runnable.
     */
    void stop() {
        if (!_task.isCancelled()) {
            _task.cancel();
        }
        ENDER_CRYSTALS.forEach(crystal -> crystal.setBeamTarget(null)); // surviving ones
    }

    // ------------------------------------------------------------------------
    /**
     * Player may be null if the skip stage command is executed.
     *
     * @param crystal the exploding crystal.
     * @param player the player who blew up the crystal.
     */
    void handleCrystalDeath(EnderCrystal crystal, Player player) {
        ENDER_CRYSTALS.remove(crystal);
        // check if it's time for stage 2
        if (ENDER_CRYSTALS.size() == 0) {
            _fight.setStage(FightStage.SECOND);
            stop();
        } else {
            if (player != null && player.isOnline()) {
                DragonHelper.chargePlayer(player, _fight);
                _target = player;
                _state = CrystalState.ATTACK_PLAYER;
                _mainDelay = 20 * 3 + 1;
            }
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Returns a random EnderCrystal, or null if supplied with a null crystal.
     * Will try five times to avoid choosing the given crystal, but will
     * return it if it's the last one or in the extremely unlikely event that
     * the given crystal is chosen all five times with other possibilities.
     *
     * @param avoidChoosing a crystal that should not be considered unless it is
     *                      the last remaining crystal.
     * @return a random EnderCrystal.
     */
    private EnderCrystal getRandomCrystal(EnderCrystal avoidChoosing) {
        for (int i = 1; true; i++) {
            EnderCrystal randomCrystal = MathUtil.getRandomObject(ENDER_CRYSTALS);
            if (randomCrystal != avoidChoosing || i == 5) {
                return randomCrystal;
            }
        }
    }

    private void doAction() {
        if (_subDelay != 0) {
            _subDelay--;
            return;
        }
        switch (_state) {
            case SEARCHING_FOR_MASTER:
                // rotate crystal beams in search
                for (EnderCrystal crystal : ENDER_CRYSTALS) {
                    crystal.setBeamTarget(getRandomCrystal(crystal).getLocation());
                }
                _subDelay = 10;
                break;

            case FOCUS_ON_MASTER:
                // crystals focusing on the master crystal do nothing after
                // their beam is set until the _mainDelay runs out
                break;

            case ATTACK_PLAYER:
                // player has just blown up a crystal
                if (_fight.inRange(_target) && ENDER_CRYSTALS.size() > 0) {
                    int amplifier = ENDER_CRYSTALS.size() + 2;
                    if (Util.isFlying(_target)) {
                        _target.setGliding(false);
                    }
                    _target.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 3, amplifier));
                    for (EnderCrystal crystal : ENDER_CRYSTALS) {
                        crystal.setBeamTarget(_target.getLocation().subtract(0, 1, 0));
                    }
                }
                break;
        }
    }

    @Override
    public void run() {
        if (_mainDelay != 0) {
            doAction();
        } else {
            // change state
            switch (_state) {

                // crystals are done searching
                case SEARCHING_FOR_MASTER:
                    _state = CrystalState.FOCUS_ON_MASTER;
                    _masterCrystal = getRandomCrystal(null);
                    for (EnderCrystal crystal : ENDER_CRYSTALS) {
                        if (crystal != _masterCrystal) {
                            crystal.setInvulnerable(true);
                            crystal.setBeamTarget(_masterCrystal.getLocation());
                        } else {
                            _fight.playSound(Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.8f);
                            crystal.setInvulnerable(false);
                            crystal.setBeamTarget(crystal.getLocation().add(0,15,0));
                        }
                    }
                    _mainDelay = 20 * MathUtil.random(30, 50);
                    return;

                // time ran out and the player(s) did not blow up a crystal
                case FOCUS_ON_MASTER:
                    regenerateCrystal();
                    _state = CrystalState.SEARCHING_FOR_MASTER;
                    _mainDelay = 20 * MathUtil.random(12, 18);
                    return;

                // crystals are done focusing on the player & are ready to deal damage
                case ATTACK_PLAYER:
                    if (_target != null && _target.isOnline()) {
                        Util.WORLD_THE_END.playSound(_target.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, 6, 1.5f);
                        _target.damage(MathUtil.gaussian(6.5, 1.39), _masterCrystal);
                        _target.removePotionEffect(PotionEffectType.LEVITATION);
                    }
                    _state = CrystalState.SEARCHING_FOR_MASTER;
                    _mainDelay = 20 * MathUtil.random(12, 18);
                    _target = null;
                    return;
            }
        }

        _mainDelay--;
    }

}
