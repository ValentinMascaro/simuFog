public interface Nodes {
    public Message read(String filename, int replique);
    public Message store(Message msg, int replique);
    public Message write(Message msg, int replique);
}
