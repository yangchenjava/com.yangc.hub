package com.yangc.bridge.service;

import java.util.List;

import com.yangc.bridge.bean.ClientStatus;
import com.yangc.bridge.bean.ServerStatus;

public interface BridgeService {

	public ServerStatus getServerStatus();

	public List<ClientStatus> getClientStatusList_page();

	public boolean isOnline(String username);

}
