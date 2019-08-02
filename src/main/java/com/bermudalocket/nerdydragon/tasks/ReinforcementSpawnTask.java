package com.bermudalocket.nerdydragon.tasks;

import com.bermudalocket.nerdydragon.EnderDragonFight;
import com.bermudalocket.nerdydragon.FightStage;
import com.bermudalocket.nerdydragon.NerdyDragon;
import com.bermudalocket.nerdydragon.Util;
import com.bermudalocket.nerdydragon.util.OrderedPair;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Bat;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Evoker;
import org.bukkit.entity.Phantom;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Arrays;
import java.util.HashSet;

public class ReinforcementSpawnTask extends AbstractFightTask {

    private final FightStage this.stage;

    public ReinforcementSpawnTask(EnderDragonFight fight) {
        super(fight, true);
        this.stage = fight.getStage();
    }

    @Override
    public void run() {
        double rand = Util.nextDouble();
        int n = Util.random(this.stage.MAX_REINF_PER_CLUSTER);

        if (rand <= 0.30) {
            NerdyDragon.playSound(Sound.ENTITY_EVOKER_PREPARE_SUMMON, 0.8f);
            for (int i = 1; i <= n; i++) {
                spawnPhantomWithPassenger();
            }
        } else if (rand <= 0.50) {
            NerdyDragon.playSound(Sound.ENTITY_ENDERMAN_STARE, 1.5f);
            spawnAngryEnderman();
        } else if (rand <= 0.75) {
            if (this.stage == FightStage.SECOND) {
                return;
            }
            NerdyDragon.playSound(Sound.ENTITY_ILLUSIONER_CAST_SPELL, 0.7f);
            for (int i = 1; i <= n; i++) {
                spawnShulker();
            }
        } else {
            if (this.stage == FightStage.SECOND || this.stage == FightStage.THIRD) {
                return;
            }
            NerdyDragon.playSound(Sound.ENTITY_EVOKER_PREPARE_WOLOLO, 0.6f);
            for (int i = 1; i <= 3; i++) {
                spawnEvokerRidingBat();
            }
        }
    }

    private void spawnAngryEnderman() {
        Player target = Util.getRandomObject(_fight.getNearbyPlayers());
        Location loc = target.getLocation();
        Location spawnLoc = loc.toVector()
                               .add(new Vector(0, 10, 0))
                               .toLocation(target.getWorld());
        Enderman enderman = (Enderman) _fight.spawnReinforcement(spawnLoc, EntityType.ENDERMAN);
        enderman.setTarget(target);
    }

    private void spawnPhantomWithPassenger() {
        int phantomSize = Util.random(this.stage.MIN_PHANTOM_SIZE, this.stage.MAX_PHANTOM_SIZE);
        EntityType passengerType = Util.getRandomObject(DEFAULT_PASSENGERS);
        Location loc = _fight.getCenter().clone().add(0, Util.random(15, 60), 0);
        Phantom phantom = (Phantom) _fight.spawnReinforcement(loc, EntityType.PHANTOM);
        phantom.setSize(phantomSize);
        Entity passenger = _fight.spawnReinforcement(phantom.getLocation(), passengerType);
        phantom.addPassenger(passenger);
        phantom.setLeashHolder(passenger);
    }

    private void spawnShulker() {
        World world = getDragon().getWorld();
        OrderedPair<Integer> coords = Util.getRandomCoordinates(30);
        int x = coords.getA();
        int z = coords.getB();
        double y = world.getHighestBlockYAt(x, z) + 1;
        Location spawnLoc = new Location(world, x, y, z);
        _fight.spawnReinforcement(spawnLoc, EntityType.SHULKER);
    }

    private void spawnEvokerRidingBat() {
        World world = getDragon().getWorld();
        OrderedPair<Integer> coords = Util.getRandomCoordinates(30);
        int x = coords.getA();
        int z = coords.getB();
        double y = world.getHighestBlockYAt(x, z) + 12;
        Location spawnLoc = new Location(world, x, y, z);
        Bat bat = (Bat) _fight.spawnReinforcement(spawnLoc, EntityType.BAT);
        Evoker evoker = (Evoker) _fight.spawnReinforcement(spawnLoc, EntityType.EVOKER);
        bat.addPassenger(evoker);
    }

    private static final HashSet<EntityType> DEFAULT_PASSENGERS = new HashSet<>(Arrays.asList(
        EntityType.VINDICATOR,
        EntityType.STRAY,
        EntityType.SKELETON,
        EntityType.ENDERMAN
    ));

}
