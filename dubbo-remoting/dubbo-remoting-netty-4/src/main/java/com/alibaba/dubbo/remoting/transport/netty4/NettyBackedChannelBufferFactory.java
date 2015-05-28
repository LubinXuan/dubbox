package com.alibaba.dubbo.remoting.transport.netty4;

import com.alibaba.dubbo.remoting.buffer.ChannelBuffer;
import com.alibaba.dubbo.remoting.buffer.ChannelBufferFactory;
import io.netty.buffer.Unpooled;

import java.nio.ByteBuffer;

/**
 * Wrap netty dynamic channel buffer.
 *
 * @author <a href="mailto:gang.lvg@taobao.com">kimi</a>
 */
public class NettyBackedChannelBufferFactory implements ChannelBufferFactory {

    private static final NettyBackedChannelBufferFactory INSTANCE = new NettyBackedChannelBufferFactory();

    public static ChannelBufferFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public ChannelBuffer getBuffer(int capacity) {
        return new NettyBackedChannelBuffer(Unpooled.directBuffer(capacity));
    }

    @Override
    public ChannelBuffer getBuffer(byte[] array, int offset, int length) {
        return new NettyBackedChannelBuffer(Unpooled.copiedBuffer(array,offset,length));
    }

    @Override
    public ChannelBuffer getBuffer(ByteBuffer nioBuffer) {
        return new NettyBackedChannelBuffer(Unpooled.wrappedBuffer(nioBuffer));
    }
}
