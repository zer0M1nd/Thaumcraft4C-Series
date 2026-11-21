package infusionbrewing;

import net.minecraft.core.*;
import net.minecraft.core.dispenser.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;

public final class BrewingDispensers {
    private BrewingDispensers(){}
    public static void register(){
        DispenserBlock.registerBehavior(InfusionBrewing.MAGIC_BOTTLE.get(),new DefaultDispenseItemBehavior(){
            @Override protected ItemStack execute(BlockSource source,ItemStack stack){
                var pos=front(source);var fluid=source.getLevel().getFluidState(pos);int id=PotionFluids.id(fluid);
                if(id==0||!fluid.isSource())return stack;
                source.getLevel().setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
                // Keep legacy whole-stack bottle filling, bounded by the configured stack limit.
                var filled=PotionData.base(id).potion(false);filled.setCount(stack.getCount());return filled;
            }
        });
        var vanilla=((DispenserBlock)Blocks.DISPENSER).getDispenseMethod(new ItemStack(Items.BUCKET));
        var potionCollector=new DefaultDispenseItemBehavior(){
            @Override protected ItemStack execute(BlockSource s,ItemStack stack){
                if(!s.getLevel().getFluidState(front(s)).isSource())return super.execute(s,stack);
                return collect(s,stack);
            }
        };
        var eject=new DefaultDispenseItemBehavior();
        // Vanilla now recognizes every BucketPickup, including our fluids. Route the
        // disabled ordinary-bucket case explicitly so the legacy option still works.
        DispenserBlock.registerBehavior(Items.BUCKET,(s,stack)->PotionFluids.id(s.getLevel().getFluidState(front(s)))!=0
            ?(BrewingConfig.MAGIC_BUCKET.get()?eject:potionCollector).dispense(s,stack):vanilla.dispense(s,stack));
        DispenserBlock.registerBehavior(InfusionBrewing.MAGIC_BUCKET.get(),potionCollector);
        for(var family:PotionFluids.ALL)DispenserBlock.registerBehavior(family.bucket.get(),new DefaultDispenseItemBehavior(){
            @Override protected ItemStack execute(BlockSource s,ItemStack stack){
                var pos=front(s);if(!s.getLevel().isEmptyBlock(pos))return stack;
                s.getLevel().setBlockAndUpdate(pos,family.block.get().defaultBlockState());
                return new ItemStack(BrewingConfig.MAGIC_BUCKET.get()?InfusionBrewing.MAGIC_BUCKET.get():Items.BUCKET);
            }
        });
    }
    private static BlockPos front(BlockSource s){return s.getPos().relative(s.getBlockState().getValue(DispenserBlock.FACING));}
    private static ItemStack collect(BlockSource s,ItemStack stack){
        var pos=front(s);var state=s.getLevel().getBlockState(pos);var fluid=state.getFluidState();
        if(!fluid.isSource())return stack;
        ItemStack filled;
        if(state.getBlock() instanceof BucketPickup pickup)filled=pickup.pickupBlock(s.getLevel(),pos,state);else return stack;
        if(filled.isEmpty())return stack;
        stack.shrink(1);if(stack.isEmpty())return filled;
        if(((DispenserBlockEntity)s.getEntity()).addItem(filled)<0)new DefaultDispenseItemBehavior().dispense(s,filled);
        return stack;
    }
}
