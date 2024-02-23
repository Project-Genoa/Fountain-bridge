package micheal65536.fountain;

public enum ShutdownMode
{
	NONE, // server will not shut down automatically
	HOST, // server will shut down once the player that is "hosting" the game has disconnected, kicking any other players that are still connected
	LAST; // server will shut down once the last player has disconnected
}
