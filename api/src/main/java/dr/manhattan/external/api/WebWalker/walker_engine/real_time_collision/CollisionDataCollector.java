package dr.manhattan.external.api.WebWalker.walker_engine.real_time_collision;

import dr.manhattan.external.api.M;
import dr.manhattan.external.api.WebWalker.wrappers.RSTile;
import dr.manhattan.external.api.player.MPlayer;
import net.runelite.api.Client;
import net.runelite.rs.api.RSClient;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
public class CollisionDataCollector {
    private static Client client = M.getInstance().getClient();

    public static int[][] getCollisionData(){
        int[][] collisionData = ((RSClient)client).getCollisionMaps()[0].getFlags();
        return collisionData;
    }

    public static void generateRealTimeCollision(){
        RealTimeCollisionTile.clearMemory();

        RSTile playerPosition = new RSTile(MPlayer.location());
        int[][] collisionData = getCollisionData();

        if (collisionData == null) {
            return;
        }

        for (int i = 0; i < collisionData.length; i++) {
            for (int j = 0; j < collisionData[i].length; j++) {
                RSTile localTile = new RSTile(i, j, playerPosition.getPlane(), RSTile.TYPES.LOCAL);
                RSTile worldTile = localTile.toWorldTile();
                RealTimeCollisionTile.create(worldTile.getX(), worldTile.getY(), worldTile.getPlane(), collisionData[i][j]);
            }
        }
    }

    public static void updateRealTimeCollision(){
        RSTile playerPosition = new RSTile(MPlayer.location());
        int[][] collisionData = getCollisionData();
        if(collisionData == null)
            return;
        for (int i = 0; i < collisionData.length; i++) {
            for (int j = 0; j < collisionData[i].length; j++) {
                RSTile localTile = new RSTile(i, j, playerPosition.getPlane(), RSTile.TYPES.LOCAL);
                RSTile worldTile = localTile.toWorldTile();
                RealTimeCollisionTile realTimeCollisionTile = RealTimeCollisionTile.get(worldTile.getX(), worldTile.getY(), worldTile.getPlane());
                if (realTimeCollisionTile != null){
                    realTimeCollisionTile.setCollisionData(collisionData[i][j]);
                } else {
                    RealTimeCollisionTile.create(worldTile.getX(), worldTile.getY(), worldTile.getPlane(), collisionData[i][j]);
                }
            }
        }
    }

}