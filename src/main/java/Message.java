public class Message {
    public int msgType; // 0 false 1 true , inutile pour appel à xxTo 2 store&garde
    public String nomFichier;
    public String contenuFichier;
    public int destinataire;
    public int poidsFichier;
    public int distance;
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
    public Message(int msgType, String nomFichier) {
        this.msgType = msgType;
        this.nomFichier = nomFichier;

    }

}
