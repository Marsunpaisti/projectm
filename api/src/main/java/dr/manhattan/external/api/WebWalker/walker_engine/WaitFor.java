package dr.manhattan.external.api.WebWalker.walker_engine;

import dr.manhattan.external.api.M;
import dr.manhattan.external.api.calc.MCalc;
import dr.manhattan.external.api.player.MPlayer;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;


public class WaitFor {
    private static Client client = M.getInstance().getClient();

    public static Condition getNotMovingCondition(){
        return new Condition() {
            final WorldPoint initialTile = MPlayer.location();
            final long movingDelay = 1300, startTime = System.currentTimeMillis();

            @Override
            public Return active() {
                if (System.currentTimeMillis() - startTime > movingDelay && initialTile.equals(MPlayer.location()) && !MPlayer.isMoving()) {
                    return Return.FAIL;
                }
                return Return.IGNORE;
            }
        };
    }

    public static int getMovementRandomSleep(WorldPoint position){
        return getMovementRandomSleep(Math.round(MPlayer.location().distanceToHypotenuse(position)));
    }

    public static int getMovementRandomSleep(int distance){
        final double multiplier =  MPlayer.isRunEnabled() ? 0.3 : 0.6;
        final int base = random(1800, 2400);
        if (distance > 25){
            return base;
        }
        int sleep = (int) (multiplier * distance);

        return (int)MCalc.distributedRandom(false, (int)Math.round(base * .8), (int)Math.round(base * 1.2), (int)Math.round(base * 0.1), base) + sleep;
    }

    public static Return isOnScreenAndClickable(WorldPoint targetPos){
        return WaitFor.condition(getMovementRandomSleep(targetPos),
                () -> {
                    if (targetPos.isInScene(client) && targetPos.distanceToHypotenuse(MPlayer.location()) < 10) {
                        return Return.SUCCESS;
                    }
                    return Return.IGNORE;
                });
    }

    public static Return condition(int timeout, Condition condition){
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() < startTime + timeout){
            switch (condition.active()){
                case SUCCESS: return Return.SUCCESS;
                case FAIL: return Return.FAIL;
                case IGNORE: milliseconds(75);
            }
        }
        return Return.TIMEOUT;
    }

    /**
     *
     * @param timeout
     * @param condition
     * @param <V>
     * @return waits {@code timeout} for the return value to not be null.
     */
    public static <V> V getValue(int timeout, ReturnCondition<V> condition){
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() < startTime + timeout){
            V v = condition.getValue();
            if (v != null){
                return v;
            }
            milliseconds(25);
        }
        return null;
    }

    public static int random(int low, int high) {
        return MCalc.nextInt(low, high);
    }

    public static Return milliseconds(int low, int high){
        try {
            Thread.sleep(random(low, high));
        } catch (InterruptedException e){
            throw new IllegalStateException("Break out");
        }
        return Return.IGNORE;
    }

    public static Return milliseconds(int amount){
        return milliseconds(amount, amount);
    }


    public enum Return {
        TIMEOUT,    //EXIT CONDITION BECAUSE OF TIMEOUT
        SUCCESS,    //EXIT CONDITION BECAUSE SUCCESS
        FAIL,       //EXIT CONDITION BECAUSE OF FAILURE
        IGNORE      //NOTHING HAPPENS, CONTINUE CONDITION

    }

    public interface ReturnCondition <V> {
        V getValue();
    }

    public interface Condition{
        Return active();
        default Condition combine(Condition a){
            Condition b = this;
            return () -> {
                Return result = a.active();
                return result != Return.IGNORE ? result : b.active();
            };
        }
    }

}