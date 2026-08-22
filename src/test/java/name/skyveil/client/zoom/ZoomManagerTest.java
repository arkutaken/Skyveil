package name.skyveil.client.zoom;

import name.skyveil.client.config.SkyveilConfig;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

import static org.junit.jupiter.api.Assertions.*;

class ZoomManagerTest {
    @Test void defaultsToEnabledCKey(){SkyveilConfig config=new SkyveilConfig();assertTrue(config.zoom.enabled);assertEquals(GLFW.GLFW_KEY_C,config.zoom.key);}
    @Test void scrollUpZoomsInAndDownZoomsOut(){assertEquals(3,ZoomManager.adjustLevel(2,1));assertEquals(1,ZoomManager.adjustLevel(2,-1));}
    @Test void zoomLevelIsClamped(){assertEquals(0,ZoomManager.adjustLevel(0,-1));assertEquals(5,ZoomManager.adjustLevel(5,1));}
    @Test void fovUsesSelectedMagnification(){assertEquals(35.0f,ZoomManager.zoomedFov(70,0));assertEquals(70.0/12.0,ZoomManager.zoomedFov(70,5),.0001);assertEquals(4.0,ZoomManager.factor(2));}
    @Test void extremeVanillaFovNeverBecomesInvalid(){assertEquals(1.0f,ZoomManager.zoomedFov(1,5));}
}
