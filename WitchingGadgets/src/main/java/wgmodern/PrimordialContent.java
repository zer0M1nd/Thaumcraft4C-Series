package wgmodern;

import java.util.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.common.extensions.IForgeMenuType;

/** The enabled primordial branch of the author's 1.1.10 release. */
public class PrimordialContent {
 public static final RegistryObject<Item> EMPTY_PEARL=Content.ITEMS.register("powerless_pearl",()->new Content.NamedItem("item.WG_Material.powerlessPearl",new Item.Properties()));
 public static final RegistryObject<Block> VOID_BLOCK=Content.BLOCKS.register("void_metal_block",()->new Block(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(4,9).noOcclusion().isValidSpawn((state,level,pos,type)->false)));
 public static final RegistryObject<Item> VOID_BLOCK_ITEM=Content.ITEMS.register("void_metal_block",()->new BlockItem(VOID_BLOCK.get(),new Item.Properties()){@Override public String getDescriptionId(){return "tile.WG_MetalDevice.voidmetalBlock";}});
 public static final RegistryObject<Item> GLOVE=Content.ITEMS.register("primordial_glove",PrimordialGlove::new);
 public static final Map<String,RegistryObject<Item>> WEAPONS=new LinkedHashMap<>(),ARMOR=new LinkedHashMap<>();
 static {for(String kind:List.of("sword","hammer","axe"))WEAPONS.put(kind,Content.ITEMS.register("primordial_"+kind,()->new PrimordialGear.Weapon(kind)));for(var type:ArmorItem.Type.values()){String kind=switch(type){case HELMET->"helmet";case CHESTPLATE->"chestplate";case LEGGINGS->"leggings";case BOOTS->"boots";};ARMOR.put(kind,Content.ITEMS.register("primordial_"+kind,()->new PrimordialGear.Armor(type)));}}
 public static final RegistryObject<MenuType<PrimordialMenu>> MENU=Content.MENUS.register("primordial_glove",()->IForgeMenuType.create((id,inv,buf)->new PrimordialMenu(id,inv,buf.readBoolean())));
 public static void register(){}
 public static int returnedPearls(ItemStack stack){return stack.getItem() instanceof PrimordialGlove?1:stack.getItem() instanceof PrimordialGear.Weapon?2:stack.getItem() instanceof PrimordialGear.Armor?3:0;}
}
