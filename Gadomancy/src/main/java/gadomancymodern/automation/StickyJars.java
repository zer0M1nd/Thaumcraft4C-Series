package gadomancymodern.automation;

import gadomancymodern.Gadomancy;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="gadomancy")
public class StickyJars {
    public record Placement(net.minecraft.world.InteractionHand hand,Direction side) {}
    protected static final java.util.Map<Player,Placement> PLACEMENTS=new java.util.WeakHashMap<>();
    public static boolean sticky(ItemStack stack){return StickyJarRecipe.jar(stack)&&stack.hasTag()&&stack.getTag().getBoolean("isStickyJar");}
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void remember(net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock event){
        if(!event.getLevel().isClientSide&&sticky(event.getItemStack())&&event.getFace()!=null)PLACEMENTS.put(event.getEntity(),new Placement(event.getHand(),event.getFace().getOpposite()));
    }
    @SubscribeEvent(priority=EventPriority.LOWEST,receiveCanceled=true) public static void place(net.minecraftforge.event.level.BlockEvent.EntityPlaceEvent event){
        if(!(event.getEntity() instanceof Player player)||player.level().isClientSide)return;
        Placement placement=PLACEMENTS.remove(player);if(placement==null||event.isCanceled())return;
        ItemStack stack=player.getItemInHand(placement.hand());if(!sticky(stack)||!(event.getLevel().getBlockEntity(event.getPos()) instanceof tc4revival.content.EssentiaJarBlockEntity original))return;
        var world=player.level();boolean voidJar=original.voidJar();var state=(original instanceof RemoteJarEntity?Gadomancy.REMOTE_JAR.get():Gadomancy.STICKY.get()).defaultBlockState().setValue(StickyJarBlock.VOID,voidJar);world.setBlock(event.getPos(),state,3);
        if(world.getBlockEntity(event.getPos()) instanceof StickyJarBlockEntity jar){
            jar.readItem(stack);jar.support(placement.side());jar.face(player.getDirection().getOpposite());orient(jar,player);
            // A one-tick update can merge into the initial chunk broadcast while the client
            // still predicts the ordinary jar. Its differently typed BE packet is then ignored.
            // Wait for a separate broadcast after the placement acknowledgement.
            world.scheduleTick(event.getPos(),state.getBlock(),2);
        }
    }
    public static void orient(StickyJarBlockEntity jar,Player player){
        if(jar.support().getAxis()==Direction.Axis.Y)return;
        float pitch=player.getXRot(),yaw=net.minecraft.util.Mth.wrapDegrees(player.getYRot());
        yaw=switch(jar.support()){case WEST->yaw-90;case NORTH->(180-Math.abs(yaw))*(yaw<0?1:-1);case EAST->yaw+90;default->yaw;};
        jar.face(Math.abs(yaw)<Math.abs(pitch)?(pitch<0?Direction.SOUTH:Direction.NORTH):(yaw<0?Direction.EAST:Direction.WEST));
    }
    @SubscribeEvent public static void tooltip(net.minecraftforge.event.entity.player.ItemTooltipEvent event){if(sticky(event.getItemStack()))event.getToolTip().add(Math.min(1,event.getToolTip().size()),net.minecraft.network.chat.Component.translatable("gadomancy.lore.stickyjar").withStyle(net.minecraft.ChatFormatting.GREEN));}
}
