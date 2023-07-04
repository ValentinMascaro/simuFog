import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

public class Node implements Components{
    private int id;
    private List<Hub> voisinsHub;
    private List<Node> voisinsNode;
    private Hashtable<String,String> contenu; // hashMap<filename,contenu> pour l'instant pas de contenu, on se contente d'avoir nom,nom
    public Node(int id)
    {
        this.id=id;
        this.voisinsHub=new ArrayList<>();
        this.voisinsNode=new ArrayList<>();
        this.contenu=new Hashtable<>();
    }
    public boolean store(String filename)
    {
        //System.out.println("Node "+this.getId()+" store "+filename);
        this.contenu.put(filename,"Le"+filename);
        return true;
    }
    public boolean read(String filename)
    {
       // System.out.println("Node "+this.getId()+" give "+filename);
        return this.contenu.containsKey(filename);
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

    public List<Hub> getVoisinsHub() {
        return voisinsHub;
    }

    public int getId() {
        return id;
    }

    public void remove(String nom) {
        this.contenu.remove(nom);
    }
}
