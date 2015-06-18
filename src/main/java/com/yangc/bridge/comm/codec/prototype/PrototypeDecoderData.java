package com.yangc.bridge.comm.codec.prototype;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.CharsetUtil;

import java.util.List;

import com.yangc.bridge.bean.ResultBean;
import com.yangc.bridge.bean.TBridgeChat;
import com.yangc.bridge.bean.TBridgeFile;
import com.yangc.bridge.bean.UserBean;
import com.yangc.bridge.comm.protocol.ContentType;
import com.yangc.bridge.comm.protocol.Tag;

public class PrototypeDecoderData extends ByteToMessageDecoder {

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

	private boolean decodeResult(int position, ByteBuf in, List<Object> out) {
		if (in.readableBytes() >= 40) {
			byte[] uuid = new byte[36];
			in.readBytes(uuid);
			int dataLength = in.readInt();
			if (in.readableBytes() >= 1 + dataLength + 2) {
				if (in.readByte() == Tag.END) {
					byte success = in.readByte();
					byte[] data = new byte[dataLength - 1];
					in.readBytes(data);

					byte crc = 0;
					byte[] b = new byte[43 + dataLength];
					in.getBytes(position, b);
					for (int i = 0; i < b.length; i++) {
						crc += b[i];
					}
					if (in.readByte() == crc && in.readByte() == Tag.FINAL) {
						ResultBean result = new ResultBean();
						result.setUuid(new String(uuid, CharsetUtil.UTF_8));
						result.setSuccess(success == 0 ? false : true);
						result.setData(new String(data, CharsetUtil.UTF_8));
						out.add(result);
						return true;
					}
				}
			}
		}
		return false;
	}

	private boolean decodeLogin(int position, ByteBuf in, List<Object> out) {
		if (in.readableBytes() >= 41) {
			byte[] uuid = new byte[36];
			in.readBytes(uuid);
			if (in.readByte() == Tag.END) {
				short usernameLength = in.readShort();
				short passwordLength = in.readShort();
				if (in.readableBytes() >= usernameLength + passwordLength + 2) {
					byte[] username = new byte[usernameLength];
					byte[] password = new byte[passwordLength];
					in.readBytes(username);
					in.readBytes(password);

					byte crc = 0;
					byte[] b = new byte[43 + usernameLength + passwordLength];
					in.getBytes(position, b);
					for (int i = 0; i < b.length; i++) {
						crc += b[i];
					}
					if (in.readByte() == crc && in.readByte() == Tag.FINAL) {
						UserBean user = new UserBean();
						user.setUuid(new String(uuid, CharsetUtil.UTF_8));
						user.setUsername(new String(username, CharsetUtil.UTF_8));
						user.setPassword(new String(password, CharsetUtil.UTF_8));
						out.add(user);
						return true;
					}
				}
			}
		}
		return false;
	}

	private boolean decodeChat(int position, ByteBuf in, List<Object> out) {
		if (in.readableBytes() >= 44) {
			byte[] uuid = new byte[36];
			in.readBytes(uuid);
			short fromLength = in.readShort();
			short toLength = in.readShort();
			int dataLength = in.readInt();
			if (in.readableBytes() >= fromLength + toLength + 1 + dataLength + 2) {
				byte[] from = new byte[fromLength];
				byte[] to = new byte[toLength];
				in.readBytes(from);
				in.readBytes(to);
				if (in.readByte() == Tag.END) {
					byte[] data = new byte[dataLength];
					in.readBytes(data);

					byte crc = 0;
					byte[] b = new byte[46 + fromLength + toLength + 1 + dataLength];
					in.getBytes(position, b);
					for (int i = 0; i < b.length; i++) {
						crc += b[i];
					}
					if (in.readByte() == crc && in.readByte() == Tag.FINAL) {
						TBridgeChat chat = new TBridgeChat();
						chat.setUuid(new String(uuid, CharsetUtil.UTF_8));
						chat.setFrom(new String(from, CharsetUtil.UTF_8));
						chat.setTo(new String(to, CharsetUtil.UTF_8));
						chat.setData(new String(data, CharsetUtil.UTF_8));
						out.add(chat);
						return true;
					}
				}
			}
		}
		return false;
	}

	private boolean decodeReadyFile(int position, ByteBuf in, List<Object> out) {
		if (in.readableBytes() >= 40) {
			byte[] uuid = new byte[36];
			in.readBytes(uuid);
			short fromLength = in.readShort();
			short toLength = in.readShort();
			if (in.readableBytes() >= fromLength + toLength + 3) {
				byte[] from = new byte[fromLength];
				byte[] to = new byte[toLength];
				in.readBytes(from);
				in.readBytes(to);
				if (in.readByte() == Tag.END) {
					short fileNameLength = in.readShort();
					if (in.readableBytes() >= fileNameLength + 10) {
						byte[] fileName = new byte[fileNameLength];
						in.readBytes(fileName);
						long fileSize = in.readLong();

						byte crc = 0;
						byte[] b = new byte[42 + fromLength + toLength + 3 + fileNameLength + 8];
						in.getBytes(position, b);
						for (int i = 0; i < b.length; i++) {
							crc += b[i];
						}
						if (in.readByte() == crc && in.readByte() == Tag.FINAL) {
							TBridgeFile file = new TBridgeFile();
							file.setContentType(ContentType.READY_FILE);
							file.setUuid(new String(uuid, CharsetUtil.UTF_8));
							file.setFrom(new String(from, CharsetUtil.UTF_8));
							file.setTo(new String(to, CharsetUtil.UTF_8));
							file.setFileName(new String(fileName, CharsetUtil.UTF_8));
							file.setFileSize(fileSize);
							out.add(file);
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	private boolean decodeTransmitFile(int position, ByteBuf in, List<Object> out) {
		if (in.readableBytes() >= 44) {
			byte[] uuid = new byte[36];
			in.readBytes(uuid);
			short fromLength = in.readShort();
			short toLength = in.readShort();
			int dataLength = in.readInt();
			if (in.readableBytes() >= fromLength + toLength + 1 + dataLength + 2) {
				byte[] from = new byte[fromLength];
				byte[] to = new byte[toLength];
				in.readBytes(from);
				in.readBytes(to);
				if (in.readByte() == Tag.END) {
					short fileNameLength = in.readShort();
					byte[] fileName = new byte[fileNameLength];
					in.readBytes(fileName);
					long fileSize = in.readLong();
					byte[] fileMd5 = new byte[32];
					in.readBytes(fileMd5);
					int offset = in.readInt();
					byte[] data = new byte[dataLength - fileNameLength - 46];
					in.readBytes(data);

					byte crc = 0;
					byte[] b = new byte[46 + fromLength + toLength + 1 + dataLength];
					in.getBytes(position, b);
					for (int i = 0; i < b.length; i++) {
						crc += b[i];
					}
					if (in.readByte() == crc && in.readByte() == Tag.FINAL) {
						TBridgeFile file = new TBridgeFile();
						file.setContentType(ContentType.TRANSMIT_FILE);
						file.setUuid(new String(uuid, CharsetUtil.UTF_8));
						file.setFrom(new String(from, CharsetUtil.UTF_8));
						file.setTo(new String(to, CharsetUtil.UTF_8));
						file.setFileName(new String(fileName, CharsetUtil.UTF_8));
						file.setFileSize(fileSize);
						file.setFileMd5(new String(fileMd5, CharsetUtil.UTF_8));
						file.setOffset(offset);
						file.setData(data);
						out.add(file);
						return true;
					}
				}
			}
		}
		return false;
	}

}
