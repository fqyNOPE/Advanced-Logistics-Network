package logistics;

import mindustry.type.*;
import mindustry.world.meta.*;

/**
 * The flying logistics robot. Fast, fragile, unarmed; carries items between
 * logistics buildings. Uses {@link LogisticsAI} as its controller, which is
 * re-applied automatically after loading a save.
 */
public class LogisticsUnitTypes{
    public static UnitType logisticsBot;

    public static void load(){
        logisticsBot = new UnitType("logistics-bot"){{
            flying = true;
            lowAltitude = true;
            speed = 2f;
            accel = 0.5f;
            drag = 0.2f;
            health = 120f;
            hitSize = 4f;
            itemCapacity = 40;
            rotateSpeed = 12f;
            wobble = true;
            playerControllable = false;
            logicControllable = false;
            crashDamageMultiplier = 0f;
            envEnabled = Env.any;
            //the fork defaults envDisabled to Env.scorching, which would kill the
            //drone instantly on Erekir (its sectors have a scorching environment)
            envDisabled = Env.none;
            researchCostMultiplier = 0f;
            alwaysUnlocked = true;
            //always use the logistics AI, even after loading a save
            controller = u -> new LogisticsAI();
        }};
    }
}
