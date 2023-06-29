public class fichierDemande {
    private int demande;
    private String nom;

    public fichierDemande(int demande, String nom) {
        this.demande = demande;
        this.nom = nom;
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
