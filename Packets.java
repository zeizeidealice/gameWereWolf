package chatting;

import java.io.Serializable;

public class Packets implements Serializable
{    
    public int id;
    public String title;

    public Packets(int id, String title)
    {
        this.id = id;
        this.title = title;
    }    
}