package automagy.content;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.*;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.*;
public final class NetherHeartLootModifier extends LootModifier {
 public static final Codec<NetherHeartLootModifier> CODEC=RecordCodecBuilder.create(instance->codecStart(instance).apply(instance,NetherHeartLootModifier::new));
 public NetherHeartLootModifier(LootItemCondition[] conditions){super(conditions);}
 @Override protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> drops,LootContext context){
  if(!automagy.AutomagyConfig.HEART_DROPS.get()||automagy.AutomagyConfig.HEART_NETHER_ONLY.get()&&!context.getLevel().dimension().equals(net.minecraft.world.level.Level.NETHER))return drops;
  var entity=context.getParamOrNull(LootContextParams.THIS_ENTITY);if(!(entity instanceof net.minecraft.world.entity.player.Player player)||player.getAbilities().instabuild)return drops;
  var tool=context.getParamOrNull(LootContextParams.TOOL);if(tool!=null&&net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH,tool)>0)return drops;
  if(context.getParamOrNull(LootContextParams.BLOCK_ENTITY) instanceof net.minecraft.world.level.block.entity.SpawnerBlockEntity spawner){var tag=spawner.getSpawner().save(new net.minecraft.nbt.CompoundTag());if(tag.getCompound("SpawnData").getCompound("entity").getString("id").equals("minecraft:blaze"))drops.add(new ItemStack(Content.NETHER_HEART.get()));}return drops;
 }
 @Override public Codec<? extends IGlobalLootModifier> codec(){return CODEC;}
}
