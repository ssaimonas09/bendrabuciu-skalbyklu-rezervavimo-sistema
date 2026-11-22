import java.time.LocalTime;

public record ReservationSettings(int durationMinutes, LocalTime opensAt, LocalTime closesAt) {
}
