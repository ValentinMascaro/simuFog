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




        int nbrFichierMaxDepart=2;
        int F = 10;
        int S = 100000;
        int C = 4;
        int R = 6;
        int limitCache=10;


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
        List<List<Integer>> _topology = new ArrayList<>(List.of(
                List.of(1), // hub 0
                List.of(0,2), // hub 1
                List.of(1,3), // hub 2
                List.of(2) //hub 3

        ));
        for(;F<70;F+=10) {
            int seed = 1;
            double ASFread=0;
            double ASFstore=0;
            double ASFreStore=0;
            double ASFwrite=0;
            double ASFglob=0;
            double ASFincrease=0;

            double Chordread=0;
            double Chordstore=0;
            double Chordwrited=0;
            double Chordglob=0;
            int test=0;
            double preAvg=Double.MAX_VALUE;
            double avg=0.0;
            double epsilon = S/10.0;
            do {

                List<AbstractNode> Hubs = new ArrayList<>();
                //Time time=new Time();
                for (int i = 0; i < topology.size(); i++) {
                    Hubs.add(new Hub(i, nbrFichierMaxDepart, limitCache));
                }
                for (int i = 0; i < topology.size(); i++) {
                    Hubs.get(i).setVoisins(topology.get(i).stream().map(f -> Hubs.get(f)).toList());
                    Hubs.get(i).setTopology(topology);
                    Hubs.get(i).TopologyMoyenne();
                }

                List<AbstractCompo> nodeChords = new ArrayList<>();
                for (int i = 0; i < 16; i++) {
                    nodeChords.add(new NodeChord(i, topology, 160));
                }
                for (int i = 0; i < topology.size(); i++) {
                    nodeChords.get(i).setVoisins(topology.get(i).stream().map(f -> nodeChords.get(f)).toList());
                    nodeChords.get(i).setTopology();
                }
                simuExpo(nodeChords, F, S, seed, C, R);
                simuExpoNew(Hubs, F, S, seed, C, R);

                List<Integer> ASFINcrease = Hubs.stream().map(f -> f.getChargeReseauxIncrease()).toList();
                List<Integer> ASFRead = Hubs.stream().map(f -> f.getChargeReseauxRead()).toList();
                List<Integer> ASFStore = Hubs.stream().map(f -> f.getChargeReseauxStore()).toList();
                List<Integer> ASFReStore = Hubs.stream().map(f -> f.getChargeReseauxReStore()).toList();
                List<Integer> ASFWrite = Hubs.stream().map(f -> f.getChargeReseauxWrite()).toList();
                List<Integer> ASFGlobal = Hubs.stream().map(f -> f.getChargeReseaux()).toList();
                ASFglob += ASFGlobal.stream().mapToInt(Integer::intValue).sum();
                ASFstore += ASFStore.stream().mapToInt(Integer::intValue).sum();
                ASFread += ASFRead.stream().mapToInt(Integer::intValue).sum();
                ASFreStore += ASFReStore.stream().mapToInt(Integer::intValue).sum();
                ASFwrite += ASFWrite.stream().mapToInt(Integer::intValue).sum();
                ASFincrease += ASFINcrease.stream().mapToInt(Integer::intValue).sum();

                //   System.out.println("test : "+test);
                List<Integer> listCharge2 = nodeChords.stream().map(f -> f.getChargeReseaux()).toList();
                List<Integer> chordRead = nodeChords.stream().map(f -> f.getChargeReseauxRead()).toList();
                List<Integer> chordStore = nodeChords.stream().map(f -> f.getChargeReseauxStore()).toList();
                List<Integer> chordWrite = nodeChords.stream().map(f -> f.getChargeReseauxWrite()).toList();
                Chordglob += listCharge2.stream().mapToInt(Integer::intValue).sum();
                Chordstore += chordStore.stream().mapToInt(Integer::intValue).sum();
                Chordread += chordRead.stream().mapToInt(Integer::intValue).sum();
                Chordwrited += chordWrite.stream().mapToInt(Integer::intValue).sum();
                seed++;

                //System.out.println(Hubs.stream().map(f -> "Hubs " + f.getId() + " : " + f.getNbrFichier() + " / " + f.getNbrFichierMax() + " " + f.getFichierDemande() + "\n").toList());
                //System.out.println("--");
                for (AbstractNode hubAclose : Hubs) {
                    hubAclose.closeCache();
                }
                preAvg = avg;
                avg = ASFglob / ++test;
                if (test % 100 == 0) {
                    System.out.println("avg : " + avg + " preAvg " + preAvg);
                }
            } while (Math.abs(preAvg - avg) > epsilon || test < 100);
            System.out.println("Contexte");
            System.out.println("    Nbr file " + F);
            System.out.println("    Nbr demande " + S);
            System.out.println("    Limite cache " + limitCache);
            System.out.println("    Nbr replique " + R);
            System.out.println("    Chorum " + C);

            System.out.println("Resultat ");
            System.out.println("    ASF");
            System.out.println("        ASF global : " + ASFglob / test);
            System.out.println("        ASF Read : " + ASFread / test);
            System.out.println("        ASF Write : " + ASFwrite / test);
            System.out.println("        ASF Store : " + ASFstore / test + " Restore : " + ASFreStore / test + " Increase : " + ASFincrease / test);
            System.out.println("    Chord");
            ;
            System.out.println("        Chord global : " + Chordglob / test);
            System.out.println("        Chord Read : " + Chordread / test);
            System.out.println("        Chord Write : " + Chordwrited / test);
            System.out.println("        Chord Store : " + Chordstore / test);
        }
    }

    private static List<Integer>  simuExpoNew(List<AbstractNode> hubs, int F, int S, int seed, int C, int R) {
        int H = hubs.size(); // H nombre de hubs , F nombre de file , S nombre de demande TOTAL ( ecriture/lecture/reecriture )
        //List<Pair<Integer,Integer>> simulation = new ArrayList<>();
        LinkedList<Pair<Integer, Integer>> simulation = new LinkedList<>();
        List<Integer> alreadyEncounter = new ArrayList<>();

        Random rand = new Random(seed);
        simuleDemande(0,S,F,H,rand,simulation);
        int nbrFile=F/10;
        int s = 0;
        int simulationSize = simulation.size();
        int time = 0;
        while (alreadyEncounter.size() < F) {
            // System.out.println("Taille de la simulation : "+simulation.size());
            //System.out.println("s "+s);
            //  System.out.println(s);
            if (s >= simulationSize * 0.1) {
                simuleDemande(nbrFile,S,F,H,rand,simulation);
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
                hubs.get(tmp.second()).store(new Message("fichier" + tmp.first(),R));
            } else {
                double rand50 = rand.nextDouble();
                if (rand50<0.5) {
                    hubs.get(tmp.second()).read(new Message("fichier" + tmp.first(), C));
                } else {
                    hubs.get(tmp.second()).write(new Message("fichier" + tmp.first(),"reecriture"+s, C));
                }
            }
            s++;
        }
        //  System.out.println("S = "+s+" / "+S);
        return alreadyEncounter;
    }

    public static void simuleDemande(int nbrFile,int S,int F,int H, Random rand,LinkedList<Pair<Integer,Integer>> simulation)
    {
        //Random rand=new Random(seed);
        for (int i = 0; i < F / 10; i++) {
            simulationDemande simulationFichieri = new simulationDemande(nbrFile++, S, F, H, rand);
            for (int h = 0; h < H; h++) {
                simulation.addAll(simulationFichieri.getHFi(h));

            }
        }
        Collections.shuffle(simulation, rand);
    }
    private static List<Integer> simuExpo(List<AbstractCompo> hubs, int F, int S, int seed, int C, int R) {
        int H = hubs.size(); // H nombre de hubs , F nombre de file , S nombre de demande TOTAL ( ecriture/lecture/reecriture )
        //List<Pair<Integer,Integer>> simulation = new ArrayList<>();
        LinkedList<Pair<Integer, Integer>> simulation = new LinkedList<>();
        List<Integer> alreadyEncounter = new ArrayList<>();

        Random rand = new Random(seed);
        simuleDemande(0,S,F,H,rand,simulation);
        int nbrFile=F/10;
        int s = 0;
        int simulationSize = simulation.size();
        int time = 0;
        while (alreadyEncounter.size() < F) {
            // System.out.println("Taille de la simulation : "+simulation.size());
            //System.out.println("s "+s);
            //  System.out.println(s);
            if (s >= simulationSize * 0.1) {
                simuleDemande(nbrFile,S,F,H,rand,simulation);
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
                hubs.get(tmp.second()).store("fichier" + tmp.first(), R);
            } else {
                double rand50 = rand.nextDouble();
                if (rand50<0.5) {
                    hubs.get(tmp.second()).read("fichier" + tmp.first(), C);
                } else {
                    hubs.get(tmp.second()).write("fichier" + tmp.first(), "reecriture" + s, C);
                }
            }
            s++;
        }
        //  System.out.println("S = "+s+" / "+S);
return alreadyEncounter;
    }
}