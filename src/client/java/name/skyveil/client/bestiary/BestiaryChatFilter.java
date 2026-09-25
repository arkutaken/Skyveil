package name.skyveil.client.bestiary;

import name.skyveil.client.config.ConfigManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** Client-thread state machine that preserves styled mob/tier lines and removes only their reward block. */
public final class BestiaryChatFilter {
    private static final boolean DEBUG=false;
    private static final Logger LOGGER=LoggerFactory.getLogger("skyveil-bestiary");
    // A border alone could belong to another message. Hold it briefly until a
    // BESTIARY header confirms the block; confirmed blocks get a longer timeout.
    private static final long PREFIX_TIMEOUT_MS=700,BLOCK_TIMEOUT_MS=2500;
    private static final int MAX_BLOCK_LINES=16,MAX_PREFIX_LINES=4;
    private static final Pattern BORDER=Pattern.compile("^[▬━─═-]{20,}$");
    private static final Pattern MILESTONE=Pattern.compile(".*\\d{1,3}\\s*➡\\s*\\d{1,3}.*");
    private static final Pattern ROMAN_TIER=Pattern.compile("(?i)^(?:tier\\s+)?[ivxlcdm]+[!.]?$",Pattern.CASE_INSENSITIVE);
    // Keep original components so a false alarm can be replayed with its styling.
    private static final List<Component> PENDING_PREFIX=new ArrayList<>();
    private static State state=State.NORMAL;
    private static long deadline;
    private static int blockLines;
    private static Component pendingHeader;
    private static Component pendingMob;
    private BestiaryChatFilter() {}

    /**
     * Returns true when vanilla should skip this chat line. Some skipped lines
     * are only buffered: an unconfirmed prefix is replayed if recognition fails.
     * Overlay/action-bar messages never participate in this chat state machine.
     */
    public static boolean shouldSuppress(Component component,boolean overlay){
        Minecraft client=Minecraft.getInstance();
        // Packet handlers initially run on Netty. Never mutate filter or GUI state until vanilla's
        // packet-thread handoff has placed the handler on Minecraft's client thread.
        if(!client.isSameThread())return false;
        if(overlay)return false;
        if(!ConfigManager.get().bestiary.hideRewards){flushPending(client);finishPendingSummary(client);reset();return false;}
        long now=System.currentTimeMillis();
        if(state!=State.NORMAL&&now>deadline){flushPending(client);finishPendingSummary(client);reset();}
        String raw=component.getString(),text=normalize(raw);Classification classification=classify(text);
        // Interpret the line in context: a separator is ambiguous before a header,
        // but ends the block once rewards have started.
        boolean suppress=switch(state){
            case NORMAL -> startIfCandidate(component,classification,now);
            case POSSIBLE_START -> continueCandidate(component,classification,now);
            case AWAIT_TIER -> acceptTier(component,classification,now);
            case AFTER_TIER -> filterAfterTier(component,classification,now);
            case REWARDS -> filterRewards(classification,now);
        };
        debug(raw,text,classification,suppress);return suppress;
    }

    private static boolean startIfCandidate(Component component,Classification classification,long now){
        // Do not discard a border until the following lines identify its owner.
        if(classification==Classification.BORDER){
            PENDING_PREFIX.add(component);
            state=State.POSSIBLE_START;
            deadline=now+PREFIX_TIMEOUT_MS;
            return true;
        }
        // A header also starts a block when the server omits its opening border.
        if(classification==Classification.HEADER){
            pendingHeader=component;
            state=State.AWAIT_TIER;
            deadline=now+BLOCK_TIMEOUT_MS;
            blockLines=0;
            return true;
        }
        return false;
    }

    private static boolean continueCandidate(Component component,Classification classification,long now){
        if(classification==Classification.BLANK){
            // Bound the buffer even if unrelated blank messages keep arriving.
            if(PENDING_PREFIX.size()>=MAX_PREFIX_LINES){
                flushPending(Minecraft.getInstance());
                reset();
                return false;
            }
            PENDING_PREFIX.add(component);
            deadline=now+PREFIX_TIMEOUT_MS;
            return true;
        }
        if(classification==Classification.HEADER){
            // Confirmation lets us drop the decorative border and blank prefix.
            PENDING_PREFIX.clear();
            pendingHeader=component;
            state=State.AWAIT_TIER;
            deadline=now+BLOCK_TIMEOUT_MS;
            blockLines=0;
            return true;
        }
        // This was another chat message: replay its prefix, then allow this line.
        flushPending(Minecraft.getInstance());
        reset();
        return false;
    }

    // After a confirmed header, retain the first nondecorative line for the summary;
    // reward suppression starts only when filterAfterTier recognizes its boundary.
    private static boolean acceptTier(Component component,Classification classification,long now){
        deadline=now+BLOCK_TIMEOUT_MS;
        if(classification==Classification.BLANK||classification==Classification.HEADER)return true;
        if(classification==Classification.BORDER){reset();return true;}
        // The first content line is retained as the mob/tier summary. Its text
        // can contain any mob name, so it is not restricted to a name regex.
        pendingMob=component;
        state=State.AFTER_TIER;
        blockLines=1;
        return true;
    }

    private static boolean filterAfterTier(Component component,Classification classification,long now){
        if(++blockLines>MAX_BLOCK_LINES){reset();return false;}
        deadline=now+BLOCK_TIMEOUT_MS;
        if(classification==Classification.BORDER){
            finishPendingSummary(Minecraft.getInstance());
            reset();
            return true;
        }
        if(classification==Classification.REWARD){
            // Publish the useful summary before suppressing the reward body.
            finishPendingSummary(Minecraft.getInstance());
            state=State.REWARDS;
            return true;
        }
        if(classification==Classification.BLANK)return true;
        if(classification==Classification.TIER_CONTINUATION){
            // Some server formats put the tier on a separate line.
            emitSummary(Minecraft.getInstance(),pendingHeader,pendingMob,component);
            pendingHeader=null;
            pendingMob=null;
            return true;
        }
        // Unexpected content ends recognition and is allowed through unchanged.
        finishPendingSummary(Minecraft.getInstance());
        reset();
        return false;
    }

    private static boolean filterRewards(Classification classification,long now){
        // Once inside rewards, hide the body until its closing border. The line
        // cap and timeout limit suppression if the expected closing border is lost.
        if(++blockLines>MAX_BLOCK_LINES){reset();return false;}
        deadline=now+BLOCK_TIMEOUT_MS;
        if(classification==Classification.BORDER){reset();return true;}
        return true;
    }

    public static void tick(Minecraft client){
        // Expire buffers even when no new chat arrives; otherwise a held prefix
        // or summary could remain invisible indefinitely.
        if(!ConfigManager.get().bestiary.hideRewards){flushPending(client);finishPendingSummary(client);reset();return;}
        if(state!=State.NORMAL&&System.currentTimeMillis()>deadline){flushPending(client);finishPendingSummary(client);reset();}
    }

    private static Classification classify(String text){
        // Classification only describes the line. The current state decides
        // whether that description is enough evidence to suppress it.
        if(text.isBlank())return Classification.BLANK;
        if(BORDER.matcher(text).matches())return Classification.BORDER;
        if(text.equalsIgnoreCase("BESTIARY"))return Classification.HEADER;
        String upper=text.toUpperCase(Locale.ROOT);
        if(upper.contains("REWARD")||MILESTONE.matcher(text).matches()||looksLikeReward(upper))return Classification.REWARD;
        if(upper.contains("BESTIARY TIER")||ROMAN_TIER.matcher(text).matches())return Classification.TIER_CONTINUATION;
        return Classification.UNRELATED;
    }

    private static boolean looksLikeReward(String upper){
        String trimmed=upper.trim();
        if(!(trimmed.startsWith("+")||trimmed.startsWith("-")||trimmed.startsWith("•")))return false;
        return upper.contains("SKYBLOCK XP")||upper.contains("COMBAT XP")||upper.contains("MAGIC FIND")||upper.contains("STRENGTH")||upper.contains("HEALTH")||upper.contains("COIN")||upper.contains("EXPERIENCE")||upper.contains("ORB");
    }

    private static String normalize(String input){
        // Normalize only for matching. Display uses the untouched Component,
        // preserving colors, click actions, hover text, and nested siblings.
        if(input==null)return "";String stripped=input.replaceAll("(?i)§[0-9A-FK-OR]","");StringBuilder result=new StringBuilder();
        stripped.codePoints().filter(code->Character.getType(code)!=Character.FORMAT&&!Character.isISOControl(code)).forEach(result::appendCodePoint);
        return result.toString().trim().replaceAll("\\s+"," ");
    }
    /** Replays an unconfirmed prefix in its original order, then releases it. */
    private static void flushPending(Minecraft client){
        if(PENDING_PREFIX.isEmpty())return;
        if(client.gui!=null){
            for(Component component:PENDING_PREFIX){
                client.gui.getChat().addServerSystemMessage(component);
            }
        }
        PENDING_PREFIX.clear();
    }

    /** Emits a buffered mob line when no separate tier line arrived. */
    private static void finishPendingSummary(Minecraft client){
        if(pendingMob!=null){
            emitSummary(client,pendingHeader,pendingMob,null);
            pendingHeader=null;
            pendingMob=null;
        }
    }
    private static void emitSummary(Minecraft client,Component header,Component mob,Component tier){
        if(client.gui==null||mob==null)return;
        // Append the original server components instead of reconstructing plain strings. This
        // retains ClickEvent, HoverEvent, formatting, and any nested interactive siblings.
        Component summary=header==null
            ?Component.literal("[Bestiary Level Up] ").withStyle(ChatFormatting.GOLD,ChatFormatting.BOLD).append(mob.copy())
            :Component.empty().append(header.copy()).append(Component.literal(" ")).append(mob.copy());
        if(tier!=null&&!normalize(tier.getString()).isBlank())summary=summary.copy().append(Component.literal(" ")).append(tier.copy());
        client.gui.getChat().addServerSystemMessage(summary);
    }
    // Reset discards buffers; callers must replay/emit anything they want to keep first.
    private static void reset(){state=State.NORMAL;deadline=0;blockLines=0;pendingHeader=null;pendingMob=null;PENDING_PREFIX.clear();}
    private static void debug(String raw,String stripped,Classification classification,boolean suppressed){if(DEBUG)LOGGER.info("raw={} stripped={} state={} class={} suppressed={}",raw,stripped,state,classification,suppressed);}
    private enum State{
        NORMAL,         // No block is being examined.
        POSSIBLE_START, // A border is buffered, but no header has confirmed it.
        AWAIT_TIER,     // A header was seen; wait for the first content line.
        AFTER_TIER,     // A mob line is buffered; accept an optional tier or rewards.
        REWARDS        // The summary is emitted; suppress the remaining reward body.
    }
    private enum Classification{BORDER,HEADER,BLANK,REWARD,TIER_CONTINUATION,UNRELATED}
}
