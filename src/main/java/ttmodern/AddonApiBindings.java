package ttmodern;

import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import tc4revival.research.*;
import thaumcraft.api.research.ResearchPrerequisites;
import thaumcraft.api.wands.FocusStorageRegistry;

/** Common-side bindings; the base synchronizes materialized research to clients. */
public final class AddonApiBindings {
    public static void setup(){
        for(String aspect:List.of("AER","IGNIS","AQUA","TERRA","ORDO","PERDITIO"))
            ResearchPrerequisites.conditional(id("crop_"+aspect.toLowerCase(Locale.ROOT)),Tinkerer.ID+":INFUSED_POTIONS",Tinkerer.ID+":FIRE_"+aspect,()->TinkererConfig.FIRE.get());
        ResearchPrerequisites.register(id("kami"),(entry,catalog,current)->{
            if(!entry.key().equals(Tinkerer.ID+":ICHOR"))return current;
            // Only the original authors' fixed research IDs gate entry to KAMI.
            // Missing optional metals/removed entries and disabled features cannot make Ichor impossible.
            var hidden=new LinkedHashSet<>(current.hidden());
            for(String key:LegacyIchorPrerequisites.KEYS)
                catalog.find(key).filter(ResearchCatalogs::configuredEnabled).ifPresent(candidate->hidden.add(key));
            return new ResearchPrerequisites.Parents(current.visible(),List.copyOf(hidden));
        });
        FocusStorageRegistry.register(id("ichor_pouch"),stack->stack.getItem() instanceof IchorPouchItem?new FocusStorageRegistry.Storage(){
            public int slots(){return IchorPouchItem.SIZE;}
            public ItemStack get(int slot){return IchorPouchItem.contents(stack).get(slot).copy();}
            public boolean accepts(int slot,ItemStack value){return FocusStorageRegistry.Storage.super.accepts(slot,value)&&(value.isEmpty()||IchorPouchItem.accepts(value));}
            public void set(int slot,ItemStack value){
                if(!accepts(slot,value))throw new IllegalArgumentException("Invalid pouch focus");
                var contents=IchorPouchItem.contents(stack);contents.set(slot,value.copy());IchorPouchItem.save(stack,contents);
            }
        }:null);
    }
    private static ResourceLocation id(String key){return ResourceLocation.fromNamespaceAndPath(Tinkerer.ID,key);}
    private AddonApiBindings(){}
}
