package chatting;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server
{
    private int port;
    private int sequence;
    private boolean stop;
    private ServerSocket serverSocket;
    private int clientCount;
    private int maxCount;
    List<Socket> clients;

    class ServerThread extends Thread
    {
        Socket socket = null;
        int id = 0;
        public ServerThread(Socket socket, int id)
        {
            this.socket = socket;
            this.id = id;
            System.out.println(this.id);
        }
        public void run()
        {
            try
            {
                Server.this.handleSocket(socket);
                Packets p = new Packets(id, "Connected,you are player " + id);
                Server.this.send(p, socket);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    /*pass addresses of clients from outside*/
    public Server(int port, int maxCount)
    {
        this.port = port;
        this.stop = false;
        this.maxCount = maxCount;
        this.clients = new ArrayList<Socket>();
        this.clientCount = 0;
        try
        {
            serverSocket = new ServerSocket(port);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    public void run()
    {
        System.out.println("Sever running");
        Socket socket = null;
        while (clientCount < maxCount)
        {
            try
            {
                socket = serverSocket.accept();
                clients.add(socket);
                clientCount++;
                ServerThread st = new ServerThread(socket, clientCount);
                st.start();
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    private Packets handleSocket(Socket socket) throws IOException, ClassNotFoundException
    {
        ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
        Packets p = (Packets)ois.readObject();
        System.out.println("id : " + p.id);
        System.out.println("val: " + p.title);
        return p;
    }

    public void send(Packets packets, Socket socket) throws UnknownHostException, IOException
    {
        ObjectOutputStream os = new ObjectOutputStream(socket.getOutputStream());
        os.writeObject(packets);
        socket.getOutputStream().flush();
    }

    private void broadcast(Packets packets) throws UnknownHostException, IOException
    {
        for(int i = 0; i < clients.size(); i++)
        {
            send(packets, clients.get(i));
        }
    }

    /*function used to fail this server*/
    public void fail()
    {
        this.stop = true;
    }

    /*function used to restart this server*/
    public void unfail()
    {
        this.stop = false;
    }

    public static void main(String[] args)
    {
        // 最大玩家数量，测试的时候是6，每个身份各一个人，具体游戏的时候可以再改
        int maxCount = 6;
        String[] chineseIdentity = new String[] {" ", "平民", "狼人", "女巫", "预言家", "守卫", "猎人"};
        // 所有人的存活状况
        int[] status = new int[maxCount];
        // 用1~6表示的身份数组，identity[i]表示第i位玩家的身份
        List<Integer> identity = new ArrayList<Integer>();
        identity.add(2);
        identity.add(2);
        identity.add(1);
        identity.add(1);
        identity.add(4);
        identity.add(5);

        // 有身份的人的身份信息，wolf[i]表示第i个狼的编号（从0开始，即0号玩家是player1）；
        // 其他也一样，表示该身份的玩家的编号
        // ，如果没有该身份的玩家，则为-1
        List<Integer> wolf = new ArrayList<Integer>();
        int witch = -1, foresee = -1, guard = -1, hunter = -1;

        Server s = new Server(8080, maxCount);
        s.run();

        try
        {
            Thread.sleep(1000);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        // 游戏开始
        System.out.println("game starts now");
        System.out.println(" ");

        // 随机分配身份并统计身份信息
        Collections.shuffle(identity);
        for (int i = 0 ; i < maxCount; ++i)
        {
            if (identity.get(i) == 2)
                wolf.add(i);
            if (identity.get(i) == 3)
                witch = i;
            if (identity.get(i) == 4)
                foresee = i;
            if (identity.get(i) == 5)
                guard = i;
            if (identity.get(i) == 6)
                hunter = i;
        }

        for (int i = 0; i < maxCount; ++i)
        {
            try
            {
                s.send(new Packets(identity.get(i), "欢迎来到游戏, 您的身份是：" + chineseIdentity[identity.get(i)]), s.clients.get(i));
                if (identity.get(i) == 2) // 给狼人发送同伴信息
                    s.send(new Packets(wolf.size(), "您的同伴有：" + wolf.toString()), s.clients.get(i));
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }
        try
        {
            s.broadcast(new Packets(0, "天黑请闭眼，狼人请决定要杀的人"));
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

        int kill = -1;
        while (kill == -1)
        {
            try
            {
                for (int i = 0; i < wolf.size(); ++i)
                    s.send(new Packets(-1, "wolf deciding"), s.clients.get(wolf.get(i)));
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
            // 循环接受每一个狼的决定和理由，并将决定和理由告诉其他狼
            for (int i = 0; i < wolf.size(); ++i)
            {
                try
                {
                    Packets temp = s.handleSocket(s.clients.get(wolf.get(i)));
                    if (i == 0)
                        kill = temp.id;
                    if (kill != temp.id && kill != -1)
                        kill = -1;
                    for (int j = 0; j < wolf.size(); ++j)
                    {
                        if (j != i)
                            s.send(new Packets(-1, "wolf " + i + " want to kill " + temp.id), s.clients.get(wolf.get(j)));
                    }
                }
                catch(Exception e)
                {
                    e.printStackTrace();
                }
            }
        }
        try
        {
            for (int i = 0; i < wolf.size(); ++i)
                s.send(new Packets(0, "wolf decided on " + kill), s.clients.get(wolf.get(i)));
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        // 如果玩家选择不做事情（不验身份/不救人/不毒死人/不守卫/不开枪）则发回的id为-1
        int save = -1, poison = -1, test = -1, guarded = -1, shot = -1;
        try
        {
            if (foresee != -1)
            {
                s.broadcast(new Packets(0, "预言家，你要验证一名玩家的身份么？"));
                test = s.handleSocket(s.clients.get(foresee)).id;
                if (test == -1)
                    s.send(new Packets(0, "未选择玩家"), s.clients.get(foresee));
                else if (identity.get(test - 1) == 2)
                    s.send(new Packets(0, "他是坏人"), s.clients.get(foresee));
                else
                    s.send(new Packets(0, "他是好人"), s.clients.get(foresee));
            }
            else
                s.broadcast(new Packets(0, "无预言家"));
            if (guard != -1)
            {
                s.broadcast(new Packets(0, "守卫，你要保护一名玩家么？"));
                guarded = s.handleSocket(s.clients.get(guard)).id;
                if (guarded == kill)
                    kill = -1;
            }
            else
                s.broadcast(new Packets(0, "无守卫"));
            if (witch != -1)
            {
                if (kill != -1)
                {
                    s.broadcast(new Packets(0, "女巫，有人被杀了，你要救他么？"));
                    save = s.handleSocket(s.clients.get(witch)).id;
                    if (save != -1)
                        kill = -1;
                }
                else
                {
                    s.broadcast(new Packets(-1, "女巫，今夜无事发生"));
                }
                s.broadcast(new Packets(0, "女巫，你有一瓶毒药，你要杀死一名玩家么？"));
                poison = s.handleSocket(s.clients.get(witch)).id;
                if (guarded == poison)
                    poison = -1;
            }
            else
            {
                s.broadcast(new Packets(0, "无女巫可救人"));
                s.broadcast(new Packets(0, "无女巫可杀人"));
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        try
        {
            if (hunter != -1)
            {
                if (kill - 1 == hunter)
                {
                    s.broadcast(new Packets(-1, "猎人，夜里你被杀了，你要开枪么？"));
                    shot = s.handleSocket(s.clients.get(hunter)).id;
                }
                else
                    s.broadcast(new Packets(0, "猎人，今夜无事发生。"));
            }
            else
                s.broadcast(new Packets(0, "无猎人"));
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        if (kill != -1)
            status[kill - 1] = 1;
        if (poison != -1)
            status[poison - 1] = 1;
        if (shot != -1)
            status[shot - 1] = 1;
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < maxCount; ++i)
            sb.append(status[i]);
        try
        {
            s.broadcast(new Packets(0, "天亮了，现在的存活状况是：" + sb.toString()));
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
}