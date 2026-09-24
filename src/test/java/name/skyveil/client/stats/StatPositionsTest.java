package name.skyveil.client.stats;

import com.google.gson.Gson;
import name.skyveil.client.config.SkyveilConfig;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StatPositionsTest {
    @Test void independentlyMovedAndResizedDisplaysSurviveSaving(){
        var config=new SkyveilConfig.PlayerStats();
        var health=config.positions.get(PlayerStat.HEALTH);
        health.hudX=30;health.hudY=50;health.scale=1.7;
        var mana=config.positions.get(PlayerStat.MANA);
        mana.hudX=200;mana.hudY=120;mana.scale=.8;
        var gson=new Gson();
        var restored=gson.fromJson(gson.toJson(config),SkyveilConfig.PlayerStats.class);
        assertEquals(5,restored.positions.size());
        assertEquals(30,restored.positions.get(PlayerStat.HEALTH).hudX);
        assertEquals(1.7,restored.positions.get(PlayerStat.HEALTH).scale);
        assertEquals(200,restored.positions.get(PlayerStat.MANA).hudX);
        assertEquals(.8,restored.positions.get(PlayerStat.MANA).scale);
        assertEquals(-1,restored.positions.get(PlayerStat.VITALITY).hudX);
        assertEquals(1,restored.positions.get(PlayerStat.SPEED).scale);
        assertEquals(-1,restored.positions.get(PlayerStat.DEFENSE).hudY);
    }
}