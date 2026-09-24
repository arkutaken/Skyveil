package name.skyveil.client.performance;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PingMeasurementTest {
    @Test void measuresOnlyMatchingReplies(){
        var ping=new PingMeasurement();
        assertEquals(-1,ping.value(0));
        long token=ping.request(0);
        assertNotEquals(0,token);
        assertFalse(ping.receive(123,100_000_000));
        assertEquals(-1,ping.value(100_000_000));
        assertTrue(ping.receive(token,132_000_000));
        assertEquals(132,ping.value(132_000_000));
    }

    @Test void rateLimitsAndExpiresStaleValues(){
        var ping=new PingMeasurement();
        long token=ping.request(0);
        ping.receive(token,100_000_000);
        assertEquals(0,ping.request(1_000_000_000L));
        assertNotEquals(0,ping.request(5_000_000_000L));
        assertEquals(-1,ping.value(16_000_000_000L));
    }

    @Test void timesOutAndConsumesLateRepliesWithoutMeasuringThem(){
        var ping=new PingMeasurement();
        long old=ping.request(0);
        assertEquals(0,ping.request(5_000_000_000L));
        long fresh=ping.request(10_000_000_000L);
        assertNotEquals(old,fresh);
        assertTrue(ping.receive(old,10_100_000_000L));
        assertEquals(-1,ping.value(10_100_000_000L));
        ping.receive(fresh,10_140_000_000L);
        assertEquals(140,ping.value(10_140_000_000L));
    }

    @Test void worldChangesRejectRepliesFromPreviousSession(){
        var ping=new PingMeasurement();
        long old=ping.request(0);
        ping.reset();
        long fresh=ping.request(1_000_000_000L);
        assertNotEquals(old,fresh);
        assertTrue(ping.receive(old,1_100_000_000L));
        assertEquals(-1,ping.value(1_100_000_000L));
        ping.receive(fresh,1_120_000_000L);
        assertEquals(120,ping.value(1_120_000_000L));
    }
}