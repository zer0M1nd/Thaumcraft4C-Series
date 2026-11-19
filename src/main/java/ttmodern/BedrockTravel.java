package ttmodern;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid=Tinkerer.ID)
public final class BedrockTravel {
    public static final ResourceKey<Level> DIMENSION=ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,ResourceLocation.parse(Tinkerer.ID+":bedrock"));
    static boolean enabled(){return TinkererConfig.KAMI.get()&&TinkererConfig.BEDROCK_ENABLED.get();}
    static boolean surface(Level world){return world.dimensionType().natural()&&world.dimensionType().hasSkyLight();}
    static boolean replace(Player player,BlockPos pos,BlockState state,boolean breaking){
        var world=player.level();if(world.isClientSide||player.isSpectator()||!world.hasChunkAt(pos)||!world.getWorldBorder().isWithinBounds(pos)||!world.mayInteract(player,pos)||!player.mayBuild())return false;
        var before=world.getBlockState(pos);if(breaking&&net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.level.BlockEvent.BreakEvent(world,pos,before,player)))return false;
        var snapshot=net.minecraftforge.common.util.BlockSnapshot.create(world.dimension(),world,pos);if(!world.setBlock(pos,state,2))return false;
        if(net.minecraftforge.event.ForgeEventFactory.onBlockPlace(player,snapshot,net.minecraft.core.Direction.UP)){snapshot.restore(true,false);return false;}world.updateNeighborsAt(pos,state.getBlock());return true;
    }
    @net.minecraftforge.eventbus.api.SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.HIGHEST)
    public static void click(net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock event){
        if(event.getAction()!=net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock.Action.START||!(event.getEntity() instanceof ServerPlayer player)||!player.getMainHandItem().is(Tinkerer.AWAKENED_PICK.get())||!player.level().getBlockState(event.getPos()).is(Blocks.BEDROCK))return;
        var pos=event.getPos();if(player.distanceToSqr(pos.getCenter())>Math.pow(player.getBlockReach()+1,2))return;
        boolean bedrock=player.level().dimension().equals(DIMENSION);boolean portal=bedrock?pos.getY()>=254:player.level().dimension().equals(Level.OVERWORLD)&&pos.getY()<player.level().getMinBuildHeight()+5;
        if(!enabled()&&!(bedrock&&portal))return;
        if(portal||bedrock){
            event.setCanceled(true);
            if(portal)replace(player,pos,Tinkerer.BEDROCK_PORTAL.get().defaultBlockState(),true);
            else if(breakBedrock(player,pos))AwakenedMining.mine(player,pos,event.getFace(),player.getMainHandItem(),AwakenedModes.mode(player.getMainHandItem()),Blocks.BEDROCK.defaultBlockState());
        }
    }
    static boolean breakBedrock(ServerPlayer player,BlockPos pos){
        if(!player.level().dimension().equals(DIMENSION)||pos.getY()<2||pos.getY()>=254)return false;
        return replace(player,pos,Blocks.AIR.defaultBlockState(),true);
    }
    static net.minecraft.world.InteractionResult usePick(net.minecraft.world.item.context.UseOnContext context) {
        var player=context.getPlayer();if(player==null)return net.minecraft.world.InteractionResult.PASS;
        return AwakenedModes.use(context.getLevel(),player,context.getHand(),"pick").getResult();
    }
    static boolean travel(ServerPlayer player,BlockPos source){
        if(!player.isAlive()||player.isSpectator()||player.isPassenger()||player.isVehicle()||player.isOnPortalCooldown())return false;
        var world=player.serverLevel();boolean entering=!world.dimension().equals(DIMENSION);if(entering&&(!enabled()||!surface(world)))return false;
        var target=player.server.getLevel(entering?DIMENSION:Level.OVERWORLD);if(target==null)return false;
        if(!net.minecraftforge.common.ForgeHooks.onTravelToDimension(player,target.dimension()))return false;
        double x,z,y;
        if(entering){
            var pos=new BlockPos(source.getX(),249,source.getZ());if(!target.getWorldBorder().isWithinBounds(pos))return false;target.getChunkAt(pos);
            // Never clear player storage or overwrite an existing structure to make the entry shaft.
            for(int i=1;i<=5;i++){var state=target.getBlockState(pos.above(i));if(!state.isAir()&&!state.is(Blocks.STONE)&&!state.is(Blocks.BEDROCK)&&!state.is(Tinkerer.BEDROCK_PORTAL.get()))return false;}
            if(!prepareEntry(target,player,pos))return false;x=pos.getX()+.5;y=251;z=pos.getZ()+.5;
        }else{
            var landing=returnPosition(target,player,source);if(landing==null)return false;x=landing.getX()+.5;y=landing.getY();z=landing.getZ()+.5;
        }
        player.setPortalCooldown();player.teleportTo(target,x,y,z,player.getYRot(),player.getXRot());player.fallDistance=0;return player.level()==target;
    }
    static boolean prepareEntry(ServerLevel target,ServerPlayer player,BlockPos floor){
        if(!target.getWorldBorder().isWithinBounds(floor)||target.isOutsideBuildHeight(floor.above(5)))return false;
        for(int i=1;i<=5;i++){var state=target.getBlockState(floor.above(i));if(!state.isAir()&&!state.is(Blocks.STONE)&&!state.is(Blocks.BEDROCK)&&!state.is(Tinkerer.BEDROCK_PORTAL.get()))return false;}
        if(!target.isEmptyBlock(floor)&&!target.getBlockState(floor).isFaceSturdy(target,floor,net.minecraft.core.Direction.UP))return false;
        var snapshots=new java.util.ArrayList<net.minecraftforge.common.util.BlockSnapshot>();
        for(int i=0;i<=5;i++){
            var pos=floor.above(i);if(!target.mayInteract(player,pos))return false;var state=target.getBlockState(pos);
            if(i==0&&!state.isAir())continue;if(i==5&&!state.is(Blocks.BEDROCK))continue;
            snapshots.add(net.minecraftforge.common.util.BlockSnapshot.create(target.dimension(),target,pos));
        }
        boolean success=false;try{
            for(var snapshot:snapshots){var pos=snapshot.getPos();var state=pos.equals(floor)?Blocks.STONE.defaultBlockState():pos.equals(floor.above(5))?Tinkerer.BEDROCK_PORTAL.get().defaultBlockState():Blocks.AIR.defaultBlockState();if(!target.setBlock(pos,state,2)&&!target.getBlockState(pos).equals(state))return false;}
            if(!snapshots.isEmpty()&&net.minecraftforge.event.ForgeEventFactory.onMultiBlockPlace(player,snapshots,net.minecraft.core.Direction.UP))return false;success=true;for(var snapshot:snapshots)target.updateNeighborsAt(snapshot.getPos(),target.getBlockState(snapshot.getPos()).getBlock());return true;
        }finally{if(!success)for(int i=snapshots.size()-1;i>=0;i--)snapshots.get(i).restore(true,false);}
    }
    static BlockPos returnPosition(ServerLevel target,ServerPlayer player,BlockPos source){
        for(int attempt=0;attempt<32;attempt++){
            int x=source.getX()+target.random.nextInt(100)-50,z=source.getZ()+target.random.nextInt(100)-50;var column=new BlockPos(x,0,z);if(!target.getWorldBorder().isWithinBounds(column))continue;target.getChunkAt(column);
            int y=target.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z);var pos=new BlockPos(x,y,z);if(y<target.getMinBuildHeight()+1||y+2>=target.getMaxBuildHeight())continue;
            if(target.getBlockState(pos.below()).isFaceSturdy(target,pos.below(),net.minecraft.core.Direction.UP)&&target.getFluidState(pos).isEmpty()&&target.noCollision(player,player.getBoundingBox().move(x+.5-player.getX(),y-player.getY(),z+.5-player.getZ())))return pos;
        }return null;
    }
}
