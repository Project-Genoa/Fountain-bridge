package micheal65536.fountain.utils.entities;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.MetadataType;
import org.jetbrains.annotations.NotNull;

public class MinecartJavaEntityInstance<T extends MinecartBedrockEntityInstance> extends BaseJavaEntityInstanceWithSingleBedrockEntityInstance<T>
{
	@NotNull
	private final String bedrockEntityIdentifier;

	public MinecartJavaEntityInstance(@NotNull String bedrockEntityIdentifier, @NotNull T bedrockEntityInstance)
	{
		super(bedrockEntityIdentifier, bedrockEntityInstance);
		this.bedrockEntityIdentifier = bedrockEntityIdentifier;
	}

	@Override
	protected void onAdded()
	{
		this.bedrockEntityInstance.setPos(this.getPos().add(0.0f, 0.35f, 0.0f));
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
	protected void onPositionChanged()
	{
		this.bedrockEntityInstance.setPos(this.getPos().add(0.0f, 0.35f, 0.0f));
		this.bedrockEntityInstance.setOnGround(this.getOnGround());
		this.bedrockEntityInstance.setYaw(this.getYaw());
		this.bedrockEntityInstance.setPitch(this.getPitch());
		this.bedrockEntityInstance.sendPosition();
	}

	@Override
	protected void onMetadataFieldChanged(@NotNull EntityMetadata<?, ? extends MetadataType<?>> metadata)
	{
		super.onMetadataFieldChanged(metadata);
		getMetadataField(metadata, 8, MetadataType.INT, value ->
		{
			this.bedrockEntityInstance.hurtTicks = value;
		});
		getMetadataField(metadata, 9, MetadataType.INT, value ->
		{
			this.bedrockEntityInstance.hurtDirection = value;
		});
		// TODO: block
	}
}