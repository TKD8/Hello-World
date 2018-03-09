public class Inode {
   private final static int iNodeSize = 32;       // fix to 32 bytes
   public final static int directSize = 11;      // # direct pointers
   private final static int maxBytes = 512;         // maximum size

   public int length;                             // file size in bytes
   public short count;                            // # file-table entries pointing to this
   public short flag;                             // 0 = unused, 1 = used, ...
   public short direct[] = new short[11]; // direct pointers
   public short indirect;                         // a indirect pointer

   // a default constructor
   // assigns empty values

   public Inode( ) {                                     
      length = 0;
      count = 0;
      flag = 1;
      for ( int i = 0; i < 11; ++i){
         this.direct[i] = -1;
      }
      this.indirect = -1;
   }

   // retrieving inode from disk
   public Inode( short iNumber ) {                       
          int n = 1 + iNumber / 16;
        byte[] arrby = new byte[512];
        SysLib.rawread((int)n, (byte[])arrby);
        int n2 = iNumber % 16 * 32;
        this.length = SysLib.bytes2int((byte[])arrby, (int)n2);
        this.count = SysLib.bytes2short((byte[])arrby, (int)(n2 += 4));
        this.flag = SysLib.bytes2short((byte[])arrby, (int)(n2 += 2));
        n2 += 2;
        for (int i = 0; i < 11; ++i) {
            this.direct[i] = SysLib.bytes2short((byte[])arrby, (int)n2);
            n2 += 2;
        }
        this.indirect = SysLib.bytes2short((byte[])arrby, (int)n2);
        n2 += 2;
   }

   // save to disk as the i-th inode
   public void toDisk( short iNumber ) {  
        int n;
        byte[] arrby = new byte[32];
        int n2 = 0;
        SysLib.int2bytes((int)this.length, (byte[])arrby, (int)n2);
        SysLib.short2bytes((short)this.count, (byte[])arrby, (int)(n2 += 4));
        SysLib.short2bytes((short)this.flag, (byte[])arrby, (int)(n2 += 2));
        n2 += 2;
        for (n = 0; n < 11; ++n) {
            SysLib.short2bytes((short)this.direct[n], (byte[])arrby, (int)n2);
            n2 += 2;
        }
        SysLib.short2bytes((short)this.indirect, (byte[])arrby, (int)n2);
        n2 += 2;
        n = 1 + iNumber / 16;
        byte[] arrby2 = new byte[512];
        SysLib.rawread((int)n, (byte[])arrby2);
        n2 = iNumber % 16 * 32;
        System.arraycopy(arrby, 0, arrby2, n2, 32);
        SysLib.rawwrite((int)n, (byte[])arrby2);
        
   } 
   

    public  int findIndexBlock()
    {
            return indirect;
       
    }

    public boolean registerIndexBlock(short iNumber) {
        for (int i = 0; i < 11; ++i) {
            if (this.direct[i] != -1) continue;
            return false;
        }
        if (this.indirect != -1) {
            return false;
        }
        this.indirect = iNumber;
        byte[] arrby = new byte[512];
        for (int i = 0; i < 256; ++i) {
            SysLib.short2bytes((short)-1, (byte[])arrby, (int)(i * 2));
        }
        SysLib.rawwrite((int)iNumber, (byte[])arrby);
        return true;
   }

     public int findTargetBlock(int offset){
        int n2 = offset / 512;
        if (n2 < 11) {
            return this.direct[n2];
        }
        if (this.indirect < 0) {
            return -1;
        }
        byte[] arrby = new byte[512];
        SysLib.rawread((int)this.indirect, (byte[])arrby);
        int n3 = n2 - 11;
        return SysLib.bytes2short((byte[])arrby, (int)(n3 * 2));
    }

    public int registerTargetBlock (int entry, short offset){   //public int getIndexBlockNumber(int entry, short offset){
        int n2 = entry / 512;
        if (n2 < 11) {
            if (this.direct[n2] >= 0) {
                return -1;
            }
            if (n2 > 0 && this.direct[n2 - 1] == -1) {
                return -2;
            }
            this.direct[n2] = offset;
            return 0;
        }
        if (this.indirect < 0) {
            return -3;
        }
        byte[] arrby = new byte[512];
        SysLib.rawread((int)this.indirect, (byte[])arrby);
        int n3 = n2 - 11;
        if (SysLib.bytes2short((byte[])arrby, (int)(n3 * 2)) > 0) {
            return -1;
        }
        SysLib.short2bytes((short)offset, (byte[])arrby, (int)(n3 * 2));
        SysLib.rawwrite((int)this.indirect, (byte[])arrby);
        return 0;
    }
    public byte[] unregisterIndexBlock(){  //public byte[] freeIndirectBlock(){
        if (this.indirect >= 0) {
            byte[] arrby = new byte[512];
            SysLib.rawread((int)this.indirect, (byte[])arrby);
            this.indirect = -1;
            return arrby;
        }
        return null;
    }


    

    public boolean setIndexBlock(short blockNumber){
        for (int i = 0; i < directSize; i++){
            if (direct[i] == -1){
                return false;
            }
        }
        if (indirect != -1){
            return false;
        }
        indirect = blockNumber;
        byte[ ] data = new byte[maxBytes];
        for(int i = 0; i < (maxBytes/2); i++){
            SysLib.short2bytes((short) -1, data, i * 2);
        }
        SysLib.rawwrite(blockNumber, data);
       return true;
    }
    
    
}