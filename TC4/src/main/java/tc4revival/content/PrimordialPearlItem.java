package tc4revival.content;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import tc4revival.research.KnowledgeSavedData;

/** The old single-use node rejuvenator, including its explosion and flux aftermath. */
public  class PrimordialPearlItem extends Item {
    public PrimordialPearlItem() { super(new Properties().stacksTo(1).rarity(Rarity.EPIC)); }
    @Override public Rarity getRarity(ItemStack stack) { return Rarity.EPIC; }
    @Override public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
        for (int line=5; line<=6; line++) lines.add(Component.translatable("item.ItemEldritchObject.text."+line).withStyle(ChatFormatting.DARK_PURPLE));
    }
    @Override public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        var level=context.getLevel(); var pos=context.getClickedPos(); var player=context.getPlayer();
        if (player==null || !(level.getBlockEntity(pos) instanceof AuraNodeBlockEntity node)) return InteractionResult.PASS;
        player.swing(context.getHand());
        if (level.isClientSide) return InteractionResult.SUCCESS;
        boolean researched=KnowledgeSavedData.get(level.getServer()).snapshot(player.getUUID())
                .map(knowledge->knowledge.researchMarkers().contains("PRIMNODE")).orElse(false);
        stack.shrink(1); // Creative players consumed the pearl too.
        node.rejuvenate(researched,level.random);
        level.explode(null,pos.getX()+.5,pos.getY()+1.5,pos.getZ()+.5,
                3+level.random.nextFloat()*(researched ? 3 : 5),false,Level.ExplosionInteraction.BLOCK);
        for (int attempt=0; attempt<33; attempt++) {
            var target=pos.offset(level.random.nextInt(6)-level.random.nextInt(6),
                    level.random.nextInt(6)-level.random.nextInt(6),level.random.nextInt(6)-level.random.nextInt(6));
            if (level.isOutsideBuildHeight(target) || !level.hasChunkAt(target) || !level.isEmptyBlock(target)) continue;
            var flux=target.getY()<pos.getY() ? TCArcane.FLUX_GOO.get() : TCArcane.FLUX_GAS.get();
            level.setBlock(target,flux.defaultBlockState().setValue(FiniteFluidBlock.QUANTA,9),3);
        }
        return InteractionResult.CONSUME;
    }
}
