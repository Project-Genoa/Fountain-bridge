package micheal65536.fountain.utils.entities;

import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataType;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class RabbitBedrockEntityInstance extends AgeableBedrockEntityInstance
{
	public int variant = 0;

	private boolean jumping = false;

	public RabbitBedrockEntityInstance()
	{
		super(0.55f, 0.35f);
	}

	public final void sendJump()
	{
		this.jumping = true;
		this.sendData();
		this.jumping = false;
	}

	@Override
	protected void getEntityData(@NotNull Map<EntityDataType<?>, ?> entityData)
	{
		super.getEntityData(entityData);
		putEntityData(entityData, EntityDataTypes.VARIANT, this.variant);    // TODO: doesn't work
		putEntityData(entityData, EntityDataTypes.JUMP_DURATION, this.jumping ? (byte) 3 : (byte) 0);
	}
}