package dr.manhattan.external.api.WebWalker.walker_engine.interaction_handling;

import dr.manhattan.external.api.M;
import dr.manhattan.external.api.WebWalker.shared.InterfaceHelper;
import dr.manhattan.external.api.WebWalker.walker_engine.WaitFor;
import dr.manhattan.external.api.WebWalker.wrappers.Keyboard;
import dr.manhattan.external.api.WebWalker.wrappers.RSInterface;
import dr.manhattan.external.api.calc.MCalc;
import dr.manhattan.external.api.player.MPlayer;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.queries.NPCQuery;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.api.widgets.WidgetInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
public class NPCInteraction {
    public static String[] GENERAL_RESPONSES = {"Sorry, I'm a bit busy.", "OK then.", "Yes.", "Okay..."};
    private static final int
            ITEM_ACTION_INTERFACE_WINDOW = 193,
            NPC_TALKING_INTERFACE_WINDOW = 231,
            PLAYER_TALKING_INTERFACE_WINDOW = 217,
            SELECT_AN_OPTION_INTERFACE_WINDOW = 219,
            SINGLE_OPTION_DIALOGUE_WINDOW = 229;

    private static final int[] ALL_WINDOWS = new int[]{ITEM_ACTION_INTERFACE_WINDOW, NPC_TALKING_INTERFACE_WINDOW, PLAYER_TALKING_INTERFACE_WINDOW, SELECT_AN_OPTION_INTERFACE_WINDOW, SINGLE_OPTION_DIALOGUE_WINDOW};


    private static NPCInteraction instance;

    private NPCInteraction(){

    }

    private static NPCInteraction getInstance(){
        return instance != null ? instance : (instance = new NPCInteraction());
    }

    /**
     *
     * @param rsnpcFilter
     * @param talkOptions
     * @param replyAnswers
     * @return
     */
    public static boolean talkTo(Predicate<NPC> rsnpcFilter, String[] talkOptions, String[] replyAnswers) {
        if (!clickNpcAndWaitChat(rsnpcFilter, talkOptions)){
            return false;
        }
        handleConversation(replyAnswers);
        return true;
    }

    /**
     *
     * @param rsnpcFilter
     * @param options
     * @return
     */
    public static boolean clickNpcAndWaitChat(Predicate<NPC> rsnpcFilter, String... options) {
        return clickNpc(rsnpcFilter, options) && waitForConversationWindow();
    }

    public static boolean clickNpc(Predicate<NPC> rsnpcFilter, String... options) {
        NPC npc = new NPCQuery().filter(rsnpcFilter).result(M.getInstance().getClient()).nearestTo(MPlayer.get());
        if (npc == null) {
            log.info("Cannot find NPC.");
        }

        return InteractionHelper.click(npc, options);
    }

    public static boolean waitForConversationWindow(){
        Player player = MPlayer.get();
        Actor rsCharacter = null;

        if (player != null){
            rsCharacter = player.getInteracting();
        }
        return WaitFor.condition(rsCharacter != null ? WaitFor.getMovementRandomSleep(rsCharacter.getWorldLocation()) : 10000, () -> {
            if (isConversationWindowUp()) {
                return WaitFor.Return.SUCCESS;
            }
            return WaitFor.Return.IGNORE;
        }) == WaitFor.Return.SUCCESS;
    }

    public static boolean isConversationWindowUp(){
        return Arrays.stream(ALL_WINDOWS).anyMatch((w) -> M.getInstance().getClient().getWidget(w, 0) != null && !M.getInstance().getClient().getWidget(w, 0).isHidden());
    };

    public static void handleConversationRegex(String regex){
        while (true){
            if (WaitFor.condition(MCalc.nextInt(650, 800), () -> isConversationWindowUp() ? WaitFor.Return.SUCCESS : WaitFor.Return.IGNORE) != WaitFor.Return.SUCCESS){
                break;
            }

            if (getClickHereToContinue() != null){
                clickHereToContinue();
                continue;
            }

            List<RSInterface> selectableOptions = getAllOptions(regex);
            if (selectableOptions == null || selectableOptions.size() == 0){
                WaitFor.milliseconds(100);
                continue;
            }

            WaitFor.milliseconds((int)MCalc.distributedRandom(false, 350, 2250, 350, 775));
            log.info("Replying with option: " + selectableOptions.get(0).getText());
            Keyboard.typeString(selectableOptions.get(0).getIndex() + "");
            waitForNextOption();
        }
    }

    public static void handleConversation(String... options){
        log.info("Handling... " + Arrays.asList(options));
        List<String> blackList = new ArrayList<>();
        int limit = 0;
        while (limit++ < 50){
            if (WaitFor.condition(MCalc.nextInt(650, 800), () -> isConversationWindowUp() ? WaitFor.Return.SUCCESS : WaitFor.Return.IGNORE) != WaitFor.Return.SUCCESS){
                log.info("Conversation window not up.");
                break;
            }

            if (getClickHereToContinue() != null){
                clickHereToContinue();
                limit = 0;
                continue;
            }

            List<RSInterface> selectableOptions = getAllOptions(options);
            if (selectableOptions == null || selectableOptions.size() == 0){
                WaitFor.milliseconds(150);
                continue;
            }

            for (RSInterface selected : selectableOptions){
                if(blackList.contains(selected.getText())){
                    continue;
                }
                WaitFor.milliseconds((int)MCalc.distributedRandom(false, 350, 2250, 350, 775));
                log.info("Replying with option: " + selected.getText());
                blackList.add(selected.getText());
                Keyboard.typeString(selected.getIndex() + "");
                waitForNextOption();
                limit = 0;
                break;
            }
            WaitFor.milliseconds(20,40);
        }
        if(limit > 50){
            log.info("Reached conversation limit.");
        }
    }

    /**
     *
     * @return Click here to continue conversation interface
     */
    private static RSInterface getClickHereToContinue(){
        List<RSInterface> list = getConversationDetails();
        if (list == null){
            return null;
        }
        Optional<RSInterface> optional = list.stream().filter(rsInterface -> rsInterface.getText().equals("Click here to continue")).findAny();
        return optional.isPresent() ? optional.get() : null;
    }

    /**
     * Presses space bar
     */
    private static void clickHereToContinue(){
        log.info("Clicking continue.");
        Keyboard.typeKeys(' ');
        waitForNextOption();
    }

    /**
     * Waits for chat conversation text change.
     */
    private static void waitForNextOption(){
        List<String> interfaces = getAllInterfaces().stream().map(RSInterface::getText).collect(Collectors.toList());
        WaitFor.condition(5000, () -> {
            if (!interfaces.equals(getAllInterfaces().stream().map(RSInterface::getText).collect(Collectors.toList()))){
                return WaitFor.Return.SUCCESS;
            }
            return WaitFor.Return.IGNORE;
        });
    }

    /**
     *
     * @return List of all reply-able interfaces that has valid text.
     */
    private static List<RSInterface> getConversationDetails(){
        for (int window : ALL_WINDOWS){
            List<RSInterface> details = InterfaceHelper.getAllInterfaces(window).stream().filter(rsInterfaceChild -> {
                if (rsInterfaceChild.getTextureID() != -1) {
                    return false;
                }
                String text = rsInterfaceChild.getText();
                return text != null && text.length() > 0;
            }).collect(Collectors.toList());
            if (details.size() > 0) {
                log.info("Conversation Options: [" + details.stream().map(RSInterface::getText).collect(
                        Collectors.joining(", ")) + "]");
                return details;
            }
        }
        return null;
    }

    /**
     *
     * @return List of all Chat interfaces
     */
    private static List<RSInterface> getAllInterfaces(){
        ArrayList<RSInterface> interfaces = new ArrayList<>();
        for (int window : ALL_WINDOWS) {
            interfaces.addAll(InterfaceHelper.getAllInterfaces(window));
        }
        return interfaces;
    }

    /**
     *
     * @param regex
     * @return list of conversation clickable options that matches {@code regex}
     */
    private static List<RSInterface> getAllOptions(String regex){
        List<RSInterface> list = getConversationDetails();
        return list != null ? list.stream().filter(rsInterface -> rsInterface.getText().matches(regex)).collect(
                Collectors.toList()) : null;
    }

    /**
     *
     * @param options
     * @return list of conversation clickable options that is contained in options.
     */
    private static List<RSInterface> getAllOptions(String... options){
        final List<String> optionList = Arrays.stream(options).map(String::toLowerCase).collect(Collectors.toList());
        List<RSInterface> list = getConversationDetails();
        return list != null ? list.stream().filter(rsInterface -> optionList.contains(rsInterface.getText().trim().toLowerCase())).collect(
                Collectors.toList()) : null;
    }
}