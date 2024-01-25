package micheal65536.fountain.utils.entities;

import com.github.steveice10.mc.protocol.data.game.entity.EntityEvent;
import com.github.steveice10.mc.protocol.data.game.entity.attribute.Attribute;
import com.github.steveice10.mc.protocol.data.game.entity.attribute.AttributeType;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.MetadataType;
import com.github.steveice10.mc.protocol.data.game.entity.player.Animation;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityEventType;
import org.cloudburstmc.protocol.bedrock.packet.AnimatePacket;
import org.jetbrains.annotations.NotNull;

public class LivingJavaEntityInstance<T extends LivingBedrockEntityInstance> extends BaseJavaEntityInstanceWithSingleBedrockEntityInstance<T>
{
	public LivingJavaEntityInstance(@NotNull String bedrockEntityIdentifier, @NotNull T bedrockEntityInstance)
	{
		super(bedrockEntityIdentifier, bedrockEntityInstance);
	}

	@Override
	protected void onMetadataFieldChanged(@NotNull EntityMetadata<?, ? extends MetadataType<?>> metadata)
	{
		super.onMetadataFieldChanged(metadata);
		getMetadataField(metadata, 8, MetadataType.BYTE, value ->
		{
			this.bedrockEntityInstance.usingItem = (value & 0x01) != 0;
		});
		getMetadataField(metadata, 9, MetadataType.FLOAT, value ->
		{
			this.bedrockEntityInstance.health = value;
		});
	}

	@Override
	protected void onAttributeChanged(@NotNull Attribute attribute)
	{
		super.onAttributeChanged(attribute);
		if (attribute.getType() instanceof AttributeType.Builtin)
		{
			switch ((AttributeType.Builtin) attribute.getType())
			{
				case GENERIC_MAX_HEALTH ->
				{
					this.bedrockEntityInstance.maxHealth = (float) getAttributeValueWithModifiers(attribute);
				}
				case GENERIC_MOVEMENT_SPEED, GENERIC_FLYING_SPEED ->
				{
					this.bedrockEntityInstance.movementSpeed = (float) getAttributeValueWithModifiers(attribute);
				}
			}
		}
	}

	@Override
	protected void onHurt()
	{
		super.onHurt();
		this.bedrockEntityInstance.sendEvent(EntityEventType.HURT);
	}

	@Override
	public boolean handleEvent(@NotNull EntityEvent entityEvent)
	{
		switch (entityEvent)
		{
			case LIVING_DEATH ->
			{
				this.bedrockEntityInstance.sendEvent(EntityEventType.DEATH);
				return true;
			}
			case MAKE_POOF_PARTICLES ->
			{
				this.bedrockEntityInstance.sendEvent(EntityEventType.DEATH_SMOKE_CLOUD);
				return true;
			}
			default ->
			{
				return super.handleEvent(entityEvent);
			}
		}
	}

	@Override
	public boolean handleAnimation(@NotNull Animation animation)
	{
		switch (animation)
		{
			case SWING_ARM ->
			{
				this.bedrockEntityInstance.sendAnimation(AnimatePacket.Action.SWING_ARM);
				return true;
			}
			case CRITICAL_HIT ->
			{
				this.bedrockEntityInstance.sendAnimation(AnimatePacket.Action.CRITICAL_HIT);
				return true;
			}
			default ->
			{
				return super.handleAnimation(animation);
			}
		}
	}
}