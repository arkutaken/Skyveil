package name.skyveil.client.gui;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class HudAlignmentTest {
    @Test void fractionalDragAndScrollAllowFineAdjustmentsBelowHalfSize(){
        assertEquals(.333,HudAlignment.resizeScale(.3,3.3,.66,100,20),.000001);
        double increased=HudAlignment.scrollScale(.3,.25);
        assertTrue(increased>.3&&increased<.302);
        assertEquals(.3,HudAlignment.scrollScale(increased,-.25),.000001);
        assertEquals(.25,HudAlignment.scrollScale(.3,-100));
        assertEquals(2,HudAlignment.scrollScale(1,100));
    }
    @Test void nearbyAlignmentGuideDoesNotChangeTheRequestedSize(){
        var resized=HudAlignment.resize(100,40,100,20,1.499,
            List.of(new HudAlignment.Rect(150,100,100,20)),500,300,true);
        assertEquals(1.499,resized.scale());
        assertEquals(250d,resized.verticalGuide());
    }
    @Test void alignsEdgesAndCentersWithOtherDisplays(){
        var other=new HudAlignment.Rect(100,100,100,20);
        var edge=HudAlignment.move(new HudAlignment.Rect(103,102,100,20),List.of(other),500,300,true);
        assertEquals(100,edge.x());assertEquals(100,edge.y());
        assertEquals(100d,edge.verticalGuide());assertEquals(100d,edge.horizontalGuide());
        var centered=HudAlignment.move(new HudAlignment.Rect(127,40,50,10),List.of(other),500,300,true);
        assertEquals(125,centered.x());assertEquals(150d,centered.verticalGuide());
    }
    @Test void connectsAdjacentEdgesAndScreenCenter(){
        var result=HudAlignment.move(new HudAlignment.Rect(203,123,80,20),
            List.of(new HudAlignment.Rect(100,100,100,20)),500,300,true);
        assertEquals(200,result.x());assertEquals(120,result.y());
        var screen=HudAlignment.move(new HudAlignment.Rect(203,141,100,20),List.of(),500,300,true);
        assertEquals(200,screen.x());assertEquals(140,screen.y());
    }
    @Test void freeMovementAndDistantElementsDoNotSnap(){
        var rect=new HudAlignment.Rect(103,102,100,20);
        var other=List.of(new HudAlignment.Rect(100,100,100,20));
        var off=HudAlignment.move(rect,other,500,300,false);
        assertEquals(103,off.x());assertNull(off.verticalGuide());
        var far=HudAlignment.move(new HudAlignment.Rect(320,210,60,10),other,500,300,true);
        assertEquals(320,far.x());assertEquals(210,far.y());assertNull(far.verticalGuide());
    }
    @Test void clampingKeepsElementsOnScreen(){
        var result=HudAlignment.move(new HudAlignment.Rect(-30,290,100,20),List.of(),500,300,true);
        assertEquals(0,result.x());assertEquals(280,result.y());
    }
    @Test void cornerResizingPreservesAspectRatioAndClampsScale(){
        assertEquals(1.5,HudAlignment.resizeScale(1,50,10,100,20),.0001);
        assertEquals(.25,HudAlignment.resizeScale(1,-1000,-1000,100,20));
        assertEquals(2,HudAlignment.resizeScale(1,1000,1000,100,20));
    }
    @Test void resizingStaysContinuousNearOtherEdgesAndRespectsScreenSpace(){
        var result=HudAlignment.resize(100,40,100,20,1.47,
            List.of(new HudAlignment.Rect(150,100,100,20)),500,300,true);
        assertEquals(1.47,result.scale());assertNull(result.verticalGuide());
        var clamped=HudAlignment.resize(400,40,100,20,2,List.of(),500,300,false);
        assertEquals(1,clamped.scale());
    }
}