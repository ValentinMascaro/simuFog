import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class NodeChord extends AbstractCompo{

    private int chargeReseaux;
    private int chargeReseauxRead;
    private int chargeReseauxWrite;
    private int chargeReseauxStore;

    private int chargeReseauxIncrease;

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
        return chargeReseaux;
    }

    private final int keySpaceId;
    private int id;
    private int ownKeySpace;
    private int keySpace;
    List<AbstractCompo> voisins;
    List<List<Integer>> topology;
    private HashMap<Integer,AbstractCompo> routingTable; // pour aller à la node x qui n'est pas voisine on va à la node prev[x]
    private HashMap<Integer,AbstractCompo> routingTableWithKeySpace;//
    private HashMap<Integer,String> fileStorage;

    public NodeChord(int id, List<List<Integer>> topology, int keySpace)
    {
        this.chargeReseauxRead=0;
        this.chargeReseauxStore=0;
        this.chargeReseauxWrite=0;
        this.chargeReseaux=0;
        this.ownKeySpace=keySpace / topology.size() ;
        this.keySpaceId=(1+id)*ownKeySpace;
        this.id=id;
        this.topology=topology;
        this.keySpace=keySpace;
        this.routingTableWithKeySpace=new HashMap<>();
        this.fileStorage=new HashMap<>();
    }
    public void setVoisins( List<AbstractCompo> voisins)
    {
        this.voisins=voisins;
    }
    public void setTopology()
    {
        djikstra(topology,this.id,this.topology.size()); // on souhaite juste set la routing table à partir de djkistra, pas besoin des distances
        int k =0;
        for(int i=0;i<topology.size();i++)
        {
            if(routingTable.containsKey(i)) {
                for (int j = 1; j < this.ownKeySpace+1 ; j++) {
                    this.routingTableWithKeySpace.put((i*10)+j, this.routingTable.get(i));

                }
            }
        }
        /*System.out.println("Node : "+this.id);
        for(int i=0;i<161;i++)
        {
            if(routingTableWithKeySpace.containsKey(i)) {
                System.out.println(i + " : " + routingTableWithKeySpace.get(i).getId());
            }
        }*/
    }
    private int posHash(int hash,int r)
    {
        int retour = Math.abs((hash*r)%160);
        if(retour==0){return 1;}
        return retour;
    }
    public boolean store(String filename, int replique)
    {
        this.chargeReseaux+=1;
        this.chargeReseauxStore+=1;
        int hash = calculateHashInt(filename);
        for(int i =0;i<replique;i++)
        {
          //  System.out.println(this.posHash(hash,i+1));
            this.storeTo(filename,this.posHash(hash,i+1));
        }
        return true;
    }
    public boolean isMyOwn(int hubId)
    {
        //System.out.println("IsmyOwn "+this.keySpaceId+" "+hubId);
        return (hubId<=this.keySpaceId && this.keySpaceId-hubId<=9);
    }
    public boolean storeTo(String filename,int hubId)
    {
        this.chargeReseaux+=1;
        this.chargeReseauxStore+=1;
        if(isMyOwn(hubId))
        {
            return this.take(filename);
        }
       // System.out.println("Node "+this.getId()+" keyspaceid : "+keySpaceId+" "+hubId+" via ");
        //System.out.println(this.routingTableWithKeySpace.get(hubId).getId());
        return this.routingTableWithKeySpace.get(hubId).storeTo(filename,hubId);
    }
    public boolean take(String filename)
    {
        //System.out.println("Node "+this.getId()+" take "+filename);
        this.fileStorage.put(calculateHashInt(filename),filename);
        return true;
    }
    public boolean write(String filename, String contenu, int replique)
    {
        this.chargeReseaux+=1;
        this.chargeReseauxWrite+=1;
        int hash = calculateHashInt(filename);
        List<Integer> posF = new ArrayList<>();
        int i =0;
        int r=0;
        while(i<replique)
        {
            if(this.writeTo(filename,contenu,this.posHash(hash,i+1)))
            {
                r++;
            }
            if(i>3)
            {
                System.out.println("Node "+this.getId()+"Can't find replica "+filename+" "+this.posHash(hash,i+1));
                return false;
            }
            i++;
        }
        return true;
    }
    public boolean writeTo(String filename,String contenu,int hubId)
    {
        this.chargeReseaux+=1;
        this.chargeReseauxWrite+=1;
        if(isMyOwn(hubId))
        {
            return this.writeIn(filename,contenu);
        }
      //  System.out.println("Node "+this.getId()+" keyspaceid : "+keySpaceId+" "+hubId+" via ");
        //System.out.println(this.routingTableWithKeySpace.get(hubId).getId());
        return this.routingTableWithKeySpace.get(hubId).writeTo(filename,contenu,hubId);
    }
    public boolean writeIn(String filename, String contenu)
    {
        return this.fileStorage.containsKey(calculateHashInt(filename));
    }
    public boolean read(String filename, int replique)
    {
        this.chargeReseaux+=1;
        this.chargeReseauxRead+=1;
        int hash = calculateHashInt(filename);
        int i=0;
        List<Integer> posF = new ArrayList<>();
        int r=0;
        while(r<replique)
        {
            if(this.readTo(filename,this.posHash(hash,i+1)))
            {
                r++;
            }
            if(i>3)
            {
                System.out.println("Node "+this.getId()+"Can't find replica "+filename+" "+this.posHash(hash,i+1));
                return false;
            }
            i++;
        }
        return true;
    }
    public boolean readTo(String filename,int hubId)
    {
        this.chargeReseaux+=1;
        this.chargeReseauxRead+=1;

        if(isMyOwn(hubId))
        {
            return this.give(filename);
        }
        //System.out.println("Node "+this.getId()+" keyspaceid : "+keySpaceId+" "+hubId+" via ");
     //   System.out.println(this.routingTableWithKeySpace.get(hubId).getId());
        return this.routingTableWithKeySpace.get(hubId).readTo(filename,hubId);
    }
    public boolean give(String filename)
    {
        return this.fileStorage.containsKey(calculateHashInt(filename));
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
    private int calculateHashInt(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] encodedHash = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            BigInteger bigInteger = new BigInteger(1, encodedHash);
            return Math.abs(bigInteger.intValue());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return 0;
    }
    public List<Integer> getDigitList(int number) {
        List<Integer> digitList = new ArrayList<>();
        String numberString = String.valueOf(number);

        for (int i = 0; i < numberString.length(); i++) {
            int digit = Character.getNumericValue(numberString.charAt(i));
            digitList.add(digit);
        }

        return digitList;
    }
    public  String getFichier() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Hubs "+this.getId()+" :\n");
        for (Integer key : this.fileStorage.keySet()) {
            String fd = this.fileStorage.get(key);
            stringBuilder.append("Clé : ").append(key).append(", Nom : ").append(fileStorage.get(key)).append("\n");
        }
        return stringBuilder.toString();
    }
}
