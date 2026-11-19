package conciliummodern;
import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.item.ItemStack;
/** Explicit host contract; no ordinary potion or renamed item substitutes for Witchery mechanics. */
public final class WitcheryBridge {
 public interface Host {
  String modId();
  /** Original Witchery item identity or GENERIC metadata, resolved to the host's actual item. */
  ItemStack item(String legacyName,int metadata);
  /** Keep ingredient order and repeats, and ask the host's brewing registry to derive effects. */
  ItemStack brew(List<ItemStack> ingredients,Integer color,boolean updateName);
  ThrowableProjectile brewProjectile(ServerLevel level,LivingEntity thrower,ItemStack brew);
  boolean vampire(Player player);
  int werewolfLevel(Player player);
  MobEffect gasMask();
  ResourceLocation broomTexture();
  default ConciliumSpellIntegration.WitcheryHost spells(){return null;}
  default MobEffect colorfulEffect(){return null;}
  /** Optional village-burning-site capabilities supplied by the real host. */
  default net.minecraft.world.level.block.state.BlockState stockade(){return null;}
  default EntityType<? extends Mob> witchHunter(){return null;}
 }
 private static volatile Host installed;
 public static synchronized void register(Host host){Objects.requireNonNull(host);if(!net.minecraftforge.fml.ModList.get().isLoaded(host.modId()))throw new IllegalArgumentException("Witchery host mod is absent: "+host.modId());if(installed==host)return;if(installed!=null)throw new IllegalStateException("Witchery host already installed");var spells=host.spells();if(spells!=null&&!ConciliumSpellIntegration.registerWitchery(spells))throw new IllegalStateException("Witchery spells could not be registered");installed=host;}
 public static Optional<Host> host(){return Optional.ofNullable(installed);}
 public static boolean available(){return installed!=null;}
 public static boolean alchemistAvailable(){return available()&&net.minecraftforge.fml.ModList.get().isLoaded("bloodmagic");}
 public static ItemStack item(String name,int metadata){return host().map(h->h.item(name,metadata)).orElse(ItemStack.EMPTY);}
 public static ItemStack brew(String name){if(!available())return ItemStack.EMPTY;var recipe=Brews.DATA.getAsJsonArray(name);if(recipe==null)throw new IllegalArgumentException("Unknown original brew "+name);var ingredients=new ArrayList<ItemStack>();for(var row:recipe){var token=row.getAsJsonObject();String id=token.get("item").getAsString();int meta=token.has("metadata")?token.get("metadata").getAsInt():0;ItemStack stack;
 if(id.startsWith("minecraft:")){var item=net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(id));stack=item==null?ItemStack.EMPTY:new ItemStack(item);}else stack=item(id.substring("witchery:".length()),meta);if(stack.isEmpty())throw new IllegalStateException("Host lacks brew ingredient "+id+":"+meta);ingredients.add(stack.copy());}
 return installed.brew(List.copyOf(ingredients),name.equals("moonshine")?0xfec20c:null,name.equals("moonshine"));}
 private static class Brews {static final com.google.gson.JsonObject DATA=load();static com.google.gson.JsonObject load(){try(var in=Objects.requireNonNull(WitcheryBridge.class.getResourceAsStream("/conciliummodern/witchery_brews.json"))){return com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();}catch(java.io.IOException e){throw new IllegalStateException(e);}}}
 private WitcheryBridge(){}
}
