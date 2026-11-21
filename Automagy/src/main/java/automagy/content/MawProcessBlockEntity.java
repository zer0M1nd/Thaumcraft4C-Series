package automagy.content;

import java.util.UUID;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class MawProcessBlockEntity extends BlockEntity {
    public UUID owner;
    private int actions,ticks,consumed;
    public MawProcessBlockEntity(BlockPos pos,BlockState state){super(Content.MAW_PROCESS_TILE.get(),pos,state);}
    public static void tick(Level level,BlockPos pos,BlockState state,MawProcessBlockEntity process){
        if(!(level instanceof ServerLevel server))return;
        if(++process.ticks<10){process.setChanged();return;}
        if(process.actions<5&&process.consumed<4){
            BlockPos target=pos.relative(Direction.from3DDataValue(process.actions+1));
            if(!level.hasChunkAt(target))return;
            var block=level.getBlockState(target);boolean obsidian=block.is(Blocks.OBSIDIAN);
            if(!obsidian&&level.getBlockEntity(target) instanceof tc4revival.content.PortableHoleBlockEntity hole){
                var original=NbtUtils.readBlockState(net.minecraft.core.registries.BuiltInRegistries.BLOCK.asLookup(),hole.getUpdateTag().getCompound("Original"));obsidian=original.is(Blocks.OBSIDIAN);
            }
            var player=process.owner==null?null:server.getServer().getPlayerList().getPlayer(process.owner);
            if(process.owner!=null&&player==null)return;
            if(obsidian&&player!=null&&player.level()==level&&player.mayBuild()&&level.mayInteract(player,target)){
                var event=new net.minecraftforge.event.level.BlockEvent.BreakEvent(level,target,block,player);
                if(!net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event)&&level.setBlock(target,Blocks.AIR.defaultBlockState(),3)){process.consumed++;process.ticks=0;}
            }
        }
        process.actions++;process.setChanged();
        thaumcraft.api.fx.LegacyEffects.emit(level,"dark_wisp_path",pos.getCenter().add(.4,.4,.4),pos.getCenter(),0x222222,.5f,40,2,.2f,-.025f);
        if(process.actions>9){
            level.setBlock(pos,Blocks.AIR.defaultBlockState(),3);
            for(int i=0;i<process.consumed;i++)Block.popResource(level,pos,new ItemStack(Content.HUNGRY_MAW.get()));
            level.playSound(null,pos,net.minecraft.sounds.SoundEvents.PLAYER_BURP,net.minecraft.sounds.SoundSource.BLOCKS,.5f,.8f);
        }
    }
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);tag.putInt("Actions",actions);tag.putInt("Ticks",ticks);tag.putInt("Consumed",consumed);if(owner!=null)tag.putUUID("Owner",owner);}
    @Override public void load(CompoundTag tag){super.load(tag);actions=Math.max(0,Math.min(10,tag.getInt("Actions")));ticks=Math.max(0,Math.min(10,tag.getInt("Ticks")));consumed=Math.max(0,Math.min(4,tag.getInt("Consumed")));owner=tag.hasUUID("Owner")?tag.getUUID("Owner"):null;}
}
