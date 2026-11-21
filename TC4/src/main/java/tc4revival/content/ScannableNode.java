package tc4revival.content;

import tc4revival.aspect.AspectAmounts;

/** Node identity survives conversion, transport and release. */
public interface ScannableNode {
    AspectAmounts aspects();
    AuraNodeType nodeType();
    default AuraNodeModifier nodeModifier() { return null; }
    String nodeId();
}
