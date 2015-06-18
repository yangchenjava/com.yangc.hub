package com.yangc.bridge.listener;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.yangc.bridge.comm.Server;

public class NettyLoaderListener implements ServletContextListener {

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		WebApplicationContext webApplicationContext = WebApplicationContextUtils.getWebApplicationContext(sce.getServletContext());
		Server server = (Server) webApplicationContext.getBean("com.yangc.bridge.comm.Server");
		server.start();
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
	}

}
