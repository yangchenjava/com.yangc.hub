package com.yangc.bridge.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.yangc.bridge.bean.ClientStatus;
import com.yangc.bridge.bean.ServerStatus;
import com.yangc.bridge.comm.Server;
import com.yangc.bridge.service.BridgeService;

@Service
public class BridgeServiceImpl implements BridgeService {

	@Autowired
	private Server server;

	@Override
	public ServerStatus getServerStatus() {
		return this.server.getServerStatus();
	}

	@Override
	public List<ClientStatus> getClientStatusList_page() {
		return this.server.getClientStatusList_page();
	}

	@Override
	public boolean isOnline(String username) {
		return this.server.isOnline(username);
	}

}
