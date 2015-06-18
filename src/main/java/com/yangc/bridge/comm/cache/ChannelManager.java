package com.yangc.bridge.comm.cache;

import io.netty.channel.Channel;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ChannelManager {

	private static final ConcurrentMap<Long, Channel> CHANNELS = new ConcurrentHashMap<Long, Channel>();

	public static Channel get(Long key) {
		if (key != null) {
			return CHANNELS.get(key);
		}
		return null;
	}

	public static Channel put(Long key, Channel value) {
		if (key != null && value != null) {
			return CHANNELS.put(key, value);
		}
		return null;
	}

	public static Channel remove(Long key) {
		if (key != null) {
			return CHANNELS.remove(key);
		}
		return null;
	}

	public static void clear() {
		CHANNELS.clear();
	}

	public static boolean containsKey(Long key) {
		return CHANNELS.containsKey(key);
	}

	public static long size() {
		return CHANNELS.size();
	}

	public static Map<Long, Channel> map() {
		return new HashMap<Long, Channel>(CHANNELS);
	}

}
