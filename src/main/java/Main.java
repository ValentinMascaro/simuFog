import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.spi.CachingProvider;
import java.sql.SQLOutput;
import java.util.*;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) {
//Moyenne ASFOptionalDouble[995039.0625]
        List<AbstractCompo> Hubs = new ArrayList<>();
        for (int i = 0; i < 16; i++) {
            Hubs.add(new Hub(i, 2));
        }
        List<Node> Nodes = new ArrayList<>();
        for (int j = 0; j < 200; j++) {
            Nodes.add(new Node(j));
        }
        Collections.shuffle(Nodes, new Random(1));
        List<List<Integer>> topology = new ArrayList<>(List.of(
                List.of(1, 4), // hub 0
                List.of(0, 4, 3), // hub 1
                List.of(7, 8), // hub 2
                List.of(1, 5, 7), // hub 3
                List.of(0, 1, 5),//hub 4
                List.of(4, 3, 7, 6),//hub 5
                List.of(5, 8, 10),//hub 6
                List.of(3, 2, 8, 5, 6),//hub 7
                List.of(2, 7, 6, 9, 10),//hub 8
                List.of(12, 8),//hub 91
                List.of(8, 6, 11),//hub 10
                List.of(12, 10, 13),//hub 11
                List.of(9, 11),//hub 12
                List.of(14, 11),//hub 13
                List.of(13, 15),//hub 14
                List.of(14) // hub 15
        ));

        for (int i = 0; i < topology.size(); i++) {
            Hubs.get(i).setVoisins(topology.get(i).stream().map(f -> Hubs.get(f)).toList());
            Hubs.get(i).setReseauDeVoisins(topology);
            Hubs.get(i).TopologyMoyenne();

            for (int n = 0; n < 6; n++) {
                Hubs.get(i).connectTo(Nodes.get(n));
                Nodes.get(n).connectTo(Hubs.get(i));
                Nodes.remove(n);
            }

        }
        int seed = 4;
        int F = 500;
        int S = 300000;
        simuExpo(Hubs, F, S, seed);
        System.out.println(Hubs.stream().map(f -> "Hubs " + f.getId() + " : " + f.getNbrFichier() + " / " + f.getNbrFichierMax() + " " + f.getFichierDemande() + "\n").toList());
        // System.out.println(Hubs.stream().map(f -> "Hubs " + f.getId() + " charge reseau : " + f.getChargeReseaux() + " \n").toList());


        List<AbstractCompo> nodeChords = new ArrayList<>();
        for (int i = 0; i < 16; i++) {
            nodeChords.add(new NodeChord(i, topology, 160));
        }
        for (int i = 0; i < topology.size(); i++) {
            nodeChords.get(i).setVoisins(topology.get(i).stream().map(f -> nodeChords.get(f)).toList());
            nodeChords.get(i).setTopology();
        }

       List<Integer> alreadtEncounter = simuExpo(nodeChords, F, S, seed);
      System.out.println("Nbr fichier " + alreadtEncounter.size());
        System.out.println(nodeChords.stream().map(f->f.getFichier()).toList());
        List<Integer> ASFRead = Hubs.stream().map(f -> f.getChargeReseauxRead()).toList();
        List<Integer> ASFStore = Hubs.stream().map(f -> f.getChargeReseauxStore()).toList();
        List<Integer> ASFReStore = Hubs.stream().map(f -> f.getChargeReseauxReStore()).toList();
        List<Integer> ASFWrite = Hubs.stream().map(f -> f.getChargeReseauxWrite()).toList();
        List<Integer> ASFGlobal = Hubs.stream().map(f -> f.getChargeReseaux()).toList();
        System.out.println("Moyenne ASF" + ASFGlobal.stream().mapToInt(Integer::intValue).average());
        System.out.println("Moyenne ASF Store" + ASFStore.stream().mapToInt(Integer::intValue).average());
        System.out.println("Moyenne ASF Read" + ASFRead.stream().mapToInt(Integer::intValue).average());
        System.out.println("Moyenne ASF Restore" + ASFReStore.stream().mapToInt(Integer::intValue).average());
        System.out.println("Moyenne ASF Write" + ASFWrite.stream().mapToInt(Integer::intValue).average());
        System.out.println("---");
        List<Integer> listCharge2 = nodeChords.stream().map(f -> f.getChargeReseaux()).toList();
        List<Integer> chordRead = nodeChords.stream().map(f -> f.getChargeReseauxRead()).toList();
        List<Integer> chordStore = nodeChords.stream().map(f -> f.getChargeReseauxStore()).toList();
        List<Integer> chordWrite = nodeChords.stream().map(f -> f.getChargeReseauxWrite()).toList();
        System.out.println("Moyenne chord" + listCharge2.stream().mapToInt(Integer::intValue).average());
        System.out.println("Moyenne chord Store" + chordStore.stream().mapToInt(Integer::intValue).average());
        System.out.println("Moyenne chord Read" + chordRead.stream().mapToInt(Integer::intValue).average());
        System.out.println("Moyenne chord Write" + chordWrite.stream().mapToInt(Integer::intValue).average());
       System.out.println(alreadtEncounter.stream().sorted().toList());
    }

    public static void simuleDemande(int nbrFile,int S,int F,int H, int seed,LinkedList<Pair<Integer,Integer>> simulation)
    {
        Random rand=new Random(seed);
        for (int i = 0; i < F / 10; i++) {
            simulationDemande simulationFichieri = new simulationDemande(nbrFile++, S, F, H, seed);
            for (int h = 0; h < H; h++) {
                simulation.addAll(simulationFichieri.getHFi(h));
                seed++;
            }
        }
        Collections.shuffle(simulation, rand);
    }
    private static List<Integer> simuExpo(List<AbstractCompo> hubs, int F, int S, int seed) {
        int H = hubs.size(); // H nombre de hubs , F nombre de file , S nombre de demande TOTAL ( ecriture/lecture/reecriture )
        //List<Pair<Integer,Integer>> simulation = new ArrayList<>();
        LinkedList<Pair<Integer, Integer>> simulation = new LinkedList<>();
        List<Integer> alreadyEncounter = new ArrayList<>();

        Random rand = new Random(seed);
        simuleDemande(0,S,F,H,seed,simulation);
        int nbrFile=F/10;
        int s = 0;
        int simulationSize = simulation.size();
        int time = 0;
        while (alreadyEncounter.size() < F) {
            // System.out.println("Taille de la simulation : "+simulation.size());
            //System.out.println("s "+s);
            //  System.out.println(s);
            if (s >= simulationSize * 0.1) {
                simuleDemande(nbrFile,S,F,H,seed,simulation);
                nbrFile+=F/10;
                simulationSize = simulation.size();
                time++;

                s = 0;
            }
            //if(s>=simulation.size()){break;} // can't control how long is simulation
            Pair<Integer, Integer> tmp = simulation.poll();
            if (tmp == null) {
                System.out.println("Simu fini prématurément :( ");
                break;
            }
            if (!alreadyEncounter.contains(tmp.first())) {

                alreadyEncounter.add(tmp.first());
                hubs.get(tmp.second()).store("fichier" + tmp.first(), 3);
            } else {
                double rand75 = rand.nextDouble();
                if (rand75 < 0.9) {
                    hubs.get(tmp.second()).read("fichier" + tmp.first(), 3);
                } else {
                    hubs.get(tmp.second()).write("fichier" + tmp.first(), "reecriture" + s, 3);
                }
            }
            s++;
        }
        //  System.out.println("S = "+s+" / "+S);
return alreadyEncounter;
    }
}