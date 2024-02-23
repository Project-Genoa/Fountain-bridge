package micheal65536.fountain;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Log4J2LoggerFactory;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.Configurator;
import org.cloudburstmc.netty.channel.raknet.RakChannelFactory;
import org.cloudburstmc.protocol.bedrock.BedrockServerSession;
import org.cloudburstmc.protocol.bedrock.data.definitions.BlockDefinition;
import org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition;
import org.cloudburstmc.protocol.bedrock.netty.initializer.BedrockServerInitializer;
import org.cloudburstmc.protocol.common.Definition;
import org.cloudburstmc.protocol.common.DefinitionRegistry;

import micheal65536.fountain.mappings.DirectSounds;
import micheal65536.fountain.registry.BedrockBiomes;
import micheal65536.fountain.registry.BedrockBlocks;
import micheal65536.fountain.registry.BedrockItems;
import micheal65536.fountain.registry.EarthItemCatalog;
import micheal65536.fountain.registry.JavaBlocks;
import micheal65536.fountain.registry.JavaItems;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Locale;
import java.util.function.IntFunction;

public class Main
{
	public static final DefinitionRegistry<BlockDefinition> BLOCK_DEFINITION_REGISTRY = new CachingDefinitionRegistry<>(runtimeId -> new BlockDefinition()
	{
		@Override
		public int getRuntimeId()
		{
			return runtimeId;
		}
	});
	public static final DefinitionRegistry<ItemDefinition> ITEM_DEFINITION_REGISTRY = new CachingDefinitionRegistry<>(runtimeId -> new ItemDefinition()
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
			String name = BedrockItems.getName(runtimeId);
			return name != null ? name : "unknown";
		}

		@Override
		public int getRuntimeId()
		{
			return runtimeId;
		}
	});

	public static void main(String[] args)
	{
		Configurator.setRootLevel(Level.DEBUG);
		InternalLoggerFactory.setDefaultFactory(Log4J2LoggerFactory.INSTANCE);

		for (Class<?> staticClassToInitialise : new Class[]{
				BedrockBlocks.class,
				JavaBlocks.class,
				BedrockItems.class,
				JavaItems.class,
				EarthItemCatalog.class,
				BedrockBiomes.class,

				DirectSounds.class
		})
		{
			try
			{
				staticClassToInitialise.getConstructor().newInstance();
			}
			catch (ReflectiveOperationException exception)
			{
				throw new AssertionError(exception);
			}
		}

		Options options = new Options();
		options.addOption(Option.builder()
				.option("hostPlayerUUID")
				.hasArg()
				.argName("uuid")
				.desc("UUID of the player who is \"hosting\" the buildplate.")
				.build());
		options.addOption(Option.builder()
				.option("shutdownMode")
				.hasArg()
				.argName("mode")
				.desc("Specifies the shutdown mode of the server. One of HOST, LAST, or NONE.")
				.build());
		CommandLine commandLine;
		try
		{
			commandLine = new DefaultParser().parse(options, args);
		}
		catch (ParseException exception)
		{
			LogManager.getLogger().fatal(exception);
			System.exit(1);
			return;
		}

		String hostPlayerUUID = commandLine.getOptionValue("hostPlayerUUID", "").toLowerCase(Locale.ROOT);
		ShutdownMode shutdownMode;
		try
		{
			shutdownMode = ShutdownMode.valueOf(commandLine.getOptionValue("shutdownMode", "NONE"));
		}
		catch (IllegalArgumentException exception)
		{
			LogManager.getLogger().fatal("Invalid shutdown mode {}", commandLine.getOptionValue("shutdownMode", "NONE"));
			System.exit(1);
			return;
		}

		SessionsManager sessionsManager = new SessionsManager(hostPlayerUUID, shutdownMode);
		new ServerBootstrap()
				.channelFactory(RakChannelFactory.server(NioDatagramChannel.class))
				.group(new NioEventLoopGroup())
				.childHandler(new BedrockServerInitializer()
				{
					@Override
					protected void initSession(BedrockServerSession session)
					{
						sessionsManager.newClientConnection(session);
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