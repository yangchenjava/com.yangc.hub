package com.yangc.bridge.comm.codec.prototype;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import com.yangc.bridge.comm.protocol.ContentType;
import com.yangc.bridge.comm.protocol.Tag;
import com.yangc.bridge.comm.protocol.prototype.ProtocolChat;
import com.yangc.bridge.comm.protocol.prototype.ProtocolFile;
import com.yangc.bridge.comm.protocol.prototype.ProtocolHeart;
import com.yangc.bridge.comm.protocol.prototype.ProtocolLogin;
import com.yangc.bridge.comm.protocol.prototype.ProtocolResult;

public class PrototypeEncoderData extends MessageToByteEncoder<Object> {

	@Override
	protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
		if (msg instanceof ProtocolResult) {
			this.encodeResult(out, (ProtocolResult) msg);
		} else if (msg instanceof ProtocolLogin) {
			this.encodeLogin(out, (ProtocolLogin) msg);
		} else if (msg instanceof ProtocolChat) {
			this.encodeChat(out, (ProtocolChat) msg);
		} else if (msg instanceof ProtocolFile) {
			this.encodeFile(out, (ProtocolFile) msg);
		} else if (msg instanceof ProtocolHeart) {
			this.encodeHeart(out, (ProtocolHeart) msg);
		}

		byte crc = 0;
		byte[] b = new byte[out.writerIndex()];
		out.getBytes(0, b);
		for (int i = 0; i < b.length; i++) {
			crc += b[i];
		}
		out.writeByte(crc);
		out.writeByte(Tag.FINAL);
	}

	private void encodeResult(ByteBuf out, ProtocolResult protocol) {
		out.writeByte(Tag.START);
		out.writeByte(protocol.getContentType());
		out.writeBytes(protocol.getUuid());
		out.writeInt(protocol.getDataLength());
		out.writeByte(Tag.END);
		out.writeByte(protocol.getSuccess());
		out.writeBytes(protocol.getData());
	}

	private void encodeLogin(ByteBuf out, ProtocolLogin protocol) {
		out.writeByte(Tag.START);
		out.writeByte(protocol.getContentType());
		out.writeBytes(protocol.getUuid());
		out.writeByte(Tag.END);
		out.writeShort(protocol.getUsernameLength());
		out.writeShort(protocol.getPasswordLength());
		out.writeBytes(protocol.getUsername());
		out.writeBytes(protocol.getPassword());
	}

	private void encodeChat(ByteBuf out, ProtocolChat protocol) {
		out.writeByte(Tag.START);
		out.writeByte(protocol.getContentType());
		out.writeBytes(protocol.getUuid());
		out.writeShort(protocol.getFromLength());
		out.writeShort(protocol.getToLength());
		out.writeInt(protocol.getDataLength());
		out.writeBytes(protocol.getFrom());
		out.writeBytes(protocol.getTo());
		out.writeByte(Tag.END);
		out.writeBytes(protocol.getData());
	}

	private void encodeFile(ByteBuf out, ProtocolFile protocol) {
		// 准备发送文件
		if (protocol.getContentType() == ContentType.READY_FILE) {
			out.writeByte(Tag.START);
			out.writeByte(protocol.getContentType());
			out.writeBytes(protocol.getUuid());
			out.writeShort(protocol.getFromLength());
			out.writeShort(protocol.getToLength());
			out.writeBytes(protocol.getFrom());
			out.writeBytes(protocol.getTo());
			out.writeByte(Tag.END);
			out.writeShort(protocol.getFileNameLength());
			out.writeBytes(protocol.getFileName());
			out.writeLong(protocol.getFileSize());
		}
		// 传输文件
		else if (protocol.getContentType() == ContentType.TRANSMIT_FILE) {
			out.writeByte(Tag.START);
			out.writeByte(protocol.getContentType());
			out.writeBytes(protocol.getUuid());
			out.writeShort(protocol.getFromLength());
			out.writeShort(protocol.getToLength());
			out.writeInt(protocol.getDataLength());
			out.writeBytes(protocol.getFrom());
			out.writeBytes(protocol.getTo());
			out.writeByte(Tag.END);
			out.writeShort(protocol.getFileNameLength());
			out.writeBytes(protocol.getFileName());
			out.writeLong(protocol.getFileSize());
			out.writeBytes(protocol.getFileMd5());
			out.writeInt(protocol.getOffset());
			out.writeBytes(protocol.getData());
		}
	}

	private void encodeHeart(ByteBuf out, ProtocolHeart protocol) {
		out.writeByte(Tag.START);
		out.writeByte(ContentType.HEART);
		out.writeByte(Tag.END);
	}

}
