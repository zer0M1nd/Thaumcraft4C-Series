package ttmodern;

import dan200.computercraft.api.lua.*;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraftforge.common.util.LazyOptional;

/** Loaded only after the optional ComputerCraft mod has been identified. */
public final class ComputerIntegration {
    private static final net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> DISABLED=net.minecraft.tags.BlockTags.create(net.minecraft.resources.ResourceLocation.parse(Tinkerer.ID+":peripheral_disabled"));
    private static final net.minecraftforge.common.capabilities.Capability<IPeripheral> PERIPHERAL=net.minecraftforge.common.capabilities.CapabilityManager.get(new net.minecraftforge.common.capabilities.CapabilityToken<IPeripheral>(){});
    public static void setup(){MachinePeripherals.setup();dan200.computercraft.api.ForgeComputerCraftAPI.registerPeripheralProvider((world,pos,side)->lookup(world.getBlockEntity(pos),side));}
    public static LazyOptional<IPeripheral> lookup(net.minecraft.world.level.block.entity.BlockEntity entity,net.minecraft.core.Direction side){
        if(entity==null||entity.isRemoved()||entity.getBlockState().is(DISABLED))return LazyOptional.empty();
        if(entity instanceof InterfaceEntity link){
            link.refreshDestination();var target=link.destination();if(target==null||target.getBlockState().is(DISABLED))return LazyOptional.empty();
            var source=target.getCapability(PERIPHERAL,side);if(!source.isPresent())source=lookup(target,side);
            if(!source.isPresent())return LazyOptional.empty();
            var peripheral=source.orElseThrow(IllegalStateException::new);
            LazyOptional<IPeripheral> result=LazyOptional.of(()->peripheral);
            link.onTargetInvalidated(result::invalidate);
            var weak=new java.lang.ref.WeakReference<>(result);
            source.addListener(ignored->{var current=weak.get();if(current!=null)current.invalidate();});
            return result;
        }
        if(entity instanceof GolemConnectorEntity connector){LazyOptional<IPeripheral> result=LazyOptional.of(()->new Golem(connector));connector.onInvalidated(result::invalidate);return result;}
        if(!(entity instanceof AspectAnalyzerEntity analyzer))return MachinePeripherals.lookup(entity);
        LazyOptional<IPeripheral> result=LazyOptional.of(()->new Analyzer(analyzer));analyzer.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER,side).addListener(ignored->result.invalidate());return result;
    }
    public static final class Golem implements IPeripheral {
        private final GolemConnectorEntity entity;
        public Golem(GolemConnectorEntity entity){this.entity=entity;}
        @Override public String getType(){return "tt_golemconnector";}
        @Override public Object getTarget(){return entity;}
        @Override public boolean equals(IPeripheral other){return other instanceof Golem otherGolem&&otherGolem.entity==entity;}
        private tc4revival.content.GolemEntity golem() throws LuaException {var golem=entity.golem();if(golem==null)throw new LuaException("Golem is unavailable or outside the 30-block range");return golem;}
        private <T> T checked(java.util.function.Supplier<T> action) throws LuaException {golem();try{return action.get();}catch(IllegalArgumentException|IllegalStateException error){throw new LuaException(error.getMessage());}}
        @LuaFunction(mainThread=true) public java.util.Map<Integer,String> getDecorations() throws LuaException {var result=new java.util.LinkedHashMap<Integer,String>();var decorations=golem().configuration().decorations();for(int i=0;i<decorations.length();i++)result.put(i+1,String.valueOf(decorations.charAt(i)));return result;}
        @LuaFunction(mainThread=true) public Object[] getPosition() throws LuaException {var golem=golem();return new Object[]{golem.getX(),golem.getY(),golem.getZ()};}
        @LuaFunction(value="getType",mainThread=true) public String getGolemType() throws LuaException {var config=golem().configuration();return config.customMaterial()!=null?config.customMaterial().toString():config.material().name().toLowerCase(java.util.Locale.ROOT);}
        @LuaFunction(mainThread=true) public double getHealth() throws LuaException {return golem().getHealth();}
        @LuaFunction(mainThread=true) public String getCore() throws LuaException {var config=golem().configuration();return config.customTask()!=null?config.customTask().toString():config.core()==null?"none":config.core().name().toLowerCase(java.util.Locale.ROOT);}
        @LuaFunction(mainThread=true) public Object[] getHome() throws LuaException {var golem=golem();return new Object[]{golem.home().getX(),golem.home().getY(),golem.home().getZ(),golem.homeFacing().get3DDataValue()};}
        @LuaFunction(mainThread=true) public Object[] setHome(IArguments arguments) throws LuaException {checked(()->{entity.home(new net.minecraft.core.BlockPos(GolemConnectorEntity.integer(argument(arguments,0),"x",-30000000,30000000),GolemConnectorEntity.integer(argument(arguments,1),"y",-2048,2047),GolemConnectorEntity.integer(argument(arguments,2),"z",-30000000,30000000)),net.minecraft.core.Direction.from3DDataValue(GolemConnectorEntity.integer(argument(arguments,3),"facing",0,5)));return true;});return getHome();}
        private static Object argument(IArguments arguments,int index){try{return arguments.get(index);}catch(LuaException error){throw new IllegalArgumentException(error.getMessage());}}
        @LuaFunction(mainThread=true) public java.util.Map<Integer,java.util.Map<String,Object>> getMarkers() throws LuaException {return checked(()->{var result=new java.util.LinkedHashMap<Integer,java.util.Map<String,Object>>();var markers=entity.markers();for(int i=0;i<markers.size();i++)result.put(i+1,GolemConnectorEntity.markerMap(markers.get(i)));return result;});}
        @LuaFunction(mainThread=true) public java.util.Map<String,Object> newMarker() throws LuaException {return checked(entity::newMarker);}
        @LuaFunction(mainThread=true) public int getMarkerCount() throws LuaException {return checked(()->entity.markers().size());}
        private int index(int index,int size) throws LuaException {if(index<1||index>size)throw new LuaException("Marker index must be in 1.."+size);return index-1;}
        @LuaFunction(mainThread=true) public java.util.Map<String,Object> getMarker(int index) throws LuaException {var markers=checked(entity::markers);return GolemConnectorEntity.markerMap(markers.get(index(index,markers.size())));}
        @LuaFunction(mainThread=true) public void deleteMarker(int index) throws LuaException {var markers=new java.util.ArrayList<>(checked(entity::markers));markers.remove(index(index,markers.size()));checked(()->{entity.markers(markers);return true;});}
        @LuaFunction(mainThread=true) public java.util.Map<String,Object> addMarker(java.util.Map<?,?> value) throws LuaException {var marker=checked(()->GolemConnectorEntity.marker(value));checked(()->{var markers=new java.util.ArrayList<>(entity.markers());markers.add(marker);entity.markers(markers);return true;});return GolemConnectorEntity.markerMap(marker);}
        @LuaFunction(mainThread=true) public java.util.Map<String,Object> saveMarker(int index,java.util.Map<?,?> value) throws LuaException {var markers=new java.util.ArrayList<>(checked(entity::markers));int slot=index(index,markers.size());var marker=checked(()->GolemConnectorEntity.marker(value));markers.set(slot,marker);checked(()->{entity.markers(markers);return true;});return GolemConnectorEntity.markerMap(marker);}
        @LuaFunction(mainThread=true) public java.util.Map<Integer,java.util.Map<String,Object>> setMarkers(java.util.Map<?,?> values) throws LuaException {checked(()->{if(values.size()>1024)throw new IllegalArgumentException("Too many markers");var ordered=new java.util.TreeMap<Integer,tc4revival.content.GolemMarker>();for(var entry:values.entrySet()){int index=GolemConnectorEntity.integer(entry.getKey(),"index",1,1024);if(!(entry.getValue() instanceof java.util.Map<?,?> table))throw new IllegalArgumentException("Expected marker table");ordered.put(index,GolemConnectorEntity.marker(table));}int expected=1;for(int index:ordered.keySet())if(index!=expected++)throw new IllegalArgumentException("Marker list must have consecutive indices starting at 1");entity.markers(new java.util.ArrayList<>(ordered.values()));return true;});return getMarkers();}
    }
    public static final class Analyzer implements IPeripheral {
        private final AspectAnalyzerEntity entity;
        public Analyzer(AspectAnalyzerEntity entity){this.entity=entity;}
        private void check() throws LuaException {if(entity.isRemoved()||entity.getLevel()==null||entity.getLevel().getBlockEntity(entity.getBlockPos())!=entity)throw new LuaException("Analyzer no longer exists");}
        @Override public String getType(){return "tt_aspectanalyzer";}
        @Override public Object getTarget(){return entity;}
        @Override public boolean equals(IPeripheral other){return other instanceof Analyzer analyzer&&analyzer.entity==entity;}
        @LuaFunction(mainThread=true) public boolean hasItem() throws LuaException {check();return !entity.items.getStackInSlot(0).isEmpty();}
        @LuaFunction(mainThread=true) public boolean itemHasAspects() throws LuaException {check();return !entity.aspects().isEmpty();}
        @LuaFunction(mainThread=true) public java.util.Map<Integer,String> getAspects() throws LuaException {check();var result=new java.util.LinkedHashMap<Integer,String>();int index=1;for(var tag:entity.aspects().keySet())result.put(index++,tag);return result;}
        @LuaFunction(mainThread=true) public java.util.Map<String,Integer> getAspectCount() throws LuaException {check();return entity.aspects();}
    }
}
