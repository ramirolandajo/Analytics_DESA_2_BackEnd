package ar.edu.uade.analytics.Repository;

import ar.edu.uade.analytics.Entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, Integer> {
}

