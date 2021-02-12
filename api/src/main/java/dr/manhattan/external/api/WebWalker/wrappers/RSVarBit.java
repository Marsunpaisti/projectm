package dr.manhattan.external.api.WebWalker.wrappers;

import dr.manhattan.external.api.M;
import net.runelite.api.Client;

public class RSVarBit {
    int value;

    private RSVarBit(int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }

    public static RSVarBit get(int varbitId){
        Client client = M.getInstance().getClient();
        return new RSVarBit(client.getVarbitValue(varbitId));
    }
}
