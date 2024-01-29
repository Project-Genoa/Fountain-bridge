package micheal65536.fountain;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Log4J2LoggerFactory;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.Configurator;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.netty.channel.raknet.RakChannelFactory;
import org.cloudburstmc.protocol.bedrock.BedrockClientSession;
import org.cloudburstmc.protocol.bedrock.codec.v425_genoa.Bedrock_v425_Genoa;
import org.cloudburstmc.protocol.bedrock.netty.initializer.BedrockClientInitializer;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacketHandler;
import org.cloudburstmc.protocol.bedrock.packet.LoginPacket;
import org.cloudburstmc.protocol.bedrock.packet.MovePlayerPacket;
import org.cloudburstmc.protocol.bedrock.packet.RequestChunkRadiusPacket;
import org.cloudburstmc.protocol.bedrock.packet.SetLocalPlayerAsInitializedPacket;
import org.cloudburstmc.protocol.bedrock.packet.StartGamePacket;
import org.cloudburstmc.protocol.common.PacketSignal;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class Test2
{
	public static void main(String[] args)
	{
		Configurator.setRootLevel(Level.DEBUG);
		InternalLoggerFactory.setDefaultFactory(Log4J2LoggerFactory.INSTANCE);

		new Bootstrap()
				.channelFactory(RakChannelFactory.client(NioDatagramChannel.class))
				.group(new NioEventLoopGroup())
				.handler(new BedrockClientInitializer()
				{
					@Override
					protected void initSession(BedrockClientSession session)
					{
						session.setCodec(Bedrock_v425_Genoa.CODEC);
						session.setPacketHandler(new BedrockPacketHandler()
						{
							@Override
							public PacketSignal handle(StartGamePacket packet)
							{
								RequestChunkRadiusPacket requestChunkRadiusPacket = new RequestChunkRadiusPacket();
								requestChunkRadiusPacket.setRadius(20);
								requestChunkRadiusPacket.setMaxRadius(20);
								session.sendPacket(requestChunkRadiusPacket);

								MovePlayerPacket movePlayerPacket = new MovePlayerPacket();
								movePlayerPacket.setRuntimeEntityId(packet.getRuntimeEntityId());
								movePlayerPacket.setMode(MovePlayerPacket.Mode.NORMAL);
								movePlayerPacket.setPosition(Vector3f.from(-10.0f, 50.0f, -20.0f));
								movePlayerPacket.setRotation(Vector3f.ZERO);
								movePlayerPacket.setOnGround(false);
								session.sendPacket(movePlayerPacket);

								SetLocalPlayerAsInitializedPacket setLocalPlayerAsInitializedPacket = new SetLocalPlayerAsInitializedPacket();
								setLocalPlayerAsInitializedPacket.setRuntimeEntityId(packet.getRuntimeEntityId());
								session.sendPacket(setLocalPlayerAsInitializedPacket);

								return PacketSignal.HANDLED;
							}

							@Override
							public void onDisconnect(String reason)
							{
								LogManager.getLogger().info("Disconnected: " + reason);
							}
						});

						LoginPacket loginPacket = new LoginPacket();
						loginPacket.setProtocolVersion(Bedrock_v425_Genoa.CODEC.getProtocolVersion());
						try (FileInputStream fileInputStream = new FileInputStream("/tmp/test-1"))
						{
							byte[] data = fileInputStream.readAllBytes();
							loginPacket.getChain().add(new String(data, StandardCharsets.UTF_8));
						}
						catch (IOException exception)
						{
							//
						}
						try (FileInputStream fileInputStream = new FileInputStream("/tmp/test-2"))
						{
							byte[] data = fileInputStream.readAllBytes();
							loginPacket.setExtra(new String(data, StandardCharsets.UTF_8));
						}
						catch (IOException exception)
						{
							//
						}
						session.sendPacket(loginPacket);
					}
				})
				.connect(new InetSocketAddress("127.0.0.1", 19132))
				.syncUninterruptibly();
	}
}