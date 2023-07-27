import java.util.HashMap;

public class Message {
    public int msgType; // 0 false 1 true , inutile pour appel à xxTo 2 store&garde
    public int source;
    public String nomFichier;
    public String contenuFichier;
    public int destinataire;
    public int poidsFichier;
    public int distance;
    public HashMap<Integer,Integer> hubIDemande;
    public int replique;
    public Message(int msgType, String nomFichier, String contenuFichier, int destinataire, int poidsFichier,int distance) {
        this.msgType = msgType;
        this.nomFichier = nomFichier;
        this.contenuFichier = contenuFichier;
        this.destinataire = destinataire;
        this.poidsFichier=poidsFichier;
        this.distance=distance;
    }
    public Message(int msgType, String nomFichier, String contenuFichier, int destinataire, int poidsFichier) {
        this.msgType = msgType;
        this.nomFichier = nomFichier;
        this.contenuFichier = contenuFichier;
        this.destinataire = destinataire;
        this.poidsFichier=poidsFichier;
        this.distance=0;
    }
    public Message(int msgType)
    {
        this.msgType=msgType;
    }
    public Message(int msgType, String nomFichier, String contenuFichier) {
        this.msgType = msgType;
        this.nomFichier = nomFichier;
        this.contenuFichier = contenuFichier;
    }
    public Message(int msgType, String nomFichier, String contenuFichier,int replique) {
        this.msgType = msgType;
        this.nomFichier = nomFichier;
        this.contenuFichier = contenuFichier;
        this.replique=replique;
    }
    public Message(int msgType, String nomFichier) {
        this.msgType = msgType;
        this.nomFichier = nomFichier;

    }
    public Message(int msgType, String nomFichier,int replique) {
        this.msgType = msgType;
        this.nomFichier = nomFichier;
        this.replique=replique;
    }
    public Message(int msgType, String nomFichier, int destinataire, int source) {
        this.msgType = msgType;
        this.nomFichier = nomFichier;
        this.destinataire = destinataire;
        this.source = source;

    }
    public Message(int msgType, String nomFichier, int destinataire, int source,int distance) {
        this.msgType = msgType;
        this.nomFichier = nomFichier;
        this.destinataire = destinataire;
        this.source = source;
        this.distance=distance;
    }

    public Message(int msgType, int source, String nomFichier, int destinataire, int distance, HashMap<Integer, Integer> hubIDemande) {
        this.msgType = msgType;
        this.source = source;
        this.nomFichier = nomFichier;
        this.destinataire = destinataire;
        this.distance = distance;
        this.hubIDemande = hubIDemande;
    }
}
