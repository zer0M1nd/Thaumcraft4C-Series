package thaumcraft.api.client;

import java.util.function.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import thaumcraft.api.util.ExtensionRegistry;

/** Client-only renderer composition. Overrides replace the base pass; layers always follow it. No buffer is flushed here. */
public final class DeviceRendering {
    public record Context<E extends BlockEntity>(E entity,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        public <T extends BlockEntity> Context<T> cast(Class<T> type){return new Context<>(type.cast(entity),partial,pose,buffers,light,overlay);}
    }
    private record Renderer(Predicate<BlockEntity> accepts,Consumer<Context<?>> draw) {}
    private static final ExtensionRegistry<Renderer> OVERRIDES=new ExtensionRegistry<>(),LAYERS=new ExtensionRegistry<>();
    private DeviceRendering() {}
    public static <E extends BlockEntity> void registerOverride(String id,int priority,Class<E> type,Predicate<E> accepts,Consumer<Context<E>> draw){
        OVERRIDES.register(id,priority,new Renderer(entity->type.isInstance(entity)&&accepts.test(type.cast(entity)),context->draw.accept(context.cast(type))));
    }
    public static <E extends BlockEntity> void registerLayer(String id,int priority,Class<E> type,Predicate<E> accepts,Consumer<Context<E>> draw){
        LAYERS.register(id,priority,new Renderer(entity->type.isInstance(entity)&&accepts.test(type.cast(entity)),context->draw.accept(context.cast(type))));
    }
    private static void draw(Renderer renderer,Context<?> context){context.pose().pushPose();try{renderer.draw().accept(context);}finally{context.pose().popPose();}}
    public static void render(Context<?> context,Runnable base){
        boolean replaced=false;for(var entry:OVERRIDES.entries())if(entry.value().accepts().test(context.entity())){draw(entry.value(),context);replaced=true;break;}
        if(!replaced)base.run();
        for(var entry:LAYERS.entries())if(entry.value().accepts().test(context.entity()))draw(entry.value(),context);
    }
    /** Draws one node plane into caller-owned pose/buffers; no item model or GUI camera is imposed. */
    public static void nodeItem(tc4revival.content.NodeJarData data,PoseStack pose,MultiBufferSource buffers){tc4revival.client.NodeJarRenderer.itemNode(data,pose,buffers);}
}
