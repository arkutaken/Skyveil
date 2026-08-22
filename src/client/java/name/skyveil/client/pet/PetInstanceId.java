package name.skyveil.client.pet;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Stable identity for one owned pet, independent of its changing level and XP. */
public record PetInstanceId(String value,Source source,Confidence confidence) {
    private static final Pattern STRING_UUID=Pattern.compile("(?i)(?:\\\\?[\"'])?(?:uuid|unique_?id)(?:\\\\?[\"'])?\\s*:\\s*(?:\\\\?[\"'])?([0-9a-f-]{16,36})");
    private static final Pattern INT_ARRAY_UUID=Pattern.compile("(?i)(?:\\\\?[\"'])?(?:uuid|unique_?id)(?:\\\\?[\"'])?\\s*:\\s*\\[I;([^]]+)]");

    public static PetInstanceId fromMetadata(String metadata,String type,String tier,String heldItem) {
        String raw=metadata==null?"":metadata;
        Matcher uuid=STRING_UUID.matcher(raw);
        if(uuid.find())return new PetInstanceId(uuid.group(1).toLowerCase(Locale.ROOT),Source.UUID,Confidence.EXACT);
        Matcher intUuid=INT_ARRAY_UUID.matcher(raw);
        if(intUuid.find())return new PetInstanceId(hash(intUuid.group(1).replaceAll("\\s+","")),Source.UUID,Confidence.EXACT);
        if(!raw.isBlank()) {
            String stable=raw
                .replaceAll("(?i)(?:\\\\?[\"'])?exp(?:\\\\?[\"'])?\\s*:\\s*-?[0-9]+(?:\\.[0-9]+)?(?:e[+-]?[0-9]+)?","\"exp\":0")
                .replaceAll("(?i)(?:\\\\?[\"'])?active(?:\\\\?[\"'])?\\s*:\\s*(?:true|false)","\"active\":false")
                .replaceAll("\\s+","");
            return new PetInstanceId(hash(stable),Source.COMPONENT_FINGERPRINT,Confidence.PARTIAL);
        }
        String fallback=(type+"|"+tier+"|"+heldItem).toLowerCase(Locale.ROOT);
        return new PetInstanceId(hash(fallback),Source.FALLBACK,Confidence.UNKNOWN);
    }

    /** Session-stable discriminator used only when Hypixel exposes no item UUID for identical pets. */
    public PetInstanceId disambiguated(int ordinal) {
        return new PetInstanceId(value+"#"+Math.max(1,ordinal),Source.SESSION_DISAMBIGUATED,Confidence.PARTIAL);
    }

    public boolean belongsTo(PetInstanceId base) {
        return equals(base)||value.startsWith(base.value+"#");
    }

    private static String hash(String value) {
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)),0,16);}
        catch(Exception ignored){return Integer.toUnsignedString(value.hashCode(),16);}
    }

    public enum Source {UUID,COMPONENT_FINGERPRINT,FALLBACK,SESSION_DISAMBIGUATED,LIVE_UNRESOLVED}
    public enum Confidence {EXACT,PARTIAL,UNKNOWN}
}
