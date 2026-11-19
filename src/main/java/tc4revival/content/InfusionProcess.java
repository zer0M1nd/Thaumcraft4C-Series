package tc4revival.content;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import tc4revival.aspect.Aspect;
import tc4revival.aspect.AspectAmounts;
import tc4revival.aspect.AspectNbt;
import tc4revival.aspect.TCAspects;

/** Persistent crafting transaction. The host supplies world operations in the original order. */
public  class InfusionProcess {
    public enum Experience { NO_PLAYERS, NO_LEVELS, DRAINED }
    public interface Host {
        boolean centerMatches(ItemStack expected);
        Experience drainExperience();
        boolean drainEssentia(Aspect aspect);
        List<ArcanePedestalBlockEntity> pedestals();
        void ingredientStarted(BlockPos position);
        void instabilityEvent(int event);
        void refreshSurroundings();
        void failed();
        void finish(InfusionOutput output,String player);
        void changed();
    }
    private boolean crafting,enchanting;
    private CompoundTag recovery;
    public boolean needsRecovery() { return recovery!=null; }
    public boolean retryRecovery() { if(recovery!=null)load(recovery.copy());return recovery==null; }
    private ItemStack input=ItemStack.EMPTY;
    private InfusionOutput output=InfusionOutput.item(ItemStack.EMPTY);
    private final List<InfusionIngredient> ingredients=new ArrayList<>();
    private AspectAmounts remaining=new AspectAmounts(),required=new AspectAmounts();
    private int instability,recipeInstability,xp,itemCount,delay=10;
    private BlockPos ingredientPosition;
    private int ingredientIndex=-1;
    private String player="";
    public boolean crafting() { return crafting; }
    public int instability() { return instability; }
    /** Adjust the active transaction without overflowing or changing recipe-level instability. */
    public void addInstability(int amount) {
        if (crafting) instability=(int)Math.max(Integer.MIN_VALUE,Math.min(Integer.MAX_VALUE,(long)instability+amount));
    }
    public int recipeInstability() { return recipeInstability; }
    public int delay() { return delay; }
    public AspectAmounts remaining() { return remaining.copy(); }
    public AspectAmounts required() { return required.copy(); }
    public void start(InfusionRecipe.Plan plan,ItemStack center,String player,int symmetry) {
        if (crafting || needsRecovery() || center.isEmpty()) throw new IllegalStateException("Infusion cannot start");
        input=center.copy(); output=plan.output(); ingredients.clear(); ingredients.addAll(plan.components());
        remaining=plan.aspects(); required=remaining.copy(); recipeInstability=plan.instability(); instability=Math.addExact(symmetry,recipeInstability);
        enchanting=plan.enchanting(); xp=plan.xp();
        this.player=player;
        crafting=true;
        itemCount=0; delay=10; ingredientPosition=null; ingredientIndex=-1;
    }
    public void cycle(RandomSource random,Host host) {
        if (!crafting) return;
        boolean valid=host.centerMatches(input.copy());
        if (valid && instability>0 && random.nextInt(500)<=instability) {
            host.instabilityEvent(random.nextInt(21));
            return;
        }
        if (!valid) {
            crafting=false; instability=0; remaining=new AspectAmounts(); host.failed(); host.changed(); return;
        }
        if (enchanting && xp>0) {
            var result=host.drainExperience();
            if (result==Experience.DRAINED) { xp--; delay=20; host.changed(); return; }
            if (result==Experience.NO_LEVELS && !remaining.isEmpty() && random.nextInt(3)==0) {
                addMissingAspect(random);
                increaseInstability(random,50,2); host.changed();
            }
            return;
        }
        if (enchanting && xp==0) delay=10;
        if (remaining.total()>0) {
            for (var aspect:remaining.aspects()) {
                if (remaining.amount(aspect)<=0) continue;
                if (host.drainEssentia(aspect)) { remaining.reduce(aspect,1); host.changed(); return; }
                increaseInstability(random,100,3); host.changed();
            }
            host.refreshSurroundings(); return;
        }
        if (!ingredients.isEmpty()) {
            var pedestals=host.pedestals().stream().filter(p->p!=null && !p.isRemoved() && !p.getItem(0).isEmpty()).toList();
            var assignment=InfusionMatching.assignment(pedestals.stream().map(p->p.getItem(0)).toList(),ingredients,true);
            for (int index=0;index<ingredients.size();index++) {
                var required=ingredients.get(index);
                for (int slot=0;slot<pedestals.size();slot++) {
                    var pedestal=pedestals.get(slot);
                    if (!required.matches(pedestal.getItem(0))) continue;
                    if (assignment!=null ? assignment[slot]!=index
                            : ingredients.stream().filter(i->i.matches(pedestal.getItem(0))).count()!=1) continue;
                    if (itemCount==0 || ingredientIndex!=index || !pedestal.getBlockPos().equals(ingredientPosition)) {
                        itemCount=5; ingredientIndex=index; ingredientPosition=pedestal.getBlockPos().immutable();
                        host.ingredientStarted(ingredientPosition);
                    }
                    else if (itemCount--<=1) {
                        var stack=pedestal.removeItem(0,1);
                        var remainder=stack.hasCraftingRemainingItem() ? stack.getCraftingRemainingItem() : ItemStack.EMPTY;
                        if(pedestal.isEmpty())pedestal.setFromInfusion(remainder);
                        else if(!remainder.isEmpty())net.minecraft.world.Containers.dropItemStack(pedestal.getLevel(),
                                pedestal.getBlockPos().getX()+.5,pedestal.getBlockPos().getY()+1,pedestal.getBlockPos().getZ()+.5,remainder);
                        ingredients.remove(index); ingredientPosition=null; ingredientIndex=-1;
                    }
                    host.changed(); return;
                }
                if (!remaining.isEmpty() && random.nextInt(1+index)==0) {
                    addMissingAspect(random); increaseInstability(random,50,2); host.changed();
                }
            }
            return;
        }
        crafting=false; instability=0;
        var result=output; output=InfusionOutput.item(ItemStack.EMPTY);
        // Clear the transaction before publishing the result so callbacks cannot finish it twice.
        host.finish(result,player); remaining=new AspectAmounts(); host.changed();
    }
    private void addMissingAspect(RandomSource random) {
        var aspects=remaining.aspects(); var aspect=aspects.get(random.nextInt(aspects.size()));
        if (remaining.amount(aspect)<Integer.MAX_VALUE) remaining.add(aspect,1);
    }
    private void increaseInstability(RandomSource random,int base,int multiplier) {
        // High dynamic enchantment levels made the old nextInt bound nonpositive.
        int bound=(int)Math.max(1,Math.min(Integer.MAX_VALUE,base-(long)recipeInstability*multiplier));
        if (random.nextInt(bound)==0 && instability<Integer.MAX_VALUE) instability++;
        if (instability>25) instability=25;
    }
    public CompoundTag save() {
        if(recovery!=null)return recovery.copy(); // Preserve the original transaction verbatim for repair/retry.
        var tag=new CompoundTag(); tag.putBoolean("Crafting",crafting); tag.putBoolean("Enchanting",enchanting);
        tag.put("Input",input.save(new CompoundTag()));
        tag.put("Output",output.save());
        var components=new ListTag(); for (var ingredient:ingredients) components.add(StringTag.valueOf(ingredient.definition().toString()));
        tag.put("Ingredients",components); AspectNbt.write(tag,remaining);
        var originalCost=new CompoundTag();AspectNbt.write(originalCost,required);tag.put("RecipeCost",originalCost);
        tag.putInt("Instability",instability); tag.putInt("RecipeInstability",recipeInstability); tag.putInt("Experience",xp); tag.putString("Player",player);
        tag.putInt("ItemCount",itemCount); tag.putInt("Delay",delay); tag.putInt("IngredientIndex",ingredientIndex);
        if(ingredientPosition!=null)tag.putLong("IngredientPosition",ingredientPosition.asLong());
        return tag;
    }
    public void load(CompoundTag tag) {
        recovery=null;crafting=false; ingredients.clear(); remaining=new AspectAmounts(); required=new AspectAmounts(); itemCount=0; delay=10;
        try {
        itemCount=Math.max(0,Math.min(5,tag.getInt("ItemCount")));
        if(tag.contains("Delay"))delay=Math.max(1,Math.min(20,tag.getInt("Delay")));
        ingredientIndex=tag.contains("IngredientIndex")?tag.getInt("IngredientIndex"):-1;
        ingredientPosition=tag.contains("IngredientPosition")?BlockPos.of(tag.getLong("IngredientPosition")):null;
        input=ItemStack.of(tag.getCompound("Input")); output=InfusionOutput.load(tag.getCompound("Output"));
        enchanting=tag.getBoolean("Enchanting"); instability=tag.getInt("Instability"); recipeInstability=tag.getInt("RecipeInstability");
        if(enchanting && tag.getBoolean("Crafting"))output=output.asEnchantment(input);
        xp=Math.max(0,tag.getInt("Experience")); player=tag.getString("Player");
            if(tag.contains("Ingredients")&&!(tag.get("Ingredients") instanceof ListTag list&& (list.isEmpty()||list.getElementType()==Tag.TAG_STRING)))throw new IllegalArgumentException("Invalid Ingredients list");
            var components=tag.getList("Ingredients",Tag.TAG_STRING);
            for (int index=0;index<components.size();index++) ingredients.add(new InfusionIngredient(com.google.gson.JsonParser.parseString(components.getString(index)).getAsJsonObject()));
            var outstanding=AspectNbt.read(tag,TCAspects.catalog());
            var cost=tag.contains("RecipeCost",Tag.TAG_COMPOUND)?AspectNbt.read(tag.getCompound("RecipeCost"),TCAspects.catalog()):outstanding;
            if(!outstanding.problems().isEmpty()||!cost.problems().isEmpty())throw new IllegalArgumentException(outstanding.problems()+" / "+cost.problems());
            remaining=outstanding.amounts();required=cost.amounts();
            for(var aspect:remaining.aspects())if(remaining.amount(aspect)<0)throw new IllegalArgumentException("Negative remaining essentia");
            if(tag.getBoolean("Crafting")&&(input.isEmpty()||!output.consumesCenter()&&output.apply(input).isEmpty()))throw new IllegalArgumentException("Missing infusion input/output");
            crafting=tag.getBoolean("Crafting") && !input.isEmpty();
        } catch (RuntimeException malformed) {
            recovery=tag.copy();ingredients.clear();remaining=new AspectAmounts();required=new AspectAmounts();crafting=false;
            com.mojang.logging.LogUtils.getLogger().error("Infusion transaction suspended; original NBT retained in Process for recovery",malformed);
        }
    }
}
