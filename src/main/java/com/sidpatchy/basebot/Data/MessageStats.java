package com.sidpatchy.basebot.Data;

import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.CategoryChart;
import org.knowm.xchart.CategoryChartBuilder;
import org.knowm.xchart.style.Styler;

import java.io.File;
import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class MessageStats {
    private final MessageStore store;

    public MessageStats(MessageStore store) {
        this.store = store;
    }

    // === TOTALS ===

    public long getTotalMessages() throws IOException {
        return store.readAllMessages().size();
    }

    public long getTotalMessagesByUser(long userID) throws IOException {
        return store.filter(msg -> msg.author().userId() == userID).size();
    }

    // === FREQUENCY ANALYSIS ===

    // Hour of day (0-23)
    public Map<Integer, Long> getHourlyFrequency(Long userID, ZoneId zoneId) throws IOException {
        var messages = userID == null ?
                store.readAllMessages() :
                store.filter(msg -> msg.author().userId() == userID);

        return messages.stream()
                .collect(Collectors.groupingBy(
                        msg -> Instant.ofEpochSecond(msg.timestamp())
                                .atZone(zoneId).getHour(),
                        Collectors.counting()
                ));
    }

    // Day of week (1=Monday, 7=Sunday)
    public Map<Integer, Long> getDailyFrequency(Long userID, ZoneId zoneId) throws IOException {
        var messages = userID == null ?
                store.readAllMessages() :
                store.filter(msg -> msg.author().userId() == userID);

        return messages.stream()
                .collect(Collectors.groupingBy(
                        msg -> Instant.ofEpochSecond(msg.timestamp())
                                .atZone(zoneId).getDayOfWeek().getValue(),
                        Collectors.counting()
                ));
    }

    // Month of year (1-12)
    public Map<Integer, Long> getMonthlyFrequency(Long userID, ZoneId zoneId) throws IOException {
        var messages = userID == null ?
                store.readAllMessages() :
                store.filter(msg -> msg.author().userId() == userID);

        return messages.stream()
                .collect(Collectors.groupingBy(
                        msg -> Instant.ofEpochSecond(msg.timestamp())
                                .atZone(zoneId).getMonthValue(),
                        Collectors.counting()
                ));
    }

    // Year
    public Map<Integer, Long> getYearlyFrequency(Long userID, ZoneId zoneId) throws IOException {
        var messages = userID == null ?
                store.readAllMessages() :
                store.filter(msg -> msg.author().userId() == userID);

        return messages.stream()
                .collect(Collectors.groupingBy(
                        msg -> Instant.ofEpochSecond(msg.timestamp())
                                .atZone(zoneId).getYear(),
                        Collectors.counting()
                ));
    }

    // === FIRST MESSAGE ANALYSIS ===

    public String getAverageFirstMessageTime(Long userID, Period period, ZoneId zoneId) throws IOException {
        var messages = userID == null ?
                store.readAllMessages() :
                store.filter(msg -> msg.author().userId() == userID);

        if (messages.isEmpty()) return "No messages";

        // Group by period (day/week/month/year) and find first message of each
        Map<LocalDate, LocalTime> firstMessages = new HashMap<>();

        for (var msg : messages) {
            ZonedDateTime zdt = Instant.ofEpochSecond(msg.timestamp())
                    .atZone(zoneId);
            LocalDate periodKey = getPeriodKey(zdt.toLocalDate(), period);
            LocalTime time = zdt.toLocalTime();

            firstMessages.merge(periodKey, time, (existing, newTime) ->
                    newTime.isBefore(existing) ? newTime : existing);
        }

        // Calculate average time
        double avgSeconds = firstMessages.values().stream()
                .mapToInt(LocalTime::toSecondOfDay)
                .average()
                .orElse(0);

        return LocalTime.ofSecondOfDay((long) avgSeconds).format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    private LocalDate getPeriodKey(LocalDate date, Period period) {
        return switch (period) {
            case DAY -> date;
            case WEEK -> date.with(DayOfWeek.MONDAY);
            case MONTH -> date.withDayOfMonth(1);
            case YEAR -> date.withDayOfYear(1);
        };
    }

    public enum Period {
        DAY, WEEK, MONTH, YEAR
    }

    // === CHART GENERATION ===

    public File generateHourlyChart(Long userID, ZoneId zoneId) throws IOException {
        Map<Integer, Long> data = getHourlyFrequency(userID, zoneId);

        // Fill in missing hours with 0
        List<Integer> hours = new ArrayList<>();
        List<Long> counts = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            hours.add(i);
            counts.add(data.getOrDefault(i, 0L));
        }

        CategoryChart chart = new CategoryChartBuilder()
                .width(1200)
                .height(600)
                .title((userID == null ? "Hourly Message Frequency (All Users)" :
                        "Hourly Message Frequency: User " + userID) + " [" + zoneId.getId() + "]")
                .xAxisTitle("Hour of Day")
                .yAxisTitle("Message Count")
                .build();

        chart.getStyler().setLegendPosition(Styler.LegendPosition.InsideNW);
        chart.addSeries("Messages", hours, counts);

        File tempFile = File.createTempFile("hourly_chart_", ".png");
        BitmapEncoder.saveBitmap(chart, tempFile.getAbsolutePath(), BitmapEncoder.BitmapFormat.PNG);
        return tempFile;
    }

    public File generateDailyChart(Long userID, ZoneId zoneId) throws IOException {
        Map<Integer, Long> data = getDailyFrequency(userID, zoneId);

        List<String> days = Arrays.asList("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun");
        List<Long> counts = new ArrayList<>();
        for (int i = 1; i <= 7; i++) {
            counts.add(data.getOrDefault(i, 0L));
        }

        CategoryChart chart = new CategoryChartBuilder()
                .width(1200)
                .height(600)
                .title((userID == null ? "Daily Message Frequency (All Users)" :
                        "Daily Message Frequency: User " + userID) + " [" + zoneId.getId() + "]")
                .xAxisTitle("Day of Week")
                .yAxisTitle("Message Count")
                .build();

        chart.getStyler().setLegendPosition(Styler.LegendPosition.InsideNW);
        chart.addSeries("Messages", days, counts);

        File tempFile = File.createTempFile("daily_chart_", ".png");
        BitmapEncoder.saveBitmap(chart, tempFile.getAbsolutePath(), BitmapEncoder.BitmapFormat.PNG);
        return tempFile;
    }

    public File generateMonthlyChart(Long userID, ZoneId zoneId) throws IOException {
        Map<Integer, Long> data = getMonthlyFrequency(userID, zoneId);

        List<String> months = Arrays.asList("Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec");
        List<Long> counts = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            counts.add(data.getOrDefault(i, 0L));
        }

        CategoryChart chart = new CategoryChartBuilder()
                .width(1200)
                .height(600)
                .title((userID == null ? "Monthly Message Frequency (All Users)" :
                        "Monthly Message Frequency: User " + userID) + " [" + zoneId.getId() + "]")
                .xAxisTitle("Month")
                .yAxisTitle("Message Count")
                .build();

        chart.getStyler().setLegendPosition(Styler.LegendPosition.InsideNW);
        chart.addSeries("Messages", months, counts);

        File tempFile = File.createTempFile("monthly_chart_", ".png");
        BitmapEncoder.saveBitmap(chart, tempFile.getAbsolutePath(), BitmapEncoder.BitmapFormat.PNG);
        return tempFile;
    }

    public File generateYearlyChart(Long userID, ZoneId zoneId) throws IOException {
        Map<Integer, Long> data = getYearlyFrequency(userID, zoneId);

        List<Integer> years = new ArrayList<>(data.keySet());
        Collections.sort(years);
        List<Long> counts = years.stream()
                .map(year -> data.get(year))
                .collect(Collectors.toList());

        CategoryChart chart = new CategoryChartBuilder()
                .width(1200)
                .height(600)
                .title((userID == null ? "Yearly Message Frequency (All Users)" :
                        "Yearly Message Frequency: User " + userID) + " [" + zoneId.getId() + "]")
                .xAxisTitle("Year")
                .yAxisTitle("Message Count")
                .build();

        chart.getStyler().setLegendPosition(Styler.LegendPosition.InsideNW);
        chart.addSeries("Messages", years, counts);

        File tempFile = File.createTempFile("yearly_chart_", ".png");
        BitmapEncoder.saveBitmap(chart, tempFile.getAbsolutePath(), BitmapEncoder.BitmapFormat.PNG);
        return tempFile;
    }
}
