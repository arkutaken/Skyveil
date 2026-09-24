package name.skyveil.client.gui;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;

class HudOcclusionTest {
    @Test void clipsWorkspaceAndInventoryWithoutDiscardingUncoveredHud(){
        var screen=new HudOcclusion.Rect(0,0,900,500);
        var bottom=HudOcclusion.subtract(screen,new HudOcclusion.Rect(0,0,900,350));
        assertEquals(List.of(new HudOcclusion.Rect(0,350,900,500)),bottom);
        var visible=HudOcclusion.subtract(bottom.getFirst(),new HudOcclusion.Rect(350,350,550,450));
        assertEquals(3,visible.size());
        assertEquals(115000,visible.stream().mapToInt(r->(r.right()-r.left())*(r.bottom()-r.top())).sum());
        for(var r:visible)assertTrue(r.top()>=450||r.right()<=350||r.left()>=550);
    }
    @Test void tallTooltipOutsideStoragePanelsExcludesEveryCoveredPixel(){
        var screen=new HudOcclusion.Rect(0,0,900,500);
        var tooltip=new HudOcclusion.Rect(430,-150,890,650);
        var covers=List.of(new HudOcclusion.Rect(0,0,900,200),
            new HudOcclusion.Rect(300,210,500,400),tooltip);
        var visible=HudOcclusion.visible(screen,covers);
        assertFalse(visible.isEmpty());
        for(var piece:visible)for(var cover:covers)
            assertEquals(List.of(piece),HudOcclusion.subtract(piece,cover));
        var withoutTooltip=HudOcclusion.visible(screen,covers.subList(0,2));
        assertTrue(area(withoutTooltip)>area(visible));
        var movedTooltip=HudOcclusion.visible(screen,List.of(new HudOcclusion.Rect(0,0,100,100)));
        assertEquals(440000,area(movedTooltip));
    }
    private static int area(List<HudOcclusion.Rect> rectangles){
        return rectangles.stream().mapToInt(r->(r.right()-r.left())*(r.bottom()-r.top())).sum();
    }

    @Test void handlesFullCoverageAndNonIntersection(){
        var screen=new HudOcclusion.Rect(0,0,100,100);
        assertTrue(HudOcclusion.subtract(screen,screen).isEmpty());
        assertEquals(List.of(screen),HudOcclusion.subtract(screen,new HudOcclusion.Rect(100,0,200,100)));
    }
}