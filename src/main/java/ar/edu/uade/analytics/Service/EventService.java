package ar.edu.uade.analytics.Service;

import ar.edu.uade.analytics.Entity.Event;
import java.util.List;
import java.util.Optional;

public interface EventService {
    List<Event> getAllEvents();
    Optional<Event> getEventById(Integer id);
    Event saveEvent(Event event);
    void deleteEvent(Integer id);
}

