package gadomancymodern.node;

import gadomancymodern.Gadomancy;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import thaumcraft.api.nodes.NodeCaptureExtensions;

public final class NodeCaptureIntegration {
    private NodeCaptureIntegration() {}
    public static void setup(){
        NodeCaptureExtensions.register("gadomancy:extended_node",0,context->{
            // Select by the owning node type, retaining the old extension NBT verbatim.
            if(context.sourceType()!=Gadomancy.EXTENDED_NODE_BE.get())return Optional.empty();
            var extra=new CompoundTag();extra.put("Gadomancy",context.saved().getCompound("Gadomancy"));
            return Optional.of(new NodeCaptureExtensions.Capture(Gadomancy.EXTENDED_JAR.get().defaultBlockState(),extra));
        });
    }
}
