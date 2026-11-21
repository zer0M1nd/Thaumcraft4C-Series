package tc4revival.client;

import java.util.*;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.TC4Revival;

/** Opt-in real-client resource bake regression; custom renderers still need visual inspection. */
@Mod.EventBusSubscriber(modid="thaumcraft",bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public class ClientAssetAudit {
 @SubscribeEvent public static void baked(ModelEvent.BakingCompleted event){
  if(!Boolean.getBoolean("tc4revival.auditClientAssets"))return;
  var namespaces=Set.of("thaumcraft","thaumic_tinkerer","forbidden_magic","thaumicenergistics","thaumichorizons","thaumicbases","automagy");
  var failures=new TreeSet<String>();var custom=new TreeSet<String>();int count=0,quads=0;
  for(var id:ForgeRegistries.ITEMS.getKeys())if(namespaces.contains(id.getNamespace())){
   count++;var model=event.getModels().get(new ModelResourceLocation(id,"inventory"));
   if(model==null||model==event.getModelManager().getMissingModel()){failures.add("Missing model "+id);continue;}
   if(model.isCustomRenderer()){custom.add(id.toString());continue;}
   try{
    var faces=new ArrayList<Direction>(List.of(Direction.values()));faces.add(null);
    for(var face:faces)for(var quad:model.getQuads(null,face,RandomSource.create(42))){
     quads++;if(quad.getSprite().contents().name().equals(MissingTextureAtlasSprite.getLocation()))failures.add("Missing quad texture "+id);
    }
   }catch(Exception ex){failures.add("Model inspection "+id+": "+ex);}
  }
  var result=new LinkedHashMap<String,Object>();result.put("itemModels",count);result.put("quads",quads);result.put("failures",failures);result.put("customRenderersRequireVisualInspection",custom);
  result.put("scope","Actual client bake and default item quads; does not certify animations, overrides, GUI interaction or custom renderers.");
  try{java.nio.file.Files.writeString(FMLPaths.GAMEDIR.get().resolve("tc4-client-asset-audit.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(result));}
  catch(java.io.IOException ex){TC4Revival.LOGGER.error("Unable to write client asset audit",ex);}
  TC4Revival.LOGGER.info("TC4_CLIENT_ASSET_AUDIT items={} quads={} failures={} custom={}",count,quads,failures.size(),custom.size());
  for(String failure:failures)TC4Revival.LOGGER.error("TC4_CLIENT_ASSET_AUDIT {}",failure);
 }
}
