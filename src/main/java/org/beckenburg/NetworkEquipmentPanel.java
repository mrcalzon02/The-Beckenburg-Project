package org.beckenburg;
import java.util.function.*;
/** Host snapshots drive the shared equipment library in a network lobby. */
public final class NetworkEquipmentPanel extends EquipmentCatalogPanel {
    public NetworkEquipmentPanel(Consumer<EquipmentRackDialog.Choice> gear,
            BiConsumer<FieldSupplies.Kind,FieldSupplies.Tier> medicine){super(gear,medicine);setName("network.equipment");}
}
