package tc4revival.content;

import com.mojang.authlib.GameProfile;
import java.util.Map;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import top.theillusivec4.curios.api.CuriosApi;
import tc4revival.TC4Revival;
import tc4revival.aspect.TCAspects;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public  class VisDiscountServerTests {
    @GameTest(template="wand_test_empty")
    public static void allElementalRingPairsArmorAndVisExhaustionProduceLegacyPayments(GameTestHelper test) {
        var player=new FakePlayer(test.getLevel(),new GameProfile(UUID.randomUUID(),"DiscountAudit"));
        var inventory=CuriosApi.getCuriosInventory(player).resolve().orElseThrow();inventory.reset();
        var rings=inventory.getStacksHandler("ring").orElseThrow().getStacks();
        test.assertTrue(rings.getSlots()>=2,"Loaded slots include the two legacy ring positions");
        // Other mods may add slots (Botania adds one). TC4 discounts still use only
        // the original two positions, even when an extra position is occupied.
        if(rings.getSlots()>2)rings.setStackInSlot(2,new ItemStack(TCItems.elementalRing(ElementalRingType.AIR)));
        player.setItemSlot(EquipmentSlot.HEAD,new ItemStack(TCItems.GOGGLES.get()));
        player.setItemSlot(EquipmentSlot.CHEST,new ItemStack(TCItems.HOVER_HARNESS.get()));
        player.setItemSlot(EquipmentSlot.LEGS,new ItemStack(TCItems.ROBE_LEGS.get()));
        player.setItemSlot(EquipmentSlot.FEET,new ItemStack(TCItems.ROBE_BOOTS.get()));
        int cases=0;
        for(var first:ElementalRingType.values())for(var second:ElementalRingType.values()) {
            rings.setStackInSlot(0,new ItemStack(TCItems.elementalRing(first)));
            rings.setStackInSlot(1,new ItemStack(TCItems.elementalRing(second)));
            test.assertTrue(CuriosApi.getItemStackSlots(rings.getStackInSlot(0),player).containsKey("ring"),"Actual ring has the loaded slot tag");
            for(int disease=0;disease<4;disease++) {
                player.removeEffect(TCEffects.VIS_EXHAUST.get());player.removeEffect(TCEffects.INFECTIOUS_VIS_EXHAUST.get());
                if(disease==1)player.addEffect(new MobEffectInstance(TCEffects.VIS_EXHAUST.get(),200,0));
                if(disease==2)player.addEffect(new MobEffectInstance(TCEffects.INFECTIOUS_VIS_EXHAUST.get(),200,1));
                if(disease==3) {
                    player.addEffect(new MobEffectInstance(TCEffects.VIS_EXHAUST.get(),200,3));
                    player.addEffect(new MobEffectInstance(TCEffects.INFECTIOUS_VIS_EXHAUST.get(),200,1));
                }
                player.doTick();
                int penalty=switch(disease){case 1->10;case 2->20;case 3->40;default->0;};
                for(var aspect:TCAspects.catalog().primals()) {
                    int discount=(aspect.tag().equals("aer")?13:10)+(first.aspectTag().equals(aspect.tag())?1:0)+(second.aspectTag().equals(aspect.tag())?1:0)-penalty;
                    test.assertTrue(Math.abs(WandInventory.discount(player,aspect)-discount/100f)<.000001,"Legacy gear sum and maximum disease amplifier, not sum of both diseases");
                    var wand=TCItems.CASTING_WAND.get().create(WandParts.Rod.WOOD,WandParts.Cap.GOLD,false,true);
                    int before=TCItems.CASTING_WAND.get().vis(wand,aspect);
                    int expected=(int)(137*(1-discount/100f));
                    test.assertTrue(TCItems.CASTING_WAND.get().consumeForPlayer(wand,player,Map.of(aspect,137),false,true,true),"Actual crafting payment succeeds");
                    test.assertTrue(TCItems.CASTING_WAND.get().vis(wand,aspect)==before-expected,"Actual wand debit retains legacy float truncation for "+first+"/"+second+"/"+aspect.tag()+"/"+disease);
                    cases++;
                }
            }
        }
        player.removeEffect(TCEffects.VIS_EXHAUST.get());player.removeEffect(TCEffects.INFECTIOUS_VIS_EXHAUST.get());
        for(var slot:new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET})player.setItemSlot(slot,ItemStack.EMPTY);
        for(int slot=0;slot<rings.getSlots();slot++)rings.setStackInSlot(slot,ItemStack.EMPTY);
        player.getInventory().setItem(0,new ItemStack(TCItems.GOGGLES.get()));player.getInventory().setItem(1,new ItemStack(TCItems.elementalRing(ElementalRingType.AIR)));
        player.doTick();
        test.assertTrue(WandInventory.discount(player,TCAspects.catalog().require("aer"))==0,"Carried gear contributes no worn discount");
        TC4Revival.LOGGER.info("PASS allElementalRingPairsArmorAndVisExhaustionProduceLegacyPayments: {} cases",cases);test.succeed();
    }
}
