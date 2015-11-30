package com.yangc.bridge.comm.handler.processor;

import io.netty.channel.Channel;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import javax.jms.JMSException;
import javax.jms.ObjectMessage;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.stereotype.Service;

import com.yangc.bridge.bean.ResultBean;
import com.yangc.bridge.bean.TBridgeChat;
import com.yangc.bridge.bean.TBridgeCommon;
import com.yangc.bridge.bean.TBridgeFile;
import com.yangc.bridge.bean.UserBean;
import com.yangc.bridge.comm.Server;
import com.yangc.bridge.comm.cache.ChannelCache;
import com.yangc.bridge.comm.cache.ChannelManager;
import com.yangc.bridge.comm.handler.SendHandler;
import com.yangc.bridge.comm.handler.ServerHandler;
import com.yangc.bridge.service.CommonService;
import com.yangc.system.bean.TSysUser;
import com.yangc.system.service.UserService;
import com.yangc.utils.Message;
import com.yangc.utils.encryption.Md5Utils;

@Service
public class LoginProcessor {

	private static final AtomicLong idGenerator = new AtomicLong(0);

	@Autowired
	private ChannelCache channelCache;
	@Autowired
	private UserService userService;
	@Autowired
	private CommonService commonService;
	@Autowired
	private JmsTemplate jmsTemplate;

	private ExecutorService executorService;

	public LoginProcessor() {
		this.executorService = Executors.newSingleThreadExecutor();
	}

	/**
	 * @功能: 处理登录逻辑
	 * @作者: yangc
	 * @创建日期: 2015年1月7日 下午5:31:08
	 * @param channel
	 * @param user
	 */
	public void process(Channel channel, UserBean user) {
		this.executorService.execute(new Task(channel, user));
	}

	private class Task implements Runnable {
		private Channel channel;
		private UserBean user;

		private Task(Channel channel, UserBean user) {
			this.channel = channel;
			this.user = user;
		}

		@Override
		public void run() {
			try {
				String username = this.user.getUsername();
				List<TSysUser> users = userService.getUserListByUsernameAndPassword(username, Md5Utils.getMD5(this.user.getPassword()));

				ResultBean result = new ResultBean();
				result.setUuid(this.user.getUuid());
				if (CollectionUtils.isEmpty(users)) {
					result.setSuccess(false);
					result.setData("用户名或密码错误");
				} else if (users.size() > 1) {
					result.setSuccess(false);
					result.setData("用户重复");
				} else {
					Long channelId = idGenerator.incrementAndGet();
					this.user.setChannelId(channelId);

					Long expireChannelId = channelCache.getChannelId(username);
					if (expireChannelId != null) {
						Channel expireChannel = ChannelManager.get(expireChannelId);
						if (expireChannel != null && expireChannel.attr(ServerHandler.USER).get() != null && StringUtils.equals(expireChannel.attr(ServerHandler.USER).get().getUsername(), username)) {
							// 标识断线重连的channel
							expireChannel.attr(ServerHandler.USER).get().setExpireChannelId(expireChannelId);
							expireChannel.disconnect();
						} else {
							this.user.setExpireChannelId(expireChannelId);
							jmsTemplate.send(new MessageCreator() {
								@Override
								public javax.jms.Message createMessage(javax.jms.Session session) throws JMSException {
									ObjectMessage message = session.createObjectMessage();
									message.setStringProperty("IP", Server.IP);
									message.setObject(user);
									return message;
								}
							});
						}
					}
					this.channel.attr(ServerHandler.USER).set(this.user);
					// 添加缓存
					ChannelManager.put(channelId, this.channel);
					channelCache.putChannelId(username, channelId);

					result.setSuccess(true);
					result.setData("登录成功");
				}
				SendHandler.sendResult(this.channel, result);

				// 登录失败, 标记登录次数, 超过登录阀值就踢出
				if (!result.isSuccess()) {
					Integer loginCount = this.channel.attr(ServerHandler.LOGIN_COUNT).get() == null ? 1 : this.channel.attr(ServerHandler.LOGIN_COUNT).get();
					if (loginCount > 2) {
						this.channel.disconnect();
					} else {
						this.channel.attr(ServerHandler.LOGIN_COUNT).set(++loginCount);
					}
				}
				// 登录成功, 如果存在未读消息, 则发送
				else if (StringUtils.equals(Message.getMessage("bridge.offline_data"), "1")) {
					List<TBridgeCommon> commons = commonService.getUnreadCommonListByTo(this.user.getUsername());
					if (CollectionUtils.isNotEmpty(commons)) {
						for (TBridgeCommon common : commons) {
							if (common instanceof TBridgeChat) {
								SendHandler.sendChat(this.channel, (TBridgeChat) common);
							} else if (common instanceof TBridgeFile) {
								SendHandler.sendFile(this.channel, (TBridgeFile) common);
							}
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

}
