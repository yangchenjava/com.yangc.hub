package com.yangc.bridge.comm.handler;

import io.netty.channel.Channel;
import io.netty.util.CharsetUtil;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.protobuf.ByteString;
import com.yangc.bridge.bean.ResultBean;
import com.yangc.bridge.bean.TBridgeChat;
import com.yangc.bridge.bean.TBridgeFile;
import com.yangc.bridge.comm.Server;
import com.yangc.bridge.comm.protocol.ContentType;
import com.yangc.bridge.comm.protocol.protobuf.ProtobufMessage;
import com.yangc.bridge.comm.protocol.prototype.ProtocolChat;
import com.yangc.bridge.comm.protocol.prototype.ProtocolFile;
import com.yangc.bridge.comm.protocol.prototype.ProtocolHeart;
import com.yangc.bridge.comm.protocol.prototype.ProtocolResult;

public class SendHandler implements Runnable {

	private SendHandler() {
	}

	public static void sendHeart(Channel channel) {
		if (channel != null && channel.isActive()) {
			if (StringUtils.equals(Server.CODEC, "prototype")) {
				channel.writeAndFlush(new ProtocolHeart());
			} else {
				channel.writeAndFlush(ProtobufMessage.Heart.newBuilder().build());
			}
		}
	}

	public static void sendResult(Channel channel, ResultBean result) throws Exception {
		if (channel != null && channel.isActive()) {
			if (StringUtils.equals(Server.CODEC, "prototype")) {
				byte[] data = result.getData().getBytes(CharsetUtil.UTF_8);

				ProtocolResult protocol = new ProtocolResult();
				protocol.setContentType(ContentType.RESULT);
				protocol.setUuid(result.getUuid().getBytes(CharsetUtil.UTF_8));
				protocol.setDataLength(1 + data.length);
				protocol.setSuccess((byte) (result.isSuccess() ? 1 : 0));
				protocol.setData(data);

				channel.writeAndFlush(protocol);
			} else {
				ProtobufMessage.Result.Builder builder = ProtobufMessage.Result.newBuilder();
				builder.setUuid(result.getUuid());
				builder.setSuccess(result.isSuccess());
				builder.setData(result.getData());

				channel.writeAndFlush(builder.build());
			}
		}
	}

	public static void sendChat(Channel channel, TBridgeChat chat) throws Exception {
		if (channel != null && channel.isActive()) {
			if (StringUtils.equals(Server.CODEC, "prototype")) {
				byte[] from = chat.getFrom().getBytes(CharsetUtil.UTF_8);
				byte[] to = chat.getTo().getBytes(CharsetUtil.UTF_8);
				byte[] data = chat.getData().getBytes(CharsetUtil.UTF_8);

				ProtocolChat protocol = new ProtocolChat();
				protocol.setContentType(ContentType.CHAT);
				protocol.setUuid(chat.getUuid().getBytes(CharsetUtil.UTF_8));
				protocol.setFromLength((short) from.length);
				protocol.setToLength((short) to.length);
				protocol.setDataLength(data.length);
				protocol.setFrom(from);
				protocol.setTo(to);
				protocol.setData(data);

				channel.writeAndFlush(protocol);
			} else {
				ProtobufMessage.Chat.Builder builder = ProtobufMessage.Chat.newBuilder();
				builder.setUuid(chat.getUuid());
				builder.setFrom(chat.getFrom());
				builder.setTo(chat.getTo());
				builder.setData(chat.getData());

				channel.writeAndFlush(builder.build());
			}
		}
	}

	public static void sendReadyFile(Channel channel, TBridgeFile file) throws Exception {
		if (channel != null && channel.isActive()) {
			if (StringUtils.equals(Server.CODEC, "prototype")) {
				byte[] from = file.getFrom().getBytes(CharsetUtil.UTF_8);
				byte[] to = file.getTo().getBytes(CharsetUtil.UTF_8);
				byte[] fileName = file.getFileName().getBytes(CharsetUtil.UTF_8);

				ProtocolFile protocol = new ProtocolFile();
				protocol.setContentType(ContentType.READY_FILE);
				protocol.setUuid(file.getUuid().getBytes(CharsetUtil.UTF_8));
				protocol.setFromLength((short) from.length);
				protocol.setToLength((short) to.length);
				protocol.setFrom(from);
				protocol.setTo(to);
				protocol.setFileNameLength((short) fileName.length);
				protocol.setFileName(fileName);
				protocol.setFileSize(file.getFileSize());

				channel.writeAndFlush(protocol);
			} else {
				ProtobufMessage.File.Builder builder = ProtobufMessage.File.newBuilder();
				builder.setUuid(file.getUuid());
				builder.setFrom(file.getFrom());
				builder.setTo(file.getTo());
				builder.setFileName(file.getFileName());
				builder.setFileSize(file.getFileSize());

				channel.writeAndFlush(builder.build());
			}
		}
	}

	public static void sendTransmitFile(Channel channel, TBridgeFile file) throws Exception {
		if (channel != null && channel.isActive()) {
			if (StringUtils.equals(Server.CODEC, "prototype")) {
				byte[] from = file.getFrom().getBytes(CharsetUtil.UTF_8);
				byte[] to = file.getTo().getBytes(CharsetUtil.UTF_8);
				byte[] fileName = file.getFileName().getBytes(CharsetUtil.UTF_8);

				ProtocolFile protocol = new ProtocolFile();
				protocol.setContentType(ContentType.TRANSMIT_FILE);
				protocol.setUuid(file.getUuid().getBytes(CharsetUtil.UTF_8));
				protocol.setFromLength((short) from.length);
				protocol.setToLength((short) to.length);
				protocol.setDataLength(fileName.length + 46 + file.getData().length);
				protocol.setFrom(from);
				protocol.setTo(to);
				protocol.setFileNameLength((short) fileName.length);
				protocol.setFileName(fileName);
				protocol.setFileSize(file.getFileSize());
				protocol.setFileMd5(file.getFileMd5().getBytes(CharsetUtil.UTF_8));
				protocol.setOffset(file.getOffset());
				protocol.setData(file.getData());

				channel.writeAndFlush(protocol);
			} else {
				ProtobufMessage.File.Builder builder = ProtobufMessage.File.newBuilder();
				builder.setUuid(file.getUuid());
				builder.setFrom(file.getFrom());
				builder.setTo(file.getTo());
				builder.setFileName(file.getFileName());
				builder.setFileSize(file.getFileSize());
				builder.setFileMd5(file.getFileMd5());
				builder.setOffset(file.getOffset());
				builder.setData(ByteString.copyFrom(file.getData()));

				channel.writeAndFlush(builder.build());
			}
		}
	}

	public static void sendFile(Channel channel, TBridgeFile file) {
		if (file.getFileSize() < 128 * 1024) {
			sendUnreadFile(channel, file, 8 * 1024);
		} else {
			new Thread(new SendHandler(channel, file, 512 * 1024)).start();
		}
	}

	private static void sendUnreadFile(Channel channel, TBridgeFile file, int bufferedLength) {
		if (channel != null && channel.isActive()) {
			File unreadFile = new File(FileUtils.getTempDirectoryPath() + "/com.yangc.bridge/" + file.getTo() + "/" + file.getUuid());
			BufferedInputStream bis = null;
			try {
				int offset = -1;
				byte[] data = new byte[bufferedLength];
				if (StringUtils.equals(Server.CODEC, "prototype")) {
					byte[] from = file.getFrom().getBytes(CharsetUtil.UTF_8);
					byte[] to = file.getTo().getBytes(CharsetUtil.UTF_8);
					byte[] fileName = file.getFileName().getBytes(CharsetUtil.UTF_8);

					ProtocolFile protocol = new ProtocolFile();
					protocol.setContentType(ContentType.TRANSMIT_FILE);
					protocol.setUuid(file.getUuid().getBytes(CharsetUtil.UTF_8));
					protocol.setFromLength((short) from.length);
					protocol.setToLength((short) to.length);
					protocol.setDataLength(fileName.length + 46 + data.length);
					protocol.setFrom(from);
					protocol.setTo(to);
					protocol.setFileNameLength((short) fileName.length);
					protocol.setFileName(fileName);
					protocol.setFileSize(file.getFileSize());
					protocol.setFileMd5(file.getFileMd5().getBytes(CharsetUtil.UTF_8));

					bis = new BufferedInputStream(new FileInputStream(unreadFile));
					while ((offset = bis.read(data)) != -1) {
						protocol.setOffset(offset);
						protocol.setData(data);
						channel.writeAndFlush(protocol);
					}
				} else {
					ProtobufMessage.File.Builder builder = ProtobufMessage.File.newBuilder();
					builder.setUuid(file.getUuid());
					builder.setFrom(file.getFrom());
					builder.setTo(file.getTo());
					builder.setFileName(file.getFileName());
					builder.setFileSize(file.getFileSize());
					builder.setFileMd5(file.getFileMd5());

					bis = new BufferedInputStream(new FileInputStream(unreadFile));
					while ((offset = bis.read(data)) != -1) {
						builder.setOffset(offset);
						builder.setData(ByteString.copyFrom(data));
						channel.writeAndFlush(builder.build());
					}
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					if (bis != null) bis.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private Channel channel;
	private TBridgeFile file;
	private int bufferedLength;

	private SendHandler(Channel channel, TBridgeFile file, int bufferedLength) {
		this.channel = channel;
		this.file = file;
		this.bufferedLength = bufferedLength;
	}

	@Override
	public void run() {
		sendUnreadFile(channel, file, bufferedLength);
	}

}
