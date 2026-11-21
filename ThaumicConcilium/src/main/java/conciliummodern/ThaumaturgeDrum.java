package conciliummodern;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import tc4revival.content.*;

public final class ThaumaturgeDrum extends Item implements thaumcraft.api.IWarpingGear {
    private final boolean warped;
    public ThaumaturgeDrum(boolean warped){super(new Properties().stacksTo(1).rarity(Rarity.RARE));this.warped=warped;}
    public boolean warped(){return warped;}
    // Original addWarpToItem targets the ordinary variant, not metadata 1.
    @Override public int getWarp(ItemStack stack,Player player){return warped?0:3;}
    @Override public int getUseDuration(ItemStack stack){return 72000;}
    @Override public UseAnim getUseAnimation(ItemStack stack){return UseAnim.NONE;}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){player.startUsingItem(hand);return InteractionResultHolder.consume(player.getItemInHand(hand));}
    @Override public void onUseTick(Level level,LivingEntity wearer,ItemStack stack,int remaining){
        if(!(level instanceof ServerLevel server) || !(wearer instanceof Player player))return;
        int elapsed=getUseDuration(stack)-remaining;
        if(elapsed%8==0)level.playSound(null,player.blockPosition(),SoundEvents.NOTE_BLOCK_BASEDRUM.value(),SoundSource.PLAYERS,1,player.getRandom().nextFloat());
        if(elapsed%(warped?10:30)!=0 || !level.getWorldBorder().isWithinBounds(player.blockPosition()) || !level.hasChunkAt(player.blockPosition()))return;
        net.minecraft.world.entity.Mob summoned;
        if(warped){
            if(level.getEntitiesOfClass(MindSpiderEntity.class,player.getBoundingBox().inflate(32),LivingEntity::isAlive).size()>=16)return;
            var spider=TCEntities.MIND_SPIDER.get().create(server);if(spider==null)return;spider.setHarmless(true);summoned=spider;
        }else{
            if(level.getEntitiesOfClass(WispEntity.class,player.getBoundingBox().inflate(64,32,64),LivingEntity::isAlive).size()>=16)return;
            var wisp=TCEntities.WISP.get().create(server);if(wisp==null)return;
            var primals=tc4revival.aspect.TCAspects.catalog().primals();wisp.setAspectTag(primals.get(player.getRandom().nextInt(primals.size())).tag());summoned=wisp;
        }
        summoned.moveTo(player.getX(),player.getY(),player.getZ(),player.getRandom().nextFloat()*360,0);
        // Direct player-driven summoning, not natural spawning; EntityJoinLevel can still veto it.
        server.addFreshEntity(summoned);
    }
    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer){consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions(){@Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer(){return DrumRenderer.instance();}});}
}
