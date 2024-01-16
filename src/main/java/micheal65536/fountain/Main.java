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
import org.cloudburstmc.protocol.bedrock.data.definitions.BlockDefinition;
import org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition;
import org.cloudburstmc.protocol.bedrock.netty.initializer.BedrockServerInitializer;
import org.cloudburstmc.protocol.common.Definition;
import org.cloudburstmc.protocol.common.DefinitionRegistry;

import micheal65536.fountain.registry.BedrockBlockPalette;
import micheal65536.fountain.registry.BedrockItemPalette;
import micheal65536.fountain.registry.EarthItemCatalog;
import micheal65536.fountain.registry.ItemMappings;
import micheal65536.fountain.registry.JavaBlockTranslator;
import micheal65536.fountain.registry.JavaItemPalette;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.function.IntFunction;

public class Main
{
	public static void main(String[] args)
	{
		Configurator.setRootLevel(Level.DEBUG);
		InternalLoggerFactory.setDefaultFactory(Log4J2LoggerFactory.INSTANCE);

		BedrockBlockPalette.init();
		JavaBlockTranslator.init();
		BedrockItemPalette.init();
		JavaItemPalette.init();
		ItemMappings.init();
		EarthItemCatalog.init();

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
						session.getPeer().getCodecHelper().setBlockDefinitions(new CachingDefinitionRegistry<>(runtimeId -> new BlockDefinition()
						{
							@Override
							public int getRuntimeId()
							{
								return runtimeId;
							}
						}));
						session.getPeer().getCodecHelper().setItemDefinitions(new CachingDefinitionRegistry<>(runtimeId -> new ItemDefinition()
						{
							// TODO

							@Override
							public boolean isComponentBased()
							{
								return false;
							}

							@Override
							public String getIdentifier()
							{
								String name = BedrockItemPalette.getName(runtimeId);
								return name != null ? name : "unknown";
							}

							@Override
							public int getRuntimeId()
							{
								return runtimeId;
							}
						}));
					}
				})
				.bind(new InetSocketAddress("0.0.0.0", 19132))
				.syncUninterruptibly();
	}

	private static final class CachingDefinitionRegistry<T extends Definition> implements DefinitionRegistry<T>
	{
		private final HashMap<Integer, T> cache = new HashMap<>();

		private final IntFunction<T> definitionCreator;

		public CachingDefinitionRegistry(IntFunction<T> definitionCreator)
		{
			this.definitionCreator = definitionCreator;
		}

		@Override
		public T getDefinition(int runtimeId)
		{
			T definition = this.cache.getOrDefault(runtimeId, null);
			if (definition == null)
			{
				definition = this.definitionCreator.apply(runtimeId);
				this.cache.put(runtimeId, definition);
			}
			return definition;
		}

		@Override
		public boolean isRegistered(T definition)
		{
			// TODO
			return true;
		}
	}
}