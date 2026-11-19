package thaumicfutures.expansion;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import tc4revival.aspect.AspectAmounts;
import tc4revival.content.*;

public class RestoredSmelter extends AlchemyFurnaceBlockEntity {
    private final boolean voidMetal;
    public RestoredSmelter(BlockPos pos,BlockState state){
        super(RestoredContent.SMELTER_ENTITY.get(),pos,state);
        voidMetal=state.is(RestoredContent.VOID_SMELTER.get());
    }
    @Override public int capacity(){return voidMetal?400:200;}
    @Override protected double smeltingSpeed(){return voidMetal?2:4;}
    @Override public int exportInterval(){return Math.max(1,super.exportInterval()/(voidMetal?2:4));}
    @Override public int alembicLimit(){return 10;}
    @Override protected AspectAmounts smeltedYield(AspectAmounts input){
        if(voidMetal)return input;
        var output=new AspectAmounts();
        for(var aspect:input.aspects()){
            int n=input.amount(aspect),whole=(int)(n*.9),extra=level.random.nextFloat()<n*.9-whole?1:0;
            if(whole+extra>0)output.add(aspect,whole+extra);
        }
        return output;
    }
    @Override public void tick(){
        if(!thaumicfutures.FutureFeatures.Group.RESTORATION.enabled())return;
        super.tick();
        if(voidMetal&&burning()&&data.get(0)>0&&level instanceof ServerLevel server&&level.getGameTime()%20==0&&level.random.nextInt(20)==0){
            var targets=level.getEntitiesOfClass(ServerPlayer.class,new net.minecraft.world.phys.AABB(worldPosition).inflate(8),p->!p.isSpectator()&&!p.getAbilities().instabuild);
            if(!targets.isEmpty()){
                var target=targets.get(level.random.nextInt(targets.size()));
                tc4revival.network.FocusEffectPacket.nodeZap(server,net.minecraft.world.phys.Vec3.atCenterOf(worldPosition),target.getEyePosition());
                target.hurt(level.damageSources().magic(),4+level.random.nextInt(4));
            }
        }
    }
    @Override public net.minecraft.network.chat.Component getDisplayName(){return net.minecraft.network.chat.Component.translatable(getBlockState().getBlock().getDescriptionId());}
    public static class Device extends AlchemyFurnaceBlock {
        @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new RestoredSmelter(pos,state);}
        @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){
            return level.isClientSide?null:createTickerHelper(type,RestoredContent.SMELTER_ENTITY.get(),(world,pos,block,entity)->entity.tick());
        }
    }
}
