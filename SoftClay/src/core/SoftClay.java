package core;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.util.Random;

import org.osbot.rs07.api.model.Player;
import org.osbot.rs07.script.Script;
import org.osbot.rs07.script.ScriptManifest;

@ScriptManifest(name = "SoftClay", author = "adambrodin", version = 1.0, info = "Makes Soft Clay by combining water with clay.", logo = "https://oldschool.runescape.wiki/images/thumb/b/b0/Soft_clay_detail.png/130px-Soft_clay_detail.png")
public class SoftClay extends Script {
	private static int NET_PROFIT_PER_INVENTORY = 84;
	private static int TIME_PER_INVENTORY = 16800;
	private static String PRIMARY_ITEM_NAME = "Clay";
	private static String SECONDARY_ITEM_NAME = "Jug of Water";

	private int currentSessionProfit = 0;
	private boolean inBank = false;
	private Random rand;

	@Override
	public void onStart() {
		log("Soft Clay Bot - made By Adam Brodin");
		rand = new Random();
		BankItems();
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
			bank.depositAll();
			bank.withdraw(PRIMARY_ITEM_NAME, 14);
			bank.withdraw(SECONDARY_ITEM_NAME, 14);
			bank.close();
			inBank = false;
		} catch (Exception e) {
			log(e.getStackTrace());
		}
	}

	public void MakeSoftClay() throws InterruptedException {
		getInventory().getItem(PRIMARY_ITEM_NAME).interact("Use");
		getInventory().getItem(SECONDARY_ITEM_NAME).interact("Use");

		while (!getDialogues().inDialogue()) {
			Thread.sleep(100);
		}
		getKeyboard().pressKey(KeyEvent.VK_SPACE);
		currentSessionProfit += NET_PROFIT_PER_INVENTORY;

		Thread.sleep(rand.nextInt((TIME_PER_INVENTORY + 1500) + 1) + TIME_PER_INVENTORY + 500);
	}

	@Override
	public int onLoop() throws InterruptedException {
		if (ReadyToUse()) {
			MakeSoftClay();
			BankItems();
		}
		return random(100, 400);
	}

	@Override
	public void onPaint(Graphics2D g) {
		Font font = new Font("Open Sans", Font.BOLD, 25);
		g.setFont(font);
		g.setColor(Color.CYAN);

		String netProfitText = "CURRENT SESSION NET PROFIT: " + Integer.toString(currentSessionProfit) + "GP";
		g.drawString(netProfitText, 50, 50);
	}

}