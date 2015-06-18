package com.yangc.bridge.comm.handler.processor;

import io.netty.channel.Channel;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.Session;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.stereotype.Service;

import com.yangc.bridge.bean.ResultBean;
import com.yangc.bridge.bean.TBridgeChat;
import com.yangc.bridge.bean.TBridgeCommon;
import com.yangc.bridge.bean.TBridgeFile;
import com.yangc.bridge.comm.Server;
import com.yangc.bridge.comm.cache.ChannelCache;
import com.yangc.bridge.comm.cache.ChannelManager;
import com.yangc.bridge.comm.handler.SendHandler;
import com.yangc.bridge.comm.protocol.ContentType;
import com.yangc.bridge.service.CommonService;
import com.yangc.utils.encryption.Md5Utils;

@Service
public class ChatAndFileProcessor {

	private static final Map<String, LinkedBlockingQueue<TBridgeCommon>> COMMON_QUEUE = new HashMap<String, LinkedBlockingQueue<TBridgeCommon>>();

	@Autowired
	private ChannelCache channelCache;
	@Autowired
	private CommonService commonService;
	@Autowired
	private JmsTemplate jmsTemplate;

	private ExecutorService executorService;

	public ChatAndFileProcessor() {
		this.executorService = Executors.newCachedThreadPool();
	}

	/**
	 * @功能: 处理消息转发逻辑
	 * @作者: yangc
	 * @创建日期: 2015年1月7日 下午5:31:33
	 * @param common
	 * @throws InterruptedException
	 */
	public void process(TBridgeCommon common) throws InterruptedException {
		// 异步处理消息,每个用户有自己的消息队列,保证了同一用户的消息是有序的
		String toUsername = common.getTo();
		if (StringUtils.isNotBlank(toUsername)) {
			synchronized (COMMON_QUEUE) {
				if (COMMON_QUEUE.containsKey(toUsername)) {
					LinkedBlockingQueue<TBridgeCommon> queue = COMMON_QUEUE.get(toUsername);
					queue.put(common);
				} else {
					LinkedBlockingQueue<TBridgeCommon> queue = new LinkedBlockingQueue<TBridgeCommon>();
					queue.put(common);
					COMMON_QUEUE.put(toUsername, queue);
					this.executorService.execute(new Task(toUsername, queue));
				}
			}
		}
	}

	private class Task implements Runnable {
		private String toUsername;
		private LinkedBlockingQueue<TBridgeCommon> queue;

		private Task(String toUsername, LinkedBlockingQueue<TBridgeCommon> queue) {
			this.toUsername = toUsername;
			this.queue = queue;
		}

		private void sendResult(TBridgeCommon common) throws Exception {
			ResultBean result = new ResultBean();
			result.setUuid(common.getUuid());
			result.setSuccess(true);
			result.setData("success");
			SendHandler.sendResult(ChannelManager.get(channelCache.getChannelId(common.getFrom())), result);
		}

		private void saveCommon(TBridgeCommon common) {
			TBridgeCommon c = new TBridgeCommon();
			BeanUtils.copyProperties(common, c);
			commonService.addCommon(c);
			common.setId(c.getId());
		}

		@Override
		public void run() {
			while (true) {
				try {
					while (!this.queue.isEmpty()) {
						TBridgeCommon common = this.queue.poll();
						if (common instanceof TBridgeChat) {
							this.sendResult(common);
							this.saveCommon(common);

							final TBridgeChat chat = (TBridgeChat) common;
							commonService.addChat(chat);

							Channel channel = ChannelManager.get(channelCache.getChannelId(this.toUsername));
							if (channel != null) {
								SendHandler.sendChat(channel, chat);
							} else {
								jmsTemplate.send(new MessageCreator() {
									@Override
									public Message createMessage(Session session) throws JMSException {
										ObjectMessage message = session.createObjectMessage();
										message.setStringProperty("IP", Server.IP);
										message.setObject(chat);
										return message;
									}
								});
							}
						} else if (common instanceof TBridgeFile) {
							final TBridgeFile file = (TBridgeFile) common;
							if (file.getContentType() == ContentType.TRANSMIT_FILE) {
								File dir = new File(FileUtils.getTempDirectory(), "com.yangc.bridge/" + this.toUsername);
								if (!dir.exists() || !dir.isDirectory()) {
									dir.delete();
									dir.mkdirs();
								}
								File targetFile = new File(dir, file.getUuid());
								if (!targetFile.exists() || !targetFile.isFile()) {
									targetFile.delete();
									targetFile.createNewFile();
								}
								RandomAccessFile raf = null;
								try {
									raf = new RandomAccessFile(targetFile, "rw");
									raf.seek(raf.length());
									raf.write(file.getData(), 0, file.getOffset());
								} catch (IOException e) {
									e.printStackTrace();
								} finally {
									try {
										if (raf != null) raf.close();
									} catch (IOException e) {
										e.printStackTrace();
									}
								}

								if (targetFile.length() == file.getFileSize() && Md5Utils.getMD5String(targetFile).equals(file.getFileMd5())) {
									this.sendResult(common);
									this.saveCommon(common);
									commonService.addFile(file);
								}
							}

							Channel channel = ChannelManager.get(channelCache.getChannelId(this.toUsername));
							if (channel != null) {
								switch (file.getContentType()) {
								case ContentType.READY_FILE:
									SendHandler.sendReadyFile(channel, file);
									break;
								case ContentType.TRANSMIT_FILE:
									SendHandler.sendTransmitFile(channel, file);
									break;
								}
							} else {
								jmsTemplate.send(new MessageCreator() {
									@Override
									public Message createMessage(Session session) throws JMSException {
										ObjectMessage message = session.createObjectMessage();
										message.setStringProperty("IP", Server.IP);
										message.setObject(file);
										return message;
									}
								});
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				synchronized (COMMON_QUEUE) {
					if (this.queue.isEmpty()) {
						COMMON_QUEUE.remove(this.toUsername);
						break;
					}
				}
			}
		}
	}

}
