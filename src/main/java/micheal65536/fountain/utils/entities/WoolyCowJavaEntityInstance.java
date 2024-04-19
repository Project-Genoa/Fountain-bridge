package micheal65536.fountain.utils.entities;

import com.github.steveice10.mc.protocol.data.game.entity.EntityEvent;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.MetadataType;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityEventType;
import org.jetbrains.annotations.NotNull;

// TODO: need sound mappings

public class WoolyCowJavaEntityInstance<T extends WoolyCowBedrockEntityInstance> extends AgeableJavaEntityInstance<T>
{
	public WoolyCowJavaEntityInstance(@NotNull String bedrockEntityIdentifier, @NotNull T bedrockEntityInstance)
	{
		super(bedrockEntityIdentifier, bedrockEntityInstance);
	}

	@Override
	protected void onMetadataFieldChanged(@NotNull EntityMetadata<?, ? extends MetadataType<?>> metadata)
	{
		super.onMetadataFieldChanged(metadata);
		getMetadataField(metadata, 17, MetadataType.BOOLEAN, value ->
		{
			this.bedrockEntityInstance.sheared = value;
		});
	}

	@Override
	public boolean handleEvent(@NotNull EntityEvent entityEvent)
	{
		switch (entityEvent)
		{
			case SHEEP_GRAZE_OR_TNT_CART_EXPLODE ->
			{
				// TODO: does not work (no eating animation is shown)
				this.bedrockEntityInstance.sendEvent(EntityEventType.EAT_GRASS);
				return true;
			}
			default ->
			{
				return super.handleEvent(entityEvent);
			}
		}
	}
}