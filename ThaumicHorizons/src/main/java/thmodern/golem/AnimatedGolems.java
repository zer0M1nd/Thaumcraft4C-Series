package thmodern.golem;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeHooks;
import tc4revival.content.*;
import thaumcraft.api.golems.GolemApi;

public final class AnimatedGolems {
    public static final ResourceLocation DATA=GolemProfiles.id("animation"),TASK=GolemProfiles.id("animated_guard");
    public interface Payment { boolean commit(); default void refund(){} }
    public static boolean eligible(ServerLevel level,BlockPos pos){
        if(!level.hasChunkAt(pos)||level.isOutsideBuildHeight(pos))return false;
        var state=level.getBlockState(pos);
        return !state.isAir()&&!state.hasBlockEntity()&&state.getDestroySpeed(level,pos)>=0
                &&(state.isSolidRender(level,pos)||state.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK,GolemProfiles.id("animation_whitelist"))));
    }
    public static boolean animate(ServerPlayer player,BlockPos pos,int lifetime,boolean berserk,boolean explosive,Payment payment){
        var level=player.serverLevel();
        if(lifetime<=0||!eligible(level,pos)||!level.mayInteract(player,pos)||!player.mayBuild())return false;
        var state=level.getBlockState(pos);
        if(ForgeHooks.onBlockBreakEvent(level,player.gameMode.getGameModeForPlayer(),player,pos)<0||!level.getBlockState(pos).equals(state))return false;
        var golem=TCEntities.GOLEM.get().create(level);if(golem==null)return false;
        var profile=GolemProfiles.select(state);
        golem.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,player.getYRot(),0);
        golem.state().owner(player.getGameProfile().getName());golem.state().home(pos,Direction.DOWN);
        var data=new CompoundTag();data.putUUID("Owner",player.getUUID());data.putInt("Remaining",lifetime);
        data.putBoolean("Berserk",berserk);data.putBoolean("Explosive",explosive);
        GolemApi.data(golem,DATA,data);GolemApi.configure(golem,profile.id(),TASK,state);golem.setHealth(golem.getMaxHealth());
        golem.setCustomName(Component.translatable("entity.thaumichorizons.block_golem",state.getBlock().getName()));
        if(!level.addFreshEntity(golem))return false;
        // Spawn listeners may cancel, alter the source, or alter the caster's resources.
        if(!level.getBlockState(pos).equals(state)||!payment.commit()){golem.discard();return false;}
        if(!level.removeBlock(pos,false)){payment.refund();golem.discard();return false;}
        level.levelEvent(2001,pos,net.minecraft.world.level.block.Block.getId(state));
        level.playSound(null,pos,SoundEvents.ILLUSIONER_CAST_SPELL,SoundSource.PLAYERS,1,1);
        level.broadcastEntityEvent(golem,(byte)7);return true;
    }
    /** Natural animus lightning has no player owner and creates a temporary berserk guardian. */
    public static boolean animateNaturally(ServerLevel level,BlockPos pos,int lifetime){
        if(lifetime<=0||!eligible(level,pos))return false;
        var state=level.getBlockState(pos);var golem=TCEntities.GOLEM.get().create(level);if(golem==null)return false;
        golem.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);golem.state().owner("");golem.state().home(pos,Direction.DOWN);
        var data=new CompoundTag();data.putInt("Remaining",lifetime);data.putBoolean("Berserk",true);
        GolemApi.data(golem,DATA,data);GolemApi.configure(golem,GolemProfiles.select(state).id(),TASK,state);golem.setHealth(golem.getMaxHealth());
        golem.setCustomName(Component.translatable("entity.thaumichorizons.block_golem",state.getBlock().getName()));
        if(!level.addFreshEntity(golem))return false;
        if(!level.getBlockState(pos).equals(state)||!level.removeBlock(pos,false)){golem.discard();return false;}
        level.levelEvent(2001,pos,net.minecraft.world.level.block.Block.getId(state));level.broadcastEntityEvent(golem,(byte)7);return true;
    }
    public static boolean isAnimated(GolemEntity golem){return !golem.configuration().addonData(DATA).isEmpty();}
    /** Called by the vortex when it consumes powder thrown by a player. */
    public static GolemEntity summonVoid(ServerPlayer owner,net.minecraft.world.phys.Vec3 position){
        var level=owner.serverLevel();var golem=TCEntities.GOLEM.get().create(level);if(golem==null)return null;
        golem.moveTo(position.x,position.y,position.z,owner.getYRot(),0);
        golem.state().owner(owner.getGameProfile().getName());golem.state().home(BlockPos.containing(position),Direction.DOWN);
        var data=new CompoundTag();data.putUUID("Owner",owner.getUUID());data.putBoolean("Void",true);
        GolemApi.data(golem,DATA,data);GolemApi.configure(golem,GolemProfiles.id("material/void"),TASK,null);
        golem.setHealth(golem.getMaxHealth());golem.setCustomName(Component.translatable("entity.thaumichorizons.void_golem"));
        return level.addFreshEntity(golem)?golem:null;
    }
    /** Four rare events warn the owner three times, then replace an uncontrolled voidling. */
    public static void advanceVoid(GolemEntity golem){
        if(!(golem.level() instanceof ServerLevel level)||golem.isRemoved()||!golem.isAlive()||golem.configuration().core()!=null)return;
        var data=golem.configuration().addonData(DATA);if(!data.getBoolean("Void"))return;
        int stage=data.getInt("VoidCount");
        if(stage>=3){
            var guardian=TCEntities.ELDRITCH_GUARDIAN.get().create(level);if(guardian==null)return;
            guardian.moveTo(golem.position());guardian.restrictTo(golem.blockPosition(),32);
            if(!level.addFreshEntity(guardian))return;
            tc4revival.network.FocusEffectPacket.burst(level,golem.getBoundingBox().getCenter(),2);
            golem.dropCargo();golem.discard();return;
        }
        var owner=owner(golem);if(owner!=null)owner.sendSystemMessage(Component.translatable("thaumichorizons.golemWarning"+(stage+1)).withStyle(net.minecraft.ChatFormatting.ITALIC,net.minecraft.ChatFormatting.DARK_PURPLE));
        data.putInt("VoidCount",stage+1);GolemApi.data(golem,DATA,data);
    }
    public static Player owner(GolemEntity golem){var data=golem.configuration().addonData(DATA);return data.hasUUID("Owner")?golem.level().getPlayerByUUID(data.getUUID("Owner")):null;}
    public static void installGoals(GolemEntity golem,GoalSelector work,GoalSelector targets){
        work.addGoal(0,new GolemAvoidCreeperGoal(golem));
        work.addGoal(1,new Guard(golem));
        work.addGoal(2,new Follow(golem));
        work.addGoal(7,new LookAtPlayerGoal(golem,Player.class,6));work.addGoal(8,new RandomLookAroundGoal(golem));
        work.addGoal(5,new OpenDoorGoal(golem,true));
    }
    private static boolean validTarget(GolemEntity golem,LivingEntity target){
        if(target==golem||!target.isAlive()||target.isInvulnerable()||target.isAlliedTo(golem)||target==owner(golem))return false;
        if(target instanceof Player player&&(player.isCreative()||player.isSpectator()))return false;
        var data=golem.configuration().addonData(DATA);
        if(!data.getBoolean("Berserk")){
            var owner=owner(golem);
            return target instanceof Monster||target==golem.getLastHurtByMob()||(owner!=null&&(target==owner.getLastHurtByMob()||target==owner.getLastHurtMob()));
        }
        return !target.getName().getString().equals(golem.getName().getString());
    }
    private static final class Guard extends Goal {
        private final GolemEntity golem;private LivingEntity target;private int cooldown;
        Guard(GolemEntity golem){this.golem=golem;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
        @Override public boolean canUse(){
            if(!golem.workEnabled())return false;
            target=golem.level().getEntitiesOfClass(LivingEntity.class,golem.getBoundingBox().inflate(16),e->validTarget(golem,e)&&golem.getSensing().hasLineOfSight(e)).stream().min(Comparator.comparingDouble(golem::distanceToSqr)).orElse(null);
            return target!=null;
        }
        @Override public boolean canContinueToUse(){return golem.workEnabled()&&target!=null&&validTarget(golem,target)&&golem.distanceToSqr(target)<1024;}
        @Override public void start(){golem.setTarget(target);}
        @Override public void stop(){target=null;golem.setTarget(null);golem.getNavigation().stop();}
        @Override public void tick(){
            if(target==null)return;golem.getLookControl().setLookAt(target,30,30);
            golem.getNavigation().moveTo(target,golem.navigationSpeed()*3);
            if(cooldown>0)cooldown--;
            if(cooldown==0&&golem.distanceToSqr(target)<2.25&&golem.getSensing().hasLineOfSight(target)){golem.doHurtTarget(target);golem.action();cooldown=20;}
        }
        @Override public boolean requiresUpdateEveryTick(){return true;}
    }
    private static final class Follow extends Goal {
        private final GolemEntity golem;private Player owner;
        Follow(GolemEntity golem){this.golem=golem;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
        @Override public boolean canUse(){owner=owner(golem);double min=golem.configuration().addonData(DATA).getBoolean("Explosive")?8:2;return owner!=null&&golem.workEnabled()&&golem.distanceToSqr(owner)>min*min;}
        @Override public boolean canContinueToUse(){double min=golem.configuration().addonData(DATA).getBoolean("Explosive")?8:2;return owner!=null&&owner.isAlive()&&golem.workEnabled()&&golem.distanceToSqr(owner)>min*min;}
        @Override public void tick(){
            if(owner==null)return;golem.getLookControl().setLookAt(owner,10,30);
            if(golem.tickCount%10!=0)return;
            if(golem.getNavigation().moveTo(owner,golem.navigationSpeed()*3)||golem.isLeashed()||golem.distanceToSqr(owner)<144)return;
            var center=owner.blockPosition();
            for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++){
                if(Math.abs(x)<2&&Math.abs(z)<2)continue;var pos=center.offset(x,0,z);
                if(!golem.level().hasChunkAt(pos)||!golem.level().getBlockState(pos.below()).isFaceSturdy(golem.level(),pos.below(),Direction.UP))continue;
                var destination=new net.minecraft.world.phys.Vec3(pos.getX()+.5,pos.getY(),pos.getZ()+.5);
                if(!golem.level().noCollision(golem,golem.getBoundingBox().move(destination.subtract(golem.position()))))continue;
                golem.teleportTo(destination.x,destination.y,destination.z);golem.getNavigation().stop();return;
            }
        }
        @Override public void stop(){golem.getNavigation().stop();owner=null;}
    }
    public static void tick(GolemEntity golem){
        if(!(golem.level() instanceof ServerLevel level)||!golem.isAlive()||golem.isRemoved()||!isAnimated(golem))return;
        var data=golem.configuration().addonData(DATA);var config=golem.configuration();
        if(config.core()!=null){
            if(config.customTask()!=null){GolemApi.configure(golem,config.customMaterial(),null,config.blockAppearance());data.putBoolean("Permanent",true);GolemApi.data(golem,DATA,data);}
            return;
        }
        if(data.getBoolean("Permanent")){restore(golem,false);return;}
        if(data.getBoolean("Void")){
            if(golem.tickCount%10==0){
                if(golem.getRandom().nextInt(500)==0)advanceVoid(golem);
                var owner=owner(golem);if(owner!=null)golem.state().home(owner.blockPosition(),Direction.DOWN);
            }
            return;
        }
        int remaining=data.getInt("Remaining");
        if(remaining<=0){restore(golem,true);return;}
        data.putInt("Remaining",remaining-1);golem.configuration().addonData(DATA,data);
        // Timer is persistent configuration, but need not broadcast the whole golem every tick.
        var owner=owner(golem);if(owner!=null&&golem.tickCount%10==0)golem.state().home(owner.blockPosition(),Direction.DOWN);
    }
    public static void restore(GolemEntity golem,boolean allowExplosion){
        if(!(golem.level() instanceof ServerLevel level)||!golem.isAlive()||golem.isRemoved()||!isAnimated(golem))return;
        var data=golem.configuration().addonData(DATA);var state=golem.configuration().blockAppearance();
        if(state==null||state.isAir()){golem.dropCargo();golem.discard();return;}
        if(allowExplosion&&(state.is(Blocks.TNT)||data.getBoolean("Explosive"))){
            golem.dropCargo();golem.discard();level.explode(golem,golem.getX(),golem.getY(),golem.getZ(),state.is(Blocks.TNT)?4:3,net.minecraft.world.level.Level.ExplosionInteraction.MOB);return;
        }
        var center=new BlockPos((int)Math.round(golem.getX()-.5),(int)Math.round(golem.getY()),(int)Math.round(golem.getZ()-.5));
        var positions=new LinkedHashSet<BlockPos>();positions.add(center);
        for(int y:new int[]{0,-1,1})for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)positions.add(center.offset(x,y,z));
        boolean placed=false;
        for(var pos:positions){
            if(!level.hasChunkAt(pos)||level.isOutsideBuildHeight(pos)||!level.isEmptyBlock(pos)||!state.canSurvive(level,pos))continue;
            var owner=owner(golem);
            if(owner instanceof ServerPlayer player&&!level.mayInteract(player,pos))continue;
            var snapshot=net.minecraftforge.common.util.BlockSnapshot.create(level.dimension(),level,pos);
            if(net.minecraftforge.event.ForgeEventFactory.onBlockPlace(golem,snapshot,Direction.UP))continue;
            if(level.setBlock(pos,state,3)){placed=true;break;}
        }
        if(!placed){
            var item=new ItemStack(state.getBlock());
            if(item.isEmpty()||golem.spawnAtLocation(item)==null)return; // Retry later; never silently destroy the captured block.
        }
        golem.dropCargo();golem.discard();
    }
    private AnimatedGolems(){}
}
