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
        boolean outputFits(CrucibleRecipe recipe);
        int draw(Aspect aspect);
        void complete(CrucibleRecipe recipe,String author);
        void changed();
    }
    private final ArrayList<ResourceLocation> recipes=new ArrayList<>();
    private final ArrayList<AspectAmounts> recipeEssentia=new ArrayList<>();
    private final ArrayList<String> authors=new ArrayList<>();
    private final java.util.Map<ResourceLocation,CrucibleRecipe> resolvedRecipes=new java.util.HashMap<>();
    private AspectAmounts stored=new AspectAmounts();
    private int current=-1,maxRecipes=1,counter;
    private boolean heat,reconcile;
    private CrucibleRecipe currentRecipe;
    private Aspect suction;
    public List<ResourceLocation> selected() { return List.copyOf(recipes); }
    public AspectAmounts stored() { return stored.copy(); }
    public int limit() { return maxRecipes; }
    public int current() { return current; }
    public Aspect suction() { return suction; }
    public int suctionAmount() { return suction==null ? 0 : 128; }
    public void refreshLimit(Host host) {
        int limit=Math.max(1,Math.min(19,host.recipeLimit()));
        if (maxRecipes!=limit) {
            maxRecipes=limit;
            while(recipes.size()>maxRecipes) {
                int last=recipes.size()-1; resolvedRecipes.remove(recipes.remove(last));
                if(last<recipeEssentia.size())recipeEssentia.remove(last);
                if(last<authors.size())authors.remove(last);
            }
            current=-1;currentRecipe=null;suction=null;
            host.changed();
        }
    }
    public boolean toggle(CrucibleRecipe recipe,String author,Host host) {
        reconcile(host);
        int index=recipes.indexOf(recipe.getId());
        if (index>=0) {
            recipes.remove(index); recipeEssentia.remove(index); authors.remove(index); current=-1;
        } else {
            // The old GUI enforced the cap, but its packet handler did not. Reject forged over-cap requests.
            if (recipes.size()>=maxRecipes) return false;
            recipes.add(recipe.getId()); recipeEssentia.add(recipe.costs()); authors.add(author);
            resolvedRecipes.put(recipe.getId(),recipe);
        }
        host.changed(); return true;
    }
    private void reconcile(Host host) {
        if (!reconcile && recipes.stream().anyMatch(id->host.recipe(id).orElse(null)!=resolvedRecipes.get(id))) {
            // Modern datapacks can replace recipes while the machine is running. Never combine
            // an old cached cost with the replacement recipe's output.
            reconcile=true; current=-1; currentRecipe=null; suction=null;
        }
        if (reconcile) {
            // Recipe ids are loaded before server recipes exist; reconstruct costs on the first server tick.
            for (int index=recipes.size()-1;index>=0;index--) if (host.recipe(recipes.get(index)).isEmpty()) {
                recipes.remove(index); if (index<authors.size()) authors.remove(index);
            }
            recipeEssentia.clear();
            resolvedRecipes.clear();
            for (var id:recipes) {
                var recipe=host.recipe(id).orElseThrow();
                recipeEssentia.add(recipe.costs()); resolvedRecipes.put(id,recipe);
            }
            while (authors.size()<recipes.size()) authors.add("");
            while(authors.size()>recipes.size())authors.remove(authors.size()-1); reconcile=false;
            host.changed();
        }
    }
    public void tick(Host host) {
        reconcile(host);
        if (counter==0 || counter%40==0) {
            heat=host.heated(); refreshLimit(host);
        }
        counter++;
        if (!heat || host.powered() || counter%5!=0 || recipes.isEmpty()) return;
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
        suction=null; var demand=recipeEssentia.get(current);
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
        if (aspect==null || amount<=0 || currentRecipe==null) return amount;
        int needed=currentRecipe.costs().amount(aspect)-stored.amount(aspect);
        if (needed<=0) return amount;
        int accepted=Math.min(needed,amount); stored.add(aspect,accepted); host.changed(); return amount-accepted;
    }
    public boolean take(Aspect aspect,int amount,Host host) {
        if (aspect==null || amount<0 || stored.amount(aspect)<amount) return false;
        stored.remove(aspect,amount); host.changed(); return true;
    }
    public void save(CompoundTag tag) {
        AspectNbt.write(tag,stored); tag.putByte("maxrec",(byte)maxRecipes);
        var selected=new ListTag(); for (var id:recipes) selected.add(StringTag.valueOf(id.toString())); tag.put("SelectedRecipes",selected);
        var owners=new ListTag(); for (String author:authors) owners.add(StringTag.valueOf(author)); tag.put("OutputPlayer",owners);
    }
    public void load(CompoundTag tag) {
        stored=AspectNbt.read(tag,TCAspects.catalog()).amounts(); maxRecipes=Math.max(1,Math.min(19,tag.getByte("maxrec")));
        recipes.clear(); recipeEssentia.clear(); authors.clear(); resolvedRecipes.clear();
        var selected=tag.getList("SelectedRecipes",Tag.TAG_STRING); var owners=tag.getList("OutputPlayer",Tag.TAG_STRING);
        for (int index=0;index<Math.min(19,selected.size());index++) {
            var id=ResourceLocation.tryParse(selected.getString(index)); if (id==null) continue;
            recipes.add(id); authors.add(index<owners.size() ? owners.getString(index) : "");
        }
        // The old loader ignores trailing author entries after a recipe-limit reduction.
        current=-1; currentRecipe=null; suction=null; counter=0; heat=false; reconcile=true;
    }
}
