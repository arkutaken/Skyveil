package name.skyveil.client.update;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ReleaseNoticeManagerTest {
    @Test void releaseNotesIgnoreBlankLinesAndWhitespace(){
        assertEquals(java.util.List.of("First change", "Second change"),ReleaseNoticeManager.parseNotes("  First change  \n\nSecond change\n"));
    }
}
