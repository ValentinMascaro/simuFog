

public class Time {
    private int temps;
    public Time()
    {
        temps=0;
    }
    public Time(int temps)
    {
        this.temps=temps;
    }
    public void increaseTemps()
    {
        temps++;
    }
    public int getTemps()
    {
        return temps;
    }
    public void setTemps(int temps)
    {
        this.temps=temps;
    }
}
