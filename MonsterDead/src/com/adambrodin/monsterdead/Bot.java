package com.adambrodin.monsterdead;

import org.dreambot.api.Client;
import org.dreambot.api.data.GameState;
import org.dreambot.api.methods.Calculations;
import org.dreambot.api.methods.combat.Combat;
import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.interactive.GameObjects;
import org.dreambot.api.methods.interactive.NPCs;
import org.dreambot.api.methods.skills.Skill;
import org.dreambot.api.methods.skills.Skills;
import org.dreambot.api.methods.tabs.Tab;
import org.dreambot.api.methods.tabs.Tabs;
import org.dreambot.api.script.AbstractScript;
import org.dreambot.api.script.Category;
import org.dreambot.api.script.ScriptManifest;

@ScriptManifest(category = Category.COMBAT, name = "MonsterDead", description = "Kills monsters ruthlessly.", author = "Adam Brodin", version = 1.0)
public class Bot extends AbstractScript {
    private String npcToAttack = "Kurask";

    private int kills;

    @Override
    public void onStart() {
        sleepUntil(() -> Client.getGameState() == GameState.LOGGED_IN, 30000); // Wait until the player is logged to function properly
        sleepUntil(() -> NPCs.closest(npcToAttack) != null, 300000);
    }

    @Override
    public int onLoop() {
        if (!getLocalPlayer().isInCombat() && getLocalPlayer().canAttack()) {
            NPCs.closest(npcToAttack).interact("Attack");
            log("Attacking " + npcToAttack);
        }

        if (Skills.getBoostedLevels(Skill.PRAYER) <= 10) {
            if (!Tabs.isOpen(Tab.INVENTORY)) {
                Tabs.open(Tab.INVENTORY);
                sleepUntil(() -> Tabs.isOpen(Tab.INVENTORY), 5000);
            }

            if (Inventory.contains("Prayer potion(4)")) {
                Inventory.get("Prayer potion(4)").interact("Drink");
                log("Drinking prayer potion");
            } else if (Inventory.contains("Prayer potion(3)")) {
                Inventory.get("Prayer potion(3)").interact("Drink");
                log("Drinking prayer potion");
            } else if (Inventory.contains("Prayer potion(2)")) {
                Inventory.get("Prayer potion(2)").interact("Drink");
                log("Drinking prayer potion");
            } else if (Inventory.contains("Prayer potion(1)")) {
                Inventory.get("Prayer potion(1)").interact("Drink");
                log("Drinking prayer potion");
            }
        }

        /*if (Combat.getSpecialPercentage() >= 50) {
            Combat.toggleSpecialAttack(true);
            log("Speccing!");
            sleep(500);
        }*/

        sleepUntil(() -> getLocalPlayer().isInCombat(), 2000);
        sleepUntil(() -> !getLocalPlayer().isInCombat(), 30000);
        kills++;

        if (kills >= Calculations.random(3, 7)) {
            try {
                GameObjects.closest(6).interact("Fire");
            } catch (Exception e) {
            }

            try {
                GameObjects.closest(14916).interact("Repair");
            } catch (Exception e) {
            }
            sleep(2000);
            kills = 0;
        }

        return 0;
    }
}
