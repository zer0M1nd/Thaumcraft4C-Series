package thaumicomniverse.compat.gregtech;

import com.gregtechceu.gtceu.api.capability.recipe.*;
import com.gregtechceu.gtceu.api.machine.*;
import com.gregtechceu.gtceu.api.machine.multiblock.part.TieredIOPartMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableRecipeHandlerTrait;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.lowdragmc.lowdraglib.syncdata.annotation.*;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.lowdragmc.lowdraglib.gui.widget.*;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import tc4revival.aspect.*;
import tc4revival.content.EssentiaTransport;
import thaumcraft.api.aspects.IEssentiaStorage;
import thaumcraft.api.aspects.EssentiaStorageApi;
import thaumicomniverse.RevivalFeatures;
import java.util.*;

/** One aspect per hatch. Total capacity starts at 64 at LV and doubles at each tier. */
public class EssentiaHatch extends TieredIOPartMachine implements IEssentiaStorage,EssentiaTransport {
    public static final ManagedFieldHolder FIELDS=new ManagedFieldHolder(EssentiaHatch.class,TieredIOPartMachine.MANAGED_FIELD_HOLDER);
    @Persisted @DescSynced private String aspectId="";
    @Persisted @DescSynced private int stored;
    public final FuelHandler fuelHandler=new FuelHandler(this);
    private TickableSubscription pullSubscription;
    public EssentiaHatch(IMachineBlockEntity holder,int tier){super(holder,tier,IO.IN);}
    @Override public ManagedFieldHolder getFieldHolder(){return FIELDS;}
    public int capacity(){return 64 << Math.max(0,Math.min(13,getTier()-1));}
    public Aspect aspect(){return aspectId.isEmpty()?null:TCAspects.catalog().find(aspectId).orElse(null);}
    public int amount(){return stored;}
    public boolean enabled(){return isWorkingEnabled()&&RevivalFeatures.Group.GREGTECH.enabled();}
    private void changed(){markDirty();fuelHandler.notifyListeners();if(getLevel()!=null){var be=getLevel().getBlockEntity(getPos());if(be!=null)EssentiaStorageApi.changed(be);}}
    @Override public AspectAmounts contents(){var aspect=aspect();return aspect==null||stored<=0?new AspectAmounts():new AspectAmounts().add(aspect,stored);}
    @Override public int capacity(Aspect aspect){return aspect!=null&&(stored==0||aspect.tag().equals(aspectId))?capacity():0;}
    @Override public int insert(Aspect aspect,int amount,boolean simulate){
        if(amount<0)throw new IllegalArgumentException("Negative essentia");
        if(!enabled()||aspect==null||stored>0&&!aspect.tag().equals(aspectId))return 0;
        int moved=Math.min(amount,Math.max(0,capacity()-stored));
        if(!simulate&&moved>0){aspectId=aspect.tag();stored+=moved;changed();}return moved;
    }
    @Override public int extract(Aspect aspect,int amount,boolean simulate){
        if(amount<0)throw new IllegalArgumentException("Negative essentia");
        if(aspect==null||!aspect.tag().equals(aspectId))return 0;
        int moved=Math.min(amount,stored);if(!simulate&&moved>0){stored-=moved;if(stored==0)aspectId="";changed();}return moved;
    }
    @Override public void onLoad(){super.onLoad();if(!isRemote())pullSubscription=subscribeServerTick(this::pull);}
    @Override public void onUnload(){if(pullSubscription!=null){pullSubscription.unsubscribe();pullSubscription=null;}super.onUnload();}
    private void pull(){
        if(!enabled()||stored>=capacity()||!(getLevel() instanceof ServerLevel level)||getOffsetTimer()%5!=0)return;
        // Inventory access is deliberately unsided: a rotated jar or machine can supply the front.
        var source=EssentiaStorageApi.atInventory(level,getPos().relative(getFrontFacing()));
        for(var aspect:source.contents().aspects()){
            int possible=insert(aspect,Math.min(8,source.contents().amount(aspect)),true);
            if(possible>0){insert(aspect,source.extract(aspect,possible,false),false);break;}
        }
        if(stored>=capacity())return;
        for(var direction:Direction.values()){
            var pos=getPos().relative(direction);if(!level.hasChunkAt(pos))continue;
            if(!(level.getBlockEntity(pos) instanceof EssentiaTransport transport))continue;
            var face=direction.getOpposite();var type=transport.essentiaType(face);
            if(type!=null&&transport.canOutput(face)&&transport.connectable(face)&&transport.suction(face)<128&&transport.minimumSuction()<=128&&insert(type,1,true)>0){
                insert(type,transport.takeEssentia(type,1,face),false);
            }
        }
    }
    @Override public Widget createUIWidget(){
        return new WidgetGroup(0,0,176,64).addWidget(new ComponentPanelWidget(4,8,lines->{
            lines.add(Component.translatable("thaumicomniverse.gt.hatch_contents",stored,capacity()));
            if(aspect()!=null)lines.add(Component.literal(thaumcraft.api.aspects.AspectViews.display(aspect()).displayName()));
        }).setMaxWidthLimit(168));
    }
    @Override public boolean connectable(Direction face){return true;}
    @Override public boolean canInput(Direction face){return enabled();}
    @Override public boolean canOutput(Direction face){return false;}
    @Override public Aspect suctionType(Direction face){return aspect();}
    @Override public int suction(Direction face){return enabled()&&stored<capacity()?128:0;}
    @Override public int minimumSuction(){return 0;}
    @Override public Aspect essentiaType(Direction face){return aspect();}
    @Override public int essentiaAmount(Direction face){return stored;}
    @Override public int takeEssentia(Aspect aspect,int amount,Direction face){return 0;}
    @Override public int addEssentia(Aspect aspect,int amount,Direction face){return insert(aspect,amount,false);}

    public static class FuelHandler extends NotifiableRecipeHandlerTrait<EssentiaFuel> {
        private final EssentiaHatch hatch;
        public FuelHandler(EssentiaHatch hatch){super(hatch);this.hatch=hatch;}
        @Override public IO getHandlerIO(){return IO.IN;}
        @Override public RecipeCapability<EssentiaFuel> getCapability(){return EssentiaFuelCapability.CAP;}
        @Override public List<Object> getContents(){return hatch.stored<=0?List.of():List.of(new EssentiaFuel(hatch.aspectId,hatch.stored));}
        @Override public double getTotalContentAmount(){return hatch.stored;}
        @Override public int getSize(){return 1;}
        @Override public List<EssentiaFuel> handleRecipeInner(IO io,GTRecipe recipe,List<EssentiaFuel> left,boolean simulate){
            if(io!=IO.IN||!hatch.enabled())return left;
            int available=hatch.stored;var remaining=new ArrayList<EssentiaFuel>();
            for(var need:left){int used=need.aspect().equals(hatch.aspectId)?Math.min(available,need.amount()):0;available-=used;
                if(used<need.amount())remaining.add(new EssentiaFuel(need.aspect(),need.amount()-used));}
            if(!simulate&&available!=hatch.stored){hatch.stored=available;if(available==0)hatch.aspectId="";hatch.changed();}
            return remaining.isEmpty()?null:remaining;
        }
    }
}
