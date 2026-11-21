package fmmodern;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.gametest.*;

@GameTestHolder("forbidden_magic") @PrefixGameTestTemplate(false)
public class ShardServerTests {
    /** Observe actual world insertion, even before a newly loaded Nether chunk's spatial index is visible. */
    private static class DropProbe implements AutoCloseable {
        private final ServerLevel level;private final AABB bounds;
        private final java.util.List<ItemEntity> items=new java.util.ArrayList<>();
        DropProbe(ServerLevel level,BlockPos pos){this.level=level;bounds=new AABB(pos).inflate(5);net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(this);}
        @net.minecraftforge.eventbus.api.SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.LOWEST)
        public void joined(net.minecraftforge.event.entity.EntityJoinLevelEvent event){
            if(event.getLevel()==level&&event.getEntity() instanceof ItemEntity item&&bounds.contains(item.position()))items.add(item);
        }
        int count(String id){return items.stream().filter(e->e.getItem().is(Content.item(id))).mapToInt(e->e.getItem().getCount()).sum();}
        void clear(){items.forEach(Entity::discard);items.clear();}
        @Override public void close(){net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(this);clear();}
    }
    private static Player player(ServerLevel level, BlockPos pos) {
        var player = new Player(level, pos, 0, new GameProfile(UUID.randomUUID(), "ShardTest")) {
            @Override public boolean isSpectator() { return false; }
            @Override public boolean isCreative() { return false; }
        };
        player.setPos(pos.getCenter());
        return player;
    }
    private static int drops(ServerLevel level, BlockPos pos, String item) {
        return level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(5)).stream()
                .filter(e -> e.getItem().is(Content.item(item))).mapToInt(e -> e.getItem().getCount()).sum();
    }
    private static void clearDrops(ServerLevel level, BlockPos pos) {
        level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(5)).forEach(Entity::discard);
    }
    @GameTest(template="test_empty", timeoutTicks=200)
    public static void ridingCropDropsLustWithoutKilling(GameTestHelper test) {
        var level = test.getLevel().getServer().getLevel(Level.NETHER);
        var pos = test.absolutePos(new BlockPos(2, 3, 2)).atY(160);
        level.getChunk(pos);
        level.setChunkForced(pos.getX()>>4,pos.getZ()>>4,true);
        test.runAfterDelay(20,()->{
        var victim = EntityType.HUSK.create(level);
        var player = player(level, pos);
        var crop = new ItemStack(Content.CROP.get());
        crop.getOrCreateTag().putBoolean("Unbreakable", true);
        player.setItemSlot(EquipmentSlot.MAINHAND, crop);
        victim.setPos(pos.getCenter());victim.setNoAi(true);level.addFreshEntity(victim);
        boolean previous = ForbiddenConfig.NO_LUST.get();
        try (var probe=new DropProbe(level,pos)) {
            ForbiddenConfig.NO_LUST.set(false);victim.getRandom().setSeed(76543);
            clearDrops(level,pos);
            for(int i=0;i<96;i++) { victim.invulnerableTime=0;victim.setHealth(victim.getMaxHealth());player.attack(victim); }
            test.assertTrue(victim.isAlive() && probe.count("lust_shard")>0, "Actual nonlethal crop hits drop lust in the Nether: alive="+victim.isAlive()+", health="+victim.getHealth()+", drops="+probe.count("lust_shard"));
            probe.clear();clearDrops(level,pos);ForbiddenConfig.NO_LUST.set(true);
            for(int i=0;i<96;i++) { victim.invulnerableTime=0;victim.setHealth(victim.getMaxHealth());player.attack(victim); }
            test.assertTrue(probe.count("lust_shard")==0, "noLust disables the on-hit reward");
            ForbiddenConfig.NO_LUST.set(false);
            var overworldVictim=EntityType.HUSK.create(test.getLevel());
            overworldVictim.setPos(test.absolutePos(new BlockPos(2,2,2)).getCenter());
            var overworldPlayer=player(test.getLevel(),overworldVictim.blockPosition());
            try(var overworldProbe=new DropProbe(test.getLevel(),overworldVictim.blockPosition())) {
                for(int i=0;i<96;i++)crop.getItem().hurtEnemy(crop,overworldVictim,overworldPlayer);
                test.assertTrue(overworldProbe.count("lust_shard")==0,"Overworld hits do not reward lust");
            }
            var pig=EntityType.PIG.create(level);pig.setPos(pos.getCenter());
            for(int i=0;i<96;i++)crop.getItem().hurtEnemy(crop,pig,player);
            test.assertTrue(probe.count("lust_shard")==0,"Passive animals are not monsters");
        } finally { ForbiddenConfig.NO_LUST.set(previous);victim.discard();clearDrops(level,pos);level.setChunkForced(pos.getX()>>4,pos.getZ()>>4,false); }
        test.succeed();
        });
    }
    @GameTest(template="test_empty", timeoutTicks=200)
    public static void envySurvivesVanillaSpawnAndDropsForBothPiglins(GameTestHelper test) {
        var level=test.getLevel().getServer().getLevel(Level.NETHER);
        var pos=test.absolutePos(new BlockPos(2,3,2)).atY(170);level.getChunk(pos);
        level.setChunkForced(pos.getX()>>4,pos.getZ()>>4,true);
        test.runAfterDelay(20,()->{
        try {
        long winningSeed=0;
        while(RandomSource.create(winningSeed).nextInt(175)!=1)winningSeed++;
        for(var type:java.util.List.of(EntityType.ZOMBIFIED_PIGLIN,EntityType.PIGLIN)) {
            var mob=type.create(level);mob.setPos(pos.getCenter());mob.setNoAi(true);
            clearDrops(level,pos);
            try (var probe=new DropProbe(level,pos)) {
                mob.getRandom().setSeed(winningSeed);
                ForgeEventFactory.onFinalizeSpawn(mob,level,level.getCurrentDifficultyAt(pos),MobSpawnType.NATURAL,null,null);
                test.assertTrue(mob.getPersistentData().getBoolean("fmPendingEnvy"),"Natural spawn rolled the legacy chance");
                level.addFreshEntity(mob);
                test.assertTrue(mob.getMainHandItem().is(Content.item("envy_shard")),type+" keeps shard after vanilla weapon initialization");
                test.assertTrue(!mob.getPersistentData().contains("fmPendingEnvy"),"Spawn reward is consumed once");
                var saved=mob.saveWithoutId(new net.minecraft.nbt.CompoundTag());var restored=type.create(level);restored.load(saved);
                test.assertTrue(restored.getMainHandItem().is(Content.item("envy_shard")),"Reload retains the carried shard");
                var player=player(level,pos);
                mob.invulnerableTime=0;mob.hurt(level.damageSources().playerAttack(player),1000);
                test.assertTrue(probe.count("envy_shard")==1,type+" drops exactly one envy shard on a player kill: alive="+mob.isAlive()+", drops="+probe.count("envy_shard"));
            } finally { mob.discard();clearDrops(level,pos); }
        }
        } finally { level.setChunkForced(pos.getX()>>4,pos.getZ()>>4,false); }
        test.succeed();
        });
    }
}
