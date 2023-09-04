import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class RegionFileCache {
   private static final Map<File, Reference<RegionFile>> cache = new HashMap();

   private RegionFileCache() {
   }

   public static synchronized RegionFile getRegionFile(File var0, int var1, int var2) {
      File var3 = new File(var0, "region");
      File var4 = new File(var3, "r." + (var1 >> 5) + "." + (var2 >> 5) + ".mcr");
      Reference var5 = (Reference)cache.get(var4);
      if (var5 != null && var5.get() != null) {
         return (RegionFile)var5.get();
      } else {
         if (!var3.exists()) {
            var3.mkdirs();
         }

         RegionFile var6 = new RegionFile(var4);
         cache.put(var4, new SoftReference(var6));
         return var6;
      }
   }

   public static synchronized void clear() {
      Iterator var0 = cache.values().iterator();

      while(var0.hasNext()) {
         Reference var1 = (Reference)var0.next();

         try {
            if (var1.get() != null) {
               ((RegionFile)var1.get()).close();
            }
         } catch (IOException var3) {
            var3.printStackTrace();
         }
      }

      cache.clear();
   }

   public static int getSizeDelta(File var0, int var1, int var2) {
      RegionFile var3 = getRegionFile(var0, var1, var2);
      return var3.getSizeDelta();
   }

   public static DataInputStream getChunkDataInputStream(File var0, int var1, int var2) {
      RegionFile var3 = getRegionFile(var0, var1, var2);
      return var3.getChunkDataInputStream(var1 & 31, var2 & 31);
   }

   public static DataOutputStream getChunkDataOutputStream(File var0, int var1, int var2) {
      RegionFile var3 = getRegionFile(var0, var1, var2);
      return var3.getChunkDataOutputStream(var1 & 31, var2 & 31);
   }
}
