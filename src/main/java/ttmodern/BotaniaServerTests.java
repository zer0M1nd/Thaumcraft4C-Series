package ttmodern;

import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
import net.minecraft.world.item.ItemStack;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public final class BotaniaServerTests {
    @GameTest(template="wand_test_empty") public static void tinkererBotaniaBurstLensAndOwnerRules(GameTestHelper test) throws Exception {
        if(!net.minecraftforge.fml.ModList.get().isLoaded("botania")){test.succeed();return;}
        var world=test.getLevel();var type=net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getValue(net.minecraft.resources.ResourceLocation.parse("botania:mana_burst"));test.assertTrue(type!=null,"Installed Botania provides the expected burst entity");var projectile=(net.minecraft.world.entity.projectile.Projectile)type.create(world);
        var player=net.minecraftforge.common.util.FakePlayerFactory.get(world,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"BotaniaCheck"));var api=Class.forName("vazkii.botania.api.internal.ManaBurst");api.getMethod("setFake",boolean.class).invoke(projectile,false);api.getMethod("setSourceLens",ItemStack.class).invoke(projectile,ItemStack.EMPTY);
        test.assertTrue(BotaniaDeflection.harmless(projectile,player),"Normal transmission bursts must not be moved");var items=Class.forName("vazkii.botania.common.item.BotaniaItems");var damaging=new ItemStack((net.minecraft.world.item.Item)items.getField("lensDamage").get(null));api.getMethod("setSourceLens",ItemStack.class).invoke(projectile,damaging);test.assertTrue(!BotaniaDeflection.harmless(projectile,player),"Damage lens is eligible for deflection");
        api.getMethod("setFake",boolean.class).invoke(projectile,true);test.assertTrue(BotaniaDeflection.harmless(projectile,player),"Simulation bursts are never moved");api.getMethod("setFake",boolean.class).invoke(projectile,false);api.getMethod("setShooterUUID",java.util.UUID.class).invoke(projectile,player.getUUID());test.assertTrue(BotaniaDeflection.harmless(projectile,player),"Own shooter UUID exempts even a damage burst");projectile.discard();test.succeed();
    }
}
