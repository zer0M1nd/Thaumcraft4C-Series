package advancedthaumaturgy;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraft.world.level.ClipContext;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import tc4revival.aspect.*;
import tc4revival.content.*;
import thaumcraft.api.aspects.IEssentiaStorage;

public class MachineEntity extends SyncedEntity implements EssentiaTransport,IEssentiaStorage,RevealedAspectContainer {
    public static final Map<String,Integer> FUELS=Map.of("ignis",4,"terra",2,"aer",2,"aqua",2,"ordo",4,"perditio",2,"arbor",1,"herba",1,"metallum",1,"potentia",8);
    private AspectAmounts stored=new AspectAmounts();
    private final EssentiaAccess jars=new EssentiaAccess();
    private boolean active,ticket;
    private int ticks,cursor,herbaHalf,energy,burnEnergy,burnValue,ammo,idleTicks;
    private UUID target;
    private final List<Runnable> peripheralInvalidations=new ArrayList<>();
    public void addPeripheralInvalidation(Runnable callback){peripheralInvalidations.add(callback);}
    private LazyOptional<IEnergyStorage> energyCap=createEnergyCap();
    private LazyOptional<IEnergyStorage> createEnergyCap(){return LazyOptional.of(()->new IEnergyStorage(){
        public int receiveEnergy(int max,boolean simulate){return 0;}
        public int extractEnergy(int max,boolean simulate){int n=Math.min(Math.max(0,max),Math.min(energy,ATConfig.output()));if(!simulate&&n>0){energy-=n;setChanged();}return n;}
        public int getEnergyStored(){return energy;}public int getMaxEnergyStored(){return ATConfig.storage();}public boolean canExtract(){return true;}public boolean canReceive(){return false;}
    });}
    public MachineEntity(BlockPos pos,BlockState state){super(AdvancedThaumaturgy.MACHINE_ENTITY.get(),pos,state);}
    public MachineBlock.Kind kind(){return ((MachineBlock)getBlockState().getBlock()).kind;}
    public boolean active(){return active;}
    public int energy(){return energy;}
    public int remainingFuelEnergy(){return burnEnergy;}
    public void setActive(boolean value){
        if(kind()!=MachineBlock.Kind.WATCHFUL&&kind()!=MachineBlock.Kind.HEALING&&kind()!=MachineBlock.Kind.FIRE)return;
        if(active==value)return;active=value;ammo=0;target=null;idleTicks=0;sync();
        if(kind()==MachineBlock.Kind.WATCHFUL)updateTicket();
    }
    private void updateTicket(){
        if(!(level instanceof ServerLevel server))return;
        boolean want=active&&ATConfig.enabled("microlith");
        if(want!=ticket){
            // Forge returns false for an already restored ticket, not just for failure.
            // This mod is registered, so after an idempotent add/remove the requested state holds.
            net.minecraftforge.common.world.ForgeChunkManager.forceChunk(server,AdvancedThaumaturgy.ID,worldPosition,worldPosition.getX()>>4,worldPosition.getZ()>>4,want,true);ticket=want;
        }
    }
    public void releaseTicket(){if(level instanceof ServerLevel server){net.minecraftforge.common.world.ForgeChunkManager.forceChunk(server,AdvancedThaumaturgy.ID,worldPosition,worldPosition.getX()>>4,worldPosition.getZ()>>4,false,true);ticket=false;}}
    @Override public void onLoad(){super.onLoad();if(kind()==MachineBlock.Kind.WATCHFUL&&active)updateTicket();}
    private List<String> accepted(){return switch(kind()){
        case FERTILIZER->List.of("aqua","herba");case ENGINE->ATConfig.fuels().keySet().stream().sorted().toList();case HEALING->List.of("sano");case FIRE->List.of("ignis");case FLUX->List.of("vitium");default->List.of();};}
    @Override public AspectAmounts contents(){return stored.copy();}
    @Override public AspectAmounts revealedAspects(){return contents();}
    @Override public int totalCapacity(){return kind()==MachineBlock.Kind.FERTILIZER?128:64;}
    @Override public int capacity(Aspect a){return a!=null&&accepted().contains(a.tag())?64:0;}
    @Override public int insert(Aspect a,int amount,boolean simulate){
        if(amount<0)throw new IllegalArgumentException("Negative essentia");
        if(a==null||kind()==MachineBlock.Kind.FLUX||capacity(a)==0)return 0;
        if(kind()==MachineBlock.Kind.ENGINE&&!stored.isEmpty()&&stored.amount(a)==0)return 0;
        int n=Math.min(amount,Math.max(0,capacity(a)-stored.amount(a)));if(!simulate&&n>0){stored.add(a,n);sync();}return n;
    }
    @Override public int extract(Aspect a,int amount,boolean simulate){
        if(amount<0)throw new IllegalArgumentException("Negative essentia");if(kind()!=MachineBlock.Kind.FLUX)return 0;
        int n=Math.min(amount,stored.amount(a));if(!simulate&&n>0){stored.remove(a,n);sync();}return n;
    }
    public boolean phial(Player player,InteractionHand hand){
        var stack=player.getItemInHand(hand);if(!(stack.getItem() instanceof EssentiaPhialItem item))return false;
        var values=item.storedAspects(stack);var a=values.size()==1?values.aspects().get(0):kind()==MachineBlock.Kind.FLUX?TCAspects.catalog().require("vitium"):null;
        boolean fill=values.isEmpty();if(a==null||(fill?extract(a,8,true):insert(a,8,true))!=8)return false;
        if(level.isClientSide)return true;
        if(fill)extract(a,8,false);else insert(a,8,false);
        stack.shrink(1);var returned=fill?EssentiaPhialItem.filled(a):new ItemStack(TCItems.EMPTY_PHIAL.get());
        if(!player.getInventory().add(returned))player.drop(returned,false);player.containerMenu.broadcastChanges();return true;
    }
    @Override public boolean connectable(Direction side){return side!=null&&side!=Direction.UP&&!accepted().isEmpty()&&kind()!=MachineBlock.Kind.FERTILIZER;}
    @Override public boolean canInput(Direction side){return connectable(side)&&kind()!=MachineBlock.Kind.FLUX;}
    @Override public boolean canOutput(Direction side){return connectable(side)&&kind()==MachineBlock.Kind.FLUX;}
    @Override public Aspect suctionType(Direction side){
        var possible=accepted();if(possible.isEmpty())return null;
        if(kind()==MachineBlock.Kind.ENGINE&&!stored.isEmpty())return stored.aspects().get(0);
        return TCAspects.catalog().require(possible.get(Math.floorMod(ticks/100,possible.size())));
    }
    @Override public int suction(Direction side){var a=suctionType(side);return canInput(side)&&a!=null&&insert(a,1,true)>0?128:0;}
    @Override public int minimumSuction(){return 128;}
    @Override public Aspect essentiaType(Direction side){return stored.isEmpty()?null:stored.aspects().get(0);}
    @Override public int essentiaAmount(Direction side){var a=essentiaType(side);return a==null?0:stored.amount(a);}
    @Override public int takeEssentia(Aspect a,int n,Direction side){return canOutput(side)?extract(a,n,false):0;}
    @Override public int addEssentia(Aspect a,int n,Direction side){return canInput(side)?insert(a,n,false):0;}
    @Override public boolean renderExtendedTube(){return kind().microlith();}
    private void pullTubes(){
        for(var direction:Direction.values()){
            if(!canInput(direction))continue;var p=worldPosition.relative(direction);
            if(!level.hasChunkAt(p)||!(level.getBlockEntity(p) instanceof EssentiaTransport tube)||!tube.canOutput(direction.getOpposite()))continue;
            var a=tube.essentiaType(direction.getOpposite());if(a==null||insert(a,1,true)!=1||tube.essentiaAmount(direction.getOpposite())<1)continue;
            if(tube.takeEssentia(a,1,direction.getOpposite())==1){insert(a,1,false);break;}
        }
    }
    public void tick(){
        if(!(level instanceof ServerLevel server))return;++ticks;
        if(kind()==MachineBlock.Kind.WATCHFUL){updateTicket();return;}
        if(!ATConfig.enabled(kind().feature))return;
        if(kind()==MachineBlock.Kind.ENGINE){if(ATConfig.tubes())pullTubes();else if(ticks%2==0)for(String tag:accepted()){var a=TCAspects.catalog().require(tag);if(insert(a,1,true)==1&&jars.take(server,worldPosition,a,20,2)==1){insert(a,1,false);break;}}engine(server);return;}
        if(kind()==MachineBlock.Kind.FERTILIZER){fertilize(server);return;}
        if(kind()==MachineBlock.Kind.FLUX){flux(server);return;}
        if(kind()==MachineBlock.Kind.HEALING||kind()==MachineBlock.Kind.FIRE)pullTubes();
        if(!active)return;
        if(kind()==MachineBlock.Kind.HEALING&&ticks%41==0){
            var a=TCAspects.catalog().require("sano");if(stored.amount(a)==0)return;
            for(var p:server.getEntitiesOfClass(Player.class,new AABB(worldPosition).inflate(5)))if(p.isAlive()&&p.getHealth()<p.getMaxHealth()){
                p.heal(1);stored.remove(a,1);sync();tc4revival.network.FocusEffectPacket.nodeZap(server,worldPosition.getCenter(),p.position().add(0,1,0));break;
            }
        }
        if(kind()==MachineBlock.Kind.FIRE)sentry(server);
    }
    private void engine(ServerLevel server){
        if(burnEnergy==0&&!stored.isEmpty()&&energy<ATConfig.storage()){
            var a=stored.aspects().get(0);burnValue=ATConfig.fuels().getOrDefault(a.tag(),0);
            if(burnValue>0){stored.remove(a,1);burnEnergy=(int)Math.min(Integer.MAX_VALUE,(long)burnValue*ATConfig.energyPerValue());setChanged();}
        }
        int n=(int)Math.min(burnEnergy,Math.min((long)ATConfig.output()*burnValue,Math.max(0,ATConfig.storage()-energy)));
        if(n>0){energy+=n;burnEnergy-=n;setChanged();}
        // Original redstone signal controls active upward output, not generation or passive extraction.
        var above=server.getBlockEntity(worldPosition.above());
        if(server.hasNeighborSignal(worldPosition)&&energy>0&&above!=null)
            above.getCapability(ForgeCapabilities.ENERGY,Direction.DOWN).ifPresent(receiver->{int offered=Math.min(energy,ATConfig.output());int taken=Math.max(0,Math.min(offered,receiver.receiveEnergy(offered,false)));if(taken>0){energy-=taken;setChanged();}});
        if(ticks%20==0&&(n>0||energy>0)){sync();server.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,worldPosition.getX()+.5,worldPosition.getY()+.8,worldPosition.getZ()+.5,3,.25,.3,.25,.02);}
    }
    private void fertilize(ServerLevel server){
        if(ticks%2!=0)return;
        for(String tag:List.of("aqua","herba")){var a=TCAspects.catalog().require(tag);if(stored.amount(a)<64&&jars.take(server,worldPosition,a,16,2)==1){stored.add(a,1);sync();}}
        var herba=TCAspects.catalog().require("herba");if(herbaHalf<2&&stored.amount(herba)>0){stored.remove(herba,1);herbaHalf+=2;setChanged();}
        int index=cursor++%289;BlockPos p=worldPosition.offset(index/17-8,0,index%17-8);if(!server.hasChunkAt(p))return;
        var state=server.getBlockState(p);
        if(state.getBlock() instanceof FarmBlock&&state.getValue(FarmBlock.MOISTURE)<=4){server.setBlock(p,state.setValue(FarmBlock.MOISTURE,state.getValue(FarmBlock.MOISTURE)+1),3);return;}
        if(herbaHalf<1)return;
        if(state.getBlock() instanceof BonemealableBlock crop&&crop.isValidBonemealTarget(server,p,state,false)&&crop.isBonemealSuccess(server,server.random,p,state)){
            crop.performBonemeal(server,server.random,p,state);herbaHalf--;setChanged();server.levelEvent(1505,p,0);
        }else if(state.getBlock() instanceof FlowerBlock&&server.random.nextInt(100)<30){
            var target=p.relative(Direction.Plane.HORIZONTAL.getRandomDirection(server.random));if(server.isEmptyBlock(target)&&state.canSurvive(server,target)){server.setBlock(target,state,3);herbaHalf--;setChanged();}
        }
    }
    private void flux(ServerLevel server){
        if(ticks%20!=0)return;
        for(int x=-12;x<12;x++)for(int y=-12;y<12;y++)for(int z=-12;z<12;z++){
            var p=worldPosition.offset(x,y,z);if(!server.hasChunkAt(p))continue;var state=server.getBlockState(p);
            if(state.is(TCArcane.FLUX_GOO.get())){server.setBlockAndUpdate(p,TCArcane.FLUX_GAS.get().defaultBlockState());return;}
            if(ticks%60==0&&state.is(TCArcane.FLUX_GAS.get())){
                // removeBlock restores this custom fluid's legacy block instead of clearing it.
                server.setBlockAndUpdate(p,Blocks.AIR.defaultBlockState());var taint=TCAspects.catalog().require("vitium");if(server.random.nextInt(100)<=15&&stored.amount(taint)<64){stored.add(taint,1);sync();}
                tc4revival.network.FocusEffectPacket.nodeZap(server,p.getCenter(),worldPosition.getCenter());return;
            }
        }
    }
    private void sentry(ServerLevel server){
        if(ticks%10!=0)return;
        Monster mob=target==null?null:server.getEntity(target) instanceof Monster found?found:null;
        Vec3 src=worldPosition.getCenter().add(0,.4,0);
        if(mob==null||!mob.isAlive()||mob.distanceToSqr(src)>400||!visible(server,src,mob)){
            target=null;for(var candidate:server.getEntitiesOfClass(Monster.class,new AABB(worldPosition).inflate(8,4,8)))if(candidate.isAlive()&&visible(server,src,candidate)){mob=candidate;target=mob.getUUID();break;}
            if(target==null){idleTicks+=10;if(idleTicks>200)ammo=0;return;}
        }
        idleTicks=0;
        if(ammo==0){var fire=TCAspects.catalog().require("ignis");if(stored.amount(fire)==0)return;stored.remove(fire,1);ammo=10;}
        Vec3 direction=mob.getBoundingBox().getCenter().subtract(src).normalize();
        var shot=new SentryFireball(AdvancedThaumaturgy.SENTRY_FIREBALL.get(),server);shot.setPos(src.add(direction.scale(.8)));shot.setDeltaMovement(direction.scale(.6));shot.xPower=direction.x*.1;shot.yPower=direction.y*.1;shot.zPower=direction.z*.1;
        if(server.addFreshEntity(shot)){ammo--;sync();server.levelEvent(1018,worldPosition,0);}
    }
    private boolean visible(ServerLevel server,Vec3 src,Monster mob){
        // The ray starts inside the sentry's collision box. Only its own block is transparent.
        return server.clip(new ClipContext(src,mob.getBoundingBox().getCenter(),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,mob){
            @Override public net.minecraft.world.phys.shapes.VoxelShape getBlockShape(BlockState state,net.minecraft.world.level.BlockGetter world,BlockPos pos){
                return pos.equals(worldPosition)?net.minecraft.world.phys.shapes.Shapes.empty():super.getBlockShape(state,world,pos);
            }
        }).getType()==HitResult.Type.MISS;
    }
    @Override protected void saveAdditional(CompoundTag n){super.saveAdditional(n);AspectNbt.write(n,stored);n.putBoolean("active",active);n.putInt("energy",energy);n.putInt("burnEnergy",burnEnergy);n.putInt("burnValue",burnValue);n.putInt("herbaHalf",herbaHalf);n.putInt("cursor",cursor);n.putInt("ammo",ammo);}
    @Override public void load(CompoundTag n){super.load(n);stored=AspectNbt.read(n,TCAspects.catalog()).amounts();active=n.getBoolean("active");energy=Math.max(0,n.getInt("energy"));burnEnergy=Math.max(0,n.getInt("burnEnergy"));burnValue=Math.max(0,n.getInt("burnValue"));herbaHalf=Math.max(0,n.getInt("herbaHalf"));cursor=Math.max(0,n.getInt("cursor"));ammo=Math.max(0,n.getInt("ammo"));}
    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap,Direction side){if(kind()==MachineBlock.Kind.ENGINE&&cap==ForgeCapabilities.ENERGY&&(side==null||side==Direction.UP))return energyCap.cast();return super.getCapability(cap,side);}
    @Override public void invalidateCaps(){super.invalidateCaps();energyCap.invalidate();peripheralInvalidations.forEach(Runnable::run);peripheralInvalidations.clear();}
    @Override public void reviveCaps(){super.reviveCaps();energyCap=createEnergyCap();}
}
