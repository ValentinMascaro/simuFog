import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class fichierDemande {
    private int demande;
    private String nom;
    public String contenu;
    private Node node;

    private int poids;
    private boolean lock;
    private HashMap<Integer,Integer> hubIDemande;

    public fichierDemande(int demande, String nom) {
        this.demande = demande;
        this.nom = nom;
        this.lock=true;
        this.hubIDemande=new HashMap<>();
    }
    public fichierDemande(int demande, String nom,String contenu) {
        this.demande = demande;
        this.nom = nom;
        this.contenu=contenu;
        this.lock=true;
        this.hubIDemande=new HashMap<>();
    }
    public void lock(boolean isLibre)
    {
        this.lock=isLibre;
    }
    public boolean isLibre()
    {
        return this.lock;
    }
    public int calculPoids()
    {
        return this.getDemande();
        // TODO
    }
    public int getPoids() {
        return poids;
    }

    public void setPoids(int poids) {
        this.poids = poids;
    }

    public fichierDemande(int demande, String nom, Node node) {
        this.demande = demande;
        this.nom = nom;
        this.node=node;
        this.lock=true;
    }
    public void setNode(Node node)
    {
        this.node=node;
    }

    public Node getNode() {
        return node;
    }

    public void addDemande()
    {
        this.demande++;
    }

    public int getDemande() {
        return demande;
    }

    public String getNom() {
        return nom;
    }
    public void setHubIDemande(int hubI,int demande)
    {
        this.hubIDemande.put(hubI,demande);
    }
    public void addHubIdemande(int hubI)
    {
        int tmp = 1;
        if(this.hubIDemande.containsKey(hubI))
        {
            tmp= this.hubIDemande.get(hubI)+1;
        }
        this.setHubIDemande(hubI,tmp);
    }
    public HashMap<Integer, Integer> getHubIDemande() {
        return hubIDemande;
    }
}
