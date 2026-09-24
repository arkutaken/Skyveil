package name.skyveil.client.slayer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VoidgloomOverlayTest {
    private static final String NUKEKUBI="eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZWIwNzU5NGUyZGYyNzM5MjFhNzdjMTAxZDBiZmRmYTExMTVhYmVkNWI5YjIwMjllYjQ5NmNlYmE5YmRiYjRiMyJ9fX0=";
    @Test void matchesOnlyTheDedicatedNukekubiTexture(){
        assertTrue(VoidgloomOverlay.matchesNukekubiTexture(NUKEKUBI));
        assertFalse(VoidgloomOverlay.matchesNukekubiTexture("ordinary-player-head"));
    }
    @Test void requiresTheLocalVoidgloomQuestAndBossStage(){
        assertTrue(VoidgloomOverlay.isOwnVoidgloomQuest(java.util.List.of("Voidgloom Seraph IV","Slay the boss!")));
        assertFalse(VoidgloomOverlay.isOwnVoidgloomQuest(java.util.List.of("Voidgloom Seraph IV","Boss slain!")));
        assertFalse(VoidgloomOverlay.isOwnVoidgloomQuest(java.util.List.of("Revenant Horror V","Slay the boss!")));
    }
}
