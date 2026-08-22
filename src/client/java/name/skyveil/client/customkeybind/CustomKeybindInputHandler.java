package name.skyveil.client.customkeybind;

import com.mojang.blaze3d.platform.InputConstants;
import name.skyveil.client.config.ConfigManager;
import net.minecraft.client.Minecraft;

import java.util.HashSet;
import java.util.Set;

/** Edge-triggered gameplay-only keyboard polling for user-created commands. */
public final class CustomKeybindInputHandler {
    private static final Set<Integer> DOWN=new HashSet<>();
    private static final Set<Integer> CURRENT=new HashSet<>();
    private CustomKeybindInputHandler() {}

    public static void tick(Minecraft client){
        if(!ConfigManager.get().customKeybinds.enabled){DOWN.clear();CURRENT.clear();return;}
        var bindings=CustomKeybindManager.all();CURRENT.clear();
        for(var binding:bindings)if(CustomKeybindManager.validKey(binding.key)&&InputConstants.isKeyDown(client.getWindow(),binding.key))CURRENT.add(binding.key);
        boolean canExecute=client.player!=null&&client.getConnection()!=null&&client.screen==null;
        if(canExecute)for(var binding:bindings)if(binding.enabled&&CURRENT.contains(binding.key)&&!DOWN.contains(binding.key))CustomKeybindManager.execute(binding);
        DOWN.clear();DOWN.addAll(CURRENT);
    }
}
