import java.util.LinkedList;

public class Timeline {
    LinkedList<Event> timeline;
    public Timeline()
    {
        this.timeline=new LinkedList<>();
    }
    public void add(Event event) // on suppose que les events proposé ont forcément un temps supérieur au plus petit temps de la timeline, on créé pas des events dans le passé
    {
        Integer eventTime =  event.time;
        int i =0;
        while(timeline.size()>i && timeline.get(i).time<eventTime)
        {
            i++;
        }
        if(timeline.size()==i)
        {
            this.timeline.add(event);
        }
        else {
            this.timeline.add(i, event);
        }
    }
    public Event getNextEvent()
    {
        return this.timeline.pop();
    }


}
