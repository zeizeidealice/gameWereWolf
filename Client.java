package chatting;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.io.ObjectInputStream;
import java.util.*;

public class Client
{
    private int port;
    private String host;
    private Socket socket;
    private int id;

    public Client(String host, int port)
    {
        this.id = 0;
        try
        {
            this.socket = new Socket(host, port);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        this.host = host;
        this.port = port;
    }

    public void send(Packets packets) throws UnknownHostException, IOException
    {
        ObjectOutputStream os = new ObjectOutputStream(socket.getOutputStream());
        os.writeObject(packets);
        socket.getOutputStream().flush();
    }

    public Packets handleSocket() throws IOException, ClassNotFoundException
    {
        ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
        Packets p = (Packets)ois.readObject();
        System.out.println("content:" + p.title);
        return p;
    }

    public static void main(String[] args)
    {
        Client c = new Client("127.0.0.1", 8080);
        Packets p = new Packets(c.id, "hello");
        try
        {
            // 发送链接请求，等待服务端确认
            c.send(p);
            c.handleSocket();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

        boolean wolf = false;
        int wolfnum = -1;
        boolean witch = false;
        boolean foresee = false;
        boolean guard = false;
        boolean hunter = false;

        try
        {
            int myid = c.handleSocket().id;
            if (myid == 2)
            {
                wolf = true;
                // 狼接受同伴列表
                wolfnum = c.handleSocket().id;
            }
            if (myid == 3)
            {
                witch = true;
            }
            if (myid == 4)
            {
                foresee = true;
            }
            if (myid == 5)
            {
                guard = true;
            }
            if (myid == 6)
            {
                hunter = true;
            }
            c.handleSocket();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

        Scanner s = new Scanner(System.in);
        if (wolf)
        {
            System.out.println("请决定：");
            try
            {
                boolean flag = false;
                while (!flag)
                {
                    if (c.handleSocket().id != -1)
                        break;
                    c.send(new Packets(s.nextInt(), "wolf decided"));
                    for (int i = 0; i < wolfnum - 1; ++i)
                    {
                        if (c.handleSocket().id != -1)
                            flag = true;
                    }
                }
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }
        try
        {
            c.handleSocket();
            if (foresee)
            {
                System.out.println("请决定：");
                c.send(new Packets(s.nextInt(), "foresee decided"));
                c.handleSocket();
            }
            c.handleSocket();
            if (guard)
            {
                System.out.println("请决定：");
                c.send(new Packets(s.nextInt(), "guard decided"));
            }
            if (c.handleSocket().id != -1 && witch)
            {
                System.out.println("请决定：");
                c.send(new Packets(s.nextInt(), "witch save decided"));
            }
            c.handleSocket();
            if (witch)
            {
                System.out.println("请决定：");
                c.send(new Packets(s.nextInt(), "witch kill decided"));
            }
            if (c.handleSocket().id == -1 && hunter)
            {
                System.out.println("请决定：");
                c.send(new Packets(s.nextInt(), "hunter decided"));
            }
            c.handleSocket();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

    }
}

