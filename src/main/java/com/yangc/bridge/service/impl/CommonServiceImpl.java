package com.yangc.bridge.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.yangc.bridge.bean.TBridgeChat;
import com.yangc.bridge.bean.TBridgeCommon;
import com.yangc.bridge.bean.TBridgeFile;
import com.yangc.bridge.service.CommonService;
import com.yangc.dao.BaseDao;
import com.yangc.dao.JdbcDao;

@Service
public class CommonServiceImpl implements CommonService {

	@Autowired
	private BaseDao baseDao;
	@Autowired
	private JdbcDao jdbcDao;

	@Override
	public void addCommon(TBridgeCommon common) {
		this.baseDao.save(common);
	}

	@Override
	public void addChat(TBridgeChat chat) {
		this.baseDao.save(chat);
	}

	@Override
	public void addFile(TBridgeFile file) {
		this.baseDao.save(file);
	}

	@Override
	public void updateCommonStatusByUuid(String uuid) {
		String sql = JdbcDao.SQL_MAPPING.get("bridge.common.updateCommonStatusByUuid");
		Map<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put("uuid", uuid);
		this.jdbcDao.saveOrUpdate(sql, paramMap);
	}

	@Override
	public List<TBridgeCommon> getUnreadCommonListByTo(String to) {
		String sql = JdbcDao.SQL_MAPPING.get("bridge.common.getUnreadCommonListByTo");
		Map<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put("to", to);
		List<Map<String, Object>> mapList = this.jdbcDao.findAll(sql, paramMap);
		if (CollectionUtils.isEmpty(mapList)) return null;

		List<TBridgeCommon> chats = new ArrayList<TBridgeCommon>();
		for (Map<String, Object> map : mapList) {
			Long type = MapUtils.getLong(map, "TYPE");
			if (type == 0) {
				TBridgeChat text = new TBridgeChat();
				text.setId(MapUtils.getLong(map, "ID"));
				text.setUuid(MapUtils.getString(map, "UUID"));
				text.setFrom(MapUtils.getString(map, "FROM_USERNAME"));
				text.setTo(MapUtils.getString(map, "TO_USERNAME"));
				text.setData(MapUtils.getString(map, "DATA"));
				chats.add(text);
			} else if (type == 1) {
				TBridgeFile file = new TBridgeFile();
				file.setId(MapUtils.getLong(map, "ID"));
				file.setUuid(MapUtils.getString(map, "UUID"));
				file.setFrom(MapUtils.getString(map, "FROM_USERNAME"));
				file.setTo(MapUtils.getString(map, "TO_USERNAME"));
				file.setFileName(MapUtils.getString(map, "FILE_NAME"));
				file.setFileSize(MapUtils.getLong(map, "FILE_SIZE"));
				file.setFileMd5(MapUtils.getString(map, "FILE_MD5"));
				chats.add(file);
			}
		}
		return chats;
	}

}
