package dr.manhattan.external.api.banking;

import dr.manhattan.external.api.M;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;

public class MBanking {
    public static boolean isBankOpen()
    {
        Client client = M.getInstance().getClient();
        return client.getItemContainer(InventoryID.BANK) != null;
    }

}
