package name.skyveil.client.mining;

enum CorpseType {
    LAPIS("Lapis", "LAPIS_ARMOR_HELMET", 0xFF5599FF),
    TUNGSTEN("Tungsten", "MINERAL_HELMET", 0xFFCCCCCC),
    UMBER("Umber", "ARMOR_OF_YOG_HELMET", 0xFFFFAA00);

    final String label, helmet;
    final int color;
    CorpseType(String label, String helmet, int color) {
        this.label = label; this.helmet = helmet; this.color = color;
    }
    // Helmet IDs are server item identities, not visible colors or display names.
    // Unknown equipment is not enough evidence to label a corpse.
    static CorpseType fromHelmet(String id) {
        for (var type : values()) if (type.helmet.equals(id)) return type;
        return null;
    }
    static boolean isMiningArea(String line) {
        String text = line.replaceAll("\u00a7.", "").strip();
        return text.matches("(?i).*(?:⏣|Area:|Location:)\\s*(?:Glacite Mineshafts?|Mineshaft|Base Camp|Glacite Tunnels)(?:\\s.*)?");
    }
}
