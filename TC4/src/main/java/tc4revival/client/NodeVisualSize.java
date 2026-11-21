package tc4revival.client;

/** Display-only limits. Never use the clamped value for storage, draining, or conversion. */
public class NodeVisualSize {
    public static int amount(int stored) {return Math.max(0,Math.min(100,stored));}
    private NodeVisualSize(){}
}
