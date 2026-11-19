package tc4revival.content;

import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.*;

public  class ItemMirrorItem extends BlockItem {
    public ItemMirrorItem() { super(TCArcane.ITEM_MIRROR.get(),new Properties().rarity(Rarity.UNCOMMON)); }
    public static int legacyMetadata(ItemStack stack) { return destination(stack)!=null || stack.hasTag() && stack.getTag().getInt("tc4_metadata")==1 ? 1 : 0; }
    public static GlobalPos destination(ItemStack stack) { return stack.hasTag() ? readDestination(stack.getTag()) : null; }
    public static GlobalPos readDestination(CompoundTag tag) {
        var dimension=ResourceLocation.tryParse(tag.getString("linkDimension"));
        if (dimension==null || !tag.contains("linkX",3) || !tag.contains("linkY",3) || !tag.contains("linkZ",3)) return null;
        return GlobalPos.of(ResourceKey.create(Registries.DIMENSION,dimension),new BlockPos(tag.getInt("linkX"),tag.getInt("linkY"),tag.getInt("linkZ")));
    }
    public static void writeDestination(CompoundTag tag,GlobalPos target,String name) {
        tag.putString("linkDimension",target.dimension().location().toString()); tag.putString("dimname",name);
        tag.putInt("linkX",target.pos().getX()); tag.putInt("linkY",target.pos().getY()); tag.putInt("linkZ",target.pos().getZ());
    }
    public static ItemStack linkedStack(GlobalPos target,String name) {
        var stack=new ItemStack(TCItems.ITEM_MIRROR.get()); writeDestination(stack.getOrCreateTag(),target,name); stack.getOrCreateTag().putInt("tc4_metadata",1); return stack;
    }
    public static void bindDestination(ItemStack stack,ItemMirrorBlockEntity target){
        writeDestination(stack.getOrCreateTag(),target.location(),target.location().dimension().location().toString());
        stack.getOrCreateTag().putUUID("MirrorTargetIdentity",target.identity());
        target.prepareMovement();
    }
    public static InteractionResult bind(ItemStack stack,net.minecraft.world.entity.player.Player player,ItemMirrorBlockEntity mirror){
        if(player.level().isClientSide)return InteractionResult.SUCCESS;
        if(stack.getItem() instanceof HandMirrorItem){bindDestination(stack,mirror);player.displayClientMessage(Component.translatable("tc.handmirrorlinked"),false);}
        else if(mirror.valid(true))player.displayClientMessage(Component.translatable("thaumcraft.mirror.already_linked").withStyle(net.minecraft.ChatFormatting.DARK_PURPLE,net.minecraft.ChatFormatting.ITALIC),false);
        else{
            var prepared=stack.copyWithCount(1);bindDestination(prepared,mirror);prepared.getOrCreateTag().putInt("tc4_metadata",1);
            if(!player.getAbilities().instabuild)stack.shrink(1);
            if(!player.getInventory().add(prepared))player.drop(prepared,false);
        }
        player.level().playSound(null,mirror.location().pos(),TCSounds.JAR.get(),net.minecraft.sounds.SoundSource.BLOCKS,1,2);player.containerMenu.broadcastChanges();return InteractionResult.CONSUME;
    }
    @Override public InteractionResult onItemUseFirst(ItemStack stack,UseOnContext context) {
        var level=context.getLevel(); var player=context.getPlayer(); var pos=context.getClickedPos();
        if (player==null || !(level.getBlockEntity(pos) instanceof ItemMirrorBlockEntity mirror)) return InteractionResult.PASS;
        if (!level.mayInteract(player,pos) || !player.mayUseItemAt(pos,context.getClickedFace(),stack)) return InteractionResult.FAIL;
        return bind(stack,player,mirror);
    }
    @Override public InteractionResult place(BlockPlaceContext context) {
        var stack=context.getItemInHand();var target=destination(stack);var identity=stack.hasTag()&&stack.getTag().hasUUID("MirrorTargetIdentity")?stack.getTag().getUUID("MirrorTargetIdentity"):null;
        var pos=context.getClickedPos(); var result=super.place(context);
        if (result.consumesAction() && target!=null && !context.getLevel().isClientSide && context.getLevel().getBlockEntity(pos) instanceof ItemMirrorBlockEntity mirror) { mirror.remember(target,identity); mirror.restoreLink(); }
        return result;
    }
    @Override public void appendHoverText(ItemStack stack,net.minecraft.world.level.Level level,java.util.List<Component> lines,TooltipFlag flags) { appendDestination(stack,lines,Component.translatable("thaumcraft.mirror.linked_to")); }
    static void appendDestination(ItemStack stack,java.util.List<Component> lines,net.minecraft.network.chat.MutableComponent prefix) {
        var target=destination(stack); if (target==null) return; var pos=target.pos(); var name=stack.getTag().getString("dimname");
        if (name.isEmpty()) name=target.dimension().location().toString();
        lines.add(prefix.append(" ").append(Component.translatable("thaumcraft.mirror.destination",pos.getX(),pos.getY(),pos.getZ(),name)));
    }
}
