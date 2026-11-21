package tc4revival.content;

import net.minecraft.core.Direction;

/** Room-local geometry; the placement sink supplies blocks, decorations and encounter state. */
public  class EldritchRoomGeometry {
    public enum Material {
        BEDROCK_IF_AIR, STONE, VOID, AIR, STAIR, URN_STAIR, INVERTED_STAIR,
        ROCK, NONSPAWNING_STONE, ALTAR, PORTAL, OBELISK_BASE, OBELISK_SEGMENT,
        PEDESTAL_STONE, GLYPHED_STONE, SLAB, UPPER_SLAB, CRUST, GLOWING_STONE, CRYSTAL,
        DOOR_FRAME, DOOR_LOCK, DOOR_BARRIER, TRAPPED_STONE, WEB, MIND_SPIDER_SPAWNER,
        TAINT_FIBRES, TAINT_GRASS
    }
    @FunctionalInterface public interface Sink {
        void place(int x,int y,int z,Material material,Direction facing);
        default void place(int x,int y,int z,Material material) { place(x,y,z,material,Direction.NORTH); }
    }
    public interface Environment extends Sink {
        boolean isAir(int x,int y,int z);
        boolean canCrustReplace(int x,int y,int z);
        boolean adjacentToSolid(int x,int y,int z);
        void taintBiome(int x,int z);
        void loot(int x,int y,int z,boolean crate,int rarity);
    }
    private EldritchRoomGeometry() {}

    /** Coordinates include the boundary at 16 shared with the next room. */
    public static void connections(Sink sink,EldritchMazeLayout.Cell cell,int depth,boolean tapered) {
        for (Direction direction: new Direction[]{Direction.NORTH,Direction.SOUTH,Direction.EAST,Direction.WEST}) {
            if (!cell.opens(bit(direction))) continue;
            for (int distance=0;distance<=depth;distance++) {
                int inset=tapered ? Math.max(0,2-depth+distance) : 0;
                for (int col=inset;col<11-inset;col++) for (int row=inset;row<11-inset;row++) {
                    int edge=Math.min(Math.min(row,10-row),Math.min(col,10-col));
                    boolean corner=(row==0 || row==10) && (col==0 || col==10);
                    if (corner) continue;
                    Material material;
                    Direction facing=direction;
                    if (edge==0) material=Material.BEDROCK_IF_AIR;
                    else if (edge==1) material=Material.VOID;
                    else if (edge==2) material=(row==2 || row==8) && (col==2 || col==8) ? Material.VOID : Material.STONE;
                    else if ((row==3 || row==7) && (col==3 || col==7)) {
                        material=row==3 ? Material.INVERTED_STAIR : Material.URN_STAIR;
                        facing=direction.getAxis()==Direction.Axis.Z
                                ? (col==3 ? Direction.WEST : Direction.EAST)
                                : (col==3 ? Direction.NORTH : Direction.SOUTH);
                    } else material=Material.AIR;
                    if (tapered && distance==depth && material==Material.VOID) continue;
                    int across=3+col;
                    int x=direction==Direction.EAST ? 16-distance : direction==Direction.WEST ? distance : across;
                    int z=direction==Direction.SOUTH ? 16-distance : direction==Direction.NORTH ? distance : across;
                    sink.place(x,10-row,z,material,facing);
                }
            }
        }
    }

    public static void entrance(Sink sink,EldritchMazeLayout.Cell cell) {
        chamber(sink,cell,true);
        // Four pillars are hollowed after the connecting arches have been placed.
        for (int x=3;x<=13;x++) for (int z=3;z<=13;z++) {
            if (!(x<=4 || x>=12) || !(z<=4 || z>=12)) continue;
            for (int y=1;y<12;y++) sink.place(x,y,z,Material.AIR);
        }
        for (int sideX:new int[]{-1,1}) for (int sideZ:new int[]{-1,1}) {
            int innerX=8+sideX*3,outerX=8+sideX*4,innerZ=8+sideZ*3,outerZ=8+sideZ*4;
            for (int tier=0;tier<2;tier++) {
                int y=tier==0 ? 3 : 8; Material stair=tier==0 ? Material.STAIR : Material.INVERTED_STAIR;
                Direction zFacing=sideZ<0 ? Direction.NORTH : Direction.SOUTH;
                sink.place(innerX,y,innerZ,stair,zFacing); sink.place(outerX,y,innerZ,stair,zFacing);
                sink.place(innerX,y,outerZ,stair,sideX<0 ? Direction.WEST : Direction.EAST);
            }
        }
        sink.place(8,2,8,Material.ALTAR); sink.place(8,3,8,Material.PORTAL);
        obelisk(sink,8,4,8);
    }

    /** The permanent key item and guarding entities are installed by the encounter placer. */
    public static void keyRoom(Sink sink,EldritchMazeLayout.Cell cell) {
        chamber(sink,cell,false); sink.place(8,2,8,Material.ALTAR);
    }

    public static void obelisk(Sink sink,int x,int y,int z) {
        sink.place(x,y,z,Material.OBELISK_BASE);
        for (int offset=1;offset<=4;offset++) sink.place(x,y+offset,z,Material.OBELISK_SEGMENT);
    }

    public static void library(Sink sink,EldritchMazeLayout.Cell cell) {
        shell(sink,cell,13);
        for (int x=3;x<=13;x++) for (int z=3;z<=13;z++) {
            if (boundary(x,z,3,13)) for (int y=2;y<11;y++) sink.place(x,y,z,Material.STONE);
        }
        for (int x=2;x<=14;x++) for (int z=2;z<=14;z++) {
            sandwich(sink,x,z,1,10,Material.STONE);
            if (x<=3 || x>=13 || z<=3 || z>=13) continue;
            if ((x<=5 || x>=11) && (z<=5 || z>=11)) {
                sink.place(x,2,z,Material.STONE); sink.place(x,9,z,Material.STONE);
            }
            if ((x==5 || x==11) && (z==5 || z==11)) {
                sink.place(x,3,z,Material.PEDESTAL_STONE); sink.place(x,8,z,Material.PEDESTAL_STONE);
            }
        }
        stairRing(sink,2,false); stairRing(sink,9,true);
        for (int x:new int[]{5,11}) for (int z:new int[]{5,11}) {
            sink.place(x,4,z,Material.GLYPHED_STONE); sink.place(x,5,z,Material.SLAB);
            sink.place(x,7,z,Material.GLYPHED_STONE); sink.place(x,6,z,Material.UPPER_SLAB);
        }
        sink.place(8,2,8,Material.PEDESTAL_STONE); sink.place(8,3,8,Material.GLYPHED_STONE); sink.place(8,4,8,Material.SLAB);
        sink.place(8,9,8,Material.PEDESTAL_STONE); sink.place(8,8,8,Material.GLYPHED_STONE); sink.place(8,7,8,Material.UPPER_SLAB);
        connections(sink,cell,3,true);
    }

    public static void bossQuarter(Sink sink,EldritchMazeLayout.Cell cell) {
        if (cell.feature()<2 || cell.feature()>5) throw new IllegalArgumentException("Not a boss room quarter");
        boolean east=(cell.feature()&1)==1,south=cell.feature()>=4;
        // Shared interior edges extend to 0/15; the outer wall occupies three layers.
        for (int layer=1;layer<=3;layer++) {
            int minX=east ? 0 : layer,maxX=east ? 16-layer : 15;
            int minZ=south ? 0 : layer,maxZ=south ? 16-layer : 15;
            int wallX=east ? maxX : minX,wallZ=south ? maxZ : minZ;
            Material material=layer==1 ? Material.BEDROCK_IF_AIR : layer==2 ? Material.VOID : Material.ROCK;
            for (int x=minX;x<=maxX;x++) for (int z=minZ;z<=maxZ;z++) {
                if (x!=wallX && z!=wallZ) continue;
                for (int y=layer-1;y<14-layer;y++) {
                    boolean opening=layer==2 && y<10 && ((x==wallX && z>4 && z<12 && cell.opens(east ? 4 : 8))
                            || (z==wallZ && x>4 && x<12 && cell.opens(south ? 2 : 1)));
                    if (!opening) sink.place(x,y,z,material);
                }
            }
        }
        for (int x=east ? 0 : 2;x<=(east ? 14 : 15);x++) for (int z=south ? 0 : 2;z<=(south ? 14 : 15);z++) {
            sink.place(x,-1,z,Material.BEDROCK_IF_AIR); sink.place(x,0,z,Material.VOID); sink.place(x,1,z,Material.NONSPAWNING_STONE);
            sink.place(x,13,z,Material.BEDROCK_IF_AIR); sink.place(x,12,z,Material.VOID); sink.place(x,11,z,Material.STONE);
        }
        for (int y:new int[]{2,10}) {
            Material stair=y==2 ? Material.STAIR : Material.INVERTED_STAIR;
            for (int x=east ? 0 : 4;x<=(east ? 11 : 15);x++) sink.place(x,y,south ? 12 : 4,stair,south ? Direction.SOUTH : Direction.NORTH);
            int endZ=south ? (east ? 12 : 11) : 15;
            for (int z=south ? 0 : 4;z<=endZ;z++) sink.place(east ? 12 : 4,y,z,stair,east ? Direction.EAST : Direction.WEST);
        }
        connections(sink,cell,3,true);
        Direction entrance=null;
        // The legacy door selects the last open direction in this order.
        for (Direction direction:new Direction[]{Direction.NORTH,Direction.SOUTH,Direction.EAST,Direction.WEST})
            if (cell.opens(bit(direction))) entrance=direction;
        if (entrance==null) return;
        for (int across=0;across<7;across++) for (int vertical=0;vertical<7;vertical++) {
            int edge=Math.min(Math.min(across,6-across),Math.min(vertical,6-vertical));
            if ((across==0 || across==6) && (vertical==0 || vertical==6)) continue;
            Material material;
            if (across==3 && vertical==3) material=Material.DOOR_LOCK;
            else if (edge==0 || ((across==1 || across==5) && (vertical==1 || vertical==5))) material=Material.DOOR_FRAME;
            else material=Material.DOOR_BARRIER;
            int x=entrance==Direction.EAST ? 13 : entrance==Direction.WEST ? 3 : 5+across;
            int z=entrance==Direction.SOUTH ? 13 : entrance==Direction.NORTH ? 3 : 5+across;
            sink.place(x,2+vertical,z,material,entrance);
        }
    }

    public static void nest(Environment sink,EldritchMazeLayout.Cell cell,java.util.Random random) {
        shell(sink,cell,11);
        for (int x=3;x<=13;x++) for (int z=3;z<=13;z++) for (int y=2;y<9;y++) {
            if (boundary(x,z,3,13)) sink.place(x,y,z,Material.CRUST);
            boolean bulge=(x==4 && !cell.opens(8)) || (x==12 && !cell.opens(4))
                    || (z==4 && !cell.opens(1)) || (z==12 && !cell.opens(2));
            if (bulge && random.nextBoolean()) sink.place(x,y,z,Material.CRUST);
        }
        for (int x=2;x<=14;x++) for (int z=2;z<=14;z++) {
            sandwich(sink,x,z,1,9,Material.CRUST);
            if (random.nextBoolean()) sink.place(x,8,z,Material.CRUST);
            else if (random.nextBoolean() && sink.isAir(x,8,z)) sink.place(x,8,z,Material.CRYSTAL,Direction.DOWN);
        }
        int[][] arms={{7,8},{8,7},{9,8},{8,9}};
        for (int half=0;half<2;half++) {
            int base=half==0 ? 2 : 8,step=half==0 ? 1 : -1;
            for (int offset=0;offset<3;offset++) sink.place(8,base+offset*step,8,Material.CRUST);
            for (int[] arm:arms) sink.place(arm[0],base,arm[1],Material.CRUST);
            for (int[] arm:arms) if (random.nextBoolean()) sink.place(arm[0],base+step,arm[1],Material.CRUST);
            if (half==0 && random.nextBoolean()) sink.place(8,5,8,Material.GLOWING_STONE);
        }
        connections(sink,cell,3,true);
        for (int x=3;x<=13;x++) for (int z=3;z<=13;z++) {
            if (random.nextFloat()>=.15f || !sink.isAir(x,2,z)) continue;
            float rarity=random.nextFloat(); sink.loot(x,2,z,random.nextFloat()<.2f,rarity<.15f ? 2 : rarity<.4f ? 1 : 0);
        }
    }

    public static void passage(Environment sink,EldritchMazeLayout.Cell cell,java.util.Random random) {
        connections(sink,cell,4,false);
        boolean ornate=(cell.passages()&15)==15 && random.nextBoolean();
        for (int x=5;x<=11;x++) for (int z=5;z<=11;z++) {
            boolean center=ornate && x==8 && z==8;
            sink.place(x,2,z,center ? Material.GLOWING_STONE : passageStone(cell,random));
            sink.place(x,8,z,center ? Material.GLOWING_STONE : passageStone(cell,random));
            sink.place(x,0,z,Material.BEDROCK_IF_AIR); sink.place(x,10,z,Material.BEDROCK_IF_AIR);
            sink.place(x,1,z,Material.VOID); sink.place(x,9,z,Material.VOID);
        }
        for (Direction direction:new Direction[]{Direction.NORTH,Direction.SOUTH,Direction.EAST,Direction.WEST}) {
            boolean open=cell.opens(bit(direction));
            if (open) {
                int inset=ornate ? 3 : 2;
                for (int col=inset;col<11-inset;col++) for (int row=inset;row<11-inset;row++) {
                    placeSection(sink,direction,5,row,col);
                }
                if (!ornate) for (int edge:new int[]{6,10}) {
                    int other=direction.getAxis()==Direction.Axis.Z ? (edge==6 ? 8 : 4) : (edge==6 ? 1 : 2);
                    if (cell.opens(other)) passageStairs(sink,direction,edge);
                }
            } else {
                for (int across=5;across<=11;across++) for (int row=1;row<=7;row++) {
                    int y=9-row;
                    wallPoint(sink,direction,5,across,y,passageStone(cell,random));
                    wallPoint(sink,direction,4,across,y,Material.VOID);
                    wallPoint(sink,direction,3,across,y,Material.BEDROCK_IF_AIR);
                    if (row==7) {
                        wallPoint(sink,direction,4,across,1,Material.BEDROCK_IF_AIR);
                        wallPoint(sink,direction,4,across,9,Material.BEDROCK_IF_AIR);
                    }
                    if (across==11) {
                        wallPoint(sink,direction,4,4,y,Material.BEDROCK_IF_AIR);
                        wallPoint(sink,direction,4,12,y,Material.BEDROCK_IF_AIR);
                    }
                }
                for (int across=6;across<=10;across++) passageStairs(sink,direction,across);
            }
        }
        if (ornate) {
            // The old four-way junction has intentionally asymmetric corner stair ordering.
            int[][] corners={{5,5,0},{5,6,2},{11,5,0},{11,6,3},{5,11,2},{6,11,1},{11,11,3},{10,11,1}};
            Direction[] facing={Direction.NORTH,Direction.SOUTH,Direction.WEST,Direction.EAST};
            for (int[] corner:corners) {
                sink.place(corner[0],3,corner[1],Material.URN_STAIR,facing[corner[2]]);
                sink.place(corner[0],7,corner[1],Material.INVERTED_STAIR,facing[corner[2]]);
            }
        }
        if (cell.feature()==12) {
            for (int x=4;x<=12;x++) for (int y=0;y<=8;y++) for (int z=4;z<=12;z++)
                if (sink.canCrustReplace(x,y,z) && random.nextBoolean()) sink.place(x,y,z,Material.CRUST);
        } else if (cell.feature()==13) {
            for (int x=4;x<=12;x++) for (int y=1;y<=7;y++) for (int z=4;z<=12;z++) {
                if (!sink.isAir(x,y,z) || !sink.adjacentToSolid(x,y,z)) continue;
                if (random.nextInt(3)!=0) sink.place(x,y,z,random.nextInt(4)==0 ? Material.TAINT_GRASS : Material.TAINT_FIBRES);
                sink.taintBiome(x,z);
            }
        } else if (cell.feature()==14) {
            for (int x=5;x<=11;x++) for (int y=1;y<=7;y++) for (int z=5;z<=11;z++) {
                if (sink.isAir(x,y,z) && random.nextFloat()<.35f) sink.place(x,y,z,Material.WEB);
                // Keep this ordering: after the first position, the center is no longer air.
                sink.place(8,4,8,Material.MIND_SPIDER_SPAWNER);
            }
        }
    }

    private static Material passageStone(EldritchMazeLayout.Cell cell,java.util.Random random) {
        return cell.feature()==11 && random.nextInt(3)==0 ? Material.TRAPPED_STONE : Material.STONE;
    }

    private static void passageStairs(Sink sink,Direction direction,int across) {
        int x=direction.getAxis()==Direction.Axis.Z ? across : direction==Direction.WEST ? 6 : 10;
        int z=direction.getAxis()==Direction.Axis.X ? across : direction==Direction.NORTH ? 6 : 10;
        sink.place(x,3,z,Material.URN_STAIR,direction); sink.place(x,7,z,Material.INVERTED_STAIR,direction);
    }

    private static void wallPoint(Sink sink,Direction direction,int distance,int across,int y,Material material) {
        int x=direction==Direction.EAST ? 16-distance : direction==Direction.WEST ? distance : across;
        int z=direction==Direction.SOUTH ? 16-distance : direction==Direction.NORTH ? distance : across;
        sink.place(x,y,z,material,direction);
    }

    private static void placeSection(Sink sink,Direction direction,int distance,int row,int col) {
        int edge=Math.min(Math.min(row,10-row),Math.min(col,10-col));
        if ((row==0 || row==10) && (col==0 || col==10)) return;
        Material material;
        Direction facing=direction;
        if (edge==0) material=Material.BEDROCK_IF_AIR;
        else if (edge==1) material=Material.VOID;
        else if (edge==2) material=(row==2 || row==8) && (col==2 || col==8) ? Material.VOID : Material.STONE;
        else if ((row==3 || row==7) && (col==3 || col==7)) {
            material=row==3 ? Material.INVERTED_STAIR : Material.URN_STAIR;
            facing=direction.getAxis()==Direction.Axis.Z ? (col==3 ? Direction.WEST : Direction.EAST)
                    : (col==3 ? Direction.NORTH : Direction.SOUTH);
        } else material=Material.AIR;
        int x=direction==Direction.EAST ? 16-distance : direction==Direction.WEST ? distance : 3+col;
        int z=direction==Direction.SOUTH ? 16-distance : direction==Direction.NORTH ? distance : 3+col;
        sink.place(x,10-row,z,material,facing);
    }

    private static void sandwich(Sink sink,int x,int z,int floor,int ceiling,Material surface) {
        sink.place(x,floor-2,z,Material.BEDROCK_IF_AIR); sink.place(x,floor-1,z,Material.VOID); sink.place(x,floor,z,surface);
        sink.place(x,ceiling+2,z,Material.BEDROCK_IF_AIR); sink.place(x,ceiling+1,z,Material.VOID); sink.place(x,ceiling,z,surface);
    }

    private static void stairRing(Sink sink,int y,boolean inverted) {
        Material material=inverted ? Material.INVERTED_STAIR : Material.STAIR;
        for (int across=6;across<=10;across++) {
            sink.place(across,y,4,material,Direction.NORTH); sink.place(across,y,12,material,Direction.SOUTH);
            sink.place(12,y,across,material,Direction.EAST); sink.place(4,y,across,material,Direction.WEST);
        }
    }

    private static void shell(Sink sink,EldritchMazeLayout.Cell cell,int height) {
        for (int x=1;x<=15;x++) for (int z=1;z<=15;z++) {
            if (boundary(x,z,1,15)) for (int y=0;y<height;y++) sink.place(x,y,z,Material.BEDROCK_IF_AIR);
        }
        for (int x=2;x<=14;x++) for (int z=2;z<=14;z++) {
            if (!boundary(x,z,2,14)) continue;
            for (int y=1;y<height-1;y++) {
                boolean opening=y<10 && ((x==2 && z>3 && z<12 && cell.opens(8))
                        || (x==14 && z>3 && z<12 && cell.opens(4))
                        || (z==2 && x>3 && x<12 && cell.opens(1))
                        || (z==14 && x>3 && x<12 && cell.opens(2)));
                if (!opening) sink.place(x,y,z,Material.VOID);
            }
        }
    }

    private static void chamber(Sink sink,EldritchMazeLayout.Cell cell,boolean entrance) {
        shell(sink,cell,13);
        for (int x=3;x<=13;x++) for (int z=3;z<=13;z++) {
            if (!boundary(x,z,3,13)) continue;
            if (entrance && (x<=4 || x>=12) && (z<=4 || z>=12)) continue;
            for (int y=2;y<11;y++) {
                Material material=Material.STONE;
                if (!entrance) {
                    boolean cross=(y>3 && y<9 && (x==8 || z==8))
                            || (y>4 && y<8 && (x==7 || z==7 || x==9 || z==9));
                    if (cross && y==6 && (x==8 || z==8)) continue;
                    material=cross ? Material.NONSPAWNING_STONE : Material.ROCK;
                }
                sink.place(x,y,z,material);
            }
        }
        Material floor=entrance ? Material.NONSPAWNING_STONE : Material.STONE;
        for (int x=2;x<=14;x++) for (int z=2;z<=14;z++) {
            sink.place(x,-1,z,Material.BEDROCK_IF_AIR); sink.place(x,0,z,Material.VOID); sink.place(x,1,z,floor);
            sink.place(x,13,z,Material.BEDROCK_IF_AIR); sink.place(x,12,z,Material.VOID); sink.place(x,11,z,Material.STONE);
            int elevation=Math.min(Math.abs(x-8),Math.abs(z-8));
            for (int n=0;n<elevation-1;n++) sink.place(x,1+n,z,floor);
            if (x>3 && x<13 && z>3 && z<13) for (int n=0;n<elevation;n++) sink.place(x,11-n,z,floor);
        }
        stairRing(sink,2,false);
        connections(sink,cell,3,true);
    }

    private static boolean boundary(int x,int z,int min,int max) { return x==min || x==max || z==min || z==max; }
    private static int bit(Direction direction) {
        return switch(direction) { case NORTH->1; case SOUTH->2; case EAST->4; case WEST->8; default->throw new IllegalArgumentException("Horizontal direction required"); };
    }
}
