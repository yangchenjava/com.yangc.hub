package com.yangc.bridge.comm.protocol.messagepack;

import org.msgpack.annotation.Message;

@Message
public class MessagePackResult {

	private String uuid;
	private boolean success;
	private String data;

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public boolean getSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

}
