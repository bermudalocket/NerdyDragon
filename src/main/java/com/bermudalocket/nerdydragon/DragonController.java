package com.bermudalocket.nerdydragon;

import net.minecraft.server.v1_14_R1.EntityLiving;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.craftbukkit.v1_14_R1.entity.CraftEnderDragon;
import org.bukkit.craftbukkit.v1_14_R1.entity.CraftPlayer;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.DragonFireball;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.UUID;
import java.util.function.Consumer;

public class DragonController implements DragonControllerListener {

    public DragonController() { }

    public Consumer<EnderDragonFight> onDragonFireball(DragonActionContext context) {
        final Entity target = context.getTarget();
        if (target == null) return null;

        // TODO test if all this fuckery is still necessary or if they fixed their velocity physics
        final EnderDragon dragon = context.getDragon();
        return fight -> Thread.newRepeatedThread(1, fight.getStage().MAX_EXTRA_FIREBALLS, fight.getStage().FIREBALL_TICK_INCREMENT, () -> {
            Location targetLoc = target.getLocation();
            Location dragonLoc = dragon.getEyeLocation();

            // gotta spawn the fireball ahead of the dragon otherwise it'll just crash right into it
            // like a fucking twit
            Vector farEnoughAhead = dragonLoc.toVector()
                                             .add(dragonLoc.getDirection()
                                                           .multiply(4));
            Vector dP = targetLoc.toVector()
                                 .subtract(farEnoughAhead)
                                 .normalize()
                                 .multiply(0.75);
            DragonFireball fireball = dragon.launchProjectile(DragonFireball.class, dP);
            fireball.setDirection(dP);
        });
    }

    public Consumer<EnderDragonFight> onFireballImpact(DragonActionContext context) {
        if (context.getTarget() == null) {
            return null;
        }
        AreaEffectCloud dragonBreath = (AreaEffectCloud) context.getTarget();
        Util.tagEntityWithMetadata(dragonBreath);
        PotionEffectHelper.modifyDragonBreath(dragonBreath, context.getStage());

        return fight -> {
            if (Util.tryWithChance(0.30)) {
                Thread.newRepeatedThread(1, fight.getStage().MAX_ENDERMITES, 1, () -> {
                    fight.spawnReinforcement(dragonBreath.getLocation(), EntityType.ENDERMITE);
                });
            }
        };
    }

    // TODO below -------------------------------------------------------------

    // ------------------------------------------------------------------------
    /**
     * Sometimes the dragon disappears during the middle of a fight because
     * Mojang likes to keep things fun and interesting like that. This method
     * aims to make the transition as seamless as possible by merging certain
     * information from our old dragon's instance (which we're holding) into
     * the new dragon. This currently copies health, attributes, location,
     * and phase.
     *
     * @param newDragon the new dragon.
     * @param oldDragon the missing/old dragon (should really be passed as
     *                  EnderDragonFight#getDragon).
     */
    static void mergeDragons(EnderDragon newDragon, EnderDragon oldDragon) {
        for (Attribute attribute : Attribute.values()) {
            try {
                modifyAttribute(newDragon, attribute, oldDragon.getAttribute(attribute).getValue());
                NerdyDragon.log("Merged attribute " + attribute + ".");
            } catch (Exception unsupportedAttribute) { }
        }
        newDragon.setHealth(oldDragon.getHealth());
        newDragon.teleport(oldDragon.getLocation());
        newDragon.setPhase(oldDragon.getPhase());
    }

    // ------------------------------------------------------------------------
    /**
     * Heals the given dragon by the given amount. Will not heal the dragon
     * above its max health.
     *
     * @param dragon the dragon.
     * @param amount the amount to heal.
     */
    public static void healDragon(EnderDragon dragon, double amount) {
        if (dragon.getHealth() <= 0) {
            return;
        }
        double newHealth = dragon.getHealth() + amount;
        double maxHealth = dragon.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
        if (newHealth > maxHealth) {
            newHealth = maxHealth;
        }
        dragon.setHealth(newHealth);
    }

    // ------------------------------------------------------------------------
    /**
     * Returns the dragon's max health.
     *
     * @param dragon the dragon.
     * @return the dragon's max health.
     */
    public static double getMaxHealth(EnderDragon dragon) {
        return dragon.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
    }

    // ------------------------------------------------------------------------
    /**
     * Modifies the given attribute as a scalar of the given amount. If the dragon
     * does not support the given attribute, this method will fail silently. If
     * the dragon's max health is changed, it will be healed.
     *
     * @param dragon the dragon.
     * @param attribute the attribute to modify.
     * @param amount the scalar amount by which to modify.
     */
    public static void modifyAttribute(EnderDragon dragon, Attribute attribute, double amount) {
        AttributeInstance dragonAttribute = dragon.getAttribute(attribute);
        if (dragonAttribute != null) {
            AttributeModifier modifier = new AttributeModifier(UUID.randomUUID().toString(), amount, AttributeModifier.Operation.MULTIPLY_SCALAR_1);
            dragon.getAttribute(attribute).addModifier(modifier);
            if (attribute == Attribute.GENERIC_MAX_HEALTH) {
                dragon.setHealth(dragon.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
            }
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Directs the dragon to shoot a fireball at a random player.
     *
     * @param fight the associated fight.
     */
    static void fireballRandomPlayer(EnderDragonFight fight) {
        attackPlayer(findRandomTarget(fight), EnderDragon.Phase.STRAFING, fight);
    }

    // ------------------------------------------------------------------------
    /**
     * Directs the dragon to charge a random player.
     *
     * @param fight the associated fight.
     */
    static void chargeRandomPlayer(EnderDragonFight fight) {
        attackPlayer(findRandomTarget(fight), EnderDragon.Phase.CHARGE_PLAYER, fight);
    }

    // ------------------------------------------------------------------------
    /**
     * Directs the dragon to charge the given player.
     *
     * @param player the player to charge.
     * @param fight the associated fight.
     */
    static void chargePlayer(Player player, EnderDragonFight fight) {
        attackPlayer(player, EnderDragon.Phase.CHARGE_PLAYER, fight);
    }

    // ------------------------------------------------------------------------
    /**
     * Directs the dragon to target the given player and then enter the given
     * phase.
     *
     * @param player the player.
     * @param phase the phase.
     * @param fight the associated fight.
     */
    private static void attackPlayer(Player player, EnderDragon.Phase phase, EnderDragonFight fight) {
        EnderDragon dragon = fight.getDragon();
        if (dragon == null || dragon.isDead() || !fight.inRange(player)) {
            return;
        }
        EntityLiving playerEntity = ((CraftPlayer) player).getHandle();
        ((CraftEnderDragon) dragon).getHandle().setGoalTarget(playerEntity, EntityTargetEvent.TargetReason.CLOSEST_PLAYER, false);
        dragon.setPhase(phase);
    }

    // ------------------------------------------------------------------------
    /**
     * Begins a search for a random player to target.
     *
     * @param fight the associated fight.
     * @return a random player to target.
     */
    static Player findRandomTarget(EnderDragonFight fight) {
        UUID searchUUID = UUID.randomUUID();
        Player player = Util.getRandomObject(fight.getNearbyPlayers());
        if (fight.inRange(player)) {
            return player;
        } else {
            _findTargetAttempts.put(searchUUID, 1);
            return findRandomTarget(fight, searchUUID); // try again
        }
    }

    private static Player findRandomTarget(EnderDragonFight fight, UUID searchUUID) {
        int attempts = _findTargetAttempts.get(searchUUID);
        if (attempts > 3) {
            _findTargetAttempts.remove(searchUUID);
            return null;
        }
        Player player = Util.getRandomObject(fight.getNearbyPlayers());
        if (fight.inRange(player)) {
            _findTargetAttempts.remove(searchUUID);
            return player;
        } else {
            _findTargetAttempts.put(searchUUID, attempts + 1);
            return findRandomTarget(fight, searchUUID); // try again
        }
    }

    private static final HashMap<UUID, Integer> _findTargetAttempts = new HashMap<>();

    static double getDamageRatio(double damage, EnderDragon dragon) {
        if (damage == 0) {
            return 0;
        }
        double ratio = 100 * (damage / getMaxHealth(dragon));
        return ratio >= 100 ? 100 : ratio;
    }

}
