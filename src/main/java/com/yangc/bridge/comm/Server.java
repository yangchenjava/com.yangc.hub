package com.yangc.bridge.comm;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.AdaptiveRecvByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.net.ssl.SSLEngine;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.yangc.bridge.bean.ClientStatus;
import com.yangc.bridge.bean.ServerStatus;
import com.yangc.bridge.comm.cache.ChannelCache;
import com.yangc.bridge.comm.codec.messagepack.MessagePackDecoderData;
import com.yangc.bridge.comm.codec.messagepack.MessagePackEncoderData;
import com.yangc.bridge.comm.codec.protobuf.ProtobufDecoderData;
import com.yangc.bridge.comm.codec.protobuf.ProtobufEncoderData;
import com.yangc.bridge.comm.codec.prototype.PrototypeDecoderData;
import com.yangc.bridge.comm.codec.prototype.PrototypeEncoderData;
import com.yangc.bridge.comm.handler.ServerHandler;
import com.yangc.bridge.comm.handler.ssl.SslContextFactory;
import com.yangc.utils.Message;

@Service("com.yangc.bridge.comm.Server")
public class Server {

	private static final Logger logger = Logger.getLogger(Server.class);

	public static final String IP = Message.getMessage("bridge.ipAddress");
	public static final int PORT = Integer.parseInt(Message.getMessage("bridge.port"));
	public static final int TIMEOUT = Integer.parseInt(Message.getMessage("bridge.timeout"));
	public static final String CODEC = Message.getMessage("bridge.codec");

	@Autowired
	private ServerHandler serverHandler;
	@Autowired
	private ChannelCache channelCache;

	private ChannelFuture channelFuture;

	private void init() {
		EventLoopGroup bossGroup = new NioEventLoopGroup();
		EventLoopGroup workerGroup = new NioEventLoopGroup();
		try {
			ServerBootstrap b = new ServerBootstrap();
			b.option(ChannelOption.SO_REUSEADDR, true).childOption(ChannelOption.SO_REUSEADDR, true).childOption(ChannelOption.SO_KEEPALIVE, true)
					.childOption(ChannelOption.RCVBUF_ALLOCATOR, AdaptiveRecvByteBufAllocator.DEFAULT);
			b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class).handler(new LoggingHandler()).childHandler(new ChannelInitializer<SocketChannel>() {
				@Override
				protected void initChannel(SocketChannel ch) throws Exception {
					ChannelPipeline pipeline = ch.pipeline();
					SSLEngine sslEngine = SslContextFactory.getSslContext().createSSLEngine();
					sslEngine.setNeedClientAuth(true);
					sslEngine.setUseClientMode(false);
					pipeline.addLast(new SslHandler(sslEngine));
					pipeline.addLast(new IdleStateHandler(TIMEOUT, 0, 0));
					// 解码(Inbound)按照从头到尾的顺序执行
					// 编码(Outbound)按照从尾到头的顺序执行
					if (StringUtils.equals(Server.CODEC, "protobuf")) {
						pipeline.addLast(new ProtobufDecoderData());
						pipeline.addLast(new ProtobufEncoderData());
					} else if (StringUtils.equals(Server.CODEC, "messagepack")) {
						pipeline.addLast(new MessagePackDecoderData());
						pipeline.addLast(new MessagePackEncoderData());
					} else {
						pipeline.addLast(new PrototypeDecoderData());
						pipeline.addLast(new PrototypeEncoderData());
					}
					pipeline.addLast(serverHandler);
				}
			});
			this.channelFuture = b.bind(IP, PORT).sync();
			this.channelFuture.channel().closeFuture().sync();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}
	}

	/**
	 * @功能: netty服务启动
	 * @作者: yangc
	 * @创建日期: 2014年12月30日 下午2:06:18
	 */
	public void start() {
		logger.info("==========netty服务启动=========");
		new Thread(new Runnable() {
			@Override
			public void run() {
				init();
			}
		}).start();
	}

	/**
	 * @功能: netty服务是否存活
	 * @作者: yangc
	 * @创建日期: 2014年12月30日 下午2:06:35
	 * @return
	 */
	public boolean isActive() {
		if (this.channelFuture != null) {
			return this.channelFuture.isSuccess();
		}
		return false;
	}

	/**
	 * @功能: 获取netty服务端状态
	 * @作者: yangc
	 * @创建日期: 2014年12月30日 下午2:06:56
	 * @return
	 */
	public ServerStatus getServerStatus() {
		ServerStatus serverStatus = new ServerStatus();
		serverStatus.setIpAddress(IP);
		serverStatus.setPort(PORT);
		serverStatus.setTimeout(TIMEOUT);
		serverStatus.setActive(this.isActive());
		return serverStatus;
	}

	/**
	 * @功能: 分页获取客户端连接的状态
	 * @作者: yangc
	 * @创建日期: 2014年12月30日 下午2:07:16
	 * @return
	 */
	public List<ClientStatus> getClientStatusList_page() {
		List<ClientStatus> clientStatusList = new ArrayList<ClientStatus>();
		Map<String, Long> map = this.channelCache.getChannelCache();
		if (MapUtils.isNotEmpty(map)) {
			for (Entry<String, Long> entry : map.entrySet()) {
				ClientStatus clientStatus = new ClientStatus();
				clientStatus.setUsername(entry.getKey());
				clientStatus.setChannelId(entry.getValue());
				clientStatusList.add(clientStatus);
			}
		}
		return clientStatusList;
	}

	/**
	 * @功能: 判断客户端是否在线
	 * @作者: yangc
	 * @创建日期: 2014年12月30日 下午8:31:41
	 * @param username
	 * @return
	 */
	public boolean isOnline(String username) {
		return this.channelCache.getChannelId(username) != null;
	}

}
