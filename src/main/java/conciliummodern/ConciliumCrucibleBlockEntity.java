package conciliummodern;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import tc4revival.aspect.*;
import tc4revival.content.*;
import thaumcraft.api.fx.LegacyEffects;

public class ConciliumCrucibleBlockEntity extends BlockEntity implements RevealedAspectContainer {
    private final boolean flesh;
    private int amount,cooldown;
    public ConciliumCrucibleBlockEntity(BlockPos pos,BlockState state){
        super(((ConciliumCrucibleBlock)state.getBlock()).flesh?ConciliumBlocks.FLESH_CRUCIBLE_TILE.get():ConciliumBlocks.QUICKSILVER_CRUCIBLE_TILE.get(),pos,state);
        flesh=((ConciliumCrucibleBlock)state.getBlock()).flesh;
    }
    public boolean flesh(){return flesh;}
    public int amount(){return amount;}
    public void initialize(int amount){this.amount=Mth.clamp(amount,0,64);sync();}
    public float fluidHeight(){return .3f+amount/100f;}
    @Override public AspectAmounts revealedAspects(){return amount==0?new AspectAmounts():new AspectAmounts().add(TCAspects.catalog().require(flesh?"corpus":"permutatio"),amount);}
    public void tick(){
        if(level==null)return;
        if(level.isClientSide){
            if(amount>0 && !level.hasNeighborSignal(worldPosition))for(int i=0;i<5;i++)
                net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,()->()->ConciliumParticleEffects.crucibleBubble(level,new net.minecraft.world.phys.Vec3(worldPosition.getX()+(5+level.random.nextInt(22))/32d,worldPosition.getY()+fluidHeight(),worldPosition.getZ()+(5+level.random.nextInt(22))/32d),flesh?0xed478c:0x999999));
            return;
        }
        if(cooldown>0){cooldown--;setChanged();}
        if(!flesh && cooldown==0 && !level.hasNeighborSignal(worldPosition)){cooldown=20;combineNearby();setChanged();}
    }
    private void combineNearby(){
        if(amount==0)return;
        for(int x=-8;x<=8;x++)for(int y=-8;y<=8;y++)for(int z=-8;z<=8;z++){
            var pos=worldPosition.offset(x,y,z);if(!level.hasChunkAt(pos) || !(level.getBlockEntity(pos) instanceof CrucibleBlockEntity crucible))continue;
            var mixed=combineLastPair(crucible.contents().aspects());if(mixed==null)continue;
            // Preserve water, temperature and decay timers; only replace the aspect contents.
            var saved=new CompoundTag();crucible.contents().save(saved);AspectNbt.write(saved,mixed);crucible.contents().load(saved);
            amount--;sync();crucible.synchronize();
            for(int i=0;i<5;i++)CrimsonDrainNetwork.trail(level,net.minecraft.world.phys.Vec3.atLowerCornerOf(worldPosition).add(level.random.nextDouble(),1,level.random.nextDouble()),net.minecraft.world.phys.Vec3.atCenterOf(pos).add(0,1,0),3,1,0xaaaaaa,1.3f);return;
        }
    }
    /** Original insertion order matters: only the last two different stored aspects are considered. */
    public static AspectAmounts combineLastPair(AspectAmounts input){
        var entries=input.aspects().stream().filter(a->input.amount(a)>0).toList();if(entries.size()<2)return null;
        var first=entries.get(entries.size()-1);var second=entries.get(entries.size()-2);
        var combination=TCAspects.catalog().combine(first,second);if(combination.isEmpty())return null;
        var result=new AspectAmounts();for(int i=0;i<entries.size()-2;i++)result.add(entries.get(i),input.amount(entries.get(i)));
        result.add(combination.get(),1);
        if(input.amount(second)>1)result.add(second,input.amount(second)-1);
        if(input.amount(first)>1)result.add(first,input.amount(first)-1);
        return result;
    }
    public void smelt(ItemEntity entity){
        if(flesh || !entity.isAlive() || entity instanceof SpecialItemEntity || amount>=64 || !entity.getItem().is(TCItems.material(MaterialType.QUICKSILVER)))return;
        int consumed=Math.min(entity.getItem().getCount(),(64-amount+1)/2);
        var remainder=entity.getItem().copy();remainder.shrink(consumed);entity.setItem(remainder);if(remainder.isEmpty())entity.discard();
        amount=Math.min(64,amount+consumed*2);bubble();sync();
    }
    public void smelt(MadThaumaturge entity){
        if(!flesh || !entity.isAlive() || amount>=64)return;
        entity.discard();amount=Math.min(64,amount+2);bubble();sync();
    }
    public void sip(ServerPlayer player){
        if(!flesh || amount==0 || !EtherealState.restoreFlesh(player))return;
        amount--;sync();level.playSound(null,worldPosition,TCSounds.GORE.get(),SoundSource.BLOCKS,.3f,1+level.random.nextFloat()*.4f);
    }
    private void bubble(){level.playSound(null,worldPosition,TCSounds.BUBBLE.get(),SoundSource.BLOCKS,.2f,1+level.random.nextFloat()*.4f);}
    public void sync(){setChanged();if(level!=null&&!level.isClientSide){level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);level.updateNeighbourForOutputSignal(worldPosition,getBlockState().getBlock());}}
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);tag.putInt("Amount",amount);tag.putInt("Cooldown",cooldown);}
    @Override public void load(CompoundTag tag){super.load(tag);amount=Mth.clamp(tag.getInt("Amount"),0,64);cooldown=Mth.clamp(tag.getInt("Cooldown"),0,20);}
    @Override public CompoundTag getUpdateTag(){return saveWithoutMetadata();}
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
}
