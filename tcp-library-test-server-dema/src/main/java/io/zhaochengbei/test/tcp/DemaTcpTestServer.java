package io.zhaochengbei.test.tcp;

import io.dema.tcp.*;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @Description: dema tcp 服务器
 * @Author: chengbei.zhao
 * @Date: 2021/7/27 10:48
 **/
public class DemaTcpTestServer {
    /**
     *
     */
    static private TcpServer tcpServer = new TcpServer();

    /**
     * 自服务器启动，处理包的总数。
     * 处理包的方式为：将收到的数据原样返回。
     */
    static private int processPacketTotalCount = 0;

    /**
     *
     */
    static private IoHandler ioHandler = new IoHandler() {

        @Override
        public void onAccept(TcpConnection connection){
            System.out.println("onAccept,"+connection.socket);
        }
        @Override
        public void onRead(TcpConnection connection){

            while(true){
//                System.out.println("s_onRead,"+connection.socket);
                ByteBuffer data = null;
                try {
                    data = ConnectionUtils.readPacket(connection, 0,4, 4,256);
                } catch (TcpException e) {
                    e.printStackTrace();
                }
                if(data == null){
                    break;
                }
                data.flip();
                connection.writeAndFlush(data);
                synchronized (this){
                    processPacketTotalCount++;
                }
            }
        }
        @Override
        public void onReadIdle(TcpConnection connection) {
            System.out.println("onReadIdle,reason="+connection.closeReason+","+connection.socket);
            connection.close(TcpConnectionCloseReason.ReadIdleTimeOut);
        }
        @Override
        public void onClose(TcpConnection connection,String reason){
            System.out.println("onClose,reason="+connection.closeReason+","+connection.socket);
        }
    };

    /**
     * 只负责一个任务。
     */
    static private Timer timer = new Timer();

    public static void main( String[] args )
    {
        try {
            Long time = System.currentTimeMillis();
            //100 000 is max connection count ,12 is read idle timeout time，unit is second
            tcpServer.config(100000,12);
            tcpServer.start(9090, ioHandler);
            System.out.println( "server started,cost time "+(System.currentTimeMillis()-time)+"ms,use mem "+ ((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())/(1024*1024))+"M" );

            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    System.out.println("收到并处理消息："+processPacketTotalCount+"条，总花费时间："+(System.currentTimeMillis() - time)/1000+"秒，当前连接数:"+tcpServer.getConnections().size()+"，当前占用内存："+(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())/(1024*1024)+"M");
//                    System.out.println("TotalMemory:"+Runtime.getRuntime().totalMemory()/(1024*1024)+"M");
//                    System.out.println("FreeMemory:" + Runtime.getRuntime().freeMemory()/(1024*1024)+"M");
//                    System.out.println("MaxMemory:" + Runtime.getRuntime().maxMemory()/(1024*1024)+"M");
//                    System.out.println("UsedMemory:" + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())/(1024*1024)+"M");
//                    for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
//                        long count = gc.getCollectionCount();
//                        long time1 = gc.getCollectionTime();
//                        String name = gc.getName();
//                        System.out.println(String.format("%s: %s times %s ms", name, count, time1));
//                    }
//                    List<MemoryPoolMXBean> memoryPoolMXBeanList = ManagementFactory.getMemoryPoolMXBeans();
//                    for (MemoryPoolMXBean mp : memoryPoolMXBeanList){
//                        System.out.println(String.format("%s:%s,%s",mp.getName(), mp.getCollectionUsage(), mp.getUsage()));
//                    }
                }
            }, 100, 2000);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
