package com.yangc.bridge.comm.codec.protobuf;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import com.yangc.bridge.comm.protocol.ContentType;
import com.yangc.bridge.comm.protocol.Tag;
import com.yangc.bridge.comm.protocol.protobuf.ProtobufMessage;

public class ProtobufEncoderData extends MessageToByteEncoder<Object> {

	@Override
	protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
		if (msg instanceof ProtobufMessage.Result) {
			this.encodeResult(out, (ProtobufMessage.Result) msg);
		} else if (msg instanceof ProtobufMessage.Login) {
			this.encodeLogin(out, (ProtobufMessage.Login) msg);
		} else if (msg instanceof ProtobufMessage.Chat) {
			this.encodeChat(out, (ProtobufMessage.Chat) msg);
		} else if (msg instanceof ProtobufMessage.File) {
			this.encodeFile(out, (ProtobufMessage.File) msg);
		} else if (msg instanceof ProtobufMessage.Heart) {
			this.encodeHeart(out, (ProtobufMessage.Heart) msg);
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

	private void encodeResult(ByteBuf out, ProtobufMessage.Result message) {
		out.writeByte(Tag.START);
		out.writeByte(ContentType.RESULT);
		out.writeInt(message.getSerializedSize());
		out.writeBytes(message.toByteArray());
	}

	private void encodeLogin(ByteBuf out, ProtobufMessage.Login message) {
		out.writeByte(Tag.START);
		out.writeByte(ContentType.LOGIN);
		out.writeInt(message.getSerializedSize());
		out.writeBytes(message.toByteArray());
	}

	private void encodeChat(ByteBuf out, ProtobufMessage.Chat message) {
		out.writeByte(Tag.START);
		out.writeByte(ContentType.CHAT);
		out.writeInt(message.getSerializedSize());
		out.writeBytes(message.toByteArray());
	}

	private void encodeFile(ByteBuf out, ProtobufMessage.File message) {
		out.writeByte(Tag.START);

		// 准备发送文件
		if (!message.hasData()) out.writeByte(ContentType.READY_FILE);
		// 传输文件
		else out.writeByte(ContentType.TRANSMIT_FILE);

		out.writeInt(message.getSerializedSize());
		out.writeBytes(message.toByteArray());
	}

	private void encodeHeart(ByteBuf out, ProtobufMessage.Heart message) {
		out.writeByte(Tag.START);
		out.writeByte(ContentType.HEART);
		out.writeByte(Tag.END);
	}

}
