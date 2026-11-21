package fmmodern.compat;

import fmmodern.Content;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.resources.ResourceLocation;

/** Loaded only by the integration-enabled server batch. */
public final class BotanyServerChecks {
    public static void euclidaisy(GameTestHelper test){
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(3,2,3));
        var block=ForgeRegistries.BLOCKS.getValue(ResourceLocation.parse("forbidden_magic:euclidaisy"));
        level.setBlock(pos.below(),Blocks.DIRT.defaultBlockState(),3);level.setBlock(pos,block.defaultBlockState(),3);
        var flower=(Botany.Functional)level.getBlockEntity(pos);flower.addMana(9000);
        for(int i=0;i<400;i++)flower.tickFlower();
        var drops=level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(1),e->e.getItem().is(tc4revival.content.TCItems.WISP_ESSENCE.get()));
        test.assertTrue(drops.size()==1,"One full mana buffer produces one essence");
        var aspects=tc4revival.content.TCItems.WISP_ESSENCE.get().storedAspects(drops.get(0).getItem());
        test.assertTrue(aspects.view().values().stream().mapToInt(Integer::intValue).sum()==2,"Essence contains two aspect units");
        test.assertTrue(flower.getMana()==0,"Essence production spends exactly 9000 mana");
        for(int i=0;i<400;i++)flower.tickFlower();
        test.assertTrue(level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(1),e->e.getItem().is(tc4revival.content.TCItems.WISP_ESSENCE.get())).size()==1,"Empty flower produces nothing");
        drops.forEach(ItemEntity::discard);level.removeBlock(pos,false);test.succeed();
    }
    public static void cosmetics(GameTestHelper test){
        var stack=new ItemStack(Content.COLLAR.get());var cosmetic=new ItemStack(ForgeRegistries.ITEMS.getValue(ResourceLocation.parse("botania:cosmetic_black_bowtie")));
        test.assertTrue(!cosmetic.isEmpty(),"Selected Botania cosmetic exists");
        var attachable=(vazkii.botania.api.item.CosmeticAttachable)stack.getItem();
        stack.getOrCreateTag().putString("ownerName","retained");attachable.setCosmeticItem(stack,cosmetic);
        var copy=ItemStack.of(stack.save(new CompoundTag()));
        test.assertTrue(attachable.getCosmeticItem(copy).is(cosmetic.getItem()),"Cosmetic survives item serialization");
        attachable.setCosmeticItem(copy,ItemStack.EMPTY);
        test.assertTrue(attachable.getCosmeticItem(copy).isEmpty()&&copy.getTag().getString("ownerName").equals("retained"),"Removal preserves collar data");
        test.assertTrue(!attachable.getCosmeticItem(stack).isEmpty(),"Editing copy does not mutate original");test.succeed();
    }
    public static void bloodthorn(GameTestHelper test){
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(3,2,3));
        var block=ForgeRegistries.BLOCKS.getValue(ResourceLocation.parse("forbidden_magic:bloodthorn"));
        level.setBlock(pos.below(),Blocks.DIRT.defaultBlockState(),3);level.setBlock(pos,block.defaultBlockState(),3);
        var flower=(Botany.Functional)level.getBlockEntity(pos);flower.addMana(40);
        var pig=net.minecraft.world.entity.EntityType.PIG.create(level);pig.moveTo(pos.getX()+1.5,pos.getY(),pos.getZ()+.5,0,0);pig.setNoAi(true);level.addFreshEntity(pig);
        flower.tickFlower();
        var weakness=pig.getEffect(net.minecraft.world.effect.MobEffects.WEAKNESS);
        test.assertTrue(weakness!=null&&weakness.getAmplifier()==2&&flower.getMana()==0,"Bloodthorn spends 40 mana to apply Weakness III");
        flower.addMana(40);flower.tickFlower();test.assertTrue(flower.getMana()==40,"Existing weakness is not charged twice");
        pig.discard();level.removeBlock(pos,false);test.succeed();
    }
    public static void tainthistle(GameTestHelper test){
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(3,2,3));
        var block=ForgeRegistries.BLOCKS.getValue(ResourceLocation.parse("forbidden_magic:tainthistle"));
        var goo=ForgeRegistries.BLOCKS.getValue(ResourceLocation.parse("thaumcraft:flux_goo"));
        level.setBlock(pos.below(),Blocks.DIRT.defaultBlockState(),3);level.setBlock(pos,block.defaultBlockState(),3);
        var flower=(Botany.Tainthistle)level.getBlockEntity(pos);var flux=pos.east(2);
        level.setBlock(flux,goo.defaultBlockState().setValue(tc4revival.content.FiniteFluidBlock.QUANTA,4),3);
        for(int i=0;i<80;i++)flower.tickFlower();
        test.assertTrue(level.getBlockState(flux).isAir(),"Tainthistle removes nearby flux");
        test.assertTrue(flower.getMana()>0,"Consumed flux generates mana");
        var saved=new CompoundTag();flower.writeToPacketNBT(saved);
        test.assertTrue(saved.getInt("burnTime")>0,"Unspent flux energy is persisted");
        level.removeBlock(pos,false);test.succeed();
    }
}
