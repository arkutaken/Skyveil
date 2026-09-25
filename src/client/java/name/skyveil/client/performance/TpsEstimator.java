package name.skyveil.client.performance;

/** Five packet intervals smooth network jitter without assuming a fixed server packet cadence. */
public final class TpsEstimator {
    private final long[] ticks=new long[5], nanos=new long[5];
    private long lastTick,lastNanos;
    private int count,cursor;
    private boolean initialized;

    public void reset(){initialized=false;count=0;cursor=0;}

    public void update(long gameTime,long now){
        if(!initialized){initialized=true;lastTick=gameTime;lastNanos=now;return;}
        long elapsed=now-lastNanos,delta=gameTime-lastTick;
        if(delta==0)return;
        // World-time resets and long gaps are discontinuities, not evidence of
        // extremely low TPS. Start a new baseline instead of averaging them in.
        if(delta<0||elapsed<=0||elapsed>10_000_000_000L){
            reset();update(gameTime,now);return;
        }
        ticks[cursor]=delta;nanos[cursor]=elapsed;
        cursor=(cursor+1)%ticks.length;count=Math.min(count+1,ticks.length);
        lastTick=gameTime;lastNanos=now;
    }

    public double value(long now){
        if(count==0||now-lastNanos>10_000_000_000L)return Double.NaN;
        long totalTicks=0,totalNanos=0;
        for(int i=0;i<count;i++){totalTicks+=ticks[i];totalNanos+=nanos[i];}
        return Math.min(20,totalTicks*1_000_000_000d/totalNanos);
    }
}