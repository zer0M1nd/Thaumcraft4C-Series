package tc4revival.data;

/** Cross-definition checks after all startup registries exist; reject broken addons before gameplay. */
public  class AddonDefinitionValidation {
    public static void setup(net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event) {
        event.enqueueWork(()->{
            var aspects=tc4revival.aspect.TCAspects.catalog();
            var items=new java.util.HashSet<String>();
            for(var rod:tc4revival.content.WandParts.Rod.values())validateItem(rod.item,items);
            for(var cap:tc4revival.content.WandParts.Cap.values())validateItem(cap.item,items);
            for(var rod:tc4revival.content.WandParts.Rod.values())if(rod.regenerates!=null&&!aspects.require(rod.regenerates).isPrimal())
                throw new IllegalArgumentException("Rod regeneration requires a primal aspect: "+rod.tag);
            for(var upgrade:tc4revival.content.FocusUpgrade.values())upgrade.aspects.forEach(aspects::require);
            // Force named registry validation even in a dedicated server with no focus items touched yet.
            tc4revival.network.AddonDefinitionsPacket.fingerprintNow();
        });
    }
    private static void validateItem(String item,java.util.Set<String> used) {
        if(item==null)return;
        if(!net.minecraftforge.registries.ForgeRegistries.ITEMS.containsKey(net.minecraft.resources.ResourceLocation.parse(item)) || item.equals("minecraft:air") || !used.add(item))
            throw new IllegalArgumentException("Unknown or ambiguous wand ingredient "+item);
    }
}
