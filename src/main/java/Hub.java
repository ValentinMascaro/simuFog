import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.spi.CachingProvider;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class Hub implements Components {

    private int chargeReseaux;
    public int getChargeReseaux()
    {
        return chargeReseaux;
    }
    private Hashtable<String,String> cacheMisere;
    private final int id; // identifiant de 0 à h
    private List<Hub> voisinsHub; // liste des hub voisins
    private List<Node> voisinsNode; // liste des nodes voisins
    private HashMap<String,List<Integer>> fichiersHub; // fichier x est dans les hub [a,b,c]
    private Hashtable<String,fichierDemande> fichierNode; // fichier x est dans la node y
    private List<Double> topologyMoyenneGlobal; // distance moyenne pour atteindre i
    private List<List<Integer>> reseauDeVoisins; // réseau sous la forme reseauDeVoisins[i] = liste des voisins du hub i
    private HashMap<String,fichierDemande> fichierDemande; // Liste de <String filename, nbr de demande> s'actualise à chaque appel de read (TODO)
    private int nbrFichier; // nombre de fichier stocké
    private int nbrFichierMax; // max de nombre de fichier stockable
    private HashMap<Integer,Hub> routingTable; // pour aller à la node x qui n'est pas voisine on va à la node prev[x]
    public List<Hub> getVoisinsHub() {
        return voisinsHub;
    }
    public int getNbrFichierMax() {
        return nbrFichierMax;
    }

    public HashMap<Integer, Hub> getRoutingTable() {
        return routingTable;
    }

    private Hub routingTable(int HubId)
    {
        return routingTable.get(HubId);
    }

    public Hub(int id,int nbrFichierMax)
    {
        this.chargeReseaux=0;
        this.id=id;
        this.voisinsHub=new ArrayList<>();
        this.voisinsNode=new ArrayList<>();
        this.fichierDemande =new HashMap<>();
        this.nbrFichierMax=nbrFichierMax;
        this.nbrFichier=0;
        this.fichiersHub =new HashMap<>();
        this.fichierNode=new Hashtable<>();

    }
    public boolean write(String filename,String newContenu,int replique)
    {
        this.chargeReseaux+=1;
        //System.out.println("/!\\Hub "+this.getId()+" rewriting "+filename+" by "+newContenu);
        int r=0;
        int i=0;
        List<Integer> pref = pref(filename,topologyMoyenneGlobal);
        // System.out.println(pref);
        List<Integer> findWhere = new ArrayList<>();
        while(r<replique)
        {
            if(i>=pref.size())
            {
                System.out.println("Hub "+this.getId()+ " :Can't find enought replica to write "+filename+" "+r+" / "+replique);
                return false;
            }
            if(pref.get(i)==this.getId()){chargeReseaux-=1;}
            if(giveMe(filename,pref.get(i)))
            {
                findWhere.add(pref.get(i));
                r++;
                // TODO get the replica and check it's good version
            }
            i++;
        }
        for(Integer h : findWhere)
        {
            if(this.writeTo(filename,newContenu,h))
            {
              //  System.out.println("    rewrite done at hub "+h);
            }
            else {
                System.out.println("    hub "+h+" can't rewrite "+filename);
            }
        }
        return true;
    }

    public boolean writeTo(String filename, String newContenu, Integer h) {
        if(h==this.getId())
        {
            return this.writeInNode(filename,newContenu);
        }
        // System.out.println("Hub "+this.getId()+" go by "+routingTable.get(hubId).getId());
        return this.routingTable.get(h).writeTo(filename,newContenu,h);
    }

    private boolean writeInNode(String filename, String newContenu) {
        return this.fichierNode.get(filename).getNode().write(filename,newContenu);
    }

    private void addFichierDemande(String filename)
    {
        if(this.fichierNode.containsKey(filename))
        {
            this.fichierNode.get(filename).addDemande();
            return;
        }
        if(!this.fichierDemande.containsKey(filename))
        {
            fichierDemande.put(filename,new fichierDemande(0,filename));
        }
        fichierDemande.get(filename).addDemande();

    }
    public boolean giveMe(String filename,int hubId)
    {
        chargeReseaux+=1;
        if(this.getId()==hubId)
        {
            return give(filename);
        }
        //System.out.println("Hub "+this.getId()+" go by "+routingTable.get(hubId).getId());
       return this.routingTable.get(hubId).giveMe(filename,hubId);
    }
    public boolean give(String filename)
    {
        if(fichierNode.containsKey(filename))
        {
          //  System.out.println("Hub "+this.getId()+" lecture de "+filename+" sur la node "+this.fichierNode.get(filename).getNode().getId());
            return this.fichierNode.get(filename).getNode().read(filename);
        }
        return false;
    }
    public String read(String filename,int replique)
    {
        chargeReseaux+=1;
        addFichierDemande(filename);
        int r=0;
        int i=0;
        List<Integer> pref = pref(filename,topologyMoyenneGlobal);
       // System.out.println(pref);
        List<Integer> findWhere = new ArrayList<>();
        while(r<replique)
        {
            if(i>=pref.size())
            {
                // TODO same as store
                System.out.println("Hub "+this.getId()+ " :Can't find enought replica to read "+filename+" "+r+" / "+replique);
                break;
            }
            if(pref.get(i)==this.getId()){chargeReseaux-=1;}
            //System.out.println("Hub "+this.getId()+ " : Ask "+pref.get(i)+" to give "+filename);
            if(giveMe(filename,pref.get(i)))
            {
              //  System.out.println("Hub "+this.getId()+" Succesfuly found a replica ");
                findWhere.add(pref.get(i));
                r++;
                // TODO get the replica and check it's good version
            }
            i++;

        }
       // System.out.println("Hub "+this.getId()+" found "+filename+" at hubs : "+findWhere);
        return filename;
    }
    public void increaseTo(int increase,List<Hub> hubAlreadyWarned) // broadcast and prune
    {

        chargeReseaux+=1;
        List<Hub> notWarned = voisinsHub.stream().filter(f-> !hubAlreadyWarned.contains(f)).toList();
        List<Hub> concat = new ArrayList<>(notWarned);
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
    public boolean store(fichierDemande filename) // remarriage
    {
        chargeReseaux+=1;
        //System.out.println("Hub "+this.getId()+" remarriage "+filename.getNom());
        List<Integer> pref = pref(filename.getNom(),this.topologyMoyenneGlobal);
        int r=0;
        int i=0;
        int replique=1;
        while(r<replique)
        {
            if(i>=pref.size())
            {

               // System.out.println("Max capacity limit need to be increase ");
                List<Hub> alreadywarned=new ArrayList<>(voisinsHub);
                alreadywarned.add(this);
                System.out.println("increase2 size"+" "+this.nbrFichier+" / "+this.nbrFichierMax);
                for(Hub h : voisinsHub)
                {
                    h.increaseTo(this.nbrFichierMax+1,alreadywarned);
                }
                this.nbrFichierMax++;
                i=0;
            }
            if(pref.get(i)==this.getId()){chargeReseaux-=1;}
           // System.out.println("Hub "+this.getId()+ " : Ask "+pref.get(i)+" to store "+filename);
            if(this.reStoreTo(filename.getNom(),pref.get(i)))
            {
                r++;
                if(!this.fichiersHub.containsKey(filename))
                {
                    this.fichiersHub.put(filename.getNom(),new ArrayList<>());
                }
                this.fichiersHub.get(filename.getNom()).add(pref.get(i));
                // TODO cache
                //  System.out.println("Hub "+this.getId()+" Succesfully store "+filename + " somewhere");
            }
            i++;

        }
      //  System.out.println("\tHub "+this.getId()+" restored "+filename.getNom()+" in hub : "+this.fichiersHub.get(filename.getNom())+" pref was "+pref);
        return true;
    }
    public boolean storeTo(String filename,int hubId,int poids)
    {
        chargeReseaux+=1;
        if(hubId==this.getId())
        {
            return this.take(filename,poids);
        }
        // System.out.println("Hub "+this.getId()+" go by "+routingTable.get(hubId).getId());
        return this.routingTable.get(hubId).storeTo(filename,hubId,poids);
    }
   private boolean reStoreTo(String filename, Integer hubId) {
        chargeReseaux+=1;
        int poids=0;
        if(this.fichierDemande.containsKey(filename))
        {
            poids=this.fichierDemande.get(filename).getDemande();
      //      System.out.println("Hubs "+this.getId()+"restoreto "+filename+" poids "+poids);
        //    System.out.println(this.getFichierDemande());
        }
        if(hubId==this.getId())
        {
            return this.take(filename,poids);
        }
        // System.out.println("Hub "+this.getId()+" go by "+routingTable.get(hubId).getId());
        return this.routingTable.get(hubId).reStoreTo(filename,hubId);
    }

    public boolean store(String filename,int replique)
    {
        chargeReseaux+=1;
        List<Integer> pref = pref(filename,this.topologyMoyenneGlobal);
        int r=0;
        int i=0;
        while(r<replique)
        {
            if(i>=pref.size())
            {
                // System.out.println("Max capacity limit need to be increase ");
                List<Hub> alreadywarned=new ArrayList<>(voisinsHub);
                alreadywarned.add(this);
              //  System.out.println("increase size"+i+" "+this.nbrFichier+" / "+this.nbrFichierMax);
                for(Hub h : voisinsHub)
                {
                    h.increaseTo(this.nbrFichierMax+1,alreadywarned);
                }
                this.nbrFichierMax++;
                i=0;
            }
            if(pref.get(i)==this.getId()){chargeReseaux-=1;} // on ne compte pas un envoi vers soit meme comme une charge réseaux
            //System.out.println("Hub "+this.getId()+ " : Ask "+pref.get(i)+" to store "+filename);
            if(this.storeTo(filename,pref.get(i),1))
            {
                r++;
                if(!this.fichiersHub.containsKey(filename))
                {
                    this.fichiersHub.put(filename,new ArrayList<>());
                }
                this.fichiersHub.get(filename).add(pref.get(i));
                // TODO cache
                System.out.println("Hub "+this.getId()+" Succesfully store "+filename + " in "+pref.get(i));
            }
            i++;

        }
        System.out.println("Hub "+this.getId()+" stored "+filename+" in hub : "+this.fichiersHub.get(filename)+" pref was "+pref);
        return true;
    }
    private boolean take(String filename, int poids) {

        if(this.nbrFichier<nbrFichierMax && !this.fichierNode.containsKey(filename))
        {
          //  System.out.println("Hub "+this.getId()+": store in node");
            this.nbrFichier++;
            this.storeInNode(filename);
            return true;
        }
        if(poids==0){return false;}
        else if(this.nbrFichier==this.nbrFichierMax && !this.fichierNode.containsKey(filename))
        {
            fichierDemande file = this.pireFichierDemande();
            if(file!=null && file.calculPoids()<poids)
            {
          //      System.out.println("Hubs "+this.getId()+" accepte "+filename+" retire "+file.getNom());
                this.storeInNode(filename);
                this.fichierDemande.put(file.getNom(),new fichierDemande(file.getDemande(),file.getNom())); // si on remove le fichier, on garde son nombre de demande en tant que demande hors de nos nodes
                //   System.out.println("Hubs "+this.getId()+" : "+"need to move "+file.getNom());
                file.lock(false);
                this.store(file);
           //     System.out.println("Hubs "+this.getId()+" : "+"remove "+file.getNom());
                this.removeFile(file);
                return true;
            }
            return false;
        }

       //     System.out.println("Hub "+this.getId()+": do something else");
        return false;
    }

    private void removeFile(fichierDemande file) {
        //System.out.println("--"+this.getFichierDemande());
        //System.out.println("remove "+file.getNom()+" "+this.getNbrFichier()+" / "+this.getNbrFichierMax());
        this.fichierNode.get(file.getNom()).getNode().remove(file.getNom());
        this.fichierNode.remove(file.getNom());
    }

    private fichierDemande pireFichierDemande() {
        String keyWithSmallestNumber = null;
        int smallestNumber = Integer.MAX_VALUE;
        for (Map.Entry<String, fichierDemande> entry : fichierNode.entrySet()) {
            if(entry.getValue().isLibre()) {
                int number = entry.getValue().getDemande();
                if (number < smallestNumber) {
                    smallestNumber = number;
                    keyWithSmallestNumber = entry.getKey();
                }
            }
        }if(keyWithSmallestNumber!=null) {
            return fichierNode.get(keyWithSmallestNumber);
        }
        return null;
    }

    public int getNbrFichier() {
        return nbrFichier;
    }

    public  String getFichierDemande() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Hubs "+this.getId()+" :\n");
        for (String key : this.fichierNode.keySet()) {
            fichierDemande fd = this.fichierNode.get(key);
            String nom = fd.getNom();
            stringBuilder.append("Clé : ").append(key).append(", Nom : ").append(this.fichierNode.get(fd.getNom()).calculPoids()).append("-").append(nom).append(" in node ").append(fd.getNode().getId()).append(" Contenu : "+fd.getNode().getContenu(fd.getNom())).append("\n");
        }
        for (String key : this.fichierDemande.keySet()) {
            fichierDemande fd = this.fichierDemande.get(key);
            String nom = fd.getNom();
            stringBuilder.append("Clé : ").append(key).append(", Nom : ").append(this.fichierDemande.get(fd.getNom()).calculPoids()).append("-").append(nom).append("\n");
        }

        return stringBuilder.toString();
    }

    private void storeInNode(String filename) {
      Random rand = new Random(calculateHashInt(filename));
      int thatNode = rand.nextInt(0,voisinsNode.size());
      if(this.fichierDemande.containsKey(filename))
      {
          this.fichierNode.put(filename,new fichierDemande(this.fichierDemande.get(filename).getDemande(),filename,this.voisinsNode.get(thatNode)));
          this.fichierDemande.remove(filename);
      }
      else
      {
          this.fichierNode.put(filename,new fichierDemande(0,filename,this.voisinsNode.get(thatNode)));
      }
      this.voisinsNode.get(thatNode).store(filename);
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
            Random rand = new Random(hash*i);
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
        if(this.getId()==source) { // creation table de routage
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
                    this.routingTable.put(p, this.voisinsHub.stream().filter(f -> f.getId() == prev.get(pp)).toList().get(0));
                }
            }
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

    public List<Node> getVoisinsNode() {
        return voisinsNode;
    }
}
