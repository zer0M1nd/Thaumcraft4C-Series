package thmodern.vat;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.aspect.TCAspects;
import thmodern.Horizons;

/** The top centre of a vat owns its inventory and detached creature, never a second live world entity. */
public final class VatEntity extends BaseContainerBlockEntity {
    public static final int IDLE=0,CLONING=1,INFUSING=2,GROWING_EFFIGY=3,EFFIGY=4;
    private float effigyHealth;
    private final Set<String> selfInfusions=new LinkedHashSet<>();
    public Set<String> selfInfusions(){return Set.copyOf(selfInfusions);}
    boolean canInfuseSelf(VatInfusionRecipe recipe){
        var output=recipe.output();if(!hasEffigy()||!recipe.target(null,true)||!output.has("kind")||!output.get("kind").getAsString().equals("self")||!output.has("id"))return false;
        String key=output.get("id").getAsString();return SelfInfusions.TYPES.contains(key)&&!selfInfusions.contains(key);
    }
    boolean infuseSelf(VatInfusionRecipe recipe){if(!canInfuseSelf(recipe))return false;selfInfusions.add(recipe.output().get("id").getAsString());changed();return true;}
    boolean damageInfusionSubject(float damage){
        if(hasEffigy()){effigyHealth=Math.max(0,effigyHealth-damage);return hasEffigy();}
        if(subject==null)return false;subject.setHealth(subject.getHealth()-damage);return subject.isAlive();
    }
    public boolean hasEffigy(){return effigyHealth>0;}
    public float effigyHealth(){return effigyHealth;}
    public boolean readyForSoul(){
        if(!(level instanceof ServerLevel)||mode!=EFFIGY||!hasEffigy()||subject!=null||savedSubject!=null||playerId!=null)return false;
        for(int part=0;part<36;part++){var pos=worldPosition.offset(VatStructure.offset(part));var state=level.getBlockState(pos);if(!state.is(Horizons.VAT.get())||state.getValue(VatBlock.PART)!=part)return false;}
        return true;
    }
    public boolean inhabitEffigy(ServerPlayer player){
        if(!readyForSoul()||player.isSpectator())return false;var server=(ServerLevel)level;
        player.teleportTo(server,worldPosition.getX()+.5,worldPosition.getY()-1.75,worldPosition.getZ()+.5,player.getYRot(),player.getXRot());
        if(player.level()!=server||player.distanceToSqr(worldPosition.getX()+.5,worldPosition.getY()-1.75,worldPosition.getZ()+.5)>.01)return false;
        var abilities=Set.copyOf(selfInfusions);selfInfusions.clear();effigyHealth=0;mode=IDLE;progress=0;reservedSample=ItemStack.EMPTY;essentia.clear();
        subject=player;playerId=player.getUUID();SelfInfusions.replace(player,abilities);changed();return true;
    }
    private final VatInfusionProcess infusion=new VatInfusionProcess();
    private final NonNullList<ItemStack> items=NonNullList.withSize(2,ItemStack.EMPTY);
    private final VatEssentia essentia=new VatEssentia();
    private int mode,progress;
    private LivingEntity subject;
    private CompoundTag savedSubject;
    private UUID playerId;
    private ItemStack reservedSample=ItemStack.EMPTY;
    public VatEntity(BlockPos pos,BlockState state){super(Horizons.VAT_ENTITY.get(),pos,state);}
    public int mode(){return mode;}
    public int progress(){return progress;}
    public VatEssentia essentia(){return essentia;}
    public boolean beginInfusion(ServerPlayer player){return infusion.begin(this,player);}
    void startInfusion(){mode=INFUSING;progress=0;changed();}
    void finishInfusion(){mode=hasEffigy()?EFFIGY:IDLE;essentia.clear();changed();}
    void syncInfusion(){changed();}
    public int instability(){return infusion.instability();}
    public LivingEntity subject(){return subject;}
    public boolean holds(Player player){return !isRemoved()&&player.getUUID().equals(playerId);}
    public net.minecraft.world.phys.Vec3 anchor(){return new net.minecraft.world.phys.Vec3(worldPosition.getX()+.5,worldPosition.getY()-1.75,worldPosition.getZ()+.5);}
    public static boolean canUseSamples(ServerPlayer player){return player.isCreative()||thaumcraft.api.ThaumcraftApiHelper.isResearchComplete(player,"incarnationVat");}
    /** Same sample slot and research gate as the GUI; useful when interacting directly with the glass. */
    public boolean insertEffigy(ServerPlayer player,ItemStack held){
        if(!held.is(Horizons.CORPSE_EFFIGY.get())||!stillValid(player)||!canUseSamples(player)||mode!=IDLE||subject!=null||savedSubject!=null||playerId!=null||!getItem(0).isEmpty())return false;
        setItem(0,held.copyWithCount(1));if(!player.isCreative())held.shrink(1);return true;
    }
    boolean transformSubject(VatInfusionRecipe recipe){
        if(mode!=INFUSING||subject==null||playerId!=null)return false;
        var replacement=CreatureTransformation.prepare(subject,recipe);if(replacement==null)return false;
        subject=replacement;savedSubject=null;locate(subject);changed();return true;
    }
    public boolean exchangeJar(ServerPlayer player,ItemStack held){
        if(held.is(Horizons.CREATURE_JAR_ITEM.get())&&held.hasTag()&&held.getTag().contains("VatRecovery",Tag.TAG_COMPOUND)){
            if(!stillValid(player)||mode!=IDLE||subject!=null||savedSubject!=null||playerId!=null||hasEffigy()||!isEmpty()||!essentia.stored().isEmpty())return false;
            var payload=held.getTag().getCompound("VatRecovery").copy();
            if(payload.hasUUID("Player"))return false;
            var saved=payload.getCompound("Subject");
            if(saved.hasUUID("UUID")&&player.serverLevel().getEntity(saved.getUUID("UUID"))!=null)return false;
            var prepared=new VatEntity(worldPosition,getBlockState());prepared.load(payload);
            if(prepared.mode==INFUSING&&!prepared.infusion.active())return false;
            load(payload);held.shrink(1);changed();return true;
        }
        if(mode==EFFIGY&&hasEffigy()&&selfInfusions.size()<2&&stillValid(player)&&thmodern.containment.SoulJar.contains(held)){
            var villager=thmodern.containment.SoulJar.body(held,level);if(villager==null||!thmodern.containment.CreatureJar.exchange(player,held,new ItemStack(tc4revival.content.TCItems.ESSENTIA_JAR.get())))return false;
            subject=villager;locate(subject);effigyHealth=0;selfInfusions.clear();mode=IDLE;essentia.clear();changed();return true;
        }
        if(mode!=IDLE||!stillValid(player))return false;
        if(thmodern.containment.SoulJar.contains(held))return false;
        if(held.is(Horizons.CREATURE_JAR_ITEM.get())&&subject==null&&savedSubject==null&&playerId==null){
            var prepared=thmodern.containment.CreatureJar.read(held,level);if(prepared==null)return false;
            if(!thmodern.containment.CreatureJar.exchange(player,held,new ItemStack(tc4revival.content.TCItems.ESSENTIA_JAR.get())))return false;
            subject=prepared;locate(subject);essentia.clear();progress=0;changed();return true;
        }
        if(thmodern.containment.CreatureJar.empty(held)&&subject!=null&&!(subject instanceof Player)){
            var output=thmodern.containment.CreatureJar.bottle(subject);if(output.isEmpty()||!thmodern.containment.CreatureJar.exchange(player,held,output))return false;
            subject=null;savedSubject=null;essentia.clear();changed();return true;
        }
        return false;
    }
    public LivingEntity renderSubject(){
        if(level!=null&&level.isClientSide){restoreSubject();if(subject!=null&&!(subject instanceof Player)){
            VatContainment.settlePreview(subject);subject.tickCount=(int)(level.getGameTime()&0x7fffffff);
        }}return subject;
    }
    private void changed(){setChanged();if(level!=null&&!level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    private void locate(LivingEntity entity){
        double x=worldPosition.getX()+.5,y=worldPosition.getY()-1.75,z=worldPosition.getZ()+.5;
        if(entity instanceof ServerPlayer player&&level instanceof ServerLevel server){if(player.distanceToSqr(x,y,z)>.0025)player.teleportTo(server,x,y,z,player.getYRot(),player.getXRot());}
        else {entity.moveTo(x,y,z,entity.getYRot(),entity.getXRot());VatContainment.settlePreview(entity);}
    }
    public boolean capture(LivingEntity entity){
        if(!(level instanceof ServerLevel)||entity.level()!=level||mode!=IDLE||subject!=null||savedSubject!=null||playerId!=null||!entity.isAlive()||entity.isPassenger()||entity.isVehicle())return false;
        if(entity instanceof Player player){if(!player.isAlive()||player.isSpectator())return false;playerId=player.getUUID();subject=player;locate(player);}
        else {
            var tag=new CompoundTag();if(!entity.save(tag))return false;
            var clone=EntityType.create(tag,level).orElse(null);if(!(clone instanceof LivingEntity living))return false;
            subject=living;locate(subject);entity.discard();
        }
        essentia.clear();progress=0;changed();return true;
    }
    public boolean release(){
        if(!(level instanceof ServerLevel server)||mode!=IDLE||subject==null)return false;
        if(subject instanceof ServerPlayer player){player.teleportTo(server,worldPosition.getX()+.5,worldPosition.getY()+(level.getBlockState(worldPosition.above()).is(Horizons.SOUL_BEACON.get())?2:1),worldPosition.getZ()+.5,player.getYRot(),player.getXRot());}
        else {
            subject.moveTo(worldPosition.getX()+.5,worldPosition.getY()+1,worldPosition.getZ()+.5,subject.getYRot(),0);
            if(!server.noCollision(subject)||!server.addFreshEntity(subject)){locate(subject);return false;}
        }
        subject=null;playerId=null;savedSubject=null;essentia.clear();changed();return true;
    }
    public void destroySubject(){
        boolean pollute=playerId==null&&!(subject instanceof Player)&&(subject!=null||savedSubject!=null||hasEffigy()||mode==INFUSING);
        if(subject instanceof ServerPlayer player&&level instanceof ServerLevel server)player.teleportTo(server,worldPosition.getX()+.5,worldPosition.getY()+1,worldPosition.getZ()+.5,player.getYRot(),player.getXRot());
        else if((subject!=null||savedSubject!=null)&&level instanceof ServerLevel server){
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION,worldPosition.getX()+.5,worldPosition.getY()-1,worldPosition.getZ()+.5,1,0,0,0,0);
            // The detached subject cannot leave world drops when its vat is destroyed.
        }
        subject=null;savedSubject=null;playerId=null;effigyHealth=0;selfInfusions.clear();mode=IDLE;progress=0;essentia.clear();infusion.clear();changed();
        if(pollute&&level instanceof ServerLevel server){
            // Clear the subject before the explosion can re-enter any destruction callbacks.
            server.explode(null,worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5,.5f,net.minecraft.world.level.Level.ExplosionInteraction.NONE);
            for(int attempt=0;attempt<25;attempt++){
                var pos=worldPosition.offset(server.random.nextInt(8)-server.random.nextInt(8),server.random.nextInt(8)-server.random.nextInt(8),server.random.nextInt(8)-server.random.nextInt(8));
                if(server.isOutsideBuildHeight(pos)||!server.hasChunkAt(pos)||!server.isEmptyBlock(pos))continue;
                var block=pos.getY()<worldPosition.getY()?tc4revival.content.TCArcane.FLUX_GOO.get():tc4revival.content.TCArcane.FLUX_GAS.get();
                server.setBlock(pos,block.defaultBlockState().setValue(tc4revival.content.FiniteFluidBlock.QUANTA,8),3);
            }
        }
    }
    /** Structural dismantling preserves the complete paid transaction, including old saves
     * that have no individual-item refund ledger. Instability deaths still use destroySubject. */
    public void recoverOnDismantle(){
        if(!(level instanceof ServerLevel server))return;
        if(playerId!=null){
            var player=server.getServer().getPlayerList().getPlayer(playerId);
            if(player!=null&&player.level()==level)player.teleportTo(server,worldPosition.getX()+.5,worldPosition.getY()+1,worldPosition.getZ()+.5,player.getYRot(),player.getXRot());
            playerId=null;subject=null;
        }
        ItemStack recovery=ItemStack.EMPTY;
        if(subject!=null||savedSubject!=null||hasEffigy()||mode!=IDLE||!essentia.stored().isEmpty()){
            var payload=saveWithoutMetadata();
            recovery=new ItemStack(Horizons.CREATURE_JAR_ITEM.get());
            recovery.getOrCreateTag().put("VatRecovery",payload);
            if(payload.contains("Subject",Tag.TAG_COMPOUND))recovery.getOrCreateTag().put("Captured",payload.getCompound("Subject").copy());
        }else Containers.dropContents(level,worldPosition,this);
        // Clear before drops/neighbor callbacks can re-enter dismantling.
        items.clear();subject=null;savedSubject=null;playerId=null;effigyHealth=0;selfInfusions.clear();
        mode=IDLE;progress=0;reservedSample=ItemStack.EMPTY;essentia.clear();infusion.clear();changed();
        if(!recovery.isEmpty())Containers.dropItemStack(level,worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5,recovery);
    }
    private void restoreSubject(){
        if(savedSubject!=null){var loaded=EntityType.create(savedSubject,level).orElse(null);if(loaded instanceof LivingEntity living){subject=living;savedSubject=null;locate(subject);}}
        if(playerId!=null){
            if(!(subject instanceof Player)||subject.isRemoved())subject=level.getPlayerByUUID(playerId);
            if(subject!=null&&subject.distanceToSqr(worldPosition.getCenter())>36){subject=null;playerId=null;changed();}
        }
    }
    public void tick(){
        if(!(level instanceof ServerLevel))return;
        restoreSubject();
        if(savedSubject!=null)return; // An unavailable entity type must survive until its mod/data returns.
        if(playerId!=null&&subject==null){essentia.clear();return;}
        if(subject!=null){
            if(!subject.isAlive()||subject.isRemoved()){destroySubject();return;}
            else {subject.clearFire();subject.setAirSupply(subject.getMaxAirSupply());if(subject instanceof ServerPlayer player)VatContainment.restrain(player);}
        }
        if(progress>0){progress--;setChanged();}
        if(mode==INFUSING){if(!infusion.active())finishInfusion();else infusion.tick(this);return;}
        if(mode==IDLE){
            if(subject!=null)heal();
            else if(playerId==null&&getItem(0).is(Horizons.CORPSE_EFFIGY.get())){
                essentia.clear();essentia.demand(Map.of("victus",8,"sano",8));
                reservedSample=getItem(0).copyWithCount(1);mode=GROWING_EFFIGY;progress=80;changed();
            }
            else if(playerId==null&&!getItem(1).isEmpty()&&getItem(1).is(Horizons.NUTRIENTS.get())&&VatClone.sample(getItem(0))){
                try {
                    var costs=VatClone.costs(getItem(0));for(var key:costs.keySet())TCAspects.catalog().require(key);
                    if(VatClone.prepare(level,getItem(0))==null)return;
                    essentia.clear();essentia.demand(costs);reservedSample=getItem(0).copyWithCount(1);mode=CLONING;progress=40;changed();
                }catch(IllegalArgumentException invalid){return;}
            }else if(playerId==null)essentia.clear();
        }else if(mode==GROWING_EFFIGY){
            if(!getItem(0).is(Horizons.CORPSE_EFFIGY.get())||!ItemStack.isSameItemSameTags(reservedSample,getItem(0)))abortClone();
            else if(progress==0&&essentia.consumeAll()){
                items.get(0).shrink(1);reservedSample=ItemStack.EMPTY;essentia.clear();effigyHealth=20;mode=EFFIGY;changed();
            }
        }else if(mode==CLONING){
            if(subject!=null){if(progress==0){mode=IDLE;changed();}}
            else if(!ItemStack.isSameItemSameTags(reservedSample,getItem(0))||getItem(0).isEmpty()||!getItem(1).is(Horizons.NUTRIENTS.get()))abortClone();
            else if(progress==0&&essentia.paid()){
                var prepared=VatClone.prepare(level,getItem(0));
                if(prepared==null){abortClone();return;}
                // Full surcharge and valid creation were checked before this atomic payment.
                essentia.consumeAll();items.get(0).shrink(1);items.get(1).shrink(1);reservedSample=ItemStack.EMPTY;
                subject=prepared;locate(subject);progress=800;changed();
            }
        }
        if(progress==0&&essentia.wanted()!=null)VatStructure.pull(this);
    }
    private void abortClone(){mode=IDLE;progress=0;reservedSample=ItemStack.EMPTY;essentia.clear();changed();}
    private static boolean negative(net.minecraft.world.effect.MobEffect effect){
        if(Set.of(MobEffects.POISON,MobEffects.BLINDNESS,MobEffects.HUNGER,MobEffects.WEAKNESS,MobEffects.WITHER,MobEffects.CONFUSION,MobEffects.DIG_SLOWDOWN,MobEffects.MOVEMENT_SLOWDOWN).contains(effect))return true;
        var key=ForgeRegistries.MOB_EFFECTS.getKey(effect);return key!=null&&key.getNamespace().equals("thaumcraft")&&Set.of("blurred_vision","infectious_vis_exhaust","flux_taint","thaumarhia","vis_exhaust").contains(key.getPath());
    }
    private void heal(){
        String strong=subject.getMobType()==MobType.UNDEAD?"exanimis":"sano",weak=subject.getMobType()==MobType.UNDEAD?"mortuus":"victus";
        if(progress==0&&subject.getHealth()<subject.getMaxHealth()){
            if(essentia.consume(strong,1)){subject.heal(8);progress=40;changed();}
            else if(essentia.consume(weak,1)){subject.heal(4);progress=50;changed();}
        }
        boolean harmful=subject.getActiveEffects().stream().anyMatch(effect->negative(effect.getEffect()));
        if(progress==0&&harmful&&essentia.consume(strong,1)){
            for(var effect:List.copyOf(subject.getActiveEffects()))if(negative(effect.getEffect()))subject.removeEffect(effect.getEffect());
            progress=50;harmful=false;changed();
        }
        if(progress==0&&subject instanceof Player player&&player.getFoodData().needsFood()&&essentia.consume("fames",1)){player.getFoodData().eat(4,2);progress=50;changed();}
        var demand=new LinkedHashMap<String,Integer>();
        if(subject.getHealth()<subject.getMaxHealth()){demand.put(strong,1);demand.put(weak,1);}
        if(harmful)demand.put(strong,1);
        if(subject instanceof Player player&&player.getFoodData().needsFood())demand.put("fames",1);
        essentia.demand(demand);
    }
    public int accept(String key,int amount){
        if(level==null||level.isClientSide||progress>0)return 0;
        int accepted=essentia.accept(key,amount);if(accepted>0)changed();return accepted;
    }
    @Override protected Component getDefaultName(){return Component.translatable("container.thaumichorizons.vat");}
    @Override protected AbstractContainerMenu createMenu(int id,Inventory inventory){return new VatMenu(id,inventory,worldPosition,this);}
    @Override public int getContainerSize(){return 2;}
    @Override public boolean isEmpty(){return items.stream().allMatch(ItemStack::isEmpty);}
    @Override public ItemStack getItem(int slot){return slot>=0&&slot<2?items.get(slot):ItemStack.EMPTY;}
    @Override public ItemStack removeItem(int slot,int amount){var removed=ContainerHelper.removeItem(items,slot,amount);if(!removed.isEmpty())changed();return removed;}
    @Override public ItemStack removeItemNoUpdate(int slot){return ContainerHelper.takeItem(items,slot);}
    @Override public void setItem(int slot,ItemStack stack){if(slot<0||slot>=2)return;items.set(slot,stack);stack.setCount(Math.min(stack.getCount(),stack.getMaxStackSize()));changed();}
    @Override public void clearContent(){items.clear();changed();}
    @Override public boolean stillValid(Player player){return !isRemoved()&&level!=null&&level.getBlockEntity(worldPosition)==this&&player.distanceToSqr(worldPosition.getCenter())<=64;}
    @Override public boolean canPlaceItem(int slot,ItemStack stack){return slot==0?VatClone.input(stack):slot==1&&stack.is(Horizons.NUTRIENTS.get());}
    @Override protected void saveAdditional(CompoundTag tag){
        super.saveAdditional(tag);ContainerHelper.saveAllItems(tag,items);tag.putInt("Mode",mode);tag.putInt("Progress",progress);tag.put("ReservedSample",reservedSample.save(new CompoundTag()));
        if(playerId!=null)tag.putUUID("Player",playerId);
        else if(subject!=null){var value=new CompoundTag();subject.save(value);tag.put("Subject",value);}else if(savedSubject!=null)tag.put("Subject",savedSubject.copy());
        var required=new CompoundTag();essentia.required().forEach(required::putInt);tag.put("Demand",required);
        var stored=new CompoundTag();essentia.stored().forEach(stored::putInt);tag.put("Essentia",stored);
        tag.put("Infusion",infusion.save());
        tag.putFloat("EffigyHealth",effigyHealth);
        tag.put("SelfInfusions",SelfInfusions.write(selfInfusions));
    }
    @Override public void load(CompoundTag tag){
        super.load(tag);items.clear();ContainerHelper.loadAllItems(tag,items);mode=Math.max(IDLE,Math.min(EFFIGY,tag.getInt("Mode")));progress=Math.max(0,Math.min(800,tag.getInt("Progress")));
        float health=tag.getFloat("EffigyHealth");effigyHealth=Float.isFinite(health)?Math.max(0,Math.min(20,health)):0;
        if(mode==EFFIGY&&effigyHealth==0)mode=IDLE;
        selfInfusions.clear();if(hasEffigy())selfInfusions.addAll(SelfInfusions.read(tag.getCompound("SelfInfusions")));
        infusion.load(tag.getCompound("Infusion"));
        reservedSample=ItemStack.of(tag.getCompound("ReservedSample"));subject=null;playerId=tag.hasUUID("Player")?tag.getUUID("Player"):null;
        savedSubject=playerId==null&&tag.contains("Subject",Tag.TAG_COMPOUND)?tag.getCompound("Subject").copy():null;
        essentia.clear();var demand=new LinkedHashMap<String,Integer>();var required=tag.getCompound("Demand");
        for(var key:required.getAllKeys()){int count=required.getInt(key);if(count>0&&count<=32767&&demand.size()<128)demand.put(key,count);}
        essentia.demand(demand);var stored=tag.getCompound("Essentia");for(var key:demand.keySet())essentia.accept(key,stored.getInt(key));
    }
    @Override public CompoundTag getUpdateTag(){return saveWithoutMetadata();}
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
}
