package name.skyveil.client.map;

/** User-owned marker persisted separately from static map definitions. */
public record CustomMapMarker(String id,String mapId,String name,double x,double y,double z) {}
