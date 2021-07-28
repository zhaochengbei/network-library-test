package io.zhaochengbei.test.tcp;



import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import io.dema.tcp.*;

/**
 * @Description: TCP测试客户端
 * @Author: chengbei.zhao
 * @Date: 2021/7/27 11:05
 **/

public class TcpTestClients {
    static private IoHandler ioHandler = new IoHandler() {

        @Override
        public void onAccept(TcpConnection connection) {
//            System.out.println("c_onAccept,"+connection.socket);
        }
        @Override
        public void onRead(TcpConnection connection){
            //加这个影响测试结果，验证下不丢包之后，去掉测下极限。
            synchronized (this){
                receivePacketCount ++;
            }

            try {
                ByteBuffer data = ConnectionUtils.readPacket(connection, 0,4, 4,256);
//                System.out.println("c_onRead,"+connection.socket);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onClose(TcpConnection connection,String reason) {
            System.out.println("c_onClose,"+connection.socket);

        }

        @Override
        public void onReadIdle(TcpConnection connection) {
            connection.close(TcpConnectionCloseReason.ReadIdleTimeOut);
        }

    };


    static private TcpClients tcpClients;
    /**
     * be responsiable for send data；
     */
    static private ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(),new ThreadFactory() {
        public int threadIndex = 0;
        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "sendTestData"+threadIndex);
        }
    } );

    static public Integer sendPacketCount = 0;

    static public Integer receivePacketCount = 0;

    static private Timer timer = new Timer();

    public static void main( String[] args )
    {


        try {
            int testCount = 1;
            while(testCount-- >0){
                //创建100个客户端，每个客户端每隔1秒发送一条消息，共持续20秒。
                Integer clientCount = 1000;
                Integer clientSendTotalPacketCount = 2000000;
                Long sendPacketGap = 7L;
                testClients(10-testCount,clientCount,clientSendTotalPacketCount,sendPacketGap);
            }
            executorService.shutdown();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static public void testClients(Integer roundIndex,Integer clientCount,Integer clientSendTotalPacketCount,Long sendPacketGap)throws Exception{
        System.out.println("开始第"+roundIndex+"轮测试，启动连接："+clientCount+"个，每"+Float.parseFloat(sendPacketGap.toString())/1000+"秒发送1条消息,预计共发送"+clientSendTotalPacketCount+"条数据");

        Long time = System.currentTimeMillis();
        tcpClients = new TcpClients();
        tcpClients.start("localhost", 9090, clientCount, 1*TimeUnit.MILLISECONDS.ordinal(), ioHandler);
        System.out.println("创建连接耗时"+(System.currentTimeMillis()-time)/1000+"秒");

        Long time3 = System.currentTimeMillis();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Long gapTime = System.currentTimeMillis() - time3;
                gapTime = gapTime== 0?0:gapTime/1000;
                System.out.println("总发送消息："+sendPacketCount+"条，总接收消息："+receivePacketCount+"条，总花费时间："+gapTime+"秒，平均每秒发送"+(sendPacketCount==0||gapTime == 0?0:sendPacketCount/gapTime)+"条，当前连接数:"+tcpClients.getConnections().size()+"，当前占用内存："+(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())/(1024*1024)+"M");
            }
        }, 100, 1000);

        /**
         * 给线程分配任务，让线程发送消息给服务器；
         */
        while(!sendPacketCount.equals(clientSendTotalPacketCount)){
            ArrayList<TcpConnection> connections = tcpClients.getConnections();
            long time2 = System.currentTimeMillis();
            for (int i = 0; i < connections.size(); i++) {
                TcpConnection connection = connections.get(i);
                if(time2 - connection.lastWriteTime> sendPacketGap){
                    ByteBuffer byteBuffer = getTestPacket();
                    WriteTask dataTask = new WriteTask();
                    dataTask.connection = connection;
                    byteBuffer.flip();
                    dataTask.data = byteBuffer;
                    executorService.execute(dataTask);
                    sendPacketCount++;
                    if(sendPacketCount.equals(clientSendTotalPacketCount)){
                        break;
                    }
                    connection.lastWriteTime = System.currentTimeMillis();
                }
            }
            Thread.sleep(1);
        }
        Thread.sleep(6*1000);
        tcpClients.shutdown();
        timer.cancel();
    }

    static public ByteBuffer getTestPacket(){
        ByteBuffer buffer = ByteBuffer.allocate(9);
        buffer.putInt(5);
        buffer.put(Byte.valueOf("1"));
        buffer.put(Byte.valueOf("1"));
        buffer.put(Byte.valueOf("1"));
        buffer.put(Byte.valueOf("1"));
        buffer.put(Byte.valueOf("1"));
        //		buffer.flip();
        return buffer;
    }
}