package com.adambrodin.ultrapotion;

public class PotionLevel {
    public int levelRequirement;
    public int[] combineItemIds;
    public String[] combineItemNames;
    public int potionsRequiredUntilNext;
    public int craftedPotionID;
    public String craftedPotionName; // CASE-SENSITIVE

    public PotionLevel(int levelRequirement, int[] combineItemIds, String[] combineItemNames, int craftedPotionID, int potionsRequiredUntilNext, String craftedPotionName) {
        this.levelRequirement = levelRequirement;
        this.combineItemIds = combineItemIds;
        this.combineItemNames = combineItemNames;
        this.craftedPotionID = craftedPotionID;
        this.potionsRequiredUntilNext = potionsRequiredUntilNext;
        this.craftedPotionName = craftedPotionName;
    }

    public PotionLevel(int levelRequirement, int[] combineItemIds, String[] combineItemNames, int craftedPotionID, String craftedPotionName) {
        this.levelRequirement = levelRequirement;
        this.combineItemIds = combineItemIds;
        this.combineItemNames = combineItemNames;
        this.craftedPotionID = craftedPotionID;
        this.craftedPotionName = craftedPotionName;
    }
}
