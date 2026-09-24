package name.skyveil.client.gui;

import java.util.ArrayList;
import java.util.List;

/** Non-overlapping visible pieces left when an interactive panel covers a HUD. */
public final class HudOcclusion {
    private HudOcclusion(){}
    public record Rect(int left,int top,int right,int bottom){}
    public static List<Rect> visible(Rect screen,List<Rect> covers){
        List<Rect> visible=List.of(screen);
        for(var cover:covers){
            List<Rect> next=new ArrayList<>();
            for(var area:visible)next.addAll(subtract(area,cover));
            visible=next;
        }
        return List.copyOf(visible);
    }
    public static List<Rect> subtract(Rect area,Rect cover){
        int l=Math.max(area.left,cover.left),t=Math.max(area.top,cover.top);
        int r=Math.min(area.right,cover.right),b=Math.min(area.bottom,cover.bottom);
        if(l>=r||t>=b)return List.of(area);
        List<Rect> result=new ArrayList<>();
        if(area.top<t)result.add(new Rect(area.left,area.top,area.right,t));
        if(b<area.bottom)result.add(new Rect(area.left,b,area.right,area.bottom));
        if(area.left<l)result.add(new Rect(area.left,t,l,b));
        if(r<area.right)result.add(new Rect(r,t,area.right,b));
        return List.copyOf(result);
    }
}