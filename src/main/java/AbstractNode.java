import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

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

    /*CachingProvider cachingProvider;
    CacheManager cacheManager;
    MutableConfiguration<String, List<Integer>> config;
    Cache<String, List<Integer>> cache;*/
    protected cache cache;
    protected List<Double> topologyMoyenneGlobal;
    protected List<Integer> topoLocal;
    protected List<List<Integer>> djkstraHubI;

    protected String getFichierDemande()
    {
        System.out.println("ab getFichierDemande");
        return "ab";
    }
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
    protected List<Integer> djikstra(List<List<Integer>> graph, int source,int size) {
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
        this.djkstraHubI=distance;
        List<Double> moy = new ArrayList<>();
        for(int i =0;i<size;i++)
        {
            int sum=0;
            for(int j=0;j<size;j++) {
                sum+=distance.get(j).get(i);
            }
            moy.add((double) sum / listeAdjacence.size());
        }
        this.topoLocal=new ArrayList<>();
        List<List<Integer>> tmpTopo = getIndexLists(distance.get(this.getId())).stream().filter(f->!f.isEmpty()).toList();
        for(List<Integer> l : tmpTopo)
        {
            topoLocal.addAll(l);
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
        this.cache=null;
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
        System.out.println("ab");
        return null;
    }
    public Message readTo(Message msg){
        System.out.println("ab");return null;}
    public Message give(Message msg)
    {
        System.out.println("ab");
        return null;
    }
    @Override
    public Message store(Message msg, int replique) {
        chargeReseauxStore+=1;
        String filename=msg.nomFichier;
        List<Integer> pref = pref(msg.nomFichier,this.topologyMoyenneGlobal);
        int r=0;
        int i=0;
        List<Integer> presence =new ArrayList<>();
        while(r<replique)
        {
            if(i>=pref.size())
            {
                // System.out.println("Max capacity limit need to be increase ");
                List<AbstractNode> alreadywarned=new ArrayList<>(voisins);
                alreadywarned.add(this);

                //  System.out.println("increase size"+i+" "+this.nbrFichier+" / "+this.nbrFichierMax);
                int increase = this.getNbrFichierMax()*2;
                for(AbstractNode h : voisins)
                {
                    //h.increaseTo((int)Math.floor(0.5+this.nbrFichierMax*0.1),alreadywarned);
                    h.increaseTo(increase,alreadywarned);
                }
                this.nbrFichierMax=increase;
                i=0;
            }
            //  if(pref.get(i)==this.getId()){chargeReseaux-=1;} // on ne compte pas un envoi vers soit meme comme une charge réseaux
            //System.out.println("Hub "+this.getId()+ " : Ask "+pref.get(i)+" to store "+filename);
            if(this.storeTo(new Message(1,msg.nomFichier,msg.contenuFichier,pref.get(i),1)).msgType==1)
            {
                r++;
                presence.add(pref.get(i));
                // TODO cache
                //  System.out.println("Hub "+this.getId()+" Succesfully store "+filename + " in "+pref.get(i));
            }
            i++;

        }
        this.cache.put(filename,presence);
        //  System.out.println("Hub "+this.getId()+" stored "+filename+" in hub : "+this.fichiersHub.get(filename)+" pref was "+pref);
        return new Message(1,msg.nomFichier, msg.contenuFichier, -1,-1); // on répond juste true en gros
    }

    public Message storeTo(Message msg) {
        int hubId=msg.destinataire;
        String filename=msg.nomFichier;
        if(hubId==this.getId())
        {
            return this.take(msg);
        }
        chargeReseauxStore+=1;
        // System.out.println("Hub "+this.getId()+" go by "+routingTable.get(hubId).getId());
        //return this.routingTable.get(hubId).storeTo(filename,hubId,poids);
        if(this.routingTable.get(hubId).storeTo(msg).msgType==1) // si un de mes voisins dit avoir un fichier, autant le retenir dans mon cache
        {
            //if(this.routingTable.get(hubId).getId()==hubId){
            if(this.cache.containsKey(filename))
            {
                if(!this.cache.get(filename).contains(hubId)){
                    this.cache.get(filename).add(hubId);
                }
            }
            else
            {
                this.cache.put(filename,new ArrayList<>(hubId));
            }
            return new Message(1,null,null,-1,-1);
            //}
            //return true;
        }
        return new Message(0);
    }
    public boolean reStore(Message msg)
    {
        System.out.println("restoreAb");return false;
    }
    public Message reStoreTo(Message msg) {
        System.out.println("restoreToAb");return null;}

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
        //concat.add(this);
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
    protected List<Integer> pref(String filename, List<Double> hubTopologyMoyenne)
    {
        java.lang.Integer hash = calculateHashInt(filename);
        List<java.lang.Integer> integerList = hubTopologyMoyenne.stream()
                .mapToInt(Double::intValue)
                // .sorted()
                .boxed()
                .toList();
        List<List<java.lang.Integer>> listeIndexRepet = getIndexLists(integerList).stream().filter(f -> !f.isEmpty()).toList();
        List<java.lang.Integer> pref = new ArrayList<>();
        for(int i =0;i<listeIndexRepet.size();i++)
        {
            Random rand = new Random(hash*i);
            Collections.shuffle(listeIndexRepet.get(i),rand);
            pref.addAll(listeIndexRepet.get(i));
        }
        return pref;
    }

    public List<List<Integer>> getDjkstraHubI() {
        return djkstraHubI;
    }
}
