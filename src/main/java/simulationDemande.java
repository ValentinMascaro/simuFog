import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class simulationDemande {
    private int id;
    private List<Integer> nombreDemandeParHubI;

    public simulationDemande(int id, int S,int F, int H,int seed) {
        this.id = id;
        Random rand=new Random(seed);
        this.nombreDemandeParHubI=new ArrayList<>();
        Double tmpPoidsHub = rand.nextExponential();
        for(int i=0;i<H;i++)
        {
            this.nombreDemandeParHubI.add((int)Math.floor(tmpPoidsHub*rand.nextExponential()*(S/(F*H))));
        } // nombreDemandeParHubI[i] = le nombre de fois que hubI demande le fichier id

    }
    public List<Pair<Integer,Integer>> getHFi(int indice)
    {
        List<Pair<Integer,Integer>> hfi = new ArrayList<>();
        for(int i =0;i<this.nombreDemandeParHubI.get(indice);i++)
        {
            hfi.add(new Pair<>(id,indice));
        }
        return hfi;
    }
}
