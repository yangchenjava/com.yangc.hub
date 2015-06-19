package com.yangc.bridge.comm.protocol.messagepack;

import org.msgpack.annotation.Message;

@Message
public class MessagePackChat {

	private String uuid;
	private String from;
	private String to;
	private String data;

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

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

}
