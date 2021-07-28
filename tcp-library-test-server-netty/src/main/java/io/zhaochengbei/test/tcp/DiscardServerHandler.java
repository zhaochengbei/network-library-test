package io.zhaochengbei.test.tcp;


import io.netty.buffer.ByteBuf;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.nio.ByteBuffer;

/**
 * @Description:
 * @Author: chengbei.zhao
 * @Date: 2021/7/27 16:00
 **/
@ChannelHandler.Sharable
public class DiscardServerHandler extends ChannelInboundHandlerAdapter { // (1)

    public ChannelGroup channelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        super.handlerAdded(ctx);
        channelGroup.add(ctx.channel());
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        super.handlerRemoved(ctx);
        channelGroup.remove(ctx.channel());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) { // (2)
        // Discard the received data silently.
        ByteBuf data = ((ByteBuf) msg);
        while (data.isReadable(4)){
            data.markReaderIndex();
            Integer packetLength = data.readInt();
            if(!data.isReadable(packetLength)){
                data.resetReaderIndex();
                break;
            }

            ByteBuf byteBuf = ctx.alloc().buffer(4+packetLength);
            byteBuf.writeInt(packetLength);
            for(int i = 0;i<packetLength;i++){
                byteBuf.writeByte(data.readByte());
            }

            ctx.write(byteBuf);
            ctx.flush();
            synchronized (this){
                NettyTcpTestServer.processPacketTotalCount++;
            }

        }
//        Integer readableBytesLength = ((ByteBuf) msg).readableBytes();
//        Integer packetCount  = readableBytesLength/5;
//        for(int i =0;i<packetCount++;i++){
//            ByteBuf byteBuffer = ((ByteBuf) msg).readBytes(5);
//            ctx.write(byteBuffer); // (1)
//            synchronized (this){
//                NettyTcpTestServer.processPacketTotalCount += packetCount;
//            }
//        }

    }

//    @Override public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
//        ctx.flush(); // (2)
//    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) { // (4)
        // Close the connection when an exception is raised.
        cause.printStackTrace();
        ctx.close();
    }
}
