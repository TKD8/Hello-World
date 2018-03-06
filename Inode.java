public class Inode {
   private final static int iNodeSize = 32;       // fix to 32 bytes
   public final static int directSize = 11;      // # direct pointers
   private final static int maxBytes = 512;         // maximum size

   public int length;                             // file size in bytes
   public short count;                            // # file-table entries pointing to this
   public short flag;                             // 0 = unused, 1 = used, ...
   public short direct[] = new short[directSize]; // direct pointers
   public short indirect;                         // a indirect pointer

   // a default constructor
   // assigns empty values
   Inode( ) {                                     
      length = 0;
      count = 0;
      flag = 1;
      for ( int i = 0; i < directSize; i++ )
         direct[i] = -1;
      indirect = -1;
   }

   // retrieving inode from disk
   Inode( short iNumber ) {                       
      // ignore the superblock +1
      int blockNumber = 1 + (iNumber/16); 
      byte[] data = new byte[Disk.blockSize]; // allocate byte array to size of 512 
      // read from blockNumber to data
      SysLib.rawread(blockNumber, data);
      // starting location
      int offset = (iNumber % 16) * iNodeSize;
      length = SysLib.bytes2int(data,offset);
      offset += 4;
      count = SysLib.bytes2short(data,offset);
      offset += 2;
      flag = SysLib.bytes2short(data,offset);
      offset += 2;

      for(int i = 0; i < directSize; i++)
      {
        direct[i] = SysLib.bytes2short(data,offset);
        offset += 2;
      }

      indirect = SysLib.bytes2short(data,offset);
   }

   // save to disk as the i-th inode
   int toDisk( short iNumber ) {  
        int offset = 0;
        // check for valid iNumber
        if(iNumber < 0)
        {
            return -1;
        }   
        int blockNumber = 1 + (iNumber/16); 
        byte[] data = new byte[Disk.blockSize]; // allocate byte array to size of 512 
        // read from blockNumber to data
        SysLib.rawread(blockNumber, data);
        // starting location
        int offest = (iNumber % 16) * iNodeSize;

        SysLib.int2bytes(length, data, offset);
        offset += 4;
        SysLib.short2bytes(count, data, offset);
        offset += 2;
        SysLib.short2bytes(flag, data, offset);
        offset += 2;

        for (int i = 0; i < directSize; i++){
            SysLib.short2bytes(direct[i], data, offset);
            offset += 2;
        }

        SysLib.short2bytes(indirect, data, offset);
        offset += 2;

        SysLib.rawwrite(blockNumber, data);
        return 0;
   } 
   

    byte[] freeIndirectBlock(){
        if (indirect >= 0)
        {
            byte[] data = new byte[maxBytes];
            SysLib.rawread(indirect, data);
            indirect = -1;
            return data;
        }
        else
        {
            return null;
        }
    }

    int findTargetBlock(int offset){
        int target = offset / maxBytes;
        if (target < directSize){
            return direct[target];
        }
        if (indirect < 0){
            return -1;
        }
        byte[] data = new byte[maxBytes];
        SysLib.rawread(indirect, data);
        int blockSpace = (target - directSize) * 2;
        return SysLib.bytes2short(data, blockSpace);
    }

    int getIndexBlockNumber(int entry, short offset){
        int target = entry/maxBytes;
        if (target < directSize){
            if(direct[target] >= 0){
                return -1;
            }
            if ((target > 0 ) && (direct[target - 1 ] == -1)){
                return -2;
            }
            direct[target] = offset;
            return 0;
        }
        if (indirect < 0){
            return -3;
        }
        else{
            byte[] data = new byte[maxBytes];
            SysLib.rawread(indirect,data);
            int blockSpace = (target - directSize) * 2;
            if ( SysLib.bytes2short(data, blockSpace) > 0){
                return -1;
            }
            else
            {
                SysLib.short2bytes(offset, data, blockSpace);
                SysLib.rawwrite(indirect, data);
            }
        }
        return 0;
    }

    boolean setIndexBlock(short blockNumber){
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