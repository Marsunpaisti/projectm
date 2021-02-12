package dr.manhattan.external.api.filters;

import dr.manhattan.external.api.items.MItemDefinition;
import net.runelite.api.ItemDefinition;
import net.runelite.api.widgets.WidgetItem;

import java.util.function.Predicate;

public class ItemFilters {

    public static Predicate<WidgetItem> nameContainsAny(String ...strings){
        return (widgetItem -> {
            ItemDefinition def = MItemDefinition.getDef(widgetItem.getId());
            if(def == null) return false;
            for (String s : strings) {
                if (def.getName().toLowerCase().contains(s.toLowerCase())) return true;
            }
            return false;
        });
    }
}
