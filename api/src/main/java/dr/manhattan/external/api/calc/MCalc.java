package dr.manhattan.external.api.calc;

import net.runelite.api.Point;

import java.util.concurrent.ThreadLocalRandom;

public class MCalc {
    public static int nextInt(int min, int max)
    {
        //return (int) ((Math.random() * ((max - min) + 1)) + min); //This does not allow return of negative values
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    public static Point randomize(Point p, int amount){
        if(amount <= 0) return p;
        return new Point(nextInt(p.getX() - amount / 2, p.getX() + amount/2), nextInt(p.getY() - amount / 2, p.getY() + amount/2));
    }

    private static double clamp(double val, int min, int max)
    {
        return Math.max(min, Math.min(max, val));
    }

    public static long distributedRandom(boolean weightedDistribution, int min, int max, int deviation, int mean)
    {
        if (weightedDistribution)
        {
            /* generate a gaussian random (average at 0.0, std dev of 1.0)
             * take the absolute value of it (if we don't, every negative value will be clamped at the minimum value)
             * get the log base e of it to make it shifted towards the right side
             * invert it to shift the distribution to the other end
             * clamp it to min max, any values outside of range are set to min or max */
            return (long) clamp((-Math.log(Math.abs(ThreadLocalRandom.current().nextGaussian()))) * deviation + mean, min, max);
        }
        else
        {
            /* generate a normal even distribution random */
            return (long) clamp(Math.round(ThreadLocalRandom.current().nextGaussian() * deviation + mean), min, max);
        }
    }
}
