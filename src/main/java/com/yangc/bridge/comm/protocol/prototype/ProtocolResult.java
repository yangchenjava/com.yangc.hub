package com.yangc.bridge.comm.protocol.prototype;

/**
 * @功能: 0x68 [contentType(0x00)] [uuid] [dataLength] 0x68 [success] [data] [crc] 0x16
 * @作者: yangc
 * @创建日期: 2014年8月27日 下午9:50:57
 * @return
 */
public class ProtocolResult extends Protocol {

	private byte success;
	private byte[] data;

	public byte getSuccess() {
		return success;
	}

	public void setSuccess(byte success) {
		this.success = success;
	}

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}

}
