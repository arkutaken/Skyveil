package name.skyveil.client.customkeybind;

import com.mojang.blaze3d.platform.InputConstants;
import name.skyveil.client.config.ConfigManager;
import net.fabricmc.fabric.impl.command.client.ClientCommandInternals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.*;

/** Validation, persistence, conflict reporting, and single-command dispatch. */
public final class CustomKeybindManager {
    private static List<CustomKeybindDefinition> cachedSource;
    private static List<CustomKeybindDefinition> cachedBindings=List.of();
    private CustomKeybindManager() {}

    public static void initialize(){all();}

    public static List<CustomKeybindDefinition> all(){
        List<CustomKeybindDefinition> source=ConfigManager.get().customKeybinds.bindings;
        if(source!=cachedSource){sanitize();source=ConfigManager.get().customKeybinds.bindings;cachedSource=source;cachedBindings=List.copyOf(source);}
        return cachedBindings;
    }

    public static String normalizeCommand(String raw){
        String value=raw==null?"":raw.replace('\r',' ').replace('\n',' ').trim();
        while(value.startsWith("/"))value=value.substring(1).trim();
        return value.isBlank()?"":"/"+value;
    }

    public static String keyName(int key){
        if(!validKey(key))return "Unbound";
        return InputConstants.getKey(new KeyEvent(key,0,0)).getDisplayName().getString();
    }

    public static boolean validKey(int key){return key>=GLFW.GLFW_KEY_SPACE&&key<=GLFW.GLFW_KEY_LAST;}

    public static String customConflict(int key,String editingId){
        if(!validKey(key))return null;
        for(CustomKeybindDefinition binding:all())if(binding.enabled&&!Objects.equals(binding.id,editingId)&&binding.key==key)return binding.name;
        return null;
    }

    public static String otherConflict(int key){
        if(!validKey(key))return null;
        Minecraft client=Minecraft.getInstance();
        KeyEvent event=new KeyEvent(key,0,0);
        if(client.options!=null&&client.options.keyMappings!=null)for(var mapping:client.options.keyMappings)if(mapping.matches(event))return Component.translatable(mapping.getName()).getString();
        if(ConfigManager.get().itemProtection.lockKey==key)return "Skyveil Item Lock";
        return null;
    }

    public static void save(CustomKeybindDefinition value){
        sanitize();CustomKeybindDefinition binding=normalize(value.copy());
        ConfigManager.get().customKeybinds.bindings.removeIf(existing->Objects.equals(existing.id,binding.id));
        ConfigManager.get().customKeybinds.bindings.add(binding);invalidate();ConfigManager.save();
    }

    public static void delete(String id){ConfigManager.get().customKeybinds.bindings.removeIf(binding->binding!=null&&Objects.equals(binding.id,id));invalidate();ConfigManager.save();}

    public static boolean setEnabled(CustomKeybindDefinition binding,boolean enabled){
        if(enabled&&customConflict(binding.key,binding.id)!=null)return false;
        binding.enabled=enabled;ConfigManager.save();return true;
    }

    public static void execute(CustomKeybindDefinition binding){
        String normalized=normalizeCommand(binding.command);Minecraft client=Minecraft.getInstance();
        if(normalized.isBlank()||client.getConnection()==null)return;
        String command=normalized.substring(1);
        if(!ClientCommandInternals.executeCommand(command))client.getConnection().sendCommand(command);
    }

    private static CustomKeybindDefinition normalize(CustomKeybindDefinition binding){
        if(binding.id==null||binding.id.isBlank())binding.id=UUID.randomUUID().toString();
        binding.name=binding.name==null||binding.name.trim().isBlank()?"Unnamed Keybind":binding.name.trim();
        binding.command=normalizeCommand(binding.command);
        if(!validKey(binding.key))binding.key=GLFW.GLFW_KEY_UNKNOWN;
        return binding;
    }

    private static void sanitize(){
        var config=ConfigManager.get().customKeybinds;
        if(config.bindings==null)config.bindings=new ArrayList<>();
        Set<String> ids=new HashSet<>();Set<Integer> activeKeys=new HashSet<>();
        config.bindings.removeIf(binding->{
            if(binding==null)return true;normalize(binding);
            if(binding.command.isBlank()||!ids.add(binding.id))return true;
            if(binding.enabled&&validKey(binding.key)&&!activeKeys.add(binding.key))binding.enabled=false;
            return false;
        });
    }
    private static void invalidate(){cachedSource=null;}
}
