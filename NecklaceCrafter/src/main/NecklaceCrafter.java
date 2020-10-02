package main;

import org.dreambot.api.methods.Calculations;
import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.container.impl.bank.Bank;
import org.dreambot.api.methods.container.impl.bank.BankMode;
import org.dreambot.api.methods.dialogues.Dialogues;
import org.dreambot.api.methods.grandexchange.GrandExchange;
import org.dreambot.api.methods.interactive.GameObjects;
import org.dreambot.api.methods.interactive.NPCs;
import org.dreambot.api.methods.map.Area;
import org.dreambot.api.methods.skills.Skill;
import org.dreambot.api.methods.skills.Skills;
import org.dreambot.api.methods.walking.impl.Walking;
import org.dreambot.api.methods.widget.Widgets;
import org.dreambot.api.script.AbstractScript;
import org.dreambot.api.script.Category;
import org.dreambot.api.script.ScriptManifest;
import org.dreambot.api.wrappers.interactive.GameObject;
import org.dreambot.api.wrappers.widgets.WidgetChild;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.ImageObserver;
import java.io.IOException;
import java.net.URL;

class FurnaceItem {
    int itemNeededID, itemNeededPrice, levelRequirement;
    int[] furnaceWidgetID;
    String itemName;

    public FurnaceItem(int itemNeededID, int itemNeededPrice, int levelRequirement, int[] furnaceWidgetID, String itemName) {
        this.itemNeededID = itemNeededID;
        this.itemNeededPrice = itemNeededPrice;
        this.levelRequirement = levelRequirement;
        this.furnaceWidgetID = furnaceWidgetID;
        this.itemName = itemName;
    }
}

@ScriptManifest(category = Category.CRAFTING, name = "NecklaceCrafter", description = "Crafts F2P necklaces using the Edgeville furnace.", author = "Adam Brodin", version = 1.0)
public class NecklaceCrafter extends AbstractScript {
    // Variables
    private FurnaceItem[] furnaceItems = new FurnaceItem[]
            {
                    new FurnaceItem(0, 0, 5, new int[]{446, 7}, "Gold ring"),
                    new FurnaceItem(1607, 400, 22, new int[]{446, 22}, "Sapphire necklace"),
                    new FurnaceItem(1605, 600, 29, new int[]{446, 23}, "Emerald necklace"),
                    new FurnaceItem(1603, 1000, 40, new int[]{446, 24}, "Ruby necklace"),
            };

    private int randMinPause = 510, randMaxPause = 750;
    private int furnaceItemToCraft, jewelleryMade, gpProfitSession, goldBarPrice = 100;
    public static boolean isEligible = false;

    private Area furnaceArea = new Area(3109, 3499, 3109, 3499, 0);
    private String currentBotTask, mouldItem = "Necklace mould", goldBarItem = "Gold bar", combineItem = "Sapphire";
    private Image rectImage;

    private Image getImage(String url) {
        try {
            return ImageIO.read(new URL(url));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
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
            g.drawString("JEWELLERY MADE: " + jewelleryMade, x + 5, y + 75);
        }
    }

    private void RandomizedSleep() {
        sleep(Calculations.random(randMinPause, randMaxPause));
        currentBotTask = "IDLING";
    }

    private void CalculateOptimalNecklace() {
        int craftingLevel = Skills.getRealLevel(Skill.CRAFTING);
        for (int i = furnaceItems.length - 1; i >= 0; i--) {
            if (craftingLevel >= furnaceItems[i].levelRequirement) {
                furnaceItemToCraft = i;
                if (furnaceItems[i].itemName.contains("ring")) {
                    mouldItem = "Ring mould";
                } else {
                    mouldItem = "Necklace mould";
                }
                break;
            }
        }
    }

    private void RestockItems() {
        // Walks to the Grand Exchange
        BotSetup setup = new BotSetup();
        currentBotTask = "WALKING TO THE GRAND EXCHANGE";
        setup.WalkToArea(setup.grandExchangeArea);

        NPCs.closest("Banker").interact("Bank");
        sleepUntil(Bank::isOpen, 5000);
        CalculateOptimalNecklace();
        int goldBarsInBank = Bank.count(goldBarItem);
        Bank.depositAllItems();
        RandomizedSleep();

        if (Bank.contains(furnaceItems)) {
            Bank.setWithdrawMode(BankMode.NOTE);
            RandomizedSleep();

            for (int i = 0; i <= furnaceItems.length - 1; i++) {
                Bank.withdrawAll(furnaceItems[i].itemName);
                RandomizedSleep();
            }

            Bank.setWithdrawMode(BankMode.ITEM);
            RandomizedSleep();
        }
        Bank.withdrawAll("Coins");
        RandomizedSleep();
        Bank.close();

        // Open the Grand Exchange
        NPCs.closest("Grand Exchange Clerk").interact("Exchange");
        sleepUntil(() -> GrandExchange.isOpen(), 10000);

        for (int i = 0; i <= furnaceItems.length - 1; i++) {
            if (Inventory.contains(furnaceItems[i].itemName)) {
                setup.GEAction(false, furnaceItems[i].itemName, Inventory.count(furnaceItems[i].itemName), 1);
                RandomizedSleep();
            }
        }

        int suppliesToBuy = 100;
        if (furnaceItems[furnaceItemToCraft].itemNeededID == 0) {
            suppliesToBuy = 200;
        } else {
            suppliesToBuy = ((Inventory.get("Coins").getAmount()) / furnaceItems[furnaceItemToCraft].itemNeededPrice);
        }

        setup.GEAction(true, goldBarItem, (suppliesToBuy - goldBarsInBank > 0 ? suppliesToBuy - goldBarsInBank : 0), goldBarPrice);

        if (furnaceItems[furnaceItemToCraft].itemNeededID != 0) {
            suppliesToBuy = ((Inventory.get("Coins").getAmount()) / furnaceItems[furnaceItemToCraft].itemNeededPrice);
            setup.GEAction(true, furnaceItems[furnaceItemToCraft].itemName.substring(0, furnaceItems[furnaceItemToCraft].itemName.indexOf(" ")), suppliesToBuy, furnaceItems[furnaceItemToCraft].itemNeededPrice);
        }

        GrandExchange.close();
        sleep(2000);
        Bank.openClosest();
        sleepUntil(() -> Bank.isOpen(), 10000);
        sleep(2000);
        Bank.depositAllItems();
        sleep(2000);
        Bank.close();

        setup.WalkToArea(setup.edgevilleBankArea);
    }

    private void BankItems() {
        CalculateOptimalNecklace();

        // Walks to the Edgeville bank area
        if (!Walking.isRunEnabled()) {
            Walking.toggleRun();
        }
        GameObjects.closest("Bank booth").interact("Bank");
        currentBotTask = "WALKING TO THE BANK";

        // Deposits all unnecessary items and withdraws the required ones
        sleepUntil(Bank::isOpen, 10000);

        if (Bank.isOpen()) {
            currentBotTask = "BANKING ITEMS";
            if (Bank.count(goldBarItem) >= 13) {
                if (furnaceItems[furnaceItemToCraft].itemNeededID == 0 || Bank.count(furnaceItems[furnaceItemToCraft].itemNeededID) >= 13) {
                    Bank.depositAllExcept(mouldItem);
                    if (!Inventory.contains(mouldItem)) {
                        if (Bank.contains(mouldItem)) {
                            Bank.depositAllItems();
                            RandomizedSleep();
                            Bank.withdraw(mouldItem);
                        } else {
                            currentBotTask = "NO MOULD WAS FOUND, EXITING!!!";
                            sleep(10000);
                            stop();
                        }
                    }

                    RandomizedSleep();
                    if (furnaceItems[furnaceItemToCraft].itemNeededID == 0) {
                        Bank.withdrawAll(goldBarItem);
                    } else {
                        Bank.withdraw(goldBarItem, 13);
                    }
                    RandomizedSleep();
                    if (furnaceItems[furnaceItemToCraft].itemNeededID != 0) {
                        Bank.withdraw(furnaceItems[furnaceItemToCraft].itemNeededID, 13);
                        RandomizedSleep();
                    }
                } else {
                    RestockItems();
                }
            } else {
                RestockItems();
            }
        }

        Bank.close();
    }

    @Override
    public void onStart() {
        rectImage = getImage("https://i.imgur.com/utiuN6I.png");
        BotSetup trainer = new BotSetup();

        if (Skills.getRealLevel(Skill.CRAFTING) < 5) {
            trainer.Setup();
        } else {
            if (getLocalPlayer().distance(trainer.grandExchangeArea.getCenter()) < 30) {
                RestockItems();
            } else if (getLocalPlayer().distance(trainer.edgevilleBankArea.getCenter()) >= 10) {
                trainer.WalkToArea(trainer.edgevilleBankArea);
            }

            isEligible = true;
            CalculateOptimalNecklace();
        }
    }

    private boolean FurnaceInterfaceVisible(WidgetChild w) {
        return w != null && w.isVisible();
    }

    private void FurnaceInteract() {
        if (Inventory.contains(goldBarItem)) {
            if (furnaceItems[furnaceItemToCraft].itemNeededID == 0 || Inventory.contains(furnaceItems[furnaceItemToCraft].itemNeededID)) {
                GameObject furnace = GameObjects.closest("Furnace");
                furnace.interact("Smelt");
                currentBotTask = "OPENING FURNACE INTERFACE";

                // Waits until the interface is opened
                while (!FurnaceInterfaceVisible(Widgets.getWidgetChild(446, 1, 1))) {
                    RandomizedSleep();
                }
                Widgets.getWidgetChild(furnaceItems[furnaceItemToCraft].furnaceWidgetID[0], furnaceItems[furnaceItemToCraft].furnaceWidgetID[1]).interact();
                currentBotTask = "SMELTING JEWELLERY";

                sleepUntil(() -> (!Inventory.contains(goldBarItem) || Dialogues.inDialogue()), 45000);
                if (Dialogues.inDialogue() && Dialogues.canContinue()) {
                    currentBotTask = "IN DIALOGUE";
                    Dialogues.continueDialogue();

                    if (Inventory.contains(goldBarItem)) {
                        FurnaceInteract();
                    }
                }

                jewelleryMade += Inventory.count(furnaceItems[furnaceItemToCraft].itemName);
            }
        }
    }

    private void MakeAmulets() {
        FurnaceInteract();
    }

    private void BotLoop() {
        BankItems();
        MakeAmulets();
    }

    @Override
    public int onLoop() {
        if (isEligible) {
            BotLoop();
        }
        return 0;
    }
}
