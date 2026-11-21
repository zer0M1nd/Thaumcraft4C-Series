package timodern;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.*;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.event.level.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class InfusionRuntime {
    private static final Set<UUID> flightGranted=new HashSet<>();
    private static final Map<UUID,Long> flightUntil=new HashMap<>();
    private static final Map<UUID,ResourceKey<Level>> flightDimension=new HashMap<>();
    private static final Map<UUID,GlobalPos> respawns=new HashMap<>();
    @SubscribeEvent public void watch(ChunkWatchEvent.Watch e){var l=e.getLevel();InfusionData.get(l).entries.forEach((pos,entry)->{var p=BlockPos.of(pos);if(new ChunkPos(p).equals(e.getPos()))InfusionNetwork.send(e.getPlayer(),p,entry);});}
    @SubscribeEvent public void breakBlock(BlockEvent.BreakEvent e){if(e.getLevel() instanceof ServerLevel l){if(InfusionData.has(l,e.getPos(),"humanus")&&!e.getPlayer().isCreative())e.setCanceled(true);else InfusionData.remove(l,e.getPos());}}
    @SubscribeEvent public void explode(ExplosionEvent.Detonate e){e.getAffectedBlocks().removeIf(p->InfusionData.has(e.getLevel(),p,"humanus"));}
    @SubscribeEvent public void die(LivingDeathEvent e){if(e.getEntity() instanceof ServerPlayer p){for(var pos:BlockPos.betweenClosed(p.blockPosition().below(),p.blockPosition()))if(InfusionData.has(p.level(),pos,"exanimis")){respawns.put(p.getUUID(),GlobalPos.of(p.level().dimension(),pos.above()));break;}}}
    @SubscribeEvent public void respawn(PlayerEvent.PlayerRespawnEvent e){if(e.getEntity() instanceof ServerPlayer p){var target=respawns.remove(p.getUUID());if(target!=null){var l=p.server.getLevel(target.dimension());if(l!=null)p.teleportTo(l,target.pos().getX()+.5,target.pos().getY(),target.pos().getZ()+.5,p.getYRot(),p.getXRot());}}}
    @SubscribeEvent public void fall(LivingFallEvent e){if(InfusionData.has(e.getEntity().level(),e.getEntity().getOnPos(),"pannus"))e.setDistance(0);}
    @SubscribeEvent public void interact(PlayerInteractEvent.RightClickBlock e){if(e.getLevel() instanceof ServerLevel l&&link(l,e.getPos(),e.getEntity(),e.getHand())){e.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);e.setCanceled(true);}}
    @SubscribeEvent public void punch(PlayerInteractEvent.LeftClickBlock e){if(!(e.getLevel() instanceof ServerLevel l))return;var data=InfusionData.at(l,e.getPos());if(data==null)return;var held=e.getItemStack();
        if(data.has("sensus")){if(e.getEntity().isShiftKeyDown())data.settings.remove("disguise");else if(held.getItem() instanceof BlockItem b&&b.getBlock().defaultBlockState().isSolidRender(l,e.getPos()))data.settings.put("disguise",net.minecraft.nbt.NbtUtils.writeBlockState(b.getBlock().defaultBlockState()));InfusionData.changed(l,e.getPos());e.setCanceled(true);}
        if(data.has("fabrico")&&held.getItem() instanceof tc4revival.content.StoredAspectItem stored){var a=stored.storedAspects(held);if(!a.isEmpty()){data.settings.putInt("tint",a.aspects().get(0).color());InfusionData.changed(l,e.getPos());e.setCanceled(true);}}
    }
    public static boolean link(ServerLevel level,BlockPos pos,Player player,net.minecraft.world.InteractionHand hand){var data=InfusionData.at(level,pos);if(data==null)return false;String kind=java.util.stream.Stream.of("iter","machina","permutatio").filter(data::has).findFirst().orElse(null);if(kind==null)return false;var held=player.getItemInHand(hand);if(!held.is(Items.PAPER)&&!held.is(ThaumicInfusion.PAPER.get()))return false;
        if(held.is(Items.PAPER)){var paper=new ItemStack(ThaumicInfusion.PAPER.get());paper.getOrCreateTag().putLong("pos",pos.asLong());paper.getOrCreateTag().putString("dimension",level.dimension().location().toString());paper.getOrCreateTag().putString("effect",kind);held.shrink(1);if(held.isEmpty())player.setItemInHand(hand,paper);else if(!player.getInventory().add(paper))player.drop(paper,false);player.displayClientMessage(net.minecraft.network.chat.Component.translatable("ti.linking.begin"),true);return true;}
        var tag=held.getTag();if(tag==null||!kind.equals(tag.getString("effect")))return true;var dim=ResourceLocation.tryParse(tag.getString("dimension"));var other=dim==null?null:level.getServer().getLevel(ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,dim));var target=BlockPos.of(tag.getLong("pos"));var dest=other==null?null:InfusionData.at(other,target);
        if(dest==null||!dest.has(kind)||(other==level&&pos.equals(target))){player.displayClientMessage(net.minecraft.network.chat.Component.translatable("ti.linking.fail"),true);return true;}
        putDestination(data.settings,kind,other,target);putDestination(dest.settings,kind,level,pos);InfusionData.changed(level,pos);InfusionData.changed(other,target);player.setItemInHand(hand,new ItemStack(Items.PAPER));player.displayClientMessage(net.minecraft.network.chat.Component.translatable("ti.linking.end"),true);return true;
    }
    public static void putDestination(CompoundTag tag,String kind,ServerLevel level,BlockPos pos){var d=new CompoundTag();d.putString("dimension",level.dimension().location().toString());d.putLong("pos",pos.asLong());tag.put("link_"+kind,d);}
    public static GlobalPos destination(ServerLevel source,InfusionData.Entry e,String kind){var d=e.settings.getCompound("link_"+kind);var id=ResourceLocation.tryParse(d.getString("dimension"));if(id==null)return null;var key=ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,id);var level=source.getServer().getLevel(key);var pos=BlockPos.of(d.getLong("pos"));return level!=null&&level.hasChunkAt(pos)&&InfusionData.has(level,pos,kind)?GlobalPos.of(key,pos):null;}
    public static int signal(BlockGetter world,BlockPos pos){if(InfusionData.has(world,pos,"potentia"))return 15;if(!(world instanceof ServerLevel l))return -1;var e=InfusionData.at(l,pos);if(e==null||!e.has("machina"))return -1;var dest=destination(l,e,"machina");if(dest==null)return 0;var target=l.getServer().getLevel(dest.dimension());for(var dir:Direction.values()){var n=dest.pos().relative(dir);if(!InfusionData.has(target,n,"machina")&&target.getSignal(n,dir)>0)return 15;}return 0;}
    @SubscribeEvent public void tick(TickEvent.LevelTickEvent e){if(e.phase!=TickEvent.Phase.END||!(e.level instanceof ServerLevel l))return;long time=l.getGameTime();var store=InfusionData.get(l);var flying=new HashSet<UUID>();
        for(var row:new ArrayList<>(store.entries.entrySet())){var pos=BlockPos.of(row.getKey());if(!l.hasChunkAt(pos))continue;if(l.isEmptyBlock(pos)){InfusionData.remove(l,pos);continue;}var entry=row.getValue();
            for(String aspect:List.copyOf(entry.aspects)){var def=InfusionEffects.get(aspect);if(def==null||!InfusionConfig.enabled(aspect)||InfusionConfig.interval(def)==0||Math.floorMod(time+pos.asLong(),InfusionConfig.interval(def))!=0)continue;effect(l,pos,entry,def.aspect(),flying);}
        }
        for(var p:l.players()){var id=p.getUUID();if(flying.contains(id)){flightUntil.put(id,time+InfusionConfig.VOLATUS_INTERVAL.get());flightDimension.put(id,l.dimension());}boolean needs=l.dimension().equals(flightDimension.get(id))&&time<flightUntil.getOrDefault(id,Long.MIN_VALUE);if(needs&&!p.getAbilities().mayfly){p.getAbilities().mayfly=true;flightGranted.add(id);p.onUpdateAbilities();}else if(!needs){flightUntil.remove(id);flightDimension.remove(id);if(flightGranted.remove(id)&&!p.isCreative()&&!p.isSpectator()){p.getAbilities().mayfly=false;p.getAbilities().flying=false;p.onUpdateAbilities();}}}
    }
    public static void effect(ServerLevel l,BlockPos p,InfusionData.Entry e,String a,Set<UUID> flying){var box=new AABB(p);switch(a){
        case "aer","venenum" -> {for(var player:l.getEntitiesOfClass(Player.class,box.inflate(1)))player.addEffect(new net.minecraft.world.effect.MobEffectInstance(a.equals("aer")?net.minecraft.world.effect.MobEffects.JUMP:net.minecraft.world.effect.MobEffects.POISON,100));}
        case "alienis","infernus","pannus" -> {for(var entity:l.getEntitiesOfClass(LivingEntity.class,box.expandTowards(0,1,0))){if(a.equals("infernus"))entity.setSecondsOnFire(8);else if(a.equals("pannus"))entity.fallDistance=0;else randomTeleport(l,p,entity);}}
        case "lucrum" -> {for(var item:l.getEntitiesOfClass(ItemEntity.class,box.inflate(InfusionConfig.LUCRUM_RANGE.get())))if(item.distanceToSqr(Vec3.atCenterOf(p))>=1)item.setDeltaMovement(item.getX()>p.getX()?-.05:.05,item.getDeltaMovement().y,item.getZ()>p.getZ()?-.05:.05);}
        case "sano" -> {for(var player:l.getEntitiesOfClass(Player.class,box.inflate(15)))if(player.getFoodData().getFoodLevel()>0&&l.random.nextBoolean())player.heal(1);}
        case "vinculum" -> {for(var entity:l.getEntitiesOfClass(LivingEntity.class,box.expandTowards(0,2,0))){entity.setDeltaMovement(Vec3.ZERO);entity.hurtMarked=true;}}
        case "volatus" -> {if(l.isEmptyBlock(p.above())){int range=InfusionConfig.VOLATUS_RANGE.get();for(var down=p.below();down.getY()>=l.getMinBuildHeight()&&InfusionData.has(l,down,"volatus");down=down.below())range+=InfusionConfig.VOLATUS_RANGE.get();for(var player:l.getEntitiesOfClass(Player.class,box.expandTowards(0,range-1,0)))flying.add(player.getUUID());}}
        case "tempestas" -> {if(l.isThundering()&&l.canSeeSky(p.above())&&l.random.nextInt(50)==0){var bolt=EntityType.LIGHTNING_BOLT.create(l);if(bolt!=null){bolt.moveTo(Vec3.atBottomCenterOf(p.above()));l.addFreshEntity(bolt);}}}
        case "gelum" -> freeze(l,p);
        case "victus","mortuus" -> {if(l.getGameTime()>=e.settings.getLong("spawnCooldown"))spawn(l,p,a);}
        case "perditio" -> {if(l.random.nextInt(20)==0&&!l.getEntitiesOfClass(Player.class,box.expandTowards(0,1,0),player->!player.isShiftKeyDown()).isEmpty()){e.aspects.remove(a);l.explode(null,p.getX(),p.getY(),p.getZ(),4,Level.ExplosionInteraction.BLOCK);InfusionData.changed(l,p);}}
        case "iter" -> {var dest=destination(l,e,a);if(dest!=null){var target=l.getServer().getLevel(dest.dimension());for(var player:l.getEntitiesOfClass(ServerPlayer.class,box.expandTowards(0,1,0),Player::isShiftKeyDown)){player.teleportTo(target,dest.pos().getX()+.5,dest.pos().getY()+1,dest.pos().getZ()+.5,player.getYRot(),player.getXRot());player.setShiftKeyDown(false);}}}
        case "machina" -> {int signal=signal(l,p);if(e.settings.getInt("last_signal")!=signal){e.settings.putInt("last_signal",signal);l.updateNeighborsAt(p,l.getBlockState(p).getBlock());}}
        case "permutatio" -> {boolean signal=l.hasNeighborSignal(p);if(signal!=e.settings.getBoolean("powered")){e.settings.putBoolean("powered",signal);InfusionData.get(l).setDirty();var dest=destination(l,e,a);if(dest!=null)swap(l,p,l.getServer().getLevel(dest.dimension()),dest.pos());}}
        case "terra" -> {if(l.isEmptyBlock(p.below())){var state=l.getBlockState(p);var tile=l.getBlockEntity(p);var nbt=tile==null?null:tile.saveWithoutMetadata();if(tile!=null)l.removeBlockEntity(p);var falling=FallingBlockEntity.fall(l,p,state);falling.blockData=nbt;falling.getPersistentData().put("ti_infusion",e.save());InfusionData.remove(l,p);}}
        default -> {}
    }}
    public static void randomTeleport(ServerLevel l,BlockPos center,LivingEntity entity){int radius=InfusionConfig.TELEPORT_RADIUS.get();var safe=new ArrayList<BlockPos>();for(var p:BlockPos.betweenClosed(center.offset(-radius,-radius,-radius),center.offset(radius-1,radius-1,radius-1)))if(l.hasChunkAt(p)&&!l.isEmptyBlock(p.below())&&l.isEmptyBlock(p)&&l.isEmptyBlock(p.above()))safe.add(p.immutable());if(!safe.isEmpty()){var p=safe.get(l.random.nextInt(safe.size()));entity.teleportTo(p.getX()+.5,p.getY(),p.getZ()+.5);}}
    public static void freeze(ServerLevel l,BlockPos center){
        // 4.32 returns after the first non-null Block, even air. Do not silently turn this
        // ordinary legacy bug into a nearest-water search and thereby rebalance the effect.
        var p=center.offset(-10,-10,-10);if(!l.hasChunkAt(p))return;var s=l.getBlockState(p);
        if(s.is(Blocks.WATER))l.setBlockAndUpdate(p,Blocks.ICE.defaultBlockState());
        else if(!s.is(Blocks.SNOW)&&l.canSeeSky(p)&&l.isEmptyBlock(p.above()))l.setBlockAndUpdate(p.above(),Blocks.SNOW.defaultBlockState());
    }
    public static void spawn(ServerLevel l,BlockPos p,String aspect){boolean peaceful=aspect.equals("victus");int light=l.getMaxLocalRawBrightness(p.above());if((peaceful?light<8:light>8)||!l.isEmptyBlock(p.above())||!l.isEmptyBlock(p.above(2))||l.random.nextInt(1000)!=1)return;if(!InfusingFocus.drain(l,p,aspect,8))return;EntityType<?>[] choices=peaceful?new EntityType[]{EntityType.PIG,EntityType.SHEEP,EntityType.COW,EntityType.CHICKEN}:new EntityType[]{EntityType.CREEPER,EntityType.SKELETON,EntityType.SPIDER,EntityType.ZOMBIE,EntityType.SLIME,EntityType.ENDERMAN};var mob=choices[l.random.nextInt(choices.length)].create(l);if(mob!=null){mob.moveTo(p.getX()+.5,p.getY()+1,p.getZ()+.5,0,0);l.addFreshEntity(mob);var entry=InfusionData.at(l,p);if(entry!=null)entry.settings.putLong("spawnCooldown",l.getGameTime()+40);}}
    public static void swap(ServerLevel a,BlockPos pa,ServerLevel b,BlockPos pb){var sa=a.getBlockState(pa);var sb=b.getBlockState(pb);if(sa.getDestroySpeed(a,pa)<0||sb.getDestroySpeed(b,pb)<0)return;var ta=a.getBlockEntity(pa);var tb=b.getBlockEntity(pb);var na=ta==null?null:ta.saveWithoutMetadata();var nb=tb==null?null:tb.saveWithoutMetadata();a.removeBlockEntity(pa);b.removeBlockEntity(pb);a.setBlock(pa,sb,2);b.setBlock(pb,sa,2);if(nb!=null&&a.getBlockEntity(pa)!=null)a.getBlockEntity(pa).load(nb);if(na!=null&&b.getBlockEntity(pb)!=null)b.getBlockEntity(pb).load(na);a.sendBlockUpdated(pa,sa,sb,3);b.sendBlockUpdated(pb,sb,sa,3);a.updateNeighborsAt(pa,sb.getBlock());b.updateNeighborsAt(pb,sa.getBlock());}
}
