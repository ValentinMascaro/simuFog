import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class Node implements Components{
    private int id;
    private List<Hub> voisinsHub;
    private List<Node> voisinsNode;
    private HashMap<Integer,List<String>> contenu; // à l'emplacement x se trouve le fichier y
    public Node(int id)
    {
        this.id=id;
        this.voisinsHub=new ArrayList<>();
        this.voisinsNode=new ArrayList<>();
        this.contenu=new HashMap<>(100);
        for(int i = 0; i<100;i++)
        {
            contenu.put(i,new ArrayList<>());
        }
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
    public boolean store(String filename)
    {
        contenu.get(calculateHashInt(filename)%100).add(filename);
        return true;
    }
    public String read(String filename)
    {
         return contenu.get(calculateHashInt(filename)%100).stream().filter(f -> f.equals(filename)).collect(Collectors.toList()).get(0);
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
}
