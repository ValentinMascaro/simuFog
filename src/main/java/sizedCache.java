import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/* jcache ne prevoit pas de taille limite pour le cache, et la durée minimal est d'une minute. bref, inutilisable pour cette simulation
alors on fait tout al mano ; _ ;
 */
public class sizedCache implements cache{
    private HashMap<String, List<Integer>> positionHub;
    //private HashMap<String, Integer> positionCache;
    private LinkedList<String> cache;
    private Integer sizeLimit;
    private Integer size;
    private boolean isFull;
    private int id;
    public sizedCache(Integer sizeLimit,int id)
    {
        this.id=id;
        this.positionHub=new HashMap<>();
      //  this.positionCache=new HashMap<>();
        this.sizeLimit=sizeLimit;
        this.size=0;
        this.cache=new LinkedList<>();
        this.isFull=false;
    }

    public List<Integer> get(String filename)
    {

        if(positionHub.containsKey(filename))
        {
            cache.remove(filename);
            cache.addFirst(filename);

            return positionHub.get(filename);
        }
        else
        {
            System.out.println("this should be an exception bro");
            return null; // comme on fait un containsKey avant cette situation ne doit pas arriver
        }
    }
    public void put(String filename,List<Integer> position)
    {
        /*System.out.println("Hub "+this.id+" put "+filename+" at "+position+" size : "+size+" / "+sizeLimit);
        for(String key : positionHub.keySet())
        {
            System.out.println("    "+key+" "+positionHub.get(key));
        }*/
        if(cache.isEmpty())
        {
            size++;
            cache.add(filename);
        //    this.positionCache.put(filename,0);
            this.positionHub.put(filename,position);

            return;
        }
        if(positionHub.containsKey(filename))
        {
            cache.remove(filename);
            cache.addFirst(filename);
            positionHub.put(filename,position);
            return;
        }

        cache.addFirst(filename);
        positionHub.put(filename,position);
        if(isFull){
            String last=cache.getLast();
            cache.remove(last);
            positionHub.remove(last);
            return;
        }
        size++;
        if(size.equals(sizeLimit))
        {
            isFull=true;
        }
    }
    public boolean containsKey(String filename)
    {
        //cache.remove(filename);
        //cache.addFirst(filename); le code fait systématiquement un containskey suviie d'un get, pour éviter de refresh 2 fois, on ne refresh pas mnt
        return this.positionHub.containsKey(filename);
    }
    public void close()
    {
        this.cache=null;
        this.positionHub=null;
        System.out.println("cache close i guess");
    }
    public void remove(String filename)
    {
        this.size--;
        if(size<sizeLimit)
        {
            isFull=false;
        }
        this.cache.remove(filename);
        this.positionHub.remove(filename);
    }
}
