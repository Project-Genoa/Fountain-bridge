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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import micheal65536.fountain.connector.ConnectorPluginLogger;
import micheal65536.fountain.connector.DefaultConnectorPlugin;
import micheal65536.fountain.connector.plugin.ConnectorPlugin;
import micheal65536.fountain.mappings.DirectSounds;
import micheal65536.fountain.registry.BedrockBiomes;
import micheal65536.fountain.registry.BedrockBlocks;
import micheal65536.fountain.registry.BedrockItems;
import micheal65536.fountain.registry.EarthItemCatalog;
import micheal65536.fountain.registry.JavaBlocks;
import micheal65536.fountain.registry.JavaItems;

import java.io.File;
import java.lang.reflect.Constructor;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
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
		System.setProperty("log4j.shutdownHookEnabled", "false");
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
				.option("port")
				.hasArg()
				.argName("port")
				.type(Number.class)
				.desc("Port to listen on, defaults to 19132")
				.build());
		options.addOption(Option.builder()
				.option("serverAddress")
				.hasArg()
				.argName("address")
				.desc("Server address to connect to, defaults to 127.0.0.1")
				.build());
		options.addOption(Option.builder()
				.option("serverPort")
				.hasArg()
				.argName("port")
				.type(Number.class)
				.desc("Server port to connect to, defaults to 25565")
				.build());
		options.addOption(Option.builder()
				.option("connectorPluginJar")
				.hasArg()
				.argName("jar")
				.desc("JAR file containing the connector plugin")
				.build());
		options.addOption(Option.builder()
				.option("connectorPluginClass")
				.hasArg()
				.argName("name")
				.desc("Class name of the connector plugin")
				.build());
		options.addOption(Option.builder()
				.option("connectorPluginArg")
				.hasArg()
				.argName("arg")
				.desc("Argument for the connector plugin")
				.build());
		CommandLine commandLine;
		int port;
		String serverAddress;
		int serverPort;
		String connectorPluginJarFilename;
		String connectorPluginClassName;
		String connectorPluginArg;
		try
		{
			commandLine = new DefaultParser().parse(options, args);
			port = commandLine.hasOption("port") ? (int) (long) commandLine.getParsedOptionValue("port") : 19132;
			serverAddress = commandLine.hasOption("serverAddress") ? commandLine.getOptionValue("serverAddress") : "127.0.0.1";
			serverPort = commandLine.hasOption("serverPort") ? (int) (long) commandLine.getParsedOptionValue("serverPort") : 25565;
			connectorPluginJarFilename = commandLine.getOptionValue("connectorPluginJar", null);
			connectorPluginClassName = commandLine.getOptionValue("connectorPluginClass", DefaultConnectorPlugin.class.getCanonicalName());
			connectorPluginArg = commandLine.getOptionValue("connectorPluginArg", "");
		}
		catch (ParseException exception)
		{
			LogManager.getLogger().fatal(exception);
			System.exit(1);
			return;
		}

		ConnectorPlugin connectorPlugin = loadConnectorPlugin(connectorPluginJarFilename, connectorPluginClassName, connectorPluginArg);
		SessionsManager sessionsManager = new SessionsManager(serverAddress, serverPort, connectorPlugin);
		Runtime.getRuntime().addShutdownHook(new Thread(() ->
		{
			sessionsManager.shutdown();

			if (!(connectorPlugin instanceof DefaultConnectorPlugin))
			{
				LogManager.getLogger().info("Shutting down connector plugin");
			}
			try
			{
				connectorPlugin.shutdown();
			}
			catch (ConnectorPlugin.ConnectorPluginException exception)
			{
				LogManager.getLogger().warn("Connector plugin threw exception while shutting down", exception);
			}
		}));
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
				.bind(new InetSocketAddress("0.0.0.0", port))
				.syncUninterruptibly();
	}

	@NotNull
	private static ConnectorPlugin loadConnectorPlugin(@Nullable String jarFilename, @NotNull String className, @NotNull String arg)
	{
		ClassLoader classLoader;
		if (jarFilename != null)
		{
			try
			{
				classLoader = new URLClassLoader(new URL[]{new File(jarFilename).toURI().toURL()}, Main.class.getClassLoader());
			}
			catch (MalformedURLException exception)
			{
				throw new AssertionError(exception);
			}
		}
		else
		{
			classLoader = Main.class.getClassLoader();
		}

		ConnectorPlugin connectorPlugin;
		try
		{
			Class<?> aClass = classLoader.loadClass(className);
			if (!ConnectorPlugin.class.isAssignableFrom(aClass))
			{
				LogManager.getLogger().fatal("Connector plugin class does not implement connector plugin interface");
				System.exit(1);
			}
			Class<ConnectorPlugin> connectorPluginClass = (Class<ConnectorPlugin>) aClass;

			Constructor<ConnectorPlugin> connectorPluginConstructor = connectorPluginClass.getDeclaredConstructor();
			connectorPlugin = connectorPluginConstructor.newInstance();
		}
		catch (NoClassDefFoundError | ClassNotFoundException exception)
		{
			LogManager.getLogger().fatal("Connector plugin class was not found or could not be loaded", exception);
			System.exit(1);
			throw new AssertionError();
		}
		catch (NoSuchMethodException exception)
		{
			LogManager.getLogger().fatal("Connector plugin class does not provide a suitable constructor");
			System.exit(1);
			throw new AssertionError();
		}
		catch (ReflectiveOperationException exception)
		{
			LogManager.getLogger().fatal("Could not create connector plugin instance", exception);
			System.exit(1);
			throw new AssertionError();
		}

		try
		{
			connectorPlugin.init(arg, new ConnectorPluginLogger(LogManager.getLogger("Connector plugin")));
		}
		catch (ConnectorPlugin.ConnectorPluginException exception)
		{
			LogManager.getLogger().error("Connector plugin failed to initialise", exception);
			System.exit(1);
			throw new AssertionError();
		}
		if (!(connectorPlugin instanceof DefaultConnectorPlugin))
		{
			LogManager.getLogger().info("Connector plugin is initialised");
		}

		return connectorPlugin;
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