package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import tc4revival.network.FocusEffectPacket;

/** The four old boss room transformations, selected by the world's persistent encounter count. */
public  class EldritchBossEncounter {
    private final ServerLevel level;
    private final BlockPos lock;
    private final int x,z,dx,dz;
    private final java.util.List<Mob> entities=new java.util.ArrayList<>();
    private EldritchBossEncounter(ServerLevel level,BlockPos lock,int cx,int cz,int exit) {
        this.level=level; this.lock=lock; x=cx*16+16; z=cz*16+16;
        dx=exit==2 || exit==4 ? 8 : exit==3 || exit==5 ? -8 : 0;
        dz=exit==2 || exit==3 ? 8 : exit==4 || exit==5 ? -8 : 0;
    }
    public static void open(ServerLevel level,BlockPos lock,int kind) {
        if (!level.getBlockState(lock).is(TCEldritch.LOCK.get())) return;
        if (kind<0 || kind>3) throw new IllegalArgumentException("Invalid eldritch boss kind");
        var data=EldritchMazeSavedData.get(level);
        int cx=lock.getX()>>4,cz=lock.getZ()>>4,centerX=cx,centerZ=cz,exit=0;
        for (int a=-2;a<=2;a++) for (int b=-2;b<=2;b++) {
            var found=data.cell(cx+a,cz+b); if (found.isEmpty()) continue;
            var cell=found.get();
            if (cell.feature()==2) { centerX=cx+a; centerZ=cz+b; }
            if (cell.feature()>=2 && cell.feature()<=5 && (cell.passages()&15)!=0) exit=cell.feature();
        }
        var encounter=new EldritchBossEncounter(level,lock,centerX,centerZ,exit);
        String name=switch(kind) { case 0->"golem"; case 1->"warden"; case 2->"crimson"; default->"taint"; };
        for (var player:level.players()) if (player.distanceToSqr(lock.getX(),lock.getY(),lock.getZ())<300)
            player.sendSystemMessage(Component.translatable("tc.boss."+name));
        switch(kind) { case 0->encounter.golem(); case 1->encounter.warden(); case 2->encounter.cultists(); case 3->encounter.taint(); }
        var gates=new java.util.LinkedHashMap<BlockPos,GateState>();
        for (BlockPos cursor:BlockPos.betweenClosed(lock.offset(-2,-2,-2),lock.offset(2,2,2))) {
            BlockPos pos=cursor.immutable(); BlockState state=level.getBlockState(pos);
            if (!pos.equals(lock) && !state.is(TCEldritch.DOOR_BARRIER.get()) && !state.is(TCEldritch.FIELD.get())
                    && !state.is(TCArcane.AURA_NODE.get()) && !state.is(TCArcane.NITOR.get())) continue;
            var entity=level.getBlockEntity(pos);
            gates.put(pos,new GateState(state,entity==null ? null : entity.saveWithFullMetadata()));
        }
        try {
            for (Mob entity:encounter.entities) if (!level.addFreshEntity(entity)) throw new IllegalStateException("Boss encounter entity rejected");
            for (BlockPos pos:gates.keySet()) if (!pos.equals(lock)) removeGate(level,pos);
            // Keep the ticking lock until every other part of the opening has succeeded.
            removeGate(level,lock);
        } catch (RuntimeException failure) {
            for (Mob entity:encounter.entities) entity.discard();
            for (var entry:gates.entrySet()) {
                try { entry.getValue().restore(level,entry.getKey()); }
                catch (RuntimeException rollback) { failure.addSuppressed(rollback); }
            }
            throw failure;
        }
        for (BlockPos pos:gates.keySet()) if (!pos.equals(lock)) FocusEffectPacket.blockSparkle(level,pos,0x400040);
    }
    private static void removeGate(ServerLevel level,BlockPos pos) {
        if (!level.getBlockState(pos).isAir() && !level.removeBlock(pos,false))
            throw new IllegalStateException("Boss gate removal failed at "+pos);
    }
    private record GateState(BlockState state,CompoundTag tag) {
        void restore(ServerLevel level,BlockPos pos) {
            if (!level.getBlockState(pos).equals(state) && !level.setBlock(pos,state,18))
                throw new IllegalStateException("Boss gate restoration failed at "+pos);
            if (tag!=null) {
                var entity=level.getBlockEntity(pos);
                if (entity==null) throw new IllegalStateException("Boss gate block entity missing at "+pos);
                entity.load(tag.copy()); entity.setChanged();
                level.sendBlockUpdated(pos,state,state,2);
            }
        }
    }
    private void block(int worldX,int worldY,int worldZ,Block block) { block(worldX,worldY,worldZ,block.defaultBlockState()); }
    private void block(int worldX,int worldY,int worldZ,BlockState state) {
        BlockPos pos=new BlockPos(worldX,worldY,worldZ);
        if (!level.getBlockState(pos).equals(state) && !level.setBlock(pos,state,3)) throw new IllegalStateException("Boss-room block write failed at "+pos);
    }
    private void obelisk(int worldX,int worldZ) {
        block(worldX,54,worldZ,TCEldritch.OBELISK.get());
        for (int y=55;y<=58;y++) block(worldX,y,worldZ,TCEldritch.OBELISK_SEGMENT.get());
        block(worldX,52,worldZ,TCEldritch.CAP.get());
    }
    private void pedestal(int worldX,int worldZ) {
        int[][] pattern={{2,2,2},{0,-1,1},{3,3,3}};
        Direction[] directions={Direction.EAST,Direction.WEST,Direction.SOUTH,Direction.NORTH};
        for (int row=0;row<3;row++) for (int col=0;col<3;col++) {
            int value=pattern[row][col];
            block(worldX-1+col,52,worldZ-1+row,value<0 ? TCEldritch.GLOWING_STONE.get().defaultBlockState()
                    : TCEldritch.STAIRS.get().defaultBlockState().setValue(StairBlock.FACING,directions[value]));
        }
    }
    private int rarity(float rare,float uncommon) { float roll=level.random.nextFloat(); return roll<rare ? 2 : roll<uncommon ? 1 : 0; }
    private void faceLock(Mob mob,double worldX,double worldY,double worldZ) {
        double vx=lock.getX()-worldX,vy=lock.getY()-(worldY+mob.getEyeHeight()),vz=lock.getZ()-worldZ;
        mob.moveTo(worldX,worldY,worldZ,(float)(Math.toDegrees(Math.atan2(vz,vx))-90),
                (float)-Math.toDegrees(Math.atan2(vy,Math.sqrt(vx*vx+vz*vz))));
        mob.finalizeSpawn(level,level.getCurrentDifficultyAt(mob.blockPosition()),MobSpawnType.EVENT,null,null);
    }
    private void warden() {
        int farX=x+dx,farZ=z+dz;
        obelisk(farX,z); obelisk(x,farZ);
        for (int a=-1;a<=1;a++) for (int b=-1;b<=1;b++) {
            if (a!=0 && b!=0 && level.random.nextFloat()<.9f) block(farX+a,52,z+b,TCEldritch.loot(false,rarity(.1f,.3f)));
            if (a!=0 && b!=0 && level.random.nextFloat()<.9f) block(x+a,52,farZ+b,TCEldritch.loot(false,rarity(.1f,.3f)));
        }
        for (int a:new int[]{-2,2}) for (int b:new int[]{-2,2}) {
            block(x+a,53,z+b,TCEldritch.TRAP.get()); block(x+a,52,z+b,TCEldritch.PEDESTAL_STONE.get());
        }
        pedestal(farX,farZ);
        var boss=TCEntities.ELDRITCH_WARDEN.get().create(level);
        if (boss==null) throw new IllegalStateException("Missing eldritch warden");
        faceLock(boss,farX+.5,53,farZ+.5); boss.restrictTo(new BlockPos(x,52,z),32); entities.add(boss);
    }
    private void golem() {
        obelisk(x+dx,z+dz); obelisk(x-dx,z+dz); obelisk(x+dx,z-dz); pedestal(x,z);
        for (int a=-10;a<=10;a++) for (int b=-10;b<=10;b++) {
            if (Math.abs(a)<=2 || Math.abs(b)<=2 || level.random.nextFloat()>=.15f || !level.isEmptyBlock(new BlockPos(x+a,52,z+b))) continue;
            int rarity=rarity(.05f,.2f); block(x+a,52,z+b,TCEldritch.loot(level.random.nextFloat()<.3f,rarity));
        }
        var boss=TCEntities.ELDRITCH_GOLEM.get().create(level);
        if (boss==null) throw new IllegalStateException("Missing eldritch golem");
        faceLock(boss,x+.5,53,z+.5); entities.add(boss);
    }
    private void cultists() {
        for (int a=-4;a<=4;a++) for (int b=-4;b<=4;b++) {
            if ((Math.abs(a)==2 || Math.abs(b)==2) && level.random.nextBoolean()
                    || (Math.abs(a)==3 || Math.abs(b)==3) && level.random.nextFloat()>.33f
                    || (Math.abs(a)==4 || Math.abs(b)==4) && level.random.nextFloat()>.25f) continue;
            block(x+b,51,z+a,TCEldritch.DOOR_FRAME.get());
        }
        for (int a=0;a<5;a++) for (int b=0;b<5;b++) {
            if (a!=0 && a!=4 && b!=0 && b!=4) continue;
            int px=x-8+b*4,pz=z-8+a*4;
            block(px,52,pz,TCEldritch.STONE.get()); block(px,53,pz,TCEldritch.GLYPHED_STONE.get()); block(px,54,pz,TCEldritch.SLAB.get());
            block(px,60,pz,TCEldritch.STONE.get()); block(px,59,pz,TCEldritch.GLYPHED_STONE.get());
            block(px,58,pz,TCEldritch.SLAB.get().defaultBlockState().setValue(SlabBlock.TYPE,SlabType.TOP));
        }
        var portal=TCEntities.CULTIST_PORTAL.get().create(level);
        if (portal==null) throw new IllegalStateException("Missing cultist portal");
        portal.moveTo(x+.5,52,z+.5,0,0); entities.add(portal);
    }
    private boolean adjacentSolid(BlockPos pos) {
        for (Direction side:Direction.values()) { BlockPos neighbor=pos.relative(side); if (level.getBlockState(neighbor).isSolidRender(level,neighbor)) return true; }
        return false;
    }
    private void taint() {
        for (int a=-12;a<=12;a++) for (int b=-12;b<=12;b++) {
            WorldBiomeChanges.changeColumn(level,new BlockPos(x+b,52,z+a),TCBiomes.TAINTED_LAND);
            for (int c=0;c<9;c++) {
                BlockPos pos=new BlockPos(x+b,52+c,z+a);
                if (!level.isEmptyBlock(pos) || !adjacentSolid(pos) || level.random.nextInt(3)==0) continue;
                var fibres=TCArcane.TAINT_FIBRES.get().defaultBlockState().setValue(TaintFibreBlock.VARIANT,level.random.nextInt(4)==0 ? 1 : 0);
                block(pos.getX(),pos.getY(),pos.getZ(),TaintFibreBlock.faces(fibres,level,pos));
            }
            if (level.random.nextFloat()<.15f) {
                block(x+b,52,z+a,TCArcane.TAINT_CRUST.get()); if (level.random.nextFloat()<.2f) block(x+b,53,z+a,TCArcane.TAINT_CRUST.get());
            }
            if ((Math.abs(a)==4 || Math.abs(b)==4) && level.random.nextBoolean()
                    || (Math.abs(a)>=5 || Math.abs(b)>=5) && level.random.nextFloat()>.33f
                    || (Math.abs(a)>=7 || Math.abs(b)>=7) && level.random.nextFloat()>.25f) continue;
            block(x+b,51,z+a,TCArcane.TAINT_SOIL.get());
        }
        tentacle(.5,.5,level.getDifficulty()==Difficulty.HARD);
        boolean giant=!level.random.nextBoolean(); tentacle(3.5,3.5,giant); tentacle(-2.5,3.5,!giant);
        giant=!level.random.nextBoolean(); tentacle(3.5,-2.5,giant); tentacle(-2.5,-2.5,!giant);
    }
    private void tentacle(double offsetX,double offsetZ,boolean giant) {
        TaintacleEntity entity=giant ? TCEntities.TAINTACLE_GIANT.get().create(level) : TCEntities.TAINTACLE.get().create(level);
        if (entity==null) throw new IllegalStateException("Missing taintacle");
        entity.moveTo(x+offsetX,53,z+offsetZ,0,0); ChampionMobs.make(entity,true); entities.add(entity);
    }
}
