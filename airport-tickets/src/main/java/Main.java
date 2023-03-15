import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Objects;
import java.util.OptionalDouble;

public class Main {
    private static final String JSON_FIlE_PATH = "tickets.json";
    private static final String ORIGIN_NAME = "Владивосток";
    private static final String DEPARTURE_NAME = "Тель-Авив";
    private static final ZoneId DEPARTURE_TIME_ZONE = ZoneId.of("UTC+10");
    private static final ZoneId ARRIVAL_TIME_ZONE = ZoneId.of("UTC+3");


    public static void main(String[] args) {
        ArrayList<Ticket> tickets = parseJson(JSON_FIlE_PATH);
        avgTime(tickets, ORIGIN_NAME, DEPARTURE_NAME, DEPARTURE_TIME_ZONE, ARRIVAL_TIME_ZONE);
        percentile(tickets, ORIGIN_NAME, DEPARTURE_NAME, DEPARTURE_TIME_ZONE, ARRIVAL_TIME_ZONE, 90);
    }

    public static ArrayList<Ticket> parseJson(String jsonFilePath) {
        Gson gson = new Gson();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(Main.class.getClassLoader().getResourceAsStream(jsonFilePath)), StandardCharsets.UTF_8))) {
            Tickets tickets = gson.fromJson(reader, Tickets.class);
            return tickets.getTickets();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static ZonedDateTime parseDate(String date, String time, ZoneId zoneId) {
        try {
            LocalDateTime dateTime = LocalDateTime.parse(date + " " + time, DateTimeFormatter.ofPattern("dd.MM.yy H:mm"));
            return ZonedDateTime.of(dateTime, zoneId);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date or time format", e);
        }
    }

    public static void avgTime(ArrayList<Ticket> tickets, String originName, String destinationName, ZoneId departureZoneId, ZoneId arrivalZoneId) {
        OptionalDouble average = tickets.stream()
                .filter(t -> t.getOriginName().equals(originName) && t.getDestinationName().equals(destinationName))
                .mapToLong(t -> ChronoUnit.MINUTES.between(parseDate(t.getDepartureDate(), t.getDepartureTime(), departureZoneId), parseDate(t.getArrivalDate(), t.getArrivalTime(), arrivalZoneId)))
                .average();

        if (average.isPresent()) {
            System.out.printf("Среднее время полета между городами %s и %s равен %s\n", originName, destinationName, timeToString((long) average.getAsDouble()));
        } else {
            System.out.printf("Не существует билетов между городами %s и %s\n", originName, destinationName);
        }
    }

    public static void percentile(ArrayList<Ticket> tickets, String originName, String destinationName, ZoneId departureZoneId, ZoneId arrivalZoneId, Integer percentile) {
        ArrayList<Long> flightTime = tickets.stream()
                .filter(t -> t.getOriginName().equals(originName) && t.getDestinationName().equals(destinationName))
                .mapToLong(t -> ChronoUnit.MINUTES.between(parseDate(t.getDepartureDate(), t.getDepartureTime(), departureZoneId), parseDate(t.getArrivalDate(), t.getArrivalTime(), arrivalZoneId)))
                .sorted()
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        if (!flightTime.isEmpty()) {
            int index = (int) Math.round((double) percentile / 100 * flightTime.size()) - 1;
            System.out.printf("%d-й процентиль времени полета между городами %s и %s равен %s",
                    percentile, originName, destinationName, timeToString(flightTime.get(index)));
        } else System.out.printf("Не существует билетов между %s и %s\n", originName, destinationName);
    }

    private static String timeToString(long minutes) {
        long hour = minutes / 60,
                min = minutes % 60;
        return String.format("%02d часов %02d минут", hour, min);
    }

}

@AllArgsConstructor
@Setter
@Getter
@Builder
class Ticket {
    private String origin;
    @SerializedName("origin_name")
    private String originName;
    private String destination;
    @SerializedName("destination_name")
    private String destinationName;
    @SerializedName("departure_date")
    private String departureDate;
    @SerializedName("departure_time")
    private String departureTime;
    @SerializedName("arrival_date")
    private String arrivalDate;
    @SerializedName("arrival_time")
    private String arrivalTime;
    private String carrier;
    private Integer stops;
    private Integer price;
}

@Getter
@AllArgsConstructor
class Tickets {
    private ArrayList<Ticket> tickets;
}
