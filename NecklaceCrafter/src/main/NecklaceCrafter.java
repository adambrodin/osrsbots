package main;

import org.dreambot.api.methods.Calculations;
import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.container.impl.bank.Bank;
import org.dreambot.api.methods.container.impl.bank.BankMode;
import org.dreambot.api.methods.dialogues.Dialogues;
import org.dreambot.api.methods.grandexchange.GrandExchange;
import org.dreambot.api.methods.interactive.GameObjects;
import org.dreambot.api.methods.interactive.NPCs;
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
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;

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
    private final FurnaceItem[] furnaceItems = new FurnaceItem[]
            {
                    new FurnaceItem(0, 0, 5, new int[]{446, 7}, "Gold ring"),
                    new FurnaceItem(1607, 400, 22, new int[]{446, 22}, "Sapphire necklace"),
                    new FurnaceItem(1605, 600, 29, new int[]{446, 23}, "Emerald necklace"),
                    new FurnaceItem(1603, 1000, 40, new int[]{446, 24}, "Ruby necklace"),
            };

    private long startTime;
    private final int randMinPause = 510;
    private final int randMaxPause = 750;
    private int furnaceItemToCraft;
    private int jewelleryMade;
    public static boolean isEligible = false;

    private String currentBotTask;
    private String mouldItem = "Necklace mould";
    private final String goldBarItem = "Gold bar";
    private Image rectImage;

    private Image getImage(String url) {
        try {
            return ImageIO.read(new URL(url));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
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
            g.drawString("JEWELLERY MADE: " + jewelleryMade, x + 5, y + 75);
            g.drawString("TIME ELAPSED: " + SecondsToHms((int) (CountTime() / 1000)), x + 5, y + 125);
        } else {
            g.drawString("NECKLACECRAFTER V" + this.getManifest().version() + " BY " + this.getManifest().author(), x + 5, y + 85);
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

        if (Bank.count(goldBarItem) <= 13 || Bank.count(furnaceItems[furnaceItemToCraft].itemNeededID) <= 13) {
            int goldBarsInBank = Bank.count(goldBarItem);
            Bank.depositAllItems();
            RandomizedSleep();

            Bank.setWithdrawMode(BankMode.NOTE);
            RandomizedSleep();

            for (int i = 0; i <= furnaceItems.length - 1; i++) {
                Bank.withdrawAll(furnaceItems[i].itemName);
                RandomizedSleep();
                if (i > 0) {
                    Bank.withdrawAll(furnaceItems[i - 1].itemNeededID);
                }
            }

            Bank.setWithdrawMode(BankMode.ITEM);
            RandomizedSleep();
            Bank.withdrawAll("Coins");
            RandomizedSleep();
            Bank.close();

            // Open the Grand Exchange
            NPCs.closest("Grand Exchange Clerk").interact("Exchange");
            sleepUntil(GrandExchange::isOpen, 10000);

            for (int i = 0; i <= furnaceItems.length - 1; i++) {
                if (Inventory.contains(furnaceItems[i].itemName)) {
                    setup.GEAction(false, furnaceItems[i].itemName, Inventory.count(furnaceItems[i].itemName), 1);
                    RandomizedSleep();
                }
                if (i > 0 && Inventory.contains(furnaceItems[i - 1].itemNeededID)) {
                    setup.GEAction(false, furnaceItems[furnaceItemToCraft - 1].itemName.substring(0, furnaceItems[furnaceItemToCraft - 1].itemName.indexOf(" ")), Inventory.count(furnaceItems[i - 1].itemName), 1);
                    RandomizedSleep();
                }
            }

            int suppliesToBuy;
            if (furnaceItems[furnaceItemToCraft].itemNeededID == 0) {
                suppliesToBuy = 200;
            } else {
                suppliesToBuy = ((Inventory.get("Coins").getAmount()) / furnaceItems[furnaceItemToCraft].itemNeededPrice);
            }

            int goldBarPrice = 100; // TODO FETCH PRICE
            setup.GEAction(true, goldBarItem, (Math.max(suppliesToBuy - goldBarsInBank, 0)), goldBarPrice);

            if (furnaceItems[furnaceItemToCraft].itemNeededID != 0) {
                suppliesToBuy = ((Inventory.get("Coins").getAmount()) / furnaceItems[furnaceItemToCraft].itemNeededPrice);
                setup.GEAction(true, furnaceItems[furnaceItemToCraft].itemName.substring(0, furnaceItems[furnaceItemToCraft].itemName.indexOf(" ")), suppliesToBuy, furnaceItems[furnaceItemToCraft].itemNeededPrice);
            }

            GrandExchange.close();
            sleep(2000);
            Bank.openClosest();
            sleepUntil(Bank::isOpen, 10000);
            sleep(2000);
            Bank.depositAllItems();
            sleep(2000);
            Bank.close();
        }

        currentBotTask = "WALKING TO EDGEVILLE";
        setup.WalkToArea(setup.edgevilleBankArea);
    }

    private void BankItems() {
        CalculateOptimalNecklace();

        // Walks to the Edgeville bank area
        if (!Walking.isRunEnabled()) {
            Walking.toggleRun();
        }

        if (!Inventory.contains(goldBarItem) && !Inventory.contains(furnaceItems[furnaceItemToCraft].itemNeededID)) {
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
        }

        Bank.close();
    }

    private long CountTime() {
        return System.currentTimeMillis() - startTime;
    }

    @Override
    public void onStart() {
        startTime = System.currentTimeMillis();
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
                sleepUntil(() -> FurnaceInterfaceVisible(Widgets.getWidgetChild(446, 1, 1)), 20000);
                RandomizedSleep();
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

    private void BotLoop() {
        BankItems();
        FurnaceInteract();
    }

    @Override
    public int onLoop() {
        if (isEligible) {
            BotLoop();
        }
        return 0;
    }
}
