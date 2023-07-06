import java.util.*;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) {
        List<Hub> Hubs = new ArrayList<>();
        for (int i = 0; i < 16; i++) {
            Hubs.add(new Hub(i, 2));
        }
        List<Node> Nodes = new ArrayList<>();
        for(int j=0;j<200;j++)
        {
            Nodes.add(new Node(j));
        }
        Collections.shuffle(Nodes,new Random(1));
        List<List<Integer>> topology = new ArrayList<>(List.of(
                List.of(1, 4), // hub 0
                List.of(0, 4, 3), // hub 1
                List.of(7,8), // hub 2
                List.of(1,5,7), // hub 3
                List.of(0,1,5),//hub 4
                List.of(4,3,7,6),//hub 5
                List.of(5,8,10),//hub 6
                List.of(3,2,8,5,6),//hub 7
                List.of(2,7,6,9,10),//hub 8
                List.of(12,8),//hub 9
                List.of(8,6,11),//hub 10
                List.of(12,10,13),//hub 11
                List.of(9,11),//hub 12
                List.of(14,11),//hub 13
                List.of(13,15),//hub 14
                List.of(14) // hub 15
        ));

        for(int i=0;i<topology.size();i++)
        {
            Hubs.get(i).setVoisinsHub(topology.get(i).stream().map(f->Hubs.get(f)).toList());
            Hubs.get(i).setReseauDeVoisins(topology);
            Hubs.get(i).TopologyMoyenne();

            for(int n=0;n<6;n++)
            {
                Hubs.get(i).connectTo(Nodes.get(n));
                Nodes.get(n).connectTo(Hubs.get(i));
                Nodes.remove(n);
            }

        }
        int seed = 10;
        simuExpo(Hubs,100,10000,seed);
        System.out.println(Hubs.stream().map(f->"Hubs "+f.getId()+" : "+f.getNbrFichier()+" / "+f.getNbrFichierMax()+" "+f.getFichierDemande()+"\n").toList());
        System.out.println(Hubs.stream().map(f->"Hubs "+f.getId()+" charge reseau : "+f.getChargeReseaux()+" \n").toList());
        List<Integer> listCharge = Hubs.stream().map(f->f.getChargeReseaux()).toList();
        System.out.println(listCharge.stream().mapToInt(Integer::intValue).average());
        System.out.println(listCharge.stream().sorted().toList());
    }

    private static void simuExpo(List<Hub> hubs,int F, int S, int seed) {
        int H = hubs.size(); // H nombre de hubs , F nombre de file , S nombre de demande TOTAL ( ecriture/lecture/reecriture )
        List<Pair<Integer,Integer>> simulation = new ArrayList<>();
        List<Integer> alreadyEncounter = new ArrayList<>();
        Random rand = new Random(seed);
        int nbrFile=0;
        for (int i = 0; i < F/10; i++) {
            for (int h = 0; h < H; h++) {
                simulationDemande simulationFichieri = new simulationDemande(nbrFile+i, S, F, H, seed);
                simulation.addAll(simulationFichieri.getHFi(h));
                seed++;

            }
            nbrFile++;
        }
        int s=0;
        int time=0;
        while(time<9)
        {
          //  System.out.println(s);
            if(s%(S/10)==0) {
                for (int i = 0; i < F/10; i++) {
                    for (int h = 0; h < H; h++) {
                        simulationDemande simulationFichieri = new simulationDemande(nbrFile+i, S, F, H, seed);
                        simulation.addAll(simulationFichieri.getHFi(h));
                        seed++;
                    }
                    nbrFile++;
                }
                time++;
                Collections.shuffle(simulation,rand);
            }
            if(s>=simulation.size()){break;} // can't control how long is simulation
            if(!alreadyEncounter.contains(simulation.get(s).first()))
            {
                alreadyEncounter.add(simulation.get(s).first());
                hubs.get(simulation.get(s).second()).store("fichier"+simulation.get(s).first(),3);
            }
            else
            {
                double rand75= rand.nextDouble();
                if(rand75<0.9)
                {
                    hubs.get(simulation.get(s).second()).read("fichier"+simulation.get(s).first(),3);
                }else {
                    hubs.get(simulation.get(s).second()).write("fichier"+simulation.get(s).first(),"reecriture"+s,3);
                }
            }
        s++;
        }
    }
}