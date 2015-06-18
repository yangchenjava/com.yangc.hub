package com.yangc.bridge.comm.handler.processor;

import io.netty.channel.Channel;

import java.io.File;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.yangc.bridge.bean.ResultBean;
import com.yangc.bridge.comm.handler.ServerHandler;
import com.yangc.bridge.service.CommonService;

@Service
public class ResultProcessor {

	@Autowired
	private CommonService commonService;

	private ThreadPoolExecutor threadPool;

	public ResultProcessor() {
		// 初始化线程池
		this.threadPool = new ThreadPoolExecutor(5, 10, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), new ThreadPoolExecutor.DiscardOldestPolicy());
	}

	/**
	 * @功能: 处理消息响应结果
	 * @作者: yangc
	 * @创建日期: 2015年1月7日 下午5:29:31
	 * @param channel
	 * @param result
	 */
	public void process(Channel channel, ResultBean result) {
		String username = channel.attr(ServerHandler.USER).get().getUsername();
		this.threadPool.execute(new Task(result, username));
	}

	class Task implements Runnable {
		private ResultBean result;
		private String username;

		private Task(ResultBean result, String username) {
			this.result = result;
			this.username = username;
		}

		@Override
		public void run() {
			try {
				if (this.result.isSuccess()) {
					String uuid = this.result.getUuid();
					commonService.updateCommonStatusByUuid(uuid);
					File file = new File(FileUtils.getTempDirectoryPath() + "/com.yangc.bridge/" + this.username + "/" + uuid);
					if (file.exists()) {
						file.delete();
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

}
