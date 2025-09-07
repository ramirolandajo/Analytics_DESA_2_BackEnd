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
public class EventServiceImplTest {

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private EventServiceImpl service;

    @Test
    void testGetAllEvents() {
        Event e = new Event(); e.setId(1); e.setType("T");
        when(eventRepository.findAll()).thenReturn(List.of(e));
        var res = service.getAllEvents();
        assertNotNull(res);
        assertEquals(1, res.size());
        verify(eventRepository).findAll();
    }

    @Test
    void testGetEventById() {
        Event e = new Event(); e.setId(2); e.setType("T2");
        when(eventRepository.findById(2)).thenReturn(Optional.of(e));
        var opt = service.getEventById(2);
        assertTrue(opt.isPresent());
        assertEquals(2, opt.get().getId());
        verify(eventRepository).findById(2);
    }

    @Test
    void testSaveAndDeleteEvent() {
        Event e = new Event(); e.setId(3);
        when(eventRepository.save(e)).thenReturn(e);
        var saved = service.saveEvent(e);
        assertEquals(3, saved.getId());
        service.deleteEvent(3);
        verify(eventRepository).deleteById(3);
        verify(eventRepository).save(e);
    }
}

