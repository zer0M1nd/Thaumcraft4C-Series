package thmodern.soul;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import tc4revival.aspect.*;
import tc4revival.content.EssentiaTransport;
import thmodern.Horizons;
import thmodern.containment.*;

public final class SoulforgeEntity extends BlockEntity implements SoulReceiver,EssentiaTransport {
    private final SoulAssembly assembly=new SoulAssembly();
    private int forging;
    public SoulforgeEntity(BlockPos pos,BlockState state){super(Horizons.SOULFORGE_ENTITY.get(),pos,state);}
    public int souls(){return assembly.souls();}public int progress(){return assembly.bits();}public int energy(){return assembly.energy();}public boolean working(){return forging>0;}
    private void changed(){setChanged();if(level!=null&&!level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    @Override public int soulRoom(){return assembly.room();}
    @Override public int acceptSoulBits(int amount){if(level==null||level.isClientSide)return 0;int count=assembly.accept(amount);if(count>0){forging=3;changed();}return count;}
    public boolean bottle(ServerPlayer player,ItemStack held){
        if(level==null||level.isClientSide||player.level()!=level||level.getBlockEntity(worldPosition)!=this||player.distanceToSqr(worldPosition.getCenter())>64||!CreatureJar.empty(held)||souls()==0)return false;
        var professions=new java.util.ArrayList<net.minecraft.resources.ResourceLocation>();
        for(String key:java.util.List.of("farmer","librarian","cleric","armorer","butcher"))professions.add(net.minecraft.resources.ResourceLocation.parse("minecraft:"+key));
        for(var key:net.minecraft.core.registries.BuiltInRegistries.VILLAGER_PROFESSION.keySet())if(!key.getNamespace().equals("minecraft"))professions.add(key);
        var output=SoulJar.create(professions.get(level.random.nextInt(professions.size())));
        if(!CreatureJar.exchange(player,held,output))return false;assembly.takeSoul();changed();return true;
    }
    public void tick(){
        if(level==null)return;if(level.isClientSide){if(working()&&level.getGameTime()%3==0)for(int x:new int[]{0,1})for(int z:new int[]{0,1})thaumcraft.api.fx.LegacyEffects.emit(level,"forge_ring",new net.minecraft.world.phys.Vec3(worldPosition.getX()+.025+.95*x,worldPosition.getY()+.75,worldPosition.getZ()+.025+.95*z),net.minecraft.world.phys.Vec3.ZERO,0xffffff,1,3,1,0,0);return;}if(forging>0){forging--;if(forging==0)changed();}
        if(suction(Direction.UP)==0||!level.hasChunkAt(worldPosition.above()))return;
        if(level.getBlockEntity(worldPosition.above()) instanceof EssentiaTransport source&&source.connectable(Direction.DOWN)&&source.canOutput(Direction.DOWN)&&source.suction(Direction.DOWN)<128&&source.minimumSuction()<=128){
            var type=source.essentiaType(Direction.DOWN);if(type!=null&&type.tag().equals("cognitio")&&source.essentiaAmount(Direction.DOWN)>0&&source.takeEssentia(type,1,Direction.DOWN)==1)addEssentia(type,1,Direction.UP);
        }
    }
    @Override public boolean connectable(Direction face){return face==Direction.UP;}
    @Override public boolean canInput(Direction face){return connectable(face);}
    @Override public boolean canOutput(Direction face){return false;}
    @Override public Aspect suctionType(Direction face){return connectable(face)?TCAspects.catalog().require("cognitio"):null;}
    @Override public int suction(Direction face){return connectable(face)&&energy()<3000?128:0;}
    @Override public int minimumSuction(){return 0;}
    @Override public Aspect essentiaType(Direction face){return connectable(face)&&energy()>0?TCAspects.catalog().require("cognitio"):null;}
    @Override public int essentiaAmount(Direction face){return connectable(face)?energy()/1000:0;}
    @Override public int takeEssentia(Aspect type,int amount,Direction face){return 0;}
    @Override public int addEssentia(Aspect type,int amount,Direction face){if(level==null||level.isClientSide||!connectable(face)||type==null||!type.tag().equals("cognitio"))return 0;int count=assembly.addEssentia(amount);if(count>0)changed();return count;}
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);tag.putInt("Bits",progress());tag.putInt("Souls",souls());tag.putInt("Energy",energy());tag.putInt("Forging",forging);}
    @Override public void load(CompoundTag tag){super.load(tag);assembly.load(tag.getInt("Bits"),tag.getInt("Souls"),tag.getInt("Energy"));forging=Math.max(0,Math.min(3,tag.getInt("Forging")));}
    @Override public CompoundTag getUpdateTag(){return saveWithoutMetadata();}
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
}
