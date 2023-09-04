import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

class RegionTool {
   private static boolean isConsole = false;

   public static void main(String[] var0) {
      if (var0.length != 2 && var0.length != 3) {
         exitUsage();
      }

      if (System.console() != null) {
         isConsole = true;
      }

      byte var1 = 0;
      if (var0[0].equalsIgnoreCase("unpack")) {
         var1 = 1;
      } else if (var0[0].equalsIgnoreCase("pack")) {
         var1 = 2;
      }

      if (var1 == 0) {
         exitUsage();
      }

      File var2 = new File(var0[1]);
      if (!var2.exists() || !var2.isDirectory()) {
         exit("error: " + var2.getPath() + " is not a directory");
      }

      File var3 = var2;
      if (var0.length == 3) {
         var3 = new File(var0[2]);
         if (!var3.isDirectory()) {
            var3.mkdirs();
         }
      }

      if (var1 == 1) {
         unpack(var2, var3);
      } else if (var1 == 2) {
         pack(var2, var3);
      }

   }

   private static void unpack(File var0, File var1) {
      File var2 = new File(var0, "region");
      if (!var2.exists()) {
         exit("error: region directory not found");
      }

      HashSet var3 = null;
      if (var0 != var1) {
         var3 = new HashSet();
      }

      Pattern var4 = Pattern.compile("r\\.(-?[0-9]+)\\.(-?[0-9]+).mcr");
      File[] var6 = var2.listFiles();
      int var7 = var6.length;

      for(int var8 = 0; var8 < var7; ++var8) {
         File var9 = var6[var8];
         if (var9.isFile()) {
            Matcher var5 = var4.matcher(var9.getName());
            if (var5.matches()) {
               unpackRegionFile(var1, var9, var5);
               if (var3 != null) {
                  var3.add(var9);
               }
            }
         }
      }

      if (var3 != null) {
         copyDir(var0, var1, var3);
      }

   }

   private static void pack(File var0, File var1) {
      new File(var0, "region");
      HashSet var3 = null;
      if (var0 != var1) {
         var3 = new HashSet();
      }

      Pattern var4 = Pattern.compile("c\\.(-?[0-9a-z]+)\\.(-?[0-9a-z]+).dat");
      Pattern var5 = Pattern.compile("[0-9a-z]|1[0-9a-r]");
      int var6 = 0;
      int var7 = 0;
      File[] var8 = var0.listFiles();
      int var9 = var8.length;

      for(int var10 = 0; var10 < var9; ++var10) {
         File var11 = var8[var10];
         if (var11.isDirectory() && var5.matcher(var11.getName()).matches()) {
            File[] var12 = var11.listFiles();
            int var13 = var12.length;

            for(int var14 = 0; var14 < var13; ++var14) {
               File var15 = var12[var14];
               if (var15.isDirectory() && var5.matcher(var15.getName()).matches()) {
                  File[] var16 = var15.listFiles();
                  int var17 = var16.length;

                  for(int var18 = 0; var18 < var17; ++var18) {
                     File var19 = var16[var18];
                     Matcher var20 = var4.matcher(var19.getName());
                     if (var20.matches()) {
                        if (packChunk(var1, var19, var20)) {
                           ++var6;
                        } else {
                           ++var7;
                        }

                        if (var3 != null) {
                           var3.add(var19);
                        }
                     }

                     if (isConsole) {
                        System.out.print("\rpacked " + var6 + " chunks" + (var7 > 0 ? ", skipped " + var7 + " older ones" : ""));
                     }
                  }
               }
            }
         }
      }

      if (isConsole) {
         System.out.print("\r");
      }

      System.out.println("packed " + var6 + " chunks" + (var7 > 0 ? ", skipped " + var7 + " older ones" : ""));
      if (var3 != null) {
         copyDir(var0, var1, var3);
      }

   }

   private static boolean packChunk(File var0, File var1, Matcher var2) {
      int var3 = Integer.parseInt(var2.group(1), 36);
      int var4 = Integer.parseInt(var2.group(2), 36);
      RegionFile var5 = RegionFileCache.getRegionFile(var0, var3, var4);
      if (var5.lastModified() > var1.lastModified()) {
         return false;
      } else {
         byte[] var6 = new byte[4096];
         int var7 = 0;

         try {
            DataInputStream var8 = new DataInputStream(new GZIPInputStream(new FileInputStream(var1)));

            DataOutputStream var9;
            for(var9 = var5.getChunkDataOutputStream(var3 & 31, var4 & 31); var7 != -1; var7 = var8.read(var6)) {
               var9.write(var6, 0, var7);
            }

            var9.close();
            return true;
         } catch (IOException var10) {
            var10.printStackTrace();
            return false;
         }
      }
   }

   private static void unpackRegionFile(File var0, File var1, Matcher var2) {
      long var3 = var1.lastModified();
      RegionFile var5 = new RegionFile(var1);
      String var6 = var1.getName();
      int var7 = Integer.parseInt(var2.group(1));
      int var8 = Integer.parseInt(var2.group(2));
      int var9 = 0;
      int var10 = 0;

      for(int var11 = 0; var11 < 32; ++var11) {
         for(int var12 = 0; var12 < 32; ++var12) {
            DataInputStream var13 = var5.getChunkDataInputStream(var11, var12);
            if (var13 != null) {
               int var14 = var11 + (var7 << 5);
               int var15 = var12 + (var8 << 5);
               String var16 = "c." + Integer.toString(var14, 36) + "." + Integer.toString(var15, 36) + ".dat";
               File var17 = new File(var0, Integer.toString(var14 & 63, 36));
               var17 = new File(var17, Integer.toString(var15 & 63, 36));
               if (!var17.exists()) {
                  var17.mkdirs();
               }

               var17 = new File(var17, var16);
               byte[] var18 = new byte[4096];
               int var19 = 0;
               if (var17.lastModified() > var3) {
                  ++var10;
               } else {
                  try {
                     DataOutputStream var20;
                     for(var20 = new DataOutputStream(new GZIPOutputStream(new FileOutputStream(var17))); var19 != -1; var19 = var13.read(var18)) {
                        var20.write(var18, 0, var19);
                     }

                     var20.close();
                     ++var9;
                  } catch (IOException var21) {
                     var21.printStackTrace();
                  }
               }

               if (isConsole) {
                  System.out.print("\r" + var6 + ": unpacked " + var9 + " chunks" + (var10 > 0 ? ", skipped " + var10 + " newer ones" : ""));
               }
            }
         }
      }

      if (isConsole) {
         System.out.print("\r");
      }

      System.out.println(var6 + ": unpacked " + var9 + " chunks" + (var10 > 0 ? ", skipped " + var10 + " newer ones" : ""));
   }

   private static void copyDir(File var0, File var1, Set<File> var2) {
      byte[] var3 = new byte[4096];
      File[] var4 = var0.listFiles();
      int var5 = var4.length;

      for(int var6 = 0; var6 < var5; ++var6) {
         File var7 = var4[var6];
         if (var7.isDirectory()) {
            copyDir(var7, new File(var1, var7.getName()), var2);
         } else if (!var2.contains(var7)) {
            try {
               File var8 = new File(var1, var7.getName());
               var1.mkdirs();
               FileOutputStream var9 = new FileOutputStream(var8);
               FileInputStream var10 = new FileInputStream(var7);

               for(int var11 = 0; var11 != -1; var11 = var10.read(var3)) {
                  var9.write(var3, 0, var11);
               }
            } catch (IOException var12) {
               var12.printStackTrace();
            }
         }
      }

   }

   private static void exitUsage() {
      exit("regionTool: converts between chunks and regions\nusage: java -jar RegionTool.jar [un]pack <world directory> [target directory]");
   }

   private static void exit(String var0) {
      System.err.println(var0);
      System.exit(1);
   }
}
