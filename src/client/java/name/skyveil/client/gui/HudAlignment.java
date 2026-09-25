package name.skyveil.client.gui;

import java.util.ArrayList;
import java.util.List;

/** Pixel alignment shared by moving and resizing HUD elements. */
public final class HudAlignment {
    public static final double SNAP_DISTANCE=6;
    public static final double MIN_SCALE=.25,MAX_SCALE=2;
    public record Rect(double x,double y,double width,double height){}
    public record Placement(int x,int y,Double verticalGuide,Double horizontalGuide){}
    private record Axis(int position,Double guide){}
    private HudAlignment(){}

    public static Placement move(Rect rect,List<Rect> others,int screenWidth,int screenHeight,boolean snap){
        Axis x=axis(rect.x,rect.width,screenWidth,targets(others,true,screenWidth),snap);
        Axis y=axis(rect.y,rect.height,screenHeight,targets(others,false,screenHeight),snap);
        return new Placement(x.position,y.position,x.guide,y.guide);
    }

    private static Axis axis(double position,double size,int screen,List<Double> targets,boolean snap){
        int maximum=(int)Math.max(0,Math.floor(screen-size));
        int result=(int)Math.max(0,Math.min(maximum,Math.round(position)));
        int original=result;
        Double guide=null;
        double best=SNAP_DISTANCE+.001;
        if(snap)for(double target:targets)for(double anchor:new double[]{0,size/2,size}){
            double desired=target-anchor;
            int candidate=(int)Math.round(desired);
            double distance=Math.abs(desired-original);
            if(candidate>=0&&candidate<=maximum&&distance<best&&Math.abs(candidate+anchor-target)<=.501){
                best=distance;guide=target;result=candidate;
            }
        }
        return new Axis(result,guide);
    }

    private static List<Double> targets(List<Rect> others,boolean horizontal,int screen){
        List<Double> targets=new ArrayList<>();
        for(var other:others){
            double start=horizontal?other.x:other.y,size=horizontal?other.width:other.height;
            targets.add(start);targets.add(start+size/2);targets.add(start+size);
        }
        targets.add(0d);targets.add(screen/2d);targets.add((double)screen);
        return targets;
    }

    public record Resize(double scale,Double verticalGuide,Double horizontalGuide){}
    public static Resize resize(double x,double y,double baseWidth,double baseHeight,double requested,List<Rect> others,
                                int screenWidth,int screenHeight,boolean snap){
        double maximum=Math.max(MIN_SCALE,Math.min(MAX_SCALE,Math.min((screenWidth-x)/baseWidth,(screenHeight-y)/baseHeight)));
        double scale=Math.max(MIN_SCALE,Math.min(maximum,requested));
        var xs=targets(others,true,screenWidth);var ys=targets(others,false,screenHeight);
        // Guides are informational during resizing; nearby edges must never quantize the scale.
        Double vertical=null,horizontal=null;
        if(snap){
            for(double target:xs)if(Math.abs(x+baseWidth*scale-target)<.501)vertical=target;
            for(double target:ys)if(Math.abs(y+baseHeight*scale-target)<.501)horizontal=target;
        }
        return new Resize(scale,vertical,horizontal);
    }

    public static double scrollScale(double scale,double scroll){
        return Math.max(MIN_SCALE,Math.min(MAX_SCALE,scale*Math.exp(scroll*.02)));
    }

    // Project mouse movement onto the element's width/height vector. One scale
    // preserves aspect ratio even when the corner is dragged diagonally.
    public static double resizeScale(double startScale,double dx,double dy,double baseWidth,double baseHeight){
        return Math.max(MIN_SCALE,Math.min(MAX_SCALE,startScale+(dx*baseWidth+dy*baseHeight)/(baseWidth*baseWidth+baseHeight*baseHeight)));
    }
}