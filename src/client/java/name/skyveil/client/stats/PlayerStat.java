package name.skyveil.client.stats;

public enum PlayerStat {
    HEALTH("Health","\uE010",0xFFFF5555,true), DEFENSE("Defense","\uE008",0xFF55FF55,false),
    MANA("Mana","\uE003",0xFF55FFFF,true), VITALITY("Vitality","\uE028",0xFFFF5555,true),
    SPEED("Speed","\uE022",0xFFFFFFFF,false);
    public final String label;
    public final String icon;
    public final int color;
    public final boolean resource;
    PlayerStat(String label,String icon,int color,boolean resource){this.label=label;this.icon=icon;this.color=color;this.resource=resource;}
}