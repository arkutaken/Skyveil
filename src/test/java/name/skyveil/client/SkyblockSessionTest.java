package name.skyveil.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkyblockSessionTest {
    @Test void recognizesSkyblockSidebarRegardlessOfDecorationOrCase(){
        assertTrue(SkyblockSession.matchesSkyblockTitle("§6✦ SkyBlock ✦"));
        assertTrue(SkyblockSession.matchesSkyblockTitle("skyblock"));
        assertFalse(SkyblockSession.matchesSkyblockTitle("HYPIXEL LOBBY"));
        assertFalse(SkyblockSession.matchesSkyblockTitle(null));
    }

    @Test void acceptsOnlyHypixelHosts(){
        assertTrue(SkyblockSession.matchesHypixelAddress("mc.hypixel.net"));
        assertTrue(SkyblockSession.matchesHypixelAddress("proxy.hypixel.net:25565"));
        assertFalse(SkyblockSession.matchesHypixelAddress("hypixel.net.example.com"));
        assertFalse(SkyblockSession.matchesHypixelAddress("localhost"));
    }
}
