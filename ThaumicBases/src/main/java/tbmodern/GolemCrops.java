package tbmodern;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import thaumcraft.api.golems.GolemCropRegistry;

/** Supplies the original harvest core with protected, seed-paying addon crop rules. */
public final class GolemCrops {
    public static void initialize() {
        for(var crop:Content.PLANTS)if(!crop.id().equals("ashroom"))register(crop.id(),crop.seed(),crop.stages()-1,false);
        register("aurelia_petal_block",null,0,false);
        register("briar","briar",7,true);
    }
    private static void register(String id,String seed,int age,boolean doublePlant) {
        GolemCropRegistry.register(new ResourceLocation("thaumicbases",id),0,new GolemCropRegistry.Rule() {
            public boolean matches(BlockState state) {return state.is(Content.block(id));}
            public boolean mature(ServerLevel level,BlockPos pos,BlockState state) {
                if(doublePlant) return state.getValue(DoublePlantBlock.HALF)==DoubleBlockHalf.LOWER && state.getValue(SpecialPlants.Briar.AGE)>=age
                    && level.hasChunkAt(pos.above()) && level.getBlockState(pos.above()).is(state.getBlock())
                    && level.getBlockState(pos.above()).getValue(DoublePlantBlock.HALF)==DoubleBlockHalf.UPPER
                    && level.getBlockState(pos.above()).getValue(SpecialPlants.Briar.AGE)>=age;
                return !state.hasProperty(PlantBlock.AGE)||state.getValue(PlantBlock.AGE)>=age;
            }
            public boolean harvest(GolemCropRegistry.Context context) {
                var level=context.level();var pos=context.pos();var original=context.original();
                if(!context.loaded(pos)||!level.getBlockState(pos).equals(original)||!mature(level,pos,original)||!context.allowed(pos))return false;
                if(doublePlant && !context.allowed(pos.above()))return false;
                var drops=new ArrayList<>(Block.getDrops(original,level,pos,level.getBlockEntity(pos),context.player(),ItemStack.EMPTY));
                if(doublePlant)drops.addAll(Block.getDrops(level.getBlockState(pos.above()),level,pos.above(),level.getBlockEntity(pos.above()),context.player(),ItemStack.EMPTY));
                if(!context.breakBlock(pos,false))return false;
                if(context.replant() && seed!=null) {
                    var payment=drops.stream().filter(stack->stack.is(Content.item(seed))&&!stack.isEmpty()).findFirst();
                    if(payment.isPresent() && context.place(pos,original.getBlock().defaultBlockState())) {
                        boolean placed=!doublePlant || context.place(pos.above(),original.getBlock().defaultBlockState().setValue(DoublePlantBlock.HALF,DoubleBlockHalf.UPPER));
                        if(placed)payment.get().shrink(1);
                        else level.removeBlock(pos,false); // Roll back our own half-plant; never charge a seed for failed placement.
                    }
                }
                drops.forEach(stack->Block.popResource(level,pos,stack));return true;
            }
        });
    }
    private GolemCrops() {}
}
