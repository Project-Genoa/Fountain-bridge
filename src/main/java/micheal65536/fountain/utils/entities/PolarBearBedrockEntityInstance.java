package micheal65536.fountain.utils.entities;

import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class PolarBearBedrockEntityInstance extends AgeableBedrockEntityInstance
{
	public boolean attacking = false;

	@Override
	protected void getEntityFlags(@NotNull Set<EntityFlag> entityFlags)
	{
		super.getEntityFlags(entityFlags);
		if (this.attacking)
		{
			entityFlags.add(EntityFlag.STANDING);
		}
	}
}