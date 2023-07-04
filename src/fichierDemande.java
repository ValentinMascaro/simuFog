import java.util.ArrayList;
import java.util.List;

public class fichierDemande {
    private int demande;
    private String nom;
    private Node node;

    private int poids;

    public fichierDemande(int demande, String nom) {
        this.demande = demande;
        this.nom = nom;
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

}
