import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.spi.CachingProvider;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AbstractNode implements Nodes{

    protected List<Integer> djkistra;
    protected List<AbstractNode> voisins; // liste des hub voisins
    protected List<List<Integer>> Topology; // réseau sous la forme reseauDeVoisins[i] = liste des voisins du hub i
    protected int chargeReseauxRead;
    protected int chargeReseauxWrite;
    protected int chargeReseauxStore;
    protected int chargeReseauxReStore;
    protected int chargeReseauxIncrease;
    protected int nbrFichier; // nombre de fichier stocké
    protected int nbrFichierMax; // max de nombre de fichier stockable
    protected HashMap<Integer,AbstractNode> routingTable; // pour aller à la node x qui n'est pas voisine on va à la node prev[x]

    CachingProvider cachingProvider;
    CacheManager cacheManager;
    MutableConfiguration<String, List<Integer>> config;
    Cache<String, List<Integer>> cache;
    protected List<Double> topologyMoyenneGlobal;
    protected List<Integer> topoLocal;

    protected int calculateHashInt(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] encodedHash = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            BigInteger bigInteger = new BigInteger(1, encodedHash);
            return bigInteger.intValue();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return 0;
    }
    public List<Integer> djikstra(List<List<Integer>> graph, int source,int size) {
        List<Integer> listDistance = new ArrayList<>();
        List<Integer> prev = new ArrayList<>();
        //List<Integer> q = new ArrayList<>();
        List<Boolean> visited = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            listDistance.add(1000); //dist v infinity
            prev.add(-1); // prev v undefined
            visited.add(false);
        }
        listDistance.set(source, 0);
        for(int n = 0 ; n < size;n++){
            int u = minDistanceToU(listDistance,visited);
            visited.set(u,true);
            for(Integer v : graph.get(u))
            {
                int alt = listDistance.get(u) + 1;
                if(alt<listDistance.get(v))
                {
                    listDistance.set(v,alt);
                    prev.set(v,u);
                }

            }
        }
        if(this.getId()==source) { // creation table de routage
            List<Integer> voisinsHubInt = this.voisins.stream().map(f->f.getId()).toList();
            for(int p=0;p<prev.size();p++)
            {;
                while(!voisinsHubInt.contains(prev.get(p)))
                {
                    if(prev.get(p)==-1 || prev.get(p)==this.getId())
                    {
                        break;
                    }
                    prev.set(p,prev.get(prev.get(p)));
                }
                if(prev.get(p)==-1)
                {
                    prev.set(p,this.getId());
                }
                if(prev.get(p)==this.getId())
                {
                    prev.set(p,p);
                }
            }
            this.routingTable = new HashMap<>();
            for(int p = 0; p<prev.size();p++)
            {
                if(!(this.getId()==p)) {
                    int pp = p;
                    this.routingTable.put(p, this.voisins.stream().filter(f -> f.getId() == prev.get(pp)).toList().get(0));
                }
            }
        }
        List<List<Integer>> tmpTopo = getIndexLists(listDistance).stream().filter(f->!f.isEmpty()).toList();
        for(List<Integer> tmptmpTopo : tmpTopo)
        {
            this.topoLocal.addAll(tmptmpTopo);
        }
        this.djkistra=listDistance;
        return listDistance;
    }
    protected static int minDistanceToU(List<Integer> distances,List<Boolean> visited) {
        int minDistance=1000;
        int minDistanceVertex=-1;
        for (int i = 0; i < distances.size(); i++) {
            if (!visited.get(i) && distances.get(i) < minDistance) {
                minDistance = distances.get(i);
                minDistanceVertex = i;
            }
        }
        return minDistanceVertex;
    }
    protected List<List<Integer>> getIndexLists(List<Integer> inputList) {
        List<List<Integer>> resultList = new ArrayList<>();
        Map<Integer, List<Integer>> indexMap = new HashMap<>();

        for (int i = 0; i < inputList.size(); i++) {
            int number = inputList.get(i);
            indexMap.computeIfAbsent(number, k -> new ArrayList<>()).add(i);
        }

        for (int i = 0; i < inputList.size(); i++) {
            List<Integer> indexes = indexMap.getOrDefault(i, new ArrayList<>());
            resultList.add(indexes);
        }

        return resultList;
    }
    public void TopologyMoyenne()
    {
        List<List<Integer>> listeAdjacence = this.Topology;
        int size=listeAdjacence.size();
        List<List<Integer>> distance = new ArrayList<>();
        for(int i =0;i<size;i++)
        {
            distance.add(djikstra(listeAdjacence,i,size));
        }
        List<Double> moy = new ArrayList<>();
        for(int i =0;i<size;i++)
        {
            int sum=0;
            for(int j=0;j<size;j++) {
                sum+=distance.get(j).get(i);
            }
            moy.add((double) sum / listeAdjacence.size());
        }
        this.topologyMoyenneGlobal=moy;
        //return moy;
    }
    public void setTopology(List<List<Integer>> Topology)
    {
        this.Topology=Topology;
    }
    public void closeCache()
    {
        this.cache.close();
    }
    public int getId() {
        return id;
    }
    public int getChargeReseauxIncrease() {
        return chargeReseauxIncrease;
    }
    public int getChargeReseauxReStore() {
        return chargeReseauxReStore;
    }
    protected int id;

    public int getChargeReseauxRead() {
        return chargeReseauxRead;
    }

    public int getChargeReseauxWrite() {
        return chargeReseauxWrite;
    }

    public int getChargeReseauxStore() {
        return chargeReseauxStore;
    }

    public int getChargeReseaux() {
        return this.chargeReseauxIncrease+this.chargeReseauxReStore+this.chargeReseauxStore+this.chargeReseauxWrite+this.chargeReseauxRead;
    }
    @Override
    public Message read(String filename, int replique) {
        return null;
    }
    public Message readTo(Message msg){return null;}
    public Message give(String filename)
    {
        return null;
    }
    @Override
    public Message store(Message msg, int replique) {
        return null;
    }
    public Message reStoreTo(Message msg) {
        System.out.println("restoreToAb");return null;}
    public Message storeTo(Message msg){System.out.println("storeToAb");return null;}
    public Message take(Message msg){
        System.out.println("takeAb");return null;}
    @Override
    public Message write(Message msg, int replique) {
        System.out.println("write");
        return null;
    }
    public Message writeTo(Message msg){
        System.out.println("aled");return null;}
    public void increaseTo(int increase,List<AbstractNode> hubAlreadyWarned) // broadcast and prune
    {

        chargeReseauxIncrease+=1;
        List<AbstractNode> notWarned = voisins.stream().filter(f-> !hubAlreadyWarned.contains(f)).toList();
        List<AbstractNode> concat = new ArrayList<>(notWarned);
        concat.addAll(hubAlreadyWarned);
        concat.add(this);
        int i =0;
        while(i<notWarned.size())
        {
            notWarned.get(i).increaseTo(increase,concat);
            i++;
        }
        this.nbrFichierMax=increase;
    }
    public Message removeTo(Message msg)
    {return null;}
    public int getNbrFichierMax() {
        return this.nbrFichierMax;
    }
    public int getNbrFichier() {
        return this.nbrFichier;
    }

    public void setVoisins(List<AbstractNode> voisins) {
        this.voisins = voisins;
    }
}
