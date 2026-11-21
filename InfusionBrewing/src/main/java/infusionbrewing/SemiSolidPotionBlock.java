package infusionbrewing;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.AABB;

public class SemiSolidPotionBlock extends FallingBlock {
    public SemiSolidPotionBlock(){super(BlockBehaviour.Properties.copy(Blocks.SAND).strength(.5f));}
    @Override public void neighborChanged(BlockState state,Level level,BlockPos pos,Block block,BlockPos from,boolean moving){super.neighborChanged(state,level,pos,block,from,moving);if(!level.isClientSide)level.scheduleTick(pos,this,2);}
    @Override public void tick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){
        if(!level.getBlockState(pos).is(this))return;
        var neighbors=new java.util.ArrayList<BlockPos>();
        if(level.getEntitiesOfClass(ItemEntity.class,new AABB(pos.offset(-2,-1,-2),pos.offset(3,1,3))).size()<10)
            for(var p:BlockPos.betweenClosed(pos.offset(-1,-1,-1),pos.offset(1,1,1)))if(!p.equals(pos)&&level.getBlockState(p).is(this))neighbors.add(p.immutable());
        if(!neighbors.isEmpty()){
            neighbors.forEach(p->level.removeBlock(p,false));level.removeBlock(pos,false);
            popResource(level,pos,new ItemStack(this,neighbors.size()+1));
        }else super.tick(state,level,pos,random);
    }
}
