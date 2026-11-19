package ttmodern;

import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.FarmBlock;

public final class InfusedSeedItem extends Item {
    public InfusedSeedItem() { super(new Properties().rarity(Rarity.RARE)); }
    public static String aspect(ItemStack stack) { String value=stack.hasTag()?stack.getTag().getString("Aspect"):"aer";return tc4revival.aspect.TCAspects.catalog().find(value).isPresent()?value:"aer"; }
    public static ItemStack seed(String aspect,java.util.Map<String,Integer> traits) {
        var stack=new ItemStack(Tinkerer.INFUSED_SEEDS.get());var tag=stack.getOrCreateTag();tag.putString("Aspect",aspect);var genes=new CompoundTag();traits.forEach(genes::putInt);tag.put("Traits",genes);return stack;
    }
    public static java.util.Map<String,Integer> traits(ItemStack stack) {
        var result=new java.util.LinkedHashMap<String,Integer>();var tag=stack.hasTag()?stack.getTag().getCompound("Traits"):new CompoundTag();
        for(String primal:CropTraits.PRIMALS)if(tag.getInt(primal)>0)result.put(primal,Math.min(50,tag.getInt(primal)));CropTraits.cap(result,50,bound->0);return result;
    }
    @Override public InteractionResult useOn(UseOnContext context) {
        var level=context.getLevel();var pos=context.getClickedPos();var player=context.getPlayer();var stack=context.getItemInHand();
        if(player==null||context.getClickedFace()!=Direction.UP||!(level.getBlockState(pos).getBlock() instanceof FarmBlock)||!level.isEmptyBlock(pos.above()))return InteractionResult.PASS;
        if(!level.mayInteract(player,pos)||!player.mayUseItemAt(pos,Direction.UP,stack)||!player.mayUseItemAt(pos.above(),Direction.UP,stack))return InteractionResult.FAIL;
        if(!level.isClientSide) {
            var soil=level.getBlockState(pos);var oldEntity=level.getBlockEntity(pos);var oldData=oldEntity==null?null:oldEntity.saveWithFullMetadata();var snapshot=net.minecraftforge.common.util.BlockSnapshot.create(level.dimension(),level,pos.above());var soilSnapshot=net.minecraftforge.common.util.BlockSnapshot.create(level.dimension(),level,pos);
            if(!soil.is(Tinkerer.INFUSED_FARMLAND.get()))level.setBlock(pos,Tinkerer.INFUSED_FARMLAND.get().defaultBlockState(),3);
            level.setBlock(pos.above(),Tinkerer.INFUSED_CROP.get().defaultBlockState().setValue(InfusedCropBlock.FORM,CropTraits.texture(aspect(stack))),3);
            if(soil.is(Tinkerer.INFUSED_FARMLAND.get())?net.minecraftforge.event.ForgeEventFactory.onBlockPlace(player,snapshot,Direction.UP):net.minecraftforge.event.ForgeEventFactory.onMultiBlockPlace(player,java.util.List.of(soilSnapshot,snapshot),Direction.UP)) {
                level.removeBlock(pos.above(),false);level.setBlock(pos,soil,3);if(oldData!=null&&level.getBlockEntity(pos)!=null)level.getBlockEntity(pos).load(oldData);return InteractionResult.FAIL;
            }
            if(level.getBlockEntity(pos.above()) instanceof InfusedCropEntity crop)crop.initialize(aspect(stack),traits(stack));else return InteractionResult.FAIL;
            if(!player.getAbilities().instabuild)stack.shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void appendHoverText(ItemStack stack,net.minecraft.world.level.Level world,java.util.List<net.minecraft.network.chat.Component> lines,TooltipFlag flags) {
        lines.add(net.minecraft.network.chat.Component.literal(thaumcraft.api.aspects.AspectViews.display(tc4revival.aspect.TCAspects.catalog().require(aspect(stack))).displayName()));traits(stack).forEach((key,value)->lines.add(net.minecraft.network.chat.Component.literal(key+": "+value)));
    }
}
