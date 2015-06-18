package com.yangc.bridge.comm.protocol.prototype;

public class Protocol {

	private byte contentType; // 1byte 内容类型 0x00:result, 0x01:login, 0x02:chat, 0x03:readyFile, 0x04:transportFile, 0x55:heart
	private byte[] uuid; // 36byte
	private short fromLength; // 2byte
	private short toLength; // 2byte
	private int dataLength; // 4byte
	private byte[] from;
	private byte[] to;

	public byte getContentType() {
		return contentType;
	}

	public void setContentType(byte contentType) {
		this.contentType = contentType;
	}

	public byte[] getUuid() {
		return uuid;
	}

	public void setUuid(byte[] uuid) {
		this.uuid = uuid;
	}

	public short getFromLength() {
		return fromLength;
	}

	public void setFromLength(short fromLength) {
		this.fromLength = fromLength;
	}

	public short getToLength() {
		return toLength;
	}

	public void setToLength(short toLength) {
		this.toLength = toLength;
	}

	public int getDataLength() {
		return dataLength;
	}

	public void setDataLength(int dataLength) {
		this.dataLength = dataLength;
	}

	public byte[] getFrom() {
		return from;
	}

	public void setFrom(byte[] from) {
		this.from = from;
	}

	public byte[] getTo() {
		return to;
	}

	public void setTo(byte[] to) {
		this.to = to;
	}

}
