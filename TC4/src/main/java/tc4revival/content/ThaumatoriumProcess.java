package tc4revival.content;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import tc4revival.aspect.Aspect;
import tc4revival.aspect.AspectAmounts;
import tc4revival.aspect.AspectNbt;
import tc4revival.aspect.TCAspects;

/** Five-tick automatic crucible controller. */
public  class ThaumatoriumProcess {
    public interface Host {
        Optional<CrucibleRecipe> recipe(ResourceLocation id);
        ItemStack input();
        void consumeInput();
        boolean heated();
        boolean powered();
        int recipeLimit();
        default int chaosMatrices() { return 0; }
        default boolean soulHeat() { return false; }
        default Aspect drawAny() { return null; }
        default Optional<Craft> planChaos(AspectAmounts stock) { return Optional.empty(); }
        default void finishChaos(Craft craft) { complete(craft.recipe(),""); }
        boolean outputFits(CrucibleRecipe recipe);
        int draw(Aspect aspect);
        void complete(CrucibleRecipe recipe,String author);
        void changed();
    }
    public record Craft(CrucibleRecipe recipe,ItemStack result) {}
    private final ArrayList<ResourceLocation> recipes=new ArrayList<>();
    private final ArrayList<String> authors=new ArrayList<>();
    private final java.util.Map<ResourceLocation,CrucibleRecipe> resolvedRecipes=new java.util.HashMap<>();
    private AspectAmounts stored=new AspectAmounts();
    private int current=-1,maxRecipes=1,counter;
    private static final int CHAOS_CAPACITY=100;
    private int chaosMatrices, workCredit;
    private boolean heat,soulHeat,active,reconcile;
    private CrucibleRecipe currentRecipe;
    private Aspect suction;
    public List<ResourceLocation> selected() { return List.copyOf(recipes); }
    public AspectAmounts stored() { return stored.copy(); }
    public int limit() { return maxRecipes; }
    public int current() { return current; }
    public Aspect suction() { return suction; }
    public boolean chaotic() { return chaosMatrices>0; }
    public int suctionAmount() {
        if (chaotic()) return active && stored.total()<CHAOS_CAPACITY ? 128 : 0;
        return suction==null ? 0 : 128;
    }
    public void refreshLimit(Host host) {
        int matrices=Math.max(0,Math.min(9,host.chaosMatrices()));
        int limit=matrices==0 ? Math.max(1,Math.min(19,host.recipeLimit())) : 0;
        if (maxRecipes!=limit || chaosMatrices!=matrices) {
            chaosMatrices=matrices;
            maxRecipes=limit;
            while(recipes.size()>maxRecipes) {
                int last=recipes.size()-1; resolvedRecipes.remove(recipes.remove(last));
                if(last<authors.size())authors.remove(last);
            }
            current=-1;currentRecipe=null;suction=null;
            host.changed();
        }
    }
    public boolean toggle(CrucibleRecipe recipe,String author,Host host) {
        refreshLimit(host);
        if (chaotic()) return false;
        reconcile(host);
        int index=recipes.indexOf(recipe.getId());
        if (index>=0) {
            recipes.remove(index); authors.remove(index); current=-1;
        } else {
            // The old GUI enforced the cap, but its packet handler did not. Reject forged over-cap requests.
            if (recipes.size()>=maxRecipes) return false;
            recipes.add(recipe.getId()); authors.add(author);
            resolvedRecipes.put(recipe.getId(),recipe);
        }
        host.changed(); return true;
    }
    private void reconcile(Host host) {
        if (!reconcile && recipes.stream().anyMatch(id->host.recipe(id).orElse(null)!=resolvedRecipes.get(id))) {
            // A datapack reload can replace the recipe selected by this running machine.
            reconcile=true; current=-1; currentRecipe=null; suction=null;
        }
        if (reconcile) {
            // Saved ids can only be resolved once server recipes are available.
            for (int index=recipes.size()-1;index>=0;index--) if (host.recipe(recipes.get(index)).isEmpty()) {
                recipes.remove(index); if (index<authors.size()) authors.remove(index);
            }
            resolvedRecipes.clear();
            for (var id:recipes) resolvedRecipes.put(id,host.recipe(id).orElseThrow());
            while (authors.size()<recipes.size()) authors.add("");
            while(authors.size()>recipes.size())authors.remove(authors.size()-1); reconcile=false;
            host.changed();
        }
    }
    public void tick(Host host) {
        reconcile(host);
        if (counter==0 || counter%40==0) {
            heat=host.heated(); soulHeat=heat && host.soulHeat(); refreshLimit(host);
        }
        counter++;
        active=heat && !host.powered();
        if (!active) { suction=null; workCredit=0; return; }
        // Four units per tick give one ordinary operation every five ticks.
        workCredit+=(chaotic() ? chaosMatrices+1 : 2)*(soulHeat ? 3 : 2);
        while (workCredit>=20) {
            workCredit-=20;
            if (chaotic()) chaosStep(host);
            else if (!recipes.isEmpty()) rememberedStep(host);
        }
    }
    private void chaosStep(Host host) {
        if (!host.input().isEmpty()) {
            var plan=host.planChaos(stored);
            if (plan.isPresent()) {
                var remainder=plan.get().recipe().remainder(stored);
                if (remainder.isPresent()) {
                    host.consumeInput();
                    stored=remainder.get();
                    host.finishChaos(plan.get());
                    host.changed();
                }
            }
        }
        if (stored.total()<CHAOS_CAPACITY) {
            Aspect drawn=host.drawAny();
            if (drawn!=null) add(drawn,1,host);
        }
    }
    private void rememberedStep(Host host) {
        if (host.input().isEmpty()) { suction=null; return; }
        if (current<0 || current>=recipes.size() || currentRecipe==null || !currentRecipe.catalystMatches(host.input())) {
            for (int index=0;index<recipes.size();index++) {
                var candidate=host.recipe(recipes.get(index));
                if (candidate.isEmpty() || !candidate.get().catalystMatches(host.input())) continue;
                current=index; currentRecipe=candidate.get(); break;
            }
        }
        if (current<0 || current>=recipes.size() || currentRecipe==null) return;
        var outputRecipe=host.recipe(recipes.get(current));
        if (outputRecipe.isEmpty()) { current=-1; currentRecipe=null; suction=null; return; }
        if (!host.outputFits(outputRecipe.get())) return;
        suction=null; var demand=outputRecipe.get().costs();
        for (var aspect:demand.aspects().stream().sorted(java.util.Comparator.comparing(Aspect::tag)).toList()) {
            if (stored.amount(aspect)<demand.amount(aspect)) { suction=aspect; break; }
        }
        if (suction!=null) {
            int drawn=host.draw(suction); if (drawn>0) add(suction,drawn,host);
        } else if (currentRecipe.catalystMatches(host.input()) && currentRecipe.remainder(stored).isPresent()) {
            host.consumeInput(); stored=new AspectAmounts();
            host.complete(outputRecipe.get(),authors.get(current)); current=-1; host.changed();
        }
    }
    /** Amount left over, matching IAspectContainer. No current recipe must be safe for unsolicited pipe pushes. */
    public int add(Aspect aspect,int amount,Host host) {
        if (aspect==null || amount<=0) return amount;
        if (chaotic() && !active || !chaotic() && currentRecipe==null) return amount;
        int needed=chaotic() ? (int)Math.max(0,CHAOS_CAPACITY-stored.total()) : currentRecipe.costs().amount(aspect)-stored.amount(aspect);
        if (needed<=0) return amount;
        int accepted=Math.min(needed,amount); stored.add(aspect,accepted); host.changed(); return amount-accepted;
    }
    public boolean take(Aspect aspect,int amount,Host host) {
        if (aspect==null || amount<0 || stored.amount(aspect)<amount) return false;
        stored.remove(aspect,amount); host.changed(); return true;
    }
    public void save(CompoundTag tag) {
        AspectNbt.write(tag,stored); tag.putByte("maxrec",(byte)maxRecipes); tag.putByte("ChaosMatrices",(byte)chaosMatrices);
        var selected=new ListTag(); for (var id:recipes) selected.add(StringTag.valueOf(id.toString())); tag.put("SelectedRecipes",selected);
        var owners=new ListTag(); for (String author:authors) owners.add(StringTag.valueOf(author)); tag.put("OutputPlayer",owners);
    }
    public void load(CompoundTag tag) {
        stored=AspectNbt.read(tag,TCAspects.catalog()).amounts(); chaosMatrices=Math.max(0,Math.min(9,tag.getByte("ChaosMatrices")));
        maxRecipes=chaotic()?0:Math.max(1,Math.min(19,tag.getByte("maxrec")));
        recipes.clear(); authors.clear(); resolvedRecipes.clear();
        var selected=tag.getList("SelectedRecipes",Tag.TAG_STRING); var owners=tag.getList("OutputPlayer",Tag.TAG_STRING);
        for (int index=0;index<Math.min(maxRecipes,selected.size());index++) {
            var id=ResourceLocation.tryParse(selected.getString(index)); if (id==null) continue;
            recipes.add(id); authors.add(index<owners.size() ? owners.getString(index) : "");
        }
        // The old loader ignores trailing author entries after a recipe-limit reduction.
        current=-1; currentRecipe=null; suction=null; counter=0; workCredit=0; heat=false; active=false; reconcile=true;
    }
}
