package com.yangc.bridge.service;

import java.util.List;

import com.yangc.bridge.bean.TBridgeChat;
import com.yangc.bridge.bean.TBridgeCommon;
import com.yangc.bridge.bean.TBridgeFile;

public interface CommonService {

	public void addCommon(TBridgeCommon common);

	public void addChat(TBridgeChat chat);

	public void addFile(TBridgeFile file);

	public void updateCommonStatusByUuid(String uuid);

	public List<TBridgeCommon> getUnreadCommonListByTo(String to);

}
