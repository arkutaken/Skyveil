package name.skyveil.client.hunting;

/** Defines when the transient filtered-page accumulator may be discarded. */
final class AttributeMenuLifecycle {
    private AttributeMenuLifecycle(){}
    static boolean shouldReset(boolean attributeMenuCurrentlyOpen){return !attributeMenuCurrentlyOpen;}
    // Paging replaces Screen objects; that is not the same as leaving the menu.
    static boolean preserveOnScreenReplacement(){return true;}
}
