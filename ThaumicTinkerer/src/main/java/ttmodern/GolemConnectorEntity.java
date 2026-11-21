package ttmodern;

import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import tc4revival.content.*;
import java.util.*;

public final class GolemConnectorEntity extends BlockEntity {
    private UUID connected;
    private String authorizedOwner="";
    private BlockState cover;
    private final List<Runnable> invalidators=new ArrayList<>();
    public GolemConnectorEntity(BlockPos pos,BlockState state){super(Tinkerer.GOLEM_CONNECTOR_ENTITY.get(),pos,state);}
    public void onInvalidated(Runnable listener){invalidators.add(listener);}
    @Override public void invalidateCaps(){super.invalidateCaps();invalidators.forEach(Runnable::run);invalidators.clear();}
    public boolean bind(net.minecraft.world.entity.player.Player player,GolemEntity golem){if(level==null||level.isClientSide||player.level()!=level||golem.level()!=level||!inRange(golem)||!level.mayInteract(player,worldPosition)||(!golem.ownerName().isEmpty()&&!golem.ownerName().equalsIgnoreCase(player.getGameProfile().getName())&&!player.isCreative()))return false;connected=golem.getUUID();authorizedOwner=golem.ownerName();changed();return true;}
    private boolean inRange(GolemEntity golem){return golem.isAlive()&&Math.abs(golem.getX()-worldPosition.getX())<=30&&Math.abs(golem.getY()-worldPosition.getY())<=30&&Math.abs(golem.getZ()-worldPosition.getZ())<=30;}
    public GolemEntity golem(){if(isRemoved()||!(level instanceof net.minecraft.server.level.ServerLevel world)||world.getBlockEntity(worldPosition)!=this||connected==null)return null;var entity=world.getEntity(connected);return entity instanceof GolemEntity golem&&inRange(golem)&&golem.ownerName().equals(authorizedOwner)?golem:null;}
    private GolemEntity required(){var golem=golem();if(golem==null)throw new IllegalStateException("Bound golem is unavailable, out of range, or has changed owner");return golem;}
    public BlockState cover(){return cover;}
    public void cover(BlockState state){cover=state;level.setBlock(worldPosition,getBlockState().setValue(InterfaceBlock.CAMOUFLAGED,state!=null),3);changed();}
    private void changed(){setChanged();if(level!=null&&!level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    public void home(BlockPos pos,Direction face){var golem=required();validatePosition(pos,golem.level());golem.state().home(pos,face);golem.restrictTo(pos,35);golem.getNavigation().stop();}
    public List<GolemMarker> markers(){return required().markers().inDimension(level.dimension().location());}
    public void markers(List<GolemMarker> entries){if(entries.size()>1024)throw new IllegalArgumentException("At most 1024 markers are allowed");var golem=required();for(var marker:entries){if(!marker.dimension().equals(level.dimension().location()))throw new IllegalArgumentException("Markers must belong to the golem's dimension");validatePosition(marker.pos(),level);}golem.markers().replace(List.copyOf(entries));golem.state().changed();}
    private static void validatePosition(BlockPos pos,net.minecraft.world.level.Level level){if(level.isOutsideBuildHeight(pos)||!level.getWorldBorder().isWithinBounds(pos))throw new IllegalArgumentException("Position is outside world bounds");}
    public Map<String,Object> newMarker(){return markerMap(new GolemMarker(worldPosition,level.dimension().location(),Direction.UP,(byte)-1));}
    public static Map<String,Object> markerMap(GolemMarker marker){return Map.of("posX",marker.pos().getX(),"posY",marker.pos().getY(),"posZ",marker.pos().getZ(),"dim",marker.dimension().toString(),"color",(int)marker.color(),"side",marker.face().get3DDataValue());}
    static int integer(Object value,String name,int min,int max){if(!(value instanceof Number number)||!Double.isFinite(number.doubleValue())||number.doubleValue()!=Math.rint(number.doubleValue())||number.doubleValue()<min||number.doubleValue()>max)throw new IllegalArgumentException(name+" must be an integer in "+min+".."+max);return number.intValue();}
    static GolemMarker marker(Map<?,?> table){int x=integer(table.get("posX"),"posX",-30000000,30000000),y=integer(table.get("posY"),"posY",-2048,2047),z=integer(table.get("posZ"),"posZ",-30000000,30000000);var dimension=table.get("dim") instanceof String text?net.minecraft.resources.ResourceLocation.tryParse(text):null;if(dimension==null)throw new IllegalArgumentException("dim must be a dimension resource ID");return new GolemMarker(new BlockPos(x,y,z),dimension,Direction.from3DDataValue(integer(table.get("side"),"side",0,5)),(byte)integer(table.get("color"),"color",-1,15));}
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);if(connected!=null)tag.putUUID("Golem",connected);tag.putString("AuthorizedOwner",authorizedOwner);if(cover!=null)tag.put("Cover",NbtUtils.writeBlockState(cover));}
    @Override public void load(CompoundTag tag){super.load(tag);connected=tag.hasUUID("Golem")?tag.getUUID("Golem"):null;authorizedOwner=tag.getString("AuthorizedOwner");cover=tag.contains("Cover",Tag.TAG_COMPOUND)?NbtUtils.readBlockState(net.minecraft.core.registries.BuiltInRegistries.BLOCK.asLookup(),tag.getCompound("Cover")):null;}
    @Override public CompoundTag getUpdateTag(){return saveWithoutMetadata();}
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
}
