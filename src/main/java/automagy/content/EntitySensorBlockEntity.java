package automagy.content;

import automagy.api.AmplifiedSignal;
import automagy.logic.SensorRange;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public final class EntitySensorBlockEntity extends InventoryBlockEntity implements MenuProvider,AmplifiedSignal {
    public boolean adults=true,children=true,specificPlayer;
    private int count;
    public EntitySensorBlockEntity(BlockPos pos,BlockState state){super(Content.ENTITY_SENSOR_TILE.get(),pos,state,1);}
    @Override protected boolean accepts(int slot,ItemStack stack){return EntityEyeItem.linked(stack);}
    public net.minecraftforge.items.IItemHandlerModifiable eyeInventory(){return inventory;}
    public int count(){return count;}
    @Override public int amplifiedSignal(Direction out){return count;}
    public static void tick(Level level,BlockPos pos,BlockState state,EntitySensorBlockEntity tile){if(level.getGameTime()%5==Math.floorMod(pos.asLong(),5))tile.refresh();}
    public AABB detectionBounds(int mask){
        var x=SensorRange.axis((mask&1<<Direction.WEST.ordinal())!=0,(mask&1<<Direction.EAST.ordinal())!=0);
        var y=SensorRange.axis((mask&1<<Direction.DOWN.ordinal())!=0,(mask&1<<Direction.UP.ordinal())!=0);
        var z=SensorRange.axis((mask&1<<Direction.NORTH.ordinal())!=0,(mask&1<<Direction.SOUTH.ordinal())!=0);
        int minHeight=level==null?-64:level.getMinBuildHeight(),maxHeight=level==null?320:level.getMaxBuildHeight();
        return new AABB(worldPosition.getX()+x.minimum(),Math.max(minHeight,worldPosition.getY()+y.minimum()),worldPosition.getZ()+z.minimum(),
                worldPosition.getX()+x.maximum(),Math.min(maxHeight,worldPosition.getY()+y.maximum()),worldPosition.getZ()+z.maximum());
    }
    public void refresh(){
        if(level==null||level.isClientSide)return;
        int mask=0;for(Direction dir:Direction.values())if(EntitySensorBlock.blocked(level,worldPosition.relative(dir),dir))mask|=1<<dir.ordinal();
        int next=EntityEyeItem.linked(stored(0))?Math.min(32767,level.getEntitiesOfClass(LivingEntity.class,detectionBounds(mask),
                entity->entity.isAlive()&&!entity.isSpectator()&&EntityEyeItem.matches(stored(0),entity,adults,children,specificPlayer)).size()):0;
        boolean changed=next!=count;count=next;
        var state=getBlockState();var updated=state.setValue(EntitySensorBlock.MASK,mask).setValue(EntitySensorBlock.LIT,count>0);
        if(state!=updated)level.setBlock(worldPosition,updated,2);
        if(changed){sync();level.updateNeighborsAt(worldPosition,state.getBlock());level.updateNeighbourForOutputSignal(worldPosition,state.getBlock());
            for(Direction dir:Direction.values())level.updateNeighborsAt(worldPosition.relative(dir),state.getBlock());}
    }
    @Override protected void inventoryChanged(){super.inventoryChanged();refresh();}
    @Override public Component getDisplayName(){return Component.translatable("block.automagy.entity_sensor");}
    @Override public AbstractContainerMenu createMenu(int id,Inventory inventory,Player player){return new EntitySensorMenu(id,inventory,this);}
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);tag.putBoolean("Adults",adults);tag.putBoolean("Children",children);tag.putBoolean("SpecificPlayer",specificPlayer);tag.putInt("Count",count);}
    @Override public void load(CompoundTag tag){super.load(tag);adults=!tag.contains("Adults")||tag.getBoolean("Adults");children=!tag.contains("Children")||tag.getBoolean("Children");specificPlayer=tag.getBoolean("SpecificPlayer");count=Math.max(0,Math.min(32767,tag.getInt("Count")));}
}
