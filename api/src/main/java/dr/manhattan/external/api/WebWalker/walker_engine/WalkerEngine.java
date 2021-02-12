package dr.manhattan.external.api.WebWalker.walker_engine;
import dr.manhattan.external.api.M;
import dr.manhattan.external.api.WebWalker.walker_engine.interaction_handling.PathObjectHandler;
import dr.manhattan.external.api.WebWalker.wrappers.AccurateMouse;
import dr.manhattan.external.api.WebWalker.wrappers.RSTile;
import dr.manhattan.external.api.calc.MCalc;
import dr.manhattan.external.api.player.MPlayer;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import dr.manhattan.external.api.WebWalker.WalkingCondition;
import dr.manhattan.external.api.WebWalker.shared.PathFindingNode;
import dr.manhattan.external.api.WebWalker.walker_engine.bfs.BFS;
import dr.manhattan.external.api.WebWalker.walker_engine.local_pathfinding.PathAnalyzer;
import dr.manhattan.external.api.WebWalker.walker_engine.local_pathfinding.Reachable;
import dr.manhattan.external.api.WebWalker.walker_engine.real_time_collision.CollisionDataCollector;
import dr.manhattan.external.api.WebWalker.walker_engine.real_time_collision.RealTimeCollisionTile;

import javax.inject.Singleton;
import java.util.List;

@Slf4j
@Singleton
public class WalkerEngine{
    private static WalkerEngine walkerEngine;
    private static Client client = M.getInstance().getClient();
    private int attemptsForAction;
    private final int failThreshold;
    private boolean navigating;
    private List<RSTile> currentPath;

    public static WalkerEngine getInstance(){
        return walkerEngine != null ? walkerEngine : (walkerEngine = new WalkerEngine());
    }

    private WalkerEngine(){
        attemptsForAction = 0;
        failThreshold = 3;
        navigating = false;
        currentPath = null;
    }

    public boolean walkPath(List<RSTile> path){
        return walkPath(path, null);
    }

    public List<RSTile> getCurrentPath() {
        return currentPath;
    }

    /**
     *
     * @param path
     * @param walkingCondition
     * @return
     */
    public boolean walkPath(List<RSTile> path, WalkingCondition walkingCondition){
        if (path.size() == 0) {
            log.info("Path is empty");
            return false;
        }

        /* TODO:
        if (!handleTeleports(path)) {
            log.warn("Failed to handle teleports...");
            return false;
        }

        */


        navigating = true;
        currentPath = path;
        try {
            PathAnalyzer.DestinationDetails destinationDetails;
            resetAttempts();

            while (true) {

                if (client.getGameState() != GameState.LOGGED_IN){
                    return false;
                }

                /* TODO:
                if (ShipUtils.isOnShip()) {
                    if (!ShipUtils.crossGangplank()) {
                        log.info("Failed to exit ship via gangplank.");
                        failedAttempt();
                    }
                    WaitFor.milliseconds(50);
                    continue;
                }

                 */

                if (isFailedOverThreshhold()) {
                    log.info("Too many failed attempts");
                    return false;
                }

                destinationDetails = PathAnalyzer.furthestReachableTile(path);
                if (destinationDetails == null) {
                    log.info("Could not grab destination details.");
                    failedAttempt();
                    continue;
                }

                RealTimeCollisionTile currentNode = destinationDetails.getDestination();
                RSTile assumedNext = destinationDetails.getAssumed();

                if (destinationDetails.getState() != PathAnalyzer.PathState.FURTHEST_CLICKABLE_TILE) {
                    log.info(destinationDetails.toString());
                }

                final RealTimeCollisionTile destination = currentNode;
                if (destination.getRSTile().toWorldPoint().distanceToHypotenuse(MPlayer.location()) >= 20) {
                    log.info("Closest tile in path is not in minimap: " + destination);
                    failedAttempt();
                    continue;
                }

                CustomConditionContainer conditionContainer = new CustomConditionContainer(walkingCondition);
                switch (destinationDetails.getState()) {
                    case DISCONNECTED_PATH:
                        if (currentNode.getRSTile().toWorldPoint().distanceToHypotenuse(MPlayer.location()) > 10){
                            clickMinimap(currentNode);
                            WaitFor.milliseconds(1200, 3400);
                        }

                        /* TODO:
                        NavigationSpecialCase.SpecialLocation specialLocation = NavigationSpecialCase.getLocation(currentNode.getWorldPoint()),
                                specialLocationDestination = NavigationSpecialCase.getLocation(assumedNext);
                        if (specialLocation != null && specialLocationDestination != null) {
                            log.info(("[SPECIAL LOCATION] We are at " + specialLocation + " and our destination is " + specialLocationDestination);
                            if (!NavigationSpecialCase.handle(specialLocationDestination)) {
                                failedAttempt();
                            } else {
                                successfulAttempt();
                            }
                            break;
                        }

                         */

                        /* TODO:
                        Charter.LocationProperty
                                locationProperty = Charter.LocationProperty.getLocation(currentNode.getWorldPoint()),
                                destinationProperty = Charter.LocationProperty.getLocation(assumedNext);
                        if (locationProperty != null && destinationProperty != null) {
                            log.info(("Chartering to: " + destinationProperty);
                            if (!Charter.to(destinationProperty)) {
                                failedAttempt();
                            } else {
                                successfulAttempt();
                            }
                            break;
                        }

                         */
                        //DO NOT BREAK OUT
                    case OBJECT_BLOCKING:
                        RSTile walkingTile = Reachable.getBestWalkableTile(destination.getRSTile(), new Reachable());
                        if (isDestinationClose(destination) || (walkingTile != null ? AccurateMouse.clickMinimap(walkingTile) : clickMinimap(destination))) {
                            log.info("Handling Object...");
                            if (!PathObjectHandler.handle(destinationDetails, path)) {
                                failedAttempt();
                            } else {
                                successfulAttempt();
                            }
                            break;
                        }
                        break;

                    case FURTHEST_CLICKABLE_TILE:
                        if (clickMinimap(currentNode)) {
                            long offsetWalkingTimeout = System.currentTimeMillis() + MCalc.nextInt(2500, 4000);
                            WaitFor.condition(10000, () -> {
                                switch (conditionContainer.trigger()) {
                                    case EXIT_OUT_WALKER_SUCCESS:
                                    case EXIT_OUT_WALKER_FAIL:
                                        return WaitFor.Return.SUCCESS;
                                }

                                PathAnalyzer.DestinationDetails furthestReachable = PathAnalyzer.furthestReachableTile(path);
                                PathFindingNode currentDestination = BFS.bfsClosestToPath(path, RealTimeCollisionTile.get(destination.getX(), destination.getY(), destination.getZ()));
                                if (currentDestination == null) {
                                    log.info("Could not walk to closest tile in path.");
                                    failedAttempt();
                                    return WaitFor.Return.FAIL;
                                }
                                int indexCurrentDestination = path.indexOf(currentDestination.getRSTile());

                                PathFindingNode closestToPlayer = PathAnalyzer.closestTileInPathToPlayer(path);
                                if (closestToPlayer == null) {
                                    log.info("Could not detect closest tile to player in path.");
                                    failedAttempt();
                                    return WaitFor.Return.FAIL;
                                }
                                int indexCurrentPosition = path.indexOf(closestToPlayer.getRSTile());
                                if (furthestReachable == null) {
                                    log.info("Furthest reachable is null/");
                                    return WaitFor.Return.FAIL;
                                }
                                int indexNextDestination = path.indexOf(furthestReachable.getDestination().getRSTile());
                                if (indexNextDestination - indexCurrentDestination > 5 || indexCurrentDestination - indexCurrentPosition < 5) {
                                    return WaitFor.Return.SUCCESS;
                                }
                                if (System.currentTimeMillis() > offsetWalkingTimeout && !MPlayer.isMoving()){
                                    return WaitFor.Return.FAIL;
                                }
                                return WaitFor.milliseconds(100);
                            });
                        }
                        break;

                    case END_OF_PATH:
                        clickMinimap(destinationDetails.getDestination());
                        log.info("Reached end of path");
                        return true;
                }

                switch (conditionContainer.getResult()) {
                    case EXIT_OUT_WALKER_SUCCESS:
                        return true;
                    case EXIT_OUT_WALKER_FAIL:
                        return false;
                }

                WaitFor.milliseconds(50, 100);

            }
        } finally {
            navigating = false;
        }
    }

    boolean isNavigating() {
        return navigating;
    }

    boolean isDestinationClose(PathFindingNode pathFindingNode){
        final RSTile playerPosition = new RSTile(MPlayer.location());
        return playerPosition.distanceToDouble(new RSTile(pathFindingNode.getX(), pathFindingNode.getY(), pathFindingNode.getZ())) <= 12
                && (BFS.isReachable(RealTimeCollisionTile.get(playerPosition.getX(), playerPosition.getY(), playerPosition.getPlane()), RealTimeCollisionTile.get(pathFindingNode.getX(), pathFindingNode.getY(), pathFindingNode.getZ()), 200));
    }

    public boolean clickMinimap(PathFindingNode pathFindingNode){
        final RSTile playerPosition = new RSTile(MPlayer.location());
        if (playerPosition.distanceToDouble(pathFindingNode.getRSTile()) <= 1){
            return true;
        }
        PathFindingNode randomNearby = BFS.getRandomTileNearby(pathFindingNode);

        if (randomNearby == null){
            log.info("Unable to generate randomization.");
            return false;
        }

        log.info("Randomize(" + pathFindingNode.getX() + "," + pathFindingNode.getY() + "," + pathFindingNode.getZ() + ") -> (" + randomNearby.getX() + "," + randomNearby.getY() + "," + randomNearby.getZ() + ")");
        return AccurateMouse.clickMinimap(new RSTile(randomNearby.getX(), randomNearby.getY(), randomNearby.getZ())) || AccurateMouse.clickMinimap(new RSTile(pathFindingNode.getX(), pathFindingNode.getY(), pathFindingNode.getZ()));
    }

    private boolean resetAttempts(){
        return successfulAttempt();
    }

    private boolean successfulAttempt(){
        attemptsForAction = 0;
        return true;
    }

    private void failedAttempt(){
        log.info("Failed attempt on action.");
        WaitFor.milliseconds(450 * (attemptsForAction + 1), 850 * (attemptsForAction + 1));
        CollisionDataCollector.generateRealTimeCollision();
    }

    private boolean isFailedOverThreshhold(){
        return attemptsForAction >= failThreshold;
    }

    private class CustomConditionContainer {
        private WalkingCondition walkingCondition;
        private WalkingCondition.State result;
        CustomConditionContainer(WalkingCondition walkingCondition){
            this.walkingCondition = walkingCondition;
            this.result = WalkingCondition.State.CONTINUE_WALKER;
        }
        public WalkingCondition.State trigger(){
            result = (walkingCondition != null ? walkingCondition.action() : result);
            return result != null ? result : WalkingCondition.State.CONTINUE_WALKER;
        }
        public WalkingCondition.State getResult() {
            return result;
        }
    }

    /* TODO:
    private boolean handleTeleports(List<WorldPoint> path) {
        WorldPoint startPosition = path.get(0);
        WorldPoint playerPosition = MPlayer.location();
        if(startPosition.equals(playerPosition))
            return true;
        if(MBanking.isBankOpen()) MBanking.closeBank();
        for (Teleport teleport : Teleport.values()) {
            if (!teleport.getRequirement().satisfies()) continue;
            if(teleport.isAtTeleportSpot(startPosition) && !teleport.isAtTeleportSpot(playerPosition)){
                log.info("Using teleport method: " + teleport);
                teleport.trigger();
                return WaitFor.condition(MCalc.nextInt(3000, 20000),
                        () -> startPosition.distanceToHypotenuse(MPlayer.location()) < 10 ?
                                WaitFor.Return.SUCCESS : WaitFor.Return.IGNORE) == WaitFor.Return.SUCCESS;
            }
        }
        return true;
    }
    */
}