package tc4revival.content;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.*;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.*;

/** Modern block loot hook for the old elemental pick and primal crusher HarvestDropsEvent. */
public  class ElementalMiningLoot extends LootModifier {
    public static final Codec<ElementalMiningLoot> CODEC=RecordCodecBuilder.create(instance->codecStart(instance).apply(instance,ElementalMiningLoot::new));
    private static final DeferredRegister<Codec<? extends IGlobalLootModifier>> SERIALIZERS=DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS,"thaumcraft");
    static { SERIALIZERS.register("elemental_mining",()->CODEC); }
    public static void register(IEventBus bus) { SERIALIZERS.register(bus); }
    private ElementalMiningLoot(LootItemCondition[] conditions) { super(conditions); }
    @Override protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot,LootContext context) {
        var tool=context.getParamOrNull(LootContextParams.TOOL); var state=context.getParamOrNull(LootContextParams.BLOCK_STATE);
        if (state==null || tool==null || !(tool.getItem() instanceof ElementalPickaxeItem || tool.getItem() instanceof PrimalCrusherItem)
                || !(context.getParamOrNull(LootContextParams.THIS_ENTITY) instanceof Player)) return loot;
        if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH,tool)>0) return loot;
        int fortune=EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BLOCK_FORTUNE,tool);
        for (int i=0;i<loot.size();i++) {
            var before=loot.get(i); var after=SpecialMining.apply(before,fortune,context.getRandom()); loot.set(i,after);
            if (!before.is(after.getItem())) {
                var pos=context.getParamOrNull(LootContextParams.ORIGIN);
                if (pos!=null) context.getLevel().playSound(null,pos.x,pos.y,pos.z,net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP,net.minecraft.sounds.SoundSource.BLOCKS,.2f,.7f+context.getRandom().nextFloat()*.2f);
            }
        }
        return loot;
    }
    @Override public Codec<? extends IGlobalLootModifier> codec() { return CODEC; }
}
