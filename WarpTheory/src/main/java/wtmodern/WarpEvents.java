package wtmodern;

import java.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.effect.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import com.mojang.brigadier.arguments.StringArgumentType;
import tc4revival.research.PlayerKnowledge;

public class WarpEvents {
    public static void message(ServerPlayer p,String key){p.sendSystemMessage(Component.translatable("chat.warptheory."+key).withStyle(ChatFormatting.DARK_PURPLE,ChatFormatting.ITALIC));}
    @SubscribeEvent public static void clone(PlayerEvent.Clone e){
        // Active/queued effects belonged to the old player entity in the original mod.
        // End-return cloning is not a death and must retain its remaining effects.
        if(e.isWasDeath())e.getEntity().getPersistentData().remove("WarpTheory");
        else if(e.getOriginal().getPersistentData().contains("WarpTheory"))
            e.getEntity().getPersistentData().put("WarpTheory",e.getOriginal().getPersistentData().getCompound("WarpTheory").copy());
    }
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent e){
        if(!e.isEndConquered()&&e.getEntity() instanceof ServerPlayer p)WarpNetwork.send(p,"clear",BlockPos.ZERO,0);
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent e){if(e.getEntity() instanceof ServerPlayer p)WarpNetwork.send(p,"ears",BlockPos.ZERO,WarpState.tag(p).getInt("ears"));}
    @SubscribeEvent public static void definitions(net.minecraftforge.event.OnDatapackSyncEvent e){var players=e.getPlayer()==null?e.getPlayerList().getPlayers():List.of(e.getPlayer());for(var p:players)WarpNetwork.send(p,"easy_recipes",BlockPos.ZERO,WarpConfig.EASY.get()?1:0);}
    @SubscribeEvent public static void commands(RegisterCommandsEvent e){e.getDispatcher().register(Commands.literal("warptheory").requires(s->s.hasPermission(2)).then(Commands.literal("event").then(Commands.argument("players",EntityArgument.players()).then(Commands.argument("event",StringArgumentType.word()).suggests((c,b)->{WarpRules.EVENTS.forEach(x->b.suggest(x.id()));return b.buildFuture();}).executes(c->{String id=StringArgumentType.getString(c,"event");if(WarpRules.find(id)==null)return 0;int n=0;for(var p:EntityArgument.getPlayers(c,"players"))if(start(p,id))n++;return n;})))).then(Commands.literal("clear").then(Commands.argument("players",EntityArgument.players()).executes(c->{for(var p:EntityArgument.getPlayers(c,"players")){WarpState.tag(p).getAllKeys().removeIf(k->!k.equals("fallReturn")&&!k.equals("fallUntil"));WarpNetwork.send(p,"clear",BlockPos.ZERO,0);}return 1;}))));}
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event){
        if(event.phase!=TickEvent.Phase.END||!(event.player instanceof ServerPlayer p)||!p.isAlive())return;
        tickPlayer(p);
    }
    /**
     * Warp ward, the wuss/disable switch, creative mode and the addon master switch must also stop already queued
     * events; the original author only filtered new queueing, so leftovers fired through protection.
     */
    public static boolean effectsAllowed(ServerPlayer p){
        return WarpConfig.ENABLED.get()&&tc4revival.content.WarpEvents.effectsAllowed(p);
    }
    public static void tickPlayer(ServerPlayer p){
        if(WarpState.knowledge(p).totalWarp()<=0){
            // Keep the safety return for a fall that has already begun. Everything
            // else, including the queue, is discarded when the player is cleansed.
            var state=WarpState.tag(p);
            boolean clearing=state.getAllKeys().stream().anyMatch(k->!k.equals("fallReturn")&&!k.equals("fallUntil"));
            state.getAllKeys().removeIf(k->!k.equals("fallReturn")&&!k.equals("fallUntil"));
            if(clearing)WarpNetwork.send(p,"clear",BlockPos.ZERO,0);
            returnFromFall(p);
            return;
        }
        if(p.isSpectator()){returnFromFall(p);return;}
        // Queued events are held rather than dropped while protection is active, matching the maintained fork.
        if(effectsAllowed(p)){
            if(p.tickCount>0&&p.tickCount%2000==0){
                int warp=WarpState.total(p);if(warp>0&&p.getRandom().nextInt(100)<=Math.sqrt(warp)){var e=WarpState.queueOne(p,warp);if(e!=null)WarpState.remove(p,Math.min(e.cost(),WarpState.knowledge(p).warp(PlayerKnowledge.WarpKind.TEMPORARY)));}
            }
            if(p.tickCount%20==0&&p.getRandom().nextBoolean()){var id=WarpState.dequeue(p);if(id!=null)start(p,id);}
        }
        runActive(p);
    }
    public static boolean start(ServerPlayer p,String id){
        if(!effectsAllowed(p))return false;
        var r=p.getRandom();var level=p.serverLevel();var t=WarpState.tag(p);
        if((id.equals("decay")&&t.contains("swamp"))||(id.equals("swamp")&&t.contains("decay"))||(id.equals("fall")&&t.contains("fallReturn")))return false;
        switch(id){
            case "poison","nausea","jump","blind"->{MobEffect effect=switch(id){case "poison"->MobEffects.POISON;case "nausea"->MobEffects.CONFUSION;case "jump"->MobEffects.JUMP;default->MobEffects.BLINDNESS;};var old=p.getEffect(effect);var next=new MobEffectInstance(effect,400+(old==null?0:old.getDuration()),id.equals("jump")?20:0);next.setCurativeItems(new ArrayList<>());p.addEffect(next);}
            case "bats"->WarpState.add(p,id,15+r.nextInt(30));
            case "blink"->WarpState.add(p,id,10+r.nextInt(20));
            case "decay"->WarpState.add(p,id,512+r.nextInt(256));
            case "swamp"->WarpState.add(p,id,256+r.nextInt(256));
            case "ears"->{int amount=10+r.nextInt(30);WarpState.add(p,id,amount);WarpNetwork.send(p,"ears",BlockPos.ZERO,t.getInt("ears"));}
            case "tongue"->WarpState.add(p,id,10+r.nextInt(15));
            case "friend","wither"->WarpState.add(p,id,1);
            case "livestock","wind","lightning"->WarpState.add(p,id,5+r.nextInt(10));
            case "chests"->WarpState.add(p,id,15+r.nextInt(30));
            case "blood"->WarpState.add(p,id,64+r.nextInt(128));
            case "acceleration"->WarpState.add(p,id,6000+r.nextInt(12000));
            case "fall"->beginFall(p);
            case "rain"->{if(!startRain(level))return false;}
            case "fakeexplosion","fakecreeper"->{WarpState.add(p,id,1);return true;}
            default->{return false;}
        }
        message(p,id);return true;
    }
    public static void runActive(ServerPlayer p){
        if(!p.isAlive())return;
        returnFromFall(p);
        if(p.isSpectator()||WarpState.knowledge(p).totalWarp()<=0)return;
        var t=WarpState.tag(p);var level=p.serverLevel();var r=p.getRandom();
        for(String id:List.copyOf(t.getAllKeys())){
            boolean done=false;
            switch(id){
                case "acceleration"->{level.setDayTime(level.getDayTime()+2);done=true;}
                case "fakeexplosion","fakecreeper"->{if(level.getGameTime()%20==0){var pos=fakeSoundPosition(p,id);level.playSound(null,pos.x,pos.y,pos.z,id.equals("fakeexplosion")?SoundEvents.GENERIC_EXPLODE:SoundEvents.CREEPER_PRIMED,SoundSource.HOSTILE,1,1);done=true;}}
                case "bats","friend","wither","livestock"->done=spawn(p,id);
                case "blink"->{if(level.getGameTime()%20==0)done=blink(p);}
                case "wind"->{if(level.getGameTime()%20==0&&r.nextBoolean()){p.push(r.nextDouble()-r.nextDouble(),r.nextDouble(),r.nextDouble()-r.nextDouble());p.connection.send(new ClientboundSetEntityMotionPacket(p));done=true;}}
                case "lightning"->{var pos=p.blockPosition().offset(r.nextInt(3)-r.nextInt(3),0,r.nextInt(3)-r.nextInt(3));if(r.nextInt(100)==0&&level.canSeeSky(pos)){lightning(level,pos);done=true;}}
                case "chests"->{if(level.getGameTime()%10==0)done=chests(p);}
                case "decay"->done=decay(p);
                case "swamp"->done=swamp(p);
                case "blood"->{for(int i=0;i<6&&t.contains(id);i++){var pos=random(p,8);if(level.hasChunkAt(pos)&&level.isEmptyBlock(pos.below())&&!level.getBlockState(pos).isAir()&&level.getBlockState(pos).blocksMotion()){WarpNetwork.send(p,"blood",pos.above(),0);WarpState.decrement(p,id,1);if(!t.contains(id))WarpNetwork.send(p,"clear_blood",BlockPos.ZERO,0);}}}
            }
            if(done){WarpState.decrement(p,id,1);if(id.equals("blood")&&!t.contains(id))WarpNetwork.send(p,"clear_blood",BlockPos.ZERO,0);}
        }
    }
    private static void returnFromFall(ServerPlayer p){
        var t=WarpState.tag(p);
        if(t.contains("fallReturn")&&p.level().getGameTime()>=t.getLong("fallUntil")){
            var pos=t.getCompound("fallReturn");p.teleportTo(pos.getDouble("x"),pos.getDouble("y"),pos.getDouble("z"));p.fallDistance=0;
            t.remove("fallReturn");t.remove("fallUntil");blinkParticles(p);
        }
    }
    private static BlockPos random(ServerPlayer p,int range){var r=p.getRandom();return p.blockPosition().offset(r.nextInt(range)-r.nextInt(range),r.nextInt(range)-r.nextInt(range),r.nextInt(range)-r.nextInt(range));}
    /** Preserve the original author's sound positions, including its non-normalized head-yaw formula. */
    public static Vec3 fakeSoundPosition(ServerPlayer p,String id){if(id.equals("fakeexplosion")){var r=p.getRandom();return new Vec3((int)p.getX()+r.nextInt(16)-8,(int)p.getY()+r.nextInt(16)-8,(int)p.getZ()+r.nextInt(16)-8);}double yaw=p.getYHeadRot(),distance=2*Math.sin(Math.toRadians(90-yaw));return new Vec3(p.getX()-distance*Math.sin(Math.toRadians(yaw)),p.getY(),p.getZ()-distance*Math.cos(Math.toRadians(yaw)));}
    private static boolean spawn(ServerPlayer p,String id){
        var level=p.serverLevel();var r=p.getRandom();
        for(int i=0;i<6;i++){
            var pos=random(p,id.equals("friend")||id.equals("wither")?4:8);if(!level.hasChunkAt(pos)||!level.isEmptyBlock(pos))continue;
            if(!id.equals("bats")&&!level.isEmptyBlock(pos.above()))continue;
            if(id.equals("livestock")){boolean clear=true;for(int y=0;y<=25;y++)if(!level.isEmptyBlock(pos.above(y))){clear=false;break;}if(!clear)continue;pos=pos.above(25);}
            Entity entity=switch(id){case "bats"->EntityType.BAT.create(level);case "friend"->Content.FRIEND.get().create(level);case "wither"->EntityType.WITHER.create(level);default->livestockType(r).create(level);};
            if(entity==null)continue;
            entity.moveTo(pos.getX()+r.nextDouble(),pos.getY()+r.nextDouble(),pos.getZ()+r.nextDouble(),r.nextFloat()*360,0);
            if(id.equals("wither")){if(!level.getBlockState(pos.below()).blocksMotion()||!level.noCollision(entity))continue;((WitherBoss)entity).makeInvulnerable();lightning(level,pos);level.playSound(null,pos,SoundEvents.GENERIC_EXPLODE,SoundSource.HOSTILE,4,(1+(r.nextFloat()-r.nextFloat())*.2f)*.7f);level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,entity.getX(),entity.getY(),entity.getZ(),1,0,0,0,0);}
            if(id.equals("friend")){entity.setCustomName(Component.literal(FriendNames.compose(r)));((Mob)entity).setPersistenceRequired();}
            if(level.addFreshEntity(entity)){if(entity instanceof Mob m)m.playAmbientSound();return true;}
        }return false;
    }
    private static void lightning(ServerLevel level,BlockPos pos){var bolt=EntityType.LIGHTNING_BOLT.create(level);if(bolt!=null){bolt.moveTo(Vec3.atBottomCenterOf(pos));level.addFreshEntity(bolt);}}
    private static boolean blink(ServerPlayer p){var l=p.serverLevel();var r=p.getRandom();for(int i=0;i<8;i++){var pos=random(p,16);if(l.hasChunkAt(pos)&&l.isEmptyBlock(pos)&&l.isEmptyBlock(pos.above())&&l.getBlockState(pos.below()).blocksMotion()){p.teleportTo(l,pos.getX()+r.nextDouble(),pos.getY(),pos.getZ()+r.nextDouble(),(r.nextFloat()-r.nextFloat())*360,(r.nextFloat()-r.nextFloat())*90);blinkParticles(p);return true;}}return false;}
    private static void blinkParticles(ServerPlayer p){WarpNetwork.blink(p);p.level().playSound(null,p.blockPosition(),SoundEvents.ENDERMAN_TELEPORT,SoundSource.PLAYERS,1,1);}
    private static void beginFall(ServerPlayer p){var l=p.serverLevel();var t=WarpState.tag(p);var back=new net.minecraft.nbt.CompoundTag();back.putDouble("x",p.getX());back.putDouble("y",p.getY());back.putDouble("z",p.getZ());t.put("fallReturn",back);long until=l.getGameTime()+80;t.putLong("fallUntil",until);var origin=p.blockPosition();for(int x=origin.getX()-5;x<origin.getX()+5;x++)for(int z=origin.getZ()-5;z<origin.getZ()+5;z++)for(int y=l.getMinBuildHeight();y<l.getMaxBuildHeight();y++){var pos=new BlockPos(x,y,z);if(l.hasChunkAt(pos))VanishingBlock.hide(l,pos,until);}l.playSound(null,p.getX(),p.getY(),p.getZ(),SoundEvents.ENDERMAN_TELEPORT,SoundSource.PLAYERS,1,1);}
    /** The author's unreachable chicken default is retained: only three choices are rolled. */
    public static EntityType<?> livestockType(net.minecraft.util.RandomSource random){return List.of(EntityType.COW,EntityType.PIG,EntityType.SHEEP).get(random.nextInt(3));}
    public static boolean startRain(ServerLevel level){
        var weather=(net.minecraft.world.level.storage.ServerLevelData)level.getLevelData();
        if(weather.isThundering())return false;
        weather.setRaining(true);weather.setThundering(true);
        return true;
    }
    public static List<ChestBlockEntity> nearbyChests(ServerLevel level,BlockPos center){
        var found=new ArrayList<ChestBlockEntity>();
        for(var pos:BlockPos.betweenClosed(center.offset(-8,-8,-8),center.offset(8,8,8)))
            if(pos.distSqr(center)<=64&&level.hasChunkAt(pos)&&level.getBlockEntity(pos) instanceof ChestBlockEntity chest)found.add(chest);
        return found;
    }
    public static boolean shuffleChestContents(net.minecraft.util.RandomSource random,net.minecraft.world.Container first,net.minecraft.world.Container second){
        if(first.getContainerSize()==0||second.getContainerSize()==0)return false;
        int a=random.nextInt(first.getContainerSize()),b=random.nextInt(second.getContainerSize());
        var firstStack=first.getItem(a);var secondStack=second.getItem(b);
        if(!first.canPlaceItem(a,secondStack)||!second.canPlaceItem(b,firstStack))return false;
        first.setItem(a,secondStack);second.setItem(b,firstStack);first.setChanged();second.setChanged();
        return !firstStack.isEmpty()||!secondStack.isEmpty();
    }
    public static void shuffleChests(net.minecraft.util.RandomSource random,net.minecraft.world.Container first,net.minecraft.world.Container second){
        if(!shuffleChestContents(random,first,second))shuffleChestContents(random,first,second);
    }
    private static boolean chests(ServerPlayer p){
        var level=p.serverLevel();var chests=nearbyChests(level,p.blockPosition());if(chests.isEmpty())return false;
        var random=p.getRandom();var first=chests.get(random.nextInt(chests.size()));var second=chests.get(random.nextInt(chests.size()));
        boolean sounded=random.nextInt(10)==0;
        if(sounded){boolean opening=random.nextBoolean();var pos=(opening?first:second).getBlockPos();level.playSound(null,pos.getX(),pos.getY()+.5,pos.getZ(),opening?SoundEvents.CHEST_OPEN:SoundEvents.CHEST_CLOSE,SoundSource.BLOCKS,.5f,.9f+random.nextFloat()*.1f);}
        shuffleChests(random,first,second);return sounded;
    }
    private static boolean exposed(ServerLevel level,BlockPos pos){for(var d:Direction.values())if(!level.getBlockState(pos.relative(d)).blocksMotion())return true;return false;}
    public static BlockState decayState(BlockState old){
        var mapped=decayMappingState(old);
        if(mapped!=null)return mapped;
        return old.is(BlockTags.LEAVES)||old.getBlock() instanceof net.minecraftforge.common.IPlantable?Blocks.AIR.defaultBlockState():null;
    }
    /** Plants/leaves are the original decay event's fallback, not entries in its terrain map. */
    public static BlockState decayMappingState(BlockState old){
        var block=old.getBlock();Block to=null;
        if(old.is(BlockTags.LOGS)||block==Blocks.BROWN_MUSHROOM_BLOCK||block==Blocks.RED_MUSHROOM_BLOCK)to=Blocks.DIRT;
        else if(block==Blocks.GRASS_BLOCK||block==Blocks.PODZOL)to=Blocks.DIRT;
        else if(block==Blocks.DIRT||block==Blocks.COARSE_DIRT||block==Blocks.SANDSTONE||block==Blocks.GRAVEL||block==Blocks.CLAY||block==Blocks.SOUL_SAND)to=Blocks.SAND;
        else if(block==Blocks.STONE||block==Blocks.LAVA||block==Blocks.NETHERRACK||block==Blocks.END_STONE||block==Blocks.OBSIDIAN||block==Blocks.GLOWSTONE)to=Blocks.COBBLESTONE;
        else if(block==Blocks.COBBLESTONE)to=Blocks.GRAVEL;
        else if(block==Blocks.SAND||block==Blocks.RED_SAND||block==Blocks.WATER||block==Blocks.SNOW)to=Blocks.AIR;
        else if(block==Blocks.ICE||block==Blocks.SNOW_BLOCK)to=Blocks.WATER;
        else if(block==Blocks.MYCELIUM)to=Blocks.GRASS_BLOCK;
        else if(old.is(BlockTags.TERRACOTTA))to=block==Blocks.TERRACOTTA?Blocks.CLAY:Blocks.TERRACOTTA;
        else if(block==Blocks.NETHER_QUARTZ_ORE)to=Blocks.NETHERRACK;
        else if(old.is(BlockTags.COAL_ORES)||old.is(BlockTags.DIAMOND_ORES)||old.is(BlockTags.EMERALD_ORES)||old.is(BlockTags.GOLD_ORES)||old.is(BlockTags.IRON_ORES)||old.is(BlockTags.LAPIS_ORES)||old.is(BlockTags.REDSTONE_ORES))to=Blocks.STONE;
        return to==null?null:to.defaultBlockState();
    }
    private static boolean decay(ServerPlayer p){var l=p.serverLevel();var pos=random(p,8);if(!l.hasChunkAt(pos)||!exposed(l,pos)||l.getBlockEntity(pos)!=null)return false;var old=l.getBlockState(pos);var replacement=decayState(old);if(replacement==null)return false;boolean result=l.setBlock(pos,replacement,3);if(result&&replacement.isAir())l.levelEvent(2001,pos,Block.getId(old));return result;}
    private static boolean swamp(ServerPlayer p){return swampAt(p.serverLevel(),random(p,8),p.getRandom());}
    public static boolean swampAt(ServerLevel l,BlockPos pos,net.minecraft.util.RandomSource r){
        if(!l.hasChunkAt(pos)||!exposed(l,pos)||l.getBlockEntity(pos)!=null)return false;var old=l.getBlockState(pos);var up=pos.above();
        if(old.is(Blocks.WATER)&&l.isEmptyBlock(up))return l.setBlock(up,Blocks.LILY_PAD.defaultBlockState(),3);
        if(old.getBlock() instanceof SaplingBlock sapling){sapling.advanceTree(l,pos,old.setValue(SaplingBlock.STAGE,1),r);return true;}
        if(old.is(BlockTags.LEAVES)||old.is(BlockTags.LOGS))return growSwampVine(l,pos,r);
        if(r.nextBoolean()&&old.canSustainPlant(l,pos,Direction.UP,(net.minecraftforge.common.IPlantable)Blocks.OAK_SAPLING)){
            if(r.nextBoolean()){
                var above=l.getBlockState(up);
                if(above.isAir()||above.getBlock() instanceof net.minecraftforge.common.IPlantable&&!(above.getBlock() instanceof SaplingBlock)){
                    var sapling=List.of(Blocks.OAK_SAPLING,Blocks.SPRUCE_SAPLING,Blocks.BIRCH_SAPLING,Blocks.JUNGLE_SAPLING,Blocks.ACACIA_SAPLING,Blocks.DARK_OAK_SAPLING).get(r.nextInt(6));l.setBlock(up,sapling.defaultBlockState(),3);
                }
            }else if(r.nextBoolean()){
                if(l.isEmptyBlock(up)&&old.getBlock() instanceof BonemealableBlock grow)grow.performBonemeal(l,r,pos,old);
            }else if(l.isEmptyBlock(up)&&old.is(Blocks.GRASS_BLOCK))l.setBlock(pos,Blocks.PODZOL.defaultBlockState(),3);
            // Original counts this branch even when its space/soil check prevents a visible change.
            return true;
        }
        if(r.nextBoolean()&&canTurnToSwampWater(l,pos))return l.setBlock(pos,l.getBlockState(up).is(BlockTags.LOGS)?l.getBlockState(up):Blocks.WATER.defaultBlockState(),3);
        if(decayMappingState(old)!=null&&old.isSolid()&&!old.is(BlockTags.LOGS)&&!old.is(Blocks.DIRT)&&!old.is(Blocks.GRASS_BLOCK))return l.setBlock(pos,l.getBlockState(up).isSolid()?Blocks.DIRT.defaultBlockState():(r.nextBoolean()?Blocks.GRASS_BLOCK:Blocks.PODZOL).defaultBlockState(),3);
        return false;
    }
    public static boolean growSwampVine(ServerLevel level,BlockPos origin,net.minecraft.util.RandomSource random){
        var target=origin.mutable();
        for(int attempt=0;attempt<6;attempt++){
            var side=Direction.from3DDataValue(2+random.nextInt(4));
            // Old canPlaceBlockOnSide checks the support behind the current coordinate,
            // BEFORE BlockCoord.offset mutates it toward the attempted placement side.
            var support=target.relative(side.getOpposite());var state=level.getBlockState(support);
            if(!state.isSolidRender(level,support)||!state.blocksMotion())continue;
            target.move(side);
            // Preserve the ordinary old bug: an occupied destination still moves the
            // coordinate used by all subsequent attempts in this event tick.
            if(level.isEmptyBlock(target)){
                var vine=Blocks.VINE.defaultBlockState().setValue(VineBlock.getPropertyForFace(side.getOpposite()),true);
                level.setBlock(target,vine,3);return true;
            }
        }
        return false;
    }
    public static boolean canTurnToSwampWater(ServerLevel level,BlockPos pos){
        var old=level.getBlockState(pos);var cover=level.getBlockState(pos.above());
        if(!(old.isAir()||decayMappingState(old)!=null)||!(cover.isAir()||cover.is(BlockTags.LOGS)||cover.getBlock() instanceof net.minecraftforge.common.IPlantable))return false;
        for(var direction:Direction.values())if(direction!=Direction.UP){var state=level.getBlockState(pos.relative(direction));if(!state.isSolidRender(level,pos.relative(direction))&&!state.is(Blocks.WATER))return false;}
        return true;
    }
    public static String garble(String message,net.minecraft.util.RandomSource random){var words=new ArrayList<String>();for(String word:message.split("\\s+")){var chars=new StringBuilder();for(int offset=0;offset<word.length();){int code=word.codePointAt(offset);offset+=Character.charCount(code);String c=new String(Character.toChars(code));if(random.nextInt(5)==0)c="\u00a7k"+c+"\u00a7r";if(random.nextBoolean())chars.append(c);else chars.insert(0,c);}if(random.nextBoolean())words.add(chars.toString());else words.add(0,chars.toString());}return String.join(" ",words);}
    @SubscribeEvent public static void chat(ServerChatEvent e){var p=e.getPlayer();if(WarpState.tag(p).getInt("tongue")>0){e.setMessage(Component.literal(garble(e.getRawText(),p.getRandom())));WarpState.decrement(p,"tongue",1);}}
}
