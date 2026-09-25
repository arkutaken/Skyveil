package name.skyveil.client.hunting;

import name.skyveil.client.gui.SkyveilTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Compact sort bar. Only its own bounds participate in mouse handling. */
public final class ShardSortControls {
    public static final int HEIGHT=16;
    private ShardSortControls(){}

    public static void render(GuiGraphicsExtractor graphics,int x,int y,int width,ShardSortMode active,boolean descending,int mouseX,int mouseY){
        int count=ShardSortMode.values().length,gap=2,buttonWidth=(width-gap*(count-1))/count;
        for(int index=0;index<ShardSortMode.values().length;index++){
            ShardSortMode mode=ShardSortMode.values()[index];int bx=x+index*(buttonWidth+gap),bw=index==count-1?x+width-bx:buttonWidth;
            boolean selected=mode==active,hover=inside(mouseX,mouseY,bx,y,bw,HEIGHT);
            graphics.fill(bx,y,bx+bw,y+HEIGHT,selected?SkyveilTheme.HOVER:0xB8241C30);
            graphics.outline(bx,y,bw,HEIGHT,selected?SkyveilTheme.ACCENT:SkyveilTheme.OUTLINE);
            String text=mode.label()+(selected?(descending?" v":" ^"):"");
            graphics.centeredText(Minecraft.getInstance().font,text,bx+bw/2,y+4,hover?0xFFFFFFFF:SkyveilTheme.TEXT);
        }
    }

    // Clicking the active column reverses direction; a newly selected column
    // starts descending. Null leaves clicks outside these controls unconsumed.
    public static Selection click(double mouseX,double mouseY,int x,int y,int width,ShardSortMode active,boolean descending){
        int count=ShardSortMode.values().length,gap=2,buttonWidth=(width-gap*(count-1))/count;
        for(int index=0;index<ShardSortMode.values().length;index++){
            int bx=x+index*(buttonWidth+gap),bw=index==count-1?x+width-bx:buttonWidth;
            if(inside(mouseX,mouseY,bx,y,bw,HEIGHT)){
                ShardSortMode selected=ShardSortMode.values()[index];
                return new Selection(selected,selected==active?!descending:true);
            }
        }
        return null;
    }

    private static boolean inside(double px,double py,int x,int y,int width,int height){return px>=x&&px<x+width&&py>=y&&py<y+height;}
    public record Selection(ShardSortMode mode,boolean descending){}
}
