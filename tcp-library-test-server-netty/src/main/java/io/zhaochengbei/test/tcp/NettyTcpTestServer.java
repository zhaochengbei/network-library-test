package io.zhaochengbei.test.tcp;


import io.netty.bootstrap.ServerBootstrap;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import sun.misc.SharedSecrets;

import java.lang.management.BufferPoolMXBean;
import java.lang.management.ManagementFactory;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @Description: NettyTCP测试服务器
 * @Author: chengbei.zhao
 * @Date: 2021/7/27 15:59
 **/
public class NettyTcpTestServer {

    private int port;

    static private DiscardServerHandler discardServerHandler = new DiscardServerHandler();

    static public int processPacketTotalCount = 0;

    static private Timer timer = new Timer();

    public NettyTcpTestServer(int port) {
        this.port = port;
    }

    public void run() throws Exception {
        Long time = System.currentTimeMillis();

        EventLoopGroup bossGroup = new NioEventLoopGroup(); // (1)//官方源码，就不动了。
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap(); // (2)
            b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class) // (3)
                .childHandler(new ChannelInitializer<SocketChannel>() { // (4)
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(discardServerHandler);
                    }
                })
                .option(ChannelOption.SO_BACKLOG, 128)
                .handler(new LoggingHandler(LogLevel.INFO))      // (5)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.TCP_NODELAY,true); // (6)


            // Bind and start to accept incoming connections.
            ChannelFuture f = b.bind(port).sync(); // (7)

            System.out.println( "server started,cost time "+(System.currentTimeMillis()-time)+"ms,use mem "+ ((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())/(1024*1024))+"M" );

            Long time2 = System.currentTimeMillis();
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    System.out.println("收到并处理消息："+processPacketTotalCount+"条，总花费时间："+(System.currentTimeMillis() - time2)/1000+"秒，当前连接数:"+discardServerHandler.channelGroup.size()+"，当前占用内存："+(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())/(1024*1024)+"M");
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
//                    BufferPoolMXBean bufferPoolMXBean = ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class).stream().filter(e->e.getName().equals("direct")).findFirst().orElse(null);
//                    System.out.println(SharedSecrets.getJavaNioAccess().getDirectBufferPool().getCount());
//                    System.out.println(SharedSecrets.getJavaNioAccess().getDirectBufferPool().getMemoryUsed());
//                    System.out.println(bufferPoolMXBean.getMemoryUsed());


                }
            }, 100, 2000);

            // Wait until the server socket is closed.
            // In this example, this does not happen, but you can do that to gracefully
            // shut down your server.
            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws Exception {
        int port = 9090;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }

        new NettyTcpTestServer(port).run();
    }

}
