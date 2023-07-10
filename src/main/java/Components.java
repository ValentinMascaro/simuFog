public interface Components {
    //public void connectTo(Components components); // finalement sa sert à rien ¯\_(ツ)_/¯
    public boolean read(String filename,int replique);
    public boolean store(String filename,int replique);
    public boolean write(String filename,String newContenu,int replique);
}
