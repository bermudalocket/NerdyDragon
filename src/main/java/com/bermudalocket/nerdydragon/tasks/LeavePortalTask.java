package com.bermudalocket.nerdydragon.tasks;

import com.bermudalocket.nerdydragon.EnderDragonFight;
import com.bermudalocket.nerdydragon.MathUtil;
import org.bukkit.entity.EnderDragon;

import java.util.Arrays;
import java.util.HashSet;

public class LeavePortalTask extends AbstractFightTask {

    public LeavePortalTask(EnderDragonFight fight) {
        super(fight, false);
    }

    @Override
    public void run() {
        EnderDragon dragon = getDragon();
        if (isAtPortal(dragon)) {
            dragon.setPhase(EnderDragon.Phase.LEAVE_PORTAL);
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Returns true if the dragon is hovering at the portal. We have to be very
     * careful about changing the dragon's phase to LEAVE_PORTAL. If it is done
     * when the dragon is not in a compatible phase (i.e. isn't really at the
     * portal), a server crash is possible.
     *
     * @param dragon the dragon.
     * @return true if the dragon is hovering at the portal.
     */
    private boolean isAtPortal(EnderDragon dragon) {
        // check position
        double x = MathUtil.floor(MathUtil.abs(dragon.getLocation().getX()));
        double z = MathUtil.floor(MathUtil.abs(dragon.getLocation().getY()));

        // check phase
        boolean isPortalPhase = PORTAL_PHASES.contains(dragon.getPhase());

        return isPortalPhase && (x <= 3 && z <= 3);
    }

    // ------------------------------------------------------------------------
    /**
     * There is no singular phase we can use to determine if the dragon is hovering
     * at the portal, so we have to combine a position check with a phase check
     * for all of the following phases.
     */
    private static final HashSet<EnderDragon.Phase> PORTAL_PHASES = new HashSet<>(Arrays.asList(
        EnderDragon.Phase.BREATH_ATTACK,
        EnderDragon.Phase.HOVER,
        EnderDragon.Phase.SEARCH_FOR_BREATH_ATTACK_TARGET,
        EnderDragon.Phase.ROAR_BEFORE_ATTACK
    ));

}
