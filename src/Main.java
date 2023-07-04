import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) {
        List<Hub> Hubs = new ArrayList<>();
        for (int i = 0; i < 16; i++) {
            Hubs.add(new Hub(i, 1));
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
        /*
        System.out.println(Hubs.get(8).getVoisinsNode().stream().map(f->f.getId()).toList());
        System.out.println("--");
        Hubs.get(1).store("fichier1",3);

        Hubs.get(14).read("fichier1",3);
        Hubs.get(5).read("fichier1",3);
        System.out.println(Hubs.get(5).getFichierDemande());

        System.out.println("--");
        Hubs.get(3).store("fichier2",3);
        System.out.println(Hubs.get(5).getFichierDemande());
        System.out.println("--");
        Hubs.get(10).store("fichier3",3);
        System.out.println(Hubs.get(5).getFichierDemande());
        System.out.println("--");
        Hubs.get(14).store("fichier4",3);
        System.out.println(Hubs.get(5).getFichierDemande());
        System.out.println("--");
        Hubs.get(15).store("fichier5",3);
        System.out.println(Hubs.get(5).getFichierDemande());
        System.out.println("--");
        Hubs.get(8).store("fichier6",3);
        System.out.println(Hubs.get(5).getFichierDemande());
        System.out.println("---------");

        System.out.println("--");
        Hubs.get(3).read("fichier2",3);
        System.out.println("--");
        Hubs.get(4).read("fichier3",3);
        System.out.println("--");
        Hubs.get(7).read("fichier4",3);
        System.out.println("--");
        Hubs.get(10).read("fichier5",3);
        System.out.println("--");
        Hubs.get(0).read("fichier6",3);
        System.out.println("Fichier --");
        Hubs.get(0).read("fichier7",3);

        System.out.println(Hubs.get(5).getFichierDemande());
*/
        Random rand=new Random(1);
        List<Integer> alreadyHere = new ArrayList<>();
        int seed = 10;
        List<Integer> probaFichier  = generateRandomListWithMean(100,0,100,5,seed);
        List<Integer> probaTirageHubs = generateRandomListWithMean(15,0,15,4,seed);
        for(int i=0;i<10000;i++)
        {
            int h=rand.nextInt(0,probaTirageHubs.size());
            int f = rand.nextInt(0,probaFichier.size());
            if(alreadyHere.contains(probaFichier.get(f)))
            {
                if(rand.nextBoolean())
                {
                    Hubs.get(probaTirageHubs.get(h)).read("fichier"+probaFichier.get(f),3);
                }
                else {
                    Hubs.get(probaTirageHubs.get(h)).write("fichier"+probaFichier.get(f),"Un nouveau contenu "+i,3);
                }
            }
            else
            {
                Hubs.get(probaTirageHubs.get(h)).store("fichier"+probaFichier.get(f),3);
                alreadyHere.add(probaFichier.get(f));
            }
        }
        System.out.println(Hubs.stream().map(f->"Hubs "+f.getId()+" : "+f.getNbrFichier()+" / "+f.getNbrFichierMax()+" "+f.getFichierDemande()+"\n").toList());
        System.out.println(probaTirageHubs);
        System.out.println(probaFichier);
    }
    public static List<Integer> generateRandomListWithMean(int size, int min, int max, int mean,int seed) {
        List<Integer> resultList = new ArrayList<>();
        Random random = new Random(seed);

        int sum = mean * size;
        int remainingSum = sum;

        // Generate random numbers and adjust them to get the desired mean
        for (int i = 0; i < size - 1; i++) {
            int randomNumber = random.nextInt(max - min + 1) + min;
            int adjustedNumber = Math.min(randomNumber, remainingSum); // Avoid exceeding the mean
            resultList.add(adjustedNumber);
            remainingSum -= adjustedNumber;
        }

        // The last number is calculated to make sure the mean is exactly as desired
        resultList.add(sum - resultList.stream().mapToInt(Integer::intValue).sum());

        // Shuffle the list to make it less ordered
      resultList=resultList.stream().sorted().collect(Collectors.toList());

        return resultList;
    }
}