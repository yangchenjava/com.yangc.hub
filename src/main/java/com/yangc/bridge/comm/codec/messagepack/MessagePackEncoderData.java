package com.yangc.bridge.comm.codec.messagepack;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.io.IOException;

import org.apache.commons.lang3.ArrayUtils;
import org.msgpack.MessagePack;

import com.yangc.bridge.comm.protocol.ContentType;
import com.yangc.bridge.comm.protocol.Tag;
import com.yangc.bridge.comm.protocol.messagepack.MessagePackChat;
import com.yangc.bridge.comm.protocol.messagepack.MessagePackFile;
import com.yangc.bridge.comm.protocol.messagepack.MessagePackHeart;
import com.yangc.bridge.comm.protocol.messagepack.MessagePackLogin;
import com.yangc.bridge.comm.protocol.messagepack.MessagePackResult;

public class MessagePackEncoderData extends MessageToByteEncoder<Object> {

	@Override
	protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
		if (msg instanceof MessagePackResult) {
			this.encodeResult(out, (MessagePackResult) msg);
		} else if (msg instanceof MessagePackLogin) {
			this.encodeLogin(out, (MessagePackLogin) msg);
		} else if (msg instanceof MessagePackChat) {
			this.encodeChat(out, (MessagePackChat) msg);
		} else if (msg instanceof MessagePackFile) {
			this.encodeFile(out, (MessagePackFile) msg);
		} else if (msg instanceof MessagePackHeart) {
			this.encodeHeart(out, (MessagePackHeart) msg);
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

	private void encodeResult(ByteBuf out, MessagePackResult message) throws IOException {
		out.writeByte(Tag.START);
		out.writeByte(ContentType.RESULT);

		byte[] bytes = new MessagePack().write(message);
		out.writeInt(bytes.length);
		out.writeBytes(bytes);
	}

	private void encodeLogin(ByteBuf out, MessagePackLogin message) throws IOException {
		out.writeByte(Tag.START);
		out.writeByte(ContentType.LOGIN);

		byte[] bytes = new MessagePack().write(message);
		out.writeInt(bytes.length);
		out.writeBytes(bytes);
	}

	private void encodeChat(ByteBuf out, MessagePackChat message) throws IOException {
		out.writeByte(Tag.START);
		out.writeByte(ContentType.CHAT);

		byte[] bytes = new MessagePack().write(message);
		out.writeInt(bytes.length);
		out.writeBytes(bytes);
	}

	private void encodeFile(ByteBuf out, MessagePackFile message) throws IOException {
		out.writeByte(Tag.START);

		// 准备发送文件
		if (ArrayUtils.isEmpty(message.getData())) out.writeByte(ContentType.READY_FILE);
		// 传输文件
		else out.writeByte(ContentType.TRANSMIT_FILE);

		byte[] bytes = new MessagePack().write(message);
		out.writeInt(bytes.length);
		out.writeBytes(bytes);
	}

	private void encodeHeart(ByteBuf out, MessagePackHeart message) {
		out.writeByte(Tag.START);
		out.writeByte(ContentType.HEART);
		out.writeByte(Tag.END);
	}

}
