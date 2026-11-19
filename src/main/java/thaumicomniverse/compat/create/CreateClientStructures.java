package thaumicomniverse.compat.create;

import com.simibubi.create.content.contraptions.Contraption;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import java.lang.reflect.*;
import java.util.Map;

/** Cached capability adapter: no symbolic references to classes removed in Create 6.0.7. */
public final class CreateClientStructures {
    private static final Field LEGACY;
    private static final Method CLIENT, BLOCK_ENTITY;
    static {
        Field legacy=null;Method client=null,blockEntity=null;
        try {
            try { legacy=Contraption.class.getField("presentBlockEntities"); }
            catch(NoSuchFieldException modern) {
                client=Contraption.class.getMethod("getOrCreateClientContraptionLazy");
                blockEntity=client.getReturnType().getMethod("getBlockEntity",BlockPos.class);
            }
        } catch(ReflectiveOperationException error) { throw new ExceptionInInitializerError(error); }
        LEGACY=legacy;CLIENT=client;BLOCK_ENTITY=blockEntity;
    }
    public static BlockEntity blockEntity(Contraption contraption,BlockPos pos) {
        try {
            if(LEGACY!=null)return (BlockEntity)((Map<?,?>)LEGACY.get(contraption)).get(pos);
            return (BlockEntity)BLOCK_ENTITY.invoke(CLIENT.invoke(contraption),pos);
        } catch(ReflectiveOperationException error) { throw new IllegalStateException("Create client structure lookup failed",error); }
    }
    private CreateClientStructures() {}
}
