package micheal65536.fountain.utils;

import com.github.steveice10.mc.protocol.codec.MinecraftCodecHelper;
import com.github.steveice10.mc.protocol.packet.common.serverbound.ServerboundCustomPayloadPacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.apache.logging.log4j.LogManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import micheal65536.fountain.PlayerSession;
import micheal65536.fountain.registry.JavaBlocks;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.function.BiConsumer;

// Implements the Fabric registry sync protocol, allowing us to receive the runtime IDs for content that is added via the Fabric mod
public class FabricRegistryManager
{
	private final PlayerSession playerSession;
	private final MinecraftCodecHelper minecraftCodecHelper;

	private final HashMap<Integer, String> fabricBlocks = new HashMap<>();
	private final HashMap<Integer, String> fabricItems = new HashMap<>();
	private final HashMap<String, Integer> fabricItemsId = new HashMap<>();
	private final HashMap<Integer, String> fabricEntities = new HashMap<>();

	private ByteBuf registryDataBuf = null;

	public FabricRegistryManager(@NotNull PlayerSession playerSession, @NotNull MinecraftCodecHelper minecraftCodecHelper)
	{
		this.playerSession = playerSession;
		this.minecraftCodecHelper = minecraftCodecHelper;
	}

	public void enableRegistrySyncChannel()
	{
		LogManager.getLogger().debug("Enabling Fabric registry sync");
		this.playerSession.sendJavaPacket(new ServerboundCustomPayloadPacket("minecraft:register", "fabric:registry/sync/direct".getBytes(StandardCharsets.US_ASCII)));
	}

	public boolean handleRegistrySyncData(byte[] data)
	{
		if (data.length == 0)
		{
			int registryGroupsCount = this.minecraftCodecHelper.readVarInt(this.registryDataBuf);
			for (int registryGroupIndex = 0; registryGroupIndex < registryGroupsCount; registryGroupIndex++)
			{
				String registryNamePrefix = this.minecraftCodecHelper.readString(this.registryDataBuf);
				int registryCount = this.minecraftCodecHelper.readVarInt(this.registryDataBuf);
				for (int registryIndex = 0; registryIndex < registryCount; registryIndex++)
				{
					String registryName = this.minecraftCodecHelper.readString(this.registryDataBuf);
					String registryType = (registryNamePrefix.length() > 0 ? registryNamePrefix : "minecraft") + ":" + registryName;
					if (registryType.equals("minecraft:block"))
					{
						// TODO: validate ID/order of vanilla blocks
						int maxVanillaBlockId = JavaBlocks.getMaxVanillaBlockId();
						this.readRegistry((id, name) ->
						{
							if (name.startsWith("minecraft:"))
							{
								return;
							}
							else
							{
								String[] states = JavaBlocks.getStatesForNonVanillaBlock(name);
								if (states == null)
								{
									LogManager.getLogger().warn("Fabric registry contained unrecognised block {}, block state IDs will likely be incorrect", name);
									return;
								}
								for (String state : states)
								{
									this.fabricBlocks.put(maxVanillaBlockId + this.fabricBlocks.size() + 1, name + state);
								}
							}
						});
					}
					else if (registryType.equals("minecraft:item"))
					{
						this.readRegistry((id, name) ->
						{
							if (name.startsWith("minecraft:"))
							{
								return;
							}
							else
							{
								this.fabricItems.put(id, name);
								this.fabricItemsId.put(name, id);
							}
						});
					}
					else if (registryType.equals("minecraft:entity_type"))
					{
						this.readRegistry((id, name) ->
						{
							if (name.startsWith("minecraft:"))
							{
								return;
							}
							else
							{
								this.fabricEntities.put(id, name);
							}
						});
					}
					else
					{
						this.readRegistry((id, name) ->
						{
							// empty
						});
					}
				}
			}

			LogManager.getLogger().debug("Finished Fabric registry sync");
			this.playerSession.sendJavaPacket(new ServerboundCustomPayloadPacket("fabric:registry/sync/complete", new byte[0]));

			return true;
		}
		else
		{
			LogManager.getLogger().debug("Got Fabric registry sync data");

			if (this.registryDataBuf == null)
			{
				this.registryDataBuf = Unpooled.buffer();
			}

			this.registryDataBuf.writeBytes(data);

			return false;
		}
	}

	private void readRegistry(@NotNull BiConsumer<Integer, String> consumer)
	{
		int currentId = 0;
		int namespaceCount = this.minecraftCodecHelper.readVarInt(this.registryDataBuf);
		for (int namespaceIndex = 0; namespaceIndex < namespaceCount; namespaceIndex++)
		{
			String prefix = this.minecraftCodecHelper.readString(this.registryDataBuf);
			int batchCount = this.minecraftCodecHelper.readVarInt(this.registryDataBuf);
			for (int batchIndex = 0; batchIndex < batchCount; batchIndex++)
			{
				currentId += this.minecraftCodecHelper.readVarInt(this.registryDataBuf);
				int entryCount = this.minecraftCodecHelper.readVarInt(this.registryDataBuf);
				for (int entryIndex = 0; entryIndex < entryCount; entryIndex++)
				{
					String name = this.minecraftCodecHelper.readString(this.registryDataBuf);
					consumer.accept(currentId, (prefix.length() > 0 ? prefix : "minecraft") + ":" + name);
					currentId++;
				}
				currentId--;
			}
		}
	}

	@Nullable
	public String getBlockName(int id)
	{
		return this.fabricBlocks.getOrDefault(id, null);
	}

	@Nullable
	public String getItemName(int id)
	{
		return this.fabricItems.getOrDefault(id, null);
	}

	public int getItemId(@NotNull String name)
	{
		return this.fabricItemsId.getOrDefault(name, -1);
	}

	@Nullable
	public String getEntityName(int id)
	{
		return this.fabricEntities.getOrDefault(id, null);
	}
}