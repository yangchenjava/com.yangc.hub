package com.yangc.bridge.comm.handler;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.AttributeKey;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.apache.log4j.Logger;
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

	private static final Logger logger = Logger.getLogger(ServerHandler.class);

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
		logger.info("channelActive - " + remoteAddress);
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
		logger.info("channelInactive - " + remoteAddress);
		UserBean user = ctx.attr(USER).getAndRemove();
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
		logger.info("userEventTriggered - " + remoteAddress);
		if (evt instanceof IdleStateEvent) {
			IdleStateEvent event = (IdleStateEvent) evt;
			if (event.state() == IdleState.READER_IDLE) {
				ctx.disconnect();
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
		ctx.disconnect();
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		logger.info("channelRead");
		if (msg instanceof Byte) {
			SendHandler.sendHeart(ctx.channel());
		} else if (msg instanceof ResultBean) {
			this.resultReceived(ctx, (ResultBean) msg);
		} else if (msg instanceof UserBean) {
			this.loginReceived(ctx, (UserBean) msg);
		} else if (msg instanceof TBridgeChat) {
			this.chatReceived(ctx, (TBridgeChat) msg);
		} else if (msg instanceof TBridgeFile) {
			this.fileReceived(ctx, (TBridgeFile) msg);
		} else {
			ctx.disconnect();
		}
	}

	private void resultReceived(ChannelHandlerContext ctx, ResultBean result) throws Exception {
		if (ctx.attr(USER).get() != null) {
			this.resultProcessor.process(ctx.channel(), result);
		} else {
			ctx.disconnect();
		}
	}

	private void loginReceived(ChannelHandlerContext ctx, UserBean user) throws Exception {
		this.loginProcessor.process(ctx.channel(), user);
	}

	private void chatReceived(ChannelHandlerContext ctx, TBridgeChat chat) throws Exception {
		if (ctx.attr(USER).get() != null) {
			this.chatAndFileProcessor.process(chat);
		} else {
			ctx.disconnect();
		}
	}

	private void fileReceived(ChannelHandlerContext ctx, TBridgeFile file) throws Exception {
		if (ctx.attr(USER).get() != null) {
			this.chatAndFileProcessor.process(file);
		} else {
			ctx.disconnect();
		}
	}

}
