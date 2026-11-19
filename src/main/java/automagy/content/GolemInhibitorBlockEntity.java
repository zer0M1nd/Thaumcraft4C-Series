package automagy.content;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import tc4revival.content.*;

public final class GolemInhibitorBlockEntity extends BlockEntity implements GolemLinkDevice {
    private UUID identity=UUID.randomUUID();
    public boolean omitLinked=true;
    private boolean active;
    public boolean active(){return active;}
    public GolemInhibitorBlockEntity(BlockPos pos,BlockState state){super(Content.INHIBITOR_TILE.get(),pos,state);}
    @Override public UUID linkIdentity(){return identity;}
    @Override public void resetLinks(){identity=UUID.randomUUID();sync();}
    public void toggle(){omitLinked=!omitLinked;sync();}
    private void sync(){setChanged();if(level!=null&&!level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    public static void tick(Level level,BlockPos pos,BlockState state,GolemInhibitorBlockEntity device){
        if(level.isClientSide)return;
        int fetters=0;
        for(int i=1;i<=4;i++){BlockPos below=pos.below(i);if(!level.hasChunkAt(below))break;var block=level.getBlockState(below);if(!block.is(TCArcane.GOLEM_FETTER.get())||!block.getValue(GolemFetterBlock.POWERED))break;fetters++;}
        if(device.active!=(fetters>0)){device.active=fetters>0;device.sync();}
        if(fetters==0)return;int range=fetters*5;
        var golems=level.getEntitiesOfClass(GolemEntity.class,new AABB(pos.getX()-range,pos.getY()-1.5,pos.getZ()-range,pos.getX()+range+1,pos.getY()+range,pos.getZ()+range+1));
        golems.sort(Comparator.comparing(g->g.getUUID().toString()));int links=0;
        for(var golem:golems){
            if(golem.state().inactive())continue;
            boolean linked=links<6&&GolemLinkerItem.linked(golem,device,device);if(linked)links++;
            if(linked!=device.omitLinked){golem.state().inactive(true);golem.getNavigation().stop();}
        }
    }
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);tag.putBoolean("Active",active);tag.putUUID("Identity",identity);tag.putBoolean("OmitLinked",omitLinked);}
    @Override public void load(CompoundTag tag){super.load(tag);active=tag.getBoolean("Active");identity=tag.hasUUID("Identity")?tag.getUUID("Identity"):UUID.randomUUID();omitLinked=!tag.contains("OmitLinked")||tag.getBoolean("OmitLinked");}
    @Override public CompoundTag getUpdateTag(){return saveWithoutMetadata();}
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
}
