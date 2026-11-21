package tc4revival.content;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.context.UseOnContext;

public  class EssentiaResonatorItem extends Item {
    public EssentiaResonatorItem() { super(new Properties().stacksTo(1).rarity(Rarity.UNCOMMON)); }
    @Override public boolean isFoil(ItemStack stack) { return stack.hasTag(); }
    @Override public InteractionResult onItemUseFirst(ItemStack stack,UseOnContext context) {
        var level=context.getLevel(); var pos=context.getClickedPos(); var player=context.getPlayer();
        if (player==null || !(level.getBlockEntity(pos) instanceof EssentiaTransport transport)) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        var face=context.getClickedFace();
        if (transport instanceof EssentiaTubeBlockEntity tube) {
            int part=EssentiaTubeBlock.tracePart(tube,player); if (part>=0 && part<6) face=net.minecraft.core.Direction.from3DDataValue(part);
        }
        if (transport instanceof EssentiaTubeBlockEntity tube && tube.flow().kind()==EssentiaTubeFlow.Kind.BUFFER) {
            var contents=tube.flow().contents();
            for (var aspect:contents.aspects().stream().sorted(java.util.Comparator.comparing(tc4revival.aspect.Aspect::tag)).toList()) player.displayClientMessage(Component.translatable("tc.resonator1",Integer.toString(contents.amount(aspect)),aspect.displayName()),false);
        } else {
            var type=transport.essentiaType(face);
            if (type!=null) player.displayClientMessage(Component.translatable("tc.resonator1",Integer.toString(transport.essentiaAmount(face)),type.displayName()),false);
        }
        var type=transport.suctionType(face);
        player.displayClientMessage(Component.translatable("tc.resonator2",Integer.toString(transport.suction(face)),type==null ? Component.translatable("tc.resonator3") : Component.literal(type.displayName())),false);
        level.playSound(null,pos.getX(),pos.getY(),pos.getZ(),TCSounds.ALEMBIC_KNOCK.get(),net.minecraft.sounds.SoundSource.BLOCKS,.5f,1.9f+level.random.nextFloat()*.1f);
        return InteractionResult.CONSUME;
    }
}
