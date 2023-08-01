import java.util.*;
import java.util.stream.Collectors;

public class Hub extends AbstractNode {
    private HashMap<String,fichierDemande> fichierDemande; // Liste de <String filename, nbr de demande> s'actualise à chaque appel de read (TODO)
    private HashMap<String,fichierDemande> fichiersHub; //
    public Hub(int id,int nbrFichierMax, Time time,Integer limit)
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
        this.cache=new timedCache(limit,time);
    }
    public Hub(int id,int nbrFichierMax, Integer limit)
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
        this.cache=new sizedCache(limit,this.getId());
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
    public Message read(Message msg) {
        String filename = msg.nomFichier;
        int replique=msg.replique;
     //  System.out.println("Hub "+this.getId()+" try to read "+filename);
        List<Integer> presence = new ArrayList<>();
        List<Message> listeRetour = new ArrayList<>();
        List<Integer> pref = pref(filename,topologyMoyenneGlobal);
        chargeReseauxRead+=1;
        addFichierDemande(filename);
        int r=0;
        if(fichiersHub.containsKey(filename))
        {
            r++;
            presence.add(this.getId());
            pref.removeAll(presence);
        }
        if(cache.containsKey(filename))
        {
            pref.removeAll(cache.get(filename));
            List<Integer> leCache = cache.get(filename);
            leCache.removeAll(presence);
          //  System.out.println("    Hub "+this.getId()+" tough "+filename+" was at hubs : "+leCache);
            int i=0;
            while(i<leCache.size() && r<replique)
            {
                Message retour = readTo(new Message(1,filename,leCache.get(i),this.getId(),0));
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
        while(r<replique)
        {
            if(i>=pref.size())
            {
                System.out.println("Can't find replica");
                return new Message(0,null,null,-1,-1);
            }
            Message retour = readTo(new Message(1,filename,pref.get(i),this.getId(),0));
            if (retour.msgType==1)
            {
                listeRetour.add(retour);
                //  System.out.println("Hub "+this.getId()+" Succesfuly found a replica ");
                presence.add(pref.get(i));
                r++;
                // TODO get the replica and check it's good version
            }
            i++;
        }
       // System.out.println("    Hub "+this.getId()+" found "+filename+" at hubs "+presence);
        boolean success=false;
        if(fichierDemande.containsKey(filename)) {

            if (!this.fichiersHub.containsKey(filename) && nbrFichier<nbrFichierMax){
                List<Pair<Integer,Pair<Integer,Integer>>> newPrefByReplique = new ArrayList<>(); // pair poids,distance
                for(int re=0;re<presence.size();re++)
                {
                    List<Integer> newPoids= newPref(listeRetour.get(re).hubIDemande);
                    List<Integer> newPref = getAsIndexList(newPoids);
                    if(newPref.indexOf(this.getId())<newPref.indexOf(presence.get(re)))
                    {
                        newPrefByReplique.add(new Pair(presence.get(re),new Pair(newPoids.get(this.getId()),listeRetour.get(re).distance)));
                    }
                }
               newPrefByReplique=sortPairs(newPrefByReplique);
                for(int re=0;re<newPrefByReplique.size();re++)
                {
                    chargeReseauxReStore++;
                    if(removeTo(new Message(2,filename,newPrefByReplique.get(re).first(),this.getId())).msgType==1) {
                        this.take(new Message(2,filename));
                        presence.remove(Integer.valueOf(newPrefByReplique.get(re).first()));
                        presence.add(this.getId());
                        success=true;
                        break;
                    }
                }
                }
          }
        if(success)
        {
            if(!fichierDemande.isEmpty()) {
                fichierDemande pireFichierDemande = pireFichierDemande();
                String thatFileIsBadlyPlaced = pireFichierDemande.getNom();
                List<Integer> newPoids = newPref(this.fichiersHub.get(thatFileIsBadlyPlaced).getHubIDemande());
                List<Integer> newPref = getAsIndexList(newPoids);
                for(Integer p : newPref)
                {
                    if(p==this.getId()){break;}
                    Message retour = this.reStoreTo(new Message(3, this.getId(), thatFileIsBadlyPlaced, p, 0, fichiersHub.get(thatFileIsBadlyPlaced).getHubIDemande()));
                    if(retour.msgType==1)
                    {
                        this.fichierDemande.put(thatFileIsBadlyPlaced,pireFichierDemande);
                        nbrFichier--;
                        this.fichiersHub.remove(thatFileIsBadlyPlaced);
                        break;
                    }
                }
            }
        }
        if(cache.containsKey(filename))
        {
            cache.remove(filename);
        }
        cache.put(filename,presence);

      //  System.out.println("    Hub "+this.getId()+" change cache "+filename+" for : "+cache.get(filename));
        return new Message(1,null,null,-1,-1); // oué on devrai retourner le contenu mais flemme, j'ai mal gerer les msg et il est perdu en chemin  ¯\_(ツ)_/¯
    }
    public List<Pair<Integer, Pair<Integer,Integer>>> sortPairs(List<Pair<Integer, Pair<Integer,Integer>>> pairList) {
        Comparator<Pair<Integer, Pair<Integer,Integer>>> customComparator = (p1, p2) -> {
            int distanceComparison = p1.second().second().compareTo(p2.second().second());
            if (distanceComparison > 0) {
                return -1;
            }
            if(distanceComparison<0)
            {
                return 1;
            }else {
                return p1.second().first().compareTo(p2.second().first());
            }
        };

        Collections.sort(pairList, customComparator);

        return pairList;
    }
    public  int getMaxIndex(List<Integer> integerList) {
        int maxIndex = 0;
        int maxValue = Integer.MIN_VALUE;

        for (int i = 0; i < integerList.size(); i++) {
            int currentValue = integerList.get(i);
            if (currentValue > maxValue) {
                maxValue = currentValue;
                maxIndex = i;
            }
        }

        return maxIndex;
    }
    public List<Integer> getActualIndex(List<Integer> newPref, List<Integer> oldPresence) {
        List<Integer> actualIndex = new ArrayList<>();

        for (int element : oldPresence) {
            int index = newPref.indexOf(element);
            actualIndex.add(index);
        }

        return actualIndex;
    }
    private List<Integer> newPref(HashMap<Integer, Integer> hubIDemandeActual) {

        List<Integer> newTopo=new ArrayList<>();
        for(int i=0;i<topologyMoyenneGlobal.size();i++)
        {
            int sum=0;
            //for(int j = 0 ; j < topologyMoyenneGlobal.size();j++)
            //{
            //System.out.println("--");
                for(Integer key : hubIDemandeActual.keySet())
                {
                    int demande = hubIDemandeActual.get(key);
            //        System.out.println("Hub "+key+" demande : "+demande);
              //      System.out.println(djkstraHubI.get(i).get(key)+" * "+demande);
                    sum+=demande*djkstraHubI.get(i).get(key);
                }
          //  System.out.println(i+" "+sum);
                newTopo.add(sum);
            //}
        }
        //System.out.println("newtopo "+newTopo);
        //System.out.println(djkstraHubI);
       return newTopo;
     //   return getAsIndexList(newTopo);;
    }

    private List<Integer> getAsIndexList(List<Integer> intList) {
        List<Integer> indexList = new ArrayList<>();

        // Créer une liste d'indices de 0 à size - 1
        for (int i = 0; i < intList.size(); i++) {
            indexList.add(i);
        }
        // Trier la liste d'indices en utilisant les valeurs correspondantes de la liste de doubles
        Collections.sort(indexList, Comparator.comparingInt(intList::get));

        return indexList;
    }
    @Override
    public Message readTo(Message msg) {
        int hubId= msg.destinataire;
        String filename= msg.nomFichier;
        if(this.getId()==hubId)
        {
            return give(msg);
        }
        msg.distance+=1;
        chargeReseauxRead+=1; //1724 4556
        return routingTable.get(hubId).readTo(msg);
        
    }
    public Message reStoreTo(Message msg) { // copie de store, juste pour comptabilisé les restore
        int hubId=msg.destinataire;
        String filename=msg.nomFichier;
        if(hubId==this.getId())
        {
            return this.take(msg);
        }
        chargeReseauxReStore+=1;
        // System.out.println("Hub "+this.getId()+" go by "+routingTable.get(hubId).getId());
        //return this.routingTable.get(hubId).storeTo(filename,hubId,poids);
        return routingTable.get(hubId).reStoreTo(msg);

    }


    @Override
    public Message give(Message msg) {
        String filename = msg.nomFichier;

        if(fichiersHub.containsKey(filename)) {

            this.fichiersHub.get(filename).addHubIdemande(msg.source);
            //  System.out.println("Hub "+this.getId()+" lecture de "+filename+" sur la node "+this.fichierNode.get(filename).getNode().getId());
            return new Message(1, this.getId(), filename, msg.source, msg.distance, fichiersHub.get(filename).getHubIDemande());
        }
            return new Message(0);
    }

    @Override
    public Message removeTo(Message msg) {
        int destinaire = msg.destinataire;
        if(destinaire==this.getId())
        {
            if(this.fichiersHub.containsKey(msg.nomFichier)) {
                List<Integer> newPoids = newPref(this.fichiersHub.get(msg.nomFichier).getHubIDemande());
                List<Integer> newPref = getAsIndexList(newPoids);
                if (newPref.indexOf(this.getId())>newPref.indexOf(msg.source)) {
                    this.fichierDemande.put(msg.nomFichier, this.fichiersHub.get(msg.nomFichier));
                    this.fichiersHub.remove(msg.nomFichier);
                    this.nbrFichier--;
                    //System.out.println("Hub"+this.getId()+"remove "+msg.nomFichier);
                    if(false)//(nbrFichier<nbrFichierMax)
                    {
                        if(!fichierDemande.isEmpty()) {
                            fichierDemande meilleurFichier = meilleurFichierDemande();
                            String iwantthatfile=meilleurFichier.getNom();
                            if (this.cache.containsKey(iwantthatfile)) {
                                List<Integer> pref = this.cache.get(iwantthatfile);
                                //System.out.println("Hub"+this.getId()+" cache = "+pref);
                                for (Integer p : pref) {
                                    //System.out.println("Hub "+this.getId()+" try to read "+iwantthatfile+" in "+p);
                                    chargeReseauxReStore++;
                                    Message retour =this.reStoreTo(new Message(1, iwantthatfile, p, this.getId()));
                                    if (retour.msgType==1) {
                                        chargeReseauxReStore++;
                                        //System.out.println("Hub"+this.getId()+" read "+iwantthatfile);
                                        if (this.removeTo(new Message(1, iwantthatfile, p, this.getId())).msgType == 1) {
                                       //     System.out.println("Hub"+this.getId()+" got "+iwantthatfile+" from "+p);
                                            this.take(new Message(2, iwantthatfile));
                                            this.fichiersHub.get(iwantthatfile).setHubIDemandeGlobal(retour.hubIDemande);
                                            break;
                                        }
                                     //   System.out.println("Hub"+this.getId()+" Hub "+p+" said no :( for "+iwantthatfile);
                                    }
                                   // System.out.println("Hub "+this.getId()+" couldn't read "+iwantthatfile+" from "+p);
                                }
                            }
                        }
                    }
                    this.cache.put(msg.nomFichier,new ArrayList<>(List.of(msg.source)));
                    return new Message(1);
                }
                return new Message(0);
            }
        }
        return this.routingTable.get(destinaire).removeTo(msg);
    }



    @Override
    public Message take(Message msg) {
        String filename = msg.nomFichier;

        if(msg.msgType==2) // force take
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
            if(msg.msgType==3){
                this.fichiersHub.get(msg.nomFichier).setHubIDemandeGlobal(msg.hubIDemande);
            }
            return new Message(1,filename,msg.contenuFichier,msg.destinataire, msg.poidsFichier);
        }
        return new Message(0);
    }


    @Override
    public Message write(Message msg) {
        int replique=msg.replique;
        //System.out.println("Hub "+this.getId()+"write");
        this.chargeReseauxWrite+=1;
        String filename=msg.nomFichier;
        List<Integer> presence = new ArrayList<>();
        List<Integer> pref = pref(filename,topologyMoyenneGlobal);
        //System.out.println("/!\\Hub "+this.getId()+" rewriting "+filename+" by "+newContenu);

        int r=0;
        if(fichiersHub.containsKey(filename))
        {
            r++;
            presence.add(this.getId());
            pref.removeAll(presence);
        }
        if(cache.containsKey(filename))
        {
            pref.removeAll(cache.get(filename));
            List<Integer> leCache= cache.get(filename);
            leCache.removeAll(presence);
        //    System.out.println("    Hub "+this.getId()+" tough "+filename+" was at hubs : "+leCache);
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
            if(writeTo(new Message(1,filename,msg.contenuFichier,pref.get(i),-1)).msgType==1)
            {
                r++;
                presence.add(pref.get(i));
                // TODO get the replica and check it's good version
            }
            i++;
        }
       // System.out.println("    Hub "+this.getId()+" found "+filename+" were at hubs : "+presence);
        if(cache.containsKey(filename))
        {

            cache.remove(filename);
        }
        cache.put(filename,presence);
      //  System.out.println("    Hub "+this.getId()+" change cache "+filename+" for : "+presence);
        return new Message(1,null,null,-1,-1);
    }

    @Override
    public Message writeTo(Message msg) {
        String filename=msg.nomFichier;
        String newContenu=msg.contenuFichier;
        int h = msg.destinataire;
        if(h==this.getId())
        {
            return writeIn(msg);
        }
        this.chargeReseauxWrite+=1;
        return this.routingTable.get(h).writeTo(msg);
    }
    private Message writeIn(Message msg)
    {
        String filename = msg.nomFichier;

        if(fichiersHub.containsKey(filename)) {
            //this.fichiersHub.get(filename).addHubIdemande(msg.source);
            //  System.out.println("Hub "+this.getId()+" lecture de "+filename+" sur la node "+this.fichierNode.get(filename).getNode().getId());
            return new Message(1, this.getId(), filename, msg.source, msg.distance, fichiersHub.get(filename).getHubIDemande());
        }
        return new Message(0);
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
    private fichierDemande meilleurFichierDemande() {
        String keyWithSmallestNumber = null;
        int biggestNumber = Integer.MIN_VALUE;
        for (Map.Entry<String, fichierDemande> entry : fichierDemande.entrySet()) {
            if(entry.getValue().isLibre()) {
                int number = entry.getValue().getDemande();
                if (number > biggestNumber) {
                    biggestNumber = number;
                    keyWithSmallestNumber = entry.getKey();
                }
            }
        }if(keyWithSmallestNumber!=null) {
            return fichierDemande.get(keyWithSmallestNumber);
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
    public int getDemande(String filename)
    {
        if(this.fichierDemande.containsKey(filename))
        {
            return this.fichierDemande.get(filename).getDemande();
        }
        return 0;
    }
    public void setFakeDemandeFromHubI(String nomfichier, int demande, int hubI)
    {
     if(this.getId()==hubI)
     {
         this.fichiersHub.get(nomfichier).addHubIdemande(hubI,demande);
         this.fichiersHub.get(nomfichier).addDemande(demande);
     }
     else{
         this.fichiersHub.get(nomfichier).addHubIdemande(hubI,demande);
     }
    }
    public void setFakeDemande(String nomFichier,int demande)
    {
        if(this.fichiersHub.containsKey(nomFichier))
        {
            this.fichiersHub.get(nomFichier).addHubIdemande(this.getId(),demande);
            this.fichiersHub.get(nomFichier).addDemande(demande);
            return;
        }
        if(this.fichierDemande.containsKey(nomFichier))
        {
            this.fichierDemande.get(nomFichier).setDemande(demande);
        }else{
            this.fichierDemande.put(nomFichier,new fichierDemande(demande,nomFichier));
        }
    }
    public cache getCache()
    {
        return this.cache;
    }
}