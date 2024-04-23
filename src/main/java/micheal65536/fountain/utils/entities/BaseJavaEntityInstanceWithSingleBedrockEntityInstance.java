package micheal65536.fountain.utils.entities;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.MetadataType;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.Pose;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityLinkData;
import org.cloudburstmc.protocol.bedrock.packet.SetEntityLinkPacket;
import org.jetbrains.annotations.NotNull;

import micheal65536.fountain.utils.EntityManager;

public class BaseJavaEntityInstanceWithSingleBedrockEntityInstance<T extends BaseBedrockEntityInstance> extends EntityManager.JavaEntityInstance
{
	@NotNull
	private final String bedrockEntityIdentifier;
	@NotNull
	protected final T bedrockEntityInstance;

	public BaseJavaEntityInstanceWithSingleBedrockEntityInstance(@NotNull String bedrockEntityIdentifier, @NotNull T bedrockEntityInstance)
	{
		this.bedrockEntityIdentifier = bedrockEntityIdentifier;
		this.bedrockEntityInstance = bedrockEntityInstance;
		this.bedrockEntityInstance.setAttackCallback(bedrockEntityInstance1 ->
		{
			this.sendAttack();
		});
		this.bedrockEntityInstance.setInteractCallback(bedrockEntityInstance1 ->
		{
			this.sendInteract();
		});
	}

	@Override
	protected void onAdded()
	{
		this.bedrockEntityInstance.setPos(this.getPos());
		this.bedrockEntityInstance.setVelocity(this.getVelocity());
		this.bedrockEntityInstance.setOnGround(this.getOnGround());
		this.bedrockEntityInstance.setYaw(this.getYaw());
		this.bedrockEntityInstance.setPitch(this.getPitch());
		this.bedrockEntityInstance.setHeadYaw(this.getHeadYaw());
		this.bedrockEntityInstance.setHandMain(this.getHandMain());
		this.bedrockEntityInstance.setHandSecondary(this.getHandSecondary());
		this.bedrockEntityInstance.setArmorHead(this.getArmorHead());
		this.bedrockEntityInstance.setArmorChest(this.getArmorChest());
		this.bedrockEntityInstance.setArmorLegs(this.getArmorLegs());
		this.bedrockEntityInstance.setArmorFeet(this.getArmorFeet());
		this.addBedrockEntity(this.bedrockEntityIdentifier, this.bedrockEntityInstance);
	}

	@Override
	protected void onRemoved()
	{
		this.bedrockEntityInstance.remove();
	}

	@Override
	protected void onPositionChanged()
	{
		this.bedrockEntityInstance.setPos(this.getPos());
		this.bedrockEntityInstance.setOnGround(this.getOnGround());
		this.bedrockEntityInstance.setYaw(this.getYaw());
		this.bedrockEntityInstance.setPitch(this.getPitch());
		this.bedrockEntityInstance.sendPosition();
	}

	@Override
	protected void onVelocityChanged()
	{
		this.bedrockEntityInstance.setVelocity(this.getVelocity());
		this.bedrockEntityInstance.sendVelocity();
	}

	@Override
	protected void onHeadYawChanged()
	{
		this.bedrockEntityInstance.setHeadYaw(this.getHeadYaw());
		this.bedrockEntityInstance.sendHeadYaw();
	}

	@Override
	protected void onEquipmentChanged()
	{
		this.bedrockEntityInstance.setHandMain(this.getHandMain());
		this.bedrockEntityInstance.setHandSecondary(this.getHandSecondary());
		this.bedrockEntityInstance.setArmorHead(this.getArmorHead());
		this.bedrockEntityInstance.setArmorChest(this.getArmorChest());
		this.bedrockEntityInstance.setArmorLegs(this.getArmorLegs());
		this.bedrockEntityInstance.setArmorFeet(this.getArmorFeet());
		this.bedrockEntityInstance.sendEquipment();
	}

	@Override
	protected void onMetadataFieldChanged(@NotNull EntityMetadata<?, ? extends MetadataType<?>> metadata)
	{
		getMetadataField(metadata, 0, MetadataType.BYTE, value ->
		{
			this.bedrockEntityInstance.burning = (value & 0x01) != 0;
			this.bedrockEntityInstance.gliding = (value & 0x80) != 0;
		});
		getMetadataField(metadata, 1, MetadataType.INT, value ->
		{
			this.bedrockEntityInstance.air = value;
		});
		getMetadataField(metadata, 2, MetadataType.OPTIONAL_CHAT, value ->
		{
			if (value.isPresent())
			{
				Component component = value.get();
				this.bedrockEntityInstance.nametag = PlainTextComponentSerializer.plainText().serialize(component);
			}
			else
			{
				this.bedrockEntityInstance.nametag = null;
			}
		});
		getMetadataField(metadata, 3, MetadataType.BOOLEAN, value ->
		{
			this.bedrockEntityInstance.nametagVisible = value;
		});
		getMetadataField(metadata, 5, MetadataType.BOOLEAN, value ->
		{
			this.bedrockEntityInstance.gravity = value;
		});
		getMetadataField(metadata, 6, MetadataType.POSE, value ->
		{
			this.bedrockEntityInstance.sleeping = value == Pose.SLEEPING;
			this.bedrockEntityInstance.swimming = value == Pose.SWIMMING;
		});
	}

	@Override
	protected void afterMetadataBatchChange()
	{
		this.bedrockEntityInstance.sendData();
	}

	@Override
	protected void afterAttributeBatchChange()
	{
		this.bedrockEntityInstance.sendData();
	}

	@Override
	protected void onMounted(@NotNull EntityManager.JavaEntityInstance mount)
	{
		if (mount instanceof BaseJavaEntityInstanceWithSingleBedrockEntityInstance<?>)
		{
			SetEntityLinkPacket setEntityLinkPacket = new SetEntityLinkPacket();
			setEntityLinkPacket.setEntityLink(new EntityLinkData(((BaseJavaEntityInstanceWithSingleBedrockEntityInstance<?>) mount).bedrockEntityInstance.getInstanceId(), this.bedrockEntityInstance.getInstanceId(), EntityLinkData.Type.RIDER, false, false));
			this.sendBedrockPacket(setEntityLinkPacket);
		}
	}

	@Override
	protected void onUnmounted(@NotNull EntityManager.JavaEntityInstance mount)
	{
		if (mount instanceof BaseJavaEntityInstanceWithSingleBedrockEntityInstance<?>)
		{
			SetEntityLinkPacket setEntityLinkPacket = new SetEntityLinkPacket();
			setEntityLinkPacket.setEntityLink(new EntityLinkData(((BaseJavaEntityInstanceWithSingleBedrockEntityInstance<?>) mount).bedrockEntityInstance.getInstanceId(), this.bedrockEntityInstance.getInstanceId(), EntityLinkData.Type.REMOVE, false, false));
			this.sendBedrockPacket(setEntityLinkPacket);
		}
	}
}