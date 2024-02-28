/*******************************************************************************
 * Copyright (c) 2017-2019, org.smartboot. All rights reserved.
 * project name: smart-socket
 * file name: AioQuickServer.java
 * Date: 2019-12-31
 * Author: sandao (zhengjunweimail@163.com)
 *
 ******************************************************************************/

package org.smartboot.socket.transport;
/*
import org.smartboot.socket.MessageProcessor;
import org.smartboot.socket.Protocol;
import org.smartboot.socket.StateMachineEnum;
import org.smartboot.socket.VirtualBufferFactory;
import org.smartboot.socket.buffer.BufferFactory;
import org.smartboot.socket.buffer.BufferPagePool;
import org.smartboot.socket.buffer.VirtualBuffer;
import org.smartboot.socket.enhance.EnhanceAsynchronousChannelProvider;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketOption;
import java.net.StandardSocketOptions;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.spi.AsynchronousChannelProvider;
import java.security.InvalidParameterException;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

*//**
 * AIO服务端。
 *
 * <h2>示例：</h2>
 * <p>
 * <pre>
 * public class IntegerServer {
 *     public static void main(String[] args) throws IOException {
 *         AioQuickServer<Integer> server = new AioQuickServer<Integer>(8888, new IntegerProtocol(), new IntegerServerProcessor());
 *         server.start();
 *     }
 * }
 * </pre>
 * </p>
 *
 * @author 三刀
 * @version V1.0.0
 *//*
public final class AioQuickServer {
    *//**
     * Server端服务配置。
     * <p>调用AioQuickServer的各setXX()方法，都是为了设置config的各配置项</p>
     *//*
    private final IoServerConfig config = new IoServerConfig();
    *//**
     * 内存池
     *//*
    private BufferPagePool bufferPool;
    *//**
     * 读回调事件处理
     *//*
    private ReadCompletionHandler aioReadCompletionHandler;
    *//**
     * ConcurrentReadCompletionHandler 回调守护线程
     *//*
    private ThreadPoolExecutor concurrentReadCompletionHandlerExecutor;
    *//**
     * 写回调事件处理
     *//*
    private WriteCompletionHandler aioWriteCompletionHandler;
    private BufferPagePool innerBufferPool = null;

    *//**
     * asynchronousServerSocketChannel
     *//*
    private AsynchronousServerSocketChannel serverSocketChannel = null;
    *//**
     * asynchronousChannelGroup
     *//*
    private AsynchronousChannelGroup asynchronousChannelGroup;

    private VirtualBufferFactory readBufferFactory = bufferPage -> bufferPage.allocate(config.getReadBufferSize());

    *//**
     * 设置服务端启动必要参数配置
     *
     * @param port             绑定服务端口号
     * @param protocol         协议编解码
     * @param messageProcessor 消息处理器
     *//*
    public <T> AioQuickServer(int port, Protocol<T> protocol, MessageProcessor<T> messageProcessor) {
        config.setPort(port);
        config.setProtocol(protocol);
        config.setProcessor(messageProcessor);
        config.setThreadNum(Runtime.getRuntime().availableProcessors());
    }

    *//**
     * @param host             绑定服务端Host地址
     * @param port             绑定服务端口号
     * @param protocol         协议编解码
     * @param messageProcessor 消息处理器
     *//*
    public <T> AioQuickServer(String host, int port, Protocol<T> protocol, MessageProcessor<T> messageProcessor) {
        this(port, protocol, messageProcessor);
        config.setHost(host);
    }

    *//**
     * 启动Server端的AIO服务
     *
     * @throws IOException IO异常
     *//*
    public void start() throws IOException {
        if (config.isBannerEnabled()) {
            //System.out.println(IoServerConfig.BANNER + "\r\n :: smart-socket " + (config.isAioEnhance() ? "[enhance]" : "") + "::\t(" + IoServerConfig.VERSION + ")");
        }
        start0();
    }

    *//**
     * 内部启动逻辑
     *
     * @throws IOException IO异常
     *//*
    private void start0() throws IOException {
        checkAndResetConfig();
        try {
            aioWriteCompletionHandler = new WriteCompletionHandler();
            if (bufferPool == null) {
                this.bufferPool = config.getBufferFactory().create();
                this.innerBufferPool = bufferPool;
            }
            AsynchronousChannelProvider provider;
            if (config.isAioEnhance()) {
                aioReadCompletionHandler = new ReadCompletionHandler();
                provider = new EnhanceAsynchronousChannelProvider(config.isLowMemory());
            } else {
                concurrentReadCompletionHandlerExecutor = new ThreadPoolExecutor(1, 1,
                        60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
                aioReadCompletionHandler = new ConcurrentReadCompletionHandler(new Semaphore(config.getThreadNum() - 1), concurrentReadCompletionHandlerExecutor);
                provider = AsynchronousChannelProvider.provider();
            }
            asynchronousChannelGroup = provider.openAsynchronousChannelGroup(config.getThreadNum(), new ThreadFactory() {
                private byte index = 0;

                @Override
                public Thread newThread(Runnable r) {
                    return bufferPool.newThread(r, "smart-socket:Thread-" + (++index));
                }
            });
            this.serverSocketChannel = AsynchronousServerSocketChannel.open(asynchronousChannelGroup);
            //set socket options
            if (config.getSocketOptions() != null) {
                for (Map.Entry<SocketOption<Object>, Object> entry : config.getSocketOptions().entrySet()) {
                    this.serverSocketChannel.setOption(entry.getKey(), entry.getValue());
                }
            }
            //bind host
            if (config.getHost() != null) {
                serverSocketChannel.bind(new InetSocketAddress(config.getHost(), config.getPort()), config.getBacklog());
            } else {
                serverSocketChannel.bind(new InetSocketAddress(config.getPort()), config.getBacklog());
            }

            startAcceptThread();
        } catch (IOException e) {
            shutdown();
            throw e;
        }
        //System.out.println("smart-socket server started on port " + config.getPort() + ",threadNum:" + config.getThreadNum());
        //System.out.println("smart-socket server config is " + config);
    }

    private void startAcceptThread() {
        Supplier<VirtualBuffer> supplier = () -> readBufferFactory.newBuffer(bufferPool.allocateBufferPage());
        serverSocketChannel.accept(null, new CompletionHandler<AsynchronousSocketChannel, Void>() {
            @Override
            public void completed(AsynchronousSocketChannel channel, Void attachment) {
                try {
                    serverSocketChannel.accept(attachment, this);
                } catch (Throwable throwable) {
                    config.getProcessor().stateEvent(null, StateMachineEnum.ACCEPT_EXCEPTION, throwable);
                    failed(throwable, attachment);
                    serverSocketChannel.accept(attachment, this);
                } finally {
                    createSession(channel, supplier);
                }
            }

            @Override
            public void failed(Throwable exc, Void attachment) {
                exc.printStackTrace();
            }
        });
    }

    *//**
     * 检查配置项
     *//*
    private void checkAndResetConfig() {
        //确保单核CPU默认初始化至少2个线程
        if (config.getThreadNum() == 1) {
            config.setThreadNum(2);
        }
    }

    *//**
     * 为每个新建立的连接创建AIOSession对象
     *
     * @param channel 当前已建立连接通道
     *//*
    private void createSession(AsynchronousSocketChannel channel, Supplier<VirtualBuffer> supplier) {
        //连接成功则构造AIOSession对象
        TcpAioSession session = null;
        AsynchronousSocketChannel acceptChannel = channel;
        try {
            if (config.getMonitor() != null) {
                acceptChannel = config.getMonitor().shouldAccept(channel);
            }
            if (acceptChannel != null) {
                acceptChannel.setOption(StandardSocketOptions.TCP_NODELAY, true);
                session = new TcpAioSession(acceptChannel, config, aioReadCompletionHandler, aioWriteCompletionHandler, bufferPool.allocateBufferPage(), supplier);
            } else {
                config.getProcessor().stateEvent(null, StateMachineEnum.REJECT_ACCEPT, null);
                IOUtil.close(channel);
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (session == null) {
                IOUtil.close(channel);
            } else {
                session.close();
            }
        }
    }

    *//**
     * 停止服务端
     *//*
    public void shutdown() {
        try {
            if (serverSocketChannel != null) {
                serverSocketChannel.close();
                serverSocketChannel = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (!asynchronousChannelGroup.isTerminated()) {
            try {
                asynchronousChannelGroup.shutdownNow();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            asynchronousChannelGroup.awaitTermination(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (innerBufferPool != null) {
            innerBufferPool.release();
        }
        if (concurrentReadCompletionHandlerExecutor != null) {
            concurrentReadCompletionHandlerExecutor.shutdown();
            concurrentReadCompletionHandlerExecutor = null;
        }
    }

    *//**
     * 设置读缓存区大小
     *
     * @param size 单位：byte
     * @return 当前AioQuickServer对象
     *//*
    public final AioQuickServer setReadBufferSize(int size) {
        this.config.setReadBufferSize(size);
        return this;
    }

    *//**
     * 是否启用 AIO 增强模式。默认：true
     *
     * @param enabled true:启用；false:禁用
     *//*
    public AioQuickServer setAioEnhance(boolean enabled) {
        config.setAioEnhance(enabled);
        return this;
    }

    *//**
     * 是否启用控制台Banner打印
     *
     * @param bannerEnabled true:启用，false:禁用
     * @return 当前AioQuickServer对象
     *//*
    public final AioQuickServer setBannerEnabled(boolean bannerEnabled) {
        config.setBannerEnabled(bannerEnabled);
        return this;
    }

    *//**
     * 设置Socket的TCP参数配置。
     * <p>
     * AIO客户端的有效可选范围为：<br/>
     * 2. StandardSocketOptions.SO_RCVBUF<br/>
     * 4. StandardSocketOptions.SO_REUSEADDR<br/>
     * </p>
     *
     * @param socketOption 配置项
     * @param value        配置值
     * @param <V>          配置项类型
     * @return 当前AioQuickServer对象
     *//*
    public final <V> AioQuickServer setOption(SocketOption<V> socketOption, V value) {
        config.setOption(socketOption, value);
        return this;
    }

    *//**
     * 设置服务工作线程数,设置数值必须大于等于2
     *
     * @param threadNum 线程数
     * @return 当前AioQuickServer对象
     *//*
    public final AioQuickServer setThreadNum(int threadNum) {
        if (threadNum <= 1) {
            throw new InvalidParameterException("threadNum must >= 2");
        }
        config.setThreadNum(threadNum);
        return this;
    }


    *//**
     * 设置输出缓冲区容量
     *
     * @param bufferSize     单个内存块大小
     * @param bufferCapacity 内存块数量上限
     * @return 当前AioQuickServer对象
     *//*
    public final AioQuickServer setWriteBuffer(int bufferSize, int bufferCapacity) {
        config.setWriteBufferSize(bufferSize);
        config.setWriteBufferCapacity(bufferCapacity);
        return this;
    }

    *//**
     * 设置 backlog 大小
     *
     * @param backlog backlog大小
     * @return 当前AioQuickServer对象
     *//*
    public final AioQuickServer setBacklog(int backlog) {
        config.setBacklog(backlog);
        return this;
    }

    *//**
     * 设置内存池。
     * 通过该方法设置的内存池，在AioQuickServer执行shutdown时不会触发内存池的释放。
     * 该方法适用于多个AioQuickServer、AioQuickClient共享内存池的场景。
     * <b>在启用内存池的情况下会有更好的性能表现</b>
     *
     * @param bufferPool 内存池对象
     * @return 当前AioQuickServer对象
     *//*
    public final AioQuickServer setBufferPagePool(BufferPagePool bufferPool) {
        this.bufferPool = bufferPool;
        this.config.setBufferFactory(BufferFactory.DISABLED_BUFFER_FACTORY);
        return this;
    }

    *//**
     * 设置内存池的构造工厂。
     * 通过工厂形式生成的内存池会强绑定到当前AioQuickServer对象，
     * 在AioQuickServer执行shutdown时会释放内存池。
     * <b>在启用内存池的情况下会有更好的性能表现</b>
     *
     * @param bufferFactory 内存池工厂
     * @return 当前AioQuickServer对象
     *//*
    public final AioQuickServer setBufferFactory(BufferFactory bufferFactory) {
        this.config.setBufferFactory(bufferFactory);
        this.bufferPool = null;
        return this;
    }

    public final AioQuickServer setReadBufferFactory(VirtualBufferFactory readBufferFactory) {
        this.readBufferFactory = readBufferFactory;
        return this;
    }

    public AioQuickServer setLowMemory(boolean lowMemory) {
        this.config.setLowMemory(lowMemory);
        return this;
    }

}*/
