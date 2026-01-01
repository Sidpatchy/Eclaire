package com.sidpatchy.basebot.Data;

import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.CategoryChart;
import org.knowm.xchart.CategoryChartBuilder;
import org.knowm.xchart.style.Styler;

import java.awt.Color;
import com.sidpatchy.basebot.Main;
import java.io.File;
import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
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

    public Map<Long, Long> getTopUsers(int limit) throws IOException {
        return store.readAllMessages().stream()
                .collect(Collectors.groupingBy(
                        msg -> msg.author().userId(),
                        Collectors.counting()
                ))
                .entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
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

    public record HourRecord(LocalDate date, int hour, long count) {}

    public HourRecord getTopHour(Long userID, ZoneId zoneId) throws IOException {
        var messages = userID == null ?
                store.readAllMessages() :
                store.filter(msg -> msg.author().userId() == userID);

        if (messages.isEmpty()) return null;

        return messages.stream()
                .collect(Collectors.groupingBy(
                        msg -> {
                            ZonedDateTime zdt = Instant.ofEpochSecond(msg.timestamp()).atZone(zoneId);
                            return zdt.toLocalDate().atTime(zdt.getHour(), 0);
                        },
                        Collectors.counting()
                ))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(entry -> new HourRecord(entry.getKey().toLocalDate(), entry.getKey().getHour(), entry.getValue()))
                .orElse(null);
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

    public Map<LocalDate, Long> getTopDays(Long userID, int limit, ZoneId zoneId) throws IOException {
        var messages = userID == null ?
                store.readAllMessages() :
                store.filter(msg -> msg.author().userId() == userID);

        return messages.stream()
                .collect(Collectors.groupingBy(
                        msg -> Instant.ofEpochSecond(msg.timestamp())
                                .atZone(zoneId).toLocalDate(),
                        Collectors.counting()
                ))
                .entrySet().stream()
                .sorted(Map.Entry.<LocalDate, Long>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    public int getLongestStreak(Long userID, ZoneId zoneId) throws IOException {
        var messages = userID == null ?
                store.readAllMessages() :
                store.filter(msg -> msg.author().userId() == userID);

        if (messages.isEmpty()) return 0;

        List<LocalDate> dates = messages.stream()
                .map(msg -> Instant.ofEpochSecond(msg.timestamp()).atZone(zoneId).toLocalDate())
                .distinct()
                .sorted()
                .toList();

        int maxStreak = 0;
        int currentStreak = 0;
        LocalDate lastDate = null;

        for (LocalDate date : dates) {
            if (lastDate == null || date.equals(lastDate.plusDays(1))) {
                currentStreak++;
            } else {
                maxStreak = Math.max(maxStreak, currentStreak);
                currentStreak = 1;
            }
            lastDate = date;
        }

        return Math.max(maxStreak, currentStreak);
    }

    public double getConsistencyScore(Long userID, int days, ZoneId zoneId) throws IOException {
        var messages = userID == null ?
                store.readAllMessages() :
                store.filter(msg -> msg.author().userId() == userID);

        if (messages.isEmpty()) return 0;

        LocalDate now = LocalDate.now(zoneId);
        LocalDate startDate;
        long totalDays;

        if (days > 0) {
            startDate = now.minusDays(days - 1);
            totalDays = days;
        } else {
            // Since started
            startDate = messages.stream()
                    .map(msg -> Instant.ofEpochSecond(msg.timestamp()).atZone(zoneId).toLocalDate())
                    .min(LocalDate::compareTo)
                    .orElse(now);
            totalDays = ChronoUnit.DAYS.between(startDate, now) + 1;
        }

        long activeDays = messages.stream()
                .map(msg -> Instant.ofEpochSecond(msg.timestamp()).atZone(zoneId).toLocalDate())
                .filter(date -> !date.isBefore(startDate))
                .distinct()
                .count();

        return (double) activeDays / totalDays * 100;
    }

    public String getTimeToFirstE(Long userID, ZoneId zoneId) throws IOException {
        var messages = userID == null ?
                store.readAllMessages() :
                store.filter(msg -> msg.author().userId() == userID);

        if (messages.isEmpty()) return "No messages";

        Map<LocalDate, Long> firstESeconds = new HashMap<>();

        for (var msg : messages) {
            ZonedDateTime zdt = Instant.ofEpochSecond(msg.timestamp()).atZone(zoneId);
            LocalDate date = zdt.toLocalDate();
            long secondsSinceMidnight = zdt.toLocalTime().toSecondOfDay();

            firstESeconds.merge(date, secondsSinceMidnight, Math::min);
        }

        double avgSeconds = firstESeconds.values().stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0);

        long seconds = (long) avgSeconds;
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;

        return String.format("%02d:%02d:%02d", h, m, s);
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

    public String getAverageLastMessageTime(Long userID, Period period, ZoneId zoneId) throws IOException {
        var messages = userID == null ?
                store.readAllMessages() :
                store.filter(msg -> msg.author().userId() == userID);

        if (messages.isEmpty()) return "No messages";

        // Group by period (day/week/month/year) and find last message of each
        Map<LocalDate, LocalTime> lastMessages = new HashMap<>();

        for (var msg : messages) {
            ZonedDateTime zdt = Instant.ofEpochSecond(msg.timestamp())
                    .atZone(zoneId);
            LocalDate periodKey = getPeriodKey(zdt.toLocalDate(), period);
            LocalTime time = zdt.toLocalTime();

            lastMessages.merge(periodKey, time, (existing, newTime) ->
                    newTime.isAfter(existing) ? newTime : existing);
        }

        // Calculate average time
        double avgSeconds = lastMessages.values().stream()
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

    private void applyStyle(CategoryChart chart) {
        chart.getStyler().setChartBackgroundColor(new Color(54, 57, 63)); // Discord Dark Gray
        chart.getStyler().setPlotBackgroundColor(new Color(54, 57, 63));
        chart.getStyler().setPlotGridLinesColor(new Color(79, 84, 92));
        chart.getStyler().setChartFontColor(Color.WHITE);
        chart.getStyler().setAxisTickLabelsColor(Color.WHITE);
        chart.getStyler().setLegendVisible(false);
        chart.getStyler().setChartTitleBoxVisible(false);
        chart.getStyler().setSeriesColors(new Color[]{com.sidpatchy.basebot.Main.getColor()});
        chart.getStyler().setAnnotationTextPanelBackgroundColor(new Color(54, 57, 63));
        chart.getStyler().setAnnotationTextFontColor(Color.WHITE);
        chart.getStyler().setXAxisTitleVisible(true);
        chart.getStyler().setYAxisTitleVisible(true);
        chart.getStyler().setAxisTickPadding(10);
        chart.getStyler().setChartTitleVisible(true);
    }

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

        applyStyle(chart);
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

        applyStyle(chart);
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

        applyStyle(chart);
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

        applyStyle(chart);
        chart.addSeries("Messages", years, counts);

        File tempFile = File.createTempFile("yearly_chart_", ".png");
        BitmapEncoder.saveBitmap(chart, tempFile.getAbsolutePath(), BitmapEncoder.BitmapFormat.PNG);
        return tempFile;
    }
}
