package com.yangc.bridge.comm.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.AttributeKey;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.yangc.bridge.bean.ResultBean;
import com.yangc.bridge.bean.TBridgeChat;
import com.yangc.bridge.bean.TBridgeFile;
import com.yangc.bridge.bean.UserBean;
import com.yangc.bridge.comm.cache.ChannelCache;
import com.yangc.bridge.comm.cache.ChannelManager;
import com.yangc.bridge.comm.handler.processor.ChatAndFileProcessor;
import com.yangc.bridge.comm.handler.processor.LoginProcessor;
import com.yangc.bridge.comm.handler.processor.ResultProcessor;

//多个连接使用同一个ChannelHandler,要加上@Sharable注解
@Sharable
@Service
public class ServerHandler extends ChannelInboundHandlerAdapter {

	private static final Logger logger = LogManager.getLogger(ServerHandler.class);

	public static final AttributeKey<UserBean> USER = AttributeKey.valueOf("USER");
	public static final AttributeKey<Integer> LOGIN_COUNT = AttributeKey.valueOf("LOGIN_COUNT");

	@Autowired
	private ChannelCache channelCache;
	@Autowired
	private ResultProcessor resultProcessor;
	@Autowired
	private LoginProcessor loginProcessor;
	@Autowired
	private ChatAndFileProcessor chatAndFileProcessor;

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		String remoteAddress = "";
		if (ctx.channel().remoteAddress() != null) {
			InetAddress address = ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress();
			if (address != null) {
				remoteAddress = address.getHostAddress();
			}
		}
		logger.info("channelActive - {}", remoteAddress);
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		String remoteAddress = "";
		if (ctx.channel().remoteAddress() != null) {
			InetAddress address = ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress();
			if (address != null) {
				remoteAddress = address.getHostAddress();
			}
		}
		logger.info("channelInactive - {}", remoteAddress);
		UserBean user = ctx.channel().attr(USER).getAndRemove();
		if (user != null && user.getChannelId() != user.getExpireChannelId()) {
			ChannelManager.remove(user.getChannelId());
			this.channelCache.removeChannelId(user.getUsername());
		}
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		String remoteAddress = "";
		if (ctx.channel().remoteAddress() != null) {
			InetAddress address = ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress();
			if (address != null) {
				remoteAddress = address.getHostAddress();
			}
		}
		logger.info("userEventTriggered - {}", remoteAddress);
		if (evt instanceof IdleStateEvent) {
			IdleStateEvent event = (IdleStateEvent) evt;
			if (event.state() == IdleState.READER_IDLE) {
				ctx.channel().disconnect();
			}
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		String remoteAddress = "";
		if (ctx.channel().remoteAddress() != null) {
			InetAddress address = ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress();
			if (address != null) {
				remoteAddress = address.getHostAddress();
			}
		}
		logger.error("exceptionCaught - " + remoteAddress + cause.getMessage(), cause);
		ctx.channel().disconnect();
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		logger.info("channelRead");
		if (msg instanceof Byte) {
			SendHandler.sendHeart(ctx.channel());
		} else if (msg instanceof ResultBean) {
			this.resultReceived(ctx.channel(), (ResultBean) msg);
		} else if (msg instanceof UserBean) {
			this.loginReceived(ctx.channel(), (UserBean) msg);
		} else if (msg instanceof TBridgeChat) {
			this.chatReceived(ctx.channel(), (TBridgeChat) msg);
		} else if (msg instanceof TBridgeFile) {
			this.fileReceived(ctx.channel(), (TBridgeFile) msg);
		} else {
			ctx.channel().disconnect();
		}
	}

	private void resultReceived(Channel channel, ResultBean result) throws Exception {
		if (channel.attr(USER).get() != null) {
			this.resultProcessor.process(channel, result);
		} else {
			channel.disconnect();
		}
	}

	private void loginReceived(Channel channel, UserBean user) throws Exception {
		this.loginProcessor.process(channel, user);
	}

	private void chatReceived(Channel channel, TBridgeChat chat) throws Exception {
		if (channel.attr(USER).get() != null) {
			this.chatAndFileProcessor.process(chat);
		} else {
			channel.disconnect();
		}
	}

	private void fileReceived(Channel channel, TBridgeFile file) throws Exception {
		if (channel.attr(USER).get() != null) {
			this.chatAndFileProcessor.process(file);
		} else {
			channel.disconnect();
		}
	}

}
