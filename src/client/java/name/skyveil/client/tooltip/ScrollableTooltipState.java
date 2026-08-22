package name.skyveil.client.tooltip;

import name.skyveil.client.config.ConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector2i;
import org.joml.Vector2ic;

/** Small render/input bridge for vertically positioning only oversized item tooltips. */
public final class ScrollableTooltipState {
    private static final int EDGE_MARGIN=4,SCROLL_STEP=18;
    private static Screen screen;
    private static ItemStack stack;
    private static int componentHash,slotKey,containerKey,scroll,maxScroll,lastHeight;
    private static boolean oversized;
    private static long observedAt;

    private ScrollableTooltipState(){}

    public static void observe(ItemStack displayed){
        observe(displayed,Minecraft.getInstance().screen,-1,-1);
    }

    public static void observe(ItemStack displayed,Screen current,int hoveredSlot,int containerId){
        if(!ConfigManager.get().scrollableTooltips||displayed==null||displayed.isEmpty()){reset();return;}
        int hash=ItemStack.hashItemAndComponents(displayed);
        if(current!=screen||hoveredSlot!=slotKey||containerId!=containerKey||hash!=componentHash){scroll=0;lastHeight=0;}
        screen=current;stack=displayed;slotKey=hoveredSlot;containerKey=containerId;componentHash=hash;observedAt=System.currentTimeMillis();
    }

    public static Vector2ic position(int screenHeight,int tooltipHeight,Vector2ic vanilla){
        if(!ConfigManager.get().scrollableTooltips||stack==null){reset();return vanilla;}
        int available=Math.max(1,screenHeight-EDGE_MARGIN*2);
        oversized=tooltipHeight>available;
        if(!oversized){scroll=maxScroll=lastHeight=0;return vanilla;}
        maxScroll=Math.max(0,tooltipHeight-available);
        if(lastHeight!=0&&lastHeight!=tooltipHeight)scroll=0;
        lastHeight=tooltipHeight;scroll=Math.max(0,Math.min(scroll,maxScroll));
        return new Vector2i(vanilla.x(),EDGE_MARGIN-scroll);
    }

    public static boolean scroll(Screen current,double wheelDelta){
        if(!ConfigManager.get().scrollableTooltips){reset();return false;}
        if(current!=screen||!oversized||System.currentTimeMillis()-observedAt>500||wheelDelta==0)return false;
        int next=(int)Math.round(scroll-wheelDelta*SCROLL_STEP);
        scroll=Math.max(0,Math.min(next,maxScroll));
        return true;
    }

    public static void reset(){screen=null;stack=null;componentHash=scroll=maxScroll=lastHeight=0;slotKey=containerKey=-1;oversized=false;observedAt=0;}
}
