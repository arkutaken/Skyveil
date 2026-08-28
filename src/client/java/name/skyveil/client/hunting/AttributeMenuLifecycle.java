package name.skyveil.client.hunting;

/** Defines when the transient filtered-page accumulator may be discarded. */
final class AttributeMenuLifecycle {
    private AttributeMenuLifecycle(){}
    static boolean shouldReset(boolean attributeMenuCurrentlyOpen){return !attributeMenuCurrentlyOpen;}
    static boolean preserveOnScreenReplacement(){return true;}
}
