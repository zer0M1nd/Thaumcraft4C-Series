package tc4revival.research;
import java.util.*;
import net.minecraft.world.item.ItemStack;
import tc4revival.aspect.item.ItemStackAspects;
/** Read-only container display; unknown aspects never expose their identity or quantity. */
public  class InventoryAspectDisplay {
    public record Icon(String tag,int amount) {}
    private InventoryAspectDisplay() {}
    public static boolean visible(boolean normallyVisible,boolean shift) { return normallyVisible!=shift; }
    public static List<Icon> resolveSelf(net.minecraft.server.level.ServerPlayer player,PlayerKnowledge knowledge) {
        var target=ScanTargetResolver.entity(player).orElse(null);
        if(target==null||knowledge==null||target.identity().isEmpty()||!knowledge.scanned(target.kind(),target.identity().get()))return List.of();
        return presentTarget(target.kind(),target.identity().get(),target.aspects(),knowledge);
    }
    public static List<Icon> resolve(ItemStack stack,PlayerKnowledge knowledge) {
        if(stack.isEmpty() || knowledge==null)return List.of();
        if(!knowledge.scanned(PlayerKnowledge.ScanKind.OBJECT,ScanIdentity.item(stack)))return List.of();
        var amounts=ItemStackAspects.resolve(stack).orElse(null);
        return present(ScanIdentity.item(stack),amounts,knowledge);
    }
    public static List<Icon> present(String identity,tc4revival.aspect.AspectAmounts amounts,PlayerKnowledge knowledge){
        return presentTarget(PlayerKnowledge.ScanKind.OBJECT,identity,amounts,knowledge);
    }
    public static List<Icon> presentTarget(PlayerKnowledge.ScanKind kind,String identity,tc4revival.aspect.AspectAmounts amounts,PlayerKnowledge knowledge){
        if(knowledge==null||!knowledge.scanned(kind,identity))return List.of();
        if(amounts==null || amounts.isEmpty())return List.of(new Icon(ScanIdentity.EMPTY_ICON,0));
        var projected=thaumcraft.api.research.ScannerViews.inventory(new thaumcraft.api.research.ScannerViews.Inventory(kind,identity,amounts,knowledge));
        if(projected.isPresent())return projected.get();
        var aspects=new ArrayList<>(amounts.aspects());
        aspects.sort(Comparator.comparingInt(amounts::amount).reversed());
        return aspects.stream().map(a->knowledge.knows(a)?new Icon(a.tag(),amounts.amount(a)):new Icon("",0)).toList();
    }
}
