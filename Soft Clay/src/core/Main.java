package core;

import java.awt.Graphics2D;

import org.osbot.rs07.api.model.Player;
import org.osbot.rs07.script.Script;
import org.osbot.rs07.script.ScriptManifest;

@ScriptManifest(author = "adambrodin", info = "Soft Clay", name = "Soft Clay", version = 0, logo = "")
public class Main extends Script {
	private static int NET_PROFIT_PER_INVENTORY = 100;
	private int currentSessionProfit = 0;
	private boolean inBank = false;

	@Override
	public void onStart() {
		log("Soft Clay Bot - made By Adam Brodin");
		if (!bank.isOpen()) {
			BankItems();

		}
	}

	private boolean ReadyToUse() {
		Player p = myPlayer();
		if (!p.isAnimating() && !inBank) {
			return true;
		} else {
			return false;
		}
	}

	public void BankItems() {
		try {
			inBank = true;
			bank.open();
			bank.withdraw("Clay", 14);
			bank.withdraw("Bucket of Water", 14);
			bank.close();
			inBank = false;
		} catch (Exception e) {
			log(e.getStackTrace());
		}
	}
	
	public void MakeSoftClay()
	{
		getInventory().getItem("Clay").interact("Use");
		getInventory().getItem("Bucket Of Water").interact("Use");
		currentSessionProfit += NET_PROFIT_PER_INVENTORY;
	}

	@Override
	public int onLoop() throws InterruptedException {
		Player p = myPlayer();
		if (!p.isAnimating() && ReadyToUse()) {
			MakeSoftClay();
		}
		
		BankItems();
		return random(200, 300);
	}

	@Override
	public void onExit() {
		log("This session generated a net profit of: " + currentSessionProfit);
	}

	@Override
	public void onPaint(Graphics2D g) {
		
	}

}