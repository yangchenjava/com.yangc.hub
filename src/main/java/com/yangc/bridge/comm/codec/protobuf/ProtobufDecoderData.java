package com.yangc.bridge.comm.codec.protobuf;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

import com.yangc.bridge.bean.ResultBean;
import com.yangc.bridge.bean.TBridgeChat;
import com.yangc.bridge.bean.TBridgeFile;
import com.yangc.bridge.bean.UserBean;
import com.yangc.bridge.comm.protocol.ContentType;
import com.yangc.bridge.comm.protocol.Tag;
import com.yangc.bridge.comm.protocol.protobuf.ProtobufMessage;

public class ProtobufDecoderData extends ByteToMessageDecoder {

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
		if (in.readableBytes() >= 2) {
			in.markReaderIndex();
			int position = in.readerIndex();
			if (in.readByte() == Tag.START) {
				switch (in.readByte()) {
				case ContentType.HEART:
					if (this.decodeHeart(position, in, out)) return;
					break;
				case ContentType.RESULT:
					if (this.decodeResult(position, in, out)) return;
					break;
				case ContentType.LOGIN:
					if (this.decodeLogin(position, in, out)) return;
					break;
				case ContentType.CHAT:
					if (this.decodeChat(position, in, out)) return;
					break;
				case ContentType.READY_FILE:
					if (this.decodeReadyFile(position, in, out)) return;
					break;
				case ContentType.TRANSMIT_FILE:
					if (this.decodeTransmitFile(position, in, out)) return;
					break;
				}
			}
			in.resetReaderIndex();
		}
	}

	private boolean decodeHeart(int position, ByteBuf in, List<Object> out) {
		if (in.readableBytes() >= 3) {
			if (in.readByte() == Tag.END) {
				byte crc = 0;
				byte[] b = new byte[3];
				in.getBytes(position, b);
				for (int i = 0; i < b.length; i++) {
					crc += b[i];
				}
				if (in.readByte() == crc && in.readByte() == Tag.FINAL) {
					out.add(ContentType.HEART);
					return true;
				}
			}
		}
		return false;
	}

	private boolean decodeResult(int position, ByteBuf in, List<Object> out) throws Exception {
		if (in.readableBytes() >= 4) {
			int serializedSize = in.readInt();
			if (in.readableBytes() >= serializedSize + 2) {
				byte[] data = new byte[serializedSize];
				in.readBytes(data);
				ProtobufMessage.Result message = ProtobufMessage.Result.parseFrom(data);

				byte crc = 0;
				byte[] b = new byte[6 + serializedSize];
				in.getBytes(position, b);
				for (int i = 0; i < b.length; i++) {
					crc += b[i];
				}
				if (in.readByte() == crc && in.readByte() == Tag.FINAL) {
					ResultBean result = new ResultBean();
					result.setUuid(message.getUuid());
					result.setSuccess(message.getSuccess());
					result.setData(message.getData());
					out.add(result);
					return true;
				}
			}
		}
		return false;
	}

	private boolean decodeLogin(int position, ByteBuf in, List<Object> out) throws Exception {
		if (in.readableBytes() >= 4) {
			int serializedSize = in.readInt();
			if (in.readableBytes() >= serializedSize + 2) {
				byte[] data = new byte[serializedSize];
				in.readBytes(data);
				ProtobufMessage.Login message = ProtobufMessage.Login.parseFrom(data);

				byte crc = 0;
				byte[] b = new byte[6 + serializedSize];
				in.getBytes(position, b);
				for (int i = 0; i < b.length; i++) {
					crc += b[i];
				}
				if (in.readByte() == crc && in.readByte() == Tag.FINAL) {
					UserBean user = new UserBean();
					user.setUuid(message.getUuid());
					user.setUsername(message.getUsername());
					user.setPassword(message.getPassword());
					out.add(user);
					return true;
				}
			}
		}
		return false;
	}

	private boolean decodeChat(int position, ByteBuf in, List<Object> out) throws Exception {
		if (in.readableBytes() >= 4) {
			int serializedSize = in.readInt();
			if (in.readableBytes() >= serializedSize + 2) {
				byte[] data = new byte[serializedSize];
				in.readBytes(data);
				ProtobufMessage.Chat message = ProtobufMessage.Chat.parseFrom(data);

				byte crc = 0;
				byte[] b = new byte[6 + serializedSize];
				in.getBytes(position, b);
				for (int i = 0; i < b.length; i++) {
					crc += b[i];
				}
				if (in.readByte() == crc && in.readByte() == Tag.FINAL) {
					TBridgeChat chat = new TBridgeChat();
					chat.setUuid(message.getUuid());
					chat.setFrom(message.getFrom());
					chat.setTo(message.getTo());
					chat.setData(message.getData());
					out.add(chat);
					return true;
				}
			}
		}
		return false;
	}

	private boolean decodeReadyFile(int position, ByteBuf in, List<Object> out) throws Exception {
		if (in.readableBytes() >= 4) {
			int serializedSize = in.readInt();
			if (in.readableBytes() >= serializedSize + 2) {
				byte[] data = new byte[serializedSize];
				in.readBytes(data);
				ProtobufMessage.File message = ProtobufMessage.File.parseFrom(data);

				byte crc = 0;
				byte[] b = new byte[6 + serializedSize];
				in.getBytes(position, b);
				for (int i = 0; i < b.length; i++) {
					crc += b[i];
				}
				if (in.readByte() == crc && in.readByte() == Tag.FINAL) {
					TBridgeFile file = new TBridgeFile();
					file.setContentType(ContentType.READY_FILE);
					file.setUuid(message.getUuid());
					file.setFrom(message.getFrom());
					file.setTo(message.getTo());
					file.setFileName(message.getFileName());
					file.setFileSize(message.getFileSize());
					out.add(file);
					return true;
				}
			}
		}
		return false;
	}

	private boolean decodeTransmitFile(int position, ByteBuf in, List<Object> out) throws Exception {
		if (in.readableBytes() >= 4) {
			int serializedSize = in.readInt();
			if (in.readableBytes() >= serializedSize + 2) {
				byte[] data = new byte[serializedSize];
				in.readBytes(data);
				ProtobufMessage.File message = ProtobufMessage.File.parseFrom(data);

				byte crc = 0;
				byte[] b = new byte[6 + serializedSize];
				in.getBytes(position, b);
				for (int i = 0; i < b.length; i++) {
					crc += b[i];
				}
				if (in.readByte() == crc && in.readByte() == Tag.FINAL) {
					TBridgeFile file = new TBridgeFile();
					file.setContentType(ContentType.TRANSMIT_FILE);
					file.setUuid(message.getUuid());
					file.setFrom(message.getFrom());
					file.setTo(message.getTo());
					file.setFileName(message.getFileName());
					file.setFileSize(message.getFileSize());
					file.setFileMd5(message.getFileMd5());
					file.setOffset(message.getOffset());
					file.setData(message.getData().toByteArray());
					out.add(file);
					return true;
				}
			}
		}
		return false;
	}

}
