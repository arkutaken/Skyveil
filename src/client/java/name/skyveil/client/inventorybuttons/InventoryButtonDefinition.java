package name.skyveil.client.inventorybuttons;

/** Gson-friendly persistent command button definition stored in skyveil.json. */
public final class InventoryButtonDefinition {
    public String id="";
    public String command="";
    public String icon="minecraft:chest";
    public String position=InventoryButtonPosition.RIGHT_0.name();
    public boolean enabled=true;

    // Editor drafts retain the ID but own their mutable fields until Save.
    public InventoryButtonDefinition copy(){
        InventoryButtonDefinition copy=new InventoryButtonDefinition();
        copy.id=id;copy.command=command;copy.icon=icon;copy.position=position;copy.enabled=enabled;
        return copy;
    }
}
