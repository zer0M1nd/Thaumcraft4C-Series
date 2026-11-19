package emtmodern.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.storage.loot.LootParams;

public class ShieldBlock extends Block {
    public ShieldBlock(){super(BlockBehaviour.Properties.of().mapColor(net.minecraft.world.level.material.MapColor.STONE).strength(1,1.0e37f).noOcclusion().isValidSpawn((state,level,pos,type)->false).isRedstoneConductor((state,level,pos)->false).isSuffocating((state,level,pos)->false).isViewBlocking((state,level,pos)->false));}
    @Override public java.util.List<ItemStack> getDrops(BlockState state,LootParams.Builder loot){return java.util.List.of();}
    @Override public boolean canEntityDestroy(BlockState state,BlockGetter level,BlockPos pos,Entity entity){return false;}
    @Override public void onBlockExploded(BlockState state,Level level,BlockPos pos,Explosion explosion){}
}
