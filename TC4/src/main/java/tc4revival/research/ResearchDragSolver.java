package tc4revival.research;

import java.util.*;
import tc4revival.aspect.AspectCatalog;

/** Client-only planning logic with no Minecraft client linkage or server mutations.
 * A solver belongs to one drag: graph, note, knowledge and free tokens are frozen.
 */
public class ResearchDragSolver {
    public record Plan(List<ResearchHex> path, Map<ResearchHex,String> placements) {
        public Plan { path=List.copyOf(path); placements=Collections.unmodifiableMap(new LinkedHashMap<>(placements)); }
    }
    private final ResearchNote note;
    private final AspectCatalog catalog;
    private final List<String> tags;
    private final Map<String,Integer> indexes=new HashMap<>();
    private final boolean[][] related;
    private final int[] pools, tokens;
    private final Map<ResearchHex,Optional<Plan>> cache=new HashMap<>();
    private final Map<Integer,List<BitSet>> reachability=new HashMap<>();
    private final ResearchHex start;
    private int work;
    private Plan bestPlan;
    private int bestPathCost;
    private List<Map<ResearchHex,Integer>> centerBounds;
    // Fixed work bound (not a wall-clock timeout) keeps failed/complex drags deterministic.
    private static final int MAX_PATH_WORK=16000, EXTRA_STEPS=6, ENDPOINT_SLACK=8;

    public ResearchDragSolver(ResearchNote note, AspectCatalog catalog, ResearchHex start,
            Map<String,Integer> available, Set<String> free) {
        this.note=note;this.start=start;this.catalog=catalog;
        tags=catalog.all().stream().map(a->a.tag()).filter(available::containsKey).toList();
        pools=new int[tags.size()];tokens=new int[tags.size()];related=new boolean[tags.size()][tags.size()];
        for(int i=0;i<tags.size();i++) {
            indexes.put(tags.get(i),i);pools[i]=Math.max(0,available.get(tags.get(i)));tokens[i]=free.contains(tags.get(i))?1:0;
            for(int j=0;j<tags.size();j++)related[i][j]=catalog.areRelated(catalog.require(tags.get(i)),catalog.require(tags.get(j)));
        }
    }
    public ResearchNote snapshot() { return note; }
    public Optional<Plan> preview(ResearchHex end) { return cache.computeIfAbsent(end,this::solve); }
    public boolean reachable(String from,String to,int steps) {
        Integer a=indexes.get(from),b=indexes.get(to);
        return a!=null && b!=null && steps>=0 && steps<=69 && reachable(b,steps).get(a);
    }
    private BitSet reachable(int target,int steps) {
        var layers=reachability.computeIfAbsent(target,key->{var l=new ArrayList<BitSet>();var b=new BitSet(tags.size());b.set(key);l.add(b);return l;});
        while(layers.size()<=steps) {
            var before=layers.get(layers.size()-1);var next=new BitSet(tags.size());
            for(int a=0;a<tags.size();a++)for(int b=before.nextSetBit(0);b>=0;b=before.nextSetBit(b+1))
                if(related[a][b]) {next.set(a);break;}
            layers.add(next);
        }
        return layers.get(steps);
    }
    private Optional<Plan> solve(ResearchHex end) {
        if(note.complete() || start.equals(end) || !note.grid().containsKey(start) || !note.grid().containsKey(end) || connected(start,end))return Optional.empty();
        work=0;
        int minimum=start.distance(end),maximum=Math.min(note.grid().size()-1,minimum+EXTRA_STEPS);
        centerBounds=centerBounds(end,maximum);
        for(int length=minimum;length<=maximum && work<MAX_PATH_WORK;length++) {
            var path=new ArrayList<ResearchHex>();path.add(start);var used=new HashSet<ResearchHex>();used.add(start);
            bestPlan=null;bestPathCost=Integer.MAX_VALUE;
            path(path,used,end,length,0);
            if(bestPlan!=null)return Optional.of(bestPlan);
        }
        return Optional.empty();
    }
    // Squared Euclidean distance in axial coordinates, up to the grid's constant pixel scale.
    // Among equal-length feasible paths we minimize the total distance from the board centre.
    private static int centerDistance(ResearchHex h) { return h.q()*h.q()+h.q()*h.r()+h.r()*h.r(); }
    private List<Map<ResearchHex,Integer>> centerBounds(ResearchHex end,int maximum) {
        var layers=new ArrayList<Map<ResearchHex,Integer>>();layers.add(Map.of(end,0));
        for(int step=1;step<=maximum;step++) {
            var before=layers.get(step-1);var layer=new HashMap<ResearchHex,Integer>();
            for(var h:note.grid().keySet()) {
                int best=Integer.MAX_VALUE;
                for(int d=0;d<6;d++) {
                    var next=h.neighbor(d);var cell=note.grid().get(next);var tail=before.get(next);
                    if(cell!=null && tail!=null && (!cell.negative() || next.equals(end)))
                        best=Math.min(best,centerDistance(next)+tail);
                }
                if(best!=Integer.MAX_VALUE)layer.put(h,best);
            }
            layers.add(layer);
        }
        return layers;
    }
    private void path(ArrayList<ResearchHex> path,Set<ResearchHex> used,ResearchHex end,int remaining,int cost) {
        if(++work>MAX_PATH_WORK)return;
        var current=path.get(path.size()-1);
        var bound=centerBounds.get(remaining).get(current);
        // Ignoring revisits and aspect constraints gives an admissible bound. This
        // prunes equal/worse routes without spending the whole budget enumerating them.
        if(bound==null || cost+bound>=bestPathCost || current.distance(end)>remaining)return;
        if(remaining==0) {
            if(current.equals(end)) {
                var plan=fill(path);
                if(plan!=null) {bestPlan=plan;bestPathCost=cost;}
            }
            return;
        }
        if(current.equals(end))return;
        var neighbors=new ArrayList<ResearchHex>();
        for(int d=0;d<6;d++) {
            var next=current.neighbor(d);var cell=note.grid().get(next);
            if(cell!=null && !used.contains(next) && (!cell.negative() || next.equals(end)))neighbors.add(next);
        }
        neighbors.sort(Comparator.comparingInt(ResearchDragSolver::centerDistance)
                .thenComparingInt(h->h.distance(end)).thenComparingInt(ResearchHex::q).thenComparingInt(ResearchHex::r));
        for(var next:neighbors) {
            if(work>=MAX_PATH_WORK)break;
            used.add(next);path.add(next);path(path,used,end,remaining-1,cost+centerDistance(next));
            path.remove(path.size()-1);used.remove(next);
        }
    }
    private Plan fill(List<ResearchHex> full) {
        work+=64; // Bound expensive layered evaluations as well as geometric DFS nodes.
        int begin=note.grid().get(full.get(0)).negative()?1:0;
        int end=full.size()-(note.grid().get(full.get(full.size()-1)).negative()?1:0);
        if(end-begin<2)return null;
        var path=List.copyOf(full.subList(begin,end));
        int[] remaining=pools.clone(),free=tokens.clone();
        int count=(int)path.stream().filter(h->!note.grid().get(h).occupied()).count();
        if(count==0)return null;
        int threshold=(count+1)/2;
        int[] fixed=new int[path.size()];Arrays.fill(fixed,-1);
        for(int i=0;i<path.size();i++) {
            var cell=note.grid().get(path.get(i));
            if(cell.occupied()) {fixed[i]=indexes.getOrDefault(cell.aspect(),-1);if(fixed[i]<0)return null;}
        }
        for(int pos:new int[]{0,path.size()-1})if(fixed[pos]<0) {
            fixed[pos]=endpoint(path.get(pos),path,remaining,free,threshold);
            if(fixed[pos]<0)return null;
            consume(fixed[pos],remaining,free);
        }
        int size=tags.size();double[][] cost=new double[path.size()][size];int[][] previous=new int[path.size()][size];
        for(var row:cost)Arrays.fill(row,Double.POSITIVE_INFINITY);
        for(var row:previous)Arrays.fill(row,-1);
        cost[0][fixed[0]]=0;
        // The layered graph is acyclic: dynamic shortest paths are equivalent to Dijkstra,
        // without its heap. All per-aspect costs remain fixed after reserving both endpoints.
        for(int layer=1;layer<path.size();layer++)for(int a=0;a<size;a++) {
            if(fixed[layer]>=0 ? a!=fixed[layer] : remaining[a]+free[a]<threshold || !safe(path.get(layer),a))continue;
            double price=fixed[layer]>=0?0:price(a,remaining,free);
            for(int before=0;before<size;before++)if(related[before][a] && cost[layer-1][before]+price<cost[layer][a]) {
                cost[layer][a]=cost[layer-1][before]+price;previous[layer][a]=before;
            }
        }
        int chosen=fixed[path.size()-1];
        if(!Double.isFinite(cost[path.size()-1][chosen]))return null;
        var assignments=new String[path.size()];
        for(int layer=path.size()-1;layer>=0;layer--) {assignments[layer]=tags.get(chosen);chosen=previous[layer][chosen];}
        var placements=new LinkedHashMap<ResearchHex,String>();var required=new int[size];
        for(int i=0;i<path.size();i++)if(!note.grid().get(path.get(i)).occupied()) {
            int a=indexes.get(assignments[i]);
            if(++required[a]>pools[a]+tokens[a])return null;
            placements.put(path.get(i),assignments[i]);
        }
        return new Plan(path,placements);
    }
    private int endpoint(ResearchHex hex,List<ResearchHex> path,int[] remaining,int[] free,int threshold) {
        var nearest=note.grid().entrySet().stream().filter(e->e.getValue().occupied() && !e.getValue().negative()
                && !path.contains(e.getKey()) && indexes.containsKey(e.getValue().aspect()))
                .min(Comparator.<Map.Entry<ResearchHex,ResearchNote.Cell>>comparingInt(e->hex.distance(e.getKey()))
                        .thenComparingInt(e->e.getKey().q()).thenComparingInt(e->e.getKey().r()));
        if(nearest.isPresent()) {
            var e=nearest.get();int distance=hex.distance(e.getKey()),target=indexes.get(e.getValue().aspect());
            for(int j=0;j<=ENDPOINT_SLACK;j++) {
                int found=cheapest(hex,reachable(target,distance+j),remaining,free,threshold);
                if(found>=0)return found;
            }
        }
        var all=new BitSet(tags.size());all.set(0,tags.size());return cheapest(hex,all,remaining,free,threshold);
    }
    private int cheapest(ResearchHex hex,BitSet candidates,int[] remaining,int[] free,int threshold) {
        int best=-1;double cost=Double.POSITIVE_INFINITY;
        for(int a=candidates.nextSetBit(0);a>=0;a=candidates.nextSetBit(a+1)) {
            double p=price(a,remaining,free);
            if(remaining[a]+free[a]>=threshold && safe(hex,a) && p<cost) {best=a;cost=p;}
        }
        return best;
    }
    private boolean safe(ResearchHex hex,int aspect) {
        for(int d=0;d<6;d++) {
            var cell=note.grid().get(hex.neighbor(d));
            if(cell!=null && cell.negative()) {
                var other=catalog.find(cell.aspect());
                if(other.isPresent() && catalog.areRelated(catalog.require(tags.get(aspect)),other.get()))return false;
            }
        }
        return true;
    }
    private static double price(int a,int[] remaining,int[] free) {
        return remaining[a]>0?1/Math.sqrt(remaining[a]):free[a]>0?0:Double.POSITIVE_INFINITY;
    }
    private static void consume(int a,int[] remaining,int[] free) { if(remaining[a]>0)remaining[a]--;else free[a]--; }
    private boolean connected(ResearchHex from,ResearchHex to) {
        var first=note.grid().get(from);var last=note.grid().get(to);
        if(first==null || last==null || !first.occupied() || !last.occupied() || first.negative() || last.negative())return false;
        var pending=new ArrayDeque<ResearchHex>();var seen=new HashSet<ResearchHex>();pending.add(from);seen.add(from);
        while(!pending.isEmpty()) {
            var hex=pending.removeFirst();if(hex.equals(to))return true;
            Integer a=indexes.get(note.grid().get(hex).aspect());if(a==null)continue;
            for(int d=0;d<6;d++) {
                var next=hex.neighbor(d);var cell=note.grid().get(next);
                if(cell==null || !cell.occupied() || cell.negative())continue;
                Integer b=indexes.get(cell.aspect());
                if(b!=null && related[a][b] && seen.add(next))pending.add(next);
            }
        }
        return false;
    }
}
