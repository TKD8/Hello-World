/**
* CSS 430
* Winter 2018
* Professor Sung
*
* Responsible for manipulating disk. It provides user threads with system calls
* that will allow them to format, to open, to read from, to write to,
* to update the seek pointer of , to close, to delete, and to get the size of 
* their files. It hides all the implementations from user threads.
*
* @author: Natnael
* @author: Artiom
* @author: Tylor
*/

public class FileSystem{
	//  member fields
    private SuperBlock superblock;
    private Directory directory;
    private FileTable filetable;

    //  attrbutes
    private final int SEEK_SET = 0;
    private final int SEEK_CUR = 1;
    private final int SEEK_END = 2;

    /**
    * Constructor
    * initializes superblock, directory and filetable according using The
    * given parameter
    * @param blocks
    *
    */
    public FileSystem(int blocks){
        superblock = new SuperBlock(blocks);
        directory = new Directory(superblock.inodeBlocks);
	filetable = new FileTable(directory);	

        // read the "/" file from disk
        FileTableEntry directoryEntry = open("/", "r");
        int directorySize = fsize(directoryEntry);

        if(directorySize > 0){
            
            // the directory contains data
            // let's read, and copy it
            byte[] directoryData = new byte[directorySize];
            read(directoryEntry, directoryData);
            directory.bytes2directory(directoryData);
            
            }
            close(directoryEntry);	// close the diectory
    }

    /**
    * sync:
    * syncs the file system back to the physical disk. it writes
    * the directory information to the disk. calls the sync metheod
    * from superblock to synchronize the SuperBlock
    *
    */
    public void sync(){
        // open root director with write access
        FileTableEntry root = open("/", "w");
        byte [] tempData = directory.directory2bytes();
        write(root, directory.directory2bytes());	// write the directory to root
        close(root);					// close root Directory
        superblock.sync();				// sync

    }

    /**
    * format:
    * Formats the disk (Disk.java's data contents).  
    * @param files amount of files being formatted
    */
    public boolean format(int files){
        while(!filetable.fempty()){}

        superblock.format(files);	// format SuperBlock
        // creates direcoty, and register "/" in directory 0
        directory = new Directory(superblock.inodeBlocks);

        // file table is created and store directory in file table
        filetable = new FileTable(directory);	

        return true;
    }

    /**
    * open
    *
    * Responsible for opening a file specified by fileName string
    * in the given mode (where "r" = ready only, "w" = write only, 
    * "w+" = read/write, "a" = append)
    * @param fileName name of file opening
    * @param mode purpose of open
    */
    public FileTableEntry open(String fileName, String mode){

        // filename is passed to directroy
        FileTableEntry ftEntry = filetable.falloc(fileName, mode);

        if(mode == "w" && !this.deallocAllBlocks(ftEntry)){
            return null;
        }
        return ftEntry; //return FileTable Entry 
    }

    /**
    * close
    *
    * Responsible for closing the file corresponding to the given
    * file table entry. It returns true if successful or false otherwise
    * @param entry	
    */
    public boolean close(FileTableEntry entry){
            // synchronize the Entry
            synchronized (entry){
                    entry.count--;
                    if(entry.count == 0){
                            return filetable.ffree(entry);
                    }
                    return true;
            }
    }

    /**
    * fsize
    * Returns the file size in bytes indicated by the fd
    * @param fd the fileTableEntry queue
    * @return size 
    *
    */
    public synchronized int fsize(FileTableEntry fd){
            // synchronize Entry
            synchronized(fd){
                    // set a new inode object to the entries
                    Inode inode = fd.inode;
                    return inode.length;
            }
    }
    /**
    * read:
    * Reads up to buffer.length bytes from the file indicated by fd, 
    * starting at the position currently pointed to by the seek pointer.
    * If bytes remaining between the current seek pointer and the end of 
    * file are less than buffer.length, SysLib.read reads as many bytes as 
    * possible, putting them into the beginning of buffer. It increments 
    * the seek pointer by the number of bytes to have been read. The return 
    * value is the number of bytes that have been read, or a negative 
    * value upon an error.
    * 
    * @param fd table fd reading from
    * @param buffer size of data being read
    * 
    * @return amount of data
    */
    public int read (FileTableEntry fd, byte[] buffer){

        // check mode
        if((fd.mode == "w") || (fd.mode == "a")){
                return -1;
        }

        int size = buffer.length;		// total size of data to read
        int bufferTrack = 0;			// tracks the buffer
        int itrSize = 0;

        synchronized(fd){
            while(fd.seekPtr < fsize(fd) && (size > 0)){
                int currentBlock = fd.inode.findTargetBlock(fd.seekPtr);

                if(currentBlock == -1){		//out of bound
                        break;
                }

                byte[] data = new byte[512];
                SysLib.rawread(currentBlock, data);
                int dataOffSet = fd.seekPtr % 512;
                int blocksLeft = 512 - dataOffSet;
                int fileLeft = fsize(fd) - fd.seekPtr;

                int i2 = Math.min(Math.min(blocksLeft, size), fileLeft);

                System.arraycopy(data, dataOffSet, buffer, bufferTrack, i2);


                fd.seekPtr += i2;
                bufferTrack += i2;
                size -= i2;
            }
            return bufferTrack;
        }

    }

    /**
    * write;
    * Writes the contents of buffer to the file indicated by fd, starting at the position 
    * indicated by the seek pointer. The operation may overwrite existing data in the file
    * and/or append to the end of the file. SysLib.write increments the seek pointer by 
    * the number of bytes to have been written. The return value is the number of bytes 
    * that have been written, or a negative value upon an error.
    *
    * @param fd tabble fd reading from
    * @param buffer size of data being read
    * 
    * @return the number of bytes written, -1 if fails
    */
    public int write(FileTableEntry fd, byte [] buffer){

        // check mode and valid fd
        if(fd.mode == "r"){
                return -1;
        }

        synchronized(fd){
            int bufferSize = buffer.length;
            int bytesWritten = 0;

            while(bufferSize > 0){
                int location = fd.inode.findTargetBlock(fd.seekPtr);

                // check if it current block is null
                if(location == -1){
                    short newLocation = (short) superblock.getFreeBlock();
                    int testPtr = fd.inode.registerTargetBlock(fd.seekPtr, newLocation);

                    switch(testPtr){
                        case 0:
                                break;
                        case -2:
                        case -1:
                                return -1;
                        case -3:
                            short freeBlock = (short) this.superblock.getFreeBlock();

                            // indirect pointer is empty
                            if(!fd.inode.setIndexBlock(freeBlock)){
                                return -1;
                            }

                            // check block pointer error
                            if(fd.inode.registerTargetBlock(fd.seekPtr, newLocation) != 0){
                                return -1;
                            }
                            break;
                        }
                        location = newLocation;
                }

                byte [] tempBuff = new byte[512];
                SysLib.rawread(location, tempBuff);
                int tempPtr = fd.seekPtr % 512;
                int diff = 512 - tempPtr;

                if(diff > bufferSize){
                    System.arraycopy(buffer, bytesWritten, tempBuff, tempPtr,bufferSize);
                    SysLib.rawwrite(location, tempBuff);
                    fd.seekPtr += bufferSize;
                    bytesWritten += bufferSize;
                    bufferSize = 0;
                }else{
                    System.arraycopy(buffer, bytesWritten, tempBuff, tempPtr, diff);
                    SysLib.rawwrite(location, tempBuff);
                    fd.seekPtr += diff;
                    bytesWritten += diff;
                    bufferSize -= diff;
                }
            }

            // update inode length if seekPtr larger
            if(fd.seekPtr > fd.inode.length){
                    fd.inode.length = fd.seekPtr;
            }

            fd.inode.toDisk(fd.iNumber);
            return bytesWritten;
        }

    }

    /**
    * seek:
    * Updates the seek pointer corresponding to fd as follows. It returns 0 if the 
    * If whence is SEEK_SET (= 0), the file's seek pointer is set to offset bytes 
    * from the beginning of the file 
    * If whence is SEEK_CUR (= 1), the file's seek pointer is set to its current value 
    * plus the offset. The offset can be positive or negative. 
    * If whence is SEEK_END (= 2), the file's seek pointer is set to the size of the 
    * file plus the offset. The offset can be positive or negative.  
    * 
    * @param fd the file table entry
    * @param offset initial offset
    * @param whence start of seek pointer
    *
    * @return seek pointer of the Entry
    *
    */
    public int seek(FileTableEntry fd, int offset, int location) {

    synchronized (fd) {
        switch (location) {
            //beginning of file
            case SEEK_SET:
                //set seek pointer to offset of beginning of file
                fd.seekPtr = offset;
                break;
            // current position
            case SEEK_CUR:
                fd.seekPtr += offset;
                break;
            // end of file
            case SEEK_END:
                // set seek pointer to size + offset
                fd.seekPtr = fd.inode.length + offset;
                break;
            // unsuccessful
            default:
                return -1;
        }
        if (fd.seekPtr < 0) {
            fd.seekPtr = 0;
        }
        if (fd.seekPtr > fd.inode.length) {
            fd.seekPtr = fd.inode.length;
        }
        return fd.seekPtr;
    }

}

    /**
    * delete:
    * Deletes the file specified by fileName. All blocks used by file 
    * are freed. If the file is currently open, it is not deleted and 
    * the operation returns a -1. If successfully deleted a 0 is returned. 
    * 
    * @param fileName: file name need to be deleted
    * @return 0 for success and -1 for unsuccessful
    */
    public boolean delete(String fileName){
            FileTableEntry tcb = open(fileName, "w");
            short s = tcb.iNumber;
            return (close(tcb)) && (directory.ifree(s));
    }

    /**
    * deallocate:
    * this is a helper method. it helps to deallocate all blocks
    * @param FileTableEntry
    * @return true or false
    *
    */ 
private boolean deallocAllBlocks(FileTableEntry fileTableEntry) {
    short invalid = -1;
    if (fileTableEntry.inode.count != 1) {
        SysLib.cerr("Null Pointer");
        return false;
    }
    for (short blockId = 0; blockId < fileTableEntry.inode.directSize; blockId++) {
        if (fileTableEntry.inode.direct[blockId] != invalid) {
            superblock.returnBlock(blockId);
            fileTableEntry.inode.direct[blockId] = invalid;
        }
    }
    byte[] data = fileTableEntry.inode.unregisterIndexBlock();
    if (data != null) {
        short blockId;
        while ((blockId = SysLib.bytes2short(data, 0)) != invalid) {
            superblock.returnBlock(blockId);
        }
    }
    fileTableEntry.inode.toDisk(fileTableEntry.iNumber);
    return true;
    }
}