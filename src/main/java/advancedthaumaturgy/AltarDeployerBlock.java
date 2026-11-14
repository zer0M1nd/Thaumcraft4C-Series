package advancedthaumaturgy;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import tc4revival.content.*;
public class AltarDeployerBlock extends Block {
    public AltarDeployerBlock(){super(Properties.of().strength(2));}
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(!player.getAbilities().instabuild||!(player.getItemInHand(hand).getItem() instanceof CastingWandItem)||!ATConfig.enabled("altar_deployer"))return InteractionResult.PASS;
        Map<BlockPos,BlockState> structure=new LinkedHashMap<>();structure.put(pos,TCArcane.ARCANE_PEDESTAL.get().defaultBlockState());
        for(int[] p:new int[][]{{-4,-1},{-4,1},{4,-1},{4,1},{-1,4},{1,4},{-1,-4},{1,-4},{3,-3},{3,3},{-3,-3},{-3,3}})structure.put(pos.offset(p[0],0,p[1]),TCArcane.ARCANE_PEDESTAL.get().defaultBlockState());
        for(int x:new int[]{-1,1})for(int z:new int[]{-1,1}){structure.put(pos.offset(x,0,z),TCArcane.INFUSION_PILLAR.get().defaultBlockState());structure.put(pos.offset(x,1,z),TCArcane.INFUSION_PILLAR_TOP.get().defaultBlockState());}
        structure.put(pos.above(2),TCArcane.INFUSION_MATRIX.get().defaultBlockState());
        for(int x=-8;x<=8;x++)for(int z=-8;z<=8;z++)structure.put(pos.offset(x,-2,z),TCArcane.candle(0).defaultBlockState());
        for(var p:structure.keySet())if(!level.hasChunkAt(p)||!level.mayInteract(player,p)||!player.mayUseItemAt(p,hit.getDirection(),player.getItemInHand(hand))||(!p.equals(pos)&&(!level.getBlockState(p).canBeReplaced()||level.getBlockEntity(p)!=null)))return InteractionResult.FAIL;
        if(!level.isClientSide){structure.forEach((p,s)->level.setBlockAndUpdate(p,s));if(level.getBlockEntity(pos.above(2)) instanceof InfusionMatrixBlockEntity matrix&&matrix.validLocation())matrix.activate();}return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
