import java.util.List;

public interface cache {
    public List<Integer> get(String filename);
    public void put(String filename,List<Integer> position);
    public boolean containsKey(String filename);
    public void close();
    public void remove(String filename);
}
