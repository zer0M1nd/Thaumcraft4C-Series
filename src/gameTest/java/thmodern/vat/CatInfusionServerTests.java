package thmodern.vat;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.*;
import net.minecraftforge.gametest.*;
import thmodern.Horizons;
import thmodern.creature.InfusedCat;
@GameTestHolder(Horizons.ID) @PrefixGameTestTemplate(false)
public final class CatInfusionServerTests {
    private static VatInfusionRecipe recipe(String key){return VatInfusionRecipes.all().stream().filter(r->r.id.getPath().equals(key)).findFirst().orElseThrow();}
    @GameTest(template="empty")
    public static void catAndOcelotConvertWithAppearanceAndOwnership(GameTestHelper test){
        var cat=EntityType.CAT.create(test.getLevel());var owner=java.util.UUID.randomUUID();cat.setTame(true);cat.setOwnerUUID(owner);cat.setOrderedToSit(true);
        var converted=(InfusedCat)CreatureTransformation.prepare(cat,recipe("guardian_panther"));
        test.assertTrue(recipe("guardian_panther").target(cat,false)&&converted.getMaxHealth()==24&&converted.getOwnerUUID().equals(owner)&&converted.isOrderedToSit(),"Cat owner and command retained with panther base stats");
        var ocelot=EntityType.OCELOT.create(test.getLevel());var wild=(InfusedCat)CreatureTransformation.prepare(ocelot,recipe("guardian_panther"));
        test.assertTrue(recipe("guardian_panther").target(ocelot,false)&&wild.ocelotAppearance()&&wild.getOwnerUUID()==null,"Wild ocelot route retains ocelot appearance without inventing ownership");test.succeed();
    }
    @GameTest(template="empty")
    public static void guardianHealsFromFoodAndDealsSixDamage(GameTestHelper test){
        var cat=Horizons.GUARDIAN_PANTHER.get().create(test.getLevel());cat.setHealth(10);var player=new net.minecraftforge.common.util.FakePlayer(test.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"PantherFood"));player.getAbilities().instabuild=false;player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.BREAD,2));
        cat.mobInteract(player,net.minecraft.world.InteractionHand.MAIN_HAND);test.assertTrue(cat.getHealth()==15&&player.getMainHandItem().getCount()==1,"Any food heals by nutrition and spends one item");var cow=EntityType.COW.create(test.getLevel());cat.doHurtTarget(cow);test.assertTrue(cow.getHealth()==cow.getMaxHealth()-6,"Panther melee deals six damage");test.succeed();
    }
    @GameTest(template="empty")
    public static void gravekeeperIgnitesNearbyUndeadOnly(GameTestHelper test){
        var cat=Horizons.GRAVEKEEPER.get().create(test.getLevel());cat.moveTo(test.absolutePos(new BlockPos(3,3,3)).getCenter());var zombie=EntityType.ZOMBIE.create(test.getLevel());zombie.moveTo(cat.position().add(2,0,0));var cow=EntityType.COW.create(test.getLevel());cow.moveTo(cat.position().add(0,0,2));test.getLevel().addFreshEntity(zombie);test.getLevel().addFreshEntity(cow);cat.aiStep();
        test.assertTrue(zombie.getRemainingFireTicks()>=20&&!cow.isOnFire(),"Aura ignites undead without burning ordinary livestock");float health=zombie.getHealth();cat.doHurtTarget(zombie);test.assertTrue(zombie.getHealth()==health,"Undead melee deliberately relies on aura, as in legacy implementation");zombie.discard();cow.discard();test.succeed();
    }
}
