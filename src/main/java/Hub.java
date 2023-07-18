import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import java.util.*;

public class Hub extends AbstractNode {
    private HashMap<String,fichierDemande> fichierDemande; // Liste de <String filename, nbr de demande> s'actualise à chaque appel de read (TODO)
    private HashMap<String,fichierDemande> fichiersHub; //
    public Hub(int id,int nbrFichierMax)
    {
        this.chargeReseauxRead=0;
        this.chargeReseauxIncrease=0;
        this.chargeReseauxWrite=0;
        this.chargeReseauxStore=0;
        this.chargeReseauxReStore=0;
        this.id=id;
        this.voisins =new ArrayList<>();
        this.fichierDemande =new HashMap<>();
        this.nbrFichierMax=nbrFichierMax;
        this.nbrFichier=0;
        this.fichiersHub =new HashMap<>();
        this.cachingProvider = Caching.getCachingProvider();
        this.cacheManager = cachingProvider.getCacheManager();
        this.config
                = new MutableConfiguration<>();
        cache = cacheManager
                .createCache("simpleCache"+this.getId(), config);
        //cache.close();
    }
    private void addFichierDemande(String filename)
    {
        if(this.fichiersHub.containsKey(filename))
        {
            this.fichiersHub.get(filename).addDemande();
            return;
        }
        if(!this.fichierDemande.containsKey(filename))
        {
            fichierDemande.put(filename,new fichierDemande(0,filename));
        }
        fichierDemande.get(filename).addDemande();

    }
    @Override
    public Message read(String filename, int replique) {
        List<Integer> presence = new ArrayList<>();
        List<Message> listeRetour = new ArrayList<>();
        chargeReseauxRead+=1;
        addFichierDemande(filename);
        int r=0;
        if(cache.containsKey(filename))
        {
            List<Integer> leCache = cache.get(filename);
            int i=0;
            while(i<leCache.size() && r<replique)
            {
                Message retour = readTo(new Message(1,filename,null,leCache.get(i),-1));
                if(retour.msgType==1)
                {
                    listeRetour.add(retour);
                    r++;
                    presence.add(leCache.get(i));
                }
                i++;
            }
        }
        int i=0;
        List<Integer> pref = pref(filename,topologyMoyenneGlobal);
        if(cache.containsKey(filename))
        {
            pref.removeAll(cache.get(filename)); // TODO opti
        }
        while(r<replique)
        {
            if(i>=pref.size())
            {
                // TODO same as store

                return new Message(0,null,null,-1,-1);
            }
            //if(pref.get(i)==this.getId()){chargeReseaux-=1;}
            //System.out.println("Hub "+this.getId()+ " : Ask "+pref.get(i)+" to give "+filename);
            Message tmpRetour =readTo(new Message(1,filename,null,pref.get(i),-1,0));

            if (tmpRetour.msgType==1)
            {
                listeRetour.add(tmpRetour);
                //  System.out.println("Hub "+this.getId()+" Succesfuly found a replica ");
                presence.add(pref.get(i));
                r++;
                // TODO get the replica and check it's good version
            }
            i++;
        }

        if(fichierDemande.containsKey(filename)) {

            if (pireFichierDemande()!=null && (pireFichierDemande().getDemande() < fichierDemande.get(filename).getDemande())) {
                List<Integer> tmp = listeRetour.stream().map(f->f.distance).toList();
                List<Message> sortedMsg=listeRetour.stream().sorted(Comparator.comparingInt(a -> a.distance)).toList();
                //System.out.println("Hub : "+this.getId()+" "+listeRetour.stream().map(f->f.destinataire).toList());
                //System.out.println("Hub : "+this.getId()+" "+presence);
                int max=sortedMsg.get(0).destinataire;
                    int d=0;
                    int r2=0;
                    while(presence.contains(topoLocal.get(d)))
                    {
                        d++;
                        r2++;
                    }
                    if(r2<replique)
                    {
                       // System.out.println("Hubs : "+this.getId()+"d : "+d+" topo : "+topoLocal+" destinaitre : "+max+"  "+presence);
                        if(removeTo(new Message(1,filename,null,max,this.fichierDemande.get(filename).getDemande())).msgType==1) {
                            //System.out.println("Hub "+this.getId()+" ajout fichier "+filename+" hub "+topoLocal.get(d));
                            storeTo(new Message(2, filename, listeRetour.get(0).contenuFichier, topoLocal.get(d), this.fichierDemande.get(filename).getDemande()));
                            presence.remove(Integer.valueOf(max));
                            presence.add(topoLocal.get(d));
                        }

                    }
                }
            }

        if(cache.containsKey(filename))
        {
            cache.remove(filename);
        }
        cache.put(filename,presence);
        // System.out.println("Hub "+this.getId()+" found "+filename+" at hubs : "+findWhere);
        return new Message(1,null,null,-1,-1);
    }

    @Override
    public Message readTo(Message msg) {
        int hubId= msg.destinataire;
        String filename= msg.nomFichier;
        if(this.getId()==hubId)
        {
            return give(filename);
        }
        msg.distance+=1;
        chargeReseauxRead+=1; //1724 4556
        //System.out.println("Hub "+this.getId()+" go by "+routingTable.get(hubId).getId());
        //return this.routingTable.get(hubId).readTo(filename,hubId);
        if(this.routingTable.get(hubId).readTo(msg).msgType==1) // si un de mes voisins dit avoir un fichier, autant le retenir dans mon cache
        {
            //   if(this.routingTable.get(hubId).getId()==hubId){
            if(this.cache.containsKey(filename))
            {
                this.cache.get(filename).add(hubId);
            }
            else
            {
                this.cache.put(filename,new ArrayList<>(hubId));
            }
            return msg;
            // }
            //return true;
        }
        return new Message(0); //false
    }

    @Override
    public Message give(String filename) {
        if(fichiersHub.containsKey(filename))
        {
            //  System.out.println("Hub "+this.getId()+" lecture de "+filename+" sur la node "+this.fichierNode.get(filename).getNode().getId());
            return new Message(1,filename,fichiersHub.get(filename).contenu,this.getId(),-1);
        }
        return new Message(0);
    }

    @Override
    public Message removeTo(Message msg) {
        int destinaire = msg.destinataire;
        if(destinaire==this.getId())
        {
            if(this.fichiersHub.get(msg.nomFichier).getDemande()<msg.poidsFichier)
            {
                this.fichierDemande.put(msg.nomFichier,this.fichiersHub.get(msg.nomFichier));
                this.fichiersHub.remove(msg.nomFichier);
                this.nbrFichier--;
                return new Message(1);
            }
            return new Message(0);
        }
        return this.routingTable.get(destinaire).removeTo(msg);
    }



    @Override
    public Message take(Message msg) {
        String filename = msg.nomFichier;
        if(msg.msgType==2)
        {
            this.nbrFichier++;
            int demande=0;
            if(this.fichierDemande.containsKey(filename))
            {
                demande=this.fichierDemande.get(filename).getDemande();
                this.fichierDemande.remove(filename);
            }
            this.fichiersHub.put(filename,new fichierDemande(demande,filename));
           // this.fichiersHub.get(filename).lock(false); // pas le droit d'y toucher
            return new Message(1,filename,msg.contenuFichier,msg.destinataire,msg.poidsFichier);
        }
        if(this.nbrFichier<nbrFichierMax && !this.fichiersHub.containsKey(filename))
        {
            //  System.out.println("Hub "+this.getId()+": store in node");
            this.nbrFichier++;
            int demande = 0;
            if(this.fichierDemande.containsKey(filename))
            {
                demande = this.fichierDemande.get(filename).getDemande();
                this.fichierDemande.remove(filename);
            }
            this.fichiersHub.put(filename,new fichierDemande(demande,filename));
            if(msg.msgType==2){

            }
            return new Message(1,filename,msg.contenuFichier,msg.destinataire, msg.poidsFichier);
        }
        return new Message(0);
    }
    @Override
    public Message reStoreTo(Message msg) {
        return super.reStoreTo(msg);
    }





    @Override
    public Message write(Message msg, int replique) {
        this.chargeReseauxWrite+=1;
        String filename=msg.nomFichier;
        List<Integer> presence = new ArrayList<>();
        List<Integer> pref = pref(filename,topologyMoyenneGlobal);
        //System.out.println("/!\\Hub "+this.getId()+" rewriting "+filename+" by "+newContenu);
        int r=0;
        if(cache.containsKey(filename))
        {
            pref.removeAll(cache.get(filename));
            List<Integer> leCache=cache.get(filename);
            int i =0;
            while(i<leCache.size() && r<replique)
            {
                if(writeTo(new Message(1,filename,msg.contenuFichier,leCache.get(i),-1)).msgType==1)
                {
                    r++;
                    presence.add(leCache.get(i));
                }
                i++;
            }
        }
        int i=0;
        while(r<replique)
        {
            if(i>=pref.size())
            {
                System.out.println("Hub "+this.getId()+ " Write :Can't find enought replica to write "+filename+" "+r+" / "+replique);
                return new Message(0);
            }
            // if(pref.get(i)==this.getId()){chargeReseaux-=1;}
            //System.out.println("Hubs "+this.getId()+" Write to "+filename+" "+newContenu+" "+pref.get(i));
            if(writeTo(new Message(1,filename,msg.contenuFichier,pref.get(i),-1)).msgType==1)
            {
                r++;
                presence.add(pref.get(i));
                // TODO get the replica and check it's good version
            }
            i++;
        }
        if(cache.containsKey(filename))
        {
            cache.remove(filename);
        }
        cache.put(filename,presence);
        return new Message(1,null,null,-1,-1);
    }

    @Override
    public Message writeTo(Message msg) {
        String filename=msg.nomFichier;
        String newContenu=msg.contenuFichier;
        int h = msg.destinataire;
        if(fichiersHub.containsKey(filename))
        {
          //  System.out.println("writeIn");
            return this.writeIn(msg);
        }
        if(h==this.getId())
        {
           // System.out.println("falseWrite");
            return new Message(0);
        }
        this.chargeReseauxWrite+=1;
        //      System.out.println("Hubs "+this.getId()+" need to "+h);
        //    System.out.println("Hub "+this.getId()+" go by "+routingTable.get(h).getId());
        //  return this.routingTable.get(h).writeTo(filename,newContenu,h);
      //  System.out.println("msgRetour");
        Message msgRetour = this.routingTable.get(h).writeTo(msg);
        if(msgRetour.msgType==1) // si un de mes voisins dit avoir un fichier, autant le retenir dans mon cache
        {
            //if(this.routingTable.get(h).getId()==h){
            if(this.cache.containsKey(filename))
            {
                this.cache.get(filename).add(h);
            }
            else
            {
                this.cache.put(filename,new ArrayList<>(h));
            }
            return msgRetour;
            //}
            //    return true;
        }
        return msgRetour;
    }
    private Message writeIn(Message msg)
    {
        this.fichiersHub.get(msg.nomFichier).contenu= msg.contenuFichier;
        return new Message(1);
    }


    private fichierDemande pireFichierDemande() {
        String keyWithSmallestNumber = null;
        int smallestNumber = Integer.MAX_VALUE;
        for (Map.Entry<String, fichierDemande> entry : fichiersHub.entrySet()) {
            if(entry.getValue().isLibre()) {
                int number = entry.getValue().getDemande();
                if (number < smallestNumber) {
                    smallestNumber = number;
                    keyWithSmallestNumber = entry.getKey();
                }
            }
        }if(keyWithSmallestNumber!=null) {
            return fichiersHub.get(keyWithSmallestNumber);
        }
        return null;
    }

    @Override

    public  String getFichierDemande() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Hubs "+this.getId()+" :\n");
        for (String key : this.fichiersHub.keySet()) {
            fichierDemande fd = this.fichiersHub.get(key);
            String nom = fd.getNom();
            stringBuilder.append("Clé : ").append(key).append(" in ").append(this.fichiersHub.get(key).getDemande());
            if(fichiersHub.get(key).isLibre())
            {
                stringBuilder.append(" libre \n");
            }
            else
            {
                stringBuilder.append(" lock \n");
            }
        }
        for (String key : this.fichierDemande.keySet()) {
            fichierDemande fd = this.fichierDemande.get(key);
            String nom = fd.getNom();
            stringBuilder.append("Clé : ").append(key).append(", Nom : ").append(this.fichierDemande.get(fd.getNom()).calculPoids()).append("-").append(nom).append("\n");
        }

        return stringBuilder.toString();
    }

}