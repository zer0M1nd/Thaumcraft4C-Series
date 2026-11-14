package thaumicexploration;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.*;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.*;
import tc4revival.aspect.*;
import thaumcraft.api.aspects.EssentiaBuffer;
import thaumcraft.api.aspects.IEssentiaStorage;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.api.aspects.IEssentiaVessel;

public class MachineEntity extends thaumcraft.api.visnet.TileVisNode implements IEssentiaStorage, IEssentiaTransport, IEssentiaVessel, tc4revival.content.RevealedAspectContainer, MenuProvider {
    public int progress, vis, storedWarp, warmup, space, rotationTicks;
    public float flux;
    public UUID owner;
    public boolean active, crafting, powered;
    public ItemStack template=ItemStack.EMPTY;
    private AspectAmounts remaining=new AspectAmounts();
    private final EssentiaBuffer buffer=new EssentiaBuffer(Integer.MAX_VALUE,a->true,this::sync);
    public final ItemStackHandler items=new ItemStackHandler(2) {
        @Override protected void onContentsChanged(int slot) { sync(); }
        @Override public boolean isItemValid(int slot,ItemStack stack) { return slot==0&&kind().equals("think_tank")&&(stack.is(Items.BOOK)||stack.is(Items.ENCHANTED_BOOK)); }
    };
    private LazyOptional<IItemHandler> itemCapability=LazyOptional.of(()->automation(false));
    private LazyOptional<IItemHandler> inputCapability=LazyOptional.of(()->automation(true));
    private LazyOptional<IFluidHandler> fluidCapability=LazyOptional.of(UrnFluid::new);
    public MachineEntity(BlockPos pos,BlockState state) { super(ExplorationMod.MACHINE.get(),pos,state); }
    public String kind(){return ((MachineBlock)getBlockState().getBlock()).kind;}
    private boolean kind(String value){return kind().equals(value);}
    @Override public int range(){return kind("everburn_urn")||kind("soul_brazier")?8:0;}
    @Override public boolean source(){return false;}
    @Override public boolean relay(){return false;}
    public void sync(){setChanged();if(level!=null&&!level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    public static Aspect aspect(String id){return TCAspects.catalog().require(id);}
    public void tick(){
        if(level==null)return;
        if(level.isClientSide){if(warmup>0)rotationTicks++;animate();return;}
        if(range()>0)tickNetwork();
        long tick=level.getGameTime();
        switch(kind()){
            case "think_tank" -> think();
            case "everfull_urn" -> water();
            case "everburn_urn" -> {if(tick%10==0&&vis*250+lavaRemainder<=3750){int received=consumeVis(aspect("ignis"),1);if(received>0){vis+=received;sync();}}}
            case "replicator" -> replicate();
            case "crucible_souls" -> {if(tick%10==0)souls();}
            case "trash_jar" -> {if(tick%5==0)pull();if(tick%6==0&&amount()>0){buffer.extract(aspect(),1,false);sync();}}
            case "soul_brazier" -> brazier();

        }
    }
    private void animate(){
        if(level.random.nextInt(3)!=0)return;
        var particle=switch(kind()){case "everfull_urn"->ParticleTypes.SPLASH;case "everburn_urn"->ParticleTypes.FLAME;case "soul_brazier"->active?ParticleTypes.SOUL_FIRE_FLAME:null;case "trash_jar"->ParticleTypes.WITCH;default->crafting?ParticleTypes.ENCHANT:null;};
        if(particle!=null)level.addParticle(particle,worldPosition.getX()+.5,worldPosition.getY()+1.05,worldPosition.getZ()+.5,(level.random.nextDouble()-.5)*.1,.12,(level.random.nextDouble()-.5)*.1);
    }
    private boolean thinkSpace(){
        if(!level.getBlockState(worldPosition.below()).is(Blocks.BOOKSHELF))return false;
        boolean outer=true,inner=true;
        for(var pos:BlockPos.betweenClosed(worldPosition.offset(-2,-1,-2),worldPosition.offset(2,1,2))){
            if(pos.equals(worldPosition)||pos.equals(worldPosition.below()))continue;
            var state=level.getBlockState(pos);if(!state.isAir()&&!state.canBeReplaced()){
                if(Math.abs(pos.getX()-worldPosition.getX())>1||Math.abs(pos.getZ()-worldPosition.getZ())>1)outer=false;else inner=false;
            }
        }
        int next=outer?1:0;if(space!=next){space=next;sync();}return inner;
    }
    private void think(){
        var input=items.getStackInSlot(0);var output=items.getStackInSlot(1);var fragment=ExplorationMod.core("knowledge_fragment");
        int max=input.is(Items.ENCHANTED_BOOK)?2:1;
        boolean can=(input.is(Items.BOOK)||input.is(Items.ENCHANTED_BOOK))&&thinkSpace()&&(output.isEmpty()||output.is(fragment)&&output.getCount()+max<=64);
        int before=warmup;warmup=Math.max(0,Math.min(40,warmup+(can?1:-1)));if(warmup!=before)sync();
        if(!can){if(progress!=0){progress=0;sync();}return;}
        if(++progress>=200){
            double roll=level.random.nextDouble();int amount=input.is(Items.ENCHANTED_BOOK)?(roll>.66?1:roll>.33?2:0):(roll<=.1?1:0);
            input.shrink(1);if(amount>0)items.setStackInSlot(1,new ItemStack(fragment,output.getCount()+amount));progress=0;sync();
        } else setChanged();
    }
    private BlockPos waterTarget;
    private UUID burningPlayer;
    private int waterTravel;
    private IFluidHandler waterHandler(BlockPos pos){
        if(pos==null||!level.hasChunkAt(pos))return null;var target=level.getBlockEntity(pos);if(target==null||target==this)return null;
        var id=net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(target.getBlockState().getBlock()).toString();
        if(!Set.of("thaumcraft:crucible","thaumcraft:arcane_spa").contains(id)&&!id.startsWith("botania:apothecary"))return null;
        return target.getCapability(ForgeCapabilities.FLUID_HANDLER,Direction.UP).orElse(null);
    }
    private void water(){
        if(!level.getBlockState(worldPosition.above()).canBeReplaced())return;
        var area=new AABB(worldPosition).inflate(3,2,3);
        if(level.getGameTime()%5==0&&burningPlayer==null)for(var p:level.getEntitiesOfClass(Player.class,area))if(p.isOnFire()){
            burningPlayer=p.getUUID();waterTravel=(int)(15*Math.sqrt(p.distanceToSqr(Vec3.atCenterOf(worldPosition))));waterTarget=null;break;
        }
        if(burningPlayer!=null){
            var p=level.getPlayerByUUID(burningPlayer);
            if(p==null||!p.isOnFire()||!area.intersects(p.getBoundingBox())){burningPlayer=null;return;}
            waterStream(p.position().add(0,1,0));
            if(waterTravel--<=0){p.clearFire();for(var pos:BlockPos.betweenClosed(p.blockPosition().offset(-1,0,-1),p.blockPosition().offset(1,0,1)))if(level.getBlockState(pos).is(Blocks.FIRE))level.removeBlock(pos,false);burningPlayer=null;}
            return;
        }
        if(waterTarget==null&&level.getGameTime()%5==0){
            for(var pos:BlockPos.betweenClosed(worldPosition.offset(-3,-2,-3),worldPosition.offset(3,2,3))){
                var handler=waterHandler(pos);
                if(handler!=null&&handler.fill(new FluidStack(Fluids.WATER,10),IFluidHandler.FluidAction.SIMULATE)>0){waterTarget=pos.immutable();waterTravel=(int)(20*Math.sqrt(pos.distSqr(worldPosition)));break;}
            }
        }
        if(waterTarget!=null){
            var handler=waterHandler(waterTarget);
            if(handler==null||handler.fill(new FluidStack(Fluids.WATER,10),IFluidHandler.FluidAction.SIMULATE)==0){waterTarget=null;return;}
            waterStream(Vec3.atCenterOf(waterTarget).add(0,.6,0));
            if(waterTravel--<=0)handler.fill(new FluidStack(Fluids.WATER,10),IFluidHandler.FluidAction.EXECUTE);
        }
    }
    private void waterStream(Vec3 end){
        if(level.getGameTime()%5!=0)return;var start=Vec3.atCenterOf(worldPosition).add(0,.6,0);
        for(int i=0;i<10;i++){double t=i/10.;var p=start.lerp(end,t).add(0,Math.sin(t*Math.PI)*.6,0);((ServerLevel)level).sendParticles(ParticleTypes.SPLASH,p.x,p.y,p.z,1,0,0,0,0);}
    }
    public static boolean canReplicate(ItemStack stack){
        if(stack.isEmpty()||stack.hasTag()||!(stack.getItem() instanceof BlockItem bi)||bi.getBlock().defaultBlockState().hasBlockEntity())return false;
        return ExplorationConfig.replicable(stack)&&tc4revival.aspect.item.ItemStackAspects.resolve(stack).map(a->a.total()>0).orElse(false);
    }
    public void startReplication(){
        if(level==null||level.isClientSide||!kind("replicator")||crafting||!items.getStackInSlot(1).isEmpty()||!canReplicate(template)||!level.getBlockState(worldPosition.above()).canBeReplaced())return;
        remaining=tc4revival.aspect.item.ItemStackAspects.resolve(template).orElseThrow().copy();progress=100;crafting=true;sync();
    }
    private void replicate(){
        if(!crafting||!level.getBlockState(worldPosition.above()).canBeReplaced())return;
        if(remaining.total()>0){
            if(level.getGameTime()%50!=0)return;
            for(var aspect:remaining.aspects())if(remaining.amount(aspect)>0){
                for(var pos:BlockPos.betweenClosed(worldPosition.offset(-12,-10,-12),worldPosition.offset(12,5,12))){
                    if(pos.getX()==worldPosition.getX()&&pos.getZ()==worldPosition.getZ()||!level.hasChunkAt(pos))continue;
                    var source=thaumcraft.api.aspects.EssentiaStorageApi.atInventory((ServerLevel)level,pos);
                    if(source.extract(aspect,1,false)==1){remaining.remove(aspect,1);sync();return;}
                }
            }
        } else if(progress>0){progress--;setChanged();}
        else if(items.getStackInSlot(1).isEmpty()){items.setStackInSlot(1,template.copyWithCount(1));crafting=false;sync();}
    }
    private void souls(){
        if(!level.getBlockState(worldPosition.above()).canBeReplaced())return;
        var victim=level.getEntitiesOfClass(LivingEntity.class,new AABB(worldPosition).inflate(5),entity->!(entity instanceof Player)&&entity.isAlive()
            &&!net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).getPath().contains("golem")
            &&!net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).getPath().contains("thaumic_slime"))
            .stream().filter(e->e.distanceToSqr(Vec3.atCenterOf(worldPosition))<25).min(Comparator.comparingDouble(e->e.distanceToSqr(Vec3.atCenterOf(worldPosition)))).orElse(null);
        if(victim==null)return;
        var values=tc4revival.research.ScanTargetResolver.entity(victim).map(tc4revival.research.ScanTarget::aspects).orElseGet(AspectAmounts::new);
        boolean hit=victim.hurt(ExplorationDamage.source(level,ExplorationDamage.SOULS),victim.getHealth()<=1?100:1);
        if(hit&&victim.isDeadOrDying()){
            for(var entry:values.view().entrySet())for(int i=0;i<entry.getValue();i++){
                Aspect selected=entry.getKey();
                if(level.random.nextInt(3)!=0){if(!level.random.nextBoolean()){flux+=.4F;continue;}if(!selected.isPrimal())selected=aspect(selected.components().get(level.random.nextBoolean()?0:1));}
                buffer.insert(selected,1,false);
            }
            sync();
        }
        if(flux>1){var goo=net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(new net.minecraft.resources.ResourceLocation("thaumcraft:flux_goo"));if(goo!=null&&level.isEmptyBlock(worldPosition.above())){level.setBlockAndUpdate(worldPosition.above(),goo.defaultBlockState());flux--;sync();}}
        ((ServerLevel)level).sendParticles(ParticleTypes.SOUL,victim.getX(),victim.getY()+.5,victim.getZ(),2,.2,.2,.2,.02);
    }
    private void pull(){
        Direction face=kind("soul_brazier")?Direction.DOWN:Direction.UP;var pos=worldPosition.relative(face);
        if(!level.hasChunkAt(pos))return;var other=level.getBlockEntity(pos);
        if(other instanceof tc4revival.content.EssentiaTransport pipe&&pipe.connectable(face.getOpposite())&&pipe.canOutput(face.getOpposite())&&pipe.suction(face.getOpposite())<suction(face)&&suction(face)>=pipe.minimumSuction()){
            var a=pipe.essentiaType(face.getOpposite());if(a==null||insert(a,1,true)==0)return;
            if(pipe.takeEssentia(a,1,face.getOpposite())>0)insert(a,1,false);
        }
    }
    public void activate(Player player){
        if(!kind("soul_brazier")||!(level instanceof ServerLevel server))return;
        if(owner==null)owner=player.getUUID();
        if(!owner.equals(player.getUUID())){player.displayClientMessage(Component.translatable("soulbrazier.invalidplayer"),true);return;}
        if(vis<=0||buffer.contents().amount(aspect("mortuus"))<=0){player.displayClientMessage(Component.translatable("soulbrazier.nopower"),true);return;}
        var data=tc4revival.research.KnowledgeSavedData.get(server.getServer());
        int warp=data.update(owner,level.random::nextInt,k->{int n=k.warp(tc4revival.research.PlayerKnowledge.WarpKind.PERMANENT);k.setWarp(tc4revival.research.PlayerKnowledge.WarpKind.PERMANENT,0);return n;});
        if(warp<=0){player.displayClientMessage(Component.translatable("soulbrazier.noWarp"),true);return;}
        storedWarp+=warp;active=true;force(true);sync();
    }
    private void brazier(){
        if(level.getGameTime()%5==0){pull();if(vis<16){vis+=consumeVis(aspect("ignis"),1);setChanged();}}
        if(!active)return;
        if(level.getGameTime()%60==0){vis=Math.max(0,vis-3);buffer.extract(aspect("mortuus"),1,false);sync();}
        if(vis<=0||buffer.contents().amount(aspect("mortuus"))<=0){deactivate();return;}
        if(level.getGameTime()%50==0){
            var p=worldPosition.offset(level.random.nextInt(16)-level.random.nextInt(16),0,level.random.nextInt(16)-level.random.nextInt(16));
            tc4revival.content.WorldBiomeChanges.changeColumns((ServerLevel)level,List.of(p),tc4revival.content.TCBiomes.TAINTED_LAND);
            if(tc4revival.config.TCConfig.SERVER.hardNodes.get()&&level.random.nextBoolean()){
                var q=worldPosition.offset(level.random.nextInt(16)-level.random.nextInt(16),level.random.nextInt(16)-level.random.nextInt(16),level.random.nextInt(16)-level.random.nextInt(16));
                if(level.hasChunkAt(q))tc4revival.content.TaintFibreBlock.spread((ServerLevel)level,q);
            }
        }
    }
    private void force(boolean value){if(level instanceof ServerLevel server)net.minecraftforge.common.world.ForgeChunkManager.forceChunk(server,ExplorationMod.ID,worldPosition,worldPosition.getX()>>4,worldPosition.getZ()>>4,value,true);}
    @Override public void onLoad(){super.onLoad();if(level instanceof ServerLevel&&active&&kind("soul_brazier"))force(true);}
    private IItemHandler automation(boolean input){return new IItemHandler(){
        private int index(int slot){if(slot!=0)throw new IndexOutOfBoundsException(slot);return input?0:1;}
        public int getSlots(){return 1;}
        public ItemStack getStackInSlot(int slot){return items.getStackInSlot(index(slot));}
        public ItemStack insertItem(int slot,ItemStack stack,boolean simulate){return input?items.insertItem(index(slot),stack,simulate):stack;}
        public ItemStack extractItem(int slot,int amount,boolean simulate){return input?ItemStack.EMPTY:items.extractItem(index(slot),amount,simulate);}
        public int getSlotLimit(int slot){return 64;}
        public boolean isItemValid(int slot,ItemStack stack){return input&&items.isItemValid(index(slot),stack);}
    };}
    public void deactivate(){
        if(!(level instanceof ServerLevel server)||!kind("soul_brazier"))return;
        if(storedWarp>0&&owner!=null){int refund=storedWarp;tc4revival.research.KnowledgeSavedData.get(server.getServer()).update(owner,level.random::nextInt,k->{k.changeWarp(tc4revival.research.PlayerKnowledge.WarpKind.PERMANENT,refund);return null;});}
        storedWarp=0;active=false;force(false);sync();
    }
    @Override public AspectAmounts contents(){return buffer.contents();}
    @Override public AspectAmounts revealedAspects(){return contents();}
    @Override public int totalCapacity(){return kind("trash_jar")?64:kind("soul_brazier")?16:kind("crucible_souls")?Integer.MAX_VALUE:0;}
    @Override public int capacity(Aspect a){return accepts(a)?totalCapacity():0;}
    @Override public int insert(Aspect a,int n,boolean simulate){if(n<0)throw new IllegalArgumentException();return accepts(a)?buffer.insert(a,Math.min(n,Math.max(0,totalCapacity()-amount())),simulate):0;}
    @Override public int extract(Aspect a,int n,boolean simulate){return kind("crucible_souls")?buffer.extract(a,n,simulate):0;}
    @Override public Aspect aspect(){return buffer.contents().aspects().stream().findFirst().orElse(null);}
    @Override public int amount(){return (int)buffer.contents().total();}
    @Override public int capacity(){return totalCapacity();}
    @Override public boolean accepts(Aspect a){return a!=null&&(kind("soul_brazier")&&a.equals(aspect("mortuus"))||kind("trash_jar")&&(amount()==0||a.equals(aspect())));}
    @Override public boolean acceptsPhials(){return kind("trash_jar");}
    @Override public boolean contains(Aspect a,int n){return n>=0&&contents().amount(a)>=n;}
    @Override public int addToContainer(Aspect a,int n){return n-insert(a,n,false);}
    @Override public boolean takeFromContainer(Aspect a,int n){return n>=0&&extract(a,n,true)==n&&extract(a,n,false)==n;}
    @Override public boolean connectable(Direction face){return kind("crucible_souls")?face!=Direction.UP:kind("trash_jar")?face==Direction.UP:kind("soul_brazier")&&face==Direction.DOWN;}
    @Override public boolean canInput(Direction face){return connectable(face)&&!kind("crucible_souls");}
    @Override public boolean canOutput(Direction face){return connectable(face)&&kind("crucible_souls");}
    @Override public Aspect suctionType(Direction face){return kind("soul_brazier")?aspect("mortuus"):aspect();}
    @Override public int suction(Direction face){return canInput(face)&&amount()<totalCapacity()?(kind("trash_jar")?8:128):0;}
    @Override public int minimumSuction(){return kind("trash_jar")?8:0;}
    @Override public Aspect essentiaType(Direction face){return aspect();}
    @Override public int essentiaAmount(Direction face){return aspect()==null?0:contents().amount(aspect());}
    @Override public int takeEssentia(Aspect a,int n,Direction face){return canOutput(face)?extract(a,n,false):0;}
    @Override public int addEssentia(Aspect a,int n,Direction face){return canInput(face)?insert(a,n,false):0;}
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);tag.put("items",items.serializeNBT());tag.put("buffer",buffer.save());tag.put("template",template.save(new CompoundTag()));var rest=new CompoundTag();AspectNbt.write(rest,remaining);tag.put("remaining",rest);tag.putInt("progress",progress);tag.putInt("warmup",warmup);tag.putInt("space",space);tag.putInt("vis",vis);tag.putFloat("flux",flux);tag.putInt("storedWarp",storedWarp);tag.putBoolean("active",active);tag.putBoolean("crafting",crafting);tag.putBoolean("powered",powered);if(owner!=null)tag.putUUID("owner",owner);tag.putInt("lavaRemainder",lavaRemainder);}
    @Override public void load(CompoundTag tag){super.load(tag);items.deserializeNBT(tag.getCompound("items"));buffer.load(tag.getCompound("buffer"));template=ItemStack.of(tag.getCompound("template"));remaining=AspectNbt.read(tag.getCompound("remaining"),TCAspects.catalog()).amounts();progress=tag.getInt("progress");warmup=tag.getInt("warmup");space=tag.getInt("space");vis=tag.getInt("vis");flux=tag.getFloat("flux");storedWarp=tag.getInt("storedWarp");active=tag.getBoolean("active");crafting=tag.getBoolean("crafting");powered=tag.getBoolean("powered");owner=tag.hasUUID("owner")?tag.getUUID("owner"):null;lavaRemainder=tag.getInt("lavaRemainder");}
    @Override public CompoundTag getUpdateTag(){return saveWithoutMetadata();}
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
    @Override public Component getDisplayName(){return Component.translatable("block.thaumicexploration."+kind());}
    @Override public AbstractContainerMenu createMenu(int id,Inventory inventory,Player player){return new ThinkTankMenu(id,inventory,this);}
    @Override public <T> LazyOptional<T> getCapability(Capability<T> capability,Direction side){
        if(capability==ForgeCapabilities.FLUID_HANDLER&&kind().endsWith("urn")&&(side==null||side==Direction.UP))return fluidCapability.cast();
        if(capability==ForgeCapabilities.ITEM_HANDLER&&(kind("think_tank")||kind("replicator")))return (kind("think_tank")&&side==Direction.UP?inputCapability:itemCapability).cast();
        return super.getCapability(capability,side);
    }
    @Override public void invalidateCaps(){super.invalidateCaps();itemCapability.invalidate();inputCapability.invalidate();fluidCapability.invalidate();}
    @Override public void reviveCaps(){super.reviveCaps();itemCapability=LazyOptional.of(()->automation(false));inputCapability=LazyOptional.of(()->automation(true));fluidCapability=LazyOptional.of(UrnFluid::new);}
    @Override public AABB getRenderBoundingBox(){return new AABB(worldPosition).inflate(kind("think_tank")?3:1);}
    private final class UrnFluid implements IFluidHandler {
        private net.minecraft.world.level.material.Fluid fluid(){return kind("everfull_urn")?Fluids.WATER:Fluids.LAVA;}
        @Override public int getTanks(){return 1;}
        @Override public FluidStack getFluidInTank(int tank){return new FluidStack(fluid(),kind("everfull_urn")?9999:vis*250+lavaRemainder);}
        @Override public int getTankCapacity(int tank){return kind("everfull_urn")?9999:4000;}
        @Override public boolean isFluidValid(int tank,FluidStack stack){return false;}
        @Override public int fill(FluidStack stack,FluidAction action){return 0;}
        @Override public FluidStack drain(FluidStack stack,FluidAction action){return stack.getFluid()==fluid()?drain(stack.getAmount(),action):FluidStack.EMPTY;}
        @Override public FluidStack drain(int amount,FluidAction action){
            if(amount<=0)return FluidStack.EMPTY;int drained=Math.min(amount,kind("everfull_urn")?999999:vis*250+lavaRemainder);
            if(action.execute()&&kind("everburn_urn")&&drained>0){int total=vis*250+lavaRemainder-drained;vis=total/250;lavaRemainder=total%250;sync();}
            return drained==0?FluidStack.EMPTY:new FluidStack(fluid(),drained);
        }
    }
    private int lavaRemainder;
}
