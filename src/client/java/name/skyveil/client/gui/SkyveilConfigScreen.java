package name.skyveil.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import name.skyveil.client.config.ConfigCategory;
import name.skyveil.client.config.ConfigManager;
import name.skyveil.client.config.ConfigSubcategory;
import name.skyveil.client.config.SettingDefinition;
import name.skyveil.client.chatcopy.ChatCopyBinding;
import name.skyveil.client.chatcopy.ChatCopyManager;
import name.skyveil.client.pet.PetDisplayHud;
import name.skyveil.client.config.SettingsRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/** Sidebar category navigation with inline module accordions and searchable settings. */
public final class SkyveilConfigScreen extends Screen {
    private SkyveilTheme.ConfigPalette theme=SkyveilTheme.config(ConfigManager.get().blackConfigTheme);
    private ConfigCategory category;
    private final LinkedHashSet<ConfigSubcategory> expandedGroups=new LinkedHashSet<>();
    private Double requestedScroll;
    private final List<Header> headers=new ArrayList<>();
    private EditBox search;
    private double scroll,maxScroll;
    private String lastSearchQuery="";
    private List<SearchManager.Result> searchResults=List.of();
    private SettingDefinition pendingScrollSetting,highlightedSetting,draggedSlider,awaitingKey,awaitingChord;
    private final LinkedHashSet<Integer> capturedChordKeys=new LinkedHashSet<>();
    private long highlightUntil;
    private int left,top,windowWidth,windowHeight,sidebarWidth,contentX,contentTop,contentRight,contentBottom;
    private int dragSliderStart,dragSliderEnd;
    private final List<Row> visibleRows=new ArrayList<>();

    public SkyveilConfigScreen(){super(Component.literal("Skyveil Settings"));}

    @Override protected void init(){
        windowWidth=Math.min(840,Math.max(500,(int)(width*.82)));
        windowHeight=Math.min(560,Math.max(320,(int)(height*.82)));
        left=(width-windowWidth)/2;top=(height-windowHeight)/2;sidebarWidth=Math.max(142,windowWidth/5);
        contentX=left+sidebarWidth+16;contentTop=top+52;contentRight=left+windowWidth-14;contentBottom=top+windowHeight-12;
        if(category==null)category=SettingsRegistry.categories().stream().filter(c->c.id.equals("interface")).findFirst().orElse(SettingsRegistry.categories().getFirst());
        search=new EditBox(font,contentX,top+15,contentRight-contentX,22,Component.literal("Search Skyveil settings"));
        search.setHint(Component.literal("Search settings..."));
        search.setResponder(value->{scroll=0;lastSearchQuery=value;searchResults=SearchManager.search(value);});
        addRenderableWidget(search);
        theme=SkyveilTheme.config(ConfigManager.get().blackConfigTheme);

    }



    @Override public void extractRenderState(GuiGraphicsExtractor g,int mouseX,int mouseY,float partialTick){
        g.fill(0,0,width,height,theme.SCRIM);
        g.fill(left+4,top+5,left+windowWidth+4,top+windowHeight+5,0x66000000);
        g.fill(left,top,left+windowWidth,top+windowHeight,theme.WINDOW);
        g.fill(left,top,left+windowWidth,top+45,theme.WINDOW_TOP);
        g.fill(left+1,top+1,left+windowWidth-1,top+3,theme.ACCENT);
        g.outline(left,top,windowWidth,windowHeight,theme.OUTLINE);
        g.fill(left+4,top+45,left+sidebarWidth,top+windowHeight,theme.SIDEBAR);
        g.text(font,"SKYVEIL",left+16,top+16,theme.ACCENT,true);
        g.text(font,"Settings",left+16,top+29,theme.MUTED,false);
        drawCategories(g,mouseX,mouseY);
        drawContent(g,mouseX,mouseY);
        super.extractRenderState(g,mouseX,mouseY,partialTick);
    }

    private void drawCategories(GuiGraphicsExtractor g,int mouseX,int mouseY){
        int y=top+70;
        g.text(font,"CATEGORIES",left+16,y-15,theme.MUTED,false);
        for(ConfigCategory candidate:SettingsRegistry.categories()){
            boolean selected=candidate==category,hover=inside(mouseX,mouseY,left+10,y,sidebarWidth-16,38);
            if(selected){g.fill(left+10,y,left+sidebarWidth-6,y+38,theme.PANEL);g.fill(left+10,y+6,left+12,y+32,theme.ACCENT);}
            else if(hover)g.fill(left+10,y,left+sidebarWidth-6,y+38,theme.HOVER);
            g.fill(left+17,y+9,left+35,y+27,selected?theme.TEXT:theme.PANEL);
            g.centeredText(font,candidate.displayName.substring(0,1),left+26,y+14,selected?theme.ACCENT_DARK:theme.ACCENT);
            g.text(font,candidate.displayName,left+43,y+8,selected?theme.TEXT:theme.SECONDARY,true);
            int sections=candidate.settings.size()+candidate.subcategories.size();
            g.text(font,sections+(sections==1?" section":" sections"),left+43,y+22,selected?theme.SECONDARY:theme.MUTED,false);
            y+=44;
        }
        g.text(font,"Skyveil",left+15,top+windowHeight-30,theme.MUTED,false);
        g.text(font,"Make it your own",left+15,top+windowHeight-18,theme.MUTED,false);
    }

    private void drawContent(GuiGraphicsExtractor g,int mouseX,int mouseY){
        visibleRows.clear();headers.clear();
        String query=search==null?"":search.getValue();
        g.enableScissor(contentX,contentTop,contentRight,contentBottom);
        try{
            int endY;
            if(!query.isBlank())endY=drawSearch(g,query,mouseX,mouseY);
            else endY=drawAccordions(g,mouseX,mouseY);
            maxScroll=Math.max(0,endY+scroll-contentBottom+8);
            if(requestedScroll!=null){scroll=Math.max(0,Math.min(maxScroll,requestedScroll));requestedScroll=null;}
            if(scroll>maxScroll)scroll=maxScroll;
            drawScrollbar(g);
        }finally{g.disableScissor();}
    }

    private int drawAccordions(GuiGraphicsExtractor g,int mouseX,int mouseY){
        int y=contentTop+8-(int)scroll,right=contentRight-10;
        g.text(font,category.displayName,contentX+2,y,theme.ACCENT,true);
        y+=17;
        for(String line:wrapText(category.description,right-contentX-4)){
            g.text(font,line,contentX+2,y,theme.SECONDARY,false);y+=11;
        }
        y+=12;
        g.fill(contentX,y,right,y+1,theme.OUTLINE);y+=13;
        for(var setting:category.settings)y=drawSetting(g,setting,null,contentX,y,right,mouseX,mouseY,null);
        for(var group:category.subcategories){
            if(group.settings.size()==1){
                y=drawSetting(g,group.settings.getFirst(),null,contentX,y,right,mouseX,mouseY,null);
                continue;
            }
            boolean expanded=expandedGroups.contains(group);
            int headerHeight=44;
            drawHeader(g,group,expanded,contentX,y,right-contentX,headerHeight,mouseX,mouseY);
            headers.add(new Header(contentX,y,right-contentX,headerHeight,category,group));
            y+=headerHeight+6;
            if(expanded){
                int start=y;
                for(var setting:group.settings)y=drawSetting(g,setting,null,contentX+10,y,right,mouseX,mouseY,null);
                g.fill(contentX+2,start,contentX+3,y-8,theme.OUTLINE);
            }
            y+=6;
        }
        return y;
    }

    private void drawHeader(GuiGraphicsExtractor g,ConfigSubcategory group,boolean expanded,int x,int y,int width,int height,int mouseX,int mouseY){
        boolean hover=mouseY>=contentTop&&mouseY<contentBottom&&inside(mouseX,mouseY,x,y,width,height);
        g.fill(x,y,x+width,y+height,hover?theme.HOVER:theme.PANEL);
        g.fill(x,y+height-1,x+width,y+height,expanded?theme.ACCENT_DARK:theme.OUTLINE);
        g.fill(x+10,y+13,x+28,y+31,expanded?theme.ACCENT_DARK:theme.CARD);
        g.centeredText(font,expanded?"-":"+",x+19,y+18,theme.ACCENT);
        g.text(font,group.displayName,x+38,y+10,theme.ACCENT,true);
        g.text(font,group.settings.size()+" settings",x+38,y+26,theme.MUTED,false);
        g.text(font,expanded?"Collapse":"Expand",x+width-58,y+18,theme.SECONDARY,false);
    }
    private int drawSearch(GuiGraphicsExtractor g,String query,int mouseX,int mouseY){
        int y=contentTop+8-(int)scroll;
        g.text(font,"Search results",contentX+2,y,theme.TEXT,true);
        g.text(font,"Select a result to open its category and module.",contentX+2,y+16,theme.SECONDARY,false);
        y+=40;
        if(!query.equals(lastSearchQuery)){lastSearchQuery=query;searchResults=SearchManager.search(query);}
        for(SearchManager.Result result:searchResults)y=drawSearchResult(g,result,contentX,y,contentRight,mouseX,mouseY);
        if(searchResults.isEmpty()){
            g.fill(contentX,y,contentRight,y+54,theme.CARD_ALT);
            g.centeredText(font,"No matching settings",(contentX+contentRight)/2,y+20,theme.SECONDARY);
            y+=60;
        }
        return y;
    }

    private int drawSetting(GuiGraphicsExtractor g,SettingDefinition setting,String path,int x,int y,int right,int mouseX,int mouseY,SearchManager.Result result){
        if(result!=null)return drawSearchResult(g,result,x,y,right,mouseX,mouseY);
        if(setting==pendingScrollSetting){requestedScroll=y+scroll-contentTop;pendingScrollSetting=null;}
        int controlX=right-122,controlY=y+16;
        List<String> titleLines=wrapText(setting.name,Math.max(80,controlX-x-22));
        int descriptionY=10+titleLines.size()*11+8;
        List<String> descriptionLines=wrapText(setting.description,Math.max(80,controlX-x-22));
        boolean petStylePreview="hud.petDisplay.style".equals(setting.key);
        int textBottom=descriptionY+descriptionLines.size()*11;
        int height=petStylePreview?Math.max(116,textBottom+68):Math.max(60,textBottom+8);
        boolean hover=mouseY>=contentTop&&mouseY<contentBottom&&inside(mouseX,mouseY,x,y,right-x,height),highlight=setting==highlightedSetting&&System.currentTimeMillis()<highlightUntil;
        g.fill(x,y,right,y+height,highlight?theme.ACCENT_DARK:hover?theme.HOVER:theme.CARD);
        g.fill(x,y+height-1,right,y+height,highlight?theme.ACCENT:theme.OUTLINE);
        if(highlight)g.outline(x,y,right-x,height,theme.ACCENT);
        for(int line=0;line<titleLines.size();line++)g.text(font,titleLines.get(line),x+12,y+10+line*11,theme.TEXT,true);
        for(int line=0;line<descriptionLines.size();line++)g.text(font,descriptionLines.get(line),x+12,y+descriptionY+line*11,theme.SECONDARY,false);
        drawControl(g,setting,controlX,controlY,right);
        if(petStylePreview)PetDisplayHud.renderConfigPreview(g,minecraft,x+12,y+textBottom+4,.75f);
        visibleRows.add(new Row(x,y,right-x,height,setting,null,null,false));
        if(hover)g.setTooltipForNextFrame(Component.literal(setting.tooltip),mouseX,mouseY);
        return y+height+8;
    }

    private void drawControl(GuiGraphicsExtractor g,SettingDefinition setting,int controlX,int controlY,int right){
        switch(setting.type){
            case TOGGLE->{boolean on=(Boolean)setting.getter.get();g.fill(right-52,controlY,right-10,controlY+21,on?0xFF245C2D:theme.OFF);String label=on?"ON":"OFF";g.text(font,label,right-31-font.width(label)/2,controlY+7,theme.TEXT,false);}
            case DECIMAL_SLIDER->{double value=(Double)setting.getter.get(),fraction=(value-setting.minimum)/(setting.maximum-setting.minimum);g.fill(controlX,controlY+13,right-10,controlY+16,theme.OFF);int knob=controlX+(int)((right-10-controlX)*fraction);g.fill(controlX,controlY+13,knob,controlY+16,theme.ACCENT);g.fill(knob-3,controlY+9,knob+3,controlY+20,theme.TEXT);g.text(font,String.format(Locale.ROOT,"%.2f",value),controlX,controlY-2,theme.TEXT,true);}
            case COLOR->{int color=(Integer)setting.getter.get();String hex=String.format(Locale.ROOT,"#%06X",color&0xFFFFFF);g.fill(right-112,controlY,right-86,controlY+22,color);g.outline(right-112,controlY,26,22,theme.TEXT);g.text(font,hex,right-80,controlY+7,theme.TEXT,true);}
            case BUTTON->{g.fill(right-79,controlY,right-10,controlY+23,theme.ACCENT_DARK);g.centeredText(font,setting.buttonLabel,right-44,controlY+7,theme.TEXT);}
            case KEYBIND->{int key=(Integer)setting.getter.get();String label=awaitingKey==setting?"Press key...":InputConstants.getKey(new KeyEvent(key,0,0)).getDisplayName().getString();int width=Math.max(70,font.width(label)+14);g.fill(right-10-width,controlY,right-10,controlY+23,theme.ACCENT_DARK);g.centeredText(font,label,right-10-width/2,controlY+7,theme.TEXT);}
            case CHORD->{ChatCopyBinding binding=ChatCopyManager.normalize((ChatCopyBinding)setting.getter.get());String label=awaitingChord==setting?"Press keybind...":ChatCopyManager.chordName(binding);int width=Math.max(90,font.width(label)+14);g.fill(right-10-width,controlY,right-10,controlY+23,theme.ACCENT_DARK);g.centeredText(font,label,right-10-width/2,controlY+7,theme.TEXT);}
            case CHOICE->{String label=String.valueOf(setting.getter.get());int width=Math.max(82,font.width(label)+22);g.fill(right-10-width,controlY,right-10,controlY+23,theme.ACCENT_DARK);g.centeredText(font,label,right-10-width/2,controlY+7,theme.TEXT);}
        }
    }

    private int drawSearchResult(GuiGraphicsExtractor g,SearchManager.Result result,int x,int y,int right,int mouseX,int mouseY){
        int height=46;boolean hover=inside(mouseX,mouseY,x,y,right-x,height);
        g.fill(x,y,right,y+height,hover?theme.HOVER:theme.CARD);
        g.fill(x,y,x+3,y+height,theme.ACCENT_DARK);
        g.text(font,result.setting().name,x+12,y+9,theme.TEXT,true);
        g.text(font,result.path(),x+12,y+27,theme.ACCENT,false);
        g.text(font,">",right-18,y+17,hover?theme.TEXT:theme.MUTED,true);
        visibleRows.add(new Row(x,y,right-x,height,result.setting(),result,null,false));
        return y+height+7;
    }

    private void drawScrollbar(GuiGraphicsExtractor g){
        if(maxScroll<=0)return;
        int trackX=contentRight-3,trackHeight=contentBottom-contentTop;
        g.fill(trackX,contentTop,trackX+2,contentBottom,theme.PANEL);
        int thumb=Math.max(24,(int)(trackHeight*(trackHeight/(trackHeight+maxScroll))));
        int y=contentTop+(int)((trackHeight-thumb)*(scroll/maxScroll));
        g.fill(trackX,y,trackX+2,y+thumb,theme.ACCENT);
    }

    private List<String> wrapText(String text,int maxWidth){
        List<String> lines=new ArrayList<>();StringBuilder line=new StringBuilder();
        for(String word:text.trim().split("\\s+")){String candidate=line.isEmpty()?word:line+" "+word;if(!line.isEmpty()&&font.width(candidate)>maxWidth){lines.add(line.toString());line.setLength(0);line.append(word);}else{if(!line.isEmpty())line.append(' ');line.append(word);}}
        if(!line.isEmpty())lines.add(line.toString());if(lines.isEmpty())lines.add("");return lines;
    }

    @Override public boolean mouseClicked(MouseButtonEvent event,boolean doubleClick){
        if(awaitingChord!=null){for(int key=GLFW.GLFW_KEY_SPACE;key<=GLFW.GLFW_KEY_LAST;key++)if(InputConstants.isKeyDown(minecraft.getWindow(),key))capturedChordKeys.add(key);ChatCopyBinding binding=((ChatCopyBinding)awaitingChord.getter.get()).copy();binding.keys=new ArrayList<>(capturedChordKeys);binding.mouseButton=event.button();awaitingChord.set(ChatCopyManager.normalize(binding));awaitingChord=null;capturedChordKeys.clear();return true;}
        if(event.button()!=0)return super.mouseClicked(event,doubleClick);
        double mouseX=event.x(),mouseY=event.y();int y=top+70;
        for(ConfigCategory candidate:SettingsRegistry.categories()){
            if(inside(mouseX,mouseY,left+10,y,sidebarWidth-16,38)){category=candidate;scroll=0;pendingScrollSetting=null;requestedScroll=null;awaitingKey=null;awaitingChord=null;capturedChordKeys.clear();
                search.setValue("");return true;}y+=44;
        }
        if(!inside(mouseX,mouseY,contentX,contentTop,contentRight-contentX,contentBottom-contentTop))return super.mouseClicked(event,doubleClick);
        for(var header:headers)if(inside(mouseX,mouseY,header.x,header.y,header.width,header.height)){
            category=header.category;
            if(!expandedGroups.remove(header.group))expandedGroups.add(header.group);
            awaitingKey=null;awaitingChord=null;capturedChordKeys.clear();
            return true;
        }
        for(Row row:visibleRows)if(inside(mouseX,mouseY,row.x,row.y,row.width,row.height)){
            if(row.result!=null){navigateTo(row.result);return true;}
            SettingDefinition setting=row.setting;if(setting==null)continue;
            int right=row.x+row.width;
            switch(setting.type){
                case TOGGLE->setting.set(!(Boolean)setting.getter.get());
                case DECIMAL_SLIDER->{draggedSlider=setting;dragSliderStart=right-122;dragSliderEnd=right-10;setSlider(setting,mouseX,dragSliderStart,dragSliderEnd);}
                case COLOR->{minecraft.setScreen(new HexColorEditorScreen(this,setting));return true;}
                case BUTTON->setting.set(null);
                case KEYBIND->awaitingKey=setting;
                case CHORD->{awaitingChord=setting;capturedChordKeys.clear();}
                case CHOICE->{String current=String.valueOf(setting.getter.get());int index=setting.choices.indexOf(current);setting.set(setting.choices.get((index+1+setting.choices.size())%setting.choices.size()));}
            }
            return true;
        }
        return super.mouseClicked(event,doubleClick);
    }

    private void navigateTo(SearchManager.Result result){
        category=result.category();
        if(result.subcategory()!=null)expandedGroups.add(result.subcategory());
        pendingScrollSetting=result.setting();highlightedSetting=result.setting();highlightUntil=System.currentTimeMillis()+1600;
        scroll=0;search.setValue("");search.setFocused(false);
    }

    @Override public boolean mouseDragged(MouseButtonEvent event,double dragX,double dragY){if(draggedSlider!=null){setSlider(draggedSlider,event.x(),dragSliderStart,dragSliderEnd);return true;}return super.mouseDragged(event,dragX,dragY);}
    @Override public boolean mouseReleased(MouseButtonEvent event){if(draggedSlider!=null){draggedSlider=null;ConfigManager.save();return true;}return super.mouseReleased(event);}
    private void setSlider(SettingDefinition setting,double mouseX,int start,int end){double fraction=Math.max(0,Math.min(1,(mouseX-start)/(end-start)));setting.setter.accept(setting.minimum+(setting.maximum-setting.minimum)*fraction);}

    @Override public boolean mouseScrolled(double mouseX,double mouseY,double horizontal,double vertical){if(inside(mouseX,mouseY,contentX,contentTop,contentRight-contentX,contentBottom-contentTop)){scroll=Math.max(0,Math.min(maxScroll,scroll-vertical*25));return true;}return super.mouseScrolled(mouseX,mouseY,horizontal,vertical);}
    @Override public boolean keyPressed(KeyEvent event){
        if(search!=null&&search.isFocused()){awaitingKey=null;awaitingChord=null;capturedChordKeys.clear();return super.keyPressed(event);}
        if(awaitingChord!=null){if(event.key()==GLFW.GLFW_KEY_ESCAPE){awaitingChord=null;capturedChordKeys.clear();return true;}if(ChatCopyManager.validKey(event.key()))capturedChordKeys.add(event.key());return true;}
        if(awaitingKey!=null){awaitingKey.set(event.key());awaitingKey=null;return true;}
        return super.keyPressed(event);
    }
    @Override public void onClose(){if(draggedSlider!=null)ConfigManager.save();super.onClose();}
    @Override public boolean isPauseScreen(){return false;}
    private static boolean inside(double mouseX,double mouseY,int x,int y,int width,int height){return mouseX>=x&&mouseX<x+width&&mouseY>=y&&mouseY<y+height;}
    private record Header(int x,int y,int width,int height,ConfigCategory category,ConfigSubcategory group){}
    private record Row(int x,int y,int width,int height,SettingDefinition setting,SearchManager.Result result,ConfigSubcategory group,boolean back){}
    public static void open(){Minecraft.getInstance().setScreen(new SkyveilConfigScreen());}
    public static void open(String settingKey){
        var destination=SearchManager.findSetting(settingKey);
        var screen=new SkyveilConfigScreen();
        Minecraft.getInstance().setScreen(screen);
        if(destination!=null)screen.navigateTo(destination);
    }
}
