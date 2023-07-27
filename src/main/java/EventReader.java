import java.util.List;

public class EventReader {
    Timeline timeline;
    List<AbstractNode> hub;
    public EventReader(List<AbstractNode> hub)
    {
        this.timeline=new Timeline();
        this.hub=hub;
    }
    public void playNextEvent()
    {
        Event nextEvent = timeline.getNextEvent();
        List<Event> resultingEvent=null;
        Integer timeEventStarted = nextEvent.time;
        switch (nextEvent.function) {
            case "read" -> resultingEvent=hub.get(nextEvent.message.destinataire).read(nextEvent.message.nomFichier, nextEvent.message.replique); // TODO msg avec replique
            case "readTo" ->resultingEvent=hub.get(nextEvent.message.destinataire).readTo(nextEvent.message);

            case "store" -> resultingEvent=hub.get(nextEvent.message.destinataire).store(nextEvent.message, nextEvent.message.replique);
            case "storeTo" ->resultingEvent=hub.get(nextEvent.message.destinataire).storeTo(nextEvent.message);

            case "write" -> resultingEvent=hub.get(nextEvent.message.destinataire).write(nextEvent.message, nextEvent.message.replique);
            case "writeTo" ->resultingEvent=hub.get(nextEvent.message.destinataire).writeTo(nextEvent.message);

            case "increaseTo" ->resultingEvent=hub.get(nextEvent.message.destinataire).increaseTo(nextEvent.message);

            case "removeTo" ->resultingEvent=hub.get(nextEvent.message.destinataire).removeTo(nextEvent.message);

            case "returnReadTo" ->resultingEvent=hub.get(nextEvent.message.destinataire).returnReadTo(nextEvent.message);
            case "returnWriteTo" ->resultingEvent=hub.get(nextEvent.message.destinataire).returnWriteTo(nextEvent.message);
            case "returnStoreTo" ->resultingEvent=hub.get(nextEvent.message.destinataire).returnStoreTo(nextEvent.message);
            case "returnRemoveTo" ->resultingEvent=hub.get(nextEvent.message.destinataire).returnRemoveTo(nextEvent.message);



        }
        Integer timeResultingEvent = resultingEvent.time+timeEventStarted;
        this.timeline.add(new Event(timeResultingEvent,resultingEvent.message, resultingEvent.function));
    }
    public void addEvent(Event event)
    {
        this.timeline.add(event);
    }
}
