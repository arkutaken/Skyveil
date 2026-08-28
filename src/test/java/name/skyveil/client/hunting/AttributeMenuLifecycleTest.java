package name.skyveil.client.hunting;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AttributeMenuLifecycleTest {
    @Test void replacingAnAttributePagePreservesTheFilteredPageAccumulator(){
        assertTrue(AttributeMenuLifecycle.preserveOnScreenReplacement());
        assertFalse(AttributeMenuLifecycle.shouldReset(true));
    }

    @Test void actuallyLeavingTheAttributeMenuClearsTheAccumulator(){
        assertTrue(AttributeMenuLifecycle.shouldReset(false));
    }
}
