import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.spi.CachingProvider;
import java.util.List;

public class AbstractCompo implements Components{
    CachingProvider cachingProvider;
    CacheManager cacheManager;
    MutableConfiguration<String, List<Integer>> config;
    Cache<String, List<Integer>> cache;

    public void closeCache()
    {
        this.cache.close();
    }
    public int getId() {
        return id;
    }
    private int chargeReseauxReStore;

    public int getChargeReseauxIncrease() {
        return chargeReseauxIncrease;
    }
    public int getChargeReseauxReStore() {
        return chargeReseauxReStore;
    }
    private int id;

    private int chargeReseaux;
    private int chargeReseauxRead;
    private int chargeReseauxWrite;
    private int chargeReseauxStore;

    private int chargeReseauxIncrease;

    public int getChargeReseauxRead() {
        return chargeReseauxRead;
    }

    public int getChargeReseauxWrite() {
        return chargeReseauxWrite;
    }

    public int getChargeReseauxStore() {
        return chargeReseauxStore;
    }

    public int getChargeReseaux() {
        return chargeReseaux;
    }
    @Override
    public boolean read(String filename, int replique) {
        return false;
    }
    public boolean readTo(String filename,int hubId){return false;}
    public boolean give(String filename)
    {
        return false;
    }
    public void connectTo(Components components) {}
    public void TopologyMoyenne(){;}
    public int getNbrFichierMax() {
        return 1;
    }
    public int getNbrFichier() {
        return 1;
    }
    public String getFichierDemande(){return "bla";}
    @Override
    public boolean store(String filename, int replique) {
        return false;
    }
    public boolean reStoreTo(String filename, int hubId) {
        System.out.println("restoreToAb");return false;}
    public boolean storeTo(String filename,int hubId){System.out.println("storeToAb");return false;}
    public boolean storeTo(String filename,int hubId,int poids){System.out.println("storeTo");return false;}
    public boolean take(String filename){
        System.out.println("takeAb");return false;}

    public boolean take(String filename, int poids){
        System.out.println("takeAb2");return false;}
    @Override
    public boolean write(String filename, String newContenu, int replique) {
        System.out.println("write");
        return false;
    }
    public boolean writeIn(String filename, String contenu){System.out.println("writeIn");return false;}

    public boolean writeTo(String filename,String contenu,int hubId){
        System.out.println("aled");return false;}
    public void setVoisins( List<AbstractCompo> voisins)
    {

    }
    public void increaseTo(int increase,List<AbstractCompo> hubAlreadyWarned){System.out.println("incr");} // broadcast and prune
    public void setReseauDeVoisins(List<List<Integer>> reseauDeVoisins) {

    }
    public void setTopology(){}
    public  String getFichier(){
        return "bla";
    }
}