package ttmodern;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=Tinkerer.ID)
public final class AwakenedMining {
    private record Pending(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,BlockPos origin,BlockState before,Direction face,ItemStack tool,int mode,long tick){}
    private static final Map<UUID,Pending> PENDING=new HashMap<>();
    private static final Set<UUID> ACTIVE=new HashSet<>();
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void speed(PlayerEvent.BreakSpeed event){
        var player=event.getEntity();
        if(!event.isCanceled()&&TinkererConfig.KAMI.get()&&TinkererConfig.BEDROCK_ENABLED.get()
                &&player.level().dimension().equals(BedrockTravel.DIMENSION)
                &&player.getMainHandItem().is(Tinkerer.AWAKENED_PICK.get())
                &&BedrockOreFeature.generatedOre(player.level(),event.getState()))event.setNewSpeed(Float.MAX_VALUE);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void breaking(BlockEvent.BreakEvent event){
        if(!(event.getPlayer() instanceof ServerPlayer player)||ACTIVE.contains(player.getUUID())||event.isCanceled())return;
        var stack=player.getMainHandItem();if(!(stack.getItem() instanceof AwakenedMiningTool tool)||!tool.effective(event.getState()))return;
        int mode=AwakenedModes.mode(stack);if(mode==0||(tool.kind().equals("pick")&&player.isShiftKeyDown()))return;
        var end=player.getEyePosition().add(player.getLookAngle().scale(player.getBlockReach()+1));var hit=player.level().clip(new ClipContext(player.getEyePosition(),end,ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,player));
        Direction face=hit.getType()==HitResult.Type.BLOCK&&hit.getBlockPos().equals(event.getPos())?hit.getDirection():Direction.getNearest(-player.getLookAngle().x,-player.getLookAngle().y,-player.getLookAngle().z);
        PENDING.put(player.getUUID(),new Pending(player.level().dimension(),event.getPos().immutable(),event.getState(),face,stack,mode,player.level().getGameTime()));
    }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event){
        if(event.phase!=TickEvent.Phase.END||!(event.player instanceof ServerPlayer player))return;
        var pending=PENDING.remove(player.getUUID());if(pending==null||!pending.dimension.equals(player.level().dimension())||player.level().getGameTime()-pending.tick>1||player.getMainHandItem()!=pending.tool||player.level().getBlockState(pending.origin).equals(pending.before))return;
        mine(player,pending.origin,pending.face,pending.tool,pending.mode,pending.before);
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event){PENDING.remove(event.getEntity().getUUID());ACTIVE.remove(event.getEntity().getUUID());}
    static int mine(ServerPlayer player,BlockPos origin,Direction face,ItemStack stack,int mode,BlockState original){
        if(!(stack.getItem() instanceof AwakenedMiningTool tool)||!ACTIVE.add(player.getUUID()))return 0;
        try{
            var world=player.serverLevel();List<BlockPos> positions;
            if(tool.kind().equals("axe")&&mode==2){if(!original.is(BlockTags.LOGS))return 0;positions=tree(player,origin,original);}
            else positions=AwakenedArea.select(tool.kind(),mode,origin,face);
            int broken=0;
            for(var pos:positions){
                if(player.getMainHandItem()!=stack||!world.hasChunkAt(pos)||!world.getWorldBorder().isWithinBounds(pos)||!world.mayInteract(player,pos))continue;
                var state=world.getBlockState(pos);
                if(tool.kind().equals("pick")&&state.is(net.minecraft.world.level.block.Blocks.BEDROCK)&&world.dimension().equals(BedrockTravel.DIMENSION)){
                    if(BedrockTravel.breakBedrock(player,pos))broken++;continue;
                }
                if(state.isAir()||state.getDestroySpeed(world,pos)<0||!tool.effective(state)||(!player.isCreative()&&!state.canHarvestBlock(world,pos,player)))continue;
                if(player.gameMode.destroyBlock(pos))broken++;
            }
            if(broken>0){var box=new AABB(origin);for(var selected:positions)box=box.minmax(new AABB(selected));if(tool.kind().equals("axe")&&mode==2)box=new AABB(origin.offset(-5,-1,-5),origin.offset(6,65,6));
                for(var drop:world.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,box))drop.teleportTo(player.getX(),player.getY()+1,player.getZ());}
            return broken;
        }finally{ACTIVE.remove(player.getUUID());}
    }
    private static List<BlockPos> tree(ServerPlayer player,BlockPos origin,BlockState original){
        var world=player.serverLevel();var found=new LinkedHashSet<BlockPos>();var queue=new ArrayDeque<BlockPos>();queue.add(origin);found.add(origin);
        // Finite connected-tree flood, including diagonal branches, never forces chunk loads.
        while(!queue.isEmpty()&&found.size()<4096){var pos=queue.remove();for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++)for(int z=-1;z<=1;z++){
            var next=pos.offset(x,y,z);if(found.contains(next)||Math.abs(next.getX()-origin.getX())>16||Math.abs(next.getZ()-origin.getZ())>16||next.getY()<origin.getY()-1||next.getY()>origin.getY()+64||!world.hasChunkAt(next))continue;
            if(world.getBlockState(next).is(original.getBlock())){found.add(next);queue.add(next);}
        }}
        found.remove(origin);var sorted=new ArrayList<>(found);sorted.sort(Comparator.comparingInt((BlockPos pos)->pos.getY()).reversed());return sorted;
    }
    private AwakenedMining(){}
}
