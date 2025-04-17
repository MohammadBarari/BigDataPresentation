
package org.example;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class SalesAnalyzer {

    static class Sale {
        public String productName;
        public int quantity;
        public String saleDate;
    }

    public static void main(String[] args) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        List<Sale> sales = mapper.readValue(
            new File(SalesAnalyzer.class.getClassLoader().getResource("sample_sales_data.json").getFile()),
            new TypeReference<>() {}
        );

        Map<String, Integer> totalSalesPerProduct = new HashMap<>();
        Map<Month, Integer> totalSalesPerMonth = new HashMap<>();
        Map<Month, Map<String, Integer>> salesByMonth = new HashMap<>();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (Sale sale : sales) {
            totalSalesPerProduct.merge(sale.productName, sale.quantity, Integer::sum);

            LocalDate date = LocalDate.parse(sale.saleDate, formatter);
            Month month = date.getMonth();
            totalSalesPerMonth.merge(month, sale.quantity, Integer::sum);

            salesByMonth.putIfAbsent(month, new HashMap<>());
            Map<String, Integer> productSales = salesByMonth.get(month);
            productSales.merge(sale.productName, sale.quantity, Integer::sum);
        }

        printTotalPerProductPerMonth(totalSalesPerProduct);
        printTotalSalesPerMonth(totalSalesPerMonth);
        printBestSellingPerMonth(salesByMonth);
        analyzeProductTrends(sales, "Mouse");
    }

    private static void printBestSellingPerMonth(Map<Month, Map<String, Integer>> salesByMonth) {
        System.out.println("\nBest-Selling Product Per Month:");
        for (Month month : Month.values()) {
            Map<String, Integer> productSales = salesByMonth.get(month);
            if (productSales == null) continue;

            String bestProduct = Collections.max(productSales.entrySet(), Map.Entry.comparingByValue()).getKey();
            int maxSales = productSales.get(bestProduct);
            System.out.println(month + ": " + bestProduct + " (" + maxSales + " units)");
        }
    }

    private static void printTotalSalesPerMonth(Map<Month, Integer> totalSalesPerMonth) {
        System.out.println("\nTotal Sales Per Month:");
        totalSalesPerMonth.forEach((month, total) ->
            System.out.println(month + ": " + total + " units"));
    }

    private static void printTotalPerProductPerMonth(Map<String, Integer> totalSalesPerProduct) {
        System.out.println("Total Sales Per Product:");
        totalSalesPerProduct.forEach((product, total) ->
            System.out.println(product + ": " + total + " units"));
    }

    public static void analyzeProductTrends(List<Sale> sales, String targetProduct) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        //tree map for sorting
        Map<Month, Integer> monthlyTotals = new TreeMap<>();

        for (Sale sale : sales) {
            if (targetProduct != null && !sale.productName.equalsIgnoreCase(targetProduct)) {
                continue;
            }

            LocalDate date = LocalDate.parse(sale.saleDate, formatter);
            Month month = date.getMonth();
            monthlyTotals.merge(month, sale.quantity, Integer::sum);
        }

        System.out.println("\nSales Trend" + (targetProduct != null ? " for " + targetProduct : " for All Products") + ":");
        Integer previous = null;
        for (Map.Entry<Month, Integer> entry : monthlyTotals.entrySet()) {
            Month month = entry.getKey();
            int current = entry.getValue();

            if (previous == null) {
                System.out.println(month + ": " + current + " units");
            } else {
                int change = current - previous;
                double percent = (previous == 0) ? 0 : (change * 100.0) / previous;
                String trend = change > 0 ? " Increase" : change < 0 ? " Decrease" : "-> No Change";
                System.out.printf("%s: %d units (%+d, %.2f%%) %s%n", month, current, change, percent, trend);
            }
            previous = current;
        }
    }
}
