package conciliummodern;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import tc4revival.aspect.*;
import tc4revival.research.*;

public final class PredictabilityBlockEntity extends BlockEntity implements CondenserReceiver,tc4revival.content.RevealedAspectContainer {
    private BlockPos center;
    private boolean hasRift;
    private int heat;
    private AspectAmounts essentia=new AspectAmounts();
    public PredictabilityBlockEntity(BlockPos pos,BlockState state){super(ConciliumBlocks.HEX_TILE.get(),pos,state);center=pos;}
    public BlockPos center(){return center;}
    public boolean isMaster(){return worldPosition.equals(center);}
    public boolean hasRift(){return hasRift;}
    public int heat(){return heat;}
    public void configure(BlockPos master){center=master.immutable();changed();}
    @Override public AspectAmounts revealedAspects(){return essentia.copy();}
    public RiftEntity rift(){
        if(level==null || !isMaster())return null;
        return level.getEntitiesOfClass(RiftEntity.class,new AABB(worldPosition).inflate(2,4,2),rift->rift.isAlive() && worldPosition.equals(rift.boundCenter())).stream().findFirst().orElse(null);
    }
    @Override public boolean receiveCrystal(DestabilizedCrystalBlockEntity crystal){
        if(level==null || level.isClientSide || !isMaster() || rift()==null || rift().isClosing() || crystal.getLevel()!=level || crystal.amount()<=0 || !PredictabilityStructure.complete(level,center))return false;
        var aspect=TCAspects.catalog().find(crystal.aspectKey()).orElse(null);int amount=crystal.amount();
        if(aspect==null || amount>Integer.MAX_VALUE-essentia.amount(aspect))return false;
        if(crystal.drain(crystal.aspectKey(),amount)!=amount)return false;
        essentia.add(aspect,amount);heat=1200;hasRift=true;changed();return true;
    }
    public static void tick(Level world,BlockPos pos,BlockState state,PredictabilityBlockEntity tile){
        if(!(world instanceof ServerLevel level))return;
        if(!tile.isMaster()){
            if(level.hasChunkAt(tile.center) && (!(level.getBlockEntity(tile.center) instanceof PredictabilityBlockEntity master) || !master.isMaster()))
                level.setBlock(pos,tc4revival.content.TCEldritch.STONE.get().defaultBlockState(),3);
            return;
        }
        if(!PredictabilityStructure.loaded(level,pos))return;
        if(!PredictabilityStructure.complete(level,pos)){PredictabilityStructure.restore(level,pos,null);return;}
        var rift=tile.rift();
        if(tile.hasRift!=(rift!=null)){tile.hasRift=rift!=null;if(rift==null)tile.essentia=new AspectAmounts();tile.changed();}
        if(rift==null)return;
        if(rift.isClosing())return;
        if(tile.heat>=2400){rift.collapseTo(0);tile.heat=0;tile.essentia=new AspectAmounts();tile.changed();return;}
        int oldHeat=tile.heat;
        if(!tile.essentia.isEmpty())for(var item:level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(8,3,8),entity->entity.isAlive() && entity.getItem().is(ConciliumItems.VOID_SLAG.get())))tile.heat=Math.min(2400,tile.heat+2);
        var items=level.getEntitiesOfClass(ItemEntity.class,new AABB(pos.getX()-2,pos.getY(),pos.getZ()-2,pos.getX()+2,pos.getY()+4,pos.getZ()+2),ItemEntity::isAlive);
        if(items.size()==1){
            if(tile.heat>0){
                if(level.getGameTime()%60==0 && level.random.nextInt(10)>=8)for(int i=0,n=3+level.random.nextInt(3);i<n;i++){
                    var slag=new ItemEntity(level,pos.getX()+.5+(level.random.nextDouble()-.5)*2,pos.getY()+2,pos.getZ()+.5+(level.random.nextDouble()-.5)*2,new ItemStack(ConciliumItems.VOID_SLAG.get()));
                    slag.setDeltaMovement((level.random.nextDouble()-.5)*.4,.2,(level.random.nextDouble()-.5)*.4);level.addFreshEntity(slag);
                }
                tile.heat=Math.max(0,tile.heat-3);
            }else if(!tile.essentia.isEmpty())tile.craft(level,items.get(0));
        }
        if(oldHeat!=tile.heat){tile.setChanged();if(level.getGameTime()%10==0 || tile.heat==0)tile.changed();}
    }
    private void craft(ServerLevel level,ItemEntity item){
        var saved=new CompoundTag();item.saveWithoutId(saved);if(!saved.hasUUID("Thrower"))return;
        var owner=saved.getUUID("Thrower");var knowledge=KnowledgeSavedData.get(level.getServer()).snapshot(owner);if(knowledge.isEmpty())return;
        var recipe=level.getRecipeManager().getAllRecipesFor(ChainedRiftRecipe.TYPE.get()).stream().sorted(java.util.Comparator.<ChainedRiftRecipe>comparingInt(r->r.aspectCosts().size()).reversed().thenComparing(r->r.getId().toString()))
                .filter(r->r.matches(item.getItem(),essentia,key->ResearchCatalogs.rules().completed(knowledge.get(),key))).findFirst().orElse(null);
        if(recipe==null)return;
        var output=recipe.getResultItem(level.registryAccess());var result=new ItemEntity(level,item.getX(),item.getY(),item.getZ(),output);
        result.setDeltaMovement((level.random.nextDouble()-.5)*.8,.4,(level.random.nextDouble()-.5)*.8);result.setDefaultPickUpDelay();
        if(!level.addFreshEntity(result))return;
        var consumed=item.getItem().copyWithCount(recipe.catalystCount());var remainder=item.getItem().copy();remainder.shrink(recipe.catalystCount());
        if(remainder.isEmpty())item.discard();else item.setItem(remainder);
        // The original ritual consumes the whole charged reservoir after one successful recipe.
        essentia=new AspectAmounts();changed();
        var player=level.getServer().getPlayerList().getPlayer(owner);
        if(player!=null)net.minecraftforge.event.ForgeEventFactory.firePlayerCraftingEvent(player,output,new net.minecraft.world.SimpleContainer(consumed));
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT,result.getX(),result.getY(),result.getZ(),10,.4,.4,.4,0);
    }
    private void changed(){setChanged();if(level!=null && !level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);tag.putLong("center",center.asLong());tag.putBoolean("hasRift",hasRift);tag.putInt("heat",heat);var amounts=new CompoundTag();essentia.view().forEach((aspect,amount)->amounts.putInt(aspect.tag(),amount));tag.put("essentia",amounts);}
    @Override public void load(CompoundTag tag){super.load(tag);center=tag.contains("center")?BlockPos.of(tag.getLong("center")):worldPosition;hasRift=tag.getBoolean("hasRift");heat=Math.max(0,Math.min(2400,tag.getInt("heat")));essentia=new AspectAmounts();var amounts=tag.getCompound("essentia");for(String key:amounts.getAllKeys())TCAspects.catalog().find(key).ifPresent(aspect->{int count=amounts.getInt(key);if(count>0)essentia.add(aspect,count);});}
    @Override public CompoundTag getUpdateTag(){return saveWithoutMetadata();}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
    @Override public void onDataPacket(Connection connection,ClientboundBlockEntityDataPacket packet){if(packet.getTag()!=null)load(packet.getTag());}
    @Override public AABB getRenderBoundingBox(){return new AABB(worldPosition).inflate(2,2,2);}
}
