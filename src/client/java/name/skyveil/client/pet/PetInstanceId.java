package name.skyveil.client.pet;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;



/** Stable identity for one owned pet, independent of its changing level and XP. */
public record PetInstanceId(String value,Source source,Confidence confidence) {
    public static PetInstanceId fromMetadata(String metadata,String type,String tier,String heldItem) {
        String uuid=PetMetadata.parse(metadata).uuid();
        if(!uuid.isBlank())return new PetInstanceId(uuid.toLowerCase(Locale.ROOT),Source.UUID,Confidence.EXACT);
        // Mutable petInfo (XP, active state, held items, skins) is not an identity.
        // UUID-less copies are distinguished by the menu reconciler.
        String fallback=(type+"|"+tier).toLowerCase(Locale.ROOT);
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
