import java.util.HashMap;
import java.util.List;

/* jcache ne prevoit pas de taille limite pour le cache, et la durée minimal est d'une minute. bref, inutilisable pour cette simulation
alors on fait tout al mano ; _ ;
 */
public class cache {
    private HashMap<String, List<Integer>> position;
    private HashMap<String, Time> expireFile;

    private Time actualTime;
    private Integer limit;
    public cache(Integer limit, Time time)
    {
        this.position=new HashMap<>();
        this.expireFile=new HashMap<>();
        actualTime=new Time();
        this.limit=limit;
        this.actualTime=time;
    }

    public List<Integer> get(String filename)
    {
        if(position.containsKey(filename))
        {
            if(expireFile.containsKey(filename))
            {
                if(this.actualTime.getTemps()-expireFile.get(filename).getTemps()<this.limit)
                {
                    actualTime.increaseTemps();
                    this.expireFile.get(filename).setTemps(actualTime.getTemps());
                    return position.get(filename);
                }
                else
                {
                    position.remove(filename);
                    expireFile.remove(filename);
                    return null;
                }
            }
        }
        return null;
    }
    public void put(String filename,List<Integer> position)
    {
        this.actualTime.increaseTemps();
        this.position.put(filename,position);
        this.expireFile.put(filename,new Time(actualTime.getTemps()));
    }
    public boolean containsKey(String filename)
    {
        if(expireFile.containsKey(filename))
        {
            if(this.actualTime.getTemps()-expireFile.get(filename).getTemps()<this.limit)
            {
                return true;
            }
            else{
                position.remove(filename);
                expireFile.remove(filename);
                return false;
            }
        }
        return false;
    }
    public void close()
    {
        System.out.println("cache close i guess");
    }
    public void remove(String filename)
    {
        this.expireFile.remove(filename);
        this.position.remove(filename);
    }
}
