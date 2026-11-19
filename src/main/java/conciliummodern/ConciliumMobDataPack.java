package conciliummodern;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.*;
import net.minecraft.server.packs.repository.*;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
/** Optional vat recipes are absent when ingredients or their host feature are unavailable. */
@Mod.EventBusSubscriber(modid=ThaumicConcilium.ID,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class ConciliumMobDataPack extends AbstractPackResources {
 private final Map<ResourceLocation,byte[]> files=new LinkedHashMap<>();
 public ConciliumMobDataPack(String id){super(id,true);if(RemainingMobs.available("dope_squid"))try(var stream=Objects.requireNonNull(getClass().getResourceAsStream("/conciliummodern/optional/dope_squid.json"))){files.put(ResourceLocation.parse("thaumicconcilium:vat_infusions/dope_squid.json"),stream.readAllBytes());}catch(IOException e){throw new IllegalStateException(e);}}
 @SubscribeEvent public static void packs(AddPackFindersEvent e){if(e.getPackType()!=PackType.SERVER_DATA)return;String id="thaumicconcilium_optional_mobs";Pack.ResourcesSupplier supplier=ConciliumMobDataPack::new;var pack=Pack.create(id,Component.literal("Thaumic Concilium optional creatures"),true,supplier,Objects.requireNonNull(Pack.readPackInfo(id,supplier)),PackType.SERVER_DATA,Pack.Position.BOTTOM,true,PackSource.BUILT_IN);e.addRepositorySource(out->out.accept(pack));}
 @Override public IoSupplier<InputStream> getRootResource(String... path){return path.length==1&&path[0].equals("pack.mcmeta")?()->new ByteArrayInputStream("{\"pack\":{\"pack_format\":15,\"description\":\"Thaumic Concilium optional creatures\"}}".getBytes(StandardCharsets.UTF_8)):null;}
 @Override public IoSupplier<InputStream> getResource(PackType type,ResourceLocation location){byte[] data=type==PackType.SERVER_DATA?files.get(location):null;return data==null?null:()->new ByteArrayInputStream(data);}
 @Override public void listResources(PackType type,String namespace,String prefix,ResourceOutput out){if(type==PackType.SERVER_DATA)files.forEach((key,data)->{if(key.getNamespace().equals(namespace)&&(prefix.isEmpty()||key.getPath().startsWith(prefix+"/")))out.accept(key,()->new ByteArrayInputStream(data));});}
 @Override public Set<String> getNamespaces(PackType type){return type==PackType.SERVER_DATA?Set.of(ThaumicConcilium.ID):Set.of();}
 @Override public void close(){}
}
