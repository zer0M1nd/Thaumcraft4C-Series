package tc4revival.content;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FenceBlock;

public  class TravelingTrunkItem extends Item {
    public TravelingTrunkItem(Properties properties) { super(properties.stacksTo(1)); }
    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() { return tc4revival.client.TravelingTrunkItemRenderer.instance(); }
        });
    }
    @Override public InteractionResult useOn(UseOnContext context) {
        var player=context.getPlayer(); if (player==null) return InteractionResult.PASS;
        var level=context.getLevel(); if (level.isClientSide) return InteractionResult.SUCCESS;
        var trunk=TCEntities.TRAVELING_TRUNK.get().create(level); if (trunk==null) return InteractionResult.FAIL;
        var pos=context.getClickedPos(); var clicked=level.getBlockState(pos); double offset=0;
        if(!clicked.canBeReplaced())pos=pos.relative(context.getClickedFace());
        if(context.getClickedFace()==Direction.UP && clicked.is(net.minecraft.tags.BlockTags.FENCES))offset=.5;
        if(level.isOutsideBuildHeight(pos) || !level.hasChunkAt(pos) || !level.mayInteract(player,pos) || !player.mayUseItemAt(pos,context.getClickedFace(),context.getItemInHand()))return InteractionResult.FAIL;
        float yaw=net.minecraft.util.Mth.wrapDegrees(level.random.nextFloat()*360);
        trunk.moveTo(pos.getX()+.5,pos.getY()+offset,pos.getZ()+.5,yaw,0); trunk.yHeadRot=yaw; trunk.yBodyRot=yaw;
        trunk.ownerName(player.getGameProfile().getName()); var stack=context.getItemInHand();
        if (stack.hasCustomHoverName()) trunk.setCustomName(stack.getHoverName());
        if (stack.hasTag()) {
            var tag=stack.getTag();
            if (tag.contains("upgrade")) trunk.upgrade(tag.getByte("upgrade"));
            if (tag.contains("inventory")) trunk.loadInventory(tag.getList("inventory",Tag.TAG_COMPOUND));
        }
        if (!level.noCollision(trunk) || !level.addFreshEntity(trunk)) return InteractionResult.FAIL;
        trunk.playAmbientSound(); if (!player.getAbilities().instabuild) stack.shrink(1);
        return InteractionResult.CONSUME;
    }
    @Override public void appendHoverText(ItemStack stack,Level level,java.util.List<Component> tooltip,TooltipFlag flag) {
        if (!stack.hasTag()) return;
        var tag=stack.getTag();
        if (tag.contains("upgrade")) {
            int upgrade=tag.getByte("upgrade");
            tooltip.add((upgrade>=0?Component.translatable("item.ItemGolemUpgrade."+upgrade+".name").append(" "):Component.empty()).withStyle(ChatFormatting.BLUE));
        }
        if (tag.contains("inventory")) tooltip.add(Component.translatable("item.TrunkSpawner.text.1"));
    }
}
