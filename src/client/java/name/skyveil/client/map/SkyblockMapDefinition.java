package name.skyveil.client.map;

import java.util.ArrayList;
import java.util.List;

/** Static, versioned map metadata loaded from assets/skyveil/data/maps. */
public final class SkyblockMapDefinition {
    public String id="",displayName="",texture="",sourceUrl="",expectedSha256="",attribution="";
    public int imageWidth,imageHeight;
    public double minX,maxX,minZ,maxZ;
    public List<String> aliases=new ArrayList<>();
    public List<StaticMarker> npcs=new ArrayList<>();
    public List<StaticMarker> zones=new ArrayList<>();

    public boolean contains(double x,double z){return x>=minX&&x<=maxX&&z>=minZ&&z<=maxZ;}
    public double centerX(){return (minX+maxX)/2.0;}
    public double centerZ(){return (minZ+maxZ)/2.0;}

    public static final class StaticMarker {
        public String id="",name="",description="";
        public double x,y,z;
    }
}
