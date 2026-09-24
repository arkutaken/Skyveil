package name.skyveil.client.mining;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CrystalHollowsDiscoveriesTest {
    @Test void entryStaysFixedWhileWalkingAndMovesOnReentry(){
        var data=new CrystalHollowsDiscoveries();
        data.observe("Jungle",300,100,300);
        data.observe("Jungle Temple",310,101,320);
        data.observe("Jungle Temple",350,80,360);
        assertEquals(310,data.entries().iterator().next().x());
        data.observe(null,400,80,400);
        data.observe("Jungle Temple",360,80,360);
        assertEquals(310,data.entries().iterator().next().x());
        data.observe("Jungle",400,100,400);
        data.observe("Jungle Temple",410,102,420);
        assertEquals(1,data.entries().size());
        assertEquals(410,data.entries().iterator().next().x());
        assertEquals(102,data.entries().iterator().next().y());
    }
    @Test void recordsFiveSitesAndClearsAllLobbyState(){
        var data=new CrystalHollowsDiscoveries();
        for(String name:new String[]{"Mines of Divan","Goblin Queen's Den","Jungle Temple","Lost Precursor City","Khazad-dûm"})
            data.observe(name,300,75,400);
        assertEquals(5,data.entries().size());
        data.clear();
        assertTrue(data.entries().isEmpty());
        data.observe("Khazad-dûm",500,50,600);
        assertEquals(500,data.entries().iterator().next().x());
    }
    @Test void aliasesAreSameSiteAndBroadGoblinRegionIsNotAnEntrance(){
        var data=new CrystalHollowsDiscoveries();
        data.observe("Goblin Holdout",250,100,600);
        assertTrue(data.entries().isEmpty());
        data.observe("Goblin Queen's Den",300,100,600);
        data.observe("Goblin Hideout",350,100,600);
        assertEquals(300,data.entries().iterator().next().x());
        assertNull(CrystalHollowsDiscoveries.Site.fromLocation("Divan Slayer: 50%"));
        assertEquals("Mines of Divan",CrystalHollowsMap.location("§7\uE067 §bMines of Divan"));
    }

    @Test void kingUsesNpcPositionAndPersistsUntilWorldReset(){
        var data=new CrystalHollowsDiscoveries();
        data.observeKing("§6King Yolkar",310,90,620);
        var king=data.entries().iterator().next();
        assertEquals(CrystalHollowsDiscoveries.Site.KING,king.site());
        assertEquals(310,king.x());assertEquals(90,king.y());assertEquals(620,king.z());
        data.observe("Goblin Queen's Den",400,120,700);
        data.observeKing("[NPC] King Yolkar",312,90,622);
        assertEquals(2,data.entries().size());
        assertEquals(312,data.entries().stream().filter(e->e.site()==CrystalHollowsDiscoveries.Site.KING).findFirst().orElseThrow().x());
        assertFalse(CrystalHollowsDiscoveries.isGoblinKing("King Yolkar fan"));
        assertFalse(CrystalHollowsDiscoveries.isGoblinKing("Odger"));
        data.clear();assertTrue(data.entries().isEmpty());
    }
}
