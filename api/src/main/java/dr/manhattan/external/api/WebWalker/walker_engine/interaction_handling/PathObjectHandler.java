package dr.manhattan.external.api.WebWalker.walker_engine.interaction_handling;
import dr.manhattan.external.api.M;
import dr.manhattan.external.api.WebWalker.walker_engine.WaitFor;
import dr.manhattan.external.api.WebWalker.walker_engine.bfs.BFS;
import dr.manhattan.external.api.WebWalker.walker_engine.local_pathfinding.PathAnalyzer;
import dr.manhattan.external.api.WebWalker.walker_engine.local_pathfinding.Reachable;
import dr.manhattan.external.api.WebWalker.walker_engine.real_time_collision.RealTimeCollisionTile;
import dr.manhattan.external.api.WebWalker.wrappers.RSTile;
import dr.manhattan.external.api.calc.MCalc;
import dr.manhattan.external.api.filters.ItemFilters;
import dr.manhattan.external.api.objects.MObjectDefinition;
import dr.manhattan.external.api.objects.MObjects;
import dr.manhattan.external.api.player.MInventory;
import dr.manhattan.external.api.player.MPlayer;
import lombok.extern.slf4j.Slf4j;
import dr.manhattan.external.api.WebWalker.wrappers.RSVarBit;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.queries.NPCQuery;
import net.runelite.api.widgets.WidgetItem;

import javax.inject.Singleton;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class PathObjectHandler {
    private static PathObjectHandler instance;
    private final TreeSet<String> sortedOptions, sortedBlackList, sortedBlackListOptions, sortedHighPriorityOptions;

    private PathObjectHandler(){
        sortedOptions = new TreeSet<>(
                Arrays.asList("Enter", "Cross", "Pass", "Open", "Close", "Walk-through", "Use", "Pass-through", "Exit",
                        "Walk-Across", "Go-through", "Walk-across", "Climb", "Climb-up", "Climb-down", "Climb-over", "Climb over", "Climb-into", "Climb-through",
                        "Board", "Jump-from", "Jump-across", "Jump-to", "Squeeze-through", "Jump-over", "Pay-toll(10gp)", "Step-over", "Walk-down", "Walk-up","Walk-Up", "Travel", "Get in",
                        "Investigate", "Operate", "Climb-under","Jump","Crawl-down","Crawl-through","Activate","Push","Squeeze-past","Walk-Down",
                        "Swing-on", "Climb up", "Ascend", "Descend","Channel","Teleport","Pass-Through","Jump-up","Jump-down","Swing across"));

        sortedBlackList = new TreeSet<>(Arrays.asList("Coffin","Drawers","null"));
        sortedBlackListOptions = new TreeSet<>(Arrays.asList("Chop down"));
        sortedHighPriorityOptions = new TreeSet<>(Arrays.asList("Pay-toll(10gp)","Squeeze-past"));
    }

    private static PathObjectHandler getInstance(){
        return instance != null ? instance : (instance = new PathObjectHandler());
    }

    private enum SpecialObject {
        WEB("Web", "Slash", null, new SpecialCondition() {
            @Override
            boolean isSpecialLocation(PathAnalyzer.DestinationDetails destinationDetails) {
                return new MObjects()
                    .isWithinDistance(MPlayer.location(), 15)
                    .isWithinArea(LocalPoint.fromWorld(M.getInstance().getClient(), destinationDetails.getAssumed().toWorldPoint()), 1)
                    .hasName("Web")
                    .hasAction("Slash")
                    .result(M.getInstance().getClient())
                    .size() > 0;
            }
        }),
        ROCKFALL("Rockfall", "Mine", null, new SpecialCondition() {
            @Override
            boolean isSpecialLocation(PathAnalyzer.DestinationDetails destinationDetails) {
                return new MObjects()
                    .isWithinDistance(MPlayer.location(), 15)
                    .isWithinArea(LocalPoint.fromWorld(M.getInstance().getClient(), destinationDetails.getAssumed().toWorldPoint()), 1)
                    .hasName("Rockfall")
                    .hasAction("Mine")
                    .result(M.getInstance().getClient())
                    .size() > 0;
            }
        }),
        ROOTS("Roots", "Chop", null, new SpecialCondition() {
            @Override
            boolean isSpecialLocation(PathAnalyzer.DestinationDetails destinationDetails) {
                return new MObjects()
                        .isWithinDistance(MPlayer.location(), 15)
                        .isWithinArea(LocalPoint.fromWorld(M.getInstance().getClient(), destinationDetails.getAssumed().toWorldPoint()), 1)
                        .hasName("Roots")
                        .hasAction("Chop")
                        .result(M.getInstance().getClient())
                        .size() > 0;
            }
        }),
        ROCK_SLIDE("Rockslide", "Climb-over", null, new SpecialCondition() {
            @Override
            boolean isSpecialLocation(PathAnalyzer.DestinationDetails destinationDetails) {
                return new MObjects()
                        .isWithinDistance(MPlayer.location(), 15)
                        .isWithinArea(LocalPoint.fromWorld(M.getInstance().getClient(), destinationDetails.getAssumed().toWorldPoint()), 1)
                        .hasName("Rockslide")
                        .hasAction("Climb-over")
                        .result(M.getInstance().getClient())
                        .size() > 0;
            }
        }),
        ROOT("Root", "Step-over", null, new SpecialCondition() {
            @Override
            boolean isSpecialLocation(PathAnalyzer.DestinationDetails destinationDetails) {
                return new MObjects()
                        .isWithinDistance(MPlayer.location(), 15)
                        .isWithinArea(LocalPoint.fromWorld(M.getInstance().getClient(), destinationDetails.getAssumed().toWorldPoint()), 1)
                        .hasName("Root")
                        .hasAction("Step-over")
                        .result(M.getInstance().getClient())
                        .size() > 0;
            }
        }),
        BRIMHAVEN_VINES("Vines", "Chop-down", null, new SpecialCondition() {
            @Override
            boolean isSpecialLocation(PathAnalyzer.DestinationDetails destinationDetails) {
                return new MObjects()
                        .isWithinDistance(MPlayer.location(), 15)
                        .isWithinArea(LocalPoint.fromWorld(M.getInstance().getClient(), destinationDetails.getAssumed().toWorldPoint()), 1)
                        .hasName("Vines")
                        .hasAction("Chop-down")
                        .result(M.getInstance().getClient())
                        .size() > 0;
            }
        }),
        AVA_BOOKCASE ("Bookcase", "Search", new WorldPoint(3097, 3359, 0), new SpecialCondition() {
            @Override
            boolean isSpecialLocation(PathAnalyzer.DestinationDetails destinationDetails) {
                return destinationDetails.getDestination().getX() >= 3097 && destinationDetails.getAssumed().toWorldPoint().equals(new WorldPoint(3097, 3359, 0));
            }
        }),
        AVA_LEVER ("Lever", "Pull", new WorldPoint(3096, 3357, 0), new SpecialCondition() {
            @Override
            boolean isSpecialLocation(PathAnalyzer.DestinationDetails destinationDetails) {
                return destinationDetails.getDestination().getX() < 3097 && destinationDetails.getAssumed().toWorldPoint().equals(new WorldPoint(3097, 3359, 0));
            }
        }),
        ARDY_DOOR_LOCK_SIDE("Door", "Pick-lock", new WorldPoint(2565, 3356, 0), new SpecialCondition() {
            @Override
            boolean isSpecialLocation(PathAnalyzer.DestinationDetails destinationDetails) {
                return MPlayer.location().getX() >= 2565 && MPlayer.location().distanceTo(new WorldPoint(2565, 3356, 0)) < 3;
            }
        }),
        ARDY_DOOR_UNLOCKED_SIDE("Door", "Open", new WorldPoint(2565, 3356, 0), new SpecialCondition() {
            @Override
            boolean isSpecialLocation(PathAnalyzer.DestinationDetails destinationDetails) {
                return MPlayer.location().getX() < 2565 && MPlayer.location().distanceTo(new WorldPoint(2565, 3356, 0)) < 3;
            }
        }),
        YANILLE_DOOR_LOCK_SIDE("Door", "Pick-lock", new WorldPoint(2601, 9482, 0), new SpecialCondition() {
            @Override
            boolean isSpecialLocation(PathAnalyzer.DestinationDetails destinationDetails) {
                return MPlayer.location().getY() <= 9481 && MPlayer.location().distanceTo(new WorldPoint(2601, 9482, 0)) < 3;
            }
        }),
        YANILLE_DOOR_UNLOCKED_SIDE("Door", "Open", new WorldPoint(2601, 9482, 0), new SpecialCondition() {
            @Override
            boolean isSpecialLocation(PathAnalyzer.DestinationDetails destinationDetails) {
                return MPlayer.location().getY() > 9481 && MPlayer.location().distanceTo(new WorldPoint(2601, 9482, 0)) < 3;
            }
        }),
        EDGEVILLE_UNDERWALL_TUNNEL("Underwall tunnel", "Climb-into", new WorldPoint(3138, 3516, 0), new SpecialCondition() {
            @Override
            boolean isSpecialLocation(PathAnalyzer.DestinationDetails destinationDetails) {
                return destinationDetails.getAssumed().toWorldPoint().equals(new WorldPoint(3138, 3516, 0));
            }
        }),
        VARROCK_UNDERWALL_TUNNEL("Underwall tunnel", "Climb-into", new WorldPoint(3141, 3513, 0), new SpecialCondition() {
            @Override
            boolean isSpecialLocation(PathAnalyzer.DestinationDetails destinationDetails) {
                return destinationDetails.getAssumed().toWorldPoint().equals(new WorldPoint(3141, 3513, 0 ));
            }
        }),
        GAMES_ROOM_STAIRS("Stairs", "Climb-down", new WorldPoint(2899, 3565, 0), new SpecialCondition() {
            @Override
            boolean isSpecialLocation(PathAnalyzer.DestinationDetails destinationDetails) {
                return destinationDetails.getDestination().getRSTile().toWorldPoint().equals(new WorldPoint(2899, 3565, 0)) &&
                        destinationDetails.getAssumed().toWorldPoint().equals(new WorldPoint(2205, 4934, 1));
            }
        }),
        CANIFIS_BASEMENT_WALL("Wall", "Search", new WorldPoint(3480, 9836, 0),new SpecialCondition() {
            @Override
            boolean isSpecialLocation(PathAnalyzer.DestinationDetails destinationDetails) {
                return destinationDetails.getDestination().getRSTile().toWorldPoint().equals(new WorldPoint(3480, 9836, 0)) ||
                        destinationDetails.getAssumed().toWorldPoint().equals(new WorldPoint(3480, 9836, 0));
            }
        }),
        BRINE_RAT_CAVE_BOULDER("Cave", "Exit", new WorldPoint(2690, 10125, 0), new SpecialCondition() {
            @Override
            boolean isSpecialLocation(PathAnalyzer.DestinationDetails destinationDetails) {
                return destinationDetails.getDestination().getRSTile().toWorldPoint().equals(new WorldPoint(2690, 10125, 0))
                        && new NPCQuery()
                        .nameEquals("Boulder")
                        .filter((npc) -> Arrays.asList(npc.getDefinition().getActions()).contains("Roll"))
                        .result(M.getInstance().getClient())
                        .size() > 0;

            }
        });

        private String name, action;
        private WorldPoint location;
        private SpecialCondition specialCondition;

        SpecialObject(String name, String action, WorldPoint location, SpecialCondition specialCondition){
            this.name = name;
            this.action = action;
            this.location = location;
            this.specialCondition = specialCondition;
        }

        public String getName() {
            return name;
        }

        public String getAction() {
            return action;
        }

        public WorldPoint getLocation() {
            return location;
        }

        public boolean isSpecialCondition(PathAnalyzer.DestinationDetails destinationDetails){
            return specialCondition.isSpecialLocation(destinationDetails);
        }

        public static SpecialObject getValidSpecialObjects(PathAnalyzer.DestinationDetails destinationDetails){
            for (SpecialObject object : values()){
                if (object.isSpecialCondition(destinationDetails)){
                    return object;
                }
            }
            return null;
        }

    }

    private abstract static class SpecialCondition {
        abstract boolean isSpecialLocation(PathAnalyzer.DestinationDetails destinationDetails);
    }

    public static boolean handle(PathAnalyzer.DestinationDetails destinationDetails, List<RSTile> path){
        RealTimeCollisionTile start = destinationDetails.getDestination(), end = destinationDetails.getNextTile();

        GameObject[] interactiveObjects = null;

        String action = null;
        SpecialObject specialObject = SpecialObject.getValidSpecialObjects(destinationDetails);
        if (specialObject == null) {
            if ((interactiveObjects = getInteractiveObjects(start.getX(), start.getY(), start.getZ(), destinationDetails)).length < 1 && end != null) {
                interactiveObjects = getInteractiveObjects(end.getX(), end.getY(), end.getZ(), destinationDetails);
            }
        } else {
            action = specialObject.getAction();
            Predicate<GameObject> specialObjectFilter = (GameObject obj) -> {
                    ObjectDefinition def = MObjectDefinition.getDef(obj.getId());
                    if (def == null) return false;
                    return def.getName().equals(specialObject.getName()) &&
                            Arrays.asList(def.getActions()).contains(specialObject.getAction()) &&
                            obj.getWorldLocation().distanceToHypotenuse(specialObject.getLocation() != null ? specialObject.getLocation() : destinationDetails.getAssumed().toWorldPoint()) <= 1.5;
            };
            /*
            Filter<GameObject> specialObjectFilter = Filters.Objects.nameEquals(specialObject.getName())
                    .combine(Filters.Objects.actionsContains(specialObject.getAction()), true)
                    .combine(Filters.Objects.inArea(new RSArea(specialObject.getLocation() != null ? specialObject.getLocation() : destinationDetails.getAssumed(), 1)), true);


            interactiveObjects = Objects.findNearest(15, specialObjectFilter);
             */
            Client client = M.getInstance().getClient();
            interactiveObjects = new MObjects()
                    .isWithinDistance(MPlayer.location(), 15)
                    .filter(specialObjectFilter)
                    .result(client)
                    .list.toArray(GameObject[]::new);
        }

        if (interactiveObjects.length == 0) {
            return false;
        }

        StringBuilder stringBuilder = new StringBuilder("Sort Order: ");
        Arrays.stream(interactiveObjects).forEach(rsObject -> stringBuilder.append(MObjectDefinition.getDef(rsObject.getId()).getName()).append(" ").append(
                Arrays.asList(MObjectDefinition.getDef(rsObject.getId()).getActions())).append(", "));
        log.info(stringBuilder.toString());

        return handle(path, interactiveObjects[0], destinationDetails, action, specialObject);
    }

    private static boolean handle(List<RSTile> path, GameObject object, PathAnalyzer.DestinationDetails destinationDetails, String action, SpecialObject specialObject){
        PathAnalyzer.DestinationDetails current = PathAnalyzer.furthestReachableTile(path);

        if (current == null){
            return false;
        }

        RealTimeCollisionTile currentFurthest = current.getDestination();

        // TODO:
        /* Perhaps unnecessary to check in oprs
        if (!MPlayer.isMoving() && !object.isClickable()){
            if (!WalkerEngine.getInstance().clickMinimap(destinationDetails.getDestination())){
                return false;
            }
        }

        if (WaitFor.condition(General.random(5000, 8000), () -> object.isOnScreen() && object.isClickable() ? WaitFor.Return.SUCCESS : WaitFor.Return.IGNORE) != WaitFor.Return.SUCCESS) {
            return false;
        }
        */

        boolean successfulClick = false;

        if (specialObject != null) {
            log.info("Detected Special Object: " + specialObject);
            Client client = M.getInstance().getClient();
            switch (specialObject){
                case WEB:
                    List<GameObject> webs = new MObjects().atWorldLocation(object.getWorldLocation()).hasAction("Slash").result(client).list;
                    int iterations = 0;
                    while (webs.size() > 0){
                        GameObject web = webs.get(0);
                        if (canLeftclickWeb()) {
                            InteractionHelper.click(web, "Slash");
                        } else {
                            useBladeOnWeb(web);
                        }

                        /*
                        if(Game.isUptext("->")){
                            Walking.blindWalkTo(MPlayer.location());
                        }
                         */

                        if (web.getWorldLocation().distanceTo(MPlayer.location()) <= 1) {
                            WaitFor.milliseconds((int)MCalc.distributedRandom(false,50, 800, 250, 150));
                        } else {
                            WaitFor.milliseconds(2000, 4000);
                        }
                        webs = new MObjects().atWorldLocation(object.getWorldLocation()).hasAction("Slash").result(client).list;
                        if (Reachable.getMap().getParent(destinationDetails.getAssumedX(), destinationDetails.getAssumedY()) != null && (webs == null || webs.size() == 0) ){
                            successfulClick = true;
                            break;
                        }
                        if (iterations++ > 5){
                            break;
                        }
                    }
                    break;
                case ARDY_DOOR_LOCK_SIDE:
                case YANILLE_DOOR_LOCK_SIDE:
                    for (int i = 0; i < MCalc.nextInt(15, 25); i++) {
                        if (!clickOnObject(object, new String[]{specialObject.getAction()})){
                            continue;
                        }
                        if (MPlayer.location().distanceTo(specialObject.getLocation()) > 1){
                            WaitFor.condition(MCalc.nextInt(3000, 4000), () -> MPlayer.location().distanceTo(specialObject.getLocation()) <= 1 ? WaitFor.Return.SUCCESS : WaitFor.Return.IGNORE);
                        }
                        if (MPlayer.location().equals(new WorldPoint(2564, 3356, 0))){
                            successfulClick = true;
                            break;
                        }
                    }
                    break;
                case VARROCK_UNDERWALL_TUNNEL:
                    if(!clickOnObject(object,specialObject.getAction())){
                        return false;
                    }
                    successfulClick = true;
                    WaitFor.condition(10000, () ->
                            SpecialObject.EDGEVILLE_UNDERWALL_TUNNEL.getLocation().equals(MPlayer.location()) ?
                                    WaitFor.Return.SUCCESS : WaitFor.Return.IGNORE);
                    break;
                case EDGEVILLE_UNDERWALL_TUNNEL:
                    if(!clickOnObject(object,specialObject.getAction())){
                        return false;
                    }
                    successfulClick = true;
                    WaitFor.condition(10000, () ->
                            SpecialObject.VARROCK_UNDERWALL_TUNNEL.getLocation().equals(MPlayer.location()) ?
                                    WaitFor.Return.SUCCESS : WaitFor.Return.IGNORE);
                    break;
                case BRINE_RAT_CAVE_BOULDER:
                    NPC boulder = new NPCQuery()
                            .nameEquals("Boulder")
                            .filter((npc) -> Arrays.asList(npc.getDefinition().getActions()).contains("Roll"))
                            .result(client)
                            .first();
                    if (boulder == null) return false;
                    if(InteractionHelper.click(boulder, "Roll")){
                        if(WaitFor.condition(12000,
                                () -> new NPCQuery()
                                        .nameEquals("Boulder")
                                        .filter((npc) -> Arrays.asList(npc.getDefinition().getActions()).contains("Roll"))
                                        .result(client)
                                        .size() > 0 ?
                                        WaitFor.Return.SUCCESS : WaitFor.Return.IGNORE) == WaitFor.Return.SUCCESS){
                            WaitFor.milliseconds(3500, 6000);
                        }
                    }
                    break;
            }
        }

        if (!successfulClick){
            String[] validOptions = action != null ? new String[]{action} : getViableOption(
                    Arrays.stream(MObjectDefinition.getDef(object.getId()).getActions()).filter(getInstance().sortedOptions::contains).collect(
                            Collectors.toList()), destinationDetails);
            if (!clickOnObject(object, validOptions)) {
                return false;
            }
        }

        boolean strongholdDoor = isStrongholdDoor(object);

        if (strongholdDoor){
            if (WaitFor.condition(MCalc.nextInt(6700, 7800), () -> {
                WorldPoint playerPosition = MPlayer.location();
                if (BFS.isReachable(RealTimeCollisionTile.get(playerPosition.getX(), playerPosition.getY(), playerPosition.getPlane()), destinationDetails.getNextTile(), 50)) {
                    WaitFor.milliseconds(500, 1000);
                    return WaitFor.Return.SUCCESS;
                }
                if (NPCInteraction.isConversationWindowUp()) {
                    handleStrongholdQuestions();
                    return WaitFor.Return.SUCCESS;
                }
                return WaitFor.Return.IGNORE;
            }) != WaitFor.Return.SUCCESS){
                return false;
            }
        }

        WaitFor.condition(MCalc.nextInt(8500, 11000), () -> {
            DoomsToggle.handleToggle();
            PathAnalyzer.DestinationDetails destinationDetails1 = PathAnalyzer.furthestReachableTile(path);
            if (NPCInteraction.isConversationWindowUp()) {
                NPCInteraction.handleConversation(NPCInteraction.GENERAL_RESPONSES);
            }
            if (destinationDetails1 != null) {
                if (!destinationDetails1.getDestination().equals(currentFurthest)){
                    return WaitFor.Return.SUCCESS;
                }
            }
            return WaitFor.Return.IGNORE;
        });
        if (strongholdDoor){
            WaitFor.milliseconds(800, 1200);
        }
        return true;
    }

    public static GameObject[] getInteractiveObjects(int x, int y, int z, PathAnalyzer.DestinationDetails destinationDetails){
        Client client = M.getInstance().getClient();
        GameObject[] objects = new MObjects()
                .isWithinDistance(MPlayer.location(), 25)
                .filter(interactiveObjectFilter(x, y, z, destinationDetails))
                .result(client)
                .list.toArray(GameObject[]::new);

        final WorldPoint base = new WorldPoint(x, y, z);
        Arrays.sort(objects, (o1, o2) -> {
            int c = Integer.compare(Math.round(o1.getWorldLocation().distanceToHypotenuse(base)), Math.round(o2.getWorldLocation().distanceToHypotenuse(base)));
            int assumedZ = destinationDetails.getAssumedZ(), destinationZ = destinationDetails.getDestination().getZ();
            List<String> actions1 = Arrays.asList(MObjectDefinition.getDef(o1.getId()).getActions());
            List<String> actions2 = Arrays.asList(MObjectDefinition.getDef(o2.getId()).getActions());

            if (assumedZ > destinationZ){
                if (actions1.contains("Climb-up")){
                    return -1;
                }
                if (actions2.contains("Climb-up")){
                    return 1;
                }
            } else if (assumedZ < destinationZ){
                if (actions1.contains("Climb-down")){
                    return -1;
                }
                if (actions2.contains("Climb-down")){
                    return 1;
                }
            } else if(destinationDetails.getAssumed().distanceTo(destinationDetails.getDestination().getRSTile()) >= 20){
                if(actions1.contains("Climb-up") || actions1.contains("Climb-down")){
                    return -1;
                } else if(actions2.contains("Climb-up") || actions2.contains("Climb-down")){
                    return 1;
                }
            } else if(actions1.contains("Climb-up") || actions1.contains("Climb-down")){
                return 1;
            } else if(actions2.contains("Climb-up") || actions2.contains("Climb-down")){
                return -1;
            }
            return c;
        });
        StringBuilder a = new StringBuilder("Detected: ");
        Arrays.stream(objects).forEach(object -> a.append(MObjectDefinition.getDef(object.getId()).getName()).append(" "));
        log.info(a.toString());

        return objects;
    }

    /**
     * Filter that accepts only interactive objects to progress in path.
     *
     * @param x
     * @param y
     * @param z
     * @param destinationDetails context where destination is at
     * @return
     */
    private static Predicate<GameObject> interactiveObjectFilter(int x, int y, int z, PathAnalyzer.DestinationDetails destinationDetails){
        final WorldPoint position = new WorldPoint(x, y, z);
        return (GameObject obj) -> {
            ObjectDefinition def = MObjectDefinition.getDef(obj.getId());
            if (def == null){
                return false;
            }
            String name = def.getName();
            if (getInstance().sortedBlackList.contains(name)) {
                return false;
            }
            List<String> actions = Arrays.asList(def.getActions());
            if (actions.stream().anyMatch(s -> getInstance().sortedBlackListOptions.contains(s))){
                return false;
            }
            if (obj.getWorldLocation().distanceToHypotenuse(destinationDetails.getDestination().getRSTile().toWorldPoint()) > 5) {
                return false;
            }

            /* TODO:
            if (Arrays.stream(obj.getAllTiles()).noneMatch(rsTile -> rsTile.distanceTo(position) <= 2)) {
                return false;
            }
            */

            List<String> options = Arrays.asList(def.getActions());
            return options.stream().anyMatch(getInstance().sortedOptions::contains);

        };
    }

    private static String[] getViableOption(Collection<String> collection, PathAnalyzer.DestinationDetails destinationDetails){
        Set<String> set = new HashSet<>(collection);
        if (set.retainAll(getInstance().sortedHighPriorityOptions) && set.size() > 0){
            return set.toArray(new String[set.size()]);
        }
        if (destinationDetails.getAssumedZ() > destinationDetails.getDestination().getZ()){
            if (collection.contains("Climb-up")){
                return new String[]{"Climb-up"};
            }
        }
        if (destinationDetails.getAssumedZ() < destinationDetails.getDestination().getZ()){
            if (collection.contains("Climb-down")){
                return new String[]{"Climb-down"};
            }
        }
        if (destinationDetails.getAssumedY() > 5000 && destinationDetails.getDestination().getZ() == 0 && destinationDetails.getAssumedZ() == 0){
            if (collection.contains("Climb-down")){
                return new String[]{"Climb-down"};
            }
        }
        String[] options = new String[collection.size()];
        collection.toArray(options);
        return options;
    }

    private static boolean clickOnObject(GameObject object, String... options){
        boolean result;

        if (isClosedTrapDoor(object, options)){
            result = handleTrapDoor(object);
        } else {
            result = InteractionHelper.click(object, options);
            log.info("Interacting with (" +  MObjectDefinition.getDef(object.getId()).getName() + ") at " + object.getWorldLocation() + " with options: " + Arrays.toString(options) + " " + (result ? "SUCCESS" : "FAIL"));
            WaitFor.milliseconds(250,800);
        }

        return result;
    }

    private static boolean isStrongholdDoor(GameObject object){
        List<String> doorNames = Arrays.asList("Gate of War", "Rickety door", "Oozing barrier", "Portal of Death");
        return  doorNames.contains( MObjectDefinition.getDef(object.getId()).getName());
    }



    private static void handleStrongholdQuestions() {
        NPCInteraction.handleConversation("Use the Account Recovery System.",
                "No, you should never buy an account.",
                "Nobody.",
                "Don't tell them anything and click the 'Report Abuse' button.",
                "Decline the offer and report that player.",
                "Me.",
                "Only on the RuneScape website.",
                "Report the incident and do not click any links.",
                "Authenticator and two-step login on my registered email.",
                "No way! You'll just take my gold for your own! Reported!",
                "No.",
                "Don't give them the information and send an 'Abuse Report'.",
                "Don't give them my password.",
                "The birthday of a famous person or event.",
                "Through account settings on runescape.com.",
                "Secure my device and reset my RuneScape password.",
                "Report the player for phishing.",
                "Don't click any links, forward the email to reportphishing@jagex.com.",
                "Inform Jagex by emailing reportphishing@jagex.com.",
                "Don't give out your password to anyone. Not even close friends.",
                "Politely tell them no and then use the 'Report Abuse' button.",
                "Set up 2 step authentication with my email provider.",
                "No, you should never buy a RuneScape account.",
                "Do not visit the website and report the player who messaged you.",
                "Only on the RuneScape website.",
                "Don't type in my password backwards and report the player.",
                "Virus scan my device then change my password.",
                "No, you should never allow anyone to level your account.",
                "Don't give out your password to anyone. Not even close friends.",
                "Report the stream as a scam. Real Jagex streams have a 'verified' mark.",
                "Report the stream as a scam. Real Jagex streams have a 'verified' mark",
                "Read the text and follow the advice given.",
                "No way! I'm reporting you to Jagex!",
                "Talk to any banker in RuneScape.",
                "Secure my device and reset my RuneScape password.",
                "Secure my device and reset my password.",
                "Delete it - it's a fake!",
                "Use the account management section on the website.",
                "Politely tell them no and then use the 'Report Abuse' button.",
                "Through account setting on oldschool.runescape.com",
                "Through account setting on oldschool.runescape.com.",
                "Nothing, it's a fake.",
                "Only on the Old School RuneScape website.",
                "Don't share your information and report the player.");
    }


    private static boolean isClosedTrapDoor(GameObject object, String[] options){
        return  (MObjectDefinition.getDef(object.getId()).getName().equals("Trapdoor") && Arrays.asList(options).contains("Open"));
    }

    private static boolean handleTrapDoor(GameObject object){
        Client client = M.getInstance().getClient();
        if (getActions(object).contains("Open")){
            if (!InteractionHelper.click(object, "Open", () -> {
                LocatableQueryResults<GameObject> result = new MObjects().hasAction("Climb-down").isWithinDistance(object.getWorldLocation(), 2).result(client);
                if (result.size() > 0 && getActions(result.first()).contains("Climb-down")){
                    return WaitFor.Return.SUCCESS;
                }
                return WaitFor.Return.IGNORE;
            })){
                return false;
            } else {
                LocatableQueryResults<GameObject> result = new MObjects().hasAction("Climb-down").isWithinDistance(object.getWorldLocation(), 2).result(client);
                return result.size() > 0 && handleTrapDoor(result.first());
            }
        }
        log.info("Interacting with (" + MObjectDefinition.getDef(object.getId()).getName() + ") at " + object.getWorldLocation() + " with option: Climb-down");
        return InteractionHelper.click(object, "Climb-down");
    }

    public static List<String> getActions(GameObject object){
        List<String> list = new ArrayList<>();
        if (object == null){
            return list;
        }
        ObjectDefinition objectDefinition = MObjectDefinition.getDef(object.getId());
        if (objectDefinition == null){
            return list;
        }
        String[] actions = objectDefinition.getActions();
        if (actions == null){
            return list;
        }
        return Arrays.asList(actions);
    }

    private static List<Integer> SLASH_WEAPONS = new ArrayList<>(Arrays.asList(1,4,9,10,12,17,20,21));

    private static boolean canLeftclickWeb(){
        RSVarBit weaponType = RSVarBit.get(357);
        return (weaponType != null && SLASH_WEAPONS.contains(weaponType.getValue())) || MInventory.find(ItemFilters.nameContainsAny("Knife")).size() > 0;
    }

    private static boolean useBladeOnWeb(GameObject web){
        List<WidgetItem> slashitems = MInventory.find(ItemFilters.nameContainsAny("whip", "sword", "dagger", "claws", "scimitar", " axe", "knife", "halberd", "machete", "rapier"));
        if(slashitems == null || slashitems.size() == 0) return false;
        return InteractionHelper.useItemOnObject(slashitems.get(0), web);
    }

}