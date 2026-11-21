package automagy.content;

import automagy.api.LocationLink;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public final class CrystalEyeItem extends Item implements LocationLink {
    public CrystalEyeItem() { super(new Properties()); }
    @Override public Optional<GlobalPos> linkedPosition(ItemStack stack) {
        var tag=stack.getTag();
        if (tag==null || !tag.contains("Target",Tag.TAG_LONG) || !tag.contains("Dimension",Tag.TAG_STRING)) return Optional.empty();
        var id=ResourceLocation.tryParse(tag.getString("Dimension"));
        return id==null ? Optional.empty() : Optional.of(GlobalPos.of(ResourceKey.create(Registries.DIMENSION,id),BlockPos.of(tag.getLong("Target"))));
    }
    @Override public boolean isFoil(ItemStack stack) { return linkedPosition(stack).isPresent(); }
    @Override public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        var state=context.getLevel().getBlockState(context.getClickedPos());
        if(state.getBlock() instanceof DeliveryMirrorBlock mirror&&!mirror.multi&&context.getPlayer()!=null&&context.getPlayer().isShiftKeyDown())return useOn(context);
        if(state.getBlock() instanceof DeliveryMirrorBlock mirror&&!mirror.multi||state.getBlock() instanceof RequisitionTomeBlock)
            return state.use(context.getLevel(),context.getPlayer(),context.getHand(),new net.minecraft.world.phys.BlockHitResult(context.getClickLocation(),context.getClickedFace(),context.getClickedPos(),context.isInside()));
        return (context.getLevel().getBlockState(context.getClickedPos()).is(Content.REMOTE_COMPARATOR.get()) || context.getLevel().getBlockState(context.getClickedPos()).is(Content.SCRIBE_POINTER.get()) || context.getLevel().getBlockState(context.getClickedPos()).is(Content.ESSENTIA_LOCUS.get())) ? InteractionResult.PASS : useOn(context);
    }
    @Override public InteractionResult useOn(UseOnContext context) {
        var player=context.getPlayer();
        if (player==null || !player.mayUseItemAt(context.getClickedPos(),context.getClickedFace(),context.getItemInHand())) return InteractionResult.FAIL;
        if (!context.getLevel().isClientSide) {
            var tag=context.getItemInHand().getOrCreateTag();
            tag.putLong("Target",context.getClickedPos().asLong());tag.putString("Dimension",context.getLevel().dimension().location().toString());
            context.getLevel().playSound(null,context.getClickedPos(),net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME,
                    net.minecraft.sounds.SoundSource.PLAYERS,.5f,1);
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }
    @Override public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flags) {
        linkedPosition(stack).ifPresent(target->lines.add(Component.translatable("tooltip.automagy.link",
                target.pos().getX(),target.pos().getY(),target.pos().getZ(),target.dimension().location().toString())));
    }
}
