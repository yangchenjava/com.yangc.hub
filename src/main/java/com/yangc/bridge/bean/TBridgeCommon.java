package com.yangc.bridge.bean;

import com.yangc.bean.BaseBean;

public class TBridgeCommon extends BaseBean {

	private static final long serialVersionUID = -8248931483988118867L;

	private String uuid;
	private String from;
	private String to;
	private Long type;
	private Long status = 0L;

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public String getFrom() {
		return from;
	}

	public void setFrom(String from) {
		this.from = from;
	}

	public String getTo() {
		return to;
	}

	public void setTo(String to) {
		this.to = to;
	}

	public Long getType() {
		return type;
	}

	public void setType(Long type) {
		this.type = type;
	}

	public Long getStatus() {
		return status;
	}

	public void setStatus(Long status) {
		this.status = status;
	}

}
