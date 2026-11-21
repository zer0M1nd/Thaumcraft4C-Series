package wgmodern;
public class LockPuzzle {
 public static final String[] PRESETS={"0,7,6,2,4,3,5,1","5,2,3,7,0,1,6,4","5,2,6,1,7,0,4,3","0,5,6,1,2,3,4,7","4,1,2,7,0,6,3,5","4,0,2,6,3,1,7,5","2,4,0,7,5,6,3,1","6,2,4,0,7,5,1,3","6,4,0,5,2,7,1,3","4,3,5,7,6,2,1,0","5,1,4,2,7,0,6,3","4,7,0,6,3,5,1,2","3,1,2,7,4,5,6,0","7,4,0,5,2,3,1,6","6,5,2,7,1,0,3,4","3,7,0,6,4,1,5,2"};
 public static int[] preset(int n){var result=new int[9];String[] s=PRESETS[Math.floorMod(n,PRESETS.length)].split(",");for(int i=0;i<8;i++)result[i]=Integer.parseInt(s[i]);result[8]=8;return result;}
 public static boolean move(int[] board,int pos){if(board.length!=9||pos<0||pos>=9||board[pos]==8)return false;int empty=-1;for(int i=0;i<9;i++)if(board[i]==8)empty=i;if(empty<0||Math.abs(empty%3-pos%3)+Math.abs(empty/3-pos/3)!=1)return false;board[empty]=board[pos];board[pos]=8;return true;}
 public static boolean solved(int[] b){if(b.length!=9)return false;for(int i=0;i<9;i++)if(b[i]!=i)return false;return true;}
}
