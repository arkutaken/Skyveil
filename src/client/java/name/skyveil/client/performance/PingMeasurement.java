package name.skyveil.client.performance;

/** Times our own ping/pong exchanges; never trusts the server's TAB latency field. */
public final class PingMeasurement {
    // Reserve a recognizable high-bit prefix for our probes. Sequence numbers
    // distinguish replies, while nanoTime values measure elapsed round-trip time.
    private static final long PREFIX=0x53564C5000000000L;
    private long sequence,pending,sentAt,lastSent,lastReply;
    private int milliseconds=-1;
    private boolean started;

    public synchronized void reset(){
        pending=0;milliseconds=-1;started=false;
    }

    /** Zero means no probe is due. Sequence numbers survive resets to reject old replies. */
    public synchronized long request(long now){
        if(started&&now-lastSent<5_000_000_000L)return 0;
        if(pending!=0&&now-sentAt<10_000_000_000L)return 0;
        pending=PREFIX|(++sequence&0xFFFFFFFFL);
        sentAt=now;lastSent=now;started=true;
        return pending;
    }

    /** Called on the network thread, so measurements exclude client frame scheduling delay. */
    public synchronized boolean receive(long token,long now){
        if((token&0xFFFFFFFF00000000L)!=PREFIX)return false;
        if(token==pending){
            long elapsed=now-sentAt;
            if(elapsed>=0&&elapsed<10_000_000_000L){
                milliseconds=(int)Math.max(1,Math.round(elapsed/1_000_000d));
                lastReply=now;
            }
            pending=0;
        }
        // Consume late replies too, keeping our tokens out of vanilla's debug ping chart.
        return true;
    }

    public synchronized int value(long now){
        return milliseconds<0||now-lastReply>15_000_000_000L?-1:milliseconds;
    }
}