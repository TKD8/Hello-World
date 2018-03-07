import java.util.Vector;

public class FileTable {

    private Vector<FileTableEntry> table;         // the actual entity of this file table
    private Directory dir;        // the root directory
    short USED = 0;
    short UNUSED = 0;

    public FileTable( Directory directory ) { // constructor
       table = new Vector<FileTableEntry>();     // instantiate a file (structure) table
       dir = directory;           // receive a reference to the Director
    }                             // from the file system

    // major public methods
    // allocate a new file (structure) table entry for this file name
    // allocate/retrieve and register the corresponding inode using dir
    // increment this inode's count
    // immediately write back this inode to the disk
    // return a reference to this file (structure) table entry
    public synchronized FileTableEntry falloc( String filename, String mode ) {
        short iNumber = -1;
        Inode inode = null;

        while (true)
        {
            iNumber = filename.equals("/") ? 0 : dir.namei(filename);
            if(iNumber < 0){
                break;
            }
            if (iNumber >= 0)
            {
                inode = new Inode(iNumber);
                if (mode.compareTo("r") == 0)
                {
                    if (inode.flag == 0 ||
                    inode.flag == 0)
                    {
                        inode.flag = 1;
                    }
                    break;
                }
                    try
                    {
                        wait();
                    }
                    catch (InterruptedException e)
                    {}
                
            }else
                {
                    if (inode.flag == 0 || inode.flag == 3 )
                    {
                        inode.flag = 2;
                        break;
                    }
                    else
                    {
                        try
                        {
                            wait();
                        }
                        catch (InterruptedException e){}
                    }

            }
            if (!mode.equals("r"))
            {
                iNumber = dir.ialloc(filename);
                inode = new Inode();
                inode.flag = 2;
                break;
            }
            else
                return null;

        }

        inode.count++;
        inode.toDisk(iNumber);
        FileTableEntry fte = new FileTableEntry(inode, iNumber, mode);
        table.addElement(fte);
        return fte;
        
    }

    // receive a file table entry reference
    // save the corresponding inode to the disk
    // free this file table entry.
    // return true if this file table entry found in my table
    public synchronized boolean ffree( FileTableEntry e ) {
        if(table.removeElement(e))
        {
            e.inode.count--;
              switch (e.inode.flag) {
                  case 1:  e.inode.flag = 0; break;
                  case 2:  e.inode.flag = 0; break;
                  case 4:  e.inode.flag = 3; break;
                  case 5:  e.inode.flag = 3;
                }
                notify();
                e.inode.toDisk(e.iNumber);
                return true;
        }
        return false;
    }

    // return if table is empty
    // should be called before starting a format
    public synchronized boolean fempty( ) {
       return table.isEmpty( );
    }
 }
