package name.skyveil.client.gui;

import name.skyveil.client.config.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.network.chat.Component;

import java.util.*;

/** Compact, non-pausing, data-driven Skyveil settings screen. */
public final class SkyveilConfigScreen extends Screen {
    private ConfigCategory category;
    private ConfigSubcategory subcategory;
    private EditBox search;
    private double scroll;
    private double maxScroll;
    private String lastSearchQuery="";
    private List<SearchManager.Result> searchResults=List.of();
    private SettingDefinition pendingScrollSetting;
    private SettingDefinition highlightedSetting;
    private long highlightUntil;
    private int left, top, windowWidth, windowHeight, sidebarWidth;
    private final List<Row> visibleRows = new ArrayList<>();
    private SettingDefinition draggedSlider;
    private SettingDefinition awaitingKey;

    public SkyveilConfigScreen() { super(Component.literal("Skyveil Settings")); }

    @Override protected void init() {
        windowWidth=Math.min(760, Math.max(420, (int)(width*.70)));
        windowHeight=Math.min(520, Math.max(280, (int)(height*.70)));
        left=(width-windowWidth)/2; top=(height-windowHeight)/2; sidebarWidth=Math.max(110,windowWidth/5);
        if(category==null) {
            category=SettingsRegistry.categories().stream().filter(c -> c.id.equals("general")).findFirst().orElse(SettingsRegistry.categories().get(0));
            subcategory=category.subcategories.stream().filter(s -> s.id.equals("general")).findFirst().orElse(category.subcategories.get(0));
        }
        if(subcategory==null || !category.subcategories.contains(subcategory)) subcategory=category.subcategories.get(0);
        search=new EditBox(font, left+sidebarWidth+18, top+12, windowWidth-sidebarWidth-32, 20, Component.literal("Search features"));
        search.setHint(Component.literal("Search features..."));
        search.setResponder(value -> {scroll=0;lastSearchQuery=value;searchResults=SearchManager.search(value);});
        addRenderableWidget(search);
    }

    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0,0,width,height,SkyveilTheme.SCRIM);
        graphics.fill(left,top,left+windowWidth,top+windowHeight,SkyveilTheme.WINDOW);
        graphics.outline(left,top,windowWidth,windowHeight,SkyveilTheme.OUTLINE);
        graphics.fill(left,top,left+sidebarWidth,top+windowHeight,SkyveilTheme.SIDEBAR);
        graphics.text(font,"SKYVEIL",left+14,top+17,SkyveilTheme.ACCENT,true);
        drawCategories(graphics,mouseX,mouseY);
        drawContent(graphics,mouseX,mouseY);
        super.extractRenderState(graphics,mouseX,mouseY,partialTick);
    }

    private void drawCategories(GuiGraphicsExtractor g,int mx,int my) {
        int y=top+51;
        for(ConfigCategory c:SettingsRegistry.categories()) {
            boolean selected=c==category, hover=inside(mx,my,left+7,y-4,sidebarWidth-14,24);
            if(selected||hover) g.fill(left+7,y-4,left+sidebarWidth-7,y+20,selected?SkyveilTheme.ACCENT_DARK:SkyveilTheme.HOVER);
            g.text(font,c.displayName,left+17,y+3,selected?SkyveilTheme.TEXT:SkyveilTheme.SECONDARY,true); y+=27;
        }
    }

    private void drawContent(GuiGraphicsExtractor g,int mx,int my) {
        int x=left+sidebarWidth+14, contentTop=top+44, right=left+windowWidth-12;
        String query=search==null?"":search.getValue();
        if(query.isBlank()&&pendingScrollSetting!=null){scroll=scrollToSetting(pendingScrollSetting,x,right,contentTop);pendingScrollSetting=null;}
        g.enableScissor(x,contentTop,right,top+windowHeight-10);
        visibleRows.clear(); int y=contentTop+6-(int)scroll;
        if(!query.isBlank()) {
            g.text(font,"Feature search",x+4,y,SkyveilTheme.TEXT,true); y+=22;
            if(!query.equals(lastSearchQuery)){lastSearchQuery=query;searchResults=SearchManager.search(query);}
            for(SearchManager.Result result:searchResults) {
                y=drawSetting(g,result.setting(),result.category().displayName+" → "+result.subcategory().displayName,x,y,right,mx,my,result);
            }
            if(searchResults.isEmpty()) g.text(font,"No features found",x+4,y,SkyveilTheme.SECONDARY,true);
        } else {
            g.text(font,category.displayName,x+4,y,SkyveilTheme.TEXT,true); y+=20;
            int sx=x+4;
            for(ConfigSubcategory sub:category.subcategories) {
                int w=font.width(sub.displayName)+16; boolean selected=sub==subcategory, hover=inside(mx,my,sx,y,w,20);
                g.fill(sx,y,sx+w,y+20,selected?SkyveilTheme.ACCENT_DARK:hover?SkyveilTheme.HOVER:SkyveilTheme.PANEL);
                g.text(font,sub.displayName,sx+8,y+6,selected?SkyveilTheme.TEXT:SkyveilTheme.SECONDARY,true);
                visibleRows.add(new Row(sx,y,w,20,null,null,sub)); sx+=w+5;
            }
            y+=30;
            if(subcategory.settings.isEmpty()) g.text(font,"No settings in this section yet.",x+4,y,SkyveilTheme.SECONDARY,true);
            for(SettingDefinition setting:subcategory.settings) y=drawSetting(g,setting,null,x,y,right,mx,my,null);
        }
        maxScroll=Math.max(0,y+scroll-(top+windowHeight-10));
        if(scroll>maxScroll)scroll=maxScroll;
        g.disableScissor();
    }

    private int drawSetting(GuiGraphicsExtractor g, SettingDefinition s, String path,int x,int y,int right,int mx,int my,SearchManager.Result result) {
        if(result!=null)return drawSearchResult(g,result,x,y,right,mx,my);
        int controlX=right-112, controlY=y+15;
        List<String> descriptionLines=wrapText(s.description,Math.max(60,controlX-x-18));
        int descriptionBottom=y+25+descriptionLines.size()*11;
        int pathY=descriptionBottom+2;
        int h=settingHeight(s,path,x,right);
        boolean hover=inside(mx,my,x,y,right-x,h),highlight=s==highlightedSetting&&System.currentTimeMillis()<highlightUntil;
        g.fill(x,y,right,y+h,highlight?SkyveilTheme.ACCENT_DARK:hover?SkyveilTheme.HOVER:SkyveilTheme.CARD);
        g.text(font,s.name,x+10,y+9,SkyveilTheme.TEXT,true);
        for(int line=0;line<descriptionLines.size();line++)g.text(font,descriptionLines.get(line),x+10,y+25+line*11,SkyveilTheme.SECONDARY,false);
        if(path!=null) g.text(font,path,x+10,pathY,SkyveilTheme.ACCENT,false);
        switch(s.type) {
            case TOGGLE -> {
                boolean on=(Boolean)s.getter.get(); g.fill(right-48,controlY,right-10,controlY+20,on?SkyveilTheme.ACCENT:SkyveilTheme.OFF);
                g.centeredText(font,on?"ON":"OFF",right-29,controlY+6,SkyveilTheme.TEXT);
            }
            case DECIMAL_SLIDER -> {
                double value=(Double)s.getter.get(), fraction=(value-s.minimum)/(s.maximum-s.minimum);
                g.fill(controlX,controlY+8,right-10,controlY+11,SkyveilTheme.OFF);
                int knob=controlX+(int)((right-10-controlX)*fraction); g.fill(controlX,controlY+8,knob,controlY+11,SkyveilTheme.ACCENT);
                g.fill(knob-3,controlY+5,knob+3,controlY+14,SkyveilTheme.TEXT);
                g.text(font,String.format(Locale.ROOT,"%.2f",value),controlX,controlY-4,SkyveilTheme.TEXT,true);
            }
            case COLOR -> { int color=(Integer)s.getter.get(); g.fill(right-43,controlY,right-10,controlY+20,color); g.outline(right-43,controlY,33,20,SkyveilTheme.TEXT); }
            case BUTTON -> { g.fill(right-74,controlY,right-10,controlY+22,SkyveilTheme.ACCENT_DARK); g.centeredText(font,s.buttonLabel,right-42,controlY+7,SkyveilTheme.TEXT); }
            case KEYBIND -> { int key=(Integer)s.getter.get();String label=awaitingKey==s?"Press key...":InputConstants.getKey(new KeyEvent(key,0,0)).getDisplayName().getString();int w=Math.max(64,font.width(label)+12);g.fill(right-10-w,controlY,right-10,controlY+22,SkyveilTheme.ACCENT_DARK);g.centeredText(font,label,right-10-w/2,controlY+7,SkyveilTheme.TEXT); }
            case CHOICE -> {String label=String.valueOf(s.getter.get());int w=Math.max(76,font.width(label)+20);g.fill(right-10-w,controlY,right-10,controlY+22,SkyveilTheme.ACCENT_DARK);g.centeredText(font,label,right-10-w/2,controlY+7,SkyveilTheme.TEXT);}
        }
        visibleRows.add(new Row(x,y,right-x,h,s,result,null));
        if(hover) g.setTooltipForNextFrame(Component.literal(s.tooltip),mx,my);
        return y+h+7;
    }

    private int drawSearchResult(GuiGraphicsExtractor g,SearchManager.Result result,int x,int y,int right,int mx,int my){
        int h=40;boolean hover=inside(mx,my,x,y,right-x,h);
        g.fill(x,y,right,y+h,hover?SkyveilTheme.HOVER:SkyveilTheme.CARD);
        g.text(font,result.setting().name,x+10,y+8,SkyveilTheme.TEXT,true);
        g.text(font,result.path(),x+10,y+23,SkyveilTheme.ACCENT,false);
        visibleRows.add(new Row(x,y,right-x,h,result.setting(),result,null));
        return y+h+6;
    }

    private int settingHeight(SettingDefinition setting,String path,int x,int right){
        int controlX=right-112;
        int descriptionBottom=25+wrapText(setting.description,Math.max(60,controlX-x-18)).size()*11;
        int pathY=descriptionBottom+2;
        return Math.max(path==null?54:64,path==null?descriptionBottom+7:pathY+11);
    }

    private double scrollToSetting(SettingDefinition target,int x,int right,int contentTop){
        int rowY=contentTop+56;
        for(SettingDefinition setting:subcategory.settings){
            if(setting==target)return Math.max(0,rowY-(contentTop+8));
            rowY+=settingHeight(setting,null,x,right)+7;
        }
        return 0;
    }

    private List<String> wrapText(String text,int maxWidth) {
        List<String> lines=new ArrayList<>();
        StringBuilder line=new StringBuilder();
        for(String word:text.trim().split("\\s+")) {
            String candidate=line.isEmpty()?word:line+" "+word;
            if(!line.isEmpty()&&font.width(candidate)>maxWidth){lines.add(line.toString());line.setLength(0);line.append(word);}
            else {if(!line.isEmpty())line.append(' ');line.append(word);}
        }
        if(!line.isEmpty())lines.add(line.toString());
        if(lines.isEmpty())lines.add("");
        return lines;
    }

    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return super.mouseClicked(event,doubleClick);
        double mx=event.x(), my=event.y(); int y=top+51;
        for(ConfigCategory c:SettingsRegistry.categories()) {
            if(inside(mx,my,left+7,y-4,sidebarWidth-14,24)){ category=c; subcategory=c.subcategories.get(0); search.setValue(""); scroll=0; return true; } y+=27;
        }
        for(Row row:visibleRows) if(inside(mx,my,row.x,row.y,row.w,row.h)) {
            if(row.sub!=null){subcategory=row.sub;scroll=0;return true;}
            if(row.result!=null){navigateTo(row.result);return true;}
            SettingDefinition s=row.setting;
            switch(s.type) {
                case TOGGLE -> s.set(!(Boolean)s.getter.get());
                case DECIMAL_SLIDER -> { draggedSlider=s; setSlider(s,mx,sliderStart(),sliderEnd()); }
                case COLOR -> { int current=(Integer)s.getter.get(); s.set(current==0xFFFFFFFF?0xFF9B6CFF:current==0xFF9B6CFF?0xFFFFD166:0xFFFFFFFF); }
                case BUTTON -> s.set(null);
                case KEYBIND -> awaitingKey=s;
                case CHOICE -> {String current=String.valueOf(s.getter.get());int index=s.choices.indexOf(current);s.set(s.choices.get((index+1+s.choices.size())%s.choices.size()));}
            } return true;
        }
        return super.mouseClicked(event,doubleClick);
    }

    private void navigateTo(SearchManager.Result result){
        category=result.category();subcategory=result.subcategory();pendingScrollSetting=result.setting();
        highlightedSetting=result.setting();highlightUntil=System.currentTimeMillis()+1400;scroll=0;
        search.setValue("");search.setFocused(false);
    }

    @Override public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if(draggedSlider!=null) { setSlider(draggedSlider,event.x(),sliderStart(),sliderEnd()); return true; }
        return super.mouseDragged(event,dragX,dragY);
    }

    @Override public boolean mouseReleased(MouseButtonEvent event) {
        if(draggedSlider!=null) { draggedSlider=null;ConfigManager.save();return true; }
        return super.mouseReleased(event);
    }

    private int sliderStart(){return left+windowWidth-124;}
    private int sliderEnd(){return left+windowWidth-22;}

    private void setSlider(SettingDefinition s,double mouseX,int start,int end) {
        double fraction=Math.max(0,Math.min(1,(mouseX-start)/(end-start)));
        s.setter.accept(s.minimum+(s.maximum-s.minimum)*fraction);
    }
    @Override public boolean mouseScrolled(double mouseX,double mouseY,double horizontal,double vertical) {
        if(inside(mouseX,mouseY,left+sidebarWidth,top+44,windowWidth-sidebarWidth,windowHeight-54)){scroll=Math.max(0,Math.min(maxScroll,scroll-vertical*24));return true;}
        return super.mouseScrolled(mouseX,mouseY,horizontal,vertical);
    }
    @Override public void onClose(){if(draggedSlider!=null)ConfigManager.save();super.onClose();}
    @Override public boolean isPauseScreen(){return false;}
    @Override public boolean keyPressed(KeyEvent event){
        if(search!=null&&search.isFocused()){awaitingKey=null;return super.keyPressed(event);}
        if(awaitingKey!=null){awaitingKey.set(event.key());awaitingKey=null;return true;}
        return super.keyPressed(event);
    }
    private static boolean inside(double mx,double my,int x,int y,int w,int h){return mx>=x&&mx<x+w&&my>=y&&my<y+h;}
    private record Row(int x,int y,int w,int h,SettingDefinition setting,SearchManager.Result result,ConfigSubcategory sub){}
    public static void open(){Minecraft.getInstance().setScreen(new SkyveilConfigScreen());}
}
