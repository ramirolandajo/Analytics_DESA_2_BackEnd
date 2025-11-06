package ar.edu.uade.analytics.Service;

import ar.edu.uade.analytics.Entity.Event;
import ar.edu.uade.analytics.Repository.EventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    @Mock private EventRepository repo;
    @InjectMocks private EventServiceImpl svc;

    @Test
    void basicCrud_delegateToRepo() {
        Event e = new Event();
        when(repo.findAll()).thenReturn(List.of(e));
        assertEquals(1, svc.getAllEvents().size());

        when(repo.findById(3)).thenReturn(Optional.of(e));
        assertTrue(svc.getEventById(3).isPresent());

        when(repo.save(e)).thenReturn(e);
        assertSame(e, svc.saveEvent(e));

        svc.deleteEvent(5);
        verify(repo).deleteById(5);
    }
}

