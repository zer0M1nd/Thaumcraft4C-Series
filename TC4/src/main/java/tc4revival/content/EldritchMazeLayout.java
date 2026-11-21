package tc4revival.content;

import java.util.*;

/** Pure layout generation. World placement is committed separately, after a complete layout exists. */
public  class EldritchMazeLayout {
    public record Cell(int passages,int feature) {
        public Cell { if ((passages&~63)!=0 || feature<0 || feature>127) throw new IllegalArgumentException("Invalid maze cell"); }
        public short packed() { return (short)(passages|feature<<8); }
        public static Cell unpack(int packed) { return new Cell(packed&63,(packed>>>8)&127); }
        public boolean opens(int direction) { return (passages&direction)!=0; }
    }
    public record Point(int x,int z) {}
    private final int width,height;
    private final long seed;
    private final short[] cells;
    private EldritchMazeLayout(int width,int height,long seed,int[][] grid) {
        this.width=width; this.height=height; this.seed=seed; cells=new short[width*height];
        for (int z=0;z<height;z++) for (int x=0;x<width;x++) cells[z*width+x]=(short)Math.max(0,grid[z][x]);
    }
    public int width() { return width; }
    public int height() { return height; }
    public long seed() { return seed; }
    public Point entrance() { return new Point(1+width/2,1+height/2); }
    public Cell cell(int x,int z) { if (x<0 || z<0 || x>=width || z>=height) return new Cell(0,0); return Cell.unpack(cells[z*width+x]); }
    public short[] packedCells() { return cells.clone(); }
    public static EldritchMazeLayout restore(int width,int height,long seed,int[] packed) {
        dimensions(width,height); if (packed.length!=width*height) throw new IllegalArgumentException("Truncated maze cells");
        var grid=new int[height][width];
        for (int z=0;z<height;z++) for (int x=0;x<width;x++) { int value=packed[z*width+x]; if (value<0 || value>32767 || (value&192)!=0) throw new IllegalArgumentException("Malformed maze cell"); grid[z][x]=value; }
        return new EldritchMazeLayout(width,height,seed,grid);
    }
    private static void dimensions(int width,int height) { if (width<7 || height<7 || width>63 || height>63) throw new IllegalArgumentException("Maze dimensions outside safe legacy-sized bounds"); }
    public static Optional<EldritchMazeLayout> attempt(int width,int height,long seed) { dimensions(width,height); return new Builder(width,height,seed).generate(); }
    public static EldritchMazeLayout generate(int width,int height,long seed) {
        // Old generation retried forever. A bounded attempt count avoids locking a server on corrupt input.
        for (int attempt=0;attempt<4096;attempt++) { var layout=attempt(width,height,seed+attempt); if (layout.isPresent()) return layout.get(); }
        throw new IllegalStateException("Unable to generate a connected eldritch maze");
    }
    public static int opposite(int direction) { return switch(direction) { case 1->2; case 2->1; case 4->8; case 8->4; default->throw new IllegalArgumentException("Not a horizontal maze direction"); }; }
    public static int dx(int direction) { return direction==4 ? 1 : direction==8 ? -1 : 0; }
    public static int dz(int direction) { return direction==1 ? -1 : direction==2 ? 1 : 0; }
    private static  class Builder {
        final int width,height; final long seed; final int[][] grid; final Random random;
        final List<Integer> directions=new ArrayList<>(List.of(1,2,4,8));
        final ArrayList<Point> frontier=new ArrayList<>();
        Builder(int width,int height,long seed) { this.width=width; this.height=height; this.seed=seed; grid=new int[height][width]; random=new Random(seed); }
        boolean inside(int x,int z) { return x>0 && z>0 && x<width-1 && z<height-1; }
        int index() { float choice=random.nextFloat(); return choice<=.45f ? frontier.size()-1 : choice<=.9f ? random.nextInt(frontier.size()) : 0; }
        void join(int x,int z,int direction) { grid[z][x]|=direction; grid[z+dz(direction)][x+dx(direction)]|=opposite(direction); }
        Optional<EldritchMazeLayout> generate() {
            int corner=random.nextInt(4),bossX=corner==1 || corner==2 ? width-2 : 0,bossZ=corner==1 || corner==3 ? height-2 : 0;
            grid[bossZ][bossX]=2<<8; grid[bossZ][bossX+1]=3<<8; grid[bossZ+1][bossX]=4<<8; grid[bossZ+1][bossX+1]=5<<8;
            int entryX=1+width/2,entryZ=1+height/2; grid[entryZ][entryX]=1<<8;
            int obstacles=(width+height)/4;
            for (int obstacle=0;obstacle<obstacles;obstacle++) {
                int size=1+random.nextInt(3); if (size==3) obstacles--;
                int x=random.nextInt(width-size),z=random.nextInt(height-size);
                for (int a=x;a<x+size;a++) for (int b=z;b<z+size;b++) if (grid[b][a]==0) grid[b][a]=-1;
            }
            Collections.shuffle(directions,random); int initial=directions.get(0),x=entryX+dx(initial),z=entryZ+dz(initial);
            if (grid[z][x]<0) grid[z][x]=0;
            join(entryX,entryZ,initial); frontier.add(new Point(x,z));
            boolean extended=false;
            while (!frontier.isEmpty()) {
                int index=index(); var point=frontier.get(index); Collections.shuffle(directions,random); boolean carved=false;
                for (int direction:directions) {
                    int nx=point.x()+dx(direction),nz=point.z()+dz(direction);
                    if (!inside(nx,nz) || grid[nz][nx]!=0) continue;
                    join(point.x(),point.z(),direction); frontier.add(new Point(nx,nz)); extended=carved=true; break;
                }
                if (!carved) frontier.remove(index);
            }
            if (!extended) return Optional.empty();
            for (int row=0;row<height;row++) for (int col=0;col<width;col++) if (grid[row][col]<0) grid[row][col]=0;
            Collections.shuffle(directions,random);
            for (int direction:directions) {
                int nx=entryX+dx(direction),nz=entryZ+dz(direction);
                if (inside(nx,nz) && grid[nz][nx]>0 && random.nextBoolean()) join(entryX,entryZ,direction);
            }
            Collections.shuffle(directions,random);
            boolean bossConnected=false;
            connect: for (int a=0;a<2;a++) for (int b=0;b<2;b++) for (int direction:directions) {
                int nx=bossX+a+dx(direction),nz=bossZ+b+dz(direction);
                if (inside(nx,nz) && grid[nz][nx]>0 && Cell.unpack(grid[nz][nx]).feature()==0) { join(bossX+a,bossZ+b,direction); bossConnected=true; break connect; }
            }
            if (!bossConnected && !extendBoss(bossX,bossZ)) return Optional.empty();
            var ends=new ArrayList<Point>();
            for (int row=0;row<height;row++) for (int col=0;col<width;col++) {
                if (Cell.unpack(grid[row][col]).feature()==99) grid[row][col]&=63;
                if (Integer.bitCount(grid[row][col]&15)==1 && Cell.unpack(grid[row][col]).feature()==0) ends.add(new Point(col,row));
            }
            if (ends.isEmpty()) return Optional.empty();
            var key=ends.remove(random.nextInt(ends.size())); grid[key.z()][key.x()]|=6<<8;
            int rooms=ends.size()/2;
            for (int count=0;count<rooms;count++) { var room=ends.remove(random.nextInt(ends.size())); grid[room.z()][room.x()]|=(7+random.nextInt(3))<<8; }
            for (int row=0;row<height;row++) for (int col=0;col<width;col++) {
                if (Cell.unpack(grid[row][col]).feature()!=0 || (grid[row][col]&15)==0 || random.nextInt(25)!=0) continue;
                int type=switch(random.nextInt(8)) { case 0->8; case 1->10; case 2,3->11; case 4,5->12; case 6->13; default->14; };
                grid[row][col]|=type<<8;
            }
            return Optional.of(new EldritchMazeLayout(width,height,seed,grid));
        }
        boolean extendBoss(int bossX,int bossZ) {
            var exits=new ArrayList<>(List.of(1,2,4,8)); Collections.shuffle(exits,random);
            for (int a=0;a<2;a++) for (int b=0;b<2;b++) for (int exit:exits) {
                int startX=bossX+a+dx(exit),startZ=bossZ+b+dz(exit);
                if (!inside(startX,startZ) || grid[startZ][startX]!=0) continue;
                frontier.add(new Point(startX,startZ));
                while (!frontier.isEmpty()) {
                    int index=index(); var point=frontier.get(index); Collections.shuffle(directions,random); boolean carved=false;
                    for (int direction:directions) {
                        int nx=point.x()+dx(direction),nz=point.z()+dz(direction); if (!inside(nx,nz)) continue;
                        if (grid[nz][nx]==0) {
                            join(point.x(),point.z(),direction); grid[point.z()][point.x()]|=99<<8; grid[nz][nx]|=99<<8;
                            frontier.add(new Point(nx,nz)); carved=true; break;
                        }
                        if (Cell.unpack(grid[nz][nx]).feature()==0) { join(point.x(),point.z(),direction); join(bossX+a,bossZ+b,exit); frontier.clear(); return true; }
                    }
                    if (!carved) frontier.remove(index);
                }
            }
            return false;
        }
    }
}
