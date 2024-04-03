package micheal65536.fountain;

import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.packetlib.tcp.TcpClientSession;
import org.apache.logging.log4j.LogManager;
import org.cloudburstmc.protocol.bedrock.BedrockServerSession;
import org.cloudburstmc.protocol.bedrock.codec.v425_genoa.Bedrock_v425_Genoa;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacketHandler;
import org.cloudburstmc.protocol.bedrock.packet.LoginPacket;
import org.cloudburstmc.protocol.common.PacketSignal;
import org.jetbrains.annotations.NotNull;

import micheal65536.fountain.connector.PlayerConnectorPluginWrapper;
import micheal65536.fountain.connector.plugin.ConnectorPlugin;
import micheal65536.fountain.connector.plugin.Inventory;
import micheal65536.fountain.connector.plugin.PlayerLoginInfo;
import micheal65536.fountain.utils.LoginUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.ReentrantLock;

public class SessionsManager
{
	private final String serverAddress;
	private final int serverPort;
	private final ConnectorPlugin connectorPlugin;

	private final ReentrantLock lock = new ReentrantLock(true);

	private final HashSet<LoginBedrockPacketHandler> pendingSessions = new HashSet<>();
	private final HashMap<String, PlayerSession> activeSessions = new HashMap<>();

	public SessionsManager(@NotNull String serverAddress, int serverPort, @NotNull ConnectorPlugin connectorPlugin)
	{
		this.serverAddress = serverAddress;
		this.serverPort = serverPort;
		this.connectorPlugin = connectorPlugin;
	}

	public void newClientConnection(@NotNull BedrockServerSession bedrockServerSession)
	{
		this.lock.lock();

		LogManager.getLogger().info("New connection from {}", bedrockServerSession.getPeer().getSocketAddress());

		bedrockServerSession.setCodec(Bedrock_v425_Genoa.CODEC);
		bedrockServerSession.getPeer().getCodecHelper().setBlockDefinitions(Main.BLOCK_DEFINITION_REGISTRY);
		bedrockServerSession.getPeer().getCodecHelper().setItemDefinitions(Main.ITEM_DEFINITION_REGISTRY);

		LoginBedrockPacketHandler loginBedrockPacketHandler = new LoginBedrockPacketHandler(bedrockServerSession);
		this.pendingSessions.add(loginBedrockPacketHandler);
		bedrockServerSession.setPacketHandler(loginBedrockPacketHandler);

		this.lock.unlock();
	}

	private void handleLogin(@NotNull LoginBedrockPacketHandler loginBedrockPacketHandler, @NotNull LoginPacket loginPacket)
	{
		this.lock.lock();

		LoginUtils.LoginInfo loginInfo = LoginUtils.getLoginInfo(loginPacket);
		if (loginInfo == null)
		{
			LogManager.getLogger().warn("Could not get login info from login packet");
			this.disconnectPending(loginBedrockPacketHandler);
			this.lock.unlock();
			return;
		}
		if (this.activeSessions.containsKey(loginInfo.uuid))
		{
			LogManager.getLogger().warn("Multiple player logins with the same UUID {} {}", loginInfo.username, loginInfo.uuid);
			this.disconnectPending(loginBedrockPacketHandler);
			this.lock.unlock();
			return;
		}
		Inventory initialInventory;
		try
		{
			initialInventory = this.connectorPlugin.onPlayerConnected(new PlayerLoginInfo(loginInfo.uuid));
			if (initialInventory == null)
			{
				LogManager.getLogger().warn("Connector plugin rejected player login for {} {}", loginInfo.username, loginInfo.uuid);
				this.disconnectPending(loginBedrockPacketHandler);
				this.lock.unlock();
				return;
			}
		}
		catch (ConnectorPlugin.ConnectorPluginException exception)
		{
			LogManager.getLogger().warn("Connector plugin threw exception when handling player login for {} {}", loginInfo.username, loginInfo.uuid, exception);
			this.disconnectPending(loginBedrockPacketHandler);
			this.lock.unlock();
			return;
		}
		this.pendingSessions.remove(loginBedrockPacketHandler);
		LogManager.getLogger().info("Player logged in {} {}", loginInfo.username, loginInfo.uuid);

		MinecraftProtocol javaProtocol = new MinecraftProtocol(loginInfo.username);
		TcpClientSession tcpClientSession = new TcpClientSession(this.serverAddress, this.serverPort, javaProtocol);

		PlayerSession playerSession = new PlayerSession(loginBedrockPacketHandler.bedrockServerSession, tcpClientSession, initialInventory, new PlayerConnectorPluginWrapper(this.connectorPlugin, loginInfo.uuid), this::onSessionDisconnected);
		this.activeSessions.put(loginInfo.uuid, playerSession);

		playerSession.mutex.lock();
		loginBedrockPacketHandler.bedrockServerSession.setPacketHandler(new ClientPacketHandler(playerSession));
		tcpClientSession.addListener(new ServerPacketHandler(playerSession));
		tcpClientSession.connect(true);
		playerSession.mutex.unlock();

		this.lock.unlock();
	}

	private void onSessionDisconnected(PlayerSession playerSession)
	{
		this.lock.lock();
		if (this.activeSessions.containsValue(playerSession))
		{
			try
			{
				String uuid = this.activeSessions.entrySet().stream().filter(entry -> entry.getValue() == playerSession).map(Map.Entry::getKey).findAny().orElseThrow();
				if (this.activeSessions.remove(uuid) != playerSession)
				{
					throw new AssertionError();
				}
				LogManager.getLogger().info("Session has disconnected {}", uuid);
			}
			catch (NoSuchElementException noSuchElementException)
			{
				throw new AssertionError(noSuchElementException);
			}
		}
		this.lock.unlock();
	}

	private void disconnectPending(@NotNull LoginBedrockPacketHandler loginBedrockPacketHandler)
	{
		this.lock.lock();
		if (this.pendingSessions.remove(loginBedrockPacketHandler))
		{
			LogManager.getLogger().info("Pending session has disconnected");
			try
			{
				loginBedrockPacketHandler.bedrockServerSession.disconnect();
			}
			catch (IllegalStateException exception)
			{
				// empty
			}
		}
		this.lock.unlock();
	}

	public void shutdown()
	{
		this.lock.lock();

		LogManager.getLogger().info("Shutting down");

		LoginBedrockPacketHandler[] pendingSessions = this.pendingSessions.toArray(new LoginBedrockPacketHandler[0]);
		this.pendingSessions.clear();
		LogManager.getLogger().info("Disconnecting {} remaining pending sessions", pendingSessions.length);
		for (LoginBedrockPacketHandler loginBedrockPacketHandler : pendingSessions)
		{
			loginBedrockPacketHandler.bedrockServerSession.disconnect("", true);
		}

		PlayerSession[] activeSessions = this.activeSessions.values().toArray(new PlayerSession[0]);
		this.activeSessions.clear();
		LogManager.getLogger().info("Disconnecting {} remaining active sessions", activeSessions.length);
		for (PlayerSession playerSession : activeSessions)
		{
			playerSession.mutex.lock();
			playerSession.disconnect(true);
			playerSession.mutex.unlock();
		}

		this.lock.unlock();
	}

	private class LoginBedrockPacketHandler implements BedrockPacketHandler
	{
		private final BedrockServerSession bedrockServerSession;

		public LoginBedrockPacketHandler(BedrockServerSession bedrockServerSession)
		{
			this.bedrockServerSession = bedrockServerSession;
		}

		@Override
		public PacketSignal handlePacket(BedrockPacket packet)
		{
			if (packet instanceof LoginPacket)
			{
				SessionsManager.this.handleLogin(this, (LoginPacket) packet);
			}
			else
			{
				LogManager.getLogger().warn("Received non-login packet during login phase");
				SessionsManager.this.disconnectPending(this);
			}
			return PacketSignal.HANDLED;
		}

		@Override
		public void onDisconnect(String reason)
		{
			LogManager.getLogger().info("Client disconnected during login phase: {}", reason);
			SessionsManager.this.disconnectPending(this);
		}
	}
}