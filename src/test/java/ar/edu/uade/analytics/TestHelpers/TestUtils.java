package ar.edu.uade.analytics.TestHelpers;

import ar.edu.uade.analytics.Entity.Product;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.mockito.ArgumentMatcher;
import org.mockito.stubbing.OngoingStubbing;

import java.util.Objects;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Small test helper utilities used across unit tests to avoid common NPEs
 * (MeterRegistry.counter(...) == null) and to make lenient stubbings easier.
 */
public final class TestUtils {

    private TestUtils() {}

    /**
     * Returns a MeterRegistry mock where counter(...) always returns a mock Counter.
     * Call this from tests that interact with services that safeIncrement counters.
     */
    public static MeterRegistry mockMeterRegistryWithCounter() {
        MeterRegistry mr = mock(MeterRegistry.class);
        Counter c = mock(Counter.class);
        // make counter(...) return the counter mock for any args
        when(mr.counter(anyString(), any(String[].class))).thenReturn(c);
        // lenient stub in case some tests prefer lenient behavior
        lenient().when(mr.counter(anyString())).thenReturn(c);
        return mr;
    }

    /**
     * Convenience for lenient stubbing: lenient().when(methodCall)
     */
    public static <T> OngoingStubbing<T> lenientWhen(T methodCall) {
        return lenient().when(methodCall);
    }

    /**
     * Null-safe matcher for Product with given productCode.
     */
    public static ArgumentMatcher<Product> productWithCode(Integer code) {
        return p -> p != null && Objects.equals(p.getProductCode(), code);
    }
}
