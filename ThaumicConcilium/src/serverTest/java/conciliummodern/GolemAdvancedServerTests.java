package conciliummodern;

import java.util.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.gametest.*;
import tc4revival.content.*;

/** Runtime AI callbacks and actual harvested blocks; fixtures live only in serverTest. */
@GameTestHolder("thaumicconcilium") @PrefixGameTestTemplate(false)
public final class GolemAdvancedServerTests {
    @GameTest(template="empty",timeoutTicks=100)
    public static void colossusExcavatesHorizontalFiveByFiveWithSilkAndProtection(GameTestHelper test) {excavationPlane(test,Direction.UP,true,false);}
    @GameTest(template="empty",timeoutTicks=100)
    public static void colossusExcavatesNorthPlaneAndKeepsOffPlaneBlocks(GameTestHelper test) {excavationPlane(test,Direction.NORTH,false,false);}
    @GameTest(template="empty",timeoutTicks=100)
    public static void colossusExcavatesEastPlaneWithFortuneAndProtection(GameTestHelper test) {excavationPlane(test,Direction.EAST,false,true);}

    private static void excavationPlane(GameTestHelper test,Direction face,boolean silk,boolean fortune) {
        var center=test.absolutePos(new BlockPos(14,7,14));var targets=new ArrayList<BlockPos>();for(int a=-2;a<=2;a++)for(int b=-2;b<=2;b++){var p=plane(center,face,a,b);targets.add(p);test.getLevel().setBlock(p,Blocks.STONE.defaultBlockState(),3);}
        var protectedPos=plane(center,face,-2,-2);var bedrock=plane(center,face,2,-2);var wood=plane(center,face,-2,2);var ore=plane(center,face,2,2);var outside=plane(center,face,3,0);var behind=center.relative(face.getOpposite());
        test.getLevel().setBlock(bedrock,Blocks.BEDROCK.defaultBlockState(),3);test.getLevel().setBlock(wood,Blocks.OAK_PLANKS.defaultBlockState(),3);test.getLevel().setBlock(ore,Blocks.DIAMOND_ORE.defaultBlockState(),3);test.getLevel().setBlock(outside,Blocks.STONE.defaultBlockState(),3);test.getLevel().setBlock(behind,Blocks.STONE.defaultBlockState(),3);
        var player=player(test,"ConciliumMine");var source=Vec3.atCenterOf(center).add(Vec3.atLowerCornerOf(face.getNormal()).scale(3));var giant=giant(test,BlockPos.containing(source));
        test.assertTrue(player.startRiding(giant),"Actual server player mounts colossus before excavation");player.setPos(source.x,source.y-player.getEyeHeight(),source.z);aim(player,Vec3.atCenterOf(center));
        var focus=new ItemStack(TCItems.EXCAVATION_FOCUS.get());if(silk)TCItems.EXCAVATION_FOCUS.get().applyUpgrade(focus,FocusUpgrade.SILK_TOUCH,5);if(fortune)for(int rank:new int[]{1,3,5})TCItems.EXCAVATION_FOCUS.get().applyUpgrade(focus,FocusUpgrade.TREASURE,rank);
        var wand=TCItems.CASTING_WAND.get().create(WandParts.Rod.SILVERWOOD,WandParts.Cap.GOLD,false,true);WandFocusItem.attach(wand,focus);player.setItemInHand(InteractionHand.MAIN_HAND,wand);var before=wand.copy();
        var ray=test.getLevel().clip(new ClipContext(player.getEyePosition(),player.getEyePosition().add(player.getLookAngle().scale(5)),ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,player));test.assertTrue(ray.getType()==HitResult.Type.BLOCK&&ray.getBlockPos().equals(center)&&ray.getDirection()==face,"Fixture selects intended center and plane "+face);
        int[] denied={0};java.util.function.Consumer<BlockEvent.BreakEvent> protection=event->{if(event.getPlayer()==player&&event.getPos().equals(protectedPos)){event.setCanceled(true);denied[0]++;}};MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST,false,BlockEvent.BreakEvent.class,protection);
        try {player.swinging=true;giant.aiStep();}
        finally {MinecraftForge.EVENT_BUS.unregister(protection);player.swinging=false;}
        test.assertTrue(giant.action()==6&&denied[0]==1,"Rider swing enters actual six-tick digging animation and calls break protection once");
        for(var pos:targets){boolean retained=pos.equals(protectedPos)||pos.equals(bedrock)||pos.equals(wood);test.assertTrue(test.getLevel().getBlockState(pos).isAir()!=retained,"Exactly the permitted effective blocks are harvested on "+face+" at "+pos);}
        test.assertTrue(test.getLevel().getBlockState(outside).is(Blocks.STONE)&&test.getLevel().getBlockState(behind).is(Blocks.STONE),"Five-by-five operation does not mine outside its plane or into a second layer");
        var drops=test.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(center).inflate(5));int stones=drops.stream().map(ItemEntity::getItem).filter(s->s.is(silk?Items.STONE:Items.COBBLESTONE)).mapToInt(ItemStack::getCount).sum();
        int diamonds=drops.stream().map(ItemEntity::getItem).filter(s->s.is(silk?Items.DIAMOND_ORE:Items.DIAMOND)).mapToInt(ItemStack::getCount).sum();test.assertTrue(stones==21&&diamonds>=1&&diamonds<=(fortune?4:1),"Actual world drops retain stone count and selected silk/fortune tool semantics");
        var xp=test.getLevel().getEntitiesOfClass(ExperienceOrb.class,new AABB(center).inflate(5));test.assertTrue(silk?xp.isEmpty():!xp.isEmpty(),"Diamond ore XP follows actual silk setting");
        test.assertTrue(ItemStack.matches(before,wand),"Original colossus excavation does not separately debit or damage the equipped wand");
        drops.forEach(Entity::discard);xp.forEach(Entity::discard);player.stopRiding();giant.discard();test.succeed();
    }

    @GameTest(template="empty",timeoutTicks=100)
    public static void colossusReturnsToSavedHomeAndRejectsBlockedHome(GameTestHelper test) {
        var home=test.absolutePos(new BlockPos(8,3,8));for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)test.getLevel().setBlock(home.offset(x,-1,z),Blocks.STONE.defaultBlockState(),3);
        var original=giant(test,home);original.aiStep();var saved=new CompoundTag();original.saveWithoutId(saved);test.assertTrue(saved.getLong("ConciliumHome")==home.asLong(),"First actual AI callback captures physical home");original.discard();
        var restored=ConciliumGolems.BYDLO.get().create(test.getLevel());restored.load(saved);test.getLevel().addFreshEntity(restored);restored.setPos(home.getX()+60.5,home.getY(),home.getZ()+.5);restored.tickCount=20;restored.aiStep();
        test.assertTrue(restored.distanceToSqr(Vec3.atCenterOf(home))<4&&test.getLevel().noCollision(restored,restored.getBoundingBox()),"Actual distance watchdog returns reloaded colossus to safe saved home");
        var trap=test.absolutePos(new BlockPos(23,3,23));for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)for(int y=0;y<4;y++)test.getLevel().setBlock(trap.offset(x,y,z),Blocks.STONE.defaultBlockState(),3);
        restored.setPos(trap.getX()+.5,trap.getY(),trap.getZ()+.5);test.assertTrue(restored.isInWall(),"Fixture embeds colossus in real suffocating blocks");restored.tickCount=40;restored.aiStep();test.assertTrue(restored.distanceToSqr(Vec3.atCenterOf(home))<4&&!restored.isInWall(),"Wall watchdog actually returns colossus home");
        for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)for(int y=-1;y<=5;y++)test.getLevel().setBlock(home.offset(x,y,z),Blocks.STONE.defaultBlockState(),3);
        restored.setPos(trap.getX()+.5,trap.getY(),trap.getZ()+.5);var before=restored.position();restored.tickCount=60;restored.aiStep();test.assertTrue(restored.position().equals(before),"Fully blocked home never teleports entity into another suffocating destination");restored.discard();test.succeed();
    }

    @GameTest(template="empty",timeoutTicks=100)
    public static void earmuffsTagBlocksOnlyItsGolemControlLease(GameTestHelper test) {
        var player=player(test,"ConciliumMuffs");var pos=test.absolutePos(new BlockPos(8,3,8));player.moveTo(Vec3.atCenterOf(pos));var caster=ConciliumGolems.VENGEFUL.get().create(test.getLevel());caster.moveTo(player.getX()+2,player.getY(),player.getZ(),0,0);var other=ConciliumGolems.VENGEFUL.get().create(test.getLevel());other.moveTo(player.position());
        var tag=TagKey.create(Registries.ITEM,ResourceLocation.parse("thaumicconcilium:earmuffs"));var fixture=new ItemStack(Items.LEATHER_HELMET);test.assertTrue(fixture.is(tag),"serverTest-only earmuffs tag fixture was loaded");
        player.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.IRON_HELMET));caster.getRandom().setSeed(29);boolean controlled=false;for(int i=0;i<40;i++){caster.performRangedAttack(player,1);controlled|=player.hasEffect(CrimsonControl.ENSLAVED.get());}test.assertTrue(controlled,"Ordinary head armor is not ear protection");
        CrimsonControl.enslave(player,caster,60);CrimsonControl.enslave(player,other,60);player.setItemSlot(EquipmentSlot.HEAD,fixture);caster.performRangedAttack(player,1);test.assertTrue(player.hasEffect(CrimsonControl.ENSLAVED.get()),"Muffled caster cannot clear another caster's lease");CrimsonControl.release(player,other);
        for(int i=0;i<40;i++)caster.performRangedAttack(player,1);test.assertTrue(!player.hasEffect(CrimsonControl.ENSLAVED.get()),"Real earmuffs tag prevents repeated control attempts and clears this caster's lease");
        var nativeMuffs=net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(ResourceLocation.parse("witchery:earmuffs"));if(nativeMuffs!=null&&nativeMuffs!=Items.AIR){player.setItemSlot(EquipmentSlot.HEAD,new ItemStack(nativeMuffs));CrimsonControl.enslave(player,caster,60);caster.performRangedAttack(player,1);test.assertTrue(!player.hasEffect(CrimsonControl.ENSLAVED.get()),"Installed Witchery earmuffs also prevent actual control");}else com.mojang.logging.LogUtils.getLogger().info("CONCILIUM_BRANCH_UNAVAILABLE: native Witchery earmuffs item absent; actual tag-based immunity tested with isolated leather-helmet fixture.");
        caster.discard();other.discard();test.succeed();
    }

    private static BlockPos plane(BlockPos center,Direction face,int a,int b) {return switch(face.getAxis()){case Y->center.offset(a,0,b);case Z->center.offset(a,b,0);case X->center.offset(0,b,a);};}
    private static FakePlayer player(GameTestHelper test,String name) {var player=new FakePlayer(test.getLevel(),new GameProfile(UUID.randomUUID(),name));player.setGameMode(GameType.SURVIVAL);return player;}
    private static GolemBydlo giant(GameTestHelper test,BlockPos pos) {var giant=ConciliumGolems.BYDLO.get().create(test.getLevel());giant.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);giant.setNoAi(true);giant.setNoGravity(true);test.getLevel().addFreshEntity(giant);return giant;}
    private static void aim(FakePlayer player,Vec3 target) {var d=target.subtract(player.getEyePosition());player.setYRot((float)Math.toDegrees(Math.atan2(-d.x,d.z)));player.setXRot((float)-Math.toDegrees(Math.atan2(d.y,Math.sqrt(d.x*d.x+d.z*d.z))));player.yRotO=player.getYRot();player.xRotO=player.getXRot();}
    private GolemAdvancedServerTests() {}
}
