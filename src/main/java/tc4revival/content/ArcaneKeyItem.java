package tc4revival.content;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public  class ArcaneKeyItem extends Item {
    private final boolean master;
    public ArcaneKeyItem(boolean master) { super(new Properties().rarity(Rarity.UNCOMMON)); this.master=master; }
    @Override public boolean isFoil(ItemStack stack) { return stack.hasTag(); }
    static String location(BlockPos pos) { return pos.getX()+","+pos.getY()+","+pos.getZ(); }
    static boolean matches(ItemStack stack,Level level,BlockPos pos,boolean door) {
        var tag=stack.getTag();
        return tag!=null && tag.getString("location").equals(location(pos)) && tag.getString("dimension").equals(level.dimension().location().toString())
                && tag.getByte("type")== (door ? 0 : 1);
    }
    @Override public InteractionResult onItemUseFirst(ItemStack stack,UseOnContext context) {
        var level=context.getLevel(); var pos=context.getClickedPos(); var state=level.getBlockState(pos); var player=context.getPlayer();
        boolean door=state.is(TCArcane.ARCANE_DOOR.get());
        if (player==null || !door && !state.is(TCArcane.ARCANE_PRESSURE_PLATE.get())) return InteractionResult.PASS;
        if (door) pos=ArcaneDoorBlock.root(state,pos);
        if (!level.mayInteract(player,pos) || !player.mayUseItemAt(pos,context.getClickedFace(),stack)) return InteractionResult.FAIL;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof OwnedDeviceBlockEntity owned)) return InteractionResult.FAIL;
        if (door && !(level.getBlockEntity(pos.above()) instanceof OwnedDeviceBlockEntity)) return InteractionResult.FAIL;
        var name=player.getGameProfile().getName(); String message=null; float pitch=0;
        if (!stack.hasTag()) {
            if (owned.access().mayBindKey(name,master)) {
                var bound=new ItemStack(this); var tag=bound.getOrCreateTag(); tag.putString("location",location(pos));
                tag.putString("dimension",level.dimension().location().toString()); tag.putByte("type",(byte)(door ? 0 : 1));
                if (!player.getInventory().add(bound)) player.drop(bound,false);
                if (!player.isCreative()) stack.shrink(1);
                message=door ? "tc.key1" : "tc.key2"; pitch=.9f;
            }
        } else if (!matches(stack,level,pos,door)) message="tc.key7";
        else if (owned.access().grant(name,master)) {
            owned.changed();
            if (door) {
                var upper=(OwnedDeviceBlockEntity)level.getBlockEntity(pos.above());
                var permissions=new net.minecraft.nbt.CompoundTag(); owned.access().save(permissions); upper.access().load(permissions); upper.changed();
            }
            if (!player.isCreative()) stack.shrink(1);
            message=door ? "tc.key3" : "tc.key5"; pitch=1.1f;
        } else message="tc.key8";
        if (message!=null) {
            var text=Component.translatable(message);
            if (master && pitch==1.1f) text.append(Component.translatable(door ? "tc.key4" : "tc.key6"));
            player.displayClientMessage(text.withStyle(ChatFormatting.DARK_PURPLE,ChatFormatting.ITALIC),false);
        }
        if (pitch>0) { level.playSound(null,pos,TCSounds.KEY.get(),net.minecraft.sounds.SoundSource.BLOCKS,1,pitch); player.swing(context.getHand(),true); }
        return InteractionResult.CONSUME;
    }
    @Override public void appendHoverText(ItemStack stack,Level level,java.util.List<Component> lines,TooltipFlag flag) {
        var tag=stack.getTag(); if (tag==null || !tag.contains("location")) return;
        var coordinates=tag.getString("location").split(",");
        String text=coordinates.length==3 ? "x "+coordinates[0]+", z "+coordinates[2]+", y "+coordinates[1] : tag.getString("location");
        lines.add(Component.translatable("tc.key9").withStyle(ChatFormatting.DARK_PURPLE,ChatFormatting.ITALIC));
        lines.add(Component.translatable(tag.getByte("type")==0 ? "tc.key10" : "tc.key11").withStyle(ChatFormatting.DARK_PURPLE,ChatFormatting.ITALIC));
        lines.add(Component.literal(text).withStyle(ChatFormatting.DARK_PURPLE,ChatFormatting.ITALIC));
    }
}
