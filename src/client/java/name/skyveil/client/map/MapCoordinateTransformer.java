package name.skyveil.client.map;

/** The only world-X/Z to map/screen conversion used by both map views. */
public final class MapCoordinateTransformer {
    private MapCoordinateTransformer() {}

    public static double normalizedX(SkyblockMapDefinition map,double worldX){return (worldX-map.minX)/(map.maxX-map.minX);}
    public static double normalizedZ(SkyblockMapDefinition map,double worldZ){return (worldZ-map.minZ)/(map.maxZ-map.minZ);}
    public static double screenX(View view,double worldX){return view.x+view.width/2.0+(worldX-view.centerX)*view.pixelsPerBlock+view.panX;}
    public static double screenY(View view,double worldZ){return view.y+view.height/2.0+(worldZ-view.centerZ)*view.pixelsPerBlock+view.panY;}
    public static double worldX(View view,double screenX){return view.centerX+(screenX-view.x-view.width/2.0-view.panX)/view.pixelsPerBlock;}
    public static double worldZ(View view,double screenY){return view.centerZ+(screenY-view.y-view.height/2.0-view.panY)/view.pixelsPerBlock;}

    public record View(int x,int y,int width,int height,double centerX,double centerZ,double pixelsPerBlock,double panX,double panY) {}
}
