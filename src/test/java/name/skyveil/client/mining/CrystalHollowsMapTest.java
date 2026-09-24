package name.skyveil.client.mining;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CrystalHollowsMapTest {
    @Test void projectionUsesWorldBoundsAndClampsEdges(){
        assertEquals(0,CrystalHollowsMap.fraction(201));
        assertEquals(1,CrystalHollowsMap.fraction(824));
        assertEquals(.5,CrystalHollowsMap.fraction(512.5));
        assertEquals(0,CrystalHollowsMap.fraction(-20));
        assertEquals(1,CrystalHollowsMap.fraction(900));
    }
    @Test void quadrantsNucleusAndUndergroundLayer(){
        assertEquals("Jungle",CrystalHollowsMap.region(300,100,300));
        assertEquals("Mithril Deposits",CrystalHollowsMap.region(700,100,300));
        assertEquals("Goblin Holdout",CrystalHollowsMap.region(300,100,700));
        assertEquals("Precursor Remnants",CrystalHollowsMap.region(700,100,700));
        assertEquals("Crystal Nucleus",CrystalHollowsMap.region(512,100,512));
        assertEquals("Magma Fields",CrystalHollowsMap.region(512,50,512));
    }
    @Test void recognizesLocationFieldsNotCommissionsOrUnrelatedIslands(){
        assertTrue(CrystalHollowsMap.isLocation("§bArea: Crystal Hollows"));
        assertTrue(CrystalHollowsMap.isLocation(" ⏣ Mines of Divan"));
        assertTrue(CrystalHollowsMap.isLocation("\uE067 Jungle"));
        assertFalse(CrystalHollowsMap.isLocation("Area: Dwarven Mines"));
        assertFalse(CrystalHollowsMap.isLocation("Jungle Gemstone Collector: 5%"));
    }
}
