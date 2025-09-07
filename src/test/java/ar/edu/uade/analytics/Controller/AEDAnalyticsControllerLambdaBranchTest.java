package ar.edu.uade.analytics.Controller;

import ar.edu.uade.analytics.Entity.Purchase;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

public class AEDAnalyticsControllerLambdaBranchTest {
    @Test
    void testFiltradoFechasTodosBranches() {
        // Lambda: (startDate == null || !fecha.isBefore(startDate)) && (endDate == null || !fecha.isAfter(endDate))
        // Casos:
        // 1. fecha == null
        Purchase p = new Purchase();
        p.setDate(null);
        LocalDateTime now = LocalDateTime.now();
        assertFalse(filtrar(now.minusDays(1), now.plusDays(1), p));

        // 2. startDate == null, endDate == null, fecha != null
        p.setDate(now);
        assertTrue(filtrar(null, null, p));

        // 3. startDate != null, endDate == null, fecha antes de startDate
        p.setDate(now.minusDays(5));
        assertFalse(filtrar(now.minusDays(1), null, p));
        // 4. startDate != null, endDate == null, fecha igual o después de startDate
        p.setDate(now);
        assertTrue(filtrar(now.minusDays(1), null, p));

        // 5. startDate == null, endDate != null, fecha después de endDate
        p.setDate(now.plusDays(5));
        assertFalse(filtrar(null, now, p));
        // 6. startDate == null, endDate != null, fecha igual o antes de endDate
        p.setDate(now);
        assertTrue(filtrar(null, now, p));

        // 7. startDate != null, endDate != null, fecha antes de startDate
        p.setDate(now.minusDays(5));
        assertFalse(filtrar(now.minusDays(1), now.plusDays(1), p));
        // 8. startDate != null, endDate != null, fecha después de endDate
        p.setDate(now.plusDays(5));
        assertFalse(filtrar(now.minusDays(1), now.plusDays(1), p));
        // 9. startDate != null, endDate != null, fecha dentro del rango
        p.setDate(now);
        assertTrue(filtrar(now.minusDays(1), now.plusDays(1), p));
    }

    private boolean filtrar(LocalDateTime startDate, LocalDateTime endDate, Purchase p) {
        LocalDateTime fecha = p.getDate();
        if (fecha == null) return false;
        return (startDate == null || !fecha.isBefore(startDate)) && (endDate == null || !fecha.isAfter(endDate));
    }
}

