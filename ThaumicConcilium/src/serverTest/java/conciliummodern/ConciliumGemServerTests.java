package conciliummodern;

import com.mojang.authlib.GameProfile;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumicconcilium") @PrefixGameTestTemplate(false)
public final class ConciliumGemServerTests {
    private static FakePlayer player(GameTestHelper helper,String name){return new FakePlayer(helper.getLevel(),new GameProfile(UUID.nameUUIDFromBytes(name.getBytes(java.nio.charset.StandardCharsets.UTF_8)),name));}
    private static TransientCraftingContainer grid(){return new TransientCraftingContainer(new AbstractContainerMenu(null,0){@Override public ItemStack quickMoveStack(Player player,int slot){return ItemStack.EMPTY;}@Override public boolean stillValid(Player player){return true;}},3,3);}
    @GameTest(template="empty") public static void sampleNbtAndLegacyDimensions(GameTestHelper h){
        var state=Blocks.OAK_LOG.defaultBlockState().setValue(BlockStateProperties.AXIS,Direction.Axis.X);var sample=new GemCoordinates.Sample(state,Level.NETHER);
        h.assertTrue(sample.equals(GemCoordinates.read(sample.save())),"Block properties and namespaced dimension survive NBT");
        var gem=new ItemStack(ConciliumGems.TERRA.get());gem.getOrCreateTag().putString("name","minecraft:stone");gem.getTag().putInt("meta",0);
        h.assertTrue(GemCoordinates.captured(gem)==null,"Missing legacy dimension does not become overworld");
        gem.getTag().putInt("dim",0);h.assertTrue(GemCoordinates.captured(gem).dimension()==Level.OVERWORLD,"Known legacy overworld survives");
        gem.getTag().putInt("dim",33);h.assertTrue(GemCoordinates.captured(gem)==null,"Unknown numeric dimension is never guessed");
        gem.getTag().putInt("dim",-1);gem.getTag().putInt("meta",1);h.assertTrue(GemCoordinates.captured(gem)==null,"Legacy metadata is not silently discarded");
        gem.getTag().put("Captured",sample.save());h.assertTrue(GemCoordinates.captured(gem).equals(sample),"Modern sample wins over old tags");h.succeed();
    }
    @GameTest(template="empty") public static void bindingPreviewPreservesInputsAndEnforcesSixteenSampleLimit(GameTestHelper h){
        var rift=new ItemStack(ConciliumGems.RIFT.get());rift.getOrCreateTag().putBoolean("REVERSE",true);rift.getTag().putString("owner","keep");
        var terra=new ItemStack(ConciliumGems.TERRA.get());terra.getOrCreateTag().put("Captured",new GemCoordinates.Sample(Blocks.DEEPSLATE.defaultBlockState(),Level.END).save());
        var grid=grid();grid.setItem(0,rift);grid.setItem(8,terra);var recipe=new TerraGemRecipe(ResourceLocation.parse("thaumicconcilium:validation/bind"),CraftingBookCategory.MISC);
        h.assertTrue(recipe.matches(grid,h.getLevel())==ConciliumGems.available(),"Recipe availability follows Gadomancy");
        var beforeRift=rift.copy();var beforeTerra=terra.copy();var result=recipe.assemble(grid,h.getLevel().registryAccess());
        h.assertTrue(!result.isEmpty()&&GemCoordinates.samples(result).size()==1&&GemCoordinates.selected(result).dimension()==Level.END,"Binding stores one sampled dimension");
        h.assertTrue(GemCoordinates.reverse(result)&&result.getTag().getString("owner").equals("keep"),"Binding preserves unrelated NBT");
        h.assertTrue(ItemStack.matches(beforeRift,rift)&&ItemStack.matches(beforeTerra,terra),"Repeated recipe previews never consume or mutate inputs");
        result.getTag().putString("owner","new");h.assertTrue(rift.getTag().getString("owner").equals("keep"),"Result NBT does not alias input");
        grid.setItem(1,new ItemStack(Items.STONE));h.assertTrue(recipe.assemble(grid,h.getLevel().registryAccess()).isEmpty(),"Foreign ingredient rejects craft");grid.setItem(1,new ItemStack(ConciliumGems.RIFT.get()));h.assertTrue(recipe.assemble(grid,h.getLevel().registryAccess()).isEmpty(),"Duplicate rift rejects craft");grid.setItem(1,ItemStack.EMPTY);
        for(int i=0;i<16;i++)GemCoordinates.append(rift,new GemCoordinates.Sample(Blocks.STONE.defaultBlockState(),Level.OVERWORLD));
        h.assertTrue(recipe.assemble(grid,h.getLevel().registryAccess()).isEmpty(),"Full rift gem rejects a seventeenth sample");
        boolean rejected=false;try{GemCoordinates.append(rift,new GemCoordinates.Sample(Blocks.DIRT.defaultBlockState(),Level.NETHER));}catch(IllegalArgumentException expected){rejected=true;}h.assertTrue(rejected&&GemCoordinates.samples(rift).size()==16,"Direct append cannot bypass limit");
        rift.getTag().putInt("CURR",Integer.MIN_VALUE);h.assertTrue(GemCoordinates.selected(rift)!=null,"Malformed selection remains bounded");h.succeed();
    }
    @GameTest(template="empty") public static void causalDimensionAndSafeLandingUseRealServerWorld(GameTestHelper h){
        var dimension=h.getLevel().getServer().getLevel(RiftTransit.DIMENSION);h.assertTrue(dimension!=null,"Causal bouillon dimension is loaded");
        var pos=new BlockPos(4096,0,4096);dimension.getChunkAt(pos);
        h.assertTrue(dimension.getBlockState(pos).isAir(),"Original Y0 remains air");for(int y=1;y<=3;y++)h.assertTrue(dimension.getBlockState(pos.atY(y)).is(ConciliumGems.VOID_REALITY.get()),"Original floor fills Y1..3");
        var p=player(h,"ConciliumLanding");var requested=new Vec3(pos.getX()+.5,5,pos.getZ()+.5);var landing=RiftTransit.findLanding(dimension,p,requested);
        h.assertTrue(requested.equals(landing),"One safe falling block preserves exact Y5 entry and XZ");
        h.assertTrue(dimension.getBlockState(pos.atY(3)).getDestroySpeed(dimension,pos.atY(3))<0,"Causal floor remains unbreakable");
        var local=h.absolutePos(new BlockPos(8,3,8));h.getLevel().setBlockAndUpdate(local.below(),Blocks.STONE.defaultBlockState());h.getLevel().setBlockAndUpdate(local,Blocks.WATER.defaultBlockState());
        var wet=RiftTransit.findLanding(h.getLevel(),p,Vec3.atBottomCenterOf(local));h.assertTrue(wet==null||wet.y!=local.getY(),"Landing never accepts a water-filled body volume");h.succeed();
    }
    @GameTest(template="empty") public static void dimensionalRiftIdentityAndTravelGuards(GameTestHelper h){
        var rift=ConciliumEntities.RIFT.get().create(h.getLevel());h.assertTrue(rift!=null,"Rift registered");rift.initialize(234,50);RiftTransit.initialize(rift);
        var saved=new CompoundTag();rift.saveWithoutId(saved);var restored=ConciliumEntities.RIFT.get().create(h.getLevel());restored.load(saved);
        h.assertTrue(RiftTransit.active(restored),"Interdimensional identity persists through entity NBT");
        var p=player(h,"ConciliumRiftGuard");h.assertTrue(!RiftTransit.contact(restored,p),"Empty hand cannot travel");p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(ConciliumGems.RIFT_INERT.get()));h.assertTrue(!RiftTransit.contact(restored,p),"Inert gem cannot travel");
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(ConciliumGems.RIFT.get()));p.setPortalCooldown();h.assertTrue(!RiftTransit.contact(restored,p),"Portal cooldown prevents immediate reentry");
        RiftTransit.tick(restored,201);h.assertTrue(restored.isClosing(),"Dimensional rift closes after the original 200 tick lifetime");h.succeed();
    }
}
