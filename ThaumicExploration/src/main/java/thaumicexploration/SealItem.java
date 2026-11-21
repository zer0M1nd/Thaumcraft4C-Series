package thaumicexploration;

import java.util.*;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

public class SealItem extends Item {
    public final String kind;
    public final DyeColor color;
    public SealItem(String kind,DyeColor color) { super(new Properties()); this.kind=kind; this.color=color; }
    // Empty vanilla chests otherwise consume the click by opening their menu before useOn.
    @Override public InteractionResult onItemUseFirst(ItemStack stack,UseOnContext context) { return useOn(context); }
    @Override public InteractionResult useOn(UseOnContext context) {
        var level=context.getLevel(); var pos=context.getClickedPos(); var player=context.getPlayer(); var stack=context.getItemInHand();
        if (player==null||kind.equals("blank_seal")||!level.mayInteract(player,pos)||!player.mayUseItemAt(pos,context.getClickedFace(),stack)) return InteractionResult.PASS;
        boolean jar=kind.equals("jar_seal"); var state=level.getBlockState(pos); var target=level.getBlockEntity(pos);
        if(target instanceof BoundEntity bound) {
            if(bound.isJar()!=jar)return InteractionResult.FAIL;
            if(!level.isClientSide) { var linked=new ItemStack(ExplorationMod.item(kind+"_"+DyeColor.byId(bound.color).getName())); linked.getOrCreateTag().putString("network",bound.network); stack.shrink(1); if(!player.addItem(linked))player.drop(linked,false); }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        var captured=new ArrayList<ItemStack>();
        String network=stack.hasCustomHoverName()?"named:"+stack.getHoverName().getString():stack.hasTag()?stack.getTag().getString("network"):"";
        if(network.length()>128)return InteractionResult.FAIL;
        if(jar) {
            if(!(target instanceof tc4revival.content.EssentiaJarBlockEntity vessel)||state.getBlock()!=net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(new net.minecraft.resources.ResourceLocation("thaumcraft:essentia_jar"))||vessel.amount()!=0||vessel.filter()!=null)return InteractionResult.PASS;
        } else {
            if(!(target instanceof ChestBlockEntity chest)||!state.is(Blocks.CHEST)||state.getValue(ChestBlock.TYPE)!=ChestType.SINGLE)return InteractionResult.PASS;
            if(!chest.isEmpty()) { if(!level.isClientSide)player.displayClientMessage(Component.translatable("message.thaumicexploration.empty_chest"),true); return InteractionResult.FAIL; }
            for(int i=0;i<27;i++)captured.add(chest.getItem(i).copy());
        }
        if(!level.isClientSide) {
            if(network.isEmpty())network=UUID.randomUUID().toString();
            if(target instanceof ChestBlockEntity chest)chest.clearContent();
            var replacement=ExplorationMod.block(jar?"bound_jar":"bound_chest").defaultBlockState();
            if(!jar)replacement=replacement.setValue(BoundBlock.FACING,state.getValue(ChestBlock.FACING));
            level.setBlock(pos,replacement,3);
            if(!(level.getBlockEntity(pos) instanceof BoundEntity bound))throw new IllegalStateException("Missing bound endpoint");
            bound.initialize(network,color.getId());
            for(int i=0;i<captured.size();i++)if(!captured.get(i).isEmpty())bound.chest().setItem(i,captured.get(i));
            if(!player.getAbilities().instabuild)stack.shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flags) {
        String network=stack.hasCustomHoverName()?stack.getHoverName().getString():stack.hasTag()?stack.getTag().getString("network"):"";
        if(!network.isEmpty())lines.add(Component.translatable("txitems.boundJar.networkInfo",network));
    }
}
