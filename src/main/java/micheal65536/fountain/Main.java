package micheal65536.fountain;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Log4J2LoggerFactory;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.cloudburstmc.netty.channel.raknet.RakChannelFactory;
import org.cloudburstmc.protocol.bedrock.BedrockServerSession;
import org.cloudburstmc.protocol.bedrock.codec.v425_genoa.Bedrock_v425_Genoa;
import org.cloudburstmc.protocol.bedrock.netty.initializer.BedrockServerInitializer;

import micheal65536.fountain.palette.BedrockBlockPalette;
import micheal65536.fountain.palette.JavaBlockTranslator;

import java.net.InetSocketAddress;

public class Main
{
	public static void main(String[] args)
	{
		Configurator.setRootLevel(Level.DEBUG);
		InternalLoggerFactory.setDefaultFactory(Log4J2LoggerFactory.INSTANCE);

		BedrockBlockPalette.init();
		JavaBlockTranslator.init();

		new ServerBootstrap()
				.channelFactory(RakChannelFactory.server(NioDatagramChannel.class))
				.group(new NioEventLoopGroup())
				.childHandler(new BedrockServerInitializer()
				{
					@Override
					protected void initSession(BedrockServerSession session)
					{
						session.setCodec(Bedrock_v425_Genoa.CODEC);
						session.setPacketHandler(new ClientPacketHandler(session));
					}
				})
				.bind(new InetSocketAddress("0.0.0.0", 19132))
				.syncUninterruptibly();
	}
}