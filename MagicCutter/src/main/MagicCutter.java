package main;

// Imports

import org.dreambot.api.methods.Calculations;
import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.container.impl.bank.Bank;
import org.dreambot.api.methods.interactive.GameObjects;
import org.dreambot.api.methods.map.Area;
import org.dreambot.api.methods.walking.impl.Walking;
import org.dreambot.api.script.AbstractScript;
import org.dreambot.api.script.Category;
import org.dreambot.api.script.ScriptManifest;
import org.dreambot.api.wrappers.interactive.GameObject;

import java.awt.*;

@ScriptManifest(category = Category.WOODCUTTING, name = "MagicCutter", description = "Cuts magic logs in the Woodcutting Guild.", author = "Adam Brodin", version = 1.0)
public class MagicCutter extends AbstractScript {
    // Variables
    private String currentBotTask;
    private int treeTimeout = 240000, standardSleepMin = 1000, standardSleepMax = 2500, birdNestMaxDist = 5, newTreeSleep = 12500;
    private int previousInvTrees, logsObtainedSession, birdNestsSession, timeElapsedSeconds;
    private boolean readyToBot = false;
    private Area bankArea = new Area(1590, 3476, 1592, 3476, 0);

    @Override
    public void onStart() {
        // TODO check for axe, empty inventory, correct location, woodcutting level
        readyToBot = true;
    }

    private GameObject FindTree() {
        currentBotTask = "Finding a suitable tree";
        GameObject magicTree = GameObjects.closest("Magic tree");
        return magicTree;
    }

    private void ItemCheck() {
        // If a bird nest is found nearby
        if (GameObjects.all().contains("Bird nest")) {
            GameObject birdNest = GameObjects.closest("Bird nest");

            // Making sure the bird nest isn't too far away (would seem suspicious)
            if (birdNest.distance() <= birdNestMaxDist) {
                currentBotTask = "Found a bird nest";

                if (Inventory.isFull()) {
                    currentBotTask = "Making room in inventory";
                    Inventory.drop("Magic logs");
                }

                currentBotTask = "Picking up bird nest";
                birdNest.interact("Take");
                birdNestsSession++;
                sleep(Calculations.random(standardSleepMin, standardSleepMax));
            }
        }

        if (Inventory.isFull()) {
            BankItems();
        }
    }

    private void BankItems() {
        // Moves towards the bank area
        if (!Walking.isRunEnabled()) {
            Walking.toggleRun();
        }
        Walking.walk(bankArea.getRandomTile());
        sleepUntil(() -> bankArea.contains(getLocalPlayer()), 7500);
        
        GameObjects.closest("Bank chest").interact("Use");
        currentBotTask = "Walking to bank area";

        // Finds the closest bank available and opens it
        sleepUntil(() -> Bank.isOpen(), 10000);

        // Deposits everything except the tool required (axe)
        Bank.depositAllExcept("Dragon axe");
        previousInvTrees = 0;
        sleep(Calculations.random(standardSleepMin, standardSleepMax));
        Bank.close();
        sleep(Calculations.random(standardSleepMin, standardSleepMax));
    }

    private void CutTree() throws InterruptedException {
        GameObject tree = FindTree();
        if (tree != null) {
            tree.interact("Chop down");
            currentBotTask = "Chopping tree";

            // Sleeps until the tree has disappeared
            while (tree.exists()) {
                if (Inventory.isFull()) {
                    break;
                }
                int logsInInv = Inventory.count("Magic logs");
                sleepUntil(() -> (Inventory.count("Magic logs") > logsInInv) || !getLocalPlayer().isAnimating() || (GameObjects.all().contains("Bird nest") && GameObjects.closest("Bird nest").distance() <= birdNestMaxDist), Calculations.random(treeTimeout / 2, treeTimeout));
                if ((Inventory.count("Magic logs") - logsInInv) > 0) {
                    logsObtainedSession += (Inventory.count("Magic logs") - logsInInv);
                }
            }
        }

        // Waits before continuing (anti-ban)
        sleep(Calculations.random(newTreeSleep / 2, newTreeSleep));
    }

    private void BotLoop() throws InterruptedException {
        // Find a suitable tree & start cutting
        CutTree();

        // Checks if the inventory is full & looks for bird nests
        ItemCheck();
    }

    @Override
    public int onLoop() {
        try {
            if (readyToBot) {
                BotLoop();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public void onPaint(Graphics2D g) {
        Font font = new Font("Times New Roman", 1, 35);
        g.drawString("Current Bot Task: " + currentBotTask, 100, 500);
        g.drawString("Logs obtained in session: " + logsObtainedSession, 100, 525);
        g.drawString("Bird nest(s) obtained in session: " + birdNestsSession, 100, 550);
    }
}
