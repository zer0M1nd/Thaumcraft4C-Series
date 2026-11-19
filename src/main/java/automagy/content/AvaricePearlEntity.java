package automagy.content;

import java.util.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.util.FakePlayer;
import thaumcraft.api.ThaumcraftApiHelper;
import tc4revival.research.*;

public final class AvaricePearlEntity extends ThrowableItemProjectile {
    private static final Map<UUID,Integer> DISCOVERY_ATTEMPTS=new java.util.WeakHashMap<>();
    private Vec3 origin=Vec3.ZERO;
    private boolean originKnown;
    private int grabbed,slot=-1;
    private boolean noReturn,resolved;
    public AvaricePearlEntity(EntityType<? extends AvaricePearlEntity> type,Level level){super(type,level);}
    public AvaricePearlEntity(Level level,Player player,boolean miser,int slot){
        super(Content.PEARL_ENTITY.get(),player,level);origin=player.position();originKnown=true;this.slot=slot;noReturn=player.getAbilities().instabuild;
        setItem(new ItemStack(miser?Content.MISERS_PEARL.get():Content.AVARICE_PEARL.get()));
    }
    public static AvaricePearlEntity dispensed(Level level,net.minecraft.core.Position position,ItemStack stack){
        var pearl=new AvaricePearlEntity(Content.PEARL_ENTITY.get(),level);pearl.setPos(position.x(),position.y(),position.z());
        pearl.origin=pearl.position();pearl.originKnown=true;pearl.setItem(stack.copyWithCount(1));return pearl;
    }
    @Override protected Item getDefaultItem(){return Content.AVARICE_PEARL.get();}
    private boolean miser(){return getItem().is(Content.MISERS_PEARL.get());}
    private ServerPlayer player(){return getOwner() instanceof ServerPlayer p&&!(p instanceof FakePlayer)&&!p.hasDisconnected()?p:null;}
    private List<ItemEntity> nearby(boolean flying){return level().getEntitiesOfClass(ItemEntity.class,new AABB(getX()-5,getY()-(flying?5:1.5),getZ()-5,getX()+5,getY()+5,getZ()+5),Entity::isAlive);}
    @Override public void tick(){
        super.tick();if(isRemoved()||resolved)return;
        if(!level().isClientSide&&miser()&&grabbed<10&&player()!=null)collect(nearby(true),true);
        if(level().isClientSide&&miser())level().addParticle(ParticleTypes.PORTAL,getX(),getY(),getZ(),0,.1,0);
    }
    @Override protected void onHit(HitResult hit){
        super.onHit(hit);if(level().isClientSide||resolved)return;resolved=true;
        var items=nearby(false);ServerPlayer player=player();ItemStack result=ItemStack.EMPTY,extra=ItemStack.EMPTY;
        if(!miser()){
            if(!items.isEmpty()){
                boolean eligible=player!=null&&ThaumcraftApiHelper.isResearchComplete(player,"automagy:GREEDYCHEST")&&!discovered(player);
                int attempts=eligible?DISCOVERY_ATTEMPTS.merge(player.getUUID(),1,Integer::sum):0;
                int roll=random.nextInt(100)+1;
                if(automagy.logic.PearlRules.fractures(items.size(),attempts,roll)){
                    result=new ItemStack(Content.SHATTERED_PEARL.get());long count=items.stream().mapToLong(i->i.getItem().getCount()).sum();
                    if(automagy.logic.PearlRules.goldBonus(count,random.nextInt(100)+1)){extra=new ItemStack(Items.GOLD_NUGGET,random.nextInt(3)+1);taint(player,3,100);}else taint(player,5,200);
                    if(eligible){KnowledgeSavedData.get(player.server).update(player.getUUID(),player.getRandom()::nextInt,k->k.rememberResearch("@automagy:AVARICIOUS"));DISCOVERY_ATTEMPTS.remove(player.getUUID());player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.automagy.avaricious"),false);}
                }else if(roll<21)taint(player,1,100);
            }else if(random.nextInt(10)==0)taint(player,.5f,50);
        }
        if(result.isEmpty()&&!noReturn)result=new ItemStack(miser()?Content.MISERS_PEARL.get():Content.AVARICE_PEARL.get());
        deliver(result,player);deliver(extra,player);collect(items,false);
        if(level() instanceof ServerLevel server)server.sendParticles(ParticleTypes.PORTAL,getX(),getY()+.5,getZ(),64,.5,1,.5,.2);
        playSound(net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT,1,.6f+random.nextFloat()/4);discard();
    }
    private boolean discovered(ServerPlayer player){return KnowledgeSavedData.get(player.server).snapshot(player.getUUID()).map(k->k.researchMarkers().contains("@automagy:AVARICIOUS")||k.researchMarkers().contains("automagy:AVARICIOUS")).orElse(false);}
    private Vec3 home(){return originKnown?origin:position();}
    private void taint(ServerPlayer player,float damage,int duration){
        if(player==null)return;
        var key=net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE,net.minecraft.resources.ResourceLocation.parse("thaumcraft:taint"));
        var holder=player.level().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).getHolderOrThrow(key);
        player.hurt(new net.minecraft.world.damagesource.DamageSource(holder),damage);player.addEffect(new net.minecraft.world.effect.MobEffectInstance(tc4revival.content.TCEffects.FLUX_TAINT.get(),duration));
    }
    private void deliver(ItemStack stack,ServerPlayer player){
        if(stack.isEmpty())return;
        if(player!=null){if(slot>=0&&slot<player.getInventory().getContainerSize()&&player.getInventory().getItem(slot).isEmpty()){player.getInventory().setItem(slot,stack);player.getInventory().setChanged();return;}
            if(player.getInventory().add(stack))return;}
        Vec3 target=player==null?home():player.position();Level targetLevel=player==null?level():player.level();var dropped=new ItemEntity(targetLevel,target.x,target.y+.5,target.z,stack);dropped.setNoPickUpDelay();targetLevel.addFreshEntity(dropped);
    }
    private void collect(List<ItemEntity> items,boolean flying){
        ServerPlayer player=player();
        for(ItemEntity item:items){
            if(!automagy.logic.PearlRules.canCollect(miser(),grabbed))break;if(!item.isAlive())continue;
            if(player!=null){
                if(item.level()!=player.level()){Entity moved=item.changeDimension(player.serverLevel());if(!(moved instanceof ItemEntity transferred))continue;item=transferred;}
                if(!flying)item.setPos(player.getX(),player.getY()+.5,player.getZ());
                item.setNoPickUpDelay();item.playerTouch(player);
            }else {Vec3 home=home();item.setPos(home.x,home.y+.5,home.z);}
            grabbed++;
        }
    }
    @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);if(originKnown){tag.putDouble("OriginX",origin.x);tag.putDouble("OriginY",origin.y);tag.putDouble("OriginZ",origin.z);}tag.putInt("Grabbed",grabbed);tag.putInt("Slot",slot);tag.putBoolean("NoReturn",noReturn);}
    @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);originKnown=tag.contains("OriginY");origin=new Vec3(tag.getDouble("OriginX"),tag.getDouble("OriginY"),tag.getDouble("OriginZ"));grabbed=Math.max(0,tag.getInt("Grabbed"));slot=tag.contains("Slot")?tag.getInt("Slot"):-1;noReturn=tag.getBoolean("NoReturn");}
    @Override public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getAddEntityPacket(){return net.minecraftforge.network.NetworkHooks.getEntitySpawningPacket(this);}
}
