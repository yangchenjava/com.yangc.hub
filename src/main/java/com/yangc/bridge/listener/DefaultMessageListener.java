package com.yangc.bridge.listener;

import io.netty.channel.Channel;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.jms.TextMessage;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.yangc.bridge.bean.TBridgeChat;
import com.yangc.bridge.bean.TBridgeFile;
import com.yangc.bridge.bean.UserBean;
import com.yangc.bridge.comm.Server;
import com.yangc.bridge.comm.cache.ChannelCache;
import com.yangc.bridge.comm.cache.ChannelManager;
import com.yangc.bridge.comm.handler.SendHandler;
import com.yangc.bridge.comm.handler.ServerHandler;
import com.yangc.bridge.comm.protocol.ContentType;

public class DefaultMessageListener implements MessageListener {

	private static final Logger logger = LogManager.getLogger(DefaultMessageListener.class);

	private static final Map<String, LinkedBlockingQueue<Serializable>> MESSAGE_QUEUE = new HashMap<String, LinkedBlockingQueue<Serializable>>();

	@Autowired
	private ChannelCache channelCache;

	private ExecutorService executorService;

	public DefaultMessageListener() {
		this.executorService = Executors.newCachedThreadPool();
	}

	/**
	 * @功能: jms异步消息接收
	 * @作者: yangc
	 * @创建日期: 2015年1月7日 下午5:24:33
	 * @param message
	 * @see javax.jms.MessageListener#onMessage(javax.jms.Message)
	 */
	@Override
	public void onMessage(Message message) {
		if (message != null) {
			try {
				if (message instanceof TextMessage) {
					logger.info("MessageListener - Text=" + ((TextMessage) message).getText());
				} else if (message instanceof ObjectMessage) {
					ObjectMessage msg = (ObjectMessage) message;
					// 如果不是当前服务器发布的消息
					if (!StringUtils.equals(msg.getStringProperty("IP"), Server.IP)) {
						Serializable obj = msg.getObject();

						String username = null;
						if (obj instanceof UserBean) username = ((UserBean) obj).getUsername();
						else if (obj instanceof TBridgeChat) username = ((TBridgeChat) obj).getTo();
						else if (obj instanceof TBridgeFile) username = ((TBridgeFile) obj).getTo();

						// 异步处理消息,每个用户有自己的消息队列,保证了同一用户的消息是有序的
						if (StringUtils.isNotBlank(username)) {
							synchronized (MESSAGE_QUEUE) {
								if (MESSAGE_QUEUE.containsKey(username)) {
									LinkedBlockingQueue<Serializable> queue = MESSAGE_QUEUE.get(username);
									queue.put(obj);
								} else {
									LinkedBlockingQueue<Serializable> queue = new LinkedBlockingQueue<Serializable>();
									queue.put(obj);
									MESSAGE_QUEUE.put(username, queue);
									this.executorService.execute(new Task(username, queue));
								}
							}
						}
					}
				}
			} catch (JMSException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private class Task implements Runnable {
		private String username;
		private LinkedBlockingQueue<Serializable> queue;

		private Task(String username, LinkedBlockingQueue<Serializable> queue) {
			this.username = username;
			this.queue = queue;
		}

		@Override
		public void run() {
			while (true) {
				try {
					while (!this.queue.isEmpty()) {
						Serializable obj = this.queue.poll();
						if (obj instanceof UserBean) {
							UserBean user = (UserBean) obj;
							Channel expireChannel = ChannelManager.get(user.getExpireChannelId());
							if (expireChannel != null && expireChannel.attr(ServerHandler.USER).get() != null
									&& StringUtils.equals(expireChannel.attr(ServerHandler.USER).get().getUsername(), this.username)) {
								// 标识断线重连的channel
								expireChannel.attr(ServerHandler.USER).get().setExpireChannelId(user.getExpireChannelId());
								expireChannel.disconnect();
							}
						} else if (obj instanceof TBridgeChat) {
							TBridgeChat chat = (TBridgeChat) obj;
							Long channelId = channelCache.getChannelId(this.username);
							if (channelId != null) {
								Channel channel = ChannelManager.get(channelId);
								if (channel != null && StringUtils.equals(channel.attr(ServerHandler.USER).get().getUsername(), this.username)) {
									SendHandler.sendChat(channel, chat);
								}
							}
						} else if (obj instanceof TBridgeFile) {
							TBridgeFile file = (TBridgeFile) obj;
							Long channelId = channelCache.getChannelId(this.username);
							if (channelId != null) {
								Channel channel = ChannelManager.get(channelId);
								if (channel != null && StringUtils.equals(channel.attr(ServerHandler.USER).get().getUsername(), this.username)) {
									if (file.getContentType() == ContentType.READY_FILE) {
										SendHandler.sendReadyFile(channel, file);
									} else {
										SendHandler.sendTransmitFile(channel, file);
									}
								}
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				synchronized (MESSAGE_QUEUE) {
					if (this.queue.isEmpty()) {
						MESSAGE_QUEUE.remove(this.username);
						break;
					}
				}
			}
		}
	}

}
