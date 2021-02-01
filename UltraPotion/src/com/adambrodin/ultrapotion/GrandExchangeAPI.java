package com.adambrodin.ultrapotion;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

public class GrandExchangeAPI {
    private static final long HALF_HOUR = 1800000;
    private static final String url = "https://rsbuddy.com/exchange/summary.json";
    private static long cacheTime;
    private static String cache = null;

    public static PricedItem getPricedItem(int id) {
        if (shouldUpdate())
            fetchJSON();
        JsonObject query = new JsonParser().parse(cache).getAsJsonObject().getAsJsonObject(Integer.toString(id));
        Gson gson = new Gson();
        return gson.fromJson(query, PricedItem.class);

    }

    private static boolean shouldUpdate() {
        if (cache == null)
            return true;
        else if (System.currentTimeMillis() - cacheTime > HALF_HOUR)
            return true;
        return false;
    }

    private static void fetchJSON() {
        try (BufferedReader rd = new BufferedReader(new InputStreamReader(new URL(url).openConnection().getInputStream()))) {
            StringBuilder totalString = new StringBuilder();
            String line;
            while ((line = rd.readLine()) != null) {
                totalString.append(line);
            }
            cacheTime = System.currentTimeMillis();
            cache = totalString.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public class PricedItem {
        private int id, sp, buy_average, buy_quantity, sell_average, sell_quantity, overall_average, overall_quantity;
        private String name;
        private boolean members;

        public PricedItem() {
        }

        public int getShopPrice() {
            return sp;
        }

        public void setShopPrice(int sp) {
            this.sp = sp;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public int getBuyAverage() {
            return buy_average;
        }

        public void setBuyAverage(int buyAverage) {
            this.buy_average = buyAverage;
        }

        public int getBuyQuantity() {
            return buy_quantity;
        }

        public void setBuyQuantity(int buy_quantity) {
            this.buy_quantity = buy_quantity;
        }

        public int getSellAverage() {
            return sell_average;
        }

        public void setSellAverage(int sellAverage) {
            this.sell_average = sellAverage;
        }

        public int getSellQuantity() {
            return sell_quantity;
        }

        public void setSellQuantity(int sellQuantity) {
            this.sell_quantity = sellQuantity;
        }

        public int getOverallAverage() {
            return overall_average;
        }

        public void setOverallAverage(int overallAverage) {
            this.overall_average = overallAverage;
        }

        public int getOverallQuantity() {
            return overall_quantity;
        }

        public void setOverallQuantity(int overallQuantity) {
            this.overall_quantity = overallQuantity;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public boolean isMembers() {
            return members;
        }

        public void setMembers(boolean members) {
            this.members = members;
        }
    }

}