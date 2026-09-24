package name.skyveil.client;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class PriceCommandRegistrationTest {
    @Test void diagnosticsAreRegisteredOnBothAliasesAndStandalone(){
        var dispatcher=new CommandDispatcher<FabricClientCommandSource>();
        SkyveilClientEntrypoint.registerCommands(dispatcher);
        for(String root:java.util.List.of("sv","skyveil")){
            var node=dispatcher.getRoot().getChild(root).getChild("debugprices");
            assertNotNull(node);assertNotNull(node.getCommand());
        }
        assertNotNull(dispatcher.getRoot().getChild("skyveilprices").getCommand());
    }
}