package name.skyveil.client.performance;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TpsEstimatorTest {
    @Test void waitsForDataAndExpiresStaleReadings(){
        var estimator=new TpsEstimator();
        assertTrue(Double.isNaN(estimator.value(0)));
        estimator.update(100,0);
        assertTrue(Double.isNaN(estimator.value(0)));
        estimator.update(120,1_000_000_000L);
        assertEquals(20,estimator.value(1_000_000_000L));
        assertTrue(Double.isNaN(estimator.value(12_000_000_000L)));
    }

    @Test void measuresLagAndSmoothsPacketJitter(){
        var estimator=new TpsEstimator();
        estimator.update(0,0);
        estimator.update(20,2_000_000_000L);
        assertEquals(10,estimator.value(2_000_000_000L));
        estimator.update(40,2_500_000_000L);
        assertEquals(16,estimator.value(2_500_000_000L));
    }

    @Test void capsAtTwentyAndHandlesWorldChangesAndDisconnects(){
        var estimator=new TpsEstimator();
        estimator.update(100,0);
        estimator.update(120,500_000_000L);
        assertEquals(20,estimator.value(500_000_000L));
        estimator.update(0,1_000_000_000L);
        assertTrue(Double.isNaN(estimator.value(1_000_000_000L)));
        estimator.update(20,2_000_000_000L);
        estimator.reset();
        assertTrue(Double.isNaN(estimator.value(2_000_000_000L)));
    }

    @Test void rollingWindowRecoversAfterLagAndIgnoresDuplicateClockUpdates(){
        var estimator=new TpsEstimator();
        estimator.update(0,0);
        estimator.update(20,2_000_000_000L);
        estimator.update(20,2_100_000_000L);
        for(int i=1;i<=5;i++)estimator.update(20+20L*i,(2L+i)*1_000_000_000L);
        assertEquals(20,estimator.value(7_000_000_000L));
        estimator.update(200,30_000_000_000L);
        assertTrue(Double.isNaN(estimator.value(30_000_000_000L)));
    }
}