package com.yangc.bridge.comm.cache;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.Tuple;

import com.yangc.common.Pagination;
import com.yangc.common.PaginationThreadUtils;
import com.yangc.utils.Message;
import com.yangc.utils.cache.RedisUtils;

/**
 * @功能: 使用redis的sortedset,key:CACHE_NAME,member:username,score:channelId
 * @作者: yangc
 * @创建日期: 2014年12月30日 下午7:22:23
 */
@Service
public class ChannelCache {

	private static final String CACHE_NAME = Message.getMessage("bridge.cache_name");

	/**
	 * @功能: 获取username对应的channelId
	 * @作者: yangc
	 * @创建日期: 2014年12月30日 下午7:22:23
	 * @param username
	 * @return
	 */
	public Long getChannelId(String username) {
		if (StringUtils.isNotBlank(username)) {
			RedisUtils cache = RedisUtils.getInstance();
			ShardedJedis jedis = null;
			try {
				jedis = cache.getJedis();
				Double channelId = jedis.zscore(CACHE_NAME, username);
				if (channelId != null) {
					return channelId.longValue();
				}
			} catch (Exception e) {
				e.printStackTrace();
				cache.returnBrokenResource(jedis);
			} finally {
				cache.returnResource(jedis);
			}
		}
		return null;
	}

	/**
	 * @功能: 保存username对应channelId
	 * @作者: yangc
	 * @创建日期: 2014年12月30日 下午7:24:59
	 * @param username
	 * @param channelId
	 */
	public void putChannelId(String username, Long channelId) {
		if (StringUtils.isNotBlank(username) && channelId != null) {
			RedisUtils cache = RedisUtils.getInstance();
			ShardedJedis jedis = null;
			try {
				jedis = cache.getJedis();
				jedis.zadd(CACHE_NAME, channelId.doubleValue(), username);
			} catch (Exception e) {
				e.printStackTrace();
				cache.returnBrokenResource(jedis);
			} finally {
				cache.returnResource(jedis);
			}
		}
	}

	/**
	 * @功能: 移除username
	 * @作者: yangc
	 * @创建日期: 2014年12月30日 下午7:25:36
	 * @param username
	 * @return
	 */
	public Long removeChannelId(String username) {
		if (StringUtils.isNotBlank(username)) {
			RedisUtils cache = RedisUtils.getInstance();
			ShardedJedis jedis = null;
			try {
				jedis = cache.getJedis();
				Double channelId = jedis.zscore(CACHE_NAME, username);
				if (channelId != null) {
					jedis.zrem(CACHE_NAME, username);
					return channelId.longValue();
				}
			} catch (Exception e) {
				e.printStackTrace();
				cache.returnBrokenResource(jedis);
			} finally {
				cache.returnResource(jedis);
			}
		}
		return null;
	}

	/**
	 * @功能: 清空缓存,慎用
	 * @作者: yangc
	 * @创建日期: 2014年12月30日 下午7:26:06
	 */
	public void clear() {
		RedisUtils cache = RedisUtils.getInstance();
		ShardedJedis jedis = null;
		try {
			jedis = cache.getJedis();
			jedis.del(CACHE_NAME);
		} catch (Exception e) {
			e.printStackTrace();
			cache.returnBrokenResource(jedis);
		} finally {
			cache.returnResource(jedis);
		}
	}

	/**
	 * @功能: 查看缓存是否包含该username
	 * @作者: yangc
	 * @创建日期: 2014年12月30日 下午7:26:57
	 * @param username
	 * @return
	 */
	public boolean contains(String username) {
		RedisUtils cache = RedisUtils.getInstance();
		ShardedJedis jedis = null;
		try {
			jedis = cache.getJedis();
			jedis.zscore(CACHE_NAME, username);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			cache.returnBrokenResource(jedis);
		} finally {
			cache.returnResource(jedis);
		}
		return false;
	}

	/**
	 * @功能: 分页获取用户连接信息
	 * @作者: yangc
	 * @创建日期: 2014年12月30日 下午7:27:40
	 * @return
	 */
	public Map<String, Long> getChannelCache() {
		RedisUtils cache = RedisUtils.getInstance();
		ShardedJedis jedis = null;
		try {
			jedis = cache.getJedis();

			/* 获取分页情况 */
			Pagination pagination = PaginationThreadUtils.get();
			if (pagination == null) {
				pagination = new Pagination();
				PaginationThreadUtils.set(pagination);
				pagination.setPageNow(1);
			}
			if (pagination.getTotalCount() == 0) {
				pagination.setTotalCount(jedis.zcard(CACHE_NAME).intValue());
			}
			int firstResult = (pagination.getPageNow() - 1) * pagination.getPageSize();
			/* 校验分页情况 */
			if (firstResult >= pagination.getTotalCount() || firstResult < 0) {
				firstResult = 0;
				pagination.setPageNow(1);
			}
			/* 如果总数返回0, 直接返回空 */
			if (pagination.getTotalCount() == 0) {
				return null;
			}
			Set<Tuple> tuples = jedis.zrangeWithScores(CACHE_NAME, firstResult, firstResult + pagination.getPageSize() - 1);

			Map<String, Long> map = new TreeMap<String, Long>();
			for (Tuple tuple : tuples) {
				map.put(tuple.getElement(), (long) tuple.getScore());
			}
			return map;
		} catch (Exception e) {
			e.printStackTrace();
			cache.returnBrokenResource(jedis);
		} finally {
			cache.returnResource(jedis);
		}
		return null;
	}

}
