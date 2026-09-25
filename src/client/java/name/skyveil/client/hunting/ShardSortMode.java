package name.skyveil.client.hunting;

/** Sort modes for the retained local Attribute Menu progress panel. */
public enum ShardSortMode {
    RARITY("Rarity"), QUANTITY("Qty"), PRICE("Price");

    private final String label;
    ShardSortMode(String label){this.label=label;}
    public String label(){return label;}
    // Persist enum names, not translated labels; old/invalid values use rarity sorting.
    public static ShardSortMode parse(String value){
        try{return valueOf(value);}catch(Exception ignored){return RARITY;}
    }
}
