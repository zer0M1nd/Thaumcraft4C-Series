package fmmodern.compat;

import fmmodern.Content;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.FakePlayerFactory;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import wayoftime.bloodmagic.util.helper.NetworkHelper;
import wayoftime.bloodmagic.core.registry.OrbRegistry;

/** No optional Blood Magic class is touched by the dependency-free test branch. */
public final class BloodServerChecks {
    public static void inkAndOrb(GameTestHelper test){
        var player=FakePlayerFactory.get(test.getLevel(),new GameProfile(UUID.fromString("98c39109-6f1e-43f2-982c-81272735fe12"),"fm_ink_test"));
        var stack=new ItemStack(Content.item("bloodwell"));var ink=(BloodIntegration.Inkwell)stack.getItem();
        var binding=BloodIntegration.bind(stack,player);var network=NetworkHelper.getSoulNetwork(binding);int saved=network.getCurrentEssence();
        try{
            network.setCurrentEssence(200);ink.setInk(stack,4);
            test.assertTrue(ink.getInk(stack)==5&&network.getCurrentEssence()==175,"Writing spends 25 LP instead of ink");
            network.setCurrentEssence(0);ink.setInk(stack,4);
            test.assertTrue(ink.getInk(stack)==4,"An empty network consumes the backup ink");
            var copy=ItemStack.of(stack.save(new net.minecraft.nbt.CompoundTag()));
            test.assertTrue(ink.getBinding(copy).getOwnerId().equals(player.getUUID()),"Ink binding survives serialization");
            test.assertTrue(OrbRegistry.getOrbStack(BloodIntegration.ORB.get()).is(Content.item("eldritch_orb")),"Orb ID resolves to its item");
            test.assertTrue(OrbRegistry.getOrbsForTier(BloodIntegration.ORB.get().getTier()).stream().anyMatch(s->s.is(Content.item("eldritch_orb"))),"Network tier catalog includes the addon orb");
        }finally{network.setCurrentEssence(saved);}
        test.succeed();
    }
    public static void seal(GameTestHelper test){
        var player=FakePlayerFactory.get(test.getLevel(),new GameProfile(UUID.fromString("b39d2e46-4474-44ef-8031-9653e8087d14"),"fm_seal_test"));
        var network=NetworkHelper.getSoulNetwork(player);int saved=network.getCurrentEssence();
        try{
            network.setCurrentEssence(1000);player.addEffect(new net.minecraft.world.effect.MobEffectInstance(Content.BLOOD_SEAL.get(),1200));
            network.setCurrentEssence(1800);BloodIntegration.enforceSeal(player);
            test.assertTrue(network.getCurrentEssence()==1000,"Seal rejects direct LP gains after application");
            network.setCurrentEssence(400);BloodIntegration.enforceSeal(player);network.setCurrentEssence(900);BloodIntegration.enforceSeal(player);
            test.assertTrue(network.getCurrentEssence()==400,"Spending lowers the sealed LP ceiling");
            player.removeEffect(Content.BLOOD_SEAL.get());BloodIntegration.enforceSeal(player);network.setCurrentEssence(900);BloodIntegration.enforceSeal(player);
            test.assertTrue(network.getCurrentEssence()==900,"Removing seal allows LP growth again");
        }finally{player.removeEffect(Content.BLOOD_SEAL.get());BloodIntegration.enforceSeal(player);network.setCurrentEssence(saved);}
        test.succeed();
    }
    public static void ritual(GameTestHelper test){
        var ritual=wayoftime.bloodmagic.BloodMagic.RITUAL_MANAGER.getRitual("forbidden_magic_sanity");
        test.assertTrue(ritual instanceof SanityRitual,"Blood Magic discovers the addon ritual annotation");
        var components=new java.util.ArrayList<wayoftime.bloodmagic.ritual.RitualComponent>();ritual.gatherComponents(components::add);
        test.assertTrue(components.size()==24,"Ritual has the legacy 24-rune layout");
        var level=test.getLevel();var pos=test.absolutePos(new net.minecraft.core.BlockPos(3,2,3));
        var block=net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(net.minecraft.resources.ResourceLocation.parse("bloodmagic:masterritualstone"));
        test.assertTrue(block!=net.minecraft.world.level.block.Blocks.AIR,"Master ritual stone exists");
        level.setBlock(pos,block.defaultBlockState(),3);
        var stone=(wayoftime.bloodmagic.common.tile.TileMasterRitualStone)level.getBlockEntity(pos);
        var player=new net.minecraftforge.common.util.FakePlayer(level,new GameProfile(UUID.randomUUID(),"fm_ritual_test"));
        player.setPos(pos.getX()+2.5,pos.getY(),pos.getZ()+.5);level.addNewPlayer(player);
        var network=NetworkHelper.getSoulNetwork(player);int saved=network.getCurrentEssence();network.setCurrentEssence(40000);
        var tag=stone.saveWithFullMetadata();tag.putUUID("owner",player.getUUID());stone.load(tag);
        test.onEachTick(()->{
            if(level.getGameTime()%600!=0)return;
            try{
                ritual.performRitual(stone);
                var ward=player.getEffect(tc4revival.content.TCEffects.WARP_WARD.get());
                test.assertTrue(ward!=null&&ward.getDuration()>=600,"Ritual applies a refreshing warp ward");
                test.assertTrue(network.getCurrentEssence()==25000,"One protected player costs exactly 15000 LP");
                ritual.performRitual(stone);test.assertTrue(network.getCurrentEssence()==25000,"Fresh ward is not charged twice");
                test.succeed();
            }finally{player.discard();network.setCurrentEssence(saved);level.removeBlock(pos,false);}
        });
    }
}
