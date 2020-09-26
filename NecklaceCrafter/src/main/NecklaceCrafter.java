package main;

import org.dreambot.api.methods.Calculations;
import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.container.impl.bank.Bank;
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

import java.awt.*;

class FurnaceItem {
    int itemNeededID, levelRequirement;
    int[] furnaceWidgetID;
    String itemName;

    public FurnaceItem(int itemNeededID, int levelRequirement, int[] furnaceWidgetID, String itemName) {
        this.itemNeededID = itemNeededID;
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
                    new FurnaceItem(0, 5, new int[]{446, 7}, "Gold ring"),
                    new FurnaceItem(1607, 22, new int[]{446, 22}, "Sapphire necklace"),
                    new FurnaceItem(1605, 29, new int[]{446, 23}, "Emerald necklace"),
                    new FurnaceItem(1603, 40, new int[]{446, 24}, "Ruby necklace"),
                    new FurnaceItem(1601, 56, new int[]{446, 25}, "Diamond necklace")
            };

    private int randMinPause = 510, randMaxPause = 750;
    private int furnaceItemToCraft, jewelleryMade, gpProfitSession;
    public static boolean isEligible = false;

    private Area furnaceArea = new Area(3109, 3499, 3109, 3499, 0);
    private String currentBotTask, mouldItem = "Necklace mould", goldBarItem = "Gold bar", combineItem = "Sapphire";

    @Override
    public void onPaint(Graphics2D g) {
        Font font = new Font("Arial", Font.BOLD, 50);

        g.setFont(font);
        if (currentBotTask != null) {
            g.drawString("Current bot task: " + currentBotTask, 500, 500);
            g.drawString("Jewellery made: " + jewelleryMade, 500, 550);
        }
    }

    private void RandomizedSleep() {
        sleep(Calculations.random(randMinPause, randMaxPause));
        currentBotTask = "Idling..";
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
        currentBotTask = "Walking to the Grand Exchange";
        setup.WalkToArea(setup.grandExchangeArea);

        NPCs.closest("Banker").interact("Bank");
        RandomizedSleep();
        Bank.withdrawAll("necklace");
        RandomizedSleep();
        Bank.withdrawAll("ring");
        RandomizedSleep();
        Bank.withdrawAll("Coins");
        RandomizedSleep();
        Bank.close();

        // Open the Grand Exchange
        NPCs.closest("Grand Exchange Clerk").interact("Exchange");
        sleepUntil(() -> GrandExchange.isOpen(), 10000);

        setup.GEAction(false, "necklace", Inventory.count("necklace"), 1);
        setup.GEAction(false, "ring", Inventory.count("necklace"), 1);
        setup.GEAction(true, goldBarItem, 750, 500);
        if (furnaceItems[furnaceItemToCraft].itemNeededID != 0) {
            setup.GEAction(true, furnaceItems[furnaceItemToCraft].itemName.substring(0, furnaceItems[furnaceItemToCraft].itemName.indexOf(" ") - 1), 750, 500);
        }

        GrandExchange.close();
        RandomizedSleep();
        setup.WalkToArea(setup.edgevilleBankArea);
    }

    private void BankItems() {
        CalculateOptimalNecklace();

        // Walks to the Edgeville bank area
        if (!Walking.isRunEnabled()) {
            Walking.toggleRun();
        }
        GameObjects.closest("Bank booth").interact("Bank");
        currentBotTask = "Walking to the bank";

        // Deposits all unnecessary items and withdraws the required ones
        sleepUntil(Bank::isOpen, 10000);
        currentBotTask = "Banking items";
        if (Bank.count(goldBarItem) >= 13) {
            log("More than 13 gold bars");
            if (furnaceItems[furnaceItemToCraft].itemNeededID == 0 || Bank.count(furnaceItems[furnaceItemToCraft].itemNeededID) >= 13) {
                Bank.depositAllExcept(mouldItem);
                if (!Inventory.contains(mouldItem)) {
                    if (Bank.contains(mouldItem)) {
                        Bank.depositAllItems();
                        RandomizedSleep();
                        Bank.withdraw(mouldItem);
                    } else {
                        currentBotTask = "No mould was found, exiting!!!";
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

        Bank.close();
    }

    @Override
    public void onStart() {
        if (Skills.getRealLevel(Skill.CRAFTING) < 5) {
            BotSetup trainer = new BotSetup();
            trainer.Setup();
        } else {
            isEligible = true;
            CalculateOptimalNecklace();
        }
    }

    private void FurnaceInteract() {
        if (Inventory.contains(goldBarItem)) {
            if (furnaceItems[furnaceItemToCraft].itemNeededID == 0 || Inventory.contains(furnaceItems[furnaceItemToCraft].itemNeededID)) {
                GameObject furnace = GameObjects.closest("Furnace");
                furnace.interact("Smelt");
                currentBotTask = "Opening furnace interface";
                sleep(Calculations.random(7000, 9000));
                Widgets.getWidgetChild(furnaceItems[furnaceItemToCraft].furnaceWidgetID[0], furnaceItems[furnaceItemToCraft].furnaceWidgetID[1]).interact();
                currentBotTask = "Smelting jewellery";

                sleepUntil(() -> (!Inventory.contains(goldBarItem) || Dialogues.inDialogue()), 45000);
                if (Dialogues.inDialogue() && Dialogues.canContinue()) {
                    currentBotTask = "In dialogue";
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
