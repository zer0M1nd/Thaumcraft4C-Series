package fmmodern;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.*;
import net.minecraftforge.registries.*;

public final class MiningLoot extends LootModifier {
    public static final Codec<MiningLoot> CODEC=RecordCodecBuilder.create(i->codecStart(i).apply(i,MiningLoot::new));
    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> REGISTER=DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS,ForbiddenMagic.ID);
    static {REGISTER.register("mining",()->CODEC);}
    private MiningLoot(LootItemCondition[] conditions){super(conditions);}
    @Override public Codec<? extends IGlobalLootModifier> codec(){return CODEC;}
    @Override protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot,LootContext context){
        var stack=context.getParamOrNull(LootContextParams.TOOL);var state=context.getParamOrNull(LootContextParams.BLOCK_STATE);
        if(stack==null||state==null||!(context.getParamOrNull(LootContextParams.THIS_ENTITY) instanceof net.minecraft.world.entity.player.Player))return loot;
        int fortune=EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BLOCK_FORTUNE,stack),silk=EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH,stack);var random=context.getRandom();
        if(stack.is(Content.TAINT_SPADE.get())&&TaintTools.isTaint(state)&&loot.isEmpty()&&!state.is(net.minecraft.tags.BlockTags.LEAVES)&&random.nextInt(31)<=1+2*fortune+(silk>0?6:0))loot.add(new ItemStack(Content.item("taint_shard")));
        int cluster=DarkEnchantments.level("cluster",stack);
        if(cluster>0&&fortune==0)for(int i=0;i<loot.size();i++){
            var drop=loot.get(i); if(drop.is(Items.RAW_IRON))drop=new ItemStack(Items.IRON_ORE,drop.getCount());else if(drop.is(Items.RAW_GOLD))drop=new ItemStack(Items.GOLD_ORE,drop.getCount());
            var result=tc4revival.content.SpecialMining.apply(drop,cluster,random);
            // Preserve modern raw-metal drops if the cluster roll failed.
            if(!result.is(drop.getItem()))loot.set(i,result);
        }
        if(DarkEnchantments.level("consuming",stack)>0)loot.removeIf(ForbiddenConfig::garbage);
        var blockId=ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if(DarkEnchantments.level("corrupting",stack)>0&&blockId!=null&&blockId.getNamespace().equals("thaumcraft")&&blockId.getPath().endsWith("_infused_stone")&&random.nextInt(3)==1){
            var replacement=new ObjectArrayList<ItemStack>();
            for(var drop:loot){var id=ForgeRegistries.ITEMS.getKey(drop.getItem());if(id!=null&&id.getNamespace().equals("thaumcraft")&&id.getPath().endsWith("_shard")){for(int i=0;i<drop.getCount();i++){boolean noLust=fmmodern.ForbiddenConfig.value(ForbiddenConfig.NO_LUST);int choice=SinRules.corruptionSin(random.nextInt(noLust?6:7),noLust);replacement.add(new ItemStack(choice==2?Content.GLUTTONY.get():Content.item(Content.SINS[choice]+"_shard")));}}else replacement.add(drop);}
            loot.clear();loot.addAll(replacement);
        }
        if(context.getLevel().dimension()==Level.NETHER&&state.is(Blocks.NETHER_QUARTZ_ORE)&&fortune>0&&random.nextInt(60)<=fortune)loot.add(new ItemStack(Content.item("greed_shard")));
        return loot;
    }
}
