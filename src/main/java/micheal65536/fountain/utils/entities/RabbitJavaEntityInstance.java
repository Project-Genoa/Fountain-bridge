package micheal65536.fountain.utils.entities;

import com.github.steveice10.mc.protocol.data.game.entity.EntityEvent;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.MetadataType;
import org.jetbrains.annotations.NotNull;

public class RabbitJavaEntityInstance<T extends RabbitBedrockEntityInstance> extends AgeableJavaEntityInstance<T>
{
	public RabbitJavaEntityInstance(@NotNull String bedrockEntityIdentifier, @NotNull T bedrockEntityInstance)
	{
		super(bedrockEntityIdentifier, bedrockEntityInstance);
	}

	@Override
	protected void onMetadataFieldChanged(@NotNull EntityMetadata<?, ? extends MetadataType<?>> metadata)
	{
		super.onMetadataFieldChanged(metadata);
		getMetadataField(metadata, 17, MetadataType.INT, value ->
		{
			this.bedrockEntityInstance.variant = value;
		});
	}

	@Override
	public boolean handleEvent(@NotNull EntityEvent entityEvent)
	{
		switch (entityEvent)
		{
			case RABBIT_JUMP_OR_MINECART_SPAWNER_DELAY_RESET ->
			{
				this.bedrockEntityInstance.sendJump();
				return true;
			}
			default ->
			{
				return super.handleEvent(entityEvent);
			}
		}
	}
}