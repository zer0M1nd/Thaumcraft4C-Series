package advancedthaumaturgy;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.content.*;
import tc4revival.aspect.*;

/** Only physical inputs persist; the output is a transaction-local preview. */
public class WandbenchEntity extends SyncedEntity implements Container {
    private final NonNullList<ItemStack> items=NonNullList.withSize(5,ItemStack.EMPTY);
    public WandbenchEntity(BlockPos pos,BlockState state){super(AdvancedThaumaturgy.BENCH_ENTITY.get(),pos,state);}
    public record Plan(ItemStack result,int cost,int[] used){}
    public static WandParts.Rod rod(ItemStack item){if(item.is(AdvancedThaumaturgy.MERCURIAL_ROD.get()))return WandParts.Rod.require(MercurialWandItem.ROD);return Arrays.stream(WandParts.Rod.values()).filter(r->matches(item,r.item!=null?r.item:r==WandParts.Rod.WOOD?"minecraft:stick":r.tag.contains(":")?null:"thaumcraft:"+WandComponent.rodPath(r),-1)).findFirst().orElse(null);}
    public static WandParts.Cap cap(ItemStack item){return Arrays.stream(WandParts.Cap.values()).filter(r->matches(item,r.item!=null?r.item:r.tag.contains(":")?null:"thaumcraft:"+WandComponent.capPath(r),-1)).findFirst().orElse(null);}
    private static boolean matches(ItemStack stack,String id,int metadata){return !stack.isEmpty()&&id!=null&&id.equals(ForgeRegistries.ITEMS.getKey(stack.getItem()).toString())&&(metadata<0||stack.hasTag()&&stack.getTag().getInt("tc4_metadata")==metadata);}
    public Plan plan(){
        if(!ATConfig.enabled("wandbench")||!(items.get(0).getItem() instanceof CastingWandItem original))return null;
        ItemStack result=items.get(0).copyWithCount(1);int[] used={1,0,0,0,0};int cost=0;boolean changed=false;
        if(!items.get(2).isEmpty()){
            var rod=rod(items.get(2));if(rod==null)return null;
            if(!rod.tag.equals(original.rod(result).tag)){
                var tag=result.getOrCreateTag().copy();tag.putString("rod",rod.tag);
                result=new ItemStack(rod.tag.equals(MercurialWandItem.ROD)?AdvancedThaumaturgy.MERCURIAL_WAND.get():TCItems.CASTING_WAND.get());result.setTag(tag);
                used[2]=1;changed=true;cost=rod.craftCost*original.cap(result).craftCost;
            }
        }
        CastingWandItem wand=(CastingWandItem)result.getItem();
        if(!items.get(3).isEmpty()){
            var cap=cap(items.get(3));if(cap==null||items.get(3).getCount()<2)return null;
            if(!cap.tag.equals(wand.cap(result).tag)){result.getOrCreateTag().putString("cap",cap.tag);used[3]=2;cost=cap.craftCost*wand.rod(result).craftCost;changed=true;}
        }
        if(!items.get(1).isEmpty()){
            if(!ATConfig.enabled("wand_upgrades")||!(items.get(1).getItem() instanceof CrystalItem crystal)||!CrystalItem.apply(result,crystal.upgrade))return null;
            used[1]=1;cost+=42;changed=true;
        }
        if(!changed)return null;
        for(var aspect:TCAspects.catalog().primals())if(result.getOrCreateTag().getInt(aspect.tag())>wand.capacity(result))result.getOrCreateTag().putInt(aspect.tag(),wand.capacity(result));
        result.getOrCreateTag().putInt("tc4_metadata",(int)(wand.rod(result).craftCost*wand.cap(result).craftCost*(wand.isSceptre(result)?1.5f:1)));
        return new Plan(result,cost,used);
    }
    private boolean pay(Plan plan,Player player,boolean commit){
        if(plan.cost()==0)return true;
        if(!(items.get(4).getItem() instanceof CastingWandItem payer))return false;
        Map<Aspect,Integer> costs=new LinkedHashMap<>();TCAspects.catalog().primals().forEach(a->costs.put(a,plan.cost()));
        return payer.consumeForPlayer(items.get(4),player,costs,true,true,commit);
    }
    public ItemStack preview(Player player){var p=plan();return p!=null&&pay(p,player,false)?p.result():ItemStack.EMPTY;}
    public ItemStack craft(Player player){
        if(level==null||level.isClientSide||!stillValid(player))return ItemStack.EMPTY;
        var p=plan();if(p==null||!pay(p,player,true))return ItemStack.EMPTY;
        for(int i=0;i<items.size();i++)items.get(i).shrink(p.used()[i]);sync();return p.result();
    }
    @Override public int getContainerSize(){return items.size();}
    @Override public boolean isEmpty(){return items.stream().allMatch(ItemStack::isEmpty);}
    @Override public ItemStack getItem(int i){return items.get(i);}
    @Override public ItemStack removeItem(int i,int n){var result=ContainerHelper.removeItem(items,i,n);if(!result.isEmpty())sync();return result;}
    @Override public ItemStack removeItemNoUpdate(int i){return ContainerHelper.takeItem(items,i);}
    @Override public void setItem(int i,ItemStack s){items.set(i,s);s.setCount(Math.min(s.getCount(),getMaxStackSize()));sync();}
    @Override public boolean stillValid(Player p){return !isRemoved()&&p.level()==level&&p.distanceToSqr(worldPosition.getCenter())<=64;}
    @Override public void clearContent(){items.clear();sync();}
    @Override public boolean canPlaceItem(int i,ItemStack s){return switch(i){case 0,4->s.getItem() instanceof CastingWandItem;case 1->s.getItem() instanceof CrystalItem c&&c.upgrade!=CrystalItem.Upgrade.NONE;case 2->rod(s)!=null;case 3->cap(s)!=null;default->false;};}
    @Override protected void saveAdditional(CompoundTag n){super.saveAdditional(n);ContainerHelper.saveAllItems(n,items);}
    @Override public void load(CompoundTag n){super.load(n);items.clear();ContainerHelper.loadAllItems(n,items);}
}
