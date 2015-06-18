package com.yangc.bridge.resource;

import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.yangc.bean.DataGridBean;
import com.yangc.bridge.bean.ClientStatus;
import com.yangc.bridge.bean.ServerStatus;
import com.yangc.bridge.service.BridgeService;

@Controller
@RequestMapping("/bridge")
public class BridgeResource {

	private static final Logger logger = Logger.getLogger(BridgeResource.class);

	@Autowired
	private BridgeService bridgeService;

	@RequestMapping(value = "getServerStatus", method = RequestMethod.POST)
	@ResponseBody
	public ServerStatus getServerStatus() {
		logger.info("getServerStatus");
		return this.bridgeService.getServerStatus();
	}

	@RequestMapping(value = "getClientStatusList_page", method = RequestMethod.POST)
	@ResponseBody
	public DataGridBean getClientStatusList_page() {
		logger.info("getClientStatusList_page");
		List<ClientStatus> clientStatusList = this.bridgeService.getClientStatusList_page();
		return new DataGridBean(clientStatusList);
	}

	@RequestMapping(value = "isOnline", method = RequestMethod.POST)
	@ResponseBody
	public boolean isOnline(String username) {
		logger.info("isOnline");
		return this.bridgeService.isOnline(username);
	}

}
