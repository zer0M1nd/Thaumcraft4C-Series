package tc4revival.content;

import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class ModernizationServerTests {
    @GameTest(template="wand_test_empty")
    public static void everyNativeLootTableLoads(GameTestHelper test) {
        var server = test.getLevel().getServer(); int count = 0;
        for (var resource : server.getResourceManager().listResources("loot_tables", id -> id.getNamespace().equals("thaumcraft") && id.getPath().endsWith(".json")).keySet()) {
            String path = resource.getPath(); var id = ResourceLocation.parse("thaumcraft:" + path.substring("loot_tables/".length(), path.length()-5));
            test.assertTrue(server.getLootData().getLootTable(id) != LootTable.EMPTY,"Native loot table parsed and loaded: " + id); count++;
        }
        test.assertTrue(count > 70,"Full native loot inventory was checked"); test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void focusProjectilesRespectCastingHand(GameTestHelper test) {
        for (var dominant : net.minecraft.world.entity.HumanoidArm.values()) {
            var player = new net.minecraftforge.common.util.FakePlayer(test.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"LaunchHand")) {
                @Override public net.minecraft.world.entity.HumanoidArm getMainArm() { return dominant; }
            };
            player.setPos(10, 80, 10);
            for (int yaw : new int[]{0,90,180,270}) for (var hand : net.minecraft.world.InteractionHand.values()) {
                player.setYRot(yaw);
                var shots = java.util.List.of(new EmberEntity(test.getLevel(),player,0,1,0,20),
                        new ExplosiveOrbEntity(test.getLevel(),player,1,false), new FrostShardEntity(test.getLevel(),player,1,3,0,false,false),
                        new ShockOrbEntity(test.getLevel(),player,4,5),new PrimalOrbEntity(test.getLevel(),player,false),new PechBlastEntity(test.getLevel(),player,0,0,false));
                int side = (hand == net.minecraft.world.InteractionHand.MAIN_HAND ? dominant : dominant.getOpposite()) == net.minecraft.world.entity.HumanoidArm.RIGHT ? 1 : -1;
                for (var shot : shots) {
                    var velocity = shot.getDeltaMovement(); FocusProjectiles.fromHand(shot,player,hand);
                    double radians = Math.toRadians(yaw);
                    test.assertTrue(Math.abs(shot.getX()-(10-Math.cos(radians)*.16*side))<.00001 && Math.abs(shot.getZ()-(10-Math.sin(radians)*.16*side))<.00001,"All six projectile families launch from the selected arm");
                    test.assertTrue(velocity.equals(shot.getDeltaMovement()),"Hand mirroring preserves spread and speed");
                }
            }
        }
        test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void modernEquipmentAttributesAndRepairTags(GameTestHelper test) {
        var goggles=new ItemStack(TCItems.GOGGLES.get());
        var modifiers=goggles.getAttributeModifiers(EquipmentSlot.HEAD).get(TCAttributes.VIS_COST.get());
        test.assertTrue(modifiers.stream().anyMatch(m->m.getAmount()<0),"Goggles expose real beneficial cost attribute");
        test.assertTrue(goggles.getAttributeModifiers(EquipmentSlot.OFFHAND).get(TCAttributes.VIS_COST.get()).isEmpty(),"Held armor does not apply armor discount");
        var sword=new ItemStack(TCItems.tool(ThaumiumToolType.SWORD));
        test.assertTrue(sword.is(LegacyRepairEnchantment.REPAIRABLE),"Repair eligibility is tagged");
        test.assertTrue(sword.getUseAnimation()==UseAnim.NONE&&sword.getUseDuration()==0,"Ordinary swords no longer block");
        test.assertTrue(tc4revival.aspect.TCAspects.catalog().all().size()==tc4revival.aspect.AspectDefinitions.bundled().all().size(),"Every JSON aspect is registered");
        test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void nativeBagTablesAndStructureRegistry(GameTestHelper test) {
        var level=test.getLevel();var origin=test.absolutePos(new net.minecraft.core.BlockPos(3,3,3)).getCenter();
        var params=new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN,origin).create(LootContextParamSets.CHEST);
        for(var kind:LootBagItem.Kind.values()) {
            var table=level.getServer().getLootData().getLootTable(ResourceLocation.parse("thaumcraft:bags/"+kind.path));
            test.assertTrue(table!=LootTable.EMPTY,"Bag has registered loot table");
            for(int seed=0;seed<16;seed++) {
                var stacks=table.getRandomItems(params,seed+1);
                test.assertTrue(stacks.size()>=8&&stacks.size()<=12,"Native bag returns eight to twelve draws");
            }
        }
        var registry=level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE);
        for(String name:java.util.List.of("sinister_dungeon","hilltop_stones","obsidian_totem","eldritch_obelisk"))
            test.assertTrue(registry.containsKey(ResourceLocation.parse("thaumcraft:"+name)),"Structure is available to locate: "+name);
        test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void headCurioAndBranchArmorAttributes(GameTestHelper test) {
        var player=new net.minecraftforge.common.util.FakePlayer(test.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"HeadCurio"));
        var inventory=top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).resolve().orElseThrow();inventory.reset();
        var head=inventory.getStacksHandler("head").orElseThrow(()->new AssertionError("Install validation head-slot datapack"));
        var goggles=new ItemStack(TCItems.GOGGLES.get());head.getStacks().setStackInSlot(0,goggles);player.doTick();
        test.assertTrue(RevealingGear.visible(player,true)&&RevealingGear.visible(player,false),"Functional head slot reveals nodes and information");
        test.assertTrue(player.getAttributeValue(TCAttributes.VIS_COST.get())==1,"Head curio gives no vis discount");
        head.getStacks().setStackInSlot(0,ItemStack.EMPTY);head.getCosmeticStacks().setStackInSlot(0,goggles);player.doTick();
        test.assertTrue(!RevealingGear.visible(player,true),"Cosmetic slot does not grant functional sight");
        boolean enabled=tc4revival.config.TCConfig.SERVER.branchFeatures.get();
        try {
            for(var item:net.minecraftforge.registries.ForgeRegistries.ITEMS) {
                double expected=item instanceof FortressArmorItem?3:item instanceof VoidArmorItem||item instanceof VoidRobeArmorItem?2:item instanceof CultistArmorItem cultist&&cultist.definition().style==CultistEquipment.Style.LEADER?1:0;
                if(expected==0)continue;
                var stack=new ItemStack(item);var slot=((ArmorItem)item).getEquipmentSlot();
                tc4revival.config.TCConfig.SERVER.branchFeatures.set(false);
                double old=stack.getAttributeModifiers(slot).get(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS).stream().mapToDouble(m->m.getAmount()).sum();
                tc4revival.config.TCConfig.SERVER.branchFeatures.set(true);
                double modern=stack.getAttributeModifiers(slot).get(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS).stream().mapToDouble(m->m.getAmount()).sum();
                test.assertTrue(Math.abs(modern-old-expected)<.000001,"Branch toughness increment for "+item);
            }
            head.getStacks().setStackInSlot(0,goggles);tc4revival.config.TCConfig.SERVER.branchFeatures.set(false);
            test.assertTrue(!RevealingGear.visible(player,true),"Global off switch disables already equipped curio sight");
        } finally {tc4revival.config.TCConfig.SERVER.branchFeatures.set(enabled);}
        test.succeed();
    }
}
