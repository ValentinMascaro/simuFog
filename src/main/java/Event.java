public class Event {
    public Integer time;
    public Message message;
    public String function;

    public Event(Integer time, Message message, String function) {
        this.time = time;
        this.message = message;
        this.function = function;
    }
}
