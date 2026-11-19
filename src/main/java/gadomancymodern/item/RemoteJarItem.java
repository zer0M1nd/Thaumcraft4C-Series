package gadomancymodern.item;

import gadomancymodern.Gadomancy;
import gadomancymodern.automation.RemoteJarEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class RemoteJarItem extends BlockItem implements tc4revival.content.StoredAspectItem {
    public RemoteJarItem(){super(Gadomancy.REMOTE_JAR.get(),new Properties().rarity(Rarity.UNCOMMON));}
    @Override public InteractionResult onItemUseFirst(ItemStack stack,UseOnContext context){
        if(!(context.getLevel().getBlockEntity(context.getClickedPos()) instanceof RemoteJarEntity jar)||context.getPlayer()==null)return InteractionResult.PASS;
        var player=context.getPlayer();if(!context.getLevel().mayInteract(player,context.getClickedPos())||!player.mayUseItemAt(context.getClickedPos(),context.getClickedFace(),stack))return InteractionResult.FAIL;
        if(!context.getLevel().isClientSide&&!player.isShiftKeyDown()){
            var id=jar.network();if(id==null){id=java.util.UUID.randomUUID();jar.network(id);player.displayClientMessage(Component.translatable("gadomancy.info.RemoteJar.new"),true);}
            else if(!stack.hasTag()||!stack.getTag().hasUUID("networkId")||!id.equals(stack.getTag().getUUID("networkId")))player.displayClientMessage(Component.translatable("gadomancy.info.RemoteJar.connected"),true);
            stack.getOrCreateTag().putUUID("networkId",id);
        }return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        var stack=player.getItemInHand(hand);if(!level.isClientSide&&player.isShiftKeyDown()&&stack.hasTag()&&stack.getTag().hasUUID("networkId")){stack.getTag().remove("networkId");if(stack.getTag().isEmpty())stack.setTag(null);player.displayClientMessage(Component.translatable("gadomancy.info.RemoteJar.clear"),true);}return InteractionResultHolder.pass(stack);
    }
    @Override public void appendHoverText(ItemStack stack,Level level,java.util.List<Component> lines,TooltipFlag flag){
        if(stack.hasTag()&&stack.getTag().hasUUID("networkId"))lines.add(Component.translatable("gadomancy.lore.remotejar",gadomancymodern.client.JarNetworkNames.name(stack.getTag().getUUID("networkId"))));
        tc4revival.content.TCItems.FILLED_ESSENTIA_JAR.get().appendHoverText(stack,level,lines,flag);
    }
    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer){consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions(){
        protected net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer renderer;
        @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer(){if(renderer==null){var client=net.minecraft.client.Minecraft.getInstance();renderer=new net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer(client.getBlockEntityRenderDispatcher(),client.getEntityModels()){
            protected final gadomancymodern.client.RemoteJarRenderer model=new gadomancymodern.client.RemoteJarRenderer(null);
            protected final RemoteJarEntity jar=new RemoteJarEntity(net.minecraft.core.BlockPos.ZERO,Gadomancy.REMOTE_JAR.get().defaultBlockState());
            @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,com.mojang.blaze3d.vertex.PoseStack pose,net.minecraft.client.renderer.MultiBufferSource buffers,int light,int overlay){jar.readItem(stack);jar.support(net.minecraft.core.Direction.DOWN);model.render(jar,0,pose,buffers,light,overlay);}
        };}return renderer;}
    });}
}
