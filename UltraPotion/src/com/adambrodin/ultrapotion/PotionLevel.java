package com.adambrodin.ultrapotion;

public class PotionLevel {
    public int levelRequirement;
    public int[] combineItemIds;
    public String[] combineItemNames;
    public int potionsRequiredUntilNext;
    public int craftedPotionID;
    public String craftedPotionName; // CASE-SENSITIVE
    public int backupCombinePrices[];
    public int backupFinishedPotionPrice;

    public PotionLevel(int levelRequirement, int[] combineItemIds, String[] combineItemNames, int craftedPotionID, int potionsRequiredUntilNext, String craftedPotionName, int[] backupCombinePrices, int backupFinishedPotionPrice) {
        this.levelRequirement = levelRequirement;
        this.combineItemIds = combineItemIds;
        this.combineItemNames = combineItemNames;
        this.craftedPotionID = craftedPotionID;
        this.potionsRequiredUntilNext = potionsRequiredUntilNext;
        this.craftedPotionName = craftedPotionName;
        this.backupCombinePrices = backupCombinePrices;
        this.backupFinishedPotionPrice = backupFinishedPotionPrice;
    }
}
