package dr.manhattan.external.api.WebWalker.walker_engine.interaction_handling;

import dr.manhattan.external.api.WebWalker.walker_engine.WaitFor;
import dr.manhattan.external.api.calc.MCalc;
import dr.manhattan.external.api.interact.MInteract;
import net.runelite.api.GameObject;
import net.runelite.api.NPC;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.WidgetItem;

public class InteractionHelper {
    public static boolean click(GameObject object, String... actions){
        return click(object, actions, null);
    }

    public static boolean click(GameObject object, String action, WaitFor.Condition condition){
        return click(object, new String[]{action}, condition);
    }

    /**
     * Interacts with nearby object and waits for {@code condition}.
     *
     * @param object GameObject to click
     * @param actions actions to click
     * @param condition condition to wait for after the click action
     * @return if {@code condition} is null, then return the outcome of condition.
     *          Otherwise, return the result of the click action.
     */
    public static boolean click(GameObject object, String[] actions, WaitFor.Condition condition){
        if (object == null){
            return false;
        }
        if (!MInteract.GameObject(object, actions)){
            return false;
        }
        return condition == null || WaitFor.condition(MCalc.nextInt(7000, 8500), condition) == WaitFor.Return.SUCCESS;
    }

    public static boolean click(NPC npc, String... actions){
        return click(npc, actions, null);
    }

    public static boolean click(NPC npc, String action, WaitFor.Condition condition){
        return click(npc, new String[]{action}, condition);
    }

    /**
     * Interacts with nearby object and waits for {@code condition}.
     *
     * @param npc npc to click
     * @param actions actions to click
     * @param condition condition to wait for after the click action
     * @return if {@code condition} is null, then return the outcome of condition.
     *          Otherwise, return the result of the click action.
     */
    public static boolean click(NPC npc, String[] actions, WaitFor.Condition condition){
        if (npc == null){
            return false;
        }
        if (!MInteract.NPC(npc, actions)){
            return false;
        }
        return condition == null || WaitFor.condition(MCalc.nextInt(7000, 8500), condition) == WaitFor.Return.SUCCESS;
    }

    public static boolean useItemOnObject(WidgetItem item, GameObject object){
        return MInteract.UseItemOnObject(item, object);
    }


}
