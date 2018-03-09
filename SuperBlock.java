/**
 * SuperBlock.java
 * CSS 430 : final project 
 * Authour: Natnael Tezera
 * 
 * This is a Super block class. It is used to describe nuber of disk blocks.
 * the number of inodes and the block number of the head block of the freelist
 * It is the OS-managed block. No other information recorded in and no user
 * thread are able to get access of this block
 */
public class SuperBlock

{
    private final int defaultInodeBlocks = 64;
    public int totalBlocks; // the number of disk blocks
    public int inodeBlocks; // the number of inodes
    public int freeList;    // the block number of the free list's head.


	/**
	 * Default constructor
	 */
	 public SuperBlock(){
	 	 totalBlocks = 0;
		 inodeBlocks = 0;
		 freeList = 0;
	 }
	 
	/**
	* constructor: accepts the disksize as an argument.
	* It reads the SuperBlock from disk and intialize member variable  for the
	* @param diskSize
	*/
    public SuperBlock( int diskSize )
    {
        byte[] superBlock = new byte[512];
        SysLib.rawread(0, superBlock);
        totalBlocks = SysLib.bytes2int(superBlock, 0);
        inodeBlocks = SysLib.bytes2int(superBlock, 4);
        freeList = SysLib.bytes2int(superBlock, 8);

		// Disk contents are valid	
        if (totalBlocks == diskSize && inodeBlocks > 0 && freeList >= 2)
        {
            return;
        }
            totalBlocks = diskSize;
            format(); 
    }

	/**
	* format: calls the format(int) method to clean the disk
	*/
	public void format(){
		format(64);
	}
	
	/**
	* Format: 
	* Cleans the disk and resets the correct structure if there is illegal state
	* during initialization of an instance.
	* All instance variables of SuperBlock are cleared to default values and 
	* written back to the newly cleared disk.
	* @param numFiles
	*/
    public void format(int numFiles)
    {
    	if(numFiles < 0){
			numFiles = defaultInodeBlocks;
		}

		inodeBlocks = numFiles;
		Inode dummy = null;

		for(int i = 0; i < inodeBlocks; i++){
			dummy = new Inode();
			dummy.flag = 0;
			dummy.toDisk((short) i);
		}

		freeList = (2 + inodeBlocks * 32 / 512);

		byte [] newEmpty = null;	// new dummy blocks

		for (int j = freeList; j < totalBlocks; ++j){
			newEmpty = new byte[Disk.blockSize];

			// erase everything
			for(int k = 0; k < 512; k++){
				newEmpty[k] = 0;
			}

			SysLib.int2bytes(j+1, newEmpty, 0);
			SysLib.rawwrite(j, newEmpty);
		}
		sync();
    }
	/**
	* sync:
	* brings the physical SuperBlock contents inline with any updates
	* performed to the SuperBlock class instance. 
	*/
	public void sync(){
		byte [] arrayOfByte = new byte[Disk.blockSize];
		SysLib.int2bytes(freeList, arrayOfByte, 8);
		SysLib.int2bytes(totalBlocks, arrayOfByte, 0);
		SysLib.int2bytes(inodeBlocks, arrayOfByte, 4);
		SysLib.rawwrite(0, arrayOfByte);
	}

    /**
     * nextFreeBlock
     */
     public int getFreeBlock()  //nextFreeBlock()
      {
        int i = freeList;


        if (i != -1) {
          byte[] arrayOfByte = new byte[Disk.blockSize];

          SysLib.rawread(i, arrayOfByte);
          freeList = SysLib.bytes2int(arrayOfByte, 0);	// update next free block

          SysLib.int2bytes(0, arrayOfByte, 0);
          SysLib.rawwrite(i, arrayOfByte);
        }
        return i;	// return block location
      }

    public boolean returnBlock(int paramInt)
     {
       if (paramInt >= 0) {
         byte[] arrayOfByte = new byte[Disk.blockSize];

         for (int i = 0; i < 512; i++)
           arrayOfByte[i] = 0;
         SysLib.int2bytes(freeList, arrayOfByte, 0);
         SysLib.rawwrite(paramInt, arrayOfByte);
         freeList = paramInt;
         return true;
       }
       return false;
     }
   
}