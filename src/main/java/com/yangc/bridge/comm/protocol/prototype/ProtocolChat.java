package com.yangc.bridge.comm.protocol.prototype;

/**
 * @功能: 0x68 [contentType(0x02)] [uuid] [fromLength] [toLength] [dataLength] [from] [to] 0x68 [data] [crc] 0x16
 * @作者: yangc
 * @创建日期: 2014年8月27日 下午9:50:57
 * @return
 */
public class ProtocolChat extends Protocol {

	private byte[] data;

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}

}
