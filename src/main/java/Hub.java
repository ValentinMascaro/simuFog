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
                if(readTo(new Message(1,filename,null,leCache.get(i),-1)).msgType==1)
                {
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
                System.out.println("Hub "+this.getId()+ " :Can't find enought replica to read "+filename+" "+r+" / "+replique);
                return new Message(0,null,null,-1,-1);
            }
            //if(pref.get(i)==this.getId()){chargeReseaux-=1;}
            //System.out.println("Hub "+this.getId()+ " : Ask "+pref.get(i)+" to give "+filename);
            Message retour = readTo(new Message(1,filename,null,pref.get(i),-1,0));
            listeRetour.add(retour);
            if (retour.msgType==1)
            {
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
                Optional<Integer> max = tmp.stream().max(Comparator.comparingInt(a -> a));
                int d=0;
                int r2=0;
                while(presence.contains(topoLocal.get(d)))
                {
                    d++;
                    r2++;
                }
                if(r2<replique)
                {
                    if(removeTo(new Message(1,filename,null,max.get(),this.fichierDemande.get(filename).getDemande())).msgType==1) {
                        storeTo(new Message(2, filename, listeRetour.get(0).contenuFichier, d, this.fichierDemande.get(filename).getDemande()));
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
        return new Message(0);
    }

    @Override
    public Message give(String filename) {
        if(fichiersHub.containsKey(filename))
        {
            //  System.out.println("Hub "+this.getId()+" lecture de "+filename+" sur la node "+this.fichierNode.get(filename).getNode().getId());
            return new Message(1,filename,fichiersHub.get(filename).contenu,-1,-1);
        }
        return new Message(0);
    }

    @Override
    public Message removeTo(Message msg) {
        if(this.fichiersHub.get(msg.nomFichier).getDemande()<msg.poidsFichier)
        {
            return new Message(1);
        }
        return new Message(0);
    }

    @Override
    public Message store(Message msg, int replique) {
        chargeReseauxStore+=1;
        String filename=msg.nomFichier;
        List<Integer> pref = pref(msg.nomFichier,this.topologyMoyenneGlobal);
        int r=0;
        int i=0;
        List<Integer> presence =new ArrayList<>();
        while(r<replique)
        {
            if(i>=pref.size())
            {
                // System.out.println("Max capacity limit need to be increase ");
                List<AbstractNode> alreadywarned=new ArrayList<>(voisins);
                alreadywarned.add(this);
                //  System.out.println("increase size"+i+" "+this.nbrFichier+" / "+this.nbrFichierMax);
                for(AbstractNode h : voisins)
                {
                    //h.increaseTo((int)Math.floor(0.5+this.nbrFichierMax*0.1),alreadywarned);
                    h.increaseTo(this.getNbrFichierMax()*2,alreadywarned);
                }
                this.nbrFichierMax++;
                i=0;
            }
            //  if(pref.get(i)==this.getId()){chargeReseaux-=1;} // on ne compte pas un envoi vers soit meme comme une charge réseaux
            //System.out.println("Hub "+this.getId()+ " : Ask "+pref.get(i)+" to store "+filename);
            if(this.storeTo(new Message(1,msg.nomFichier,msg.contenuFichier,pref.get(i),1)).msgType==1)
            {
                r++;
                presence.add(pref.get(i));
                // TODO cache
                //  System.out.println("Hub "+this.getId()+" Succesfully store "+filename + " in "+pref.get(i));
            }
            i++;

        }
        this.cache.put(filename,presence);
        //  System.out.println("Hub "+this.getId()+" stored "+filename+" in hub : "+this.fichiersHub.get(filename)+" pref was "+pref);
        return new Message(1,msg.nomFichier, msg.contenuFichier, -1,-1); // on répond juste true en gros
    }
    @Override
    public Message storeTo(Message msg) {
        int hubId=msg.destinataire;
        String filename=msg.nomFichier;
        if(hubId==this.getId())
        {
            return this.take(msg);
        }
        chargeReseauxStore+=1;
        // System.out.println("Hub "+this.getId()+" go by "+routingTable.get(hubId).getId());
        //return this.routingTable.get(hubId).storeTo(filename,hubId,poids);
        if(this.routingTable.get(hubId).storeTo(msg).msgType==1) // si un de mes voisins dit avoir un fichier, autant le retenir dans mon cache
        {
            //if(this.routingTable.get(hubId).getId()==hubId){
            if(this.cache.containsKey(filename))
            {
                this.cache.get(filename).add(hubId);
            }
            else
            {
                this.cache.put(filename,new ArrayList<>(hubId));
            }
            return new Message(1,null,null,-1,-1);
            //}
            //return true;
        }
        return new Message(0);
    }
    @Override
    public Message take(Message msg) {
        String filename = msg.nomFichier;
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
                this.fichiersHub.get(filename).lock(false); // pas le droit d'y toucher
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
                System.out.println("Hub "+this.getId()+ " :Can't find enought replica to write "+filename+" "+r+" / "+replique);
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
        return super.writeTo(msg);
    }
    private List<Integer> pref(String filename, List<Double> hubTopologyMoyenne)
    {
        Integer hash = calculateHashInt(filename);
        List<Integer> integerList = hubTopologyMoyenne.stream()
                .mapToInt(Double::intValue)
                // .sorted()
                .boxed()
                .toList();
        List<List<Integer>> listeIndexRepet = getIndexLists(integerList).stream().filter(f -> !f.isEmpty()).toList();
        List<Integer> pref = new ArrayList<>();
        for(int i =0;i<listeIndexRepet.size();i++)
        {
            Random rand = new Random(hash*i);
            Collections.shuffle(listeIndexRepet.get(i),rand);
            pref.addAll(listeIndexRepet.get(i));
        }
        return pref;
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
}