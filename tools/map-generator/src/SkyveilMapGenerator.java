import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

/** Development-only Java Anvil world to calibrated top-down PNG converter. */
public final class SkyveilMapGenerator {
    private static final int MAX_COLLECTION=64*1024*1024;
    private static final Set<String> INVISIBLE=Set.of("minecraft:air","minecraft:cave_air","minecraft:void_air","minecraft:barrier","minecraft:light","minecraft:structure_void","minecraft:moving_piston","minecraft:jigsaw");
    private static final Pattern REGION=Pattern.compile("r\\.(-?\\d+)\\.(-?\\d+)\\.mca");
    private SkyveilMapGenerator() {}

    public static void main(String[] raw) throws Exception {
        Map<String,String> args=arguments(raw);
        Path world=findWorld(requiredPath(args,"world"));
        String id=required(args,"id"),name=required(args,"name");
        Path dimension=selectDimension(world,args.getOrDefault("dimension","auto"));
        Path output=requiredPath(args,"output");Files.createDirectories(output);
        int margin=Integer.parseInt(args.getOrDefault("margin","4"));
        int maxY=Integer.parseInt(args.getOrDefault("max-y",Integer.toString(Integer.MAX_VALUE)));
        Set<String> ignoredBlocks=new LinkedHashSet<>();for(String block:args.getOrDefault("ignore-blocks","").split(","))if(!block.isBlank())ignoredBlocks.add(block.contains(":")?block.trim():"minecraft:"+block.trim());
        Generation generation=generate(dimension,maxY,ignoredBlocks);
        Map<Long,Surface> surfaces=generation.surfaces;String componentSeed=args.getOrDefault("component-seed","");
        if(!componentSeed.isBlank()){String[] parts=componentSeed.split(",");if(parts.length!=2)throw new IllegalArgumentException("--component-seed must be X,Z");surfaces=retainComponent(surfaces,Integer.parseInt(parts[0].trim()),Integer.parseInt(parts[1].trim()));}
        String crop=args.getOrDefault("crop","");if(!crop.isBlank()){String[] parts=crop.split(",");if(parts.length!=4)throw new IllegalArgumentException("--crop must be minX,maxX,minZ,maxZ with exclusive maxima");int cropMinX=Integer.parseInt(parts[0].trim()),cropMaxX=Integer.parseInt(parts[1].trim()),cropMinZ=Integer.parseInt(parts[2].trim()),cropMaxZ=Integer.parseInt(parts[3].trim());surfaces.entrySet().removeIf(entry->keyX(entry.getKey())<cropMinX||keyX(entry.getKey())>=cropMaxX||keyZ(entry.getKey())<cropMinZ||keyZ(entry.getKey())>=cropMaxZ);if(surfaces.isEmpty())throw new IOException("Coordinate crop removed every visible block");}
        String maskPolygon=args.getOrDefault("mask-polygon","");if(!maskPolygon.isBlank()){List<int[]> polygon=new ArrayList<>();for(String point:maskPolygon.split(";")){String[] coordinates=point.split(":");if(coordinates.length!=2)throw new IllegalArgumentException("--mask-polygon points must use X:Z;X:Z");polygon.add(new int[]{Integer.parseInt(coordinates[0].trim()),Integer.parseInt(coordinates[1].trim())});}if(polygon.size()<3)throw new IllegalArgumentException("--mask-polygon requires at least three points");surfaces.entrySet().removeIf(entry->!insidePolygon(keyX(entry.getKey()),keyZ(entry.getKey()),polygon));if(surfaces.isEmpty())throw new IOException("Polygon mask removed every visible block");}
        Map<String,Long> topBlocks=new HashMap<>();for(Surface surface:surfaces.values())topBlocks.merge(surface.block,1L,Long::sum);
        System.out.println("Dominant surfaces: "+topBlocks.entrySet().stream().sorted(Map.Entry.<String,Long>comparingByValue().reversed()).limit(12).toList());
        int minX=surfaces.keySet().stream().mapToInt(SkyveilMapGenerator::keyX).min().orElseThrow()-margin;
        int maxX=surfaces.keySet().stream().mapToInt(SkyveilMapGenerator::keyX).max().orElseThrow()+margin+1;
        int minZ=surfaces.keySet().stream().mapToInt(SkyveilMapGenerator::keyZ).min().orElseThrow()-margin;
        int maxZ=surfaces.keySet().stream().mapToInt(SkyveilMapGenerator::keyZ).max().orElseThrow()+margin+1;
        int width=maxX-minX,height=maxZ-minZ;
        if(width<=0||height<=0||width>16384||height>16384)throw new IOException("Unsafe generated dimensions: "+width+"x"+height);
        BufferedImage image=new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);
        for(var entry:surfaces.entrySet()){
            int x=keyX(entry.getKey()),z=keyZ(entry.getKey());
            image.setRGB(x-minX,z-minZ,shaded(entry.getValue(),x,z,surfaces));
        }
        Path png=output.resolve(id+".png");ImageIO.write(image,"PNG",png.toFile());
        Path metadata=output.resolve(id+".generated.json");
        String json="""
            {
              "id": "%s",
              "displayName": "%s",
              "texture": "skyveil:textures/maps/%s.png",
              "imageWidth": %d,
              "imageHeight": %d,
              "minX": %d,
              "maxX": %d,
              "minZ": %d,
              "maxZ": %d,
              "orientation": "north=-Z, image top=minZ; east=+X, image right=maxX",
              "pixelsPerBlock": 1,
              "boundsConvention": "min inclusive, max exclusive",
              "sourceWorld": "%s",
              "sourceDimension": "%s",
              "maximumRenderedY": %s,
              "ignoredSurfaceBlocks": "%s",
              "retainedComponentSeed": "%s",
              "coordinateCrop": "%s",
              "worldCoordinateMask": "%s",
              "dataVersion": %d,
              "regionFiles": %d,
              "chunksRead": %d,
              "visibleColumns": %d,
              "generator": "tools/map-generator/src/SkyveilMapGenerator.java"
            }
            """.formatted(json(id),json(name),json(id),width,height,minX,maxX,minZ,maxZ,json(world.getFileName().toString()),json(world.relativize(dimension).toString().isBlank()?"overworld":world.relativize(dimension).toString()),maxY==Integer.MAX_VALUE?"null":Integer.toString(maxY),json(String.join(",",ignoredBlocks)),json(componentSeed),json(crop),json(maskPolygon),dataVersion(world),generation.regions,generation.chunks,surfaces.size());
        Files.writeString(metadata,json,StandardCharsets.UTF_8);
        System.out.print(json);
    }

    private static Map<String,String> arguments(String[] raw){
        Map<String,String> result=new LinkedHashMap<>();
        for(int i=0;i<raw.length;i++){
            if(!raw[i].startsWith("--")||i+1>=raw.length)throw new IllegalArgumentException("Expected --key value arguments");
            result.put(raw[i].substring(2),raw[++i]);
        }
        return result;
    }
    private static String required(Map<String,String> args,String key){String value=args.get(key);if(value==null||value.isBlank())throw new IllegalArgumentException("Missing --"+key);return value;}
    private static Path requiredPath(Map<String,String> args,String key){return Path.of(required(args,key)).toAbsolutePath().normalize();}
    private static String json(String value){return value.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n");}

    private static Path findWorld(Path root) throws IOException {
        if(Files.isDirectory(root.resolve("region"))&&Files.isRegularFile(root.resolve("level.dat")))return root;
        try(Stream<Path> files=Files.walk(root)){
            List<Path> matches=files.filter(path->path.getFileName().toString().equals("level.dat")).map(Path::getParent).filter(path->Files.isDirectory(path.resolve("region"))).toList();
            if(matches.size()!=1)throw new IOException("Expected exactly one overworld below "+root+", found "+matches.size());
            return matches.getFirst();
        }
    }

    private static Path selectDimension(Path world,String requested) throws IOException {
        if(!requested.equalsIgnoreCase("auto")){
            Path selected=switch(requested.toLowerCase(Locale.ROOT)){case "overworld","."->world;case "nether","dim-1"->world.resolve("DIM-1");case "end","dim1"->world.resolve("DIM1");default->world.resolve(requested);};
            if(!Files.isDirectory(selected.resolve("region")))throw new IOException("Missing dimension region directory: "+selected);
            return selected;
        }
        List<Path> candidates=new ArrayList<>();for(Path candidate:List.of(world,world.resolve("DIM-1"),world.resolve("DIM1")))if(Files.isDirectory(candidate.resolve("region")))candidates.add(candidate);
        if(candidates.isEmpty())throw new IOException("No Anvil dimension found below "+world);
        Path best=null;long bestBytes=-1;for(Path candidate:candidates){long bytes;try(Stream<Path> files=Files.list(candidate.resolve("region"))){bytes=files.filter(path->REGION.matcher(path.getFileName().toString()).matches()).mapToLong(path->{try{return Files.size(path);}catch(IOException ignored){return 0;}}).sum();}if(bytes>bestBytes){best=candidate;bestBytes=bytes;}}
        return best;
    }

    private static Generation generate(Path world,int maxY,Set<String> ignoredBlocks) throws Exception {
        Map<Long,Surface> surfaces=new HashMap<>();int chunks=0;
        List<Path> regions;
        try(Stream<Path> files=Files.list(world.resolve("region"))){regions=files.filter(path->REGION.matcher(path.getFileName().toString()).matches()).sorted().toList();}
        for(int i=0;i<regions.size();i++){
            Path region=regions.get(i);System.out.println("["+(i+1)+"/"+regions.size()+"] "+region.getFileName());
            final int[] count={0};readRegion(region,chunk->{
                count[0]++;ChunkSurface surface=chunkSurface(chunk.fallbackX,chunk.fallbackZ,chunk.root,maxY,ignoredBlocks);
                for(int index=0;index<surface.columns.length;index++){
                    Surface value=surface.columns[index];if(value==null)continue;
                    int x=surface.chunkX*16+index%16,z=surface.chunkZ*16+index/16;
                    surfaces.put(key(x,z),value);
                }
            });chunks+=count[0];
        }
        if(surfaces.isEmpty())throw new IOException("No visible surface blocks were found");
        return new Generation(surfaces,regions.size(),chunks);
    }

    private static Map<Long,Surface> retainComponent(Map<Long,Surface> all,int seedX,int seedZ) throws IOException {
        long start=0;long distance=Long.MAX_VALUE;for(long candidate:all.keySet()){long dx=(long)keyX(candidate)-seedX,dz=(long)keyZ(candidate)-seedZ,d=dx*dx+dz*dz;if(d<distance){distance=d;start=candidate;}}
        if(distance>64L*64L)throw new IOException("No rendered block within 64 blocks of component seed "+seedX+","+seedZ);
        Set<Long> visited=new HashSet<>();ArrayDeque<Long> queue=new ArrayDeque<>();visited.add(start);queue.add(start);
        while(!queue.isEmpty()){long current=queue.removeFirst();int x=keyX(current),z=keyZ(current);for(int dz=-1;dz<=1;dz++)for(int dx=-1;dx<=1;dx++){if(dx==0&&dz==0)continue;long neighbor=key(x+dx,z+dz);if(all.containsKey(neighbor)&&visited.add(neighbor))queue.addLast(neighbor);}}
        Map<Long,Surface> selected=new HashMap<>(visited.size()*4/3+1);for(long key:visited)selected.put(key,all.get(key));return selected;
    }

    private static boolean insidePolygon(int x,int z,List<int[]> polygon){boolean inside=false;for(int i=0,j=polygon.size()-1;i<polygon.size();j=i++){int[] a=polygon.get(i),b=polygon.get(j);if((a[1]>z)!=(b[1]>z)&&x<(long)(b[0]-a[0])*(z-a[1])/(double)(b[1]-a[1])+a[0])inside=!inside;}return inside;}

    private static void readRegion(Path path,Consumer<Chunk> consumer) throws Exception {
        Matcher matcher=REGION.matcher(path.getFileName().toString());if(!matcher.matches())return;
        int regionX=Integer.parseInt(matcher.group(1)),regionZ=Integer.parseInt(matcher.group(2));
        try(RandomAccessFile file=new RandomAccessFile(path.toFile(),"r")){
            byte[] locations=new byte[4096];if(file.read(locations)!=locations.length)return;
            for(int index=0;index<1024;index++){
                int location=((locations[index*4]&255)<<24)|((locations[index*4+1]&255)<<16)|((locations[index*4+2]&255)<<8)|(locations[index*4+3]&255);
                int sector=location>>>8,count=location&255;if(sector<2||count==0)continue;
                try{
                    file.seek((long)sector*4096);int length=file.readInt();if(length<2||length>count*4096-4)continue;
                    int compression=file.readUnsignedByte();byte[] compressed=new byte[length-1];file.readFully(compressed);
                    InputStream input=switch(compression){case 1->new GZIPInputStream(new ByteArrayInputStream(compressed));case 2->new InflaterInputStream(new ByteArrayInputStream(compressed));case 3->new ByteArrayInputStream(compressed);default->throw new IOException("Unsupported compression "+compression);};
                    byte[] decoded=input.readAllBytes();Map<String,Object> root=NbtReader.readRoot(decoded);
                    consumer.accept(new Chunk(regionX*32+index%32,regionZ*32+index/32,root));
                }catch(Exception error){System.err.println("warning: skipped "+path.getFileName()+" chunk "+index+": "+error.getMessage());}
            }
        }
    }

    @SuppressWarnings("unchecked") private static ChunkSurface chunkSurface(int fallbackX,int fallbackZ,Map<String,Object> root,int maxY,Set<String> ignoredBlocks){
        Map<String,Object> level=root.get("Level") instanceof Map<?,?> map?(Map<String,Object>)map:root;
        int chunkX=number(level.get("xPos"),fallbackX),chunkZ=number(level.get("zPos"),fallbackZ);
        Object rawSections=level.containsKey("sections")?level.get("sections"):level.get("Sections");
        List<Section> sections=new ArrayList<>();
        if(rawSections instanceof List<?> list)for(Object item:list)if(item instanceof Map<?,?> raw&&raw.get("Y") instanceof Number y){
            SectionPalette palette=new SectionPalette((Map<String,Object>)raw);if(!palette.empty())sections.add(new Section(y.intValue(),palette));
        }
        sections.sort(Comparator.comparingInt(Section::y).reversed());
        Surface[] result=new Surface[256];String[] overlays=new String[256];BitSet remaining=new BitSet(256);remaining.set(0,256);
        for(Section section:sections){
            if(remaining.isEmpty())break;
            for(int localY=15;localY>=0;localY--)for(int column=remaining.nextSetBit(0);column>=0;column=remaining.nextSetBit(column+1)){
                int worldY=section.y*16+localY;if(worldY>maxY)continue;
                int localX=column%16,localZ=column/16;String block=section.palette.block(localX,localY,localZ);if(INVISIBLE.contains(block)||ignoredBlocks.contains(block))continue;
                if(isGlass(block)){overlays[column]=block;continue;}
                int color=blockColor(block);if(overlays[column]!=null)color=blend(blockColor(overlays[column]),color);
                result[column]=new Surface(worldY,color,block);remaining.clear(column);
            }
        }
        for(int column=remaining.nextSetBit(0);column>=0;column=remaining.nextSetBit(column+1))if(overlays[column]!=null)result[column]=new Surface(0,blockColor(overlays[column]),overlays[column]);
        return new ChunkSurface(chunkX,chunkZ,result);
    }

    @SuppressWarnings("unchecked") private static int dataVersion(Path world){
        try(InputStream input=new GZIPInputStream(Files.newInputStream(world.resolve("level.dat")))){
            Map<String,Object> root=NbtReader.readRoot(input.readAllBytes());Map<String,Object> data=root.get("Data") instanceof Map<?,?> map?(Map<String,Object>)map:root;return number(data.get("DataVersion"),-1);
        }catch(Exception ignored){return -1;}
    }

    private static int number(Object value,int fallback){return value instanceof Number number?number.intValue():fallback;}
    private static long key(int x,int z){return ((long)x<<32)|(z&0xffffffffL);}
    private static int keyX(long key){return (int)(key>>32);}
    private static int keyZ(long key){return (int)key;}
    private static boolean isGlass(String block){return block.contains("glass")&&!block.contains("pane");}
    private static int rgba(int r,int g,int b,int a){return (a&255)<<24|(r&255)<<16|(g&255)<<8|(b&255);}

    private static int blend(int top,int bottom){
        double alpha=((top>>>24)&255)/255.0;int r=(int)Math.round(((top>>16)&255)*alpha+((bottom>>16)&255)*(1-alpha));int g=(int)Math.round(((top>>8)&255)*alpha+((bottom>>8)&255)*(1-alpha));int b=(int)Math.round((top&255)*alpha+(bottom&255)*(1-alpha));return rgba(r,g,b,255);
    }
    private static int shaded(Surface surface,int x,int z,Map<Long,Surface> all){
        int total=0,count=0;for(long key:new long[]{key(x-1,z),key(x,z-1),key(x+1,z),key(x,z+1)}){Surface neighbor=all.get(key);if(neighbor!=null){total+=neighbor.y;count++;}}
        int light=count==0?0:(int)Math.round(Math.max(-30,Math.min(30,(surface.y-total/(double)count)*3.5)));int color=surface.argb;
        return rgba(clamp(((color>>16)&255)+light),clamp(((color>>8)&255)+light),clamp((color&255)+light),(color>>>24)&255);
    }
    private static int clamp(int value){return Math.max(0,Math.min(255,value));}

    private static int blockColor(String block){
        String name=block.replace("minecraft:","");
        if(name.equals("water")||name.equals("bubble_column"))return rgba(45,92,181,230);
        if(name.contains("lava"))return rgba(240,88,18,255);
        if(name.contains("grass_block")||Set.of("grass","short_grass","tall_grass","moss_block").contains(name))return rgba(92,142,55,255);
        if(contains(name,"leaves","vine","azalea"))return rgba(67,116,48,245);
        if(contains(name,"sand","sandstone"))return name.contains("red_")?rgba(171,87,49,255):rgba(216,203,144,255);
        if(contains(name,"dirt","podzol","mud","farmland","rooted_dirt"))return rgba(126,87,55,255);
        if(contains(name,"stone","cobble","andesite","gravel","tuff"))return rgba(119,119,116,255);
        if(contains(name,"deepslate","blackstone","basalt"))return rgba(61,61,66,255);
        if(contains(name,"netherrack","nether_brick","crimson_nylium"))return rgba(113,47,49,255);
        if(contains(name,"end_stone","purpur"))return name.contains("end_stone")?rgba(218,224,158,255):rgba(167,122,167,255);
        if(contains(name,"prismarine","warped"))return rgba(71,139,132,255);
        if(name.contains("quartz"))return rgba(230,223,207,255);if(contains(name,"brick","granite"))return rgba(151,83,67,255);
        if(contains(name,"snow","powder_snow"))return rgba(239,247,248,255);if(name.contains("ice"))return rgba(115,170,238,235);
        if(name.contains("coal"))return rgba(55,55,53,255);if(name.contains("iron"))return rgba(183,177,163,255);if(name.contains("gold"))return rgba(235,190,44,255);
        if(name.contains("diamond"))return rgba(62,210,195,255);if(name.contains("emerald"))return rgba(50,181,83,255);if(name.contains("lapis"))return rgba(45,80,154,255);if(name.contains("redstone"))return rgba(176,38,31,255);
        Map<String,int[]> dyes=dyes();for(var entry:dyes.entrySet())if(name.startsWith(entry.getKey()+"_")&&contains(name,"wool","concrete","terracotta","carpet","glazed","stained_glass")){int[] c=entry.getValue();return rgba(c[0],c[1],c[2],name.contains("glass")?225:255);}
        Map<String,int[]> woods=woods();for(var entry:woods.entrySet())if(name.contains(entry.getKey())&&contains(name,"planks","log","wood","stem","hyphae","slab","stairs")){int[] c=entry.getValue();return rgba(c[0],c[1],c[2],255);}
        if(name.contains("glass"))return rgba(190,220,225,150);if(contains(name,"planks","log","wood","bookshelf","chest","barrel"))return rgba(142,103,57,255);
        if(name.contains("clay"))return rgba(159,166,179,255);if(name.contains("obsidian"))return rgba(42,30,57,255);if(name.contains("bedrock"))return rgba(72,72,72,255);
        byte[] digest;try{digest=MessageDigest.getInstance("SHA-1").digest(name.getBytes(StandardCharsets.UTF_8));}catch(Exception error){digest=name.getBytes(StandardCharsets.UTF_8);}
        int base=90+(digest[0]&255)%70;return rgba(base,Math.max(55,base-12+(digest[1]&255)%25),Math.max(50,base-20+(digest[2]&255)%30),255);
    }
    private static boolean contains(String value,String... needles){for(String needle:needles)if(value.contains(needle))return true;return false;}
    private static Map<String,int[]> dyes(){return Map.ofEntries(Map.entry("white",new int[]{224,226,226}),Map.entry("light_gray",new int[]{142,142,134}),Map.entry("gray",new int[]{62,68,71}),Map.entry("black",new int[]{25,25,25}),Map.entry("brown",new int[]{116,74,42}),Map.entry("red",new int[]{176,46,38}),Map.entry("orange",new int[]{240,118,19}),Map.entry("yellow",new int[]{249,198,39}),Map.entry("lime",new int[]{112,185,25}),Map.entry("green",new int[]{73,91,36}),Map.entry("cyan",new int[]{21,137,145}),Map.entry("light_blue",new int[]{58,175,217}),Map.entry("blue",new int[]{53,57,157}),Map.entry("purple",new int[]{121,42,172}),Map.entry("magenta",new int[]{189,68,179}),Map.entry("pink",new int[]{238,141,172}));}
    private static Map<String,int[]> woods(){return Map.of("oak",new int[]{151,116,65},"spruce",new int[]{102,76,45},"birch",new int[]{198,179,123},"jungle",new int[]{158,112,72},"acacia",new int[]{170,91,51},"dark_oak",new int[]{72,51,30},"mangrove",new int[]{117,54,48},"cherry",new int[]{209,152,157},"bamboo",new int[]{177,187,90});}

    private static final class SectionPalette {
        private final List<String> palette=new ArrayList<>();private final long[] data;private final int bits,valuesPerLong;private final long mask;
        @SuppressWarnings("unchecked") SectionPalette(Map<String,Object> section){
            Object statesObject=section.get("block_states");Map<String,Object> states=statesObject instanceof Map<?,?> map?(Map<String,Object>)map:null;
            Object paletteObject=states!=null?states.get("palette"):section.get("Palette");Object dataObject=states!=null?states.get("data"):section.get("BlockStates");
            if(paletteObject instanceof List<?> list)for(Object item:list)if(item instanceof Map<?,?> entry)palette.add(String.valueOf(entry.containsKey("Name")?entry.get("Name"):"minecraft:air"));
            data=dataObject instanceof long[] longs?longs:new long[0];bits=Math.max(4,Math.max(0,palette.size()-1)==0?0:32-Integer.numberOfLeadingZeros(palette.size()-1));valuesPerLong=64/bits;mask=(1L<<bits)-1;
        }
        boolean empty(){return palette.isEmpty()||palette.stream().allMatch(INVISIBLE::contains);}
        String block(int x,int y,int z){if(palette.isEmpty())return "minecraft:air";if(palette.size()==1||data.length==0)return palette.getFirst();int index=(y<<8)|(z<<4)|x,word=index/valuesPerLong;if(word>=data.length)return "minecraft:air";int selected=(int)((data[word]>>>(index%valuesPerLong*bits))&mask);return selected<palette.size()?palette.get(selected):"minecraft:air";}
    }

    private static final class NbtReader {
        private final DataInputStream input;private NbtReader(byte[] data){input=new DataInputStream(new ByteArrayInputStream(data));}
        static Map<String,Object> readRoot(byte[] data) throws IOException {NbtReader reader=new NbtReader(data);int type=reader.input.readUnsignedByte();if(type==0)return Map.of();reader.string();Object value=reader.payload(type);if(value instanceof Map<?,?> map){@SuppressWarnings("unchecked") Map<String,Object> result=(Map<String,Object>)map;return result;}throw new IOException("NBT root is not a compound");}
        private String string() throws IOException {return new String(input.readNBytes(input.readUnsignedShort()),StandardCharsets.UTF_8);}
        private int length() throws IOException {int length=input.readInt();if(length<0||length>MAX_COLLECTION)throw new IOException("Unsafe NBT collection length "+length);return length;}
        private Object payload(int type) throws IOException {return switch(type){case 1->input.readByte();case 2->input.readShort();case 3->input.readInt();case 4->input.readLong();case 5->input.readFloat();case 6->input.readDouble();case 7->input.readNBytes(length());case 8->string();case 9->{int child=input.readUnsignedByte(),size=length();List<Object> list=new ArrayList<>(size);for(int i=0;i<size;i++)list.add(payload(child));yield list;}case 10->{Map<String,Object> map=new LinkedHashMap<>();while(true){int child=input.readUnsignedByte();if(child==0)break;map.put(string(),payload(child));}yield map;}case 11->{int[] values=new int[length()];for(int i=0;i<values.length;i++)values[i]=input.readInt();yield values;}case 12->{long[] values=new long[length()];for(int i=0;i<values.length;i++)values[i]=input.readLong();yield values;}default->throw new IOException("Unsupported NBT tag "+type);};}
    }

    private record Surface(int y,int argb,String block){}
    private record Section(int y,SectionPalette palette){}
    private record Chunk(int fallbackX,int fallbackZ,Map<String,Object> root){}
    private record ChunkSurface(int chunkX,int chunkZ,Surface[] columns){}
    private record Generation(Map<Long,Surface> surfaces,int regions,int chunks){}
}
