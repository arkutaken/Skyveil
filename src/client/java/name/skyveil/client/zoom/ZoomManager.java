package name.skyveil.client.zoom;

import com.mojang.blaze3d.platform.InputConstants;
import name.skyveil.client.config.ConfigManager;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

/** Hold-to-zoom camera state; changes only camera FOV and consumes scroll while active. */
public final class ZoomManager {
    private static final double[] FACTORS={2,3,4,6,8,12};
    private static final int DEFAULT_LEVEL=2;
    private static KeyMapping keyMapping;
    private static int configuredKey=Integer.MIN_VALUE,level=DEFAULT_LEVEL;
    private ZoomManager(){}

    public static void initialize(KeyMapping.Category category){if(keyMapping==null)keyMapping=new KeyMapping("key.skyveil.zoom",InputConstants.Type.KEYSYM,ConfigManager.get().zoom.key,category);syncKey();}
    public static void tick(){syncKey();}
    public static boolean onScroll(double vertical){if(!active()||vertical==0)return false;level=adjustLevel(level,vertical);return true;}
    public static float modifyFov(float vanilla){return active()?zoomedFov(vanilla,level):vanilla;}
    static int adjustLevel(int current,double vertical){return Math.max(0,Math.min(FACTORS.length-1,current+(vertical>0?1:-1)));}
    static float zoomedFov(float vanilla,int selected){return (float)Math.max(1.0,vanilla/FACTORS[Math.max(0,Math.min(FACTORS.length-1,selected))]);}
    static double factor(int selected){return FACTORS[Math.max(0,Math.min(FACTORS.length-1,selected))];}
    private static boolean active(){Minecraft client=Minecraft.getInstance();return keyMapping!=null&&ConfigManager.get().zoom.enabled&&client!=null&&client.level!=null&&client.player!=null&&client.screen==null&&keyMapping.isDown();}
    private static void syncKey(){if(keyMapping==null)return;int wanted=ConfigManager.get().zoom.key;if(wanted!=configuredKey){configuredKey=wanted;keyMapping.setKey(InputConstants.Type.KEYSYM.getOrCreate(wanted));KeyMapping.resetMapping();}}
}
