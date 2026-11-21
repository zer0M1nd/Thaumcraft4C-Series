package tc4revival.research;

/** Logical GUI pixels: maximize the viewport without enlarging icons, text or tabs. */
public record ResearchBrowserLayout(int width,int height) {
    public static final ResearchBrowserLayout NORMAL=new ResearchBrowserLayout(256,230);
    public static ResearchBrowserLayout of(int screenWidth,int screenHeight,boolean maximum) {
        return maximum?new ResearchBrowserLayout(Math.max(96,screenWidth-64),Math.max(72,screenHeight-16)):NORMAL;
    }
    public int viewportWidth(){return width-32;}
    public int viewportHeight(){return height-34;}
    public int rows(){return Math.max(1,(height-14)/24);}
    public int tabsPerPage(){return rows()*2;}
    public int tabX(int index){return index>=rows()?width:-24;}
    public int tabY(int index){return index%rows()*24;}
}
