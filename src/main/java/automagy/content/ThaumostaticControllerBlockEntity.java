package automagy.content;
import automagy.logic.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import tc4revival.aspect.*;
import tc4revival.content.*;
import java.util.*;
import java.util.function.IntUnaryOperator;

public final class ThaumostaticControllerBlockEntity extends InventoryBlockEntity implements thaumcraft.api.aspects.IEssentiaTransport,RevealedAspectContainer,automagy.api.AspectReadingProvider {
    private final ThaumostaticCycle cycle=new ThaumostaticCycle();
    private final Map<Direction,String> suction=new EnumMap<>(Direction.class);
    private boolean active;private int spireY,pylons,validation=60;
    public ThaumostaticControllerBlockEntity(BlockPos pos,BlockState state){super(Content.THAUMOSTATIC_CONTROLLER_TILE.get(),pos,state,0);for(var dir:Direction.Plane.HORIZONTAL)suction.put(dir,"potentia");}
    public boolean active(){return active;}
    public void requireValidation(){validation=60;}
    private ThaumostaticStructure.Result scan(boolean assembled){return ThaumostaticStructure.scan(new ThaumostaticStructure.World(){
        private BlockPos at(int y){return new BlockPos(worldPosition.getX(),y,worldPosition.getZ());}
        public int minimumY(){return level.getMinBuildHeight();}public int maximumY(){return level.getMaxBuildHeight();}
        public boolean loaded(int y){return level.hasChunkAt(at(y));}public boolean pylon(int y){return level.getBlockState(at(y)).is(Content.THAUMOSTATIC_PYLON.get());}
        public boolean base(int y){return level.getBlockState(at(y)).is(Blocks.CHISELED_QUARTZ_BLOCK);}
        public boolean thaumium(int y){return level.getBlockState(at(y)).is(net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(net.minecraft.resources.ResourceLocation.parse("thaumcraft:thaumium_block")));}
        public boolean spire(int y){var s=level.getBlockState(at(y));return s.is(Content.THAUMOSTATIC_SPIRE.get())&&!s.getValue(ThaumostaticSpireBlock.TOP);}
        public boolean spireTop(int y){var s=level.getBlockState(at(y));return s.is(Content.THAUMOSTATIC_SPIRE.get())&&s.getValue(ThaumostaticSpireBlock.TOP);}
        public boolean air(int y){return level.isEmptyBlock(at(y));}public boolean sky(int y){return level.canSeeSky(at(y));}
    },worldPosition.getY(),assembled);}
    public boolean activate(net.minecraft.server.level.ServerPlayer player,net.minecraft.world.item.ItemStack wand){
        if(active||level==null||level.isClientSide)return false;var found=scan(false);if(!found.valid())return false;
        var costs=new thaumcraft.api.aspects.AspectList().add(thaumcraft.api.aspects.Aspect.AIR,50).add(thaumcraft.api.aspects.Aspect.FIRE,20).add(thaumcraft.api.aspects.Aspect.ORDER,20).add(thaumcraft.api.aspects.Aspect.WATER,10);
        if(!player.getAbilities().instabuild&&!thaumcraft.api.ThaumcraftApiHelper.consumeVisFromWandCrafting(wand,player,costs,true))return false;
        spireY=found.spire();pylons=found.pylons();active=true;cycle.activated();
        var spire=new BlockPos(worldPosition.getX(),spireY,worldPosition.getZ());
        level.setBlockAndUpdate(spire,Content.THAUMOSTATIC_SPIRE.get().defaultBlockState());
        level.setBlockAndUpdate(spire.above(),Content.THAUMOSTATIC_SPIRE.get().defaultBlockState().setValue(ThaumostaticSpireBlock.TOP,true));
        if(level.getBlockEntity(spire) instanceof ThaumostaticSpireBlockEntity tile)tile.controller(worldPosition);
        for(int y=found.bottom()+1;y<found.spire();y++){var pos=new BlockPos(worldPosition.getX(),y,worldPosition.getZ());if(level.getBlockEntity(pos) instanceof ThaumostaticPylonBlockEntity tile)tile.controller(worldPosition);}
        level.playSound(null,worldPosition,net.minecraft.sounds.SoundEvents.BEACON_ACTIVATE,net.minecraft.sounds.SoundSource.BLOCKS,1,1);
        level.playSound(null,spire,net.minecraft.sounds.SoundEvents.STONE_BREAK,net.minecraft.sounds.SoundSource.BLOCKS,1,1);
        sync();return true;
    }
    public void deactivate(){if(!active||level==null||level.isClientSide)return;active=false;var spire=new BlockPos(worldPosition.getX(),spireY,worldPosition.getZ());if(level.hasChunkAt(spire)&&level.getBlockState(spire).is(Content.THAUMOSTATIC_SPIRE.get()))level.destroyBlock(spire,true);sync();}
    public void switchSuction(Direction face){if(!connectable(face))return;String current=suction.get(face);suction.put(face,ThaumostaticCycle.ASPECTS.get((ThaumostaticCycle.ASPECTS.indexOf(current)+1)%3));sync();}
    private void pull(){for(var dir:Direction.Plane.HORIZONTAL){var pos=worldPosition.relative(dir);if(!level.hasChunkAt(pos)||!(level.getBlockEntity(pos) instanceof EssentiaTransport source))continue;var other=dir.getOpposite();var aspect=suctionType(dir);if(suction(dir)<=source.suction(other)||suction(dir)<source.minimumSuction()||!source.canOutput(other)||!aspect.equals(source.essentiaType(other))||source.essentiaAmount(other)<1)continue;if(source.takeEssentia(aspect,1,other)==1){cycle.insert(aspect.tag(),1);sync();}}}
    public static void tick(Level level,BlockPos pos,BlockState state,ThaumostaticControllerBlockEntity tile){
        if(!tile.active)return;
        if(++tile.validation>=60){tile.validation=0;var result=tile.scan(true);if(result.unloaded()){tile.validation=60;return;}if(!result.valid()){tile.deactivate();return;}tile.pylons=result.pylons();}
        tile.pull();var spire=new BlockPos(pos.getX(),tile.spireY,pos.getZ());if(!level.hasChunkAt(spire)||!(level.getBlockEntity(spire) instanceof ThaumostaticSpireBlockEntity top))return;
        var recipients=new ArrayList<IntUnaryOperator>();double range=tile.cycle.range(tile.pylons);for(var player:level.players()){double dx=player.getX()-pos.getX()-.5,dz=player.getZ()-pos.getZ()-.5;if(dx*dx+dz*dz<=range*range)recipients.add(n->HarnessRefueling.insert(player,n));}
        var before=tile.cycle.snapshot();if(tile.cycle.tick(top.powered(),tile.pylons,recipients))top.sparks();var after=tile.cycle.snapshot();if(!before.amounts().equals(after.amounts()))tile.sync();else if(!before.equals(after))tile.setChanged();
    }
    @Override public AspectReading aspectReading(){
        var amounts=new LinkedHashMap<String,Integer>();var capacities=new LinkedHashMap<String,Integer>();
        for(String tag:ThaumostaticCycle.ASPECTS){amounts.put(tag,cycle.amount(tag));capacities.put(tag,8);}
        return new AspectReading(amounts,capacities,24,8,true,true);
    }
    @Override public AspectAmounts revealedAspects(){var result=new AspectAmounts();for(String tag:ThaumostaticCycle.ASPECTS)if(cycle.amount(tag)>0)result.add(TCAspects.catalog().require(tag),cycle.amount(tag));return result;}
    @Override public boolean connectable(Direction face){return face!=null&&face.getAxis().isHorizontal();}
    @Override public boolean canInput(Direction face){return connectable(face);}@Override public boolean canOutput(Direction face){return connectable(face);}
    @Override public Aspect suctionType(Direction face){return connectable(face)?TCAspects.catalog().require(suction.get(face)):null;}
    @Override public int suction(Direction face){return active&&connectable(face)&&cycle.amount(suction.get(face))<8?128:0;}
    @Override public int minimumSuction(){return 0;}@Override public boolean renderExtendedTube(){return true;}
    @Override public Aspect essentiaType(Direction face){if(active||!connectable(face))return null;return revealedAspects().aspects().stream().findFirst().orElse(null);}
    @Override public int essentiaAmount(Direction face){var aspect=essentiaType(face);return aspect==null?0:cycle.amount(aspect.tag());}
    @Override public int takeEssentia(Aspect aspect,int amount,Direction face){int taken=canOutput(face)&&aspect!=null?cycle.extract(aspect.tag(),amount,active):0;if(taken>0)sync();return taken;}
    @Override public int addEssentia(Aspect aspect,int amount,Direction face){int added=canInput(face)&&aspect!=null?cycle.insert(aspect.tag(),amount):0;if(added>0)sync();return added;}
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);tag.putBoolean("Active",active);tag.putInt("SpireY",spireY);tag.putInt("Pylons",pylons);var state=cycle.snapshot();var amounts=new CompoundTag();state.amounts().forEach(amounts::putInt);tag.put("Essentia",amounts);tag.putInt("RangeTicks",state.rangeTicks());tag.putInt("RateTicks",state.rateTicks());tag.putInt("Distribution",state.distributionTicks());for(var dir:Direction.Plane.HORIZONTAL)tag.putString("Suction"+dir.getName(),suction.get(dir));}
    @Override public void load(CompoundTag tag){super.load(tag);active=tag.getBoolean("Active");spireY=tag.getInt("SpireY");pylons=Math.max(0,Math.min(6,tag.getInt("Pylons")));var amounts=new LinkedHashMap<String,Integer>();var saved=tag.getCompound("Essentia");for(String key:ThaumostaticCycle.ASPECTS)amounts.put(key,saved.getInt(key));cycle.restore(new ThaumostaticCycle.Snapshot(amounts,tag.getInt("RangeTicks"),tag.getInt("RateTicks"),tag.getInt("Distribution")));for(var dir:Direction.Plane.HORIZONTAL){String key=tag.getString("Suction"+dir.getName());suction.put(dir,ThaumostaticCycle.ASPECTS.contains(key)?key:"potentia");}validation=60;}
}
