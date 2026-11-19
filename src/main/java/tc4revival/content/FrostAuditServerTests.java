package tc4revival.content;

import java.util.*;
import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class FrostAuditServerTests {
    @GameTest(template="wand_test_empty")
    public static void frostThreeModesConserveLegacyCostsAndParameters(GameTestHelper test) {
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(3,10,3));var player=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"FrostModes"));player.moveTo(Vec3.atCenterOf(pos));var chunk=player.chunkPosition();boolean forced=level.setChunkForced(chunk.x,chunk.z,true);
        var item=TCItems.FROST_FOCUS.get();var wandItem=TCItems.CASTING_WAND.get();
        try {
            for(int mode=0;mode<3;mode++)for(int potency:new int[]{0,2}) {
                var core=new ItemStack(item);item.applyUpgrade(core,FocusUpgrade.ALCHEMISTS_FROST,1);if(potency>0){item.applyUpgrade(core,FocusUpgrade.POTENCY,2);item.applyUpgrade(core,FocusUpgrade.POTENCY,4);}if(mode>0)item.applyUpgrade(core,mode==1?FocusUpgrade.SCATTERSHOT:FocusUpgrade.ICE_BOULDER,3);
                var wand=wandItem.create(WandParts.Rod.WOOD,WandParts.Cap.GOLD,false,true);WandFocusItem.attach(wand,core);player.setItemInHand(InteractionHand.MAIN_HAND,wand);var before=wand.copy();WandCooldowns.clear(player);wand.use(level,player,InteractionHand.MAIN_HAND);
                var shards=level.getEntitiesOfClass(FrostShardEntity.class,player.getBoundingBox().inflate(5));
                test.assertTrue(shards.size()==(mode==1?5+potency*2:1),"Actual wand mode count");
                for(var shard:shards){float damage=mode==1?1:mode==2?4+potency*2:3+potency*1.5f;test.assertTrue(shard.damage()==damage&&shard.frost()==1&&shard.getOwner()==player,"Actual damage, frost upgrade and caster");test.assertTrue(Math.abs(shard.getBbWidth()-(.15f+damage*.15f))<.00001,"Damage controls collision size");shard.discard();}
                for(var aspect:tc4revival.aspect.TCAspects.catalog().primals()){int cost=switch(aspect.tag()){case "aqua"->5;case "ignis","perditio"->2;default->0;};test.assertTrue(wandItem.vis(before,aspect)-wandItem.vis(wand,aspect)==cost,"Old top-level wand cost preserved for each mode");}
                test.assertTrue(item.cooldownMillis(core)==(mode==0?200:500),"Mode activation cooldown");
            }
            tc4revival.TC4Revival.LOGGER.info("PASS frostThreeModesConserveLegacyCostsAndParameters: six mode/potency casts, ownership, size, actual legacy costs and cooldown definitions");test.succeed();
        }finally{level.getEntitiesOfClass(FrostShardEntity.class,player.getBoundingBox().inflate(5)).forEach(Entity::discard);WandCooldowns.clear(player);if(forced)level.setChunkForced(chunk.x,chunk.z,false);}
    }
    @GameTest(template="wand_test_empty")
    public static void frostSixFacesBounceAndReloadKeepLegacyRules(GameTestHelper test) {
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(3,10,3));var owner=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"FrostBounce"));owner.moveTo(Vec3.atCenterOf(pos));level.setBlock(pos,Blocks.STONE.defaultBlockState(),2);
        try {
            for(boolean boulder:new boolean[]{false,true})for(var side:Direction.values()) {
                var shard=new FrostShardEntity(level,owner,1,4,2,false,boulder);var start=Vec3.atCenterOf(pos);var incoming=new Vec3(1,2,3);shard.setPos(start);shard.setDeltaMovement(incoming);
                var expected=switch(side.getAxis()){case X->new Vec3(-1,2,3);case Y->new Vec3(1,-1.8,3);case Z->new Vec3(1,2,-3);};expected=expected.scale(boulder?.8:.5);
                var hit=new BlockHitResult(start,side,pos,false);shard.onHit(hit);test.assertTrue(shard.getDeltaMovement().distanceTo(expected)<.000001,"All six reflection faces and mode damping");test.assertTrue(shard.position().distanceTo(start.subtract(expected.normalize().scale(.05)))<.000001,"Legacy reverse motion contact offset");
                int limit=boulder?6:3;for(int count=1;count<limit;count++)shard.onHit(hit);test.assertTrue(!shard.isRemoved(),"Post decrement survives configured impact count");shard.onHit(hit);test.assertTrue(shard.isRemoved(),"Next impact consumes shard");
            }
            for(boolean fragile:new boolean[]{false,true}) {
                var source=new FrostShardEntity(level,owner,1,8,2,fragile,true);var tag=new CompoundTag();source.saveWithoutId(tag);var loaded=new FrostShardEntity(TCEntities.FROST_SHARD.get(),level);loaded.load(tag);
                test.assertTrue(loaded.damage()==8&&loaded.frost()==2&&Math.abs(loaded.getBbWidth()-1.35)<.00001,"Reload restores data and collision size");
                var payload=new FriendlyByteBuf(Unpooled.buffer());try{source.writeSpawnData(payload);test.assertTrue(payload.readableBytes()==13&&payload.readDouble()==.8&&payload.readInt()==6&&payload.readBoolean()==fragile,"Exact legacy spawn fields");payload.clear();loaded.writeSpawnData(payload);test.assertTrue(payload.readDouble()==.8&&payload.readInt()==6&&payload.readBoolean()==fragile,"NBT preserves bounce settings");}finally{payload.release();}
            }
            tc4revival.TC4Revival.LOGGER.info("PASS frostSixFacesBounceAndReloadKeepLegacyRules: 12 face/mode reflections, contact offset, bounce limits and two NBT/spawn payload cases");test.succeed();
        }finally{level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);}
    }
}
