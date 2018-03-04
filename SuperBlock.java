public class SuperBlock

{
    private final int defaultInodeBlocks = 64;
    public int totalBlocks; // the number of disk blocks
    public int inodeBlocks; // the number of inodes
    public int freeList;    // the block number of the free list's head.



    public SuperBlock( int diskSize )
    {
        byte[] superBlock = new byte[Disk.blockSize];
        SysLib.rawread(0, superBlock);
        totalBlocks = SysLib.bytes2int(superBlock, 0);
        inodeBlocks = SysLib.bytes2int(superBlock, 4);
        freeList = SysLib.bytes2int(superBlock, 8);

        if (totalBlocks == diskSize && inodeBlocks > 0 && freeList >= 2)
        {
            // Disk contents are valid
            return;
        }
        else
        {
            totalBlocks = diskSize;
            format(defaultInodeBlocks);
        }
    }
}