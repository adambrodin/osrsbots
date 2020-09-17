import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.io.File;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Random;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.osbot.rs07.api.Inventory;
import org.osbot.rs07.api.map.Position;
import org.osbot.rs07.api.ui.RS2Widget;
import org.osbot.rs07.input.mouse.MiniMapTileDestination;
import org.osbot.rs07.script.Script;
import org.osbot.rs07.script.ScriptManifest;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

@ScriptManifest(name = "HousePlankMaker", author = "adambrodin", version = 1.0, info = "Makes planks using the Demon Butler.", logo = "")
public class HousePlankMaker extends Script {
	// IDS
	private static final int HOUSE_TELEPORT_TAB_ID = 8013; // House Teleport Tab
	private static final int BANK_TELEPORT_TAB_ID = 8010; // Camelot Teleport Tab
	private static final int LOG_ITEM_ID = 6333; // Teak Logs
	private static final int PLANK_ITEM_ID = 8780; // Teak Planks

	// Expenses
	private static final int BUTLER_COST_PER_PLANK = 500, BUTLER_COST_PER_USAGE = 10000 / 8;
	private int HOUSE_TELEPORT_TAB_PRICE, BANK_TELEPORT_TAB_PRICE, LOG_ITEM_PRICE, PLANK_ITEM_PRICE;

	// GUI
	private static final int yValueStart = 50, yValueIncrement = 50, showAllTimeStatsDuration = 15;
	private Font font;

	// Statistics
	private int planksInInv, logsInBank, inventoriesLeftBeforeFailure, totalPlanksMade, sessionNetProfit,
			sessionUptimeSeconds, gpPerHour, amountOfTrips, allTimeNetProfit = 0, allTimeUptimeSeconds = 0,
			allTimeAverageGpPerHour;
	private RS2Widget houseOptions, callServant;
	private long startTimeMillis;
	private BotStates currentState;

	private enum BotStates {
		Idling, Banking, InDialogue, Moving, NavigatingMenu, Breaking;
	}

	// Etc
	private Random rand;
	private Inventory inv;

	// IO
	final String filePath = getDirectoryData() + "houseplankmaker.xml";

	private void LoadAllTimeStats() {
		try {
			DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document doc = builder.parse(new File(filePath));
			doc.normalizeDocument();

			// Gets the root element (user in our case)
			Element user = doc.getDocumentElement();
			allTimeNetProfit = Integer.parseInt(user.getElementsByTagName("AllTimeNetProfit").item(0).getTextContent());
			allTimeUptimeSeconds = Integer
					.parseInt(doc.getElementsByTagName("AllTimeUpTimeSeconds").item(0).getTextContent());
			allTimeAverageGpPerHour = (allTimeNetProfit / allTimeUptimeSeconds) * 3600;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void SaveAllTimeStats() {
		try {
			DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document doc = builder.newDocument();

			Element user = doc.createElement("user");
			doc.appendChild(user);

			Attr attr = doc.createAttribute("id");
			attr.setValue("1");
			user.setAttributeNode(attr);

			Element allTimeNetProfitElement = doc.createElement("AllTimeNetProfit");
			allTimeNetProfitElement.setTextContent(Integer.toString(allTimeNetProfit));

			Element allTimeUptimeSecondsElement = doc.createElement("AllTimeUpTimeSeconds");
			allTimeUptimeSecondsElement.setTextContent(Integer.toString(allTimeUptimeSeconds));

			user.appendChild(allTimeNetProfitElement);
			user.appendChild(allTimeUptimeSecondsElement);

			Transformer tr = TransformerFactory.newInstance().newTransformer();
			tr.setOutputProperty(OutputKeys.INDENT, "yes");
			tr.transform(new DOMSource(doc), new StreamResult(new File(filePath)));
		} catch (Exception e) {
			e.printStackTrace();
			log("Save went wrong!!!!!");
			log(e.getMessage());
		}
	}

	@Override
	public void onStart() {
		log("HousePlankMaker - made by Adam Brodin");
		LoadAllTimeStats();
		startTimeMillis = System.currentTimeMillis();
		font = new Font("Consolas", Font.BOLD, 60);
		rand = new Random();
		inv = getInventory();
		GetItemPrices();
	}

	private void GetItemPrices() {
		try {
			HOUSE_TELEPORT_TAB_PRICE = getGrandExchange().getOverallPrice(HOUSE_TELEPORT_TAB_ID);
			BANK_TELEPORT_TAB_PRICE = getGrandExchange().getOverallPrice(BANK_TELEPORT_TAB_ID);
			LOG_ITEM_PRICE = getGrandExchange().getOverallPrice(LOG_ITEM_ID);
			PLANK_ITEM_PRICE = getGrandExchange().getOverallPrice(PLANK_ITEM_ID);
		} catch (Exception e) {
			e.getStackTrace();
		}
	}

	private void RandomizedSleep(int min, int max) throws InterruptedException {
		int randSleepTime = (rand.nextInt(max) + 1) + min;
		if (randSleepTime > 2000) {
			currentState = BotStates.Idling;
		}
		Thread.sleep(randSleepTime);
	}

	private String GetCurrentStateInfo() {
		switch (currentState) {
		case Idling:
			return "IDLING..";
		case Banking:
			return "BANKING";
		case InDialogue:
			return "IN DIALOGUE";
		case Moving:
			return "MOVING";
		case NavigatingMenu:
			return "NAVIGATING MENUS";
		case Breaking:
			return "TAKING A BREAK....";
		default:
			return "ERROR";
		}
	}

	private void RandomizedHouseWalk() {
		currentState = BotStates.Moving;
		Position randWalkPos = new Position(myPosition().getX() + (rand.nextInt(3) + 1) + -3,
				myPosition().getY() + (rand.nextInt(1) + 1), 0);
		MiniMapTileDestination tileSpot = new MiniMapTileDestination(getBot(), randWalkPos);
		getMouse().click(tileSpot);
	}

	private void EventOrder() throws InterruptedException {
		RandomizedSleep(2750, 3000);

		// Bank Items
		BankItems();
		RandomizedSleep(750, 1250);

		// Teleports to player owned house
		inv.getItem(HOUSE_TELEPORT_TAB_ID).interact("Break");
		RandomizedSleep(3000, 4000);
		RandomizedHouseWalk();

		// Call Demon Butler
		CallButler();
		RandomizedSleep(1250, 1750);

		// Go through butler dialog
		ButlerDialog();
		RandomizedSleep(750, 1250);

		// Teleport to a bank
		inv.getItem(BANK_TELEPORT_TAB_ID).interact("Break");
		totalPlanksMade += planksInInv;
		amountOfTrips++;
		CalculateStats();
	}

	private void BankItems() throws InterruptedException {
		currentState = BotStates.Banking;

		// Prevent wasting time withdrawing items if they already exist in inventory
		if (!inv.contains(LOG_ITEM_ID)) {
			bank.open();
			RandomizedSleep(750, 1250);
		}

		// If an issue occurred which made the planks remain in the inventory
		if (inv.contains(PLANK_ITEM_ID)) {
			bank.depositAll(PLANK_ITEM_ID);
		}

		// Exits the bot if there are no logs left
		if (bank.contains(LOG_ITEM_ID) && bank.isOpen() && inv.contains("Coins")) {
			if (inv.getEmptySlots() > 0) {
				bank.withdrawAll(LOG_ITEM_ID);
				logsInBank = bank.getItem(LOG_ITEM_ID).getAmount();
			}
		}

		RandomizedSleep(750, 1250);
		if (bank.isOpen()) {
			bank.close();
		}

		// Calculates amount of free spaces in inv, (28 is max inv, subtracting two
		// different teleports and a cash stack)
		planksInInv = 25 - inv.getEmptySlots();
	}

	private void CallButler() throws InterruptedException {
		currentState = BotStates.NavigatingMenu;
		getKeyboard().pressKey(KeyEvent.VK_F10);
		RandomizedSleep(750, 1250);

		houseOptions = getWidgets().get(261, 101);
		if (houseOptions != null) {
			houseOptions.interact();
		}

		RandomizedSleep(750, 1250);
		callServant = getWidgets().get(370, 19, 0);
		if (callServant != null) {
			callServant.interact();
		}
	}

	private void ButlerDialog() throws InterruptedException {
		currentState = BotStates.InDialogue;
		while (getDialogues().inDialogue()) {
			getKeyboard().typeKey((char) KeyEvent.VK_1);
			RandomizedSleep(750, 1250);
			getKeyboard().pressKey(KeyEvent.VK_SPACE);
			RandomizedSleep(750, 1250);
		}

		RandomizedSleep(250, 500);
		getKeyboard().pressKey((char) KeyEvent.VK_ESCAPE);
	}

	@Override
	public int onLoop() throws InterruptedException {
		EventOrder();
		if (inventoriesLeftBeforeFailure <= 0) {
			stop();
		}

		int randDelay = rand.nextInt(100);
		if (randDelay >= 95) {
			// Takes a one minute break (anti-ban measure)
			currentState = BotStates.Breaking;
			return 60000;
		} else {

			return random(100, 500);
		}

	}

	private void CalculateStats() {
		int profitPerLog = (PLANK_ITEM_PRICE - LOG_ITEM_PRICE) - (BUTLER_COST_PER_PLANK - (BUTLER_COST_PER_USAGE / 25));
		sessionNetProfit = (totalPlanksMade * profitPerLog)
				- ((HOUSE_TELEPORT_TAB_PRICE + BANK_TELEPORT_TAB_PRICE) * amountOfTrips);

		int gpPerSecond = sessionNetProfit / sessionUptimeSeconds;
		gpPerHour = gpPerSecond * 3600;

		int logInventoriesLeft = logsInBank / 25;
		int cashInventoriesLeft = inv.getItem("Coins").getAmount()
				/ (BUTLER_COST_PER_PLANK * 25 + BUTLER_COST_PER_USAGE);
		inventoriesLeftBeforeFailure = Math.min(logInventoriesLeft, cashInventoriesLeft);
	}

	private String TimeElapsed(int inputSeconds) {
		int seconds = inputSeconds % 60;
		int minutes = (inputSeconds % 3600) / 60;
		int hours = inputSeconds / 3600;

		return Integer.toString(hours) + "H:" + Integer.toString(minutes) + "M:" + Integer.toString(seconds) + "S";
	}

	@Override
	public void onPaint(Graphics2D g) {
		long timeElapsed = System.currentTimeMillis() - startTimeMillis;
		sessionUptimeSeconds = (int) (timeElapsed) / 1000;
		int yValue = yValueStart;

		g.setFont(font);
		g.setColor(Color.GREEN);
		g.drawString("CURRENT ACTIVITY: " + GetCurrentStateInfo(), 50, yValue);
		yValue += yValueIncrement;
		g.setColor(Color.WHITE);
		g.drawString("TIME ELAPSED: " + TimeElapsed(sessionUptimeSeconds), 50, yValue);
		yValue += yValueIncrement;
		g.setColor(Color.CYAN);
		g.drawString("TOTAL PLANKS MADE: " + totalPlanksMade, 50, yValue);
		yValue += yValueIncrement;
		g.setColor(Color.MAGENTA);
		g.drawString("LOGS LEFT IN BANK: " + logsInBank, 50, yValue);
		yValue += yValueIncrement;
		g.setColor(Color.RED);
		g.drawString("INVENTORIES LEFT BEFORE FAILURE: " + inventoriesLeftBeforeFailure, 50, yValue);
		yValue += yValueIncrement;
		g.setColor(Color.PINK);
		g.drawString("TOTAL NET PROFIT: " + Integer.toString(sessionNetProfit) + " GP", 50, yValue);
		yValue += yValueIncrement;
		g.setColor(Color.YELLOW);
		g.drawString("GP/HOUR: " + NumberFormat.getInstance(Locale.US).format(gpPerHour), 50, yValue);
		yValue += yValueIncrement;

		if (sessionUptimeSeconds <= showAllTimeStatsDuration) {
			g.setColor(Color.BLUE);
			g.drawString("ALL TIME NET PROFIT: " + Integer.toString(allTimeNetProfit) + " GP", 50, yValue);
			yValue += yValueIncrement;
			g.drawString("ALL TIME UP TIME: " + TimeElapsed(allTimeUptimeSeconds), 50, yValue);
			yValue += yValueIncrement;
			g.drawString("ALL TIME AVERAGE GP/HOUR: " + Integer.toString(allTimeAverageGpPerHour), 50, yValue);
		}
	}

	@Override
	public void onExit() {
		allTimeNetProfit += sessionNetProfit;
		allTimeUptimeSeconds += sessionUptimeSeconds;

		SaveAllTimeStats();
		log("Session ended with a total net profit of: " + Integer.toString(sessionNetProfit) + " - Runtime: "
				+ TimeElapsed(sessionUptimeSeconds));
	}

}
