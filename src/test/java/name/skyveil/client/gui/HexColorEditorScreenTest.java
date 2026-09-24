package name.skyveil.client.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class HexColorEditorScreenTest {
    @Test void acceptsSixDigitRgbWithOptionalHash(){
        assertEquals(0xFF55FFFF,HexColorEditorScreen.parse("#55FFFF"));
        assertEquals(0xFFFF5500,HexColorEditorScreen.parse("ff5500"));
    }
    @Test void rejectsIncompleteOrNonHexValues(){
        assertNull(HexColorEditorScreen.parse("#FFF"));
        assertNull(HexColorEditorScreen.parse("#GG0000"));
    }
}
