/*
 * Bryce Jensen
 * 10/16/2020
 *
 *  openjdk 11.0.1 2018-10-16 LTS
 *  to compile:
 *      $ javac BlockchainE.java
 *
 *  to run, in one shell:
 *     $ java BlockchainE
 *
 *  Files needed to run:
 *                     a. checklist-block.html
 *                     b. Blockchain.java
 *                     c. BlockchainLog.txt
 *                     d. BlockchainLedgerSample.json
 *                     e. BlockInput0.txt
 *                     f. BlockInput1.txt
 *                     g. BlockInput2.txt
 *
 * Thanks: http://www.javacodex.com/Concurrency/PriorityBlockingQueue-Example
 *
 *
 * Notes:
 *       This is mini-project E of the Blockchain assignment.
 *
 *       It contains a simple blockchain program with five nodes. A dummy genesis block and
 *       four other simple blocks.
 *
 *       Each block contains some arbitrary data, the hash of the previous block,
 *       a timestamp of its creation, and the hash of the block itself.
 *
 *       When calculating the hash for each block, the contained elements in the block
 *       are turned into strings and concatenated together with a nonce to then be hashed.
 *
 *       The verifying of blocks is done by taking in the block hash prefix and trying every possible
 *       combination by incrementing our nonce  until our prefixString is equal to our designated prefix
 *		
 *	    It currently can marshall its data out into JSON format and compile successfully.
 *
 *      As of 10/17/2020 at 10:41am, I" have my readFromJSON() method flushed out. Will need to clone
 *      and test in the VM to make sure everything still compiles and does what I expect it to.
 *
 *	Starting implementation of my program accepting command line arguments
 * 	today on 10/22/2020.
 */

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.sql.Time;
import java.util.*;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadLocalRandom;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;


class BlockRecord implements Serializable // make serializable in order to send via socket
{
    static String BlockID;
    // will hold the blocks ID
    static String VerificationProcessID;
    // holds the ID of the process that verifies the block, or tries to
    static String TimeStamp;
    // the blocks time stamp
    static String PreviousHash;
    // hash of the previous block
    static UUID uuid;
    // how we will marshall data to JSON
    static String Data;
    // the data contained in the block
    static String RandomSeed;
    // this will be our means of trying to verify the block
    static String WinningHash;
    // the hash of our winning guess

    public String getTimeStamp()
    {
        return TimeStamp;
    }

    public static void setTimeStamp(String _timeStamp)
    {
        TimeStamp = _timeStamp;
    }

    public String getBlockID()
    {
        return BlockID;
        // accessor to return block ID
    }
    public void setBlockID(String _BlockID)
    {
        this.BlockID = _BlockID;
        // accessor for setting the block ID
    }

    public String getVerificationProcessID()
    {
        return VerificationProcessID;
        // accessor to return verificationProcessID
    }
    public void setVerificationProcessID(String _VerificationProcessID)
    {
        this.VerificationProcessID = _VerificationProcessID;
    }

    public UUID getUUID()
    {
        return this.uuid;
    }
    public void setUUID(UUID _uuid)
    {
        this.uuid = _uuid;
    }
    // get/setter for unique identifier

    public String getPreviousHash()
    {
        return this.PreviousHash;
    }
    public void setPreviousHash(String _PreviousHash)
    {
        this.PreviousHash = _PreviousHash;
    }
    // getter/setter for previousHash

    public String getData()
    {
        return this.Data;
    }
    public static void setData(String _Data)
    {
        Data = _Data;
    }
    // getter / setter for obtaining and setting the data contained

    public String getRandomSeed()
    {
        return this.RandomSeed;
    }
    public void setRandomSeed(String _RandomSeed)
    {
        this.RandomSeed = _RandomSeed;
    }
    // getter / setters fro gettting and setting the random seed

    public String getWinningHash()
    {
        return this.WinningHash;
    }
    public void setWinningHash(String _WinningHash)
    {
        this.WinningHash = _WinningHash;
    }
    // getter and setters to obtain or set the winning hash
}

class ProcessBlock
{
    int processID;
    PublicKey publicKey;
    int portNum;
    String IPAddress;
    /*
     * member variables for the process blocks being cast to members in the
     * multicast group
     */
}

class Ports
{
    public static int KeyServerPortBase = 6050;
    // starting port num when the process first runs for the Key Server
    public static int UVBServerPortBase = 6051;
    // starting point num when the process fisrt runs for the Unverified Block Server
    public static int BlockchainServerPortBase = 6052;
    // starting port num when the process first runs for Blockchain Server

    public static int KeyServerPort;
    // where we will hold the incremented port num for new processes running Key Server
    public static int UVBServerPort;
    // where we will hold the incremented port num for new processes running Unverified Blockchain Server
    public static int BlockchainServerPort;
    // where we will hold the incremented port num for new processes running Blockchain Server

    public void setPorts()
    {
        KeyServerPort = KeyServerPortBase + (BlockchainE.PID * 1000);
        // assign Key Server port to every new process incremented by 1000
        UVBServerPort = UVBServerPortBase + (BlockchainE.PID * 1000);
        // assign Unverified Blockchain Server port to every new process incremented by 1000
        BlockchainServerPort = BlockchainServerPortBase + (BlockchainE.PID * 1000);
        // assign Blockchain Server port to every new process incremented by 1000
    }
}


/*
    Worker that handles incoming Public Keys
 */
class PublicKeyWorker extends Thread
{
    Socket keySocket;
    // only member variable and will remain local

    PublicKeyWorker(Socket _socket)
    {
        keySocket = _socket;
        // constructor to assign argument as key socket
    }

    public void run()
    {
        try
        {
            BufferedReader input = new BufferedReader(new InputStreamReader(keySocket.getInputStream()));
            // declare and initialize new Buffered Reader for our input
            String data = input.readLine();
            // declare and initialize variable data to hold our input in String format
            System.out.println("Got key: " + data);
            // print out our key to the console
            keySocket.close();
            // close the keySocket off
        } catch (IOException ioe)
        {
            ioe.printStackTrace();
            // print out any exceptions caught out to console to debug
        }
    }
}

class PublicKeyServer implements Runnable
{
    public ProcessBlock[] PBlock = new ProcessBlock[3];
    // declare new array of Process Blocks to store the processes we plan to start up

    public void run()
    {
        int q_len = 6;
        Socket keySocket;
        System.out.println("Starting Key Server input thread using " + Integer.toString(Ports.KeyServerPort));
        // print out to the console which port is being used for the key server port

        try
        {
            ServerSocket serverSocket = new ServerSocket(Ports.KeyServerPort, q_len);
            // declare and initialize anew server socket
            while (true)
            {
                keySocket = serverSocket.accept();
                // keep accepting incoming connections
                new PublicKeyWorker(keySocket).start();
                // spawn our worker to begin handling those connections
            }
        } catch (IOException ioe)
        {
            System.out.println(ioe);
        }
    }
}

class UVBServer implements Runnable
{
    BlockingQueue<BlockRecord> queue;
    // declare a new Clocking Queue of BlockRecords

    UVBServer(BlockingQueue<BlockRecord> queue)
    {
        this.queue = queue;
        // constructor to bind priority queue to local variable queue
    }

    public static Comparator<BlockRecord> BlockTimeStampComparator = new Comparator<BlockRecord>()
    {
        @Override
        public int compare(BlockRecord _b1, BlockRecord _b2)
        {
            String s1 = _b1.getTimeStamp();
            // compare string 1 to block 1
            String s2 = _b2.getTimeStamp();
            // compare string 2 to block 2
            if (s1 == s2)
            // return true if s1 equals s2
            {
                return 0;
            }

            if (s1 == null)
            // return false if s1 is null
            {
                return -1;
            }

            if (s2 == null)
            // return false if s2 is null
            {
                return 1;
            }

            return s1.compareTo(s2);
            // return our comparison
        }
    };

    class UVBWorker extends Thread
    {
        Socket socket;
        // socket member variable

        UVBWorker (Socket _sock)
        {
            socket = _sock;
            // assign socket to argument _sock
        }

        BlockRecord BR = new BlockRecord();
        // declare and initialize a new BlockRecord

        public void run()
        {
            System.out.println("In Unverified Block Worker");
            // print out debugging statement to know where we are in console
            try
            {
                ObjectInputStream unverifiedInput = new ObjectInputStream(socket.getInputStream());
                // declare a new Object Input Stream and assign to variable unverifiedInput
                BR = (BlockRecord) unverifiedInput.readObject();
                // read in block record from unverified input and save it to variable BR
                System.out.println("Received Unverified Block: " + BR.getTimeStamp() + " " + BR.getData());
                // print to the console the unverified blocks timestamp and data contained
                queue.put(BR);
                // add our block record to our blocking queue
                // may fail if we do not have our queue set to be large enough to contain all puts
                socket.close();
                // close the sockets connection
            } catch (Exception exception)
            {
                exception.printStackTrace();
                // print out any exceptions caught to the console to debug
            }
        }
    }

    public void run()
    {
        int q_len = 6;
        // number of opsys requests
        Socket socket;
        // declare new socket to connect UVBServer
        System.out.println("Starting the Unverified Block Server input thread using: " + Integer.toString(Ports.UVBServerPort));
        // print to the client that we are starting up the UVBServer input thread
        try
        {
            ServerSocket UVBServerSocket = new ServerSocket(Ports.UVBServerPort);
            // declare and initialize new server socket  for our incoming unverified blocks
            while (true)
            {
                socket = UVBServerSocket.accept();
                // connect server socket to retrieve new UVB
                System.out.println("*New Connection to the Unverified Block Server*");
                // print out a notification to the client that we received a new connection to the UVBServer
                new UVBWorker(socket).start();
                // spawn new unverified block worker to handle new processes
            }
        } catch (IOException ioe)
        {
            ioe.printStackTrace();
            // notify client that an exception was caught
        }
    }
}

class UVBConsumer implements Runnable
{
    PriorityBlockingQueue<BlockRecord> queue;
    // using queue passed from blockchain
    int PID;
    // declare new variable to hold thread number

    UVBConsumer(PriorityBlockingQueue<BlockRecord> queue)
    {
        this.queue = queue;
        //constructor that binds UVBConsumer to queue being passed
    }

    public void run()
    {
        String data;
        // variable to hold the block data
        String timeStamp;
        // declare variable to hold timestamp
        BlockRecord tempRecord;
        // declare a temporary blockrecord variable to hold blocks being manipulated in queue
        PrintStream toBlockChainServer;
        // declare new printstream object
        Socket BlockChainSocket;
        // declare new blockchain socket
        String newBlock;
        // declare a string to hold new block
        String fakeVerifiedBlock;
        // declare a string to hold our fakeVerifiedBlock
        Random random = new Random();
        // declare and initialize new random variable

        System.out.println("Starting the Unverified Block Priority Queue Consumer Thread \n");
        // print out to the console that the UVB priority queue is starting up
        try
        {
            while(true)
            // take in the Unverified Block queue and verify the blocks
            {
                tempRecord = queue.take();
                // take next blockrecord from queue and verify the block *fake work*
                data = tempRecord.getData();
                // get the data from blockrecord
                timeStamp = tempRecord.getTimeStamp();
                // get the timestamp so we knw when this block was created
                System.out.println("Consumer retireved unverified: " + data + " " + timeStamp);
                // print out he block and timestamp to the console

                int j;
                // new int variable to help us do some fake  work
                for (int i = 0; i < 99; i++)
                {
                    j = ThreadLocalRandom.current().nextInt(0, 10);
                    // assign a random number between 0 and 10 to j
                    Thread.sleep((random.nextInt(9) * 100));
                    // have the threads sleep for a random amount opf time to simulate work
                    if (j <  3)
                    {
                        break;
                        // when j is less than 3 exit the work
                    }
                }

                if (BlockchainE.fakeBlock.indexOf(data.substring(1,9)) < 0)
                {
                    fakeVerifiedBlock = "[" + data + " verified by P" + BlockchainE.PID + " at time " + Integer.toString(ThreadLocalRandom.current().nextInt(100,1000)) + "]\n";
                    // build out our string to print out
                    System.out.println("Fake verified block: " + fakeVerifiedBlock);
                    // print out the fake verified block string to console
                    String tempBlockchain = fakeVerifiedBlock + BlockchainE.fakeBlock;
                    // build a string version of our temp blockchain

                    for (int i = 0; i < BlockchainE.numProcesses; i++)
                    {
                        BlockChainSocket = new Socket(BlockchainE.serverName, Ports.BlockchainServerPortBase + (i * 1000));
                        // declare a new blockshain socket that takes in localhost and correct port depending on the process number
                        toBlockChainServer = new PrintStream(BlockChainSocket.getOutputStream());
                        // hold output to server in variable toBlockchainServer
                        toBlockChainServer.println(tempBlockchain);
                        // print our temporary blockchan to the console
                        toBlockChainServer.flush();
                        // flush output
                        BlockChainSocket.close();
                        // close socket
                    }
                }

                Thread.sleep(1500);
                // have our processes sleep while blockchain is updated
            }
        } catch (Exception exception)
        {
            exception.printStackTrace();
            // print out any exceptions caught to the console
        }
    }
}

class BlockchainWorker extends Thread
{
    Socket socket;
    // declare a socket for our blockchain worker
    BlockchainWorker(Socket _sock)
    {
        socket = _sock;
        // assign socket to _sock in constructor
    }

        public void run()
        {
            try
            {
                BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String blockData = "";
                // declare and initialize block data to an empty string
                String blockDataInput = input.readLine();
                // declare and initialize a block data input variable  that takes in input from our Buffered Reader
                while((blockDataInput = input.readLine()) != null)
                {
                    blockData = blockData + "\n" + blockDataInput + "\n\r\n\r";
                    // print put block data to the console
                }
                BlockchainE.fakeBlock = blockData;
                // replace with winning blockchain
                System.out.println(" _____________New Blockchain_____________\n" + BlockchainE.fakeBlock + "\n\n");
                socket.close();
                // close our sockdt
            } catch (IOException ioe)
            {
                ioe.printStackTrace();
            }
        }
}


class BlockchainServer implements Runnable
{
    public void run()
    {
        int q_len = 6;
        // number of opsys requests
        Socket socket;
        // declare a new socket
        System.out.println("Starting the Blockchain server input thread using " + Integer.toString(Ports.BlockchainServerPort));
        try
        {
            ServerSocket serverSocket = new ServerSocket(Ports.BlockchainServerPort, q_len);
            // declare and implement new server socket taking in the blockchain server port
            while (true)
            {
                socket = serverSocket.accept();
                // accept incoming connections
                new BlockchainWorker(socket).start();
                // spawn new blockchain worker to handle requests
            }
        } catch (IOException ioException)
        {
            ioException.printStackTrace();
            // print out caught exceptions
        }
    }
}




public class BlockchainE
{
    public static String hash;
    public static String previousHash;
    public static String data;
    public static long timeStamp;
    public static String TimeStamp;
    public static int  nonce;
    // declaration of private member variables for block header

    public static String serverName = "localhost";
    // declare our servername and save it as a string

    public static String fakeBlock = "[first block]";
    // declare our dummy genesis block

    public static int numProcesses = 3;
    // number of processes we plan to run

    public static int PID = 0;
    // ID numberof this process

    public static final String ALGORITHM = "RSA";
    // using RSA encryption

    public static LinkedList<BlockRecord> recordList = new LinkedList<BlockRecord>();
    // declare and initialize a new linked list full of BlockRecords

    public static Comparator<BlockRecord> BlockTimeStampComparator = new Comparator<BlockRecord>()
    {
        @Override
        public int compare(BlockRecord _b1, BlockRecord _b2)
        {
            String s1 = _b1.getTimeStamp();
            // compare string 1 to block 1
            String s2 = _b2.getTimeStamp();
            // compare string 2 to block 2
            if (s1 == s2)
            // return true if s1 equals s2
            {
                return 0;
            }

            if (s1 == null)
            // return false if s1 is null
            {
                return -1;
            }

            if (s2 == null)
            // return false if s2 is null
            {
                return 1;
            }

            return s1.compareTo(s2);
            // return our comparison
        }
    };

    public static final PriorityBlockingQueue<BlockRecord> BlockchainPriorityQueue = new PriorityBlockingQueue<BlockRecord>(100, BlockTimeStampComparator);
    // declare a final blocking priority queue that is concurrent

    public static void KeySend()
    {
        Socket socket;
        // declare a new socket
        PrintStream toServer;
        // declare a new Print Stream variable
        try
        {
            for(int i = 0; i < numProcesses; i++)
            {
                socket = new Socket(serverName, Ports.KeyServerPortBase + (i * 1000));
                // initialize a new socket for each incoming process taking in each's respectine port number
                toServer = new PrintStream(socket.getOutputStream());
                // initialize a new print stream taking in output stream from socket
                toServer.println("FakeKeyProcess" + BlockchainE.PID);
                // print out fake key process
                socket.close();
                // close off connections
            }
        } catch (IOException ioException)
        {
            ioException.printStackTrace();
            // print caught exceptions to the console
        }
    }

    public static void UnverifiedSend()
    {
        Socket UnverifiedBlockSocket;
        // declare a unbverified block socket to hold client connection to UVBServer for each process
        BlockRecord tempBlockRecord;
        // declare a BlockRecord variable

        String fakeBlockData;
        // declare a variable to hold a fake block
        String T_String;
        // temp variable to hold all of the timestamp strings dynamically created
        String TimeStampString;
        // declare a string for timestamp
        Date date;
        // dec;lare a new date var
        Random random = new Random();
        // declare and initialize new random

        try
        {
            Thread.sleep(1000);
            // give time for keys to be verified
        } catch (InterruptedException interruptedException)
        {
            interruptedException.printStackTrace();
            // print interruption exception to the console
        }


        try
        {
            for (int i = 0; i < 4; i++) {
                BlockRecord blockRecord = new BlockRecord();
                // declare nd initialize a new block record variable
                fakeBlockData = "(Block# " + Integer.toString(((BlockchainE.PID + 1) * 10) + i) + " from Process: " + BlockchainE.PID + ")";
                // fill our string with our fake block data and format it
                sendData(fakeBlockData);
                // following utility code, we will need to use our dynamically built chain with block recored below
                BlockRecord.setData(fakeBlockData);
                // set our block data
                date = new Date();
                // initialize the date
                T_String = String.format("%1$s %2$tF.%2$tT", "", date);
                // create our time stamp string
                TimeStampString = T_String + "." + i;
                // add our process number as an extension so timestamps dont collide
                System.out.println("Timestamp: " + TimeStampString);
                // print out time stamp string to console
                BlockRecord.setTimeStamp(TimeStampString);
                // set the timestamp in order to sort our priority queue for the blockrecord
                BlockRecord.setTimeStamp(TimeStampString);
                // set timestamp for our BlockchainD that has the fake block in there
                recordList.add(blockRecord);
            }
            Collections.shuffle(recordList);
            // shuffle our record list

            Iterator<BlockRecord> iterator = recordList.iterator();
            // declarea and initialize a new iterator for our blockrecord object

            while (iterator.hasNext()) {
                tempBlockRecord = iterator.next();
                // hold a block in our temp var as it iterates through our record
                System.out.println(tempBlockRecord.getTimeStamp() + " " + tempBlockRecord.getData());
                // print out our temp block records timestamp
            }
            System.out.println("");
            // this is our shuffled version abouve

            ObjectOutputStream toServerOutput = null;
            // declare and intitialize an output streamfro sending java objects over socket
            for (int i = 0; i < numProcesses; i++) {
                System.out.println("Sending Unverified Blocks to process " + i + "...");
                // print  out where we are sending the unverified block
                iterator = recordList.iterator();
                // iterate from beginning of record list
                while (iterator.hasNext()) {
                    UnverifiedBlockSocket = new Socket(serverName, Ports.UVBServerPortBase + (i * 1000));
                    // initialize our unverified block socket taking in localhost and the correct port number for the respective process
                    toServerOutput = new ObjectOutputStream(UnverifiedBlockSocket.getOutputStream());
                    // initialize  output to server that takes in the unverified bloc sockets output stream
                    Thread.sleep((random.nextInt((9) * 100)));
                    // have our processes sleep for a random amount of time up to a second when sending
                    tempBlockRecord = iterator.next();
                    // hold our next block record in temp variable
                    System.out.println("Unverified Block tempBlockRecord for Process " + i + ": " + tempBlockRecord.getTimeStamp() + " " + tempBlockRecord.getData());
                    // print out the unverified block for each respective process to the console
                    toServerOutput.writeObject(tempBlockRecord);
                    // send UVB object
                    toServerOutput.flush();
                    // flush output stream
                    UnverifiedBlockSocket.close();
                    // close the connection
                }
            }
        } catch (Exception e)
        {
            e.printStackTrace();
            // print caught exception to console
        }
    }



    /*
     * public constructor for Blockchain_C
     * @param data var of type String
     * @param previousHash var of type String
     * @param timeStamp variable of type long
     */
    public BlockchainE(String data, String previousHash, long timeStamp)
    {
        this.data = data;
        this.previousHash = previousHash;
        this.timeStamp = timeStamp;
        // getters and setters
        this.hash = calculateBlockHash();
        // assigns hash to itself
    }


    public String calculateBlockHash()
    // method to calculate hash for current block
    {
        String dataToHash = previousHash + Long.toString(timeStamp) + Integer.toString(nonce) + data;
        // concatenation of hash of the previous tx ,time of tx, the tx nonce, ans the tx data
        MessageDigest digest = null;
        // declare new message digest objecgt and isntatntiate to null
        byte[] bytes = null;
        // declare and initialize a new byte array

        try
        {
            digest = MessageDigest.getInstance("SHA-256");
            // get an instance of the SHA256 hashing algorithm and store it in digest
            bytes = digest.digest(dataToHash.getBytes("UTF-8"));
            // generate the hash value of our input data and stick in in our new byte array
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException exception)
        {
            System.err.println("Exception found: " + exception);
            exception.printStackTrace();
            // print exceptions to console
        }

        StringBuffer buffer = new StringBuffer();
        // declare and initialize anew string buffer
        for (byte b: bytes)
        // cycle through all bytes in bytes
        {
            buffer.append(String.format("%02x", b));
            // turn said byte into a hex string
        }
        return buffer.toString();
        // return our string buffer that now holds our hash
    }


    /*
     * method for mining a new block
     * @param a prefix var of type integer
     *
     * please note that this implementation does not verifying any date which
     * is a crucial component of blockchains with real-world application
     */
    public String mineBlock(int prefix)
    {
        String prefixString = new String(new char[prefix]).replace('\0', '0');
        /*
         * declare and intialialize our prefix string to a new string containing our prefix integer with '\0' replaced
         * by '0' to represent the prefix we are looking for
         */

        while (!hash.substring(0, prefix).equals(prefixString))
        // while we do not have our desired solution
        {
            nonce++;
            // increment our nonce
            hash = calculateBlockHash();
            // and calculate the hash
        }
        return hash;
        // return our winning hash w=once we find our desired prefixString
    }

    public String getHash()
    {
        return this.hash;
        // getter to return hash
    }

    public String getPreviousHash()
    {
        return this.previousHash;
        // getter to return previous hash
    }

    public String getData()
    {
        return this.data;
    }

    public static void sendData(String data)
    {
        BlockchainE.data = data;
        // method to send data to the block
    }

    public String getTimeStamp()
    {
        return TimeStamp;
    }

    public void setTimeStamp(String _timeStamp)
    {
        this.TimeStamp = _timeStamp;
    }




    public static void writeToJSON()
    {
        System.out.println("\n___________In writeToJSON___________\n");
        // console header to inform the user whats executing
        UUID b_UUID = UUID.randomUUID();
        String s_uuid = b_UUID.toString();
        // declare and initialize a new random uuid and save it as a string
        System.out.println("Unique Block ID: " + s_uuid + "\n");
        // print out uuid to console

        BlockRecord blockRecord = new BlockRecord();
        // declare and initialize a new blockRecord object
        blockRecord.setVerificationProcessID("Process 2");
        // set the process ID to 2
        blockRecord.setBlockID(s_uuid);
        // set the uuid string
        blockRecord.setUUID(b_UUID);
        // set the binary uuid
        blockRecord.setData("This is the data contained within this transaction block");
        // add in some arbitrary data

        Random rand = new Random();
        // declare3 and initialize a new random variable
        int randVal = rand.nextInt(12777215);
        // the declared bound is a 0xFFFFFF mask, Elliott wants us to pick anew range so play around with this

        String randomSeed = String.format("%06X", randVal & 0x00FFFFF);
        // Masking off ll but the trailing 12 characters
        randVal = rand.nextInt(14333409);
        // this bound is meaningless, I made it up
        String randomSeed2 = Integer.toHexString(randVal);
        // second random seed string
        System.out.println("Our random seed is: " + randomSeed + "... or was it: " + randomSeed2 + "?...\n");
        // print out our two random seeds to confuse the user, we are using randomSeed2

        blockRecord.setRandomSeed(randomSeed2);
        // set the correct random seed in our blockRecord object

        String newBlockRecord = blockRecord.getBlockID() + blockRecord.getVerificationProcessID() +
                blockRecord.getPreviousHash() + blockRecord.getData() + blockRecord.getRandomSeed() +
                blockRecord.getWinningHash();
        // fill a new string up with our block data

        System.out.println("blockRecord is: " + newBlockRecord + "\n");
        // tell the console what the new block record is

        String SHA256string = "";
        // declare new string variable to hold the string version of our SHA256 hash and initialize to empty;

        try {
            MessageDigest ourMD = MessageDigest.getInstance("SHA-256");
            // get and instance of our hashing algorithm from message digest
            ourMD.update(newBlockRecord.getBytes());
            // load up the bytes of our new block record
            byte[] byteArr = ourMD.digest();
            // turn our record into a byte array

            StringBuffer stringBuf = new StringBuffer();
            for (int i = 0; i < byteArr.length; i++)
            {
                stringBuf.append(Integer.toString((byteArr[i] & 0xFF) + 0x100, 16).substring(1));
                // cycle through all bytes in our byte array and add the hexidecimal verion to our string buffer
            }

            SHA256string = stringBuf.toString();
            // more human readable this way
        } catch (NoSuchAlgorithmException noAlgEx)
        {
            System.out.println("No Algorithm exception caught: " + noAlgEx + "\n");
            noAlgEx.printStackTrace();
            // print our exceptions to the console ot be handled
        }

        blockRecord.setWinningHash(SHA256string);
        // we just let the first hash win, try to implement some real work to see how this may work

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        // declare and initialize new gson builder to contain our java object
        String json = gson.toJson(blockRecord);

        System.out.println("\nJSON blockRecord: " + json + "\n");
        // print our json string to console

        try(FileWriter writer = new FileWriter("blockRecord.json"))
        {
            gson.toJson(blockRecord, writer);
        } catch (IOException exception)
        {
            System.out.println("Caught IO exception: " + exception + "\n");
            exception.printStackTrace();
            // print exception to console to be handled
        }
    }

    public static void readFromJSON()
    {
        System.out.println("\n_______________In readFromJSON_______________");
        // create a header to indicate to the console what section is executing
        Gson gson = new Gson();
        // declare and initialize a new gson object and store it in var gson
        try (Reader reader = new FileReader("blockRecord.json"))
        {
            BlockRecord blockRecord_Read = gson.fromJson(reader, BlockRecord.class);
            // reads JSON into variable blockRecord_Read object of type BlockRecord
            System.out.println(blockRecord_Read);
            // print out our newly marshalled java object
            System.out.println("Top Secret Data Contained: " + blockRecord_Read.Data + "\n");
            // print the data contained in the read JSON block
            String uuid_read = blockRecord_Read.uuid.toString();
            // declare and initialize var uuid_read to hold the uuid read from the JSON string
            System.out.println("Stirng UUID: " + blockRecord_Read.BlockID + " \nBinary UUID: " + uuid_read + "\n");
            // print our uuid to console in both string format and binary
        } catch (IOException exception)
        {
            System.out.println("Caught an IOException trying to sneak by: " + exception + "\n");
            exception.printStackTrace();
            // print out the caught exceptions to the console to be handled
        }
    }

    public static void demonstrateUtils(String a[]) throws Exception
    {
        System.out.println("\n_____________In demonstrateUtils______________");
        // crate a header so we know what is happening in the console
        int processNum;
        // declare variable hold port number
        int unverifiedBlock_portNum;
        // declare a variable to hold the unverified block's port numbner
        int blockchain_portNum;
        // declare a variable to hold the verified blockchain port number

        /*
            //lets figure out how to implement something extra for some bragging rights

            if (a.length > 2)
            {
                System.out.println("This is my bragging rights code");
            }

            // code and explanation of bragging rights would go hewre
         */

        if (a.length < 1)
        // sets the process id to zero from command line argument if no arguments are given
        {
            processNum = 0;
        }
        else if (a[0].equals("0"))
        // sets process number to 0 according to the command line argument
        {
            processNum = 0;
        }
        else if (a[0].equals("1"))
        // sets process number to 1 according to the command line argument
        {
            processNum = 1;
        }
        else if (a[0].equals("2"))
        // sets process number to 2 according to the command line argument
        {
            processNum = 2;
        }
        else
            // sets process number to 0 by default if there is an invalid command limne argument
        {
            processNum = 0;
        }



        unverifiedBlock_portNum = 4710 +processNum;
        // sets unverified block port number according to its process number
        blockchain_portNum = 4810 + processNum;
        // sets verified blockchain port number according to its process number

        System.out.println("Process number: " + processNum + " Ports: " + unverifiedBlock_portNum + " " + blockchain_portNum + "\n");
        // print out the process number and port port nums being used to the console

        Date date = new Date();
        // declare new date variable to contain in block
        String time = String.format("%1$s %2$tF.%2$tT", "", date);
        // format our date into a string
        String timeStamp = time + "." + processNum + "\n";
        // use our time string and concatenate with the process number that is being run
        System.out.println("Timestamp: " + timeStamp);
        // print out to console

        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        // new message digest to get hashing algorithm
        messageDigest.update(fakeBlock.getBytes());
        // update our block
        byte[] byteArr = messageDigest.digest();
        // make a new byte array to hold our bytes

        StringBuffer stringBuffer = new StringBuffer();
        // declare new string buffer to convert our bytes into hex
        for (int i = 0; i < byteArr.length; i++)
        {
            stringBuffer.append(Integer.toString((byteArr[i] & 0xFF) + 0x100, 16).substring(1));
            // conversion to hexadecimal
        }

        String SHA256string = stringBuffer.toString();
        // declare a variable and initialize to hold the string version of our hash

        KeyPair keyPair = generateKeyPair(777);
        // not secure for real world use, need to use a random string

        byte[] sig = signData(SHA256string.getBytes(), keyPair.getPrivate());
        // create a new byte array to hold our signers digital signature

        boolean isVerified = verifySig(SHA256string.getBytes(), keyPair.getPublic(), sig);
        // run the verifySig method to verify the digital signature and store it in the boolean variable isVerified

        System.out.println("Hexadecimal byte[] representation of SHA256 hash: " + SHA256string + "\n");
        // print out the hex hash to the console

        /*
            this SHA256 string will need to be added to the header of our block

            below, we can see how to turn this string back into a byte array
         */

        hash = Base64.getEncoder().encodeToString(sig);
        // encodes the digital signature into string format
        System.out.println("The signed SHA256 hash string: " + hash + "\n");
        // print out the current block's hash to the console

        byte[] testSigArr = Base64.getDecoder().decode(hash);
        // make a byte array to hold the bytes to test turning our hash back into a byte array
        System.out.println("Testing signature restoration to byte[]: " + Arrays.equals(testSigArr, sig));
        // checks to see if our decoded sig matches our sig and prints the boolean

        isVerified = verifySig(SHA256string.getBytes(), keyPair.getPublic(), testSigArr);
        // replaces the bool we declared earlier to tell us if our block is verified or not

        System.out.println("Has the restored digital signature been verified: " + isVerified + "\n");
        // print out if the sig has been verified to the console

        /*
            below proves that if our key is tampered with, it will
            not return as verified
         */

        byte[] bytePublicKey = keyPair.getPublic().getEncoded();
        // saves the public key as a byte array in our var bytePublickey
        System.out.println("Key in byte array format: " + bytePublicKey);
        // print out byte array pub key to console

        String keyString = Base64.getEncoder().encodeToString(bytePublicKey);
        // save the public key from byte array to string
        System.out.println("Key in string format: " + keyString);
        // print out the key to the console in string format

        String keyStringBad = keyString.substring(0,50) + "M" + keyString.substring(51);
        // make arbitrary changes to our key to prove validity test
        System.out.println("\nBad key in string format: " + keyStringBad);
        // print out the unverified key to console

        byte[] bytePublicKey_2 = Base64.getDecoder().decode(keyString);
        // turn it back into a byte array
        System.out.println("Key in byte array format again: " + bytePublicKey_2);
        // print out the key to console in byte form once again

        X509EncodedKeySpec publicSpec = new X509EncodedKeySpec(bytePublicKey_2);
        KeyFactory keyFactory =  KeyFactory.getInstance("RSA");
        // declare and initialize a new key factory variable for an RSA key
        PublicKey restoredKey = keyFactory.generatePublic(publicSpec);
        // get a public key and store it in restoredKey var

        isVerified = verifySig(SHA256string.getBytes(), keyPair.getPublic(), testSigArr);
        // store bool value in our isVerified boolean
        System.out.println("Has signature been verified: " + isVerified + "\n");
        // print out if the signature is verified or not

        isVerified = verifySig(SHA256string.getBytes(), restoredKey, testSigArr);
        // store bool value for our restored key signature
        System.out.println("Has CONVERTED-FROM-STRING signature been verified: " + isVerified + "\n");
        // print out if the restored signature is verified

        X509EncodedKeySpec publicSpecBad = new X509EncodedKeySpec(bytePublicKey_2);
        KeyFactory keyFactoryBad = KeyFactory.getInstance("RSA");
        // declare and initialize a new bad key factory variable for an RSA key
        PublicKey restoredBadKey = keyFactoryBad.generatePublic(publicSpecBad);
        // get a public key version of the bad key to store in our var

        isVerified = verifySig(SHA256string.getBytes(), restoredBadKey, testSigArr);
        // store bool value in isVerified again
        System.out.println("Has CONVERTED-FROM-STRING signature been verified: " + isVerified + "\n");
        // print out if the restored signature is verified

        /*
            below is Elliott's guide on how to simulate work
         */

        System.out.println("Now simulating work: ");
        // print to the console to show the beginning of *work* simulation

        int rVal = 77;
        // new arbitrary value
        int tenths = 0;
        // declare and intitialize tenth var to 0
        Random random = new Random();
        // declare and initialize a new random variable

        for (int i = 0; i < 1000; i++)
        // our safe upper limit is 1000
        {
            Thread.sleep(100);
            // our fake work
            rVal = random.nextInt(100);
            // higher the bound means more work
            System.out.println(".");

            if (rVal < 10)
            // the lower the threshold means more work in this case
            {
                tenths = i;
                break;
            }
        }
        System.out.println(" <-- we did " + tenths + " tenths of a second of *work*\n");
        // print how long it took us to solve the fake work to the console
    }

    public static boolean verifySig(byte[] _data, PublicKey _key, byte[] _sig) throws Exception
    {
        Signature signer = Signature.getInstance("SHA1withRSA");
        // declare and initialize signer variable of type Signature that holds an instance of our encryption type
        signer.initVerify(_key);
        // verify the key being passed as argument
        signer.update(_data);
        // update with new data being passed as argument

        return (signer.verify(_sig));
        // return if it is verified
    }

    public static KeyPair generateKeyPair(long _seed) throws Exception
    {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        // declare and initialize a new key pair generator of type RSA
        SecureRandom rng = SecureRandom.getInstance("SHA1PRNG", "SUN");
        // declare and initialize a new secure random number generator
        rng.setSeed(_seed);
        // set our seed
        keyPairGenerator.initialize(1024, rng);

        return (keyPairGenerator.generateKeyPair());
        // return our new key pair
    }

    public static byte[] signData(byte[] _data, PrivateKey _key) throws Exception
    {
        Signature signer = Signature.getInstance("SHA1withRSA");
        // get a new instance of signer
        signer.initSign(_key);
        // sign with argument key
        signer.update(_data);
        // update with argument data

        return (signer.sign());
    }


    public static void main(String a[])
    {
        List<BlockchainE> blockchain = new ArrayList<>();
        // declare and initialize our new blockchain
        int prefix = 4;
        // declare and initialize our prefix value to 4 leading zeroes
        String prefixString = new String(new char[prefix]).replace('\0', '0');
        // declare and initialize our prefixString for this instance

        BlockchainE genesisBlock = new BlockchainE("This is the genesis Dummy Block.", "0", new Date().getTime());
        // declare and initialize a new genesis block to be our Dummy Block
        genesisBlock.mineBlock(prefix);
        // mine our Dummy Block
        blockchain.add(genesisBlock);
        // add it to our blockchain

        BlockchainE firstBlock = new BlockchainE("This is the first Simple Block.", genesisBlock.getHash(), new Date().getTime());
        // declare and initialize our first Simple Block
        firstBlock.mineBlock(prefix);
        // mine our first Simple block
        blockchain.add(firstBlock);
        // add it to our blockchain

        BlockchainE secondBlock = new BlockchainE("This is the second Simple Block.", firstBlock.getHash(), new Date().getTime());
        // declare and initialize our second Simple Block
        secondBlock.mineBlock(prefix);
        // mine our second Simple block
        blockchain.add(secondBlock);
        // add it to our blockchain

        BlockchainE thirdBlock = new BlockchainE("This is the third Simple Block.", secondBlock.getHash(), new Date().getTime());
        // declare and initialize our third Simple Block
        thirdBlock.mineBlock(prefix);
        // mine our third Simple block
        blockchain.add(thirdBlock);
        // add it to our blockchain

        BlockchainE fourthBlock = new BlockchainE("This is the fourth Simple Block.", blockchain.get(blockchain.size() - 1).getHash(), new Date().getTime());
        // declare and initialize our fourth Simple Block
        fourthBlock.mineBlock(prefix);
        // mine our fourth Simple block
        blockchain.add(fourthBlock);
        // add it to our blockchain


        boolean flag = true;
        // declare and initialize our boolean flag var to true
        String tempData = null;
        // declare adn initialize a tempData var to null
        String tempCurrentHash = null;
        // declare and initialize a tempCurrentHash var to null

        for (int i = 0; i < blockchain.size(); i++)
        // cycle through the size of the chain
        {
            String previousHash = i==0 ? "0" : blockchain.get(i - 1).getHash();
            flag = blockchain.get(i).getHash().equals(blockchain.get(i).calculateBlockHash()) &&
                    previousHash.equals((blockchain.get(i).getPreviousHash())) && blockchain.get(i).getHash().substring(0, prefix).equals(prefixString);
            /*
             * set flag equal to the boolean value if the stored hash for the current block is calculated and stored correctly
             * and if the previous block stored in the current block is actually the hash of the previous block.
             * and if the current block has been mined
             */

            tempCurrentHash = blockchain.get(i).getHash();
            // save current hash to print out
            tempData = blockchain.get(i).getData();
            // save data to tempData to print out

            if (!flag)
            {
                break;
            }
        }
        System.out.println("\nFlag: " + flag + "\nBlock: " + tempCurrentHash + "\nContent: " + tempData + "\nGood Job!\n");
        // print out the results to the console


        /*
            below is what is necessary for implementing Elliott's requirements:
                demonstrateUtil checks the command line argument for the process ID and assigns the blochain
                a port number depending on the process ID and if the blockchain is verified or unverified

                writeToJSOn does exactly that

                readFromJSON does exactly that
         */

        try
        {
            demonstrateUtils(a);
        } catch (Exception e)
        {
            e.printStackTrace();
        }

        writeToJSON();
        // write our output to JSON file
        readFromJSON();
        // read our input from a JSON file

        System.out.println("Running now\n");
        // print to the console that main is running
        //int q_len = 6;
        // num of opsys requests
        PID = (a.length < 1) ? 0 : Integer.parseInt(a[0]);
        // to determine process ID
        System.out.println("Bryce Jensen's Block Coordinating Framework for Clark Elliott's CSC435 . Stop process with ctrl+c");
        // inform the console what is runing
        System.out.println("Using process ID: " + PID + "\n");
        // print out the process number coming through
        new Ports().setPorts();
        // determine port number depending on process id

        new Thread(new PublicKeyServer()).start();
        // initiate a new thread for processing publick keys
        new Thread(new UVBServer(BlockchainPriorityQueue)).start();
        // start an new thread to process unverified blocks
        new Thread(new BlockchainServer()).start();
        // start a new thread for incoming blocks
        try
        {
            Thread.sleep(1000);
            // give servers some time to work
        } catch (Exception exception)
        {
            exception.printStackTrace();
            // print any caught exceptionsto the console
        }

        BlockchainE.KeySend();
        // send the keys

        try
        {
            Thread.sleep(1000);
        } catch (Exception exception)
        {
            exception.printStackTrace();
            // print any caught exceptionsto the console
        }

        BlockchainE.UnverifiedSend();
        // attempt to multicast some unverified blocks to all server processes

        try
        {
            Thread.sleep(1000);
            // wait for multicast
        } catch (Exception exception)
        {
            exception.printStackTrace();
            // print any caught exceptionsto the console
        }

        new Thread(new UVBConsumer(BlockchainPriorityQueue)).start();
        // begin handling queued up unverified blocks
    }
}




