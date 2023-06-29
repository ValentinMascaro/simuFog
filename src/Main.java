import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        List<Hub> Hubs = new ArrayList<>();
        for (int i = 0; i < 16; i++) {
            Hubs.add(new Hub(i, 2));
        }
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
                List.of(13)//hub 14
        ));
        for(int i=0;i<topology.size();i++)
        {
            Hubs.get(i).setVoisinsHub(topology.get(i).stream().map(f->Hubs.get(f)).toList());
            Hubs.get(i).setReseauDeVoisins(topology);
            Hubs.get(i).TopologyMoyenne();
        }
        System.out.println("fichier1");
        Hubs.get(0).store("fichier1",3);
        System.out.println("fichier2");
        Hubs.get(0).store("fichier2",3);
        System.out.println("fichier3");
        Hubs.get(0).store("fichier3",3);
    }
}