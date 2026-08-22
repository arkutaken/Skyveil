package name.skyveil.client.customkeybind;

import org.lwjgl.glfw.GLFW;

/** Gson-friendly custom command keybind stored inside skyveil.json. */
public final class CustomKeybindDefinition {
    public String id="";
    public String name="";
    public String command="";
    public int key=GLFW.GLFW_KEY_UNKNOWN;
    public boolean enabled=true;

    public CustomKeybindDefinition copy(){
        CustomKeybindDefinition copy=new CustomKeybindDefinition();
        copy.id=id;copy.name=name;copy.command=command;copy.key=key;copy.enabled=enabled;
        return copy;
    }
}
