package core;

import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.util.Random;

import org.osbot.rs07.api.model.Player;
import org.osbot.rs07.script.Script;
import org.osbot.rs07.script.ScriptManifest;

@ScriptManifest(name = "SoftClay", author = "adambrodin", version = 1.0, info = "", logo = "")
public class SoftClay extends Script 
{
	private static int NET_PROFIT_PER_INVENTORY = 100;
	private static int TIME_PER_INVENTORY = 16800;
	
	private int currentSessionProfit = 0;
	private boolean inBank = false;
	private Random rand;

	@Override
	public void onStart() 
	{
		log("Soft Clay Bot - made By Adam Brodin");
		rand = new Random();
		BankItems();
	}

	private boolean ReadyToUse()
	{
		Player p = myPlayer();
		if (!p.isAnimating() && !inBank) {
			return true;
		} else {
			return false;
		}
	}

	public void BankItems() 
	{
		try {
			inBank = true;
			bank.open();
			bank.depositAll();
			bank.withdraw("Clay", 14);
			bank.withdraw("Bucket of water", 14);
			bank.close();
			inBank = false;
		} catch (Exception e) {
			log(e.getStackTrace());
		}
	}

	public void MakeSoftClay() throws InterruptedException 
	{
		getInventory().getItem("Clay").interact("Use");
		getInventory().getItem("Bucket Of water").interact("Use");
		
		while(!getDialogues().inDialogue())
		{
			Thread.sleep(100);
		}
		getKeyboard().pressKey(KeyEvent.VK_SPACE);
		currentSessionProfit += NET_PROFIT_PER_INVENTORY;
	
		Thread.sleep(rand.nextInt((TIME_PER_INVENTORY + 1500) + 1) + TIME_PER_INVENTORY + 500);
	}

	@Override
	public int onLoop() throws InterruptedException 
	{
		if (ReadyToUse()) 
		{
			MakeSoftClay();
			BankItems();
		}
		return random(100, 400);
	}

	@Override
	public void onExit() 
	{
		log("This session generated a net profit of: " + currentSessionProfit);
	}

	@Override
	public void onPaint(Graphics2D g)
	{

	}

}