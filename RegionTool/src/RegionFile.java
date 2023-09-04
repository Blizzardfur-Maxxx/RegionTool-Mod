import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

public class RegionFile {
   static final int CHUNK_HEADER_SIZE = 5;
   private static final byte[] emptySector = new byte[4096];
   private final File fileName;
   private RandomAccessFile file;
   private final int[] offsets = new int[1024];
   private ArrayList<Boolean> sectorFree;
   private int sizeDelta;
   private long lastModified = 0L;

   public RegionFile(File var1) {
      this.fileName = var1;
      this.debugln("REGION LOAD " + this.fileName);
      this.sizeDelta = 0;

      try {
         if (var1.exists()) {
            this.lastModified = var1.lastModified();
         }

         this.file = new RandomAccessFile(var1, "rw");
         int var2;
         if (this.file.length() < 4096L) {
            for(var2 = 0; var2 < 1024; ++var2) {
               this.file.writeInt(0);
            }

            this.sizeDelta += 4096;
         }

         if ((this.file.length() & 4095L) != 0L) {
            for(var2 = 0; (long)var2 < (this.file.length() & 4095L); ++var2) {
               this.file.write(0);
            }
         }

         var2 = (int)this.file.length() / 4096;
         this.sectorFree = new ArrayList(var2);

         int var3;
         for(var3 = 0; var3 < var2; ++var3) {
            this.sectorFree.add(true);
         }

         this.sectorFree.set(0, false);
         this.file.seek(0L);

         for(var3 = 0; var3 < 1024; ++var3) {
            int var4 = this.file.readInt();
            this.offsets[var3] = var4;
            if (var4 != 0 && (var4 >> 8) + (var4 & 255) <= this.sectorFree.size()) {
               for(int var5 = 0; var5 < (var4 & 255); ++var5) {
                  this.sectorFree.set((var4 >> 8) + var5, false);
               }
            }
         }
      } catch (IOException var6) {
         var6.printStackTrace();
      }

   }

   public long lastModified() {
      return this.lastModified;
   }

   public synchronized int getSizeDelta() {
      int var1 = this.sizeDelta;
      this.sizeDelta = 0;
      return var1;
   }

   private void debug(String var1) {
   }

   private void debugln(String var1) {
      this.debug(var1 + "\n");
   }

   private void debug(String var1, int var2, int var3, String var4) {
      this.debug("REGION " + var1 + " " + this.fileName.getName() + "[" + var2 + "," + var3 + "] = " + var4);
   }

   private void debug(String var1, int var2, int var3, int var4, String var5) {
      this.debug("REGION " + var1 + " " + this.fileName.getName() + "[" + var2 + "," + var3 + "] " + var4 + "B = " + var5);
   }

   private void debugln(String var1, int var2, int var3, String var4) {
      this.debug(var1, var2, var3, var4 + "\n");
   }

   public synchronized DataInputStream getChunkDataInputStream(int var1, int var2) {
      if (this.outOfBounds(var1, var2)) {
         this.debugln("READ", var1, var2, "out of bounds");
         return null;
      } else {
         try {
            int var3 = this.getOffset(var1, var2);
            if (var3 == 0) {
               return null;
            } else {
               int var4 = var3 >> 8;
               int var5 = var3 & 255;
               if (var4 + var5 > this.sectorFree.size()) {
                  this.debugln("READ", var1, var2, "invalid sector");
                  return null;
               } else {
                  this.file.seek((long)(var4 * 4096));
                  int var6 = this.file.readInt();
                  if (var6 > 4096 * var5) {
                     this.debugln("READ", var1, var2, "invalid length: " + var6 + " > 4096 * " + var5);
                     return null;
                  } else {
                     byte var7 = this.file.readByte();
                     byte[] var8;
                     DataInputStream var9;
                     if (var7 == 1) {
                        var8 = new byte[var6 - 1];
                        this.file.read(var8);
                        var9 = new DataInputStream(new GZIPInputStream(new ByteArrayInputStream(var8)));
                        return var9;
                     } else if (var7 == 2) {
                        var8 = new byte[var6 - 1];
                        this.file.read(var8);
                        var9 = new DataInputStream(new InflaterInputStream(new ByteArrayInputStream(var8)));
                        return var9;
                     } else {
                        this.debugln("READ", var1, var2, "unknown version " + var7);
                        return null;
                     }
                  }
               }
            }
         } catch (IOException var10) {
            this.debugln("READ", var1, var2, "exception");
            return null;
         }
      }
   }

   public DataOutputStream getChunkDataOutputStream(int var1, int var2) {
      return this.outOfBounds(var1, var2) ? null : new DataOutputStream(new DeflaterOutputStream(new RegionFile.ChunkBuffer(var1, var2)));
   }

   protected synchronized void write(int var1, int var2, byte[] var3, int var4) {
      try {
         int var5 = this.getOffset(var1, var2);
         int var6 = var5 >> 8;
         int var7 = var5 & 255;
         int var8 = (var4 + 5) / 4096 + 1;
         if (var8 >= 256) {
            return;
         }

         if (var6 != 0 && var7 == var8) {
            this.debug("SAVE", var1, var2, var4, "rewrite");
            this.write(var6, var3, var4);
         } else {
            int var9;
            for(var9 = 0; var9 < var7; ++var9) {
               this.sectorFree.set(var6 + var9, true);
            }

            var9 = this.sectorFree.indexOf(true);
            int var10 = 0;
            int var11;
            if (var9 != -1) {
               for(var11 = var9; var11 < this.sectorFree.size(); ++var11) {
                  if (var10 != 0) {
                     if ((Boolean)this.sectorFree.get(var11)) {
                        ++var10;
                     } else {
                        var10 = 0;
                     }
                  } else if ((Boolean)this.sectorFree.get(var11)) {
                     var9 = var11;
                     var10 = 1;
                  }

                  if (var10 >= var8) {
                     break;
                  }
               }
            }

            if (var10 >= var8) {
               this.debug("SAVE", var1, var2, var4, "reuse");
               var6 = var9;
               this.setOffset(var1, var2, var9 << 8 | var8);

               for(var11 = 0; var11 < var8; ++var11) {
                  this.sectorFree.set(var6 + var11, false);
               }

               this.write(var6, var3, var4);
            } else {
               this.debug("SAVE", var1, var2, var4, "grow");
               this.file.seek(this.file.length());
               var6 = this.sectorFree.size();

               for(var11 = 0; var11 < var8; ++var11) {
                  this.file.write(emptySector);
                  this.sectorFree.add(false);
               }

               this.sizeDelta += 4096 * var8;
               this.write(var6, var3, var4);
               this.setOffset(var1, var2, var6 << 8 | var8);
            }
         }
      } catch (IOException var12) {
         var12.printStackTrace();
      }

   }

   private void write(int var1, byte[] var2, int var3) throws IOException {
      this.debugln(" " + var1);
      this.file.seek((long)(var1 * 4096));
      this.file.writeInt(var3 + 1);
      this.file.writeByte(2);
      this.file.write(var2, 0, var3);
   }

   private boolean outOfBounds(int var1, int var2) {
      return var1 < 0 || var1 >= 32 || var2 < 0 || var2 >= 32;
   }

   private int getOffset(int var1, int var2) throws IOException {
      return this.offsets[var1 + var2 * 32];
   }

   private void setOffset(int var1, int var2, int var3) throws IOException {
      this.offsets[var1 + var2 * 32] = var3;
      this.file.seek((long)((var1 + var2 * 32) * 4));
      this.file.writeInt(var3);
   }

   public void close() throws IOException {
      this.file.close();
   }

   class ChunkBuffer extends ByteArrayOutputStream {
      private int x;
      private int z;

      public ChunkBuffer(int var2, int var3) {
         super(8096);
         this.x = var2;
         this.z = var3;
      }

      public void close() {
         RegionFile.this.write(this.x, this.z, this.buf, this.count);
      }
   }
}
