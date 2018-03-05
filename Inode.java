public class Inode {
   private final static int iNodeSize = 32;       // fix to 32 bytes
   private final static int directSize = 11;      // # direct pointers

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
      int offest = (iNumber % 16) * iNodeSize;
      length = SysLib.byts2int(data,offset);
      offset += 4;
      count = SysLib.bytes2short(data,offset);
      offset += 2;
      flag = SysLib.bytes2short(data,offset);
      offest += 2;

      for(int i = 0; i < directSize; i++)
      {
        direct[i] = SysLib.bytes2short(data,offset);
        offest += 2;
      }

      indirect = SysLib.bytes2short(data,offest);
   }

   // save to disk as the i-th inode
   int toDisk( short iNumber ) {  
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
}