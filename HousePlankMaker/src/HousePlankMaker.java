import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.io.File;
import java.net.MalformedURLException;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Random;

import org.osbot.rs07.api.Inventory;
import org.osbot.rs07.api.Bank.BankMode;
import org.osbot.rs07.api.map.Area;
import org.osbot.rs07.api.map.Position;
import org.osbot.rs07.api.model.Item;
import org.osbot.rs07.api.model.NPC;
import org.osbot.rs07.api.ui.RS2Widget;
import org.osbot.rs07.input.mouse.MiniMapTileDestination;
import org.osbot.rs07.script.Script;
import org.osbot.rs07.script.ScriptManifest;

import javax.sound.sampled.*;

@ScriptManifest(name = "HousePlankMaker", author = "adambrodin", version = 1.0, info = "Makes planks using the Demon Butler in a player-owned house.", logo = "https://i.imgur.com/IO9a399.png")
public class HousePlankMaker extends Script {
    // IDS
    private static final int HOUSE_TELEPORT_TAB_ID = 8013, BANK_TELEPORT_TAB_ID = 8010, LOG_ITEM_ID = 6333,
            PLANK_ITEM_ID = 8780;

    // Expenses
    private static final int BUTLER_COST_PER_PLANK = 500, BUTLER_COST_PER_USAGE = 10000 / 8, BOT_BREAK_CHANCE = 95,
            BOT_BREAK_DURATION = 90000;
    private int HOUSE_TELEPORT_TAB_PRICE, BANK_TELEPORT_TAB_PRICE, LOG_ITEM_PRICE, PLANK_ITEM_PRICE;
    private boolean restockItemsOnEmpty = true, firstRun = true;

    // GUI
    private static final int guiRectWidth = 580, showAllTimeStatsDuration = 15;
    private static int textXValue, textYValueStart, yValueIncrement = 25;
    private Font font;

    // Statistics
    private int planksMadeFromInv, logsInBank = 99999, inventoriesLeftBeforeFailure, totalPlanksMade, sessionNetProfit,
            sessionUptimeSeconds, amountOfBreaksTaken = 0, totalSessionBreaktimeSeconds = 0, gpPerHour, amountOfTrips,
            timeUntilFailure;
    public int allTimeUptimeSeconds, allTimeNetProfit = 0, allTimeAverageGpPerHour;
    private RS2Widget houseOptions, callServant, invLeftBarWidget, settingsMenu;
    private Calendar calendar;
    private long startTimeMillis;
    private String expectedFailureTime = "CALCULATING...", currentBotStatus = "Idling...";

    // Etc
    private static final int standardRandomMinPause = 600, standardRandomMaxPause = 800;
    private Area pvpZoneOutsideHouse;
    private Random rand;
    private Inventory inv;
    private Client socketClient;
    private StatsIO statsIO;

    private void CalculateGUIPosition() {
        textXValue = invLeftBarWidget.getAbsX() - guiRectWidth;
        textYValueStart = invLeftBarWidget.getAbsY();
    }

    @Override
    public void onStart() {
        log("HousePlankMaker - made by Adam Brodin");
        rand = new Random();
        calendar = Calendar.getInstance();
        startTimeMillis = System.currentTimeMillis();
        font = new Font("Consolas", Font.BOLD, 20);
        settingsMenu = getWidgets().get(161, 42);
        invLeftBarWidget = getWidgets().get(161, 34);
        inv = getInventory();

        pvpZoneOutsideHouse = new Area(2953, 3223, 2953, 3229);
        GetItemPrices();

        statsIO = new StatsIO(this);
        statsIO.Start();
        statsIO.LoadAllTimeStats();

        socketClient = new Client();
        socketClient.SetAccountName(myPlayer().getName());

        if (pvpZoneOutsideHouse.contains(myPlayer())) {
            inv.getItem(BANK_TELEPORT_TAB_ID).interact("Break");
        }

        CalculateGUIPosition();
    }

    private void GetItemPrices() {
        try {
            HOUSE_TELEPORT_TAB_PRICE = getGrandExchange().getOverallPrice(HOUSE_TELEPORT_TAB_ID);
            BANK_TELEPORT_TAB_PRICE = getGrandExchange().getOverallPrice(BANK_TELEPORT_TAB_ID);
            LOG_ITEM_PRICE = getGrandExchange().getOverallPrice(LOG_ITEM_ID);
            PLANK_ITEM_PRICE = getGrandExchange().getOverallPrice(PLANK_ITEM_ID);
        } catch (final Exception e) {
            e.getStackTrace();
        }
    }

    private void RandomizedSleep() throws InterruptedException {
        final int randSleepTime = (rand.nextInt(standardRandomMinPause) + 1) + standardRandomMaxPause;
        Thread.sleep(randSleepTime);
    }

    private void RandomizedSleep(final int min, final int max) throws InterruptedException {
        final int randSleepTime = (rand.nextInt(max) + 1) + min;
        if (randSleepTime > 2000) {
            currentBotStatus = "IDLING...";
        }
        Thread.sleep(randSleepTime);
    }

    private void RandomizedSleep(final int min, final int max, final boolean showIdleStatus)
            throws InterruptedException {
        final int randSleepTime = (rand.nextInt(max) + 1) + min;
        if (randSleepTime > 2000 && showIdleStatus) {
            currentBotStatus = "IDLING...";
        }
        Thread.sleep(randSleepTime);
    }

    private void RandomizedHouseWalk() {
        currentBotStatus = "MOVING TO RANDOM LOCATION";
        final Position randWalkPos = new Position(myPosition().getX() - 1, myPosition().getY() + 1, 0);
        final MiniMapTileDestination tileSpot = new MiniMapTileDestination(getBot(), randWalkPos);
        getMouse().click(tileSpot);
    }

    private void EventOrder() throws InterruptedException, MalformedURLException {
        // Bank Items
        BankItems();

        if (inv.contains(LOG_ITEM_ID) && !inv.contains(PLANK_ITEM_ID)) {
            // Teleports to player owned house
            currentBotStatus = "TELEPORTING TO PLAYER-OWNED HOUSE";
            inv.getItem(HOUSE_TELEPORT_TAB_ID).interact("Break");
            while (!getMap().isInHouse()) {
                RandomizedSleep(250, 500);
            }

            RandomizedSleep();
            RandomizedHouseWalk();
            RandomizedSleep();

            // Call Demon Butler
            CallButler();

            // Go through butler dialog
            ButlerDialogue();
            RandomizedSleep();
        }

        // Teleport to a bank
        currentBotStatus = "TELEPORTING TO BANK";
        inv.getItem(BANK_TELEPORT_TAB_ID).interact("Break");
        RandomizedSleep(2000, 2300, false);
        CalculateGUIPosition();
    }

    private void RestockItems() {
        try {
            currentBotStatus = "RESTOCKING ITEMS";
            bank.open();
            RandomizedSleep();
            bank.depositAll();
            RandomizedSleep();

            if (bank.contains("Ring of wealth") || bank.contains(11984) || bank.contains(11980) || bank.contains(11982) || bank.contains(11986) || bank.contains(11988)) {
                log("Bank contains Ring of wealth (");
                String ringChargedName = "";
                for (Item item : bank.getItems()) {
                    if (item.getName().contains("Ring of wealth ("))
                        if (item.getAmount() >= 1) {
                            ringChargedName = item.getName();
                            break;
                        }
                }
                log("Ring charged name is: " + ringChargedName);

                if (ringChargedName == "") {
                    currentBotStatus = "No ring of wealth found!";
                    Thread.sleep(5000);
                    stop();
                }

                bank.withdraw(ringChargedName, 1);
                RandomizedSleep();
                bank.close();
                currentBotStatus = "TELEPORTING TO THE GRAND EXCHANGE";
                RandomizedSleep();
                inv.getItem(ringChargedName).interact("Rub");
                RandomizedSleep();
                getDialogues().completeDialogue("Grand Exchange");
                RandomizedSleep(2000, 3000);
                bank.open();
                RandomizedSleep();

                currentBotStatus = "WITHDRAWING COINS & PLANKS";
                bank.withdrawAll("Coins");
                RandomizedSleep();
                if (bank.getAmount(PLANK_ITEM_ID) > 0) {
                    bank.enableMode(BankMode.WITHDRAW_NOTE);
                    bank.withdrawAll(PLANK_ITEM_ID);
                }
                RandomizedSleep();
                bank.close();
                RandomizedSleep();
                final NPC geClerk = getNpcs().closest("Grand Exchange Clerk");
                geClerk.interact("Exchange");
                currentBotStatus = "BUYING & SELLING ITEMS";
                RandomizedSleep();
                if (inv.getAmount(PLANK_ITEM_ID + 1) > 0) {
                    getGrandExchange().sellItem(PLANK_ITEM_ID + 1, PLANK_ITEM_PRICE - 50,
                            inv.getItem(PLANK_ITEM_ID + 1).getAmount());
                    RandomizedSleep();
                }

                sleep(5000);
                getGrandExchange().collect();
                RandomizedSleep();
                final int costPerInventory = BUTLER_COST_PER_USAGE + (BUTLER_COST_PER_PLANK * 25)
                        + HOUSE_TELEPORT_TAB_PRICE + BANK_TELEPORT_TAB_PRICE + (LOG_ITEM_PRICE * 25);
                final int inventoriesPossible = inv.getItem("Coins").getAmount() / costPerInventory;
                getGrandExchange().buyItem(LOG_ITEM_ID, "Teak logs", LOG_ITEM_PRICE + 50,
                        (inventoriesPossible * 25) - 50);
                RandomizedSleep();
                getGrandExchange().collect();
                RandomizedSleep();
                getGrandExchange().buyItem(HOUSE_TELEPORT_TAB_ID, "Teleport to house", HOUSE_TELEPORT_TAB_PRICE + 50,
                        inventoriesPossible);
                RandomizedSleep();
                getGrandExchange().collect();
                RandomizedSleep();
                getGrandExchange().buyItem(BANK_TELEPORT_TAB_ID, "Camelot teleport", BANK_TELEPORT_TAB_PRICE + 50,
                        inventoriesPossible);
                RandomizedSleep();
                getGrandExchange().collect();
                RandomizedSleep();
                getGrandExchange().close();
                RandomizedSleep();

                bank.open();
                RandomizedSleep();
                CalculateStats();
                bank.depositAll();
                RandomizedSleep();
                bank.enableMode(BankMode.WITHDRAW_ITEM);
                RandomizedSleep();
                logsInBank = bank.getItem(LOG_ITEM_ID).getAmount();
                CalculateStats();
                bank.close();

            } else {
                log("No charged ring of wealth was found, stopping.");
                stop();
            }

        } catch (

                final InterruptedException e) {
            log(e.getMessage());
            e.printStackTrace();
            stop();
        }
    }

    private void BankItems() throws InterruptedException, MalformedURLException {
        currentBotStatus = "BANKING ITEMS";

        // Prevent wasting time withdrawing items if they already exist in inventory
        if (!inv.contains(LOG_ITEM_ID) || inv.getItem(LOG_ITEM_ID).getAmount() < 25
                || (sessionUptimeSeconds > 30 && inventoriesLeftBeforeFailure < 2) || firstRun) {
            if (!bank.isOpen()) {
                bank.open();
                RandomizedSleep();
                if (firstRun) {
                    bank.depositAll();
                    firstRun = false;
                }
            }
            if (inventoriesLeftBeforeFailure < 2 && sessionUptimeSeconds > 60) {
                if (restockItemsOnEmpty) {
                    RestockItems();
                } else {
                    currentBotStatus = "OUT OF SUPPLIES, EXITING..";
                    Thread.sleep(5000);
                    stop();
                }
            }

            // If an issue occurred which made the planks remain in the inventory
            if (inv.contains(PLANK_ITEM_ID)) {
                bank.depositAll(PLANK_ITEM_ID);
            }
            if (!inv.contains("Coins")) {
                bank.withdrawAll("Coins");
            }
            if (!inv.contains(BANK_TELEPORT_TAB_ID)) {
                bank.withdrawAll(BANK_TELEPORT_TAB_ID);
            }
            if (!inv.contains(HOUSE_TELEPORT_TAB_ID)) {
                bank.withdrawAll(HOUSE_TELEPORT_TAB_ID);
            }
            RandomizedSleep();
        }

        if (bank.contains(LOG_ITEM_ID) && bank.isOpen() && inv.contains("Coins")) {
            if (inv.getEmptySlots() > 0) {
                bank.withdrawAll(LOG_ITEM_ID);
                logsInBank = bank.getItem(LOG_ITEM_ID).getAmount();
                RandomizedSleep();
            }
        }

        if (bank.isOpen()) {
            bank.close();
        }

        RandomizedSleep(500, 1000);
    }

    private void CallButler() throws InterruptedException {
        currentBotStatus = "CALLING SERVANT";
        settingsMenu.interact();
        RandomizedSleep();

        houseOptions = getWidgets().get(116, 28);
        if (houseOptions != null) {
            houseOptions.interact();
            RandomizedSleep();
        }

        callServant = getWidgets().get(370, 19, 0);
        if (callServant != null) {
            callServant.interact();
            RandomizedSleep(1250, 1750);
        }
    }

    private void ShowChatbox() {
        final RS2Widget chatboxWidget = getWidgets().get(162, 5);
        chatboxWidget.interact();
    }

    private void ButlerDialogue() throws InterruptedException {
        int dialogueStartLogs = (int) inv.getAmount(LOG_ITEM_ID);

        currentBotStatus = "IN DIALOGUE WITH SERVANT";
        int timeBefore = sessionUptimeSeconds;
        while (getDialogues().inDialogue()) {
            getKeyboard().typeKey((char) KeyEvent.VK_1);
            RandomizedSleep();
            getKeyboard().pressKey(KeyEvent.VK_SPACE);
            RandomizedSleep();

            // If the bot gets stuck because the chatbox is hidden, show it again
            if (sessionUptimeSeconds - timeBefore > 15) {
                currentBotStatus = "TURNING ON CHATBOX, KEEP IT VISIBLE!!!";
                ShowChatbox();
                timeBefore = sessionUptimeSeconds;
            }
        }

        RandomizedSleep(250, 500);
        getKeyboard().pressKey((char) KeyEvent.VK_ESCAPE);
        if (dialogueStartLogs > 0 && (inv.getAmount(LOG_ITEM_ID) < dialogueStartLogs)) {
            int planksMade = dialogueStartLogs - (int) inv.getAmount(LOG_ITEM_ID);
            totalPlanksMade += planksMade;
            planksMadeFromInv = planksMade;
            amountOfTrips++;
        }
    }

    @Override
    public int onLoop() throws InterruptedException {
        try {
            EventOrder();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        CalculateStats();

        int randDelay = rand.nextInt(100);
        if (randDelay >= BOT_BREAK_CHANCE && sessionUptimeSeconds >= 300) {
            // Takes a break randomly based on a set chance (anti-ban measure)
            currentBotStatus = "TAKING A BREAK.....";
            int breakTime = (rand.nextInt(BOT_BREAK_DURATION) + 1) + BOT_BREAK_DURATION / 2;
            totalSessionBreaktimeSeconds += (breakTime / 1000);
            amountOfBreaksTaken++;
            return breakTime;
        } else {
            return random(100, 500);
        }

    }

    private void CalculateExpectedFailureTime() {
        calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, timeUntilFailure);
        expectedFailureTime = calendar.getTime().toString().substring(11, 20);
    }

    private void CalculateStats() {
        final int profitPerLog = (PLANK_ITEM_PRICE - LOG_ITEM_PRICE)
                - (BUTLER_COST_PER_PLANK - (BUTLER_COST_PER_USAGE / planksMadeFromInv));
        sessionNetProfit = (totalPlanksMade * profitPerLog)
                - ((HOUSE_TELEPORT_TAB_PRICE + BANK_TELEPORT_TAB_PRICE) * amountOfTrips);

        final int gpPerSecond = sessionNetProfit / sessionUptimeSeconds;
        gpPerHour = gpPerSecond * 3600;

        final int logInventoriesLeft = logsInBank / planksMadeFromInv;
        final int cashInventoriesLeft = inv.getItem("Coins").getAmount()
                / (BUTLER_COST_PER_PLANK * 25 + BUTLER_COST_PER_USAGE);
        final int houseTeleportInventoriesLeft = inv.getItem(HOUSE_TELEPORT_TAB_ID).getAmount();
        final int bankTeleportInventoriesLeft = inv.getItem(BANK_TELEPORT_TAB_ID).getAmount();

        int min = Math.min(logInventoriesLeft, cashInventoriesLeft);
        min = Math.min(min, houseTeleportInventoriesLeft);
        inventoriesLeftBeforeFailure = Math.min(min, bankTeleportInventoriesLeft);
        final int averageTimePerInventory = sessionUptimeSeconds / amountOfTrips;
        timeUntilFailure = inventoriesLeftBeforeFailure * averageTimePerInventory;

        socketClient.SetProfitPerHour(gpPerHour);
        socketClient.SendMessage();

        CalculateExpectedFailureTime();
    }

    private String SecondsToTime(final int inputSeconds) {
        final int seconds = inputSeconds % 60;
        final int minutes = (inputSeconds % 3600) / 60;
        final int hours = inputSeconds / 3600;

        return Integer.toString(hours) + "h:" + Integer.toString(minutes) + "m:" + Integer.toString(seconds) + "s";
    }

    @Override
    public void onPaint(final Graphics2D g) {
        final long timeElapsed = System.currentTimeMillis() - startTimeMillis;
        sessionUptimeSeconds = (int) (timeElapsed) / 1000;
        int yValue = textYValueStart;

        g.setFont(font);
        g.setColor(Color.BLACK);
        g.drawRoundRect(textXValue - 25, textYValueStart - 25, guiRectWidth, 290, 50, 50);
        g.fillRoundRect(textXValue - 25, textYValueStart - 25, guiRectWidth, 290, 50, 50);
        g.setColor(Color.GREEN);
        g.drawString("CURRENT STATUS: " + currentBotStatus, textXValue, yValue);
        yValue += yValueIncrement;

        g.setColor(Color.WHITE);
        g.drawString("TIME ELAPSED: " + SecondsToTime(sessionUptimeSeconds), textXValue, yValue);
        yValue += yValueIncrement;

        g.setColor(Color.CYAN);
        g.drawString("TOTAL PLANKS MADE: " + totalPlanksMade, textXValue, yValue);
        yValue += yValueIncrement;

        g.setColor(Color.MAGENTA);
        g.drawString("LOGS LEFT IN BANK: " + logsInBank, textXValue, yValue);
        yValue += yValueIncrement;

        g.setColor(Color.RED);
        g.drawString("INVENTORIES REMAINING: " + inventoriesLeftBeforeFailure, textXValue, yValue);
        yValue += yValueIncrement;

        g.drawString("ESTIMATED COMPLETION TIME - " + expectedFailureTime, textXValue, yValue);
        yValue += yValueIncrement;

        g.setColor(Color.PINK);
        g.drawString("SESSION NET PROFIT: " + NumberFormat.getInstance(Locale.US).format(sessionNetProfit) + " GP",
                textXValue, yValue);
        yValue += yValueIncrement;

        g.setColor(Color.YELLOW);
        g.drawString("GP/HOUR: " + NumberFormat.getInstance(Locale.US).format(gpPerHour), textXValue, yValue);
        yValue += yValueIncrement;

        g.setColor(Color.GRAY);
        g.drawString(
                "BREAKS TAKEN: " + amountOfBreaksTaken + " TOTALING " + SecondsToTime(totalSessionBreaktimeSeconds),
                textXValue, yValue);
        yValue += yValueIncrement;

        if (sessionUptimeSeconds <= showAllTimeStatsDuration) {
            g.setColor(Color.BLUE);
            g.drawString("ALL TIME NET PROFIT: " + NumberFormat.getInstance(Locale.US).format(allTimeNetProfit) + " GP",
                    textXValue, yValue);
            yValue += yValueIncrement;

            g.drawString("ALL TIME UP TIME: " + SecondsToTime(allTimeUptimeSeconds), textXValue, yValue);
            yValue += yValueIncrement;
            g.drawString("ALL TIME AVERAGE: " + NumberFormat.getInstance(Locale.US).format(allTimeAverageGpPerHour)
                    + " GP/HOUR", textXValue, yValue);
        } else {
            yValueIncrement = 30;
        }

    }

    @Override
    public void onExit() {
        if (sessionNetProfit > 0) {
            allTimeNetProfit += sessionNetProfit;
        } else {
            log("sessionNetProfit < 0, current Value: " + sessionNetProfit);
        }

        if (sessionUptimeSeconds > 0) {
            allTimeUptimeSeconds += sessionUptimeSeconds;
        } else {
            log("sessionUptimeSeconds < 0, current Value: " + sessionUptimeSeconds);
        }

        statsIO.SaveAllTimeStats();
        log("Session ended with a total net profit of: " + Integer.toString(sessionNetProfit) + " - Runtime: "
                + SecondsToTime(sessionUptimeSeconds));
    }

}
