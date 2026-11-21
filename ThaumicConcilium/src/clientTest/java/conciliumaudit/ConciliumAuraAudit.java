package conciliumaudit;

import com.mojang.authlib.GameProfile;
import java.util.*;
import net.minecraft.commands.Commands;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/** Test-only integration audit. It never registers replacement effects or synthesizes an outcome. */
@Mod.EventBusSubscriber(modid=ConciliumScenes.ID)
public final class ConciliumAuraAudit {
    private static Run active;
    private static Map<String,Object> latestReport=Map.of();
    @SubscribeEvent public static void commands(RegisterCommandsEvent event){
        var root=Commands.literal("conciliumaudit_aura").requires(source->source.hasPermission(2));
        var start=Commands.literal("run").executes(c->start(c.getSource().getLevel(),"all"));
        for(String mode:List.of("all","sins","terminus"))start.then(Commands.literal(mode).executes(c->start(c.getSource().getLevel(),mode)));
        root.then(start).then(Commands.literal("status").executes(c->{ConciliumScenes.guard();if(active!=null)active.record("running");if(latestReport.isEmpty())return 0;c.getSource().sendSuccess(()->Component.literal("Aura audit: "+latestReport.get("status")+"; phase="+latestReport.getOrDefault("phase","none")+"; pass="+latestReport.getOrDefault("passCount",0)+"; fail="+latestReport.getOrDefault("failCount",0)+"; unavailable="+latestReport.getOrDefault("unavailableCount",0)+"; incomplete="+latestReport.getOrDefault("incompleteCount",0)+". See concilium-audit/latest-aura.json"),false);return 1;}))
            .then(Commands.literal("cleanup").executes(c->{ConciliumScenes.guard();if(active!=null){try{active.finish("aborted");}finally{active=null;}}return 1;}));
        event.getDispatcher().register(root);
    }
    private static int start(ServerLevel level,String mode){
        ConciliumScenes.guard();if(active!=null)throw new IllegalStateException("Aura audit already running; use status or cleanup");
        if(!ModList.get().isLoaded("gadomancy")){publish(Map.of("status","unavailable","mode",mode,"reason","Real Gadomancy host is absent","visualAcceptance",false,"productionEffectsReplaced",false));return 0;}
        var run=new Run(level,mode);active=run;
        try{run.prepare();return 1;}catch(RuntimeException|Error failure){run.errors.add(failure.toString());try{run.finish("failed_setup");}finally{active=null;}throw failure;}
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event){
        if(event.phase!=TickEvent.Phase.END||active==null)return;
        try{if(active.tick())active=null;}catch(RuntimeException|Error failure){active.errors.add(failure.toString());try{active.finish("failed_exception");}finally{active=null;}}
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void joined(EntityJoinLevelEvent event){
        if(active!=null&&!event.isCanceled()&&event.getLevel()==active.level)active.joined(event.getEntity());
    }
    @SubscribeEvent public static void stopping(net.minecraftforge.event.server.ServerStoppingEvent event){if(active!=null){try{active.finish("server_stopping");}finally{active=null;}}}
    private static void publish(Map<String,Object> report){latestReport=Map.copyOf(report);ConciliumScenes.record("aura",report);}

    /** Optional host types are confined to this class, entered only after the mod-list check. */
    private static final class Run {
        final ServerLevel level;final String mode,runId=UUID.randomUUID().toString();
        final BlockPos base=new BlockPos(192,81,16),origin=base.above(2);
        final List<String> queue=new ArrayList<>();final List<Map<String,Object>> results=new ArrayList<>();final List<String> errors=new ArrayList<>();
        final Map<BlockPos,BlockState> replaced=new LinkedHashMap<>();final Set<Long> forced=new LinkedHashSet<>(),arenaChunks=new LinkedHashSet<>();
        final Map<UUID,Entity> owned=new LinkedHashMap<>();final List<Entity> pendingMeteors=new ArrayList<>();
        final Set<String> observedModes=new LinkedHashSet<>();final List<Map<String,Object>> accelerated=new ArrayList<>();
        String key="",phase="setup";int index=-1,wait,interval,nextDue,feed,attempts;long started;boolean syntheticDispatch;
        FakePlayer actor,outsidePlayer;Entity subject,control,other;Map<String,Object> previous=Map.of(),natural=Map.of();
        gadomancymodern.aura.AuraPylonEntity top,bottom;
        Run(ServerLevel level,String mode){this.level=level;this.mode=mode;started=level.getGameTime();
            if(!mode.equals("terminus"))for(String name:List.of("infernus","luxuria","gula","ira","invidia","desidia","superbia"))queue.add("forbidden_magic:"+name);
            if(!mode.equals("sins"))queue.add("thaumicomniverse:terminus");}
        void prepare(){
            for(int x=(origin.getX()-64)>>4;x<=(origin.getX()+64)>>4;x++)for(int z=(origin.getZ()-64)>>4;z<=(origin.getZ()+64)>>4;z++){
                long packed=ChunkPos.asLong(x,z);arenaChunks.add(packed);if(!level.getForcedChunks().contains(packed)){level.setChunkForced(x,z,true);forced.add(packed);}level.getChunk(x,z);
            }
            phase="await_arena";record("running");
        }
        boolean arenaReady(){for(long packed:arenaChunks){var chunk=new ChunkPos(packed);if(!level.areEntitiesLoaded(packed)||!level.isPositionEntityTicking(new BlockPos((chunk.x<<4)+8,base.getY(),(chunk.z<<4)+8)))return false;}return true;}
        void verifyAndPlaceArena(){
            var occupied=new AABB(origin.getX()-64,level.getMinBuildHeight(),origin.getZ()-64,origin.getX()+65,level.getMaxBuildHeight()+16,origin.getZ()+65);
            var existing=level.getEntities((Entity)null,occupied,e->true);
            if(!existing.isEmpty())throw new IllegalStateException("Refusing occupied audit arena; existing entities: "+existing.stream().limit(8).map(e->e.getType()+" / "+e.getUUID()).toList());
            for(var at:BlockPos.betweenClosed(base.offset(-8,-1,-8),base.offset(8,-1,8)))place(at,Blocks.SMOOTH_STONE.defaultBlockState());
        }
        void place(BlockPos at,BlockState state){
            at=at.immutable();if(!replaced.containsKey(at)){if(level.getBlockEntity(at)!=null)throw new IllegalStateException("Refusing to replace pre-existing block entity at "+at);replaced.put(at,level.getBlockState(at));}
            level.setBlockAndUpdate(at,state);
        }
        void next(){
            clearCase();index++;if(index>=queue.size()){phase="done";return;}
            key=queue.get(index);wait=0;feed=0;attempts=0;previous=Map.of();natural=Map.of();observedModes.clear();accelerated.clear();
            boolean lustBlocked=key.endsWith(":luxuria")&&fmmodern.ForbiddenConfig.value(fmmodern.ForbiddenConfig.NO_LUST);
            boolean terminusHost=ModList.get().isLoaded("thaumicomniverse")&&(ModList.get().isLoaded("avaritia")||ModList.get().isLoaded("endless"));
            if(lustBlocked||key.endsWith(":terminus")&&!terminusHost){results.add(Map.of("aspect",key,"status","unavailable","reason",lustBlocked?"Forbidden Magic noLust=true":"Terminus host absent"));next();return;}
            var effect=gadomancymodern.aura.AuraEffectRegistry.effects().get(key);
            if(effect==null||tc4revival.aspect.TCAspects.catalog().find(key).isEmpty()){results.add(Map.of("aspect",key,"status","fail","reason","Production aura registration or actual aspect missing"));next();return;}
            if(gadomancymodern.aura.AuraEffectsConfig.BLACKLIST.get().contains(key)){results.add(Map.of("aspect",key,"status","unavailable","reason","Actual Gadomancy blacklist"));next();return;}
            interval=effect.interval();int expected=switch(shortKey()){case "infernus"->5;case "luxuria"->40;case "gula"->20;case "ira","invidia"->80;case "desidia"->4;case "superbia"->8;default->600;};
            double expectedRange=shortKey().equals("superbia")||shortKey().equals("terminus")?32:8;
            if(interval!=expected||effect.range()!=expectedRange){results.add(Map.of("aspect",key,"status","fail","reason","Original interval/range mismatch","actualInterval",interval,"expectedInterval",expected,"actualRange",effect.range(),"expectedRange",expectedRange));next();return;}
            actor=player(origin.offset(-2,0,2),"AuraFixture");
            var knowledge=tc4revival.research.KnowledgeSavedData.get(level.getServer());knowledge.update(actor.getUUID(),actor.getRandom()::nextInt,k->{k.rememberResearch("gadomancy:aura_pylon");k.rememberResearch("gadomancy:aura_effects");k.setPool(tc4revival.aspect.TCAspects.catalog().require(key),0);return null;});
            var wand=tc4revival.content.TCItems.CASTING_WAND.get().create(tc4revival.content.WandParts.Rod.SILVERWOOD_STAFF,tc4revival.content.WandParts.Cap.THAUMIUM,false,true);actor.setItemInHand(InteractionHand.MAIN_HAND,wand);
            for(int i=0;i<4;i++)place(base.above(i),(i==3?gadomancymodern.Gadomancy.PYLON_CAP.get():gadomancymodern.Gadomancy.PYLON.get()).defaultBlockState());
            if(!gadomancymodern.aura.AuraPylonBlock.form(level,base.above(3),actor,InteractionHand.MAIN_HAND))throw new IllegalStateException("Real pylon form rejected fixture");
            top=(gadomancymodern.aura.AuraPylonEntity)level.getBlockEntity(origin);bottom=(gadomancymodern.aura.AuraPylonEntity)level.getBlockEntity(base);
            var crystal=tc4revival.content.TCItems.WISP_ESSENCE.get().of(tc4revival.aspect.TCAspects.catalog().require(key));var at=top.crystalPosition();spawn(new ItemEntity(level,at.x,at.y,at.z,crystal));phase="await_crystal";record("running");
        }
        String shortKey(){return key.substring(key.indexOf(':')+1);}
        FakePlayer player(BlockPos at,String name){var p=new FakePlayer(level,new GameProfile(UUID.randomUUID(),name));p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.setNoGravity(true);p.setInvulnerable(true);p.setPos(Vec3.atBottomCenterOf(at));ownGenerated(p);level.addNewPlayer(p);return p;}
        <T extends Entity>T spawn(T entity){if(entity==null)throw new IllegalStateException("Missing real fixture entity type");entity.setNoGravity(true);entity.setInvulnerable(true);ownGenerated(entity);if(entity instanceof Mob mob){mob.setNoAi(true);mob.setPersistenceRequired();}if(!level.addFreshEntity(entity))throw new IllegalStateException("Fixture entity insertion denied");return entity;}
        Cow cow(int x,int z){var c=EntityType.COW.create(level);c.setPos(Vec3.atBottomCenterOf(origin.offset(x,0,z)));return spawn(c);}
        Zombie zombie(int x,int z){var zed=EntityType.ZOMBIE.create(level);zed.setPos(Vec3.atBottomCenterOf(origin.offset(x,0,z)));zed.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.NETHERITE_HELMET));return spawn(zed);}
        ItemEntity drop(Item item,int count,int x,int z){var at=Vec3.atCenterOf(origin.offset(x,0,z));var e=new ItemEntity(level,at.x,at.y,at.z,new ItemStack(item,count));e.setPickUpDelay(32767);e.setDeltaMovement(Vec3.ZERO);return spawn(e);}
        void fixtures(){
            switch(shortKey()){
                case "infernus"->{subject=drop(Items.RAW_IRON,3,3,0);control=drop(Items.RAW_IRON,3,20,0);other=drop(Items.DIRT,4,3,3);}
                case "luxuria"->{var c=cow(3,0);c.setAge(2000);subject=c;var far=cow(20,0);far.setAge(2000);control=far;var baby=cow(3,3);baby.setAge(-24000);other=baby;}
                case "gula"->{actor.getFoodData().setFoodLevel(10);actor.getFoodData().setSaturation(0);outsidePlayer=player(origin.offset(20,0,0),"AuraOutside");outsidePlayer.getFoodData().setFoodLevel(10);outsidePlayer.getFoodData().setSaturation(0);subject=actor;control=outsidePlayer;}
                case "ira"->{subject=zombie(3,0);other=cow(4,2);control=zombie(24,0);}
                case "invidia"->{var z=zombie(3,0);z.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_SWORD));subject=z;var far=zombie(20,0);far.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_SWORD));control=far;actor.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.IRON_SWORD));}
                case "desidia"->{var c=cow(3,0);c.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,200,1));c.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,-1,0));subject=c;var far=cow(20,0);far.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,200,1));control=far;}
                case "superbia"->{subject=zombie(3,0);control=zombie(40,0);other=cow(3,3);}
                default->{}
            }
            nextDue=(top.ticks/interval+1)*interval;phase="natural_tick";previous=observe();
            feed+=bottom.addEssentia(tc4revival.aspect.TCAspects.catalog().require(key),15,Direction.DOWN);record("running");
        }
        boolean tick(){
            if(phase.equals("done")){finish("complete");return true;}
            if(++wait>1800)throw new IllegalStateException("Aura fixture timeout at "+key+" / "+phase);
            if(phase.equals("await_arena")){if(arenaReady()){verifyAndPlaceArena();next();}return false;}
            if(phase.equals("await_crystal")){if(key.equals(top.aspect)&&!top.observedCrystal.isEmpty())fixtures();return false;}
            if(!top.valid()||!key.equals(top.aspect))throw new IllegalStateException("Real pylon structure or held essence was lost");
            if(phase.equals("natural_tick")){
                if(bottom.amount<5)feed+=bottom.addEssentia(tc4revival.aspect.TCAspects.catalog().require(key),15-bottom.amount,Direction.DOWN);
                if(top.ticks<nextDue){previous=observe();return false;}
                var after=observe();var row=new LinkedHashMap<String,Object>();row.put("aspect",key);row.put("path","natural_world_block_entity_tick");row.put("pylonTicks",top.ticks);row.put("expectedDispatchTick",nextDue);row.put("interval",interval);row.put("essentiaSupplied",feed);row.put("before",previous);row.put("after",after);
                if(shortKey().equals("terminus")){var spawned=collectMeteors();row.put("spawned",spawned);row.put("observationResearch",discovered());row.put("status",spawned.stream().anyMatch(e->Boolean.TRUE.equals(e.get("inserted")))?"pass":"no_output_observed");natural=row;phase="accelerated_registry";record("running");return false;}
                boolean pass=check();row.put("status",pass?"pass":"fail");row.put("observationResearch",discovered());if(!discovered())row.put("status","fail");results.add(row);record("running");next();return false;
            }
            if(phase.equals("accelerated_registry")){
                // Deliberate dispatch acceleration, NOT elapsed pylon/world time. Each result
                // still comes from the unmodified production registry and its real RNG/world.
                attempts++;syntheticDispatch=true;try{gadomancymodern.aura.AuraEffectRegistry.distribute(key,level,origin,interval*(attempts+1));}finally{syntheticDispatch=false;}
                var spawned=collectMeteors();accelerated.add(Map.of("sample",attempts,"dispatchTickArgument",interval*(attempts+1),"spawned",spawned));
                if(attempts>=48||observedModes.containsAll(expectedMeteorModes())){
                    var row=new LinkedHashMap<String,Object>();row.put("aspect",key);row.put("natural",natural);row.put("acceleratedPath","production AuraEffectRegistry.distribute; synthetic schedule argument, no pylon tick rewrites");row.put("acceleratedSamples",List.copyOf(accelerated));row.put("observedModes",List.copyOf(observedModes));row.put("expectedHostModes",expectedMeteorModes());row.put("unavailableHostModes",unavailableMeteorModes());row.put("observationResearch",discovered());
                    row.put("status",!Boolean.TRUE.equals(natural.get("observationResearch"))?"fail":!"pass".equals(natural.get("status"))?"incomplete_natural_output":observedModes.containsAll(expectedMeteorModes())?"pass":"incomplete_random_coverage");row.put("scope","Real insertion only; meteor impact and visuals are separate checks. Missing random choice is not diagnosed as a product failure.");results.add(row);record("running");next();
                }
            }
            return false;
        }
        boolean discovered(){return tc4revival.research.KnowledgeSavedData.get(level.getServer()).snapshot(actor.getUUID()).map(k->k.researchMarkers().contains("gadomancy:aura/"+key.replace(':','/'))).orElse(false);}
        Map<String,Object> observe(){var out=new LinkedHashMap<String,Object>();out.put("worldTick",level.getGameTime());out.put("pylonTick",top.ticks);out.put("storedEssentia",bottom.amount);out.put("aspect",top.aspect);out.put("heldCrystal",top.observedCrystal.toString());if(actor!=null)out.put("actor",state(actor));if(subject!=null)out.put("subject",state(subject));if(control!=null)out.put("control",state(control));if(other!=null)out.put("other",state(other));return out;}
        Map<String,Object> state(Entity e){var out=new LinkedHashMap<String,Object>();out.put("uuid",e.getUUID().toString());out.put("id",ForgeRegistries.ENTITY_TYPES.getKey(e.getType()).toString());out.put("removed",e.isRemoved());out.put("visibleInWorld",level.getEntity(e.getUUID())==e);out.put("auditOwned",e.getTags().contains("concilium_aura_"+runId));out.put("position",e.position().toString());out.put("motion",e.getDeltaMovement().toString());if(e instanceof ItemEntity item)out.put("item",item.getItem().toString());if(e instanceof Cow cow)out.put("age",cow.getAge());if(e instanceof LivingEntity living){out.put("held",living.getMainHandItem().toString());out.put("effects",living.getActiveEffects().stream().map(f->Map.of("id",ForgeRegistries.MOB_EFFECTS.getKey(f.getEffect()).toString(),"duration",f.getDuration(),"amplifier",f.getAmplifier())).toList());}if(e instanceof Mob mob)out.put("target",mob.getTarget()==null?"none":mob.getTarget().getUUID().toString());if(e instanceof FakePlayer p){out.put("food",p.getFoodData().getFoodLevel());out.put("saturation",p.getFoodData().getSaturationLevel());}return out;}
        boolean check(){for(var e:new Entity[]{subject,control,other})if(e!=null&&(e.isRemoved()||level.getEntity(e.getUUID())!=e))return false;return switch(shortKey()){
            case "infernus"->((ItemEntity)subject).getItem().is(Items.IRON_INGOT)&&((ItemEntity)subject).getItem().getCount()==3&&((ItemEntity)control).getItem().is(Items.RAW_IRON)&&((ItemEntity)other).getItem().is(Items.DIRT);
            case "luxuria"->((Cow)subject).getAge()==0&&((Cow)control).getAge()>0&&((Cow)other).getAge()<0;
            case "gula"->actor.getFoodData().getFoodLevel()==11&&outsidePlayer.getFoodData().getFoodLevel()==10;
            case "ira"->((Mob)subject).getTarget()==other&&((Mob)control).getTarget()==null;
            case "invidia"->((LivingEntity)subject).getMainHandItem().isEmpty()&&((LivingEntity)control).getMainHandItem().is(Items.IRON_SWORD)&&actor.getMainHandItem().is(Items.IRON_SWORD)&&level.getEntitiesOfClass(ItemEntity.class,new AABB(origin).inflate(8),e->e.getItem().is(Items.IRON_SWORD)).stream().mapToInt(e->e.getItem().getCount()).sum()==1;
            case "desidia"->((LivingEntity)subject).getEffect(MobEffects.MOVEMENT_SPEED)!=null&&((LivingEntity)control).getEffect(MobEffects.MOVEMENT_SPEED)!=null&&((LivingEntity)subject).getEffect(MobEffects.MOVEMENT_SPEED).getDuration()>=((LivingEntity)control).getEffect(MobEffects.MOVEMENT_SPEED).getDuration()+19&&((LivingEntity)subject).getEffect(MobEffects.DAMAGE_RESISTANCE)!=null&&((LivingEntity)subject).getEffect(MobEffects.DAMAGE_RESISTANCE).isInfiniteDuration();
            case "superbia"->subject.getDeltaMovement().x>.8&&control.getDeltaMovement().horizontalDistanceSqr()<1e-8&&other.getDeltaMovement().horizontalDistanceSqr()<1e-8;
            default->false;};}
        void joined(Entity e){
            boolean naturalDispatch=phase.equals("natural_tick")&&top!=null&&top.ticks==nextDue;
            if(phase.equals("await_crystal")&&top!=null&&e instanceof gadomancymodern.aura.HeldEssenceEntity&&e.position().distanceToSqr(top.crystalPosition())<1e-8)ownGenerated(e);
            if(naturalDispatch&&key.endsWith(":invidia")&&subject!=null&&e instanceof ItemEntity item&&item.getItem().is(Items.IRON_SWORD)&&e.position().distanceToSqr(subject.position().add(0,.5,0))<1e-8)ownGenerated(e);
            if(key.endsWith(":terminus")&&(naturalDispatch||syntheticDispatch)&&expectedMeteorModes().contains(meteorMode(e))&&Math.abs(e.getX()-origin.getX())<64&&Math.abs(e.getZ()-origin.getZ())<64){ownGenerated(e);pendingMeteors.add(e);}
        }
        void ownGenerated(Entity e){e.addTag("concilium_aura_"+runId);owned.put(e.getUUID(),e);}
        String meteorMode(Entity e){if(e instanceof conciliummodern.ConciliumMoonstoneMeteor m)return m.shootingStar()?"shooting_star":"moonstone";if(e.getType()==tc4revival.content.TCEntities.EXPLOSIVE_ORB.get())return "stone";if(e.getType()==conciliummodern.RemainingMobs.OVERANIMATED.get())return "brethren_moon";if(e.getClass().getName().equals("wayoftime.bloodmagic.entity.projectile.EntityMeteor"))return "blood_magic";return "";}
        List<String> expectedMeteorModes(){var modes=new ArrayList<String>();modes.add("stone");if(ModList.get().isLoaded("bloodmagic"))modes.add("blood_magic");if(ModList.get().isLoaded("thaumichorizons"))modes.add("brethren_moon");if(ModList.get().isLoaded("arsmagicalegacy"))modes.addAll(List.of("moonstone","shooting_star"));return modes;}
        Map<String,String> unavailableMeteorModes(){var modes=new LinkedHashMap<String,String>();if(!ModList.get().isLoaded("bloodmagic"))modes.put("blood_magic","bloodmagic absent");if(!ModList.get().isLoaded("thaumichorizons"))modes.put("brethren_moon","thaumichorizons absent");if(!ModList.get().isLoaded("arsmagicalegacy")){modes.put("moonstone","arsmagicalegacy absent");modes.put("shooting_star","arsmagicalegacy absent");}return modes;}
        List<Map<String,Object>> collectMeteors(){var rows=new ArrayList<Map<String,Object>>();for(var e:pendingMeteors){boolean inserted=level.getEntity(e.getUUID())==e&&!e.isRemoved();String selected=meteorMode(e);rows.add(Map.of("uuid",e.getUUID().toString(),"type",ForgeRegistries.ENTITY_TYPES.getKey(e.getType()).toString(),"mode",selected,"inserted",inserted,"position",e.position().toString()));if(inserted)observedModes.add(selected);e.discard();}pendingMeteors.clear();return rows;}
        void clearCase(){
            RuntimeException failure=null;
            for(var entity:List.copyOf(owned.values()))try{if(entity.getTags().contains("concilium_aura_"+runId))entity.discard();}catch(RuntimeException problem){if(failure==null)failure=problem;else failure.addSuppressed(problem);}
            owned.clear();pendingMeteors.clear();actor=null;outsidePlayer=null;subject=null;control=null;other=null;
            for(int i=3;i>=0;i--)if(replaced.containsKey(base.above(i)))try{level.setBlockAndUpdate(base.above(i),replaced.get(base.above(i)));}catch(RuntimeException problem){if(failure==null)failure=problem;else failure.addSuppressed(problem);}
            top=null;bottom=null;if(failure!=null)throw failure;
        }
        void record(String status){var out=new LinkedHashMap<String,Object>();out.put("runId",runId);out.put("mode",mode);out.put("status",status);out.put("phase",phase);out.put("currentAspect",key);out.put("elapsedWorldTicks",level.getGameTime()-started);out.put("dimension",level.dimension().location().toString());out.put("arena",origin.toShortString());out.put("results",List.copyOf(results));out.put("errors",List.copyOf(errors));out.put("passCount",results.stream().filter(r->"pass".equals(r.get("status"))).count());out.put("failCount",results.stream().filter(r->"fail".equals(r.get("status"))).count());out.put("unavailableCount",results.stream().filter(r->"unavailable".equals(r.get("status"))).count());out.put("incompleteCount",results.stream().filter(r->String.valueOf(r.get("status")).startsWith("incomplete")).count());out.put("visualAcceptance",false);out.put("productionEffectsReplaced",false);out.put("currentPylonTicks",top==null?0:top.ticks);out.put("forcedChunksAdded",forced.size());publish(out);}
        void finish(String status){
            int before=errors.size();
            try{clearCase();}catch(RuntimeException failure){errors.add("Entity/case cleanup: "+failure);}
            for(var entry:replaced.entrySet())try{level.setBlockAndUpdate(entry.getKey(),entry.getValue());}catch(RuntimeException failure){errors.add("Block restore "+entry.getKey()+": "+failure);}
            for(long packed:forced)try{var pos=new ChunkPos(packed);level.setChunkForced(pos.x,pos.z,false);}catch(RuntimeException failure){errors.add("Chunk release "+packed+": "+failure);}
            record(errors.size()==before?status:"failed_cleanup_after_"+status);
        }
    }
    private ConciliumAuraAudit(){}
}
