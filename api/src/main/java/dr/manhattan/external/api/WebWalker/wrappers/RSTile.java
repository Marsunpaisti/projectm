package dr.manhattan.external.api.WebWalker.wrappers;

import dr.manhattan.external.api.M;
import dr.manhattan.external.api.player.MPlayer;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;

public class RSTile {
    private static Client client = M.getInstance().getClient();

    public enum TYPES {
        WORLD,
        LOCAL
    }

    public int x, y, plane;
    public RSTile.TYPES type;

    public RSTile(WorldPoint worldPoint) {
        this(worldPoint.getX(), worldPoint.getY(), worldPoint.getPlane(), TYPES.WORLD);
    }

    public RSTile(int x, int y, int plane){
        this(x, y, plane, TYPES.WORLD);
    }

    public RSTile(int x, int y, int plane, RSTile.TYPES type){
        this.x = x;
        this.y = y;
        this.plane = plane;
        this.type = type;
    }

    public RSTile toWorldTile() {
        if (this.type == TYPES.WORLD) return this;
        return new RSTile(this.x + client.getBaseX(), this.y + client.getBaseY(), this.plane, TYPES.WORLD);
    }

    public RSTile toLocalTile() {
        if (this.type == TYPES.LOCAL) return this;
        return new RSTile(this.x - client.getBaseX(), this.y - client.getBaseY(), this.plane, TYPES.LOCAL);
    }

    public TYPES getType() {
        return this.type;
    }

    public int getX(){
        return this.x;
    }

    public int getY(){
        return this.y;
    }

    public int getPlane() { return this.plane; }

    public double distanceToDouble(RSTile tile) {
        if (tile.plane != this.plane)
        {
            return Double.MAX_VALUE;
        }
        return Math.hypot(getX() - tile.getX(), getY() - tile.getY());
    }

    public int distanceTo(RSTile tile) {
        if (tile.plane != this.plane)
        {
            return Integer.MAX_VALUE;
        }
        return (int)Math.round(distanceToDouble(tile));
    }

    public boolean isInMinimap(){
        return new RSTile(MPlayer.location()).distanceToDouble(this) <= 19;
    }

    public RSTile translate(int x, int y){
        return new RSTile(this.x + x, this.y + y, this.plane, this.type);
    }

    public WorldPoint toWorldPoint() {
        return new WorldPoint(this.x, this.y, this.plane);
    }

    @Override
    public boolean equals(Object o){
        assert (o instanceof RSTile);
        if (!(o instanceof RSTile)) return false;
        RSTile other = ((RSTile)o);
        return other.getX() == getX() && other.getY() == getY() && other.getPlane() == getPlane() && other.getType() == getType();
    }
}
