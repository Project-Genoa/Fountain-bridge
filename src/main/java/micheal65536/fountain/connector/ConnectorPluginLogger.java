package micheal65536.fountain.connector;

import org.jetbrains.annotations.NotNull;

import micheal65536.fountain.connector.plugin.Logger;

public final class ConnectorPluginLogger implements Logger
{
	private final org.apache.logging.log4j.Logger logger;

	public ConnectorPluginLogger(@NotNull org.apache.logging.log4j.Logger logger)
	{
		this.logger = logger;
	}

	@Override
	public void error(@NotNull String message)
	{
		this.logger.error(message);
	}

	@Override
	public void error(@NotNull String message, @NotNull Throwable throwable)
	{
		this.logger.error(message, throwable);
	}

	@Override
	public void error(@NotNull Throwable throwable)
	{
		this.logger.error(throwable);
	}

	@Override
	public void warn(@NotNull String message)
	{
		this.logger.warn(message);
	}

	@Override
	public void warn(@NotNull String message, @NotNull Throwable throwable)
	{
		this.logger.warn(message, throwable);
	}

	@Override
	public void warn(@NotNull Throwable throwable)
	{
		this.logger.warn(throwable);
	}

	@Override
	public void info(@NotNull String message)
	{
		this.logger.info(message);
	}

	@Override
	public void info(@NotNull String message, @NotNull Throwable throwable)
	{
		this.logger.info(message, throwable);
	}

	@Override
	public void info(@NotNull Throwable throwable)
	{
		this.logger.info(throwable);
	}

	@Override
	public void debug(@NotNull String message)
	{
		this.logger.debug(message);
	}

	@Override
	public void debug(@NotNull String message, @NotNull Throwable throwable)
	{
		this.logger.debug(message, throwable);
	}

	@Override
	public void debug(@NotNull Throwable throwable)
	{
		this.logger.debug(throwable);
	}
}