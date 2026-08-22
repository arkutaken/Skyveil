package name.skyveil.client.map;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/** Validated add/edit/delete form for persistent custom markers. */
public final class MarkerEditScreen extends Screen {
    private final Screen parent;
    private final SkyblockMapDefinition map;
    private final CustomMapMarker existing;
    private final double defaultX,defaultY,defaultZ;
    private EditBox name,x,y,z;
    private String error="";
    public MarkerEditScreen(Screen parent,SkyblockMapDefinition map,CustomMapMarker existing,double x,double y,double z){super(Component.literal(existing==null?"Add Map Marker":"Edit Map Marker"));this.parent=parent;this.map=map;this.existing=existing;defaultX=x;defaultY=y;defaultZ=z;}

    @Override protected void init(){
        int left=width/2-110,top=height/2-92;
        name=new EditBox(font,left,top+30,220,20,Component.literal("Marker name"));name.setMaxLength(48);name.setHint(Component.literal("Marker name"));
        x=new EditBox(font,left,top+65,68,20,Component.literal("X"));y=new EditBox(font,left+76,top+65,68,20,Component.literal("Y"));z=new EditBox(font,left+152,top+65,68,20,Component.literal("Z"));
        name.setValue(existing==null?"":existing.name());x.setValue(number(existing==null?defaultX:existing.x()));y.setValue(number(existing==null?defaultY:existing.y()));z.setValue(number(existing==null?defaultZ:existing.z()));
        addRenderableWidget(name);addRenderableWidget(x);addRenderableWidget(y);addRenderableWidget(z);
        addRenderableWidget(Button.builder(Component.literal("Save"),button->save()).bounds(left,top+105,70,20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"),button->onClose()).bounds(left+75,top+105,70,20).build());
        if(existing!=null)addRenderableWidget(Button.builder(Component.literal("Delete"),button->{MapMarkerManager.remove(existing);minecraft.setScreen(parent);}).bounds(left+150,top+105,70,20).build());
        setInitialFocus(name);
    }
    private void save(){
        String markerName=name.getValue().trim();if(markerName.isBlank()){error="Enter a marker name.";return;}
        try{
            double px=Double.parseDouble(x.getValue().trim()),py=Double.parseDouble(y.getValue().trim()),pz=Double.parseDouble(z.getValue().trim());
            if(!Double.isFinite(px)||!Double.isFinite(py)||!Double.isFinite(pz))throw new NumberFormatException();
            if(!map.contains(px,pz)){error="X/Z are outside this map's calibrated viewport.";return;}
            if(existing==null)MapMarkerManager.add(map.id,markerName,px,py,pz);else MapMarkerManager.update(existing,markerName,px,py,pz);
            minecraft.setScreen(parent);
        }catch(NumberFormatException exception){error="Coordinates must be finite numbers.";}
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float partial){
        g.fill(0,0,width,height,0xB0100B18);int left=width/2-122,top=height/2-104;g.fill(left,top,left+244,top+150,0xEE201730);g.outline(left,top,244,150,0xFF9B6CFF);
        g.centeredText(font,existing==null?"Add Marker":"Edit Marker",width/2,top+10,0xFFF5F2FF);g.text(font,"Name",left+12,top+27,0xFFB9A0E8,false);g.text(font,"X",left+12,top+62,0xFFB9A0E8,false);g.text(font,"Y",left+88,top+62,0xFFB9A0E8,false);g.text(font,"Z",left+164,top+62,0xFFB9A0E8,false);
        if(!error.isBlank())g.centeredText(font,error,width/2,top+133,0xFFFF7777);
        super.extractRenderState(g,mx,my,partial);
    }
    @Override public void onClose(){minecraft.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
    @Override public boolean isInGameUi(){return true;}
    private static String number(double value){return String.format(Locale.ROOT,"%.1f",value);}
}
