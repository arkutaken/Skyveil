package name.skyveil.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** Flat button shared by the layout and color editors. */
public final class SkyveilButton extends Button {
    public SkyveilButton(int x,int y,int width,int height,Component label,OnPress press){
        super(x,y,width,height,label,press,DEFAULT_NARRATION);
    }
    @Override protected void extractContents(GuiGraphicsExtractor graphics,int mouseX,int mouseY,float delta){
        boolean highlighted=isHoveredOrFocused();
        graphics.fill(getX(),getY(),getX()+getWidth(),getY()+getHeight(),highlighted?SkyveilTheme.HOVER:SkyveilTheme.CARD);
        if(highlighted)graphics.outline(getX(),getY(),getWidth(),getHeight(),SkyveilTheme.ACCENT);
        graphics.centeredText(Minecraft.getInstance().font,getMessage(),getX()+getWidth()/2,
            getY()+(getHeight()-9)/2,active?SkyveilTheme.TEXT:SkyveilTheme.MUTED);
    }
}
