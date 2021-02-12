package dr.manhattan.external.api.WebWalker.wrappers;

import dr.manhattan.external.api.M;
import dr.manhattan.external.api.WebWalker.walker_engine.WaitFor;
import dr.manhattan.external.api.calc.MCalc;
import dr.manhattan.external.api.player.MPlayer;
import dr.manhattan.external.api.walking.MWalking;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;

public class AccurateMouse {

    public static boolean clickMinimap(RSTile tile){
        Client client = M.getInstance().getClient();
        if (tile == null) {
            return false;
        }
        if (!tile.toWorldPoint().isInScene(client) || tile.toWorldPoint().distanceTo(MPlayer.location()) >= 20) return false;

        return MWalking.walkTo(tile.toWorldPoint());
    }
}
