package conciliummodern;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import tc4revival.aspect.*;
import tc4revival.content.*;

public final class VisConductorFocus extends WandFocusItem {
    private final Map<Player,Session> sessions=new WeakHashMap<>();
    private static final class Session {
        final ItemStack wand; DestabilizedCrystalBlockEntity crystal; String aspect; long consumed; UUID wisp;
        Session(ItemStack wand){this.wand=wand;}
    }
    private static FocusUpgrade launcher(){return FocusUpgrade.require("thaumicconcilium:wisp_launcher");}
    private static FocusUpgrade dematerialization(){return FocusUpgrade.require("thaumicconcilium:dematerialization");}
    private boolean has(ItemStack focus,FocusUpgrade upgrade){return upgradeLevel(focus,upgrade.id)>0;}
    @Override public String sortingPrefix(){return "VISCONDUCTOR";}
    @Override public String depthTexture(){return "thaumicconcilium:item/vis_conductor_depth";}
    @Override public String ornamentTexture(){return "thaumicconcilium:item/vis_conductor_orn";}
    @Override public int color(ItemStack focus){return 0xaaaaaa;}
    @Override public Animation animation(ItemStack focus){return Animation.CHARGE;}
    @Override public boolean costPerTick(ItemStack focus){return true;}
    @Override public Map<Aspect,Integer> costs(ItemStack focus){
        var result=new LinkedHashMap<Aspect,Integer>();TCAspects.catalog().primals().forEach(a->result.put(a,5));return result;
    }
    @Override public List<FocusUpgrade> upgradesForRank(ItemStack focus,int rank){
        if(rank<1 || rank>5)return List.of();
        if(rank==1)return List.of(FocusUpgrade.FRUGAL,FocusUpgrade.POTENCY,launcher(),dematerialization());
        return has(focus,launcher())?List.of(FocusUpgrade.POTENCY,FocusUpgrade.FRUGAL,FocusUpgrade.EXTEND):List.of(FocusUpgrade.FRUGAL,FocusUpgrade.POTENCY);
    }
    @Override public boolean canApplyUpgrade(ItemStack focus,Player player,FocusUpgrade upgrade,int rank){
        if(upgrade!=dematerialization())return true;
        if(player instanceof ServerPlayer server)return thaumcraft.api.ThaumcraftApiHelper.isResearchComplete(server,"thaumicconcilium:DEMATUPGRADE");
        return player.level().isClientSide && tc4revival.client.ClientKnowledge.snapshot()
                .map(knowledge->knowledge.researchMarkers().contains("thaumicconcilium:DEMATUPGRADE")).orElse(false);
    }
    @Override public InteractionResultHolder<ItemStack> cast(ItemStack stack,Level level,Player player,InteractionHand hand,BlockHitResult hit){
        if(!level.isClientSide)sessions.put(player,new Session(stack));
        player.startUsingItem(hand);return InteractionResultHolder.consume(stack);
    }
    @Override public void usingTick(ItemStack stack,Player player,int remaining){
        if(player.level().isClientSide || !(stack.getItem() instanceof CastingWandItem wand))return;
        var session=sessions.get(player);if(session==null || session.wand!=stack){player.stopUsingItem();return;}
        var focus=attached(stack);var level=player.level();
        if(!wand.consumeForPlayer(stack,player,costs(focus),false,false,false)){sessions.remove(player);player.stopUsingItem();return;}
        if(has(focus,launcher())){
            var target=FocusTargeting.pointed(player,0,32,2,e->e instanceof WispEntity);
            if(!(target instanceof WispEntity wisp))return;
            if(!wand.consumeForPlayer(stack,player,costs(focus),false,false,true))return;
            var destination=player.position().add(player.getLookAngle().scale(1.5)).add(0,1,0);
            var motion=destination.subtract(wisp.position().add(0,wisp.getBbHeight()/2,0));
            if(motion.lengthSqr()>1)motion=motion.normalize();
            wisp.setDeltaMovement(motion);wisp.hurtMarked=true;session.wisp=wisp.getUUID();
            channelBolt(player,wisp.position(),wisp.aspectTag());return;
        }
        if(session.crystal==null){
            var hit=player.pick(8,0,false);
            if(hit instanceof BlockHitResult blockHit && level.hasChunkAt(blockHit.getBlockPos())
                    && level.getBlockEntity(blockHit.getBlockPos()) instanceof DestabilizedCrystalBlockEntity crystal
                    && crystal.amount()>0 && level.mayInteract(player,blockHit.getBlockPos())){
                session.crystal=crystal;session.aspect=crystal.aspectKey();
            }
            return;
        }
        var crystal=session.crystal;var pos=crystal.getBlockPos();
        if(crystal.isRemoved() || crystal.getLevel()!=level || player.distanceToSqr(Vec3.atCenterOf(pos))>64
                || !level.hasChunkAt(pos) || level.getBlockEntity(pos)!=crystal || !session.aspect.equals(crystal.aspectKey()) || !level.mayInteract(player,pos)){
            sessions.remove(player);player.stopUsingItem();return;
        }
        if(remaining%3==0 && crystal.amount()>0 && wand.consumeForPlayer(stack,player,costs(focus),false,false,true))
            session.consumed+=crystal.drain(session.aspect,Math.max(1,1+(int)wand.focusPotency(stack,player)));
        crystal.refreshDrainAnimation();channelBolt(player,Vec3.atLowerCornerOf(pos),session.aspect);
    }
    private static int color(String aspect){return TCAspects.catalog().find(aspect).map(a->a.color()).orElse(0xffffff);}
    private static void channelBolt(Player player,Vec3 point,String aspect){
        var random=player.getRandom();
        ConciliumArcNetwork.bolt(player,point.add(random.nextFloat(),random.nextFloat(),random.nextFloat()),color(aspect));
        if(player.tickCount%10==0)player.level().playSound(null,player.blockPosition(),TCSounds.JACOBS.get(),net.minecraft.sounds.SoundSource.PLAYERS,.5f,1);
    }
    /** Prepare snapshots only; the altar coordinates validation, resource removal, delivery and rollback. */
    public thaumcraft.api.crafting.InfusionCommit.Operation preparePolishment(ServerPlayer player,BlockPos matrix,PolishmentRecipe requirement){
        var session=sessions.get(player);var level=player.serverLevel();
        var crystal=session==null?null:session.crystal;
        var pos=crystal==null?null:crystal.getBlockPos().immutable();
        var state=pos==null?null:level.getBlockState(pos);
        var saved=crystal==null?null:crystal.saveWithoutMetadata();
        return new thaumcraft.api.crafting.InfusionCommit.Operation(){
            private boolean attempted;
            private boolean valid(){
                return session!=null&&sessions.get(player)==session&&player.isAlive()&&player.level()==level
                    &&player.distanceToSqr(Vec3.atCenterOf(matrix))<=16*16&&player.isUsingItem()&&player.getUseItem()==session.wand
                    &&session.wand.getItem() instanceof CastingWandItem&&attached(session.wand).getItem()==VisConductorFocus.this
                    &&session.consumed>=requirement.amount()&&requirement.aspect().equals(session.aspect)
                    &&crystal!=null&&!crystal.isRemoved()&&crystal.getLevel()==level&&level.hasChunkAt(pos)
                    &&level.getBlockEntity(pos)==crystal&&session.aspect.equals(crystal.aspectKey())
                    &&saved.equals(crystal.saveWithoutMetadata())&&player.distanceToSqr(Vec3.atCenterOf(pos))<=64
                    &&level.mayInteract(player,pos)&&player.mayUseItemAt(pos,net.minecraft.core.Direction.UP,session.wand);
            }
            public boolean validate(){
                return valid()&&net.minecraftforge.common.ForgeHooks.onBlockBreakEvent(level,player.gameMode.getGameModeForPlayer(),player,pos)!=-1&&valid();
            }
            public boolean commit(){
                if(!valid())return false;attempted=true;
                // Delay neighbor notifications and effects until the output has committed.
                return level.setBlock(pos,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2)&&level.isEmptyBlock(pos);
            }
            public void abort(){
                if(!attempted)return;attempted=false;
                if(!level.setBlock(pos,state,2)&&!level.getBlockState(pos).equals(state))throw new IllegalStateException("Cannot restore polishment crystal");
                if(!(level.getBlockEntity(pos) instanceof DestabilizedCrystalBlockEntity restored))throw new IllegalStateException("Missing restored polishment crystal");
                restored.load(saved.copy());restored.setChanged();session.crystal=restored;
                level.sendBlockUpdated(pos,state,state,2);
            }
            public void completed(){
                sessions.remove(player);player.stopUsingItem();level.updateNeighborsAt(pos,state.getBlock());
                ConciliumArcNetwork.emit(level,player.position().add(0,1,0),Vec3.atCenterOf(matrix),color(session.aspect),1);
                tc4revival.network.FocusEffectPacket.burst(level,Vec3.atLowerCornerOf(pos),2);
                level.playSound(null,pos,TCSounds.SHOCK.get(),net.minecraft.sounds.SoundSource.PLAYERS,.8f,.9f+level.random.nextFloat()*.1f);
            }
        };
    }
    public boolean completePolishment(ServerPlayer player,BlockPos matrix,PolishmentRecipe requirement){
        return thaumcraft.api.crafting.InfusionCommit.execute(java.util.List.of(),preparePolishment(player,matrix,requirement));
    }
    public void cancelPolishment(Player player){if(sessions.remove(player)!=null)player.stopUsingItem();}
    @Override public void release(ItemStack stack,Level level,Player player,int remaining){
        if(level.isClientSide)return;
        var session=sessions.remove(player);if(session==null || session.wand!=stack || !(stack.getItem() instanceof CastingWandItem wand))return;
        if(session.wisp!=null && level instanceof net.minecraft.server.level.ServerLevel server){
            var entity=server.getEntity(session.wisp);
            if(entity instanceof WispEntity wisp && wisp.isAlive() && player.distanceToSqr(wisp)<6){
                var charged=new ChargedWisp(ConciliumEntities.CHARGED_WISP.get(),level);charged.setPos(wisp.position());
                charged.initialize(thaumcraft.api.wands.WandActors.resolve(player),wisp.aspectTag(),(int)wand.focusPotency(stack,player),wand.focusUpgrade(stack,FocusUpgrade.EXTEND));
                if(level.addFreshEntity(charged))wisp.discard();
            }
            return;
        }
        var crystal=session.crystal;if(crystal==null || crystal.isRemoved() || crystal.getLevel()!=level)return;
        var pos=crystal.getBlockPos();
        if(!level.hasChunkAt(pos) || level.getBlockEntity(pos)!=crystal || player.distanceToSqr(Vec3.atCenterOf(pos))>64 || !level.mayInteract(player,pos))return;
        var target=FocusTargeting.pointed(player,0,32,1.1);
        if(target==null){
            if(player instanceof ServerPlayer server && net.minecraftforge.common.ForgeHooks.onBlockBreakEvent(level,server.gameMode.getGameModeForPlayer(),server,pos)==-1)return;
            level.removeBlock(pos,false);
            level.explode(player,pos.getX(),pos.getY(),pos.getZ(),2,Level.ExplosionInteraction.MOB);
        } else if(has(attached(stack),dematerialization()) && target instanceof LivingEntity living){
            float damage=(float)Math.min(Float.MAX_VALUE,session.consumed/Math.max(.25f,6-wand.focusPotency(stack,player)));
            living.hurt(level.damageSources().indirectMagic(player,player),damage);
            ConciliumArcNetwork.emit(level,living.position().add(0,.5,0),player.position().add(0,-.5,0),color(session.aspect),.2f);
            level.playSound(null,player.blockPosition(),TCSounds.SHOCK.get(),net.minecraft.sounds.SoundSource.PLAYERS,.8f,.9f+level.random.nextFloat()*.1f);
        }
        if(has(attached(stack),dematerialization()) && target instanceof RiftEntity rift && "auram".equals(session.aspect)) {
            long reduction=(long)Math.min(Integer.MAX_VALUE,session.consumed*(1.0+wand.focusPotency(stack,player)));
            rift.collapseTo((int)Math.max(0,rift.riftSize()-reduction));ConciliumArcNetwork.emit(level,rift.position().add(0,.5,0),player.position().add(0,.5,0),color(session.aspect),.2f);
        }
        if(has(attached(stack),dematerialization()) && target instanceof MaterialPeeler peeler && "auram".equals(session.aspect)){peeler.collapse();ConciliumArcNetwork.emit(level,peeler.position().add(0,.5,0),player.position().add(0,.5,0),color(session.aspect),.2f);}
    }
}
