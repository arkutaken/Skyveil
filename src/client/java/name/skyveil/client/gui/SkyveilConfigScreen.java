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

/** Category dashboard with focused, expandable feature-setting pages. */
public final class SkyveilConfigScreen extends Screen {
    private ConfigCategory category;
    private ConfigSubcategory openGroup;
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
        if(openGroup!=null&&!category.subcategories.contains(openGroup))openGroup=null;
        search=new EditBox(font,contentX,top+15,contentRight-contentX,22,Component.literal("Search Skyveil settings"));
        search.setHint(Component.literal("Search settings..."));
        search.setResponder(value->{scroll=0;lastSearchQuery=value;searchResults=SearchManager.search(value);});
        addRenderableWidget(search);
    }

    @Override public void extractRenderState(GuiGraphicsExtractor g,int mouseX,int mouseY,float partialTick){
        g.fill(0,0,width,height,SkyveilTheme.SCRIM);
        g.fill(left,top,left+windowWidth,top+windowHeight,SkyveilTheme.WINDOW);
        g.fill(left,top,left+windowWidth,top+45,SkyveilTheme.WINDOW_TOP);
        g.fill(left,top,left+4,top+windowHeight,SkyveilTheme.ACCENT);
        g.outline(left,top,windowWidth,windowHeight,SkyveilTheme.OUTLINE);
        g.fill(left+4,top+45,left+sidebarWidth,top+windowHeight,SkyveilTheme.SIDEBAR);
        g.text(font,"SKYVEIL",left+16,top+16,SkyveilTheme.TEXT,true);
        g.text(font,"CONFIG",left+16+font.width("SKYVEIL")+6,top+16,SkyveilTheme.ACCENT,true);
        drawCategories(g,mouseX,mouseY);
        drawContent(g,mouseX,mouseY);
        super.extractRenderState(g,mouseX,mouseY,partialTick);
    }

    private void drawCategories(GuiGraphicsExtractor g,int mouseX,int mouseY){
        int y=top+61;
        g.text(font,"CATEGORIES",left+15,y-9,SkyveilTheme.MUTED,true);
        for(ConfigCategory candidate:SettingsRegistry.categories()){
            boolean selected=candidate==category,hover=inside(mouseX,mouseY,left+10,y,sidebarWidth-16,38);
            if(selected)g.fill(left+10,y,left+sidebarWidth-6,y+38,SkyveilTheme.ACCENT_DARK);
            else if(hover)g.fill(left+10,y,left+sidebarWidth-6,y+38,SkyveilTheme.HOVER);
            g.fill(left+17,y+9,left+35,y+27,selected?SkyveilTheme.TEXT:SkyveilTheme.PANEL);
            g.centeredText(font,candidate.displayName.substring(0,1),left+26,y+14,selected?SkyveilTheme.ACCENT_DARK:SkyveilTheme.ACCENT);
            g.text(font,candidate.displayName,left+43,y+8,selected?SkyveilTheme.TEXT:SkyveilTheme.SECONDARY,true);
            int features=candidate.settings.size()+candidate.subcategories.size();
            g.text(font,features+" features",left+43,y+22,selected?0xFFDCD2EF:SkyveilTheme.MUTED,false);
            y+=44;
        }
        g.text(font,"Click a feature group",left+15,top+windowHeight-30,SkyveilTheme.MUTED,false);
        g.text(font,"to configure it",left+15,top+windowHeight-18,SkyveilTheme.MUTED,false);
    }

    private void drawContent(GuiGraphicsExtractor g,int mouseX,int mouseY){
        visibleRows.clear();
        String query=search==null?"":search.getValue();
        g.enableScissor(contentX,contentTop,contentRight,contentBottom);
        try{
            int endY;
            if(!query.isBlank())endY=drawSearch(g,query,mouseX,mouseY);
            else if(openGroup==null)endY=drawOverview(g,mouseX,mouseY);
            else endY=drawGroup(g,mouseX,mouseY);
            maxScroll=Math.max(0,endY+scroll-contentBottom+8);
            if(scroll>maxScroll)scroll=maxScroll;
            drawScrollbar(g);
        }finally{g.disableScissor();}
    }

    private int drawOverview(GuiGraphicsExtractor g,int mouseX,int mouseY){
        int y=contentTop+7-(int)scroll;
        g.text(font,category.displayName,contentX+2,y,SkyveilTheme.TEXT,true);
        g.text(font,category.description,contentX+2,y+16,SkyveilTheme.SECONDARY,false);
        y+=43;
        int gap=9,cardWidth=(contentRight-contentX-gap)/2,cardHeight=76;
        int directCount=category.settings.size(),total=directCount+category.subcategories.size();
        for(int index=0;index<total;index++){
            int column=index%2,row=index/2,x=contentX+column*(cardWidth+gap),cardY=y+row*(cardHeight+gap);
            if(index<directCount){
                SettingDefinition setting=category.settings.get(index);drawDirectCard(g,setting,x,cardY,cardWidth,cardHeight,mouseX,mouseY);
                visibleRows.add(new Row(x,cardY,cardWidth,cardHeight,setting,null,null,false));
            }else{
                ConfigSubcategory group=category.subcategories.get(index-directCount);drawGroupCard(g,group,x,cardY,cardWidth,cardHeight,mouseX,mouseY);
                visibleRows.add(new Row(x,cardY,cardWidth,cardHeight,null,null,group,false));
            }
        }
        int rows=(total+1)/2;
        return y+rows*(cardHeight+gap);
    }

    private void drawDirectCard(GuiGraphicsExtractor g,SettingDefinition setting,int x,int y,int width,int height,int mouseX,int mouseY){
        boolean hover=inside(mouseX,mouseY,x,y,width,height),highlight=setting==highlightedSetting&&System.currentTimeMillis()<highlightUntil;
        g.fill(x,y,x+width,y+height,highlight?SkyveilTheme.ACCENT_DARK:hover?SkyveilTheme.HOVER:SkyveilTheme.CARD);
        g.fill(x,y,x+3,y+height,highlight?SkyveilTheme.TEXT:SkyveilTheme.ACCENT);
        g.outline(x,y,width,height,hover?SkyveilTheme.ACCENT:SkyveilTheme.OUTLINE);
        g.text(font,setting.name,x+12,y+10,SkyveilTheme.TEXT,true);
        List<String> lines=wrapText(setting.description,width-24);
        for(int line=0;line<Math.min(2,lines.size());line++)g.text(font,lines.get(line),x+12,y+27+line*11,SkyveilTheme.SECONDARY,false);
        g.text(font,"Direct action",x+12,y+height-15,SkyveilTheme.MUTED,false);
        String action="Open >";g.text(font,action,x+width-font.width(action)-11,y+height-15,hover?SkyveilTheme.TEXT:SkyveilTheme.ACCENT,true);
    }

    private void drawGroupCard(GuiGraphicsExtractor g,ConfigSubcategory group,int x,int y,int width,int height,int mouseX,int mouseY){
        boolean hover=inside(mouseX,mouseY,x,y,width,height);
        g.fill(x,y,x+width,y+height,hover?SkyveilTheme.HOVER:SkyveilTheme.CARD);
        g.fill(x,y,x+3,y+height,hover?SkyveilTheme.ACCENT:SkyveilTheme.ACCENT_DARK);
        g.outline(x,y,width,height,hover?SkyveilTheme.ACCENT:SkyveilTheme.OUTLINE);
        g.text(font,group.displayName,x+12,y+10,SkyveilTheme.TEXT,true);
        List<String> lines=wrapText(group.description,width-24);
        for(int line=0;line<Math.min(2,lines.size());line++)g.text(font,lines.get(line),x+12,y+27+line*11,SkyveilTheme.SECONDARY,false);
        String count=group.settings.size()==1?"1 setting":group.settings.size()+" settings";
        g.text(font,count,x+12,y+height-15,SkyveilTheme.MUTED,false);
        String action="Configure >";
        g.text(font,action,x+width-font.width(action)-11,y+height-15,hover?SkyveilTheme.TEXT:SkyveilTheme.ACCENT,true);
    }

    private int drawGroup(GuiGraphicsExtractor g,int mouseX,int mouseY){
        int y=contentTop+7-(int)scroll;
        boolean backHover=inside(mouseX,mouseY,contentX,y,62,20);
        g.fill(contentX,y,contentX+62,y+20,backHover?SkyveilTheme.HOVER:SkyveilTheme.PANEL);
        g.text(font,"< Back",contentX+10,y+6,backHover?SkyveilTheme.TEXT:SkyveilTheme.ACCENT,true);
        visibleRows.add(new Row(contentX,y,62,20,null,null,null,true));
        g.text(font,category.displayName+"  /  ",contentX+75,y+6,SkyveilTheme.MUTED,false);
        g.text(font,openGroup.displayName,contentX+75+font.width(category.displayName+"  /  "),y+6,SkyveilTheme.ACCENT,true);
        g.text(font,openGroup.description,contentX+2,y+29,SkyveilTheme.SECONDARY,false);
        y+=53;
        for(SettingDefinition setting:openGroup.settings){
            if(setting==pendingScrollSetting){scroll=Math.max(0,y-(contentTop+8));pendingScrollSetting=null;}
            y=drawSetting(g,setting,null,contentX,y,contentRight,mouseX,mouseY,null);
        }
        return y;
    }

    private int drawSearch(GuiGraphicsExtractor g,String query,int mouseX,int mouseY){
        int y=contentTop+8-(int)scroll;
        g.text(font,"Search results",contentX+2,y,SkyveilTheme.TEXT,true);
        g.text(font,"Select a result to open its feature group.",contentX+2,y+16,SkyveilTheme.SECONDARY,false);
        y+=40;
        if(!query.equals(lastSearchQuery)){lastSearchQuery=query;searchResults=SearchManager.search(query);}
        for(SearchManager.Result result:searchResults)y=drawSearchResult(g,result,contentX,y,contentRight,mouseX,mouseY);
        if(searchResults.isEmpty()){
            g.fill(contentX,y,contentRight,y+54,SkyveilTheme.CARD_ALT);
            g.centeredText(font,"No matching settings",(contentX+contentRight)/2,y+20,SkyveilTheme.SECONDARY);
            y+=60;
        }
        return y;
    }

    private int drawSetting(GuiGraphicsExtractor g,SettingDefinition setting,String path,int x,int y,int right,int mouseX,int mouseY,SearchManager.Result result){
        if(result!=null)return drawSearchResult(g,result,x,y,right,mouseX,mouseY);
        int controlX=right-122,controlY=y+16;
        List<String> descriptionLines=wrapText(setting.description,Math.max(80,controlX-x-22));
        boolean petStylePreview="hud.petDisplay.style".equals(setting.key);
        int height=petStylePreview?116:Math.max(60,34+descriptionLines.size()*11);
        boolean hover=inside(mouseX,mouseY,x,y,right-x,height),highlight=setting==highlightedSetting&&System.currentTimeMillis()<highlightUntil;
        g.fill(x,y,right,y+height,highlight?SkyveilTheme.ACCENT_DARK:hover?SkyveilTheme.HOVER:SkyveilTheme.CARD);
        g.fill(x,y,x+3,y+height,highlight?SkyveilTheme.TEXT:SkyveilTheme.ACCENT_DARK);
        g.outline(x,y,right-x,height,hover?SkyveilTheme.OUTLINE:0xFF332C42);
        g.text(font,setting.name,x+12,y+10,SkyveilTheme.TEXT,true);
        for(int line=0;line<descriptionLines.size();line++)g.text(font,descriptionLines.get(line),x+12,y+29+line*11,SkyveilTheme.SECONDARY,false);
        drawControl(g,setting,controlX,controlY,right);
        if(petStylePreview)PetDisplayHud.renderConfigPreview(g,minecraft,x+12,y+53,.75f);
        visibleRows.add(new Row(x,y,right-x,height,setting,null,null,false));
        if(hover)g.setTooltipForNextFrame(Component.literal(setting.tooltip),mouseX,mouseY);
        return y+height+8;
    }

    private void drawControl(GuiGraphicsExtractor g,SettingDefinition setting,int controlX,int controlY,int right){
        switch(setting.type){
            case TOGGLE->{boolean on=(Boolean)setting.getter.get();g.fill(right-52,controlY,right-10,controlY+21,on?SkyveilTheme.ACCENT_DARK:SkyveilTheme.OFF);g.fill(on?right-30:right-49,controlY+3,on?right-13:right-32,controlY+18,SkyveilTheme.TEXT);}
            case DECIMAL_SLIDER->{double value=(Double)setting.getter.get(),fraction=(value-setting.minimum)/(setting.maximum-setting.minimum);g.fill(controlX,controlY+13,right-10,controlY+16,SkyveilTheme.OFF);int knob=controlX+(int)((right-10-controlX)*fraction);g.fill(controlX,controlY+13,knob,controlY+16,SkyveilTheme.ACCENT);g.fill(knob-3,controlY+9,knob+3,controlY+20,SkyveilTheme.TEXT);g.text(font,String.format(Locale.ROOT,"%.2f",value),controlX,controlY-2,SkyveilTheme.TEXT,true);}
            case COLOR->{int color=(Integer)setting.getter.get();g.fill(right-46,controlY,right-10,controlY+22,color);g.outline(right-46,controlY,36,22,SkyveilTheme.TEXT);}
            case BUTTON->{g.fill(right-79,controlY,right-10,controlY+23,SkyveilTheme.ACCENT_DARK);g.centeredText(font,setting.buttonLabel,right-44,controlY+7,SkyveilTheme.TEXT);}
            case KEYBIND->{int key=(Integer)setting.getter.get();String label=awaitingKey==setting?"Press key...":InputConstants.getKey(new KeyEvent(key,0,0)).getDisplayName().getString();int width=Math.max(70,font.width(label)+14);g.fill(right-10-width,controlY,right-10,controlY+23,SkyveilTheme.ACCENT_DARK);g.centeredText(font,label,right-10-width/2,controlY+7,SkyveilTheme.TEXT);}
            case CHORD->{ChatCopyBinding binding=ChatCopyManager.normalize((ChatCopyBinding)setting.getter.get());String label=awaitingChord==setting?"Press keybind...":ChatCopyManager.chordName(binding);int width=Math.max(90,font.width(label)+14);g.fill(right-10-width,controlY,right-10,controlY+23,SkyveilTheme.ACCENT_DARK);g.centeredText(font,label,right-10-width/2,controlY+7,SkyveilTheme.TEXT);}
            case CHOICE->{String label=String.valueOf(setting.getter.get());int width=Math.max(82,font.width(label)+22);g.fill(right-10-width,controlY,right-10,controlY+23,SkyveilTheme.ACCENT_DARK);g.centeredText(font,label,right-10-width/2,controlY+7,SkyveilTheme.TEXT);}
        }
    }

    private int drawSearchResult(GuiGraphicsExtractor g,SearchManager.Result result,int x,int y,int right,int mouseX,int mouseY){
        int height=46;boolean hover=inside(mouseX,mouseY,x,y,right-x,height);
        g.fill(x,y,right,y+height,hover?SkyveilTheme.HOVER:SkyveilTheme.CARD);
        g.fill(x,y,x+3,y+height,SkyveilTheme.ACCENT_DARK);
        g.text(font,result.setting().name,x+12,y+9,SkyveilTheme.TEXT,true);
        g.text(font,result.path(),x+12,y+27,SkyveilTheme.ACCENT,false);
        g.text(font,">",right-18,y+17,hover?SkyveilTheme.TEXT:SkyveilTheme.MUTED,true);
        visibleRows.add(new Row(x,y,right-x,height,result.setting(),result,null,false));
        return y+height+7;
    }

    private void drawScrollbar(GuiGraphicsExtractor g){
        if(maxScroll<=0)return;
        int trackX=contentRight-3,trackHeight=contentBottom-contentTop;
        g.fill(trackX,contentTop,trackX+2,contentBottom,SkyveilTheme.PANEL);
        int thumb=Math.max(24,(int)(trackHeight*(trackHeight/(trackHeight+maxScroll))));
        int y=contentTop+(int)((trackHeight-thumb)*(scroll/maxScroll));
        g.fill(trackX,y,trackX+2,y+thumb,SkyveilTheme.ACCENT);
    }

    private List<String> wrapText(String text,int maxWidth){
        List<String> lines=new ArrayList<>();StringBuilder line=new StringBuilder();
        for(String word:text.trim().split("\\s+")){String candidate=line.isEmpty()?word:line+" "+word;if(!line.isEmpty()&&font.width(candidate)>maxWidth){lines.add(line.toString());line.setLength(0);line.append(word);}else{if(!line.isEmpty())line.append(' ');line.append(word);}}
        if(!line.isEmpty())lines.add(line.toString());if(lines.isEmpty())lines.add("");return lines;
    }

    @Override public boolean mouseClicked(MouseButtonEvent event,boolean doubleClick){
        if(awaitingChord!=null){for(int key=GLFW.GLFW_KEY_SPACE;key<=GLFW.GLFW_KEY_LAST;key++)if(InputConstants.isKeyDown(minecraft.getWindow(),key))capturedChordKeys.add(key);ChatCopyBinding binding=((ChatCopyBinding)awaitingChord.getter.get()).copy();binding.keys=new ArrayList<>(capturedChordKeys);binding.mouseButton=event.button();awaitingChord.set(ChatCopyManager.normalize(binding));awaitingChord=null;capturedChordKeys.clear();return true;}
        if(event.button()!=0)return super.mouseClicked(event,doubleClick);
        double mouseX=event.x(),mouseY=event.y();int y=top+61;
        for(ConfigCategory candidate:SettingsRegistry.categories()){
            if(inside(mouseX,mouseY,left+10,y,sidebarWidth-16,38)){category=candidate;openGroup=null;search.setValue("");scroll=0;return true;}y+=44;
        }
        if(!inside(mouseX,mouseY,contentX,contentTop,contentRight-contentX,contentBottom-contentTop))return super.mouseClicked(event,doubleClick);
        for(Row row:visibleRows)if(inside(mouseX,mouseY,row.x,row.y,row.width,row.height)){
            if(row.back){openGroup=null;scroll=0;return true;}
            if(row.group!=null){openGroup=row.group;scroll=0;return true;}
            if(row.result!=null){navigateTo(row.result);return true;}
            SettingDefinition setting=row.setting;if(setting==null)continue;
            int right=row.x+row.width;
            switch(setting.type){
                case TOGGLE->setting.set(!(Boolean)setting.getter.get());
                case DECIMAL_SLIDER->{draggedSlider=setting;dragSliderStart=right-122;dragSliderEnd=right-10;setSlider(setting,mouseX,dragSliderStart,dragSliderEnd);}
                case COLOR->{int current=(Integer)setting.getter.get();setting.set(current==0xFFFFFFFF?0xFF9B6CFF:current==0xFF9B6CFF?0xFFFFD166:0xFFFFFFFF);}
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
        category=result.category();openGroup=result.subcategory();pendingScrollSetting=openGroup==null?null:result.setting();highlightedSetting=result.setting();highlightUntil=System.currentTimeMillis()+1600;scroll=0;search.setValue("");search.setFocused(false);
    }

    @Override public boolean mouseDragged(MouseButtonEvent event,double dragX,double dragY){if(draggedSlider!=null){setSlider(draggedSlider,event.x(),dragSliderStart,dragSliderEnd);return true;}return super.mouseDragged(event,dragX,dragY);}
    @Override public boolean mouseReleased(MouseButtonEvent event){if(draggedSlider!=null){draggedSlider=null;ConfigManager.save();return true;}return super.mouseReleased(event);}
    private void setSlider(SettingDefinition setting,double mouseX,int start,int end){double fraction=Math.max(0,Math.min(1,(mouseX-start)/(end-start)));setting.setter.accept(setting.minimum+(setting.maximum-setting.minimum)*fraction);}

    @Override public boolean mouseScrolled(double mouseX,double mouseY,double horizontal,double vertical){if(inside(mouseX,mouseY,contentX,contentTop,contentRight-contentX,contentBottom-contentTop)){scroll=Math.max(0,Math.min(maxScroll,scroll-vertical*25));return true;}return super.mouseScrolled(mouseX,mouseY,horizontal,vertical);}
    @Override public boolean keyPressed(KeyEvent event){
        if(search!=null&&search.isFocused()){awaitingKey=null;awaitingChord=null;capturedChordKeys.clear();return super.keyPressed(event);}
        if(awaitingChord!=null){if(event.key()==GLFW.GLFW_KEY_ESCAPE){awaitingChord=null;capturedChordKeys.clear();return true;}if(ChatCopyManager.validKey(event.key()))capturedChordKeys.add(event.key());return true;}
        if(awaitingKey!=null){awaitingKey.set(event.key());awaitingKey=null;return true;}
        if(event.key()==GLFW.GLFW_KEY_ESCAPE&&openGroup!=null){openGroup=null;scroll=0;return true;}
        return super.keyPressed(event);
    }
    @Override public void onClose(){if(draggedSlider!=null)ConfigManager.save();super.onClose();}
    @Override public boolean isPauseScreen(){return false;}
    private static boolean inside(double mouseX,double mouseY,int x,int y,int width,int height){return mouseX>=x&&mouseX<x+width&&mouseY>=y&&mouseY<y+height;}
    private record Row(int x,int y,int width,int height,SettingDefinition setting,SearchManager.Result result,ConfigSubcategory group,boolean back){}
    public static void open(){Minecraft.getInstance().setScreen(new SkyveilConfigScreen());}
}
