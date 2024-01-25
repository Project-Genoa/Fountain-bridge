package micheal65536.fountain.utils.entities;

import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class CreeperBedrockEntityInstance extends MobBedrockEntityInstance
{
	public boolean ignited = false;
	public boolean ignitedByFlintAndSteel = false;
	public boolean charged = false;

	@Override
	protected void getEntityFlags(@NotNull Set<EntityFlag> entityFlags)
	{
		super.getEntityFlags(entityFlags);
		if (this.ignited || this.ignitedByFlintAndSteel)
		{
			entityFlags.add(EntityFlag.IGNITED);
		}
		if (this.charged)
		{
			// TODO: doesn't work
			entityFlags.add(EntityFlag.POWERED);
		}
	}
}