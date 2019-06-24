package com.bermudalocket.nerdydragon;

import org.bukkit.Particle;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Arrays;
import java.util.HashSet;

class PotionEffectHelper {

    // ------------------------------------------------------------------------
    /**
     * Removes all potion effects from the given entity.
     *
     * @param entity the entity.
     */
    public static void removePotionEffects(LivingEntity entity) {
        entity.getActivePotionEffects()
            .stream()
            .map(PotionEffect::getType)
            .forEach(entity::removePotionEffect);
    }

    // ------------------------------------------------------------------------
    /**
     * Modifies the dragon breath (i.e. AreaEffectCloud) color and effects.
     *
     * @param flame the flame/breath cloud.
     * @param stage the fight stage.
     */
    public static void modifyDragonBreath(AreaEffectCloud flame, FightStage stage) {
        Util.tagEntityWithMetadata(flame);
        flame.setParticle(Particle.REDSTONE, new Particle.DustOptions(stage.FLAME_COLOR, 1));
        if (stage == FightStage.FIRST) {
            return;
        }
        for (int i = 0; i < stage.MAX_EFFECTS; i++) {
            PotionEffect effect = getPotionEffect(getRandomEffect(), stage);
            flame.addCustomEffect(effect, false);
        }
        flame.setDuration(20*12);
    }

    // ------------------------------------------------------------------------
    /**
     * Applies random effects to a player. The quantity depends on the stage.
     *
     * @param player the player.
     * @param stage the stage.
     */
    public static void applyRandomEffects(Player player, FightStage stage) {
        int effectsApplied = 0;
        int n = stage.MAX_EFFECTS;
        for (int i = 0; i < n; i++) {
            if (Util.nextDouble() <= stage.POTION_EFFECT_CHANCE) {
                PotionEffectType effect = getRandomEffect();
                int randTicks = 20 * (Util.random(stage.MAX_EXTRA_POTION_DUR) + 3);
                if (effect == PotionEffectType.UNLUCK) {
                    player.setFireTicks(randTicks);
                } else {
                    PotionEffect potionEffect = new PotionEffect(effect, randTicks, 1, false, true, true);
                    potionEffect.apply(player);
                }
                effectsApplied++;
            }
            if (effectsApplied >= stage.MAX_EFFECTS) {
                break;
            }
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Returns a PotionEffect based on the given effect type with duration based
     * on the current fight stage. If stage is null, a 3-second duration will be
     * used.
     *
     * @param type the potion type.
     * @param stage the fight stage.
     * @return a PotionEffect.
     */
    private static PotionEffect getPotionEffect(PotionEffectType type, FightStage stage) {
        int duration = 20 * (3 + Util.random(stage.MAX_EXTRA_POTION_DUR));
        return new PotionEffect(type, duration, 1);
    }

    // ------------------------------------------------------------------------
    /**
     * Returns a random negative potion effect type.
     *
     * @return a random negative potion effect type.
     */
    private static PotionEffectType getRandomEffect() {
        return Util.getRandomObject(NEGATIVE_EFFECTS);
    }

    // ------------------------------------------------------------------------
    /**
     * A set of negative potion effects that can be applied to a player in
     * retaliation.
     */
    private static final HashSet<PotionEffectType> NEGATIVE_EFFECTS = new HashSet<>(Arrays.asList(
        PotionEffectType.BLINDNESS,
        PotionEffectType.LEVITATION,
        PotionEffectType.SLOW,
        PotionEffectType.WEAKNESS,
        PotionEffectType.WITHER,
        PotionEffectType.POISON,
        PotionEffectType.UNLUCK // translates to fire
    ));

}
