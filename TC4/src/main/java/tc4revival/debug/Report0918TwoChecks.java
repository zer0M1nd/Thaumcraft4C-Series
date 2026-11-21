package tc4revival.debug;

import java.util.*;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.util.FakePlayer;
import tc4revival.aspect.*;
import tc4revival.aspect.entity.EntityAspects;
import tc4revival.content.*;

public class Report0918TwoChecks {
    public static int verify(CommandSourceStack source) {
        try { return run(source); }
        catch(RuntimeException failure) {
            tc4revival.TC4Revival.LOGGER.error("0918_2 regression failed",failure);
            source.sendFailure(Component.literal("0918_2 failed: "+failure));return 0;
        }
    }
    private static int run(CommandSourceStack source) {
        var level=source.getLevel();
        var player=new FakePlayer(level,new com.mojang.authlib.GameProfile(UUID.nameUUIDFromBytes("tc4:0918_2".getBytes(java.nio.charset.StandardCharsets.UTF_8)),"TC4Repeat"));
        repeat(source,player);
        combat(source);
        aspects(source);
        nodeLoot(source,player);
        require(OuterMazeDebugCommands.locate(source,false)==0,"Overworld query must fail");
        for(int cx:new int[]{-37,42}) {
            var plan=new EldritchMazeSavedData.Plan(cx,-cx,EldritchMazeLayout.generate(15,15,29));
            require(OuterMazeDebugCommands.locate(plan,false).isPresent()&&OuterMazeDebugCommands.locate(plan,true).isPresent(),"Maze positions missing");
        }
        source.sendSuccess(()->Component.literal("0918_2 passed: completed infusion repeat/reload/atomic failures/branch gate, shield direction, rider render anchor, legacy entity aspects, silverwood node loot, maze lookup."),false);
        return 1;
    }
    private static void repeat(CommandSourceStack source,FakePlayer player) {
        var level=source.getLevel();var pos=new BlockPos(320,200,320);level.getChunkAt(pos);
        var positions=new ArrayList<BlockPos>(List.of(pos,pos.below(2),pos.offset(3,-2,0),pos.offset(-3,-2,0)));
        for(int x:new int[]{-1,1})for(int z:new int[]{-1,1})positions.add(pos.offset(x,-2,z));
        var backups=positions.stream().map(p->net.minecraftforge.common.util.BlockSnapshot.create(level.dimension(),level,p)).toList();
        var recipes=new ArrayList<>(level.getRecipeManager().getRecipes());boolean branch=tc4revival.config.TCConfig.SERVER.branchFeatures.get();
        try {
            tc4revival.config.TCConfig.SERVER.branchFeatures.set(true);
            var recipe=new InfusionRecipe.Serializer().fromJson(ResourceLocation.parse("thaumcraft:debug_repeat"),com.google.gson.JsonParser.parseString(
                    "{\"research\":\"\",\"instability\":0,\"aspects\":{},\"center\":{\"ingredient\":{\"item\":\"minecraft:iron_ingot\"}},\"components\":[{\"ingredient\":{\"item\":\"minecraft:diamond\"}},{\"ingredient\":{\"item\":\"minecraft:diamond\"}}],\"result\":{\"item\":\"minecraft:gold_ingot\"}}").getAsJsonObject());
            var withFixture=new ArrayList<net.minecraft.world.item.crafting.Recipe<?>>(recipes);withFixture.add(recipe);level.getRecipeManager().replaceRecipes(withFixture);
            for(var p:positions)level.setBlock(p,(p.equals(pos)?TCArcane.INFUSION_MATRIX.get():p.getX()!=pos.getX()&&p.getZ()!=pos.getZ()?TCArcane.INFUSION_PILLAR.get():TCArcane.ARCANE_PEDESTAL.get()).defaultBlockState(),2);
            var matrix=(InfusionMatrixBlockEntity)level.getBlockEntity(pos);var center=(ArcanePedestalBlockEntity)level.getBlockEntity(pos.below(2));
            var east=(ArcanePedestalBlockEntity)level.getBlockEntity(pos.offset(3,-2,0));var west=(ArcanePedestalBlockEntity)level.getBlockEntity(pos.offset(-3,-2,0));
            center.setItem(0,new ItemStack(Items.IRON_INGOT));east.setItem(0,new ItemStack(Items.DIAMOND));west.setItem(0,new ItemStack(Items.DIAMOND));
            player.setPos(pos.getX()+1,pos.getY(),pos.getZ());matrix.activate();matrix.start(player);require(matrix.crafting(),"Fixture must start");
            require(!matrix.repeat(player),"Cannot repeat during infusion");
            for(int tick=0;tick<2000&&matrix.crafting();tick++)matrix.tick();
            require(!matrix.crafting()&&center.getItem(0).is(Items.GOLD_INGOT)&&east.isEmpty()&&west.isEmpty(),"Real infusion did not complete");
            var saved=matrix.saveWithoutMetadata();var reloaded=new InfusionMatrixBlockEntity(pos,matrix.getBlockState());reloaded.load(saved);level.removeBlockEntity(pos);level.setBlockEntity(reloaded);matrix=reloaded;
            player.getInventory().items.set(1,new ItemStack(Items.IRON_INGOT,2));player.getInventory().items.set(2,new ItemStack(Items.DIAMOND,4));
            var previewInventory=player.getInventory().save(new ListTag());
            require(matrix.canRepeat(player)&&previewInventory.equals(player.getInventory().save(new ListTag()))&&center.getItem(0).is(Items.GOLD_INGOT)&&east.isEmpty()&&west.isEmpty(),"Repeat hint must be a read-only complete transaction preview");
            var hit=new BlockHitResult(pos.getCenter(),net.minecraft.core.Direction.UP,pos,false);
            require(matrix.getBlockState().use(level,player,InteractionHand.MAIN_HAND,hit).consumesAction(),"Empty hand interaction unhandled");
            require(center.getItem(0).is(Items.IRON_INGOT)&&east.getItem(0).is(Items.DIAMOND)&&west.getItem(0).is(Items.DIAMOND),"Repeat placement missing");
            require(player.getInventory().countItem(Items.GOLD_INGOT)==1&&player.getInventory().countItem(Items.DIAMOND)==2,"Output exchange or consumption incorrect");
            var inventory=player.getInventory().save(new ListTag());require(!matrix.repeat(player)&&inventory.equals(player.getInventory().save(new ListTag())),"Occupied pedestal mutated inventory");
            east.clearContent();west.clearContent();player.getInventory().clearContent();inventory=player.getInventory().save(new ListTag());
            require(!matrix.repeat(player)&&east.isEmpty()&&west.isEmpty()&&inventory.equals(player.getInventory().save(new ListTag())),"Missing materials partially committed");
            player.getInventory().items.set(2,new ItemStack(Items.DIAMOND,2));
            tc4revival.config.TCConfig.SERVER.branchFeatures.set(false);require(!matrix.repeat(player),"Branch gate bypassed");
            tc4revival.config.TCConfig.SERVER.branchFeatures.set(true);require(matrix.repeat(player),"Reusable matching center not retained");
        } finally {
            tc4revival.config.TCConfig.SERVER.branchFeatures.set(branch);level.getRecipeManager().replaceRecipes(recipes);
            for(var snapshot:backups)snapshot.restore(true,false);
        }
    }
    private static void combat(CommandSourceStack source) {
        var level=source.getLevel();
        var player=new FakePlayer(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"TC4Shield")) {
            @Override public boolean isBlocking(){return true;}
            @Override public boolean isInvulnerableTo(net.minecraft.world.damagesource.DamageSource damage){return false;}
        };
        // FakePlayer is normally invulnerable, and fresh ServerPlayers also have spawn grace.
        net.minecraftforge.fml.util.ObfuscationReflectionHelper.setPrivateValue(net.minecraft.server.level.ServerPlayer.class,player,0,"f_8921_");
        player.setPos(370,200,370);player.setYRot(0);player.setXRot(0);player.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(Items.SHIELD));player.startUsingItem(InteractionHand.OFF_HAND);
        var guardian=TCEntities.ELDRITCH_GUARDIAN.get().create(level);guardian.setPos(370,200,380);
        var orb=new EldritchOrbEntity(level,guardian);orb.setPos(370,201,372);var damage=orb.impactDamage(guardian);
        require(!damage.is(net.minecraft.tags.DamageTypeTags.BYPASSES_SHIELD)&&damage.is(net.minecraft.tags.DamageTypeTags.IS_PROJECTILE)&&damage.is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR),"Projectile damage tags incorrect");
        float health=player.getHealth();require(!player.hurt(damage,6)&&player.getHealth()==health,"Forward shield did not block orb");
        orb.setPos(370,201,368);require(player.hurt(orb.impactDamage(guardian),6)&&player.getHealth()<health,"Rear hit must not be blocked");
        var crab=TCEntities.ELDRITCH_CRAB.get().create(level);require(crab.startRiding(player,true),"Crab failed to mount");
        crab.setPos(1,1,1);player.setPos(410,205,420);
        var anchor=crab.ridingRenderPosition(1);require(anchor.x==410&&anchor.z==420&&anchor.y>205,"Rider visual anchor depends on stale crab position");
        crab.stopRiding();require(crab.ridingRenderPosition(1).equals(crab.getPosition(1)),"Unmounted render anchor changed");
        crab.discard();orb.discard();guardian.discard();
    }
    private static void aspects(CommandSourceStack source) {
        var level=source.getLevel();int checked=0;
        for(var rule:EntityAspects.resolver().rules())if(rule.entity().startsWith("thaumcraft:")&&rule.variant()==tc4revival.aspect.entity.EntityAspectRule.Variant.ANY) {
            var type=net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getValue(ResourceLocation.parse(rule.entity()));var entity=type.create(level);
            require(entity!=null&&EntityAspects.resolve(entity).isPresent(),"Missing entity aspects: "+rule.entity());entity.discard();checked++;
        }
        require(checked>=23,"Incomplete legacy entity table");
        var slime=TCEntities.THAUMIC_SLIME.get().create(level);require(EntityAspects.resolve(slime).orElseThrow().aspects().amount(TCAspects.catalog().require("limus"))==2,"Slime amounts differ");slime.discard();
    }
    private static void nodeLoot(CommandSourceStack source,FakePlayer player) {
        var level=source.getLevel();var pos=new BlockPos(340,200,340);level.getChunkAt(pos);
        var before=net.minecraftforge.common.util.BlockSnapshot.create(level.dimension(),level,pos);
        try {
            level.setBlock(pos,TCArcane.SILVERWOOD_NODE_LOG.get().defaultBlockState(),2);
            var node=(AuraNodeBlockEntity)level.getBlockEntity(pos);node.configure(new AspectAmounts().add(TCAspects.catalog().require("aer"),20),AuraNodeType.PURE,null,"loot-check",false);
            var drops=net.minecraft.world.level.block.Block.getDrops(level.getBlockState(pos),level,pos,node,player,new ItemStack(Items.DIAMOND_AXE));
            require(drops.stream().filter(s->s.is(TCItems.WISP_ESSENCE.get())).mapToInt(ItemStack::getCount).sum()==3,"Tree-felling loot omitted/duplicated node essence");
            require(drops.stream().anyMatch(s->s.is(TCItems.SILVERWOOD_LOG.get())),"Normal log drop lost");
            require(level.destroyBlock(pos,true,player),"Tree-felling destroy failed");
            var entities=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(pos).inflate(1));
            require(entities.stream().filter(e->e.getItem().is(TCItems.WISP_ESSENCE.get())).mapToInt(e->e.getItem().getCount()).sum()==3,"Actual block destruction omitted/duplicated essence");
            entities.forEach(net.minecraft.world.entity.Entity::discard);
        } finally { before.restore(true,false); }
    }
    private static void require(boolean condition,String message){if(!condition)throw new IllegalStateException(message);}
}
