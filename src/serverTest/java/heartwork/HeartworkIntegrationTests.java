package heartwork;

import heartwork.compat.blade.*;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.registry.slashblade.SlashBladeDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.*;
import net.minecraftforge.gametest.*;

@GameTestHolder("heartwork") @PrefixGameTestTemplate(false)
public class HeartworkIntegrationTests {
    @GameTest(template="empty") public static void electricBladeImplementsHostEnergyContract(GameTestHelper h) {
        if(!net.minecraftforge.fml.ModList.get().isLoaded("ic2")||!net.minecraftforge.fml.ModList.get().isLoaded("slashblade")){h.succeed();return;}
        var blade=new ItemStack(Heartwork.item("eu_blade"));var item=(ElectricBladeItem)blade.getItem();
        var owner=h.spawn(EntityType.ZOMBIE,3,3,3);blade.setDamageValue(blade.getMaxDamage()/2);
        blade.getCapability(ItemSlashBlade.BLADESTATE).ifPresent(s -> s.setBroken(true));
        item.inventoryTick(blade,h.getLevel(),owner,0,true);
        double expected=2_000_000.0*(blade.getMaxDamage()-blade.getDamageValue())/blade.getMaxDamage();
        h.assertTrue(Math.abs(ic2.api.item.ElectricItem.manager.getCharge(blade)-expected)<0.1,"IC2 manager sees the FIX durability-derived charge");
        h.assertTrue(item.getTier(blade)==3&&item.getTransferLimit(blade)==2048&&!item.canProvideEnergy(blade),"Original tier/transfer/no output");
        h.assertTrue(!blade.getCapability(ItemSlashBlade.BLADESTATE).orElseThrow(IllegalStateException::new).isBroken(),"Sufficient charge clears broken state");owner.discard();h.succeed();
    }
    @GameTest(template="empty") public static void manaPoolRepairsOnlyTheBotaniaBlade(GameTestHelper h) {
        if(!net.minecraftforge.fml.ModList.get().isLoaded("botania")||!net.minecraftforge.fml.ModList.get().isLoaded("slashblade")){h.succeed();return;}
        var pos=new BlockPos(3,2,3);var poolBlock=net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(new ResourceLocation("botania:mana_pool"));
        h.setBlock(pos,poolBlock);var pool=(vazkii.botania.common.block.block_entity.mana.ManaPoolBlockEntity)h.getBlockEntity(pos);
        vazkii.botania.common.block.block_entity.mana.ManaPoolBlockEntity.serverTick(h.getLevel(),h.absolutePos(pos),pool.getBlockState(),pool);pool.receiveMana(1000);
        h.assertTrue(pool.getCurrentMana()==1000,"Host pool initialized before fixture charging");
        var registry=h.getLevel().registryAccess().registryOrThrow(SlashBladeDefinition.REGISTRY_KEY);
        var blade=registry.get(new ResourceLocation("heartwork:yingjian")).getBlade();h.assertTrue(!blade.isEmpty(),"Botania blade becomes available with the real host");blade.setDamageValue(10);
        var stand=new mods.flammpfeil.slashblade.entity.BladeStandEntity(mods.flammpfeil.slashblade.SlashBlade.RegistryEvents.BladeStand,h.getLevel());stand.setPos(net.minecraft.world.phys.Vec3.atCenterOf(h.absolutePos(pos.above())));stand.setItem(blade);
        BotaniaBladeCompat.tickStand(new mods.flammpfeil.slashblade.event.SlashBladeEvent.BladeStandTickEvent(blade,blade.getCapability(ItemSlashBlade.BLADESTATE).orElseThrow(IllegalStateException::new),stand));
        h.assertTrue(pool.getCurrentMana()==925&&blade.getDamageValue()==7,"75 mana repairs three durability through the host event; mana="+pool.getCurrentMana()+", damage="+blade.getDamageValue());
        var other=registry.get(new ResourceLocation("heartwork:roukan")).getBlade();other.setDamageValue(10);
        BotaniaBladeCompat.tickStand(new mods.flammpfeil.slashblade.event.SlashBladeEvent.BladeStandTickEvent(other,other.getCapability(ItemSlashBlade.BLADESTATE).orElseThrow(IllegalStateException::new),stand));
        h.assertTrue(pool.getCurrentMana()==925&&other.getDamageValue()==10,"Ordinary blades do not consume mana");h.succeed();
    }
}
