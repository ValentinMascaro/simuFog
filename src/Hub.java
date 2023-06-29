import com.sun.security.jgss.GSSUtil;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

public class Hub implements Components {
    private final int id;
    private List<Hub> voisinsHub;
    private List<Node> voisinsNode;
    private HashMap<String,List<Integer>> fichiers; // fichier x est dans les nodes [a,b,c]
    private List<Double> topologyMoyenneGlobal;
    private List<List<Integer>> reseauDeVoisins;
    private List<fichierDemande> fichierDemande;
    private int nbrFichier;
    private int nbrFichierMax;
    private List<Integer> prev;

    public Hub(int id,int nbrFichierMax)
    {
        this.id=id;
        this.voisinsHub=new ArrayList<>();
        this.voisinsNode=new ArrayList<>();
        this.fichierDemande=new ArrayList<>();
        this.fichiers=new HashMap<>();
        this.nbrFichierMax=nbrFichierMax;
        this.nbrFichier=0;
    }

    public List<Integer> getVoisinsHub()
    {
        return voisinsHub.stream().map(Hub::getId).collect(Collectors.toList());
    }
    public void setNumberHub(int size)
    {
        this.reseauDeVoisins=new ArrayList<>();
        for(int i=0;i<size;i++)
        {
            this.reseauDeVoisins=new ArrayList<>();
        }
    }
    private void addFichierDemande(String filename)
    {
        try{
            this.fichierDemande.stream().filter(f->f.getNom().equals(filename)).toList().get(0).addDemande();
            this.fichierDemande.sort(Comparator.comparingInt(a -> a.getDemande()));
        }catch (IndexOutOfBoundsException e){
            this.fichierDemande.add(new fichierDemande(1,filename));
        }

    }
    public void addToVoisinsReseau(List<Integer> adjacent,int idHub)
    {
        this.reseauDeVoisins.set(idHub,adjacent);
    }

    public boolean store(String filename,int replique)
    {
        List<Integer> pref = pref(filename,this.topologyMoyenneGlobal);
        System.out.println("pref : "+pref);
        int r=0;
        int i=0;
        while(r<replique)
        {
            int ii=i;

            if(this.voisinsHub.stream().anyMatch(f->f.getId()==pref.get(ii))){
                Hub tmp=this.voisinsHub.stream().filter(f->f.getId()==pref.get(ii)).toList().get(0);
                if(tmp.take(filename,10))
                {
                    System.out.println("if");
                    r++;
                    if(this.fichiers.get(filename)!=null)
                    {
                        this.fichiers.get(filename).add(pref.get(i));
                    }
                    else
                    {
                        this.fichiers.put(filename,new ArrayList<>(pref.get(i)));
                    }
                }
            }
            else{
                System.out.println("Else");
                System.out.println(prev);
                System.out.println(pref.get(ii));
                Hub tmp = this.voisinsHub.stream().filter(f->f.getId()==prev.get(pref.get(ii))).toList().get(0);
                if(tmp.to(filename,10,pref.get(i)))
                {
                    r++;
                }
            }

            i++;
            if(i>pref.size())
            {
                System.out.println("TODO i>pref.size()");
                break;
            }
        }
        return true;

    }

    private boolean to(String filename,int poids, Integer hub) {
        System.out.println(this.getId() + " to "+hub);
        System.out.println(prev);
        if(this.voisinsHub.stream().anyMatch(f->f.getId()==hub)) {
            Hub tmp = this.voisinsHub.stream().filter(f->f.getId()==hub).toList().get(0);
            System.out.println("Hub "+this.getId()+" direct send to "+hub);
            return tmp.take(filename,poids);
        }
        else
        {
            Hub tmp = this.voisinsHub.stream().filter(f->f.getId()==prev.get(hub)).toList().get(0);
            System.out.println("Hub "+this.getId()+" send to "+hub+" via "+prev.get(hub));
            return tmp.to(filename,poids,hub);
        }
    }

    private boolean take(String filename, int poids) {
        if(this.nbrFichier<nbrFichierMax)
        {
            System.out.println("Hub "+this.getId()+": do something with node");
            this.nbrFichier++;
            return true;
        }
        else
        {
            System.out.println("Hub "+this.getId()+": do something else");
            return false;
        }
    }

    public void TopologyMoyenne()
    {
        List<List<Integer>> listeAdjacence = this.reseauDeVoisins;
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
    private List<Integer> pref(String filename, List<Double> hubTopologyMoyenne)
    {
        Integer hash = calculateHashInt(filename);
        List<Integer> integerList = hubTopologyMoyenne.stream()
                .mapToInt(Double::intValue)
                // .sorted()
                .boxed()
                .toList();
        List<List<Integer>> listeIndexRepet = getIndexLists(integerList).stream().filter(f -> !f.isEmpty()).toList();
        List<Integer> pref = new ArrayList<>();
        for(int i =0;i<listeIndexRepet.size();i++)
        {
            Random rand = new Random(hash*i%listeIndexRepet.size());
            Collections.shuffle(listeIndexRepet.get(i),rand);
            pref.addAll(listeIndexRepet.get(i));
        }
        return pref;
    }
    private List<List<Integer>> getIndexLists(List<Integer> inputList) {
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
    private String calculateHashString(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] encodedHash = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : encodedHash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return null;
    }
    private int calculateHashInt(String input) {
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
        if(this.getId()==source) {

            List<Integer> voisinsHubInt = this.voisinsHub.stream().map(f->f.getId()).toList();
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
            }
            this.prev = prev;
        }
        return listDistance;
    }

    private static int minDistanceToU(List<Integer> distances,List<Boolean> visited) {
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

    public int getId() {
        return id;
    }

    @Override
    public void connectTo(Components components) {
        if(components instanceof Hub)
        {
            this.voisinsHub.add((Hub)components);
        }
        else {
            this.voisinsNode.add((Node)components);
        }
    }

    public void setVoisinsHub(List<Hub> voisinsHub) {
        this.voisinsHub = voisinsHub;
    }

    public void setReseauDeVoisins(List<List<Integer>> reseauDeVoisins) {
        this.reseauDeVoisins = reseauDeVoisins;
    }

    public List<Double> getTopologyMoyenneGlobal() {
        return topologyMoyenneGlobal;
    }

    public List<Integer> getPrev() {
        return prev;
    }
}
