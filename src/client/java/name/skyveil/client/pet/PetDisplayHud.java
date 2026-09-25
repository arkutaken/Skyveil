package name.skyveil.client.pet;

import name.skyveil.client.config.ConfigManager;
import name.skyveil.client.itemrarity.SkyblockRarity;
import name.skyveil.client.gui.HudVisibility;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Locale;

/** Compact, scalable display of the equipped Hypixel SkyBlock pet. */
public final class PetDisplayHud {
    private static final int PADDING=4,ICON=16;
    private PetDisplayHud() {}

    public static void register(){HudElementRegistry.addLast(name.skyveil.Skyveil.INSTANCE.id("pet_display"),(graphics,delta)->HudVisibility.render(graphics,()->render(graphics)));}

    private static void render(GuiGraphicsExtractor graphics) {
        var config=ConfigManager.get().petDisplay;
        var client=Minecraft.getInstance();
        if(!config.enabled||!HudVisibility.shouldRender(client)||!PetTracker.inSkyblock())return;
        PetData data=PetTracker.current();
        renderContents(graphics,client,data,config.hudX,config.hudY,(float)config.scale);
    }

    /** Created only after a screen is open; ItemStacks are unsafe before registries finish binding. */
    public static PetData previewData(){
        return new PetData(new PetInstanceId("preview",PetInstanceId.Source.FALLBACK,PetInstanceId.Confidence.UNKNOWN),"GOLDEN_DRAGON","Golden Dragon",SkyblockRarity.LEGENDARY,143,true,200,
            842350,1886700,true,false,"PET_ITEM_SHELMET","Dwarf Turtle Shelmet",
            SkyblockRarity.LEGENDARY,SkyblockRarity.LEGENDARY.rgb(),PetHeadResolver.resolve(ItemStack.EMPTY,"GOLDEN_DRAGON","Golden Dragon",SkyblockRarity.LEGENDARY),new ItemStack(Items.TURTLE_HELMET));
    }
    public static void renderPreview(GuiGraphicsExtractor graphics,Minecraft client){var c=ConfigManager.get().petDisplay;renderContents(graphics,client,previewData(),c.hudX,c.hudY,(float)c.scale);}
    public static void renderConfigPreview(GuiGraphicsExtractor graphics,Minecraft client,int x,int y,float scale){renderContents(graphics,client,previewData(),x,y,scale);}

    // Measure every visible text row, including the held item, so drawing and
    // layout-editor hitboxes use the same unscaled content dimensions.
    public static int contentWidth(Minecraft client,PetData data) {
        if(data==null)return Math.max(116,client.font.width("No Pet Equipped")+PADDING*2);
        int width=PADDING+ICON+4+Math.max(client.font.width(data.name()),client.font.width(levelText(data)))+PADDING;
        width=Math.max(width,client.font.width(xpText(data))+PADDING*2);
        if(showItemRow(data))width=Math.max(width,PADDING+ICON+4+client.font.width(itemText(data))+PADDING);
        return Math.max(126,width);
    }

    public static int contentHeight(PetData data) {
        if(data==null)return 17;
        int height=39;
        if(ConfigManager.get().petDisplay.showProgressBar)height+=6;
        if(showItemRow(data))height+=18;
        return height;
    }

    private static boolean showItemRow(PetData data) {
        return ConfigManager.get().petDisplay.showPetItem;
    }

    public static void renderContents(GuiGraphicsExtractor graphics,Minecraft client,PetData data,int x,int y,float scale) {
        int width=contentWidth(client,data),height=contentHeight(data);
        int px=0,py=0;
        graphics.pose().pushMatrix();try{graphics.pose().translate(x,y);graphics.pose().scale(scale,scale);
        var config=ConfigManager.get().petDisplay;
        int alpha=(int)Math.round(config.backgroundOpacity*255)&255;
        int rarityRgb=data==null||data.rarity()==null?0xFFAA00:data.rarity().rgb();
        var style=PetDisplayStyle.from(config.style);var palette=style.palette(rarityRgb,alpha);
        if(alpha>0&&palette.background()) {
            graphics.fill(px,py,px+width,py+height,HudVisibility.color(palette.backgroundColor()));

            if(palette.accent())graphics.fill(px,py,px+2,py+height,HudVisibility.color(palette.progressColor()));
        }
        if(data==null) {
            graphics.text(client.font,"No Pet Equipped",px+PADDING,py+4,HudVisibility.color(0xFFAAAAAA),true);
            return;
        }
        ItemStack icon=data.petIcon()==null?ItemStack.EMPTY:data.petIcon();
        int inset=PADDING;
        int top=4;
        int iconX=px+inset,iconY=py+top;
        if(!icon.isEmpty())graphics.item(icon,iconX,iconY);
        int textX=iconX+ICON+4;
        graphics.text(client.font,data.name(),textX,py+top,HudVisibility.color(name.skyveil.client.gui.SkyveilTheme.ACCENT),true);
        graphics.text(client.font,levelText(data),textX,py+top+11,HudVisibility.color(data.levelKnown()?0xFFFFFFFF:0xFFAAAAAA),true);
        graphics.text(client.font,xpText(data),px+PADDING,py+28,HudVisibility.color(data.maxed()?0xFFFFAA00:0xFFAAAAAA),true);
        int cursorY=39;
        if(config.showProgressBar) {
            int barX=px+PADDING,barRight=px+width-PADDING,barWidth=barRight-barX;
            graphics.fill(barX,py+cursorY,barRight,py+cursorY+3,HudVisibility.color(name.skyveil.client.gui.SkyveilTheme.BAR_TRACK));
            graphics.fill(barX,py+cursorY,barX+(int)Math.round(barWidth*data.progress()),py+cursorY+3,HudVisibility.color(palette.progressColor()));
            cursorY+=6;
        }
        if(showItemRow(data)) {
            ItemStack item=data.petItemIcon()==null?ItemStack.EMPTY:data.petItemIcon();
            if(!item.isEmpty()){graphics.item(item,px+inset,py+cursorY+1);}
            int itemColor=data.hasPetItem()?(data.petItemRgb()!=0?0xFF000000|data.petItemRgb():data.petItemRarity()==null?0xFFFFFFFF:0xFF000000|data.petItemRarity().rgb()):0xFF777777;
            graphics.text(client.font,itemText(data),px+inset+ICON+4,py+cursorY+5,HudVisibility.color(itemColor),true);
        }
        if(HudVisibility.dimmed())graphics.fill(0,0,width,height,0x44000000);
        PetTracker.debugHudRender(data,data.rarity()==null?0xFFFFFF:data.rarity().rgb());
        }finally{graphics.pose().popMatrix();}
    }

    private static String xpText(PetData data) {
        if(!data.levelKnown())return switch(PetTracker.syncState()) {
            case SYNCING -> "Syncing /pets data...";
            case STALE -> "Open /pets to refresh pet data";
            default -> "Open /pets to sync pet data";
        };
        if(data.maxed())return "MAX LEVEL";
        if(!data.xpKnown())return "Open /pets to sync XP";
        return format(data.xpRemaining())+" XP until Level "+(data.level()+1);
    }
    private static String levelText(PetData data){return data.levelKnown()?"Level "+data.level()+" / "+data.maxLevel():"Level unavailable";}
    private static String itemText(PetData data){return data.instanceId().source()==PetInstanceId.Source.LIVE_UNRESOLVED?"Open Pets menu to sync":data.hasPetItem()?data.petItemName():"No Pet Item";}
    private static String format(double value){return String.format(Locale.ROOT,"%,.0f",value);}
}
