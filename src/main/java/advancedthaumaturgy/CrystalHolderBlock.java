package advancedthaumaturgy;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import tc4revival.content.WandInteractable;

/** The accessible AT2-30r structure prototype. It has no machine/harvesting behavior. */
public class CrystalHolderBlock extends Block implements WandInteractable {
    public CrystalHolderBlock(){super(Properties.of().strength(1.5f).requiresCorrectToolForDrops());}
    public static Map<BlockPos,Boolean> structure(BlockPos holder){
        Map<BlockPos,Boolean> positions=new LinkedHashMap<>();
        for(int x=-1;x<=1;x++)for(int y=-4;y<=-2;y++)for(int z=-1;z<=1;z++)positions.put(holder.offset(x,y,z),y==-2);
        for(var direction:net.minecraft.core.Direction.Plane.HORIZONTAL){positions.put(holder.below(4).relative(direction,2),true);positions.put(holder.below().relative(direction),true);}
        return positions;
    }
    @Override public InteractionResult onWandUseFirst(ItemStack wand,UseOnContext context){
        if(!ATConfig.enabled("microlith"))return InteractionResult.PASS;
        Level level=context.getLevel();Player player=context.getPlayer();if(player==null)return InteractionResult.PASS;
        BlockPos origin=context.getClickedPos();var positions=structure(origin);
        if(!level.mayInteract(player,origin))return InteractionResult.FAIL;
        for(var p:positions.keySet()){
            if(!level.hasChunkAt(p)||!level.mayInteract(player,p)||!player.mayUseItemAt(p,context.getClickedFace(),wand))return InteractionResult.FAIL;
            boolean side=Math.abs(p.getX()-origin.getX())==2||Math.abs(p.getZ()-origin.getZ())==2||p.getY()==origin.getY()-1;
            var state=level.getBlockState(p);var id=net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(state.getBlock());
            boolean cosmetic=id!=null&&id.getNamespace().equals("thaumcraft")&&Set.of("obsidian_tile","obsidian_totem","arcane_stone","arcane_stone_bricks","thaumium_block","arcane_wood").contains(id.getPath());
            if(level.getBlockEntity(p)!=null||!(side?cosmetic:state.is(Blocks.OBSIDIAN)))return InteractionResult.FAIL;
        }
        if(!level.isClientSide){
            positions.forEach((p,model)->level.setBlockAndUpdate(p,(model?AdvancedThaumaturgy.MICROLITH_MODEL_MULTIBLOCK:AdvancedThaumaturgy.MICROLITH_MULTIBLOCK).get().defaultBlockState()));
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("advancedthaumaturgy.structure_wip"),false);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public Optional<ItemStack> onWandUse(Level level,ItemStack wand,Player player){return Optional.empty();}
    @Override public void onWandUseTick(ItemStack wand,Player player,int remaining){}
    @Override public void onWandRelease(ItemStack wand,Level level,Player player,int remaining){}
}
