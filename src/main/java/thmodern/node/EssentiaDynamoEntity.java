package thmodern.node;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import tc4revival.aspect.*;
import tc4revival.content.EssentiaTransport;
import thmodern.Horizons;

public final class EssentiaDynamoEntity extends DynamoEntity implements EssentiaTransport {
    private Aspect fuel;
    @Override public int visualColor(){return fuel==null?super.visualColor():fuel.color();}
    public EssentiaDynamoEntity(BlockPos pos,BlockState state) { super(Horizons.ESSENTIA_DYNAMO_ENTITY.get(),pos,state); }
    public static AspectAmounts outputFor(AspectCatalog catalog,Aspect fuel) {
        var primals=catalog.toPrimals(new AspectAmounts().add(fuel,1),AspectCatalog.Reduction.SUM);
        var result=new AspectAmounts();
        if(!primals.isEmpty())for(var primal:primals.aspects())result.add(primal,12/primals.size());
        return result;
    }
    @Override public void tick() {
        super.tick();
        if(level!=null&&level.isClientSide&&active()&&fuel!=null){
            int rgb=fuel.color();var color=new org.joml.Vector3f((rgb>>16&255)/255f,(rgb>>8&255)/255f,(rgb&255)/255f);
            double y=(level.getGameTime()%10)/20d;
            thaumcraft.api.fx.LegacyEffects.emit(level,"essentia",worldPosition.getCenter().add(0,-.5,0),worldPosition.getCenter(),rgb,.3f,20,1,0,0);
        }
        if(level==null||level.isClientSide||suction(Direction.DOWN)==0)return;
        var below=worldPosition.below();
        if(!level.hasChunkAt(below)||!(level.getBlockEntity(below) instanceof EssentiaTransport transport)
                ||!transport.connectable(Direction.UP)||!transport.canOutput(Direction.UP))return;
        var aspect=transport.essentiaType(Direction.UP);
        if(aspect==null||transport.essentiaAmount(Direction.UP)<=0||transport.suction(Direction.UP)>=128||transport.minimumSuction()>128)return;
        if(transport.takeEssentia(aspect,1,Direction.UP)==1)addEssentia(aspect,1,Direction.DOWN);
    }
    @Override public boolean connectable(Direction face){return face==Direction.DOWN;}
    @Override public boolean canInput(Direction face){return connectable(face);}
    @Override public boolean canOutput(Direction face){return false;}
    @Override public Aspect suctionType(Direction face){return null;}
    @Override public int suction(Direction face){return face==Direction.DOWN&&level!=null&&expires-level.getGameTime()<=20?128:0;}
    @Override public int minimumSuction(){return 0;}
    @Override public Aspect essentiaType(Direction face){return face==Direction.DOWN?fuel:null;}
    @Override public int essentiaAmount(Direction face){return 0;}
    @Override public int takeEssentia(Aspect aspect,int amount,Direction face){return 0;}
    @Override public int addEssentia(Aspect aspect,int amount,Direction face){
        if(level==null||level.isClientSide||aspect==null||amount<=0||suction(face)==0)return 0;
        var next=outputFor(TCAspects.catalog(),aspect);
        if(next.isEmpty())return 0;
        fuel=aspect; supply(next,(int)Math.max(0,expires-level.getGameTime())+21);return 1;
    }
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);if(fuel!=null)tag.putString("Fuel",fuel.tag());}
    @Override public void load(CompoundTag tag){super.load(tag);fuel=TCAspects.catalog().find(tag.getString("Fuel")).orElse(null);if(fuel==null)output=new AspectAmounts();}
}
