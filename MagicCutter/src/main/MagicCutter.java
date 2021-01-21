package main;
import org.dreambot.api.methods.Calculations;
import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.container.impl.bank.Bank;
import org.dreambot.api.methods.interactive.GameObjects;
import org.dreambot.api.methods.map.Area;
import org.dreambot.api.methods.walking.impl.Walking;
import org.dreambot.api.methods.widget.Widgets;
import org.dreambot.api.script.AbstractScript;
import org.dreambot.api.script.Category;
import org.dreambot.api.script.ScriptManifest;
import org.dreambot.api.wrappers.interactive.GameObject;
import org.dreambot.api.wrappers.widgets.WidgetChild;
import javax.imageio.ImageIO;
import java.awt.*;
import java.io.IOException;
import java.net.URL;

@ScriptManifest(category = Category.WOODCUTTING, name = "MagicCutter", description = "Cuts magic logs in the Woodcutting Guild.", author = "Adam Brodin", version = 1.0)
public class MagicCutter extends AbstractScript {
    // Variables
    private String currentBotTask;
    private final int standardSleepMin = 1000;
    private final int standardSleepMax = 2500;
    private final int birdNestMaxDist = 5;
    private final int newTreeSleep = 10000;
    private int logsObtainedSession;
    private int birdNestsSession;
    private int logsInBank;
    private long startTime;
    private boolean readyToBot = false;
    private Image rectImage;
    private final Area bankArea = new Area(1590, 3476, 1592, 3476, 0);

    @Override
    public void onStart() {
        // TODO check for axe, empty inventory, correct location, woodcutting level
        startTime = System.currentTimeMillis();
        rectImage = getImage("https://i.imgur.com/utiuN6I.png");
        readyToBot = true;
    }

    private GameObject FindTree() {
        currentBotTask = "FINDING A SUITABLE TREE";
        return GameObjects.closest("Magic tree");
    }

    private void ItemCheck() {
        // If a bird nest is found nearby
        if (GameObjects.all().contains("Bird nest")) {
            GameObject birdNest = GameObjects.closest("Bird nest");

            // Making sure the bird nest isn't too far away (would seem suspicious)
            if (birdNest.distance() <= birdNestMaxDist) {
                currentBotTask = "FOUND A BIRD NEST";

                if (Inventory.isFull()) {
                    currentBotTask = "MAKING ROOM IN INVENTORY";
                    Inventory.drop("Magic logs");
                }

                currentBotTask = "PICKING UP BIRD NEST";
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
        currentBotTask = "WALKING TO BANK AREA";

        // Finds the closest bank available and opens it
        sleepUntil(Bank::isOpen, 10000);

        // Deposits everything except the tool required (axe)
        Bank.depositAllExcept("Dragon axe");
        sleep(Calculations.random(standardSleepMin, standardSleepMax));
        logsInBank = Bank.count("Magic logs");
        Bank.close();
        sleep(Calculations.random(standardSleepMin, standardSleepMax));
    }

    private void CutTree() {
        GameObject tree = FindTree();
        int logsInInv = Inventory.count("Magic logs");
        if (tree != null) {
            tree.interact("Chop down");
            currentBotTask = "CHOPPING MAGIC TREE";

            // Sleeps until the tree has disappeared
            while (getLocalPlayer().isAnimating() && !Inventory.isFull()) {
                if ((Inventory.count("Magic logs") - logsInInv) > 0) {
                    logsObtainedSession += (Inventory.count("Magic logs") - logsInInv);
                    logsInInv = Inventory.count("Magic logs");
                }
                if (GameObjects.all().contains("Bird nest") && GameObjects.closest("Bird nest").distance() <= birdNestMaxDist) {
                    break;
                }
            }
        }

        if (!tree.exists()) {
            currentBotTask = "TREE DEPLETED";
        }
        // Waits before continuing (anti-ban)
        sleep(Calculations.random(newTreeSleep / 2, newTreeSleep));
    }

    private void BotLoop() {
        // Find a suitable tree & start cutting
        CutTree();

        // Checks if the inventory is full & looks for bird nests
        ItemCheck();
    }

    @Override
    public int onLoop() {
        if (readyToBot) {
            BotLoop();
        }
        return 0;
    }

    private Image getImage(String url) {
        try {
            return ImageIO.read(new URL(url));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private long CountTime() {
        return System.currentTimeMillis() - startTime;
    }

    private String SecondsToHms(int seconds) {
        final int s = seconds % 60;
        final int m = (seconds % 3600) / 60;
        final int h = seconds / 3600;

        return h + "h:" + m + "m:" + s + "s";
    }

    @Override
    public void onPaint(Graphics2D g) {
        Font font = new Font("Consolas", Font.PLAIN, 20);
        WidgetChild chatBoxWidget = Widgets.getWidgetChild(162, 0);
        int x = chatBoxWidget.getX();
        int y = chatBoxWidget.getY();
        int width = chatBoxWidget.getWidth();
        int height = chatBoxWidget.getHeight();
        g.drawImage(rectImage, x, y, width, height, null);

        g.setColor(Color.WHITE);
        g.setFont(font);
        if (currentBotTask != null) {
            g.drawString("CURRENT TASK: " + currentBotTask, x + 5, y + 25);
            g.drawString("LOGS OBTAINED: " + logsObtainedSession + " - BIRD NESTS OBTAINED: " + birdNestsSession, x + 5, y + 75);
            g.drawString("LOGS IN BANK: " + (logsInBank > 0 ? logsInBank : "BANK NOT OPENED YET"), x+5, y+100);
            g.drawString("TIME ELAPSED: " + SecondsToHms((int) (CountTime() / 1000)), x + 5, y + 150);
        } else {
            g.drawString("MAGICCUTTER V" + this.getManifest().version() + " BY " + this.getManifest().author(), x + 5, y + 85);
        }
    }
}
