package name.skyveil.client.inventorybuttons;

import java.util.Optional;

/** Logical positions recalculated from the current container origin and GUI scale. */
public enum InventoryButtonPosition {
    LEFT_0(Side.LEFT,0),LEFT_1(Side.LEFT,1),LEFT_2(Side.LEFT,2),LEFT_3(Side.LEFT,3),LEFT_4(Side.LEFT,4),LEFT_5(Side.LEFT,5),LEFT_6(Side.LEFT,6),LEFT_7(Side.LEFT,7),
    RIGHT_0(Side.RIGHT,0),RIGHT_1(Side.RIGHT,1),RIGHT_2(Side.RIGHT,2),RIGHT_3(Side.RIGHT,3),RIGHT_4(Side.RIGHT,4),RIGHT_5(Side.RIGHT,5),RIGHT_6(Side.RIGHT,6),RIGHT_7(Side.RIGHT,7),
    TOP_0(Side.TOP,0),TOP_1(Side.TOP,1),TOP_2(Side.TOP,2),TOP_3(Side.TOP,3),TOP_4(Side.TOP,4),TOP_5(Side.TOP,5),TOP_6(Side.TOP,6),TOP_7(Side.TOP,7),
    BOTTOM_0(Side.BOTTOM,0),BOTTOM_1(Side.BOTTOM,1),BOTTOM_2(Side.BOTTOM,2),BOTTOM_3(Side.BOTTOM,3),BOTTOM_4(Side.BOTTOM,4),BOTTOM_5(Side.BOTTOM,5),BOTTOM_6(Side.BOTTOM,6),BOTTOM_7(Side.BOTTOM,7);

    public enum Side { LEFT,RIGHT,TOP,BOTTOM }
    public final Side side;public final int index;
    InventoryButtonPosition(Side side,int index){this.side=side;this.index=index;}

    // Positions are relative to the current container, not fixed screen pixels.
    // Omit controls that cannot fit fully on screen rather than clipping their hitbox.
    public Optional<Rect> rectangle(int guiLeft,int guiTop,int guiWidth,int guiHeight,int screenWidth,int screenHeight,double scale){
        int size=Math.max(15,(int)Math.round(20*scale)),gap=2,pitch=size+gap,count=8;
        int x,y;
        if(side==Side.LEFT||side==Side.RIGHT){
            int start=guiTop+(guiHeight-(count*size+(count-1)*gap))/2;
            x=side==Side.LEFT?guiLeft-gap-size:guiLeft+guiWidth+gap;y=start+index*pitch;
        }else{
            int start=guiLeft+(guiWidth-(count*size+(count-1)*gap))/2;
            x=start+index*pitch;y=side==Side.TOP?guiTop-gap-size:guiTop+guiHeight+gap;
        }
        return x>=2&&y>=2&&x+size<=screenWidth-2&&y+size<=screenHeight-2?Optional.of(new Rect(x,y,size)):Optional.empty();
    }
    public static InventoryButtonPosition parse(String value){try{return valueOf(value);}catch(Exception ignored){return RIGHT_0;}}
    public record Rect(int x,int y,int size){public boolean contains(double mouseX,double mouseY){return mouseX>=x&&mouseX<x+size&&mouseY>=y&&mouseY<y+size;}}
}
