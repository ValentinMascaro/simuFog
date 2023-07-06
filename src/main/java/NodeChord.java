import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class NodeChord {

    private int chargeReseaux;

    public int getChargeReseaux() {
        return chargeReseaux;
    }

    private final int keySpaceId;
    private int id;
    private int ownKeySpace;
    private int keySpace;
    List<NodeChord> voisins;
    List<List<Integer>> topology;
    private HashMap<Integer,NodeChord> routingTable; // pour aller à la node x qui n'est pas voisine on va à la node prev[x]
    private HashMap<Integer,NodeChord> routingTableWithKeySpace;//
    private HashMap<Integer,String> fileStorage;

    public NodeChord(int id, List<List<Integer>> topology, int keySpace)
    {
        this.chargeReseaux=0;
        this.ownKeySpace=keySpace / topology.size() ;
        this.keySpaceId=(1+id)*ownKeySpace;
        this.id=id;
        this.topology=topology;
        this.keySpace=keySpace;
        this.routingTableWithKeySpace=new HashMap<>();
        this.fileStorage=new HashMap<>();
    }
    public void setVoisins( List<NodeChord> voisins)
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
                for (int j = 0; j < this.ownKeySpace + 1; j++) {
                    this.routingTableWithKeySpace.put(k, this.routingTable.get(i));
                    k++;
                }
            }else{k+=10;}
        }
        /*System.out.println("Node "+this.id);
        for(int i=0;i<161;i++)
        {
            if(routingTableWithKeySpace.containsKey(i)) {
                System.out.println(i + " : " + routingTableWithKeySpace.get(i).getId());
            }
        }*/
    }
    public boolean store(String filename, int replique)
    {
        this.chargeReseaux+=1;
        int hash = calculateHashInt(filename);
        List<Integer> posF = new ArrayList<>();
        for(int i =0;i<replique;i++)
        {
            this.storeTo(filename,hash%(i+1));
        }
        return true;
    }
    public boolean storeTo(String filename,int hubId)
    {
        chargeReseaux+=1;
        if(hubId<=this.keySpaceId && keySpaceId-hubId<=ownKeySpace)
        {
            return this.take(filename);
        }
        //System.out.println("Node "+this.getId()+" keyspaceid : "+keySpaceId+" "+hubId+" via ");
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
        chargeReseaux+=1;
        int hash = calculateHashInt(filename);
        List<Integer> posF = new ArrayList<>();
        int i =0;
        while(i<replique)
        {
            if(this.writeTo(filename,contenu,hash%(i+1)))
            {
                i++;
            }
            if(i>3)
            {
                System.out.println("Can't find replica");
                return false;
            }

        }
        return true;
    }
    public boolean writeTo(String filename,String contenu,int hubId)
    {
        chargeReseaux+=1;
        if(hubId<=this.keySpaceId && keySpaceId-hubId<=ownKeySpace)
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
        chargeReseaux+=1;
        int hash = calculateHashInt(filename);
        int i=0;
        List<Integer> posF = new ArrayList<>();
        while(i<replique)
        {
            if(this.readTo(filename,hash%(i+1)))
            {
                i++;
            }
            if(i>3)
            {
                System.out.println("Can't find replica");
                return false;
            }

        }
        return true;
    }
    public boolean readTo(String filename,int hubId)
    {
        chargeReseaux+=1;
        if(hubId<=this.keySpaceId && keySpaceId-hubId<=ownKeySpace)
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
}
