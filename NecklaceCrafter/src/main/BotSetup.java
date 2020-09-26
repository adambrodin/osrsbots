package main;

import org.dreambot.api.methods.Calculations;
import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.container.impl.bank.Bank;
import org.dreambot.api.methods.dialogues.Dialogues;
import org.dreambot.api.methods.grandexchange.GrandExchange;
import org.dreambot.api.methods.interactive.NPCs;
import org.dreambot.api.methods.map.Area;
import org.dreambot.api.methods.skills.Skill;
import org.dreambot.api.methods.skills.Skills;
import org.dreambot.api.methods.walking.impl.Walking;
import org.dreambot.api.script.AbstractScript;

import java.awt.*;

public class BotSetup extends AbstractScript {
    // Variables
    public Area grandExchangeArea = new Area(3161, 3486, 3168, 3493, 0);
    public Area edgevilleBankArea = new Area(3096, 3496, 3094, 3496, 0);
    private String leatherItemName = "Leather", needleItemName = "Needle", threadItemName = "Thread", goldBarItemName = "Gold bar", ringMouldItemName = "Ring mould", necklaceMouldItemName = "Necklace mould";
    private int minCashRequirement = 500000, leatherRequired = 100, needlesRequired = 1, threadRequired = leatherRequired / 5, goldBarsRequiredUntil22 = 350;
    private int randMinPause = 2000, randMaxPause = 3000, walkingSleepTime = 4000, runningSleepTime = 1500;
    private boolean inTraining = false;
    private String currentBotTask;

    private void RandomizedSleep() {
        sleep(Calculations.random(randMinPause, randMaxPause));
    }

    public void WalkToArea(Area area) {
        while (!area.contains(getLocalPlayer())) {
            if (Walking.getRunEnergy() > 5 && !Walking.isRunEnabled()) {
                Walking.toggleRun();
            }
            Walking.walk(area.getRandomTile());

            if (Walking.isRunEnabled()) {
                sleep(runningSleepTime);
            } else {
                sleep(walkingSleepTime);
            }
        }
    }

    public void GEAction(boolean buy, String itemName, int amount, int price) {
        if (GrandExchange.isOpen()) {
            if (buy) {
                GrandExchange.buyItem(itemName, amount, price);
            } else {
                GrandExchange.sellItem(itemName, amount, price);
            }
            sleepUntil(() -> GrandExchange.isReadyToCollect(), 60000);
            GrandExchange.collect();
            RandomizedSleep();
        }
    }

    private void BuyItems() {
        // Reached the Grand Exchange
        Bank.openClosest();
        if (Dialogues.inDialogue()) {
            while (Dialogues.inDialogue()) {
                Dialogues.continueDialogue();
            }
            Bank.openClosest();
        }
        sleepUntil(() -> Bank.isOpen(), 10000);
        Bank.depositAllExcept("Coins");
        RandomizedSleep();
        Bank.close();
        RandomizedSleep();

        // Start buying items
        NPCs.closest("Grand Exchange Clerk").interact("Exchange");
        sleepUntil(() -> GrandExchange.isOpen(), 10000);

        GEAction(true, leatherItemName, leatherRequired, 500);
        GEAction(true, needleItemName, needlesRequired, 500);
        GEAction(true, threadItemName, threadRequired, 500);
        GrandExchange.close();

        // Bought all items required for training until Level 5
    }

    private void CraftGloves() {
        Inventory.get(needleItemName).interact("Use");
        RandomizedSleep();
        Inventory.get("Leather").interact();
        sleep(3000);
        Dialogues.typeOption(1);
        sleep(2000);
    }

    private void Train() {
        Bank.openClosest();
        sleepUntil(() -> Bank.isOpen(), 10000);
        Bank.depositAllExcept(new String[]{"Thread", "Needle"});
        RandomizedSleep();

        if (!Inventory.contains(threadItemName)) {
            Bank.withdrawAll(threadItemName);
            RandomizedSleep();
        }
        if (!Inventory.contains(needleItemName)) {
            Bank.withdrawAll(needleItemName);
            RandomizedSleep();
        }
        if (!Inventory.contains(leatherItemName)) {
            Bank.withdrawAll(leatherItemName);
            RandomizedSleep();
        }
        Bank.close();
        RandomizedSleep();

        // Make leather gloves
        CraftGloves();
        sleepUntil(() -> Inventory.count(leatherItemName) <= 0 || Dialogues.inDialogue(), 60000);
        if (Dialogues.inDialogue()) {
            Dialogues.continueDialogue();
            CraftGloves();
        }
    }

    private void BuyFurnaceItems() {
        Bank.openClosest();
        sleepUntil(() -> Bank.isOpen(), 10000);
        Bank.depositAllItems();
        RandomizedSleep();
        Bank.withdrawAll("Coins");
        RandomizedSleep();
        Bank.close();

        NPCs.closest("Grand Exchange Clerk").interact("Exchange");
        sleepUntil(() -> GrandExchange.isOpen(), 10000);
        GEAction(true, ringMouldItemName, 1, 1000);
        GEAction(true, necklaceMouldItemName, 1, 1000);
        GEAction(true, goldBarItemName, goldBarsRequiredUntil22, 150);
        GrandExchange.close();
    }

    // TRADE OVER MONEY TO BOT FIRST
    public void Setup() {
        if (Inventory.get("Coins").getAmount() >= minCashRequirement) {
            inTraining = true;

            // Start by walking to the Grand Exchange to buy supplies
            WalkToArea(grandExchangeArea);

            // Walks to the Grand Exchange and buys the required starting items for training
            if (Skills.getRealLevel(Skill.CRAFTING) < 5) {
                BuyItems();
            }
            while (Skills.getRealLevel(Skill.CRAFTING) < 5) {
                Train();
            }

            // Level 5 achieved, buy items required for remaining levels (5-99)
            BuyFurnaceItems();

            // Walk to Edgeville bank
            WalkToArea(edgevilleBankArea);
            sleepUntil(() -> edgevilleBankArea.contains(getLocalPlayer()), 300000);
            NecklaceCrafter.isEligible = true;
            inTraining = false;
        } else {
            log("Not enough gold was found in inventory, exiting!!");
            currentBotTask = "Not enough gold was found in inventory, exiting!! Min amount: " + minCashRequirement;
            sleep(5000);
            stop();
        }
    }

    @Override
    public void onPaint(Graphics2D g) {
        if (currentBotTask != null && inTraining) {
            Font font = new Font("Times New Roman", 1, 75);
            g.setFont(font);
            g.drawString("Current bot task: " + currentBotTask, 25, 500);
        }
    }

    @Override
    public int onLoop() {
        return 0;
    }
}
