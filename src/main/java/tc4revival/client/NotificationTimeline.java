package tc4revival.client;

import java.util.ArrayList;
import java.util.List;

/** Clock-injected legacy notification timing, independent from the renderer and client process. */
public  class NotificationTimeline {
    public record Notice(String text, String aspect, int color, long created, long expires) {}
    public record Flight(String aspect, float x, float y, long created, long expires) {}
    private List<Notice> notices = new ArrayList<>();
    private final List<Flight> flights = new ArrayList<>();
    public void clear() { notices.clear(); flights.clear(); }
    public void add(String text, String aspect, int color, long now, int delay) {
        long firstBonus = notices.isEmpty() ? delay / 2 : 0;
        notices.add(new Notice(text, aspect, color, now + delay / 4, now + delay + firstBonus));
    }
    public void fly(String aspect, float x, float y, long starts) {
        flights.add(new Flight(aspect, x, y, starts, starts + 1500));
    }
    public List<Notice> notices(long now, int delay) {
        var next = new ArrayList<Notice>();
        for (int i = 0; i < notices.size(); i++) {
            var notice = notices.get(i);
            if (notice.expires() >= now) next.add(i == 0 ? notice
                    : new Notice(notice.text(), notice.aspect(), notice.color(), notice.created(), now + delay));
        }
        notices = next;
        return List.copyOf(notices);
    }
    public List<Flight> flights(long now) {
        flights.removeIf(flight -> flight.expires() < now);
        return List.copyOf(flights);
    }
}
