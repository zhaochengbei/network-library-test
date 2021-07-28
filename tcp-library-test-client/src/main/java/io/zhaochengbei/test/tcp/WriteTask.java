package io.zhaochengbei.test.tcp;

import io.dema.tcp.TcpConnection;

import java.nio.ByteBuffer;

/**
 * @Description: 向连接写数据任务
 * @Author: chengbei.zhao
 * @Date: 2021/7/27 11:10
 **/
public class WriteTask  implements Runnable {

    public TcpConnection connection;

    public ByteBuffer data;

    @Override
    public void run() {
        try {
            connection.writeAndFlush(data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}