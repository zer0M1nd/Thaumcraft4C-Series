package tc4revival.content;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import tc4revival.network.FocusEffectPacket;
import tc4revival.network.TCNetwork;
import tc4revival.network.WarpGainPacket;
import tc4revival.research.KnowledgeSavedData;
import tc4revival.research.PlayerKnowledge;

/** All twenty-one old random outcomes, including the repeated weights. */
public  class InfusionInstability {
    private InfusionInstability() {}
    public static void apply(ServerLevel level,BlockPos matrix,List<BlockPos> pedestals,int event) {
        switch(event) {
            case 0,2,10,13 -> eject(level,matrix,pedestals,0);
            case 6,17 -> eject(level,matrix,pedestals,1);
            case 1,11 -> eject(level,matrix,pedestals,2);
            case 3,8,14 -> zap(level,matrix,false);
            case 5,16 -> harm(level,matrix,false);
            case 12 -> zap(level,matrix,true);
            case 19 -> eject(level,matrix,pedestals,3);
            case 7 -> eject(level,matrix,pedestals,4);
            case 4,15 -> eject(level,matrix,pedestals,5);
            case 18 -> harm(level,matrix,true);
            case 9 -> explode(level,matrix,1.5f+level.random.nextFloat());
            case 20 -> warp(level,matrix);
            default -> throw new IllegalArgumentException("Unknown infusion instability event");
        }
    }
    private static List<LivingEntity> targets(ServerLevel level,BlockPos matrix) { return level.getEntitiesOfClass(LivingEntity.class,new AABB(matrix).inflate(10)); }
    private static void zap(ServerLevel level,BlockPos matrix,boolean all) {
        for (var target:targets(level,matrix)) {
            FocusEffectPacket.nodeZap(level,Vec3.atCenterOf(matrix),target.position().add(0,target.getBbHeight()/2,0));
            target.hurt(level.damageSources().magic(),4+level.random.nextInt(4));
            if (!all) break;
        }
    }
    private static void harm(ServerLevel level,BlockPos matrix,boolean all) {
        for (var target:targets(level,matrix)) {
            if (level.random.nextBoolean()) target.addEffect(new MobEffectInstance(TCEffects.FLUX_TAINT.get(),120,0,false,true));
            else {
                var effect=new MobEffectInstance(TCEffects.VIS_EXHAUST.get(),2400,0,true,true);
                effect.setCurativeItems(List.of()); target.addEffect(effect);
            }
            if (!all) break;
        }
    }
    private static void warp(ServerLevel level,BlockPos matrix) {
        var players=level.getEntitiesOfClass(ServerPlayer.class,new AABB(matrix).inflate(10));
        if (players.isEmpty()) return;
        var player=players.get(level.random.nextInt(players.size()));
        boolean sticky=level.random.nextFloat()<.25f;
        int amount=sticky ? 1 : 1+level.random.nextInt(5);
        if (tc4revival.config.TCConfig.SERVER.disableWarp.get()) return;
        var kind=sticky ? PlayerKnowledge.WarpKind.STICKY : PlayerKnowledge.WarpKind.TEMPORARY;
        KnowledgeSavedData.get(level.getServer()).update(player.getUUID(),level.random::nextInt,knowledge->{
            knowledge.changeWarp(kind,amount);knowledge.setWarpCounter(knowledge.totalWarp());return null;
        });
        TCNetwork.send(player,new WarpGainPacket(kind,amount));
    }
    private static void eject(ServerLevel level,BlockPos matrix,List<BlockPos> pedestals,int type) {
        for (int attempt=0;attempt<50 && !pedestals.isEmpty();attempt++) {
            var pos=pedestals.get(level.random.nextInt(pedestals.size()));
            if (!level.hasChunkAt(pos) || !(level.getBlockEntity(pos) instanceof ArcanePedestalBlockEntity pedestal) || pedestal.isEmpty()) continue;
            if (type<3 || type==5) {
                // Legacy InventoryUtils emits one entire slot, using randomness independent of world events.
                var random=new java.util.Random();
                var item=new net.minecraft.world.entity.item.ItemEntity(level,pos.getX()+.1+random.nextFloat()*.8,
                        pos.getY()+.1+random.nextFloat()*.8,pos.getZ()+.1+random.nextFloat()*.8,pedestal.getItem(0).copy());
                item.setDeltaMovement(random.nextGaussian()*.05,random.nextGaussian()*.05+.2,random.nextGaussian()*.05);
                level.addFreshEntity(item);pedestal.clearContent();
            }
            else pedestal.setItem(0,ItemStack.EMPTY);
            if (type==1 || type==3) {
                level.setBlock(pos.above(),TCArcane.FLUX_GOO.get().defaultBlockState().setValue(FiniteFluidBlock.QUANTA,8),3);
                level.playSound(null,pos,SoundEvents.GENERIC_SWIM,SoundSource.BLOCKS,.3f,1);
            } else if (type==2 || type==4) {
                level.setBlock(pos.above(),TCArcane.FLUX_GAS.get().defaultBlockState().setValue(FiniteFluidBlock.QUANTA,8),3);
                level.playSound(null,pos,SoundEvents.FIRE_EXTINGUISH,SoundSource.BLOCKS,.3f,1);
            } else if (type==5) explode(level,pos,1);
            level.blockEvent(pos,pedestal.getBlockState().getBlock(),11,0);
            FocusEffectPacket.nodeZap(level,Vec3.atCenterOf(matrix),Vec3.atCenterOf(pos).add(0,1,0)); return;
        }
    }
    private static void explode(ServerLevel level,BlockPos pos,float power) {
        level.explode(null,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,power,Level.ExplosionInteraction.NONE);
    }
}
