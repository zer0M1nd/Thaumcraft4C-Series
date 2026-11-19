package conciliummodern;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraft.core.Direction;
import tc4revival.content.TCArcane;
import tc4revival.content.TCEffects;
import tc4revival.research.KnowledgeSavedData;
import tc4revival.research.PlayerKnowledge.WarpKind;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/** Original temporary-warp cleansing with its thaumarhia/flux side effects. */
public final class TightBelt extends Item implements ICurioItem,thaumcraft.api.IWarpingGear {
    public TightBelt() { super(new Properties().stacksTo(1).rarity(Rarity.UNCOMMON)); }
    @Override public int getWarp(ItemStack stack,net.minecraft.world.entity.player.Player player) { return 3; }
    @Override public void curioTick(SlotContext context,ItemStack stack) {
        if(!(context.entity() instanceof ServerPlayer player) || player.hasEffect(TCEffects.WARP_WARD.get()))return;
        var data=KnowledgeSavedData.get(player.server);
        var knowledge=data.snapshot(player.getUUID()).orElse(null);
        if(knowledge==null || knowledge.warp(WarpKind.TEMPORARY)<=0)return;
        player.addEffect(new MobEffectInstance(TCEffects.THAUMARHIA.get(),60,Math.min(3,knowledge.totalWarp()/15),true,true));
        var level=player.serverLevel();
        if(level.random.nextInt(25)!=0)return;
        data.update(player.getUUID(),level.random::nextInt,next->{next.changeWarp(WarpKind.TEMPORARY,-1);return null;});
        // blockPosition floors negative coordinates; never load a chunk to vent flux.
        var pos=player.blockPosition().above();
        if(level.isOutsideBuildHeight(pos) || !level.hasChunkAt(pos) || !level.isEmptyBlock(pos) || !level.mayInteract(player,pos))return;
        if(ForgeEventFactory.onBlockPlace(player,BlockSnapshot.create(level.dimension(),level,pos),Direction.UP))return;
        level.setBlock(pos,TCArcane.FLUX_GAS.get().defaultBlockState(),3);
    }
}
