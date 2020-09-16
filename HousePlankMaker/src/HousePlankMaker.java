import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.util.Random;

import org.osbot.rs07.api.Inventory;
import org.osbot.rs07.api.ui.RS2Widget;
import org.osbot.rs07.script.Script;
import org.osbot.rs07.script.ScriptManifest;

@ScriptManifest(name = "HousePlankMaker", author = "adambrodin", version = 1.0, info = "Makes planks using the Demon Butler.", logo = "")
public class HousePlankMaker extends Script {
	private static int HOUSE_TELEPORT_TAB_ID = 8013; // House Teleport Tab
	private static int BANK_TELEPORT_TAB_ID = 8010; // Camelot Teleport Tab
	private static int LOG_ITEM_ID = 6333; // Teak Logs
	private static int PLANK_ITEM_ID = 8780; // Teak Planks
	private static int[] houseOptionsID = new int[] { 261, 101 };

	// Expenses
	private static int HOUSE_TELEPORT_TAB_PRICE, BANK_TELEPORT_TAB_PRICE, LOG_ITEM_PRICE, PLANK_ITEM_PRICE;
	private static int BUTLER_COST_PER_PLANK = 500, BUTLER_COST_PER_USAGE = 10000 / 8;

	private int planksInInv, totalPlanksMade, totalNetProfit, timeElapsedSeconds, gpPerHour, amountOfTrips;
	private RS2Widget houseOptions, callServant;
	private Inventory inv;
	private Random rand;
	private Font font = new Font("Open Sans", Font.BOLD, 20);

	@Override
	public void onStart() {
		rand = new Random();
		inv = getInventory();
		houseOptions = getWidgets().get(houseOptionsID[0], houseOptionsID[1]);
		callServant = getWidgets().getWidgetContainingText("Call Servant");
		GetItemPrices();
	}

	private void GetItemPrices() {
		try {
			HOUSE_TELEPORT_TAB_PRICE = grandExchange.getOverallPrice(HOUSE_TELEPORT_TAB_ID);
			BANK_TELEPORT_TAB_ID = grandExchange.getOverallPrice(BANK_TELEPORT_TAB_ID);
			LOG_ITEM_ID = grandExchange.getOverallPrice(LOG_ITEM_ID);
			PLANK_ITEM_PRICE = grandExchange.getOverallPrice(PLANK_ITEM_ID);
		} catch (Exception e) {
			e.getStackTrace();
		}
	}

	private void RandomizedSleep(int min, int max) throws InterruptedException {
		Thread.sleep((rand.nextInt(max) + 1) + min);
	}

	private void EventOrder() throws InterruptedException {
		// Bank Items
		BankItems();

		// Teleports to player owned house
		inv.getItem(HOUSE_TELEPORT_TAB_ID).interact("Teleport");
		RandomizedSleep(500, 2000);

		// Call Demon Butler
		CallButler();
		RandomizedSleep(750, 1500);

		// Go through butler dialog
		ButlerDialog();
		RandomizedSleep(300, 700);

		// Teleport to a bank
		totalPlanksMade += planksInInv;
		inv.getItem(BANK_TELEPORT_TAB_ID).interact("Teleport");
		amountOfTrips++;
	}

	private void BankItems() throws InterruptedException {
		bank.open();
		RandomizedSleep(100, 200);

		// Exits the bot if there are no logs left
		if (bank.contains(LOG_ITEM_ID)) {
			bank.withdrawAll(LOG_ITEM_ID);
		} else {
			onExit();
		}

		RandomizedSleep(100, 200);
		bank.close();

		// Calculates amount of free spaces in inv, (28 is max inv, subtracting two
		// different teleports and a cash stack)
		planksInInv = 25 - inv.getEmptySlots();
	}

	private void CallButler() throws InterruptedException {
		getKeyboard().pressKey(KeyEvent.VK_F10);
		RandomizedSleep(100, 500);

		if (houseOptions != null && callServant != null) {
			houseOptions.interact();
			callServant.interact("Call Servant");
		}
	}

	private void ButlerDialog() throws InterruptedException {
		getKeyboard().pressKey(KeyEvent.VK_1);
		RandomizedSleep(750, 1500);
		getKeyboard().pressKey(KeyEvent.VK_SPACE);
		RandomizedSleep(600, 1500);
		getKeyboard().pressKey(KeyEvent.VK_1);
		RandomizedSleep(900, 1200);
		getKeyboard().pressKey(KeyEvent.VK_SPACE);
		RandomizedSleep(800, 1400);
	}

	@Override
	public int onLoop() throws InterruptedException {
		EventOrder();
		return random(100, 400);
	}

	private void CalculateStats() {
		gpPerHour = totalNetProfit / ((int) timeElapsedSeconds / (60 * 60));

		int profitPerLog = (PLANK_ITEM_PRICE - LOG_ITEM_PRICE) - (BUTLER_COST_PER_PLANK - (BUTLER_COST_PER_USAGE / 25));

		totalNetProfit = (totalPlanksMade * profitPerLog)
				- ((HOUSE_TELEPORT_TAB_PRICE + BANK_TELEPORT_TAB_PRICE) * amountOfTrips);
	}

	private String TimeElapsed() {
		int hours = (int) timeElapsedSeconds / (60 * 60);
		int minutes = (int) (timeElapsedSeconds / 60) % 60;
		return Integer.toString(hours) + " HOURS, " + Integer.toString(minutes) + " MINUTES, "
				+ Integer.toString(timeElapsedSeconds) + " SECONDS";
	}

	@Override
	public void onPaint(Graphics2D g) {
		CalculateStats();

		g.setFont(font);
		g.setColor(Color.WHITE);
		g.drawString("TIME ELAPSED: " + TimeElapsed(), 50, 50);
		g.setColor(Color.CYAN);
		g.drawString("TOTAL PLANKS MADE: " + totalPlanksMade, 50, 100);
		g.setColor(Color.MAGENTA);
		g.drawString("TOTAL NET PROFIT: " + Integer.toString(totalNetProfit) + " GP", 50, 150);
		g.setColor(Color.YELLOW);
		g.drawString("GP/HOUR: " + Integer.toString(gpPerHour), 50, 200);
	}

}
