package dr.manhattan.external.api.WebWalker.wrappers;

import dr.manhattan.external.api.interact.MInteract;
import net.runelite.api.widgets.Widget;

import java.util.Arrays;
import java.util.stream.Collectors;

public class RSInterface {
    Widget widget;

    public RSInterface(Widget w) {
        this.widget = w;
    }

    public String getText() {
        return this.widget.getText();
    }

    public int getIndex() {
        return this.widget.getIndex();
    }

    public int getTextureID(){
        return this.widget.getSpriteId();
    }

    public RSInterface[] getChildren(){
        return Arrays.stream(widget.getChildren()).map(RSInterface::new).toArray(RSInterface[]::new);
    }

    public RSInterface[] getComponents(){
        return Arrays.stream(widget.getChildren()).map(RSInterface::new).toArray(RSInterface[]::new);
    }

    public String[] getActions() {
        return this.widget.getActions();
    }

    public boolean click(String ...options){
        return MInteract.Widget(this.widget, options);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof RSInterface)) return false;

        return ((RSInterface) o).widget.equals(this.widget);
    }
}
