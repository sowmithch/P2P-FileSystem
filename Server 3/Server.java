import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.lang.*;
import java.io.*;
import java.net.DatagramPacket;
import java.net.InetAddress;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.catalog.Catalog;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.security.spec.KeySpec;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
@SuppressWarnings("unchecked")
public class Server{
    static List<Integer> replicaSendPortsList = new ArrayList<Integer>();
    static List<Integer> replicaReceivePortsList = new ArrayList<Integer>();
    static List<Integer> replicaPortsList = new ArrayList<Integer>();
    static List<Integer> serverPortsList = new ArrayList<Integer>();
    static List<String> pubKeys = new ArrayList<String>();
    static Map<String,List<String>> readPermissions; 
    static Map<String,List<String>> writePermissions; 
    static Map<String,List<String>> ownerPermissions; 
    static List<String> filesAvailable;
    static Map<String,String> users;
    static Map<String,Integer> fileLocks = new HashMap<>();
    static DatagramSocket serverSocket,replicaSocket,replicaSendSocket,replicaReceiveSocket;
     static int id;
    static InetAddress ip;
    static String key = "absdhrightyghtfd";
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES";
    private static final String SECRET_KEY = "my_super_secret_key_ho_ho_ho";
    
    private static final String SALT = "ssshhhhhhhhhhh!!!!";
    static String loginUser;
    public static void main(String args[]) throws FileNotFoundException,IOException{
        id = Integer.valueOf(args[0]);
        ip = InetAddress.getLocalHost();
        File ports = new File("./meta/ports.txt");
        Scanner scnr = new Scanner(ports);
        while(scnr.hasNextLine()){
            String line = scnr.nextLine();
            replicaSendPortsList.add(Integer.valueOf(line.split(":")[1]));
            replicaReceivePortsList.add(Integer.valueOf(line.split(":")[2]));
            serverPortsList.add(Integer.valueOf(line.split(":")[3]));
            replicaPortsList.add(Integer.valueOf(line.split(":")[4]));
        }
        serverSocket = new DatagramSocket(serverPortsList.get(id));
        replicaSendSocket = new DatagramSocket(replicaSendPortsList.get(id));
        replicaReceiveSocket = new DatagramSocket(replicaReceivePortsList.get(id));
        replicaSocket = new DatagramSocket(replicaPortsList.get(id));
        //firstTimeInitializeMetaData();
        
        InitializeMetaData();
        benchmarkTest();
        System.out.println("1)User Login\n2)Register User");
        Scanner sc = new Scanner(System.in);
        int choice = sc.nextInt();
        if(choice == 1){
            System.out.println("Enter User Name");
            String userName = sc.next();
            System.out.println("Enter Password");
            String password = sc.next();
            String message = "login "+userName+" "+password;
            server(message);
        }
        else if(choice == 2){
            System.out.println("Enter User Name");
            String userName = sc.next();
            System.out.println("Enter Password");
            String password = sc.next();
            String message = "addUser "+userName+" "+password;
            server(message);
        }
        Thread t2 = new Thread(new Runnable(){public void run(){replica();}});
        t2.start();
        
        Thread validationThread = new Thread(new Runnable(){public void run(){validation();}});
        validationThread.start();

        serverStarter();
    }
    public static void validation(){
        while(true){
            try{
            Thread.sleep(10000);
            validateFileSystem();
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }
    }
    public static void InitializeMetaData(){
        File readPermFile = new File("./meta/readPermissions.txt");  
            readPermissions = (Map<String,List<String>>)deserializeObject(readPermFile.getPath());         
        File writePermFile = new File("./meta/writePermissions.txt");
            writePermissions = (Map<String,List<String>>)deserializeObject(writePermFile.getPath());         
        File ownerPermFile = new File("./meta/ownerPermissions.txt");
            ownerPermissions = (Map<String,List<String>>)deserializeObject(ownerPermFile.getPath());         
        File usersFile = new File("./meta/users.txt");
            users = (Map<String,String>)deserializeObject(usersFile.getPath()); 

        String path = "./Filesystem";
        
        filesAvailable= listAllFiles(path);
        for(int i=0;i<filesAvailable.size();i++){
            fileLocks.put(filesAvailable.get(i), 0);
        }
        
    }
    public static void firstTimeInitializeMetaData(){
            readPermissions = new HashMap();
            serializeObject(readPermissions, "./meta/readPermissions.txt");
                                
                   
            writePermissions = new HashMap();       
            serializeObject(writePermissions, "./meta/writePermissions.txt");
                                
            ownerPermissions = new HashMap();     
            serializeObject(ownerPermissions, "./meta/ownerPermissions.txt");
                                 
            users = new HashMap();
            serializeObject(users, "./meta/users.txt");
        
        
    }
    public static List<String> listAllFiles(String path){
        File dir = new File(path);
        List<String> files = new LinkedList<>();
        for(int i=0;i<dir.list().length;i++)
        {
            File f = dir.listFiles()[i];
            if(f.isDirectory()){
                listAllFiles(path+"/"+f.getName());
            }
            else{
                files.add(path+"/"+f.getName());
            }
        }
        return files;
    }
    public static void validateFileSystem(){
        try{
            List<String> listOfallfiles = listAllFiles("./Filesystem");
            for(int i=0;i<listOfallfiles.size();i++){
                if(!filesAvailable.contains(listOfallfiles.get(i))){
                    File file = new File(listOfallfiles.get(i));
                    System.out.println(listOfallfiles.get(i)+" is created maliciously");
                    file.delete();
                }
            }
        for(int i=0;i<replicaSendPortsList.size();i++){
            if(i==id)
                continue;
            for(int j=0;j<filesAvailable.size();j++){     
                String inp = "search "+"server "+filesAvailable.get(j)+" "+id;
                byte buf[] = null;
                buf = encrypt(inp).getBytes();
                DatagramPacket DpSend =
                    new DatagramPacket(buf, buf.length, ip, replicaReceivePortsList.get(i));
                replicaSendSocket.send(DpSend);
            }
            
        }
    }
    catch(Exception e){
        e.printStackTrace();
    }
    }
    public static void serializeObject(Object object,String fileName){
        try{
        FileOutputStream file = new FileOutputStream(fileName);
        ObjectOutputStream out = new ObjectOutputStream(file);
        out.writeObject(object);    
        out.close();
        file.close();
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }
    public static Object deserializeObject(String fileName){
        Object object = null;
        try{
        FileInputStream file = new FileInputStream(fileName);
        ObjectInputStream in = new ObjectInputStream(file);
        object = in.readObject();
        in.close();
        file.close();
        return object;
        }
        catch(Exception e){
            e.printStackTrace();
        }
        return object;
    } 
    public static String queryReplicasForFile(String fileName){
        try{
        List<Integer> ids = sendSearch(fileName);
        if(ids.size()>0){
            List<String> fileData = sendRead(fileName, ids.get(0));
            String location = "./tmp/"+System.currentTimeMillis();
            File tempFile = new File(location);
            if(!tempFile.exists())
                tempFile.createNewFile();
            BufferedWriter out = new BufferedWriter(new FileWriter(tempFile, true));
            for(int i=0;i<fileData.size();i++){
                out.write(fileData.get(i));
            }
            out.close();
            return location;
        }
        return null;
    }
    catch(Exception e){
        e.printStackTrace();
    }
    return null;
    }

    public static void benchmarkTest(){
        long time = System.nanoTime();
        for(int i=0;i<100000;i++){
            server("read jai jai.txt");
        }
        long elapsedtime = System.nanoTime() - time;
        System.out.println(elapsedtime+" nano seconds");
    }

    public static void serverStarter(){
        while(true){
        
            System.out.println("1)List all files\n2)Create File\n3)Read File\n4)Write File\n5)Delete File\n6)Add User\n7)Grant permissions\n8)Revoke permissions\n9)create directory\n10)Delete directory");
            Scanner sc = new Scanner(System.in);
            int choice = sc.nextInt();
            String message = "";
            switch(choice){
                case 1:
                    message = "list "+loginUser+" allfiles";
                    server(message);
                    break;
                case 2:
                    System.out.println("Enter the File Name");
                    String fileName = sc.next();
                    message = "create "+loginUser+" "+fileName;
                    server(message);
                    
                    break;
                case 3:
                    System.out.println("Enter the File Name");
                    fileName = sc.next();
                    message = "read "+loginUser+" "+fileName;
                    server(message);
                    break;
                case 4:
                    System.out.println("Enter the File Name");
                    fileName = sc.next();
                    message = "write "+loginUser+" "+fileName;
                    server(message);
                    break;
                case 5:
                        System.out.println("Enter the File Name");
                        fileName = sc.next();
                        message = "delete "+loginUser+" "+fileName;
                        server(message);
                        break;
                case 6:
                        System.out.println("Enter the user Name");
                        String addUser = sc.next();
                        System.out.println("Enter the password");
                        String userPassword = sc.next();
                        message = "addUser "+addUser+" "+userPassword;
                        server(message);
                        break;
                case 7:
                        System.out.println("Enter the file Name");
                        fileName = sc.next();
                        System.out.println("Enter the user Name");
                        addUser = sc.next();
                        System.out.println("Enter the permission(owner-read-write)");
                        String Permission = sc.next();
                        message = "grantPermission "+loginUser+" "+fileName+" "+addUser+" "+Permission;
                        server(message);
                        break;
                case 8:
                        System.out.println("Enter the file Name");
                        fileName = sc.next();
                        System.out.println("Enter the user Name");
                        addUser = sc.next();
                        System.out.println("Enter the permission to revoke (owner-read-write)");
                        Permission = sc.next();
                        message = "revokePermission "+loginUser+" "+fileName+" "+addUser+" "+Permission;
                        server(message);
                        break;
                case 9:
                        System.out.println("Enter the directory name");
                        String dirName = sc.next();
                        message = "mkdir "+loginUser+" "+dirName;
                        server(message);

                        break;
                case 10:
                        System.out.println("Enter the directory name");
                        dirName = sc.next();
                        message = "rmdir "+loginUser+" "+dirName;
                        server(message);
                        break;
            }
        }
    }
    public static void server(String receivedData){
        try{ 
            
                String operation = receivedData.split(" ")[0];
                String user = receivedData.split(" ")[1];
                String fileName = "./Filesystem/"+receivedData.split(" ")[2];
                String message = "";
                switch(operation){
                    case "read":
                            try{
                            if(filesAvailable.contains(fileName)){
                                if(readPermissions.get(fileName).contains(loginUser)){
                                    File file = new File(fileName);
                                    if(file.exists()){
                                        String tmpFilePath = readFrom(fileName);
                                        File tmpFile = new File(tmpFilePath);
                                        Scanner myReader = new Scanner(tmpFile);
                                        while(myReader.hasNextLine()){
                                            System.out.println(myReader.nextLine());   
                                        }
                                        
                                        myReader.close();
                                        tmpFile.delete();
                                        
                                    }
                                }
                                else{
                                    System.out.println("Failed you dont have permission");
                                }

                            }
                            else{
                                System.out.println("Failed file not available");
                                
                            }
                        }
                        catch(Exception e){
                            System.out.println("Error while accessing the file");
                        }
                            break;
                    case "write": 
                            try{
                            if(filesAvailable.contains(fileName)){
                                if(fileLocks.get(fileName) == 0){
                                    fileLocks.put(fileName,1);
                                File file = new File(fileName);
                                if(writePermissions.get(fileName).contains(loginUser)){
                                    
                                    byte[] replicabuf;
                                    DatagramPacket ReplicaDpSend;
                                    for(int i=0;i<replicaReceivePortsList.size();i++){
                                        if(id == i)
                                        continue;
                                        replicabuf = encrypt(receivedData).getBytes();
                                        ReplicaDpSend =
                                            new DatagramPacket(replicabuf, replicabuf.length, ip, replicaReceivePortsList.get(i));
                                            serverSocket.send(ReplicaDpSend); 
                                    }
                                    System.out.println("Enter the data to write to file");
                                    while (true)
                                    {
                                        Scanner sc = new Scanner(System.in);
                                        String writeData = sc.nextLine();
                                        if(writeData.startsWith("Done")){
                                            fileLocks.put(fileName,0);
                                            for(int i=0;i<replicaReceivePortsList.size();i++){
                                                if(id == i)
                                                continue;
                                            replicabuf = encrypt("Done").getBytes();
                                            ReplicaDpSend =
                                                new DatagramPacket(replicabuf, replicabuf.length, ip, replicaPortsList.get(i));
                                                serverSocket.send(ReplicaDpSend); 
                                            }
                                            break;
                                        }
                                        writeTo(fileName,writeData);
                                        for(int i=0;i<replicaReceivePortsList.size();i++){
                                            if(id == i)
                                            continue;
                                            replicabuf = encrypt(writeData).getBytes();
                                            ReplicaDpSend =
                                                new DatagramPacket(replicabuf, replicabuf.length, ip, replicaPortsList.get(i));
                                                serverSocket.send(ReplicaDpSend); 

                                        }
                                    }
                                }
                                else{
                                    System.out.println("Failed you dont have permission");
                                    
                                }
                                }
                                else{
                                    System.out.println("File already in use");
                                }
                            }
                            else{
                                System.out.println("Failed file not available");
                                
                            }
                        }
                        catch(Exception e){
                            System.out.println("Error while writing to file");
                                
                        }
                            break;
                    case "login":
                            try{
                            String password = receivedData.split(" ")[2];
                            if(users.containsKey(user) && users.get(user).equals(password)){
                                loginUser = user;
                                System.out.println("login success");
                            }
                            else{
                                System.out.println("login failed");
                                System.exit(1);
                            }
                            
                            }
                            catch(Exception e){
                                System.out.println("Error");
                                   
                            }
                            break;
                    case "addUser":
                                try{
                                 String password = receivedData.split(" ")[2];
                                if(!users.containsKey(user)){
                                users.put(user, password);
                                for(int i=0;i<replicaReceivePortsList.size();i++){
                                    if(id == i)
                                        continue;
                                    byte[] replicabuf = encrypt(receivedData).getBytes();
                                    DatagramPacket ReplicaDpSend =
                                        new DatagramPacket(replicabuf, replicabuf.length, ip, replicaReceivePortsList.get(i));
                                        serverSocket.send(ReplicaDpSend); 
                                }
                                serializeObject(users, "./meta/users.txt");
                                System.out.println ("success user added");
                                }
                                else{
                                    System.out.println("user already exists");
                                }
                                }
                                catch(Exception e){
                                    System.out.println("Error");
                                    
                                }
                            break;
                    case "create":
                                try{
                                File file = new File(fileName);
                                if(!file.exists()){
                                    file.createNewFile();
                                    List<String> userList = new ArrayList<>();
                                    userList.add(loginUser);
                                    ownerPermissions.put(fileName, userList);
                                    readPermissions.put(fileName, userList);
                                    writePermissions.put(fileName, userList);
                                    serializeObject(readPermissions, "./meta/readPermissions.txt");
                                    serializeObject(writePermissions, "./meta/writePermissions.txt");
                                    serializeObject(ownerPermissions, "./meta/ownerPermissions.txt");
                                    filesAvailable.add(fileName);
                                    for(int i=0;i<replicaReceivePortsList.size();i++){
                                        if(id == i)
                                            continue;
                                        byte[] replicabuf = encrypt(receivedData).getBytes();
                                        DatagramPacket ReplicaDpSend =
                                            new DatagramPacket(replicabuf, replicabuf.length, ip, replicaReceivePortsList.get(i));
                                            serverSocket.send(ReplicaDpSend); 
                                    }
                                    System.out.println("success file created");
                                   
                                }
                                else{
                                    System.out.println("File already exists");
                                }
                                
                            }
                            catch(Exception e){
                                System.out.println("Error");
                            }
                            break;
                    case "delete":
                                try{
                                File file = new File(fileName);
                                if(file.exists()){
                                    if(ownerPermissions.get(fileName).contains(user)){
                                    file.delete();
                                    for(int i=0;i<replicaReceivePortsList.size();i++){
                                        if(id == i)
                                            continue;
                                        byte[] replicabuf = encrypt(receivedData).getBytes();
                                        DatagramPacket ReplicaDpSend =
                                            new DatagramPacket(replicabuf, replicabuf.length, ip, replicaReceivePortsList.get(i));
                                            serverSocket.send(ReplicaDpSend); 
                                    }
                                readPermissions.remove(fileName);
                                serializeObject(readPermissions, "./meta/readPermissions.txt");
                                writePermissions.remove(fileName);
                                serializeObject(writePermissions, "./meta/writePermissions.txt");
                                ownerPermissions.remove(fileName);
                                serializeObject(ownerPermissions, "./meta/ownerPermissions.txt");
                                filesAvailable.remove(fileName);
                                System.out.println("File deleted");

                                }
                                else{
                                    System.out.println("Failed you dont have permission to delete the file");
                                }
                                }
                                else{
                                    System.out.println("Failed File doesn't exist");
                                }
                                
                                }
                                catch(Exception e){
                                    System.out.println("Error File not deleted");
                                    
                                }
                            break;
                    case "grantPermission":
                                        try{
                                        System.out.println(fileName);
                                        if(ownerPermissions.get(fileName).contains(user)){
                                            String userName = receivedData.split(" ")[3];
                                            String permission = receivedData.split(" ")[4];
                                            if(permission.charAt(0)=='1'){
                                                ownerPermissions.get(fileName).add(userName);
                                                readPermissions.get(fileName).add(userName);
                                                writePermissions.get(fileName).add(userName);
                                                serializeObject(readPermissions, "./meta/readPermissions.txt");
                                                serializeObject(writePermissions, "./meta/writePermissions.txt");
                                                serializeObject(ownerPermissions, "./meta/ownerPermissions.txt");
                                            }
                                            if(permission.charAt(1)=='1'){
                                                writePermissions.get(fileName).add(userName);
                                                serializeObject(writePermissions, "./meta/writePermissions.txt");
                                            }
                                            if(permission.charAt(2)=='1'){
                                                readPermissions.get(fileName).add(userName);
                                                serializeObject(readPermissions, "./meta/readPermissions.txt");
                                            }
                                            for(int i=0;i<replicaReceivePortsList.size();i++){
                                                if(id == i)
                                                    continue;
                                                byte[] replicabuf = encrypt(receivedData).getBytes();
                                                DatagramPacket ReplicaDpSend =
                                                    new DatagramPacket(replicabuf, replicabuf.length, ip, replicaReceivePortsList.get(i));
                                                    serverSocket.send(ReplicaDpSend); 
                                            }
                                            System.out.println("Permissions granted to the user");
                                        }
                                        else{
                                            System.out.println("You dont have the ownership to grant permissions");
                                        }
                                        
                                    }
                                    catch(Exception e){
                                        System.out.println("Error");
                                        
                                    }
                                        break;
                    case "revokePermission":
                                        try{
                                        if(ownerPermissions.get(fileName).contains(user)){
                                            String userName = receivedData.split(" ")[3];
                                            String permission = receivedData.split(" ")[4];
                                            if(permission.charAt(0)=='0'){
                                                ownerPermissions.get(fileName).remove(userName);
                                                serializeObject(ownerPermissions, "./meta/ownerPermissions.txt");
                                            }
                                            if(permission.charAt(1)=='0'){
                                                writePermissions.get(fileName).remove(userName);
                                                serializeObject(writePermissions, "./meta/writePermissions.txt");
                                            }
                                            if(permission.charAt(2)=='0'){
                                                readPermissions.get(fileName).remove(userName);
                                                serializeObject(readPermissions, "./meta/readPermissions.txt");
                                            }
                                            for(int i=0;i<replicaReceivePortsList.size();i++){
                                                if(id == i)
                                                    continue;
                                                byte[] replicabuf = encrypt(receivedData).getBytes();
                                                DatagramPacket ReplicaDpSend =
                                                    new DatagramPacket(replicabuf, replicabuf.length, ip, replicaReceivePortsList.get(i));
                                                    serverSocket.send(ReplicaDpSend); 
                                            }
                                            System.out.println("Permissions revoked from the user");
                                        }
                                        else{
                                            System.out.println("You dont have the ownership to revoke permissions");
                                        }
                                        
                                        }
                                        catch(Exception e){
                                            System.out.println("Error");
                                            
                                        }
                                        break;
                    case "list":
                                        try{

                                        for(String path:filesAvailable){
                                            if(new File(path).isDirectory() || ownerPermissions.get(path).contains(user) || readPermissions.get(path).contains(user) || writePermissions.get(path).contains(user)){
                                                System.out.println(path.substring(13));
                                            }
                                        }
                                        
                                        }
                                        catch(Exception e){
                                            System.out.println("Error");
                                            
                                        }
                                        break;
                    case "mkdir":
                                        try{
                                        File file = new File(fileName);
                                        if(!file.exists()){
                                            file.mkdirs();
                                            filesAvailable.add(fileName);
                                            System.out.println("success directory created");
                                            for(int i=0;i<replicaReceivePortsList.size();i++){
                                                if(id == i)
                                                    continue;
                                                byte[] replicabuf = encrypt(receivedData).getBytes();
                                                DatagramPacket ReplicaDpSend =
                                                    new DatagramPacket(replicabuf, replicabuf.length, ip, replicaReceivePortsList.get(i));
                                                    serverSocket.send(ReplicaDpSend); 
                                            }
                                        }
                                        else{
                                            System.out.println("Failed directory already exists");
                                        }
                                        
                                        }
                                        catch(Exception e){
                                            System.out.println("Error");
                                        }
                                        break;
                    case "rmdir":
                                        try{
                                        File file = new File(fileName);
                                        if(file.exists() && file.list().length == 0){
                                            file.delete();
                                            filesAvailable.remove(fileName);
                                            System.out.println("success directory deleted");
                                            for(int i=0;i<replicaReceivePortsList.size();i++){
                                                if(id == i)
                                                    continue;
                                                byte[] replicabuf = encrypt(receivedData).getBytes();
                                                DatagramPacket ReplicaDpSend =
                                                    new DatagramPacket(replicabuf, replicabuf.length, ip, replicaReceivePortsList.get(i));
                                                    serverSocket.send(ReplicaDpSend); 
                                            }
                                        }
                                        else if(!file.exists()){
                                            System.out.println("Failed directory doesn't exists");
                                        }
                                        else if(file.list().length>0){
                                            System.out.println("Failed directory is not empty");
                                        }
                                        else{
                                            System.out.println("Failed");
                                        }
                                        
                                        }
                                        catch(Exception e){
                                            System.out.println("Error");
                                        }
                                        break;
            }
            
        }
        catch(Exception e){
            e.printStackTrace();
        }
        
    }
    public static void replica(){
        try{
        byte[] receive = new byte[65535];
        DatagramPacket DpReceive = null;
        while (true)
        {
            DpReceive = new DatagramPacket(receive, receive.length);
            replicaReceiveSocket.receive(DpReceive);
            //System.out.println("Client:-" + data(receive));
            String receivedData = decrypt(data(receive).toString());
            String operation = receivedData.split(" ")[0];
            String user = receivedData.split(" ")[1];
            
            String fileName;
            if(receivedData.split(" ")[2].startsWith("./Filesystem"))
                fileName =receivedData.split(" ")[2];
            else
                fileName = "./Filesystem/" + receivedData.split(" ")[2];
            int port = DpReceive.getPort();
            switch(operation){
                case "search":
                            try{
                                int serverId = Integer.valueOf(receivedData.split(" ")[3]);
                            //System.out.println(fileName);
                            File file = new File(fileName);
                                if(!file.exists()){
                                    System.out.println(fileName+" is missing in the filesystem");
                                    String message = encrypt("read server "+fileName+ " "+id);
                                    byte[] buf = message.getBytes();
                                    DatagramPacket DpSend =
                                        new DatagramPacket(buf, buf.length, ip, replicaReceivePortsList.get(serverId));
                                        replicaSocket.send(DpSend); 
                                        file.createNewFile();
                                        byte[] receiveData = new byte[65535];                      
                    
                                    while(true){
                                        receiveData = new byte[65535];
                                        DatagramPacket PacketReceive = new DatagramPacket(receiveData, receiveData.length);
                                        replicaSocket.receive(PacketReceive);
                                        String replicaData = decrypt(data(receiveData).toString());
                                        if(!replicaData.startsWith("done") && !replicaData.startsWith("Error" )){
                                            writeTo(fileName, replicaData);
                                        }
                                        else
                                            break;
                                    }
                                }
                                    
                            }
                            catch(Exception e){
                                System.out.println("Error");
                            }                    
                            break;
                case "read":
                            try{
                            File file = new File(fileName);
                            if(file.exists()){
                                String tmpFilePath = readFrom(fileName);
                                        File tmpFile = new File(tmpFilePath);
                                        Scanner myReader = new Scanner(tmpFile);
                                        while(myReader.hasNextLine()){
                                            String data = encrypt(myReader.nextLine());
                                            byte[] buf = data.getBytes();
                                            DatagramPacket DpSend =
                                            new DatagramPacket(buf, buf.length, ip, port);
                                            replicaReceiveSocket.send(DpSend);   
                                        }
                                        myReader.close();
                                        tmpFile.delete();
                                        String msg = encrypt("done"+user);
                                            byte[] buf = msg.getBytes();   
                                            DatagramPacket DpSend =
                                        new DatagramPacket(buf, buf.length, ip, port);
                                        replicaReceiveSocket.send(DpSend); 
                            }
                            }
                            catch(Exception e){
                                String msg = encrypt("Error"+user);
                                            byte[] buf = msg.getBytes();   
                                            DatagramPacket DpSend =
                                        new DatagramPacket(buf, buf.length, ip, port);
                                        replicaReceiveSocket.send(DpSend); 
                            }
                            break;
                case "write":
                            fileLocks.put(fileName,1);
                            File file = new File(fileName);
                            if(file.exists()){
                                byte[] receiveData = new byte[65535];
                                DatagramPacket PacketReceive = null;
                                while (true)
                                {
                                    PacketReceive = new DatagramPacket(receiveData, receiveData.length);
                                    replicaSocket.receive(PacketReceive);
                                    System.out.println("Client:-" + decrypt(data(receiveData).toString()));
                                    String writeData = decrypt(data(receiveData).toString());
                                    if(writeData.startsWith("Done")){
                                        fileLocks.put(fileName,0);
                                        break;
                                    }
                                    writeTo(fileName,writeData);
                                    receiveData = new byte[65535];
                                }
                            }
                            break;
                case "delete":
                            file = new File(fileName);
                            if(file.exists()){
                                file.delete();
                                readPermissions.remove(fileName);
                                serializeObject(readPermissions, "./meta/readPermissions.txt");
                                writePermissions.remove(fileName);
                                serializeObject(writePermissions, "./meta/writePermissions.txt");
                                ownerPermissions.remove(fileName);
                                serializeObject(ownerPermissions, "./meta/ownerPermissions.txt");
                                filesAvailable.remove(fileName);
                            }
                            break;
                case "create":
                            file = new File(fileName);
                            if(!file.exists()){
                                file.createNewFile();
                                filesAvailable.add(fileName);
                                List<String> permlist = new ArrayList<>();
                                permlist.add(user);
                                readPermissions.put(fileName,permlist);
                                serializeObject(readPermissions, "./meta/readPermissions.txt");
                                writePermissions.put(fileName,permlist);
                                serializeObject(writePermissions, "./meta/writePermissions.txt");
                                ownerPermissions.put(fileName,permlist);
                                serializeObject(ownerPermissions, "./meta/ownerPermissions.txt");
                            }
                            break;
                case "addUser":
                            String password = receivedData.split(" ")[2];
                            users.put(user, password);
                            break;
                case "grantPermission":
                            String userName = receivedData.split(" ")[3];
                            String permission = receivedData.split(" ")[4];
                            if(permission.charAt(0)=='1'){
                                ownerPermissions.get(fileName).add(userName);
                                readPermissions.get(fileName).add(userName);
                                writePermissions.get(fileName).add(userName);
                                serializeObject(readPermissions, "./meta/readPermissions.txt");
                                serializeObject(writePermissions, "./meta/writePermissions.txt");
                                serializeObject(ownerPermissions, "./meta/ownerPermissions.txt");
                            }
                            if(permission.charAt(1)=='1'){
                                writePermissions.get(fileName).add(userName);
                                serializeObject(writePermissions, "./meta/writePermissions.txt");
                            }
                            if(permission.charAt(2)=='1'){
                                readPermissions.get(fileName).add(userName);
                                serializeObject(readPermissions, "./meta/readPermissions.txt");
                            }
                            break;
                case "revokePermission":
                            userName = receivedData.split(" ")[3];
                            permission = receivedData.split(" ")[4];
                            if(permission.charAt(0)=='0'){
                                ownerPermissions.get(fileName).remove(userName);
                                serializeObject(ownerPermissions, "./meta/ownerPermissions.txt");
                            }
                            if(permission.charAt(1)=='0'){
                                writePermissions.get(fileName).remove(userName);
                                serializeObject(writePermissions, "./meta/writePermissions.txt");
                            }
                            if(permission.charAt(2)=='0'){
                                readPermissions.get(fileName).remove(userName);
                                serializeObject(readPermissions, "./meta/readPermissions.txt");
                            }
                            break;
                case "mkdir":
                            file = new File(fileName);
                            if(!file.exists()){
                                file.mkdirs();
                                filesAvailable.add(fileName);
                            }
                            break;
                case "rmdir":
                            file = new File(fileName);
                            if(file.exists() && file.list().length==0){
                                file.delete();
                                filesAvailable.remove(fileName);
                            }
                            break;
                
                
            }
            receive = new byte[65535];
        } 
    }
    catch(Exception e){
        e.printStackTrace();
    }
    }
    public static void writeTo(String fileName,String data)
    {
        try{
            String tempDecrypt = "./tmp/decrypt";
            File tmpFile = new File(tempDecrypt);
            if(tmpFile.exists()){
                tmpFile.delete();
                tmpFile.createNewFile();
            }
            else
                tmpFile.createNewFile();
            File file = new File(fileName);
            decrypt(file, tmpFile);
        BufferedWriter out = new BufferedWriter(new FileWriter(tempDecrypt, true));
        out.write(data);
        out.close();
        encrypt(tmpFile, file);
        tmpFile.delete();
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }
    public static String readFrom(String fileName){
        try{
        String tempDecrypt = "./tmp/decrypt";
        File tmpFile = new File(tempDecrypt);
        if(tmpFile.exists()){
            tmpFile.delete();
            tmpFile.createNewFile();
        }
        else
            tmpFile.createNewFile();
            File file = new File(fileName);
        decrypt(file, tmpFile);
        return tempDecrypt;
    }
    catch(Exception e){
        e.printStackTrace();
    }
    return null;
    }
    public static StringBuilder data(byte[] a)
    {
        if (a == null)
            return null;
        StringBuilder ret = new StringBuilder();
        int i = 0;
        while (a[i] != 0)
        {
            ret.append((char) a[i]);
            i++;
        }
        return ret;
    }
    public static List<Integer> sendSearch(String fileName){
        try{
            List<Integer> found = new ArrayList<>();
            for(int i=0;i<replicaSendPortsList.size();i++){
                if(i==id)
                    continue;
                String inp = "search "+fileName+" "+id;
                byte buf[] = null;
                buf = inp.getBytes();
                DatagramPacket DpSend =
                    new DatagramPacket(buf, buf.length, ip, replicaReceivePortsList.get(i));
                replicaSendSocket.send(DpSend);
                byte[] receive = new byte[65535];                      
                DatagramPacket DpReceive = null;
                DpReceive = new DatagramPacket(receive, receive.length);
                replicaSendSocket.receive(DpReceive);
                System.out.println("Client:-" + data(receive));
                String receivedData = data(receive).toString();
                if(receivedData.startsWith("Yes")){
                    found.add(i);
                }
            }
            return found;
        }
        catch(Exception e){
            e.printStackTrace();
        }
        return null;
    }
    public static List<String> sendRead(String fileName,int peer){
        try{
            Scanner sc = new Scanner(System.in);
            
            List<String> fileData = new ArrayList<>();
            String inp = "read "+fileName+" "+id;
            byte buf[] = null;
            buf = inp.getBytes();
            DatagramPacket DpSend =
                new DatagramPacket(buf, buf.length, ip, replicaReceivePortsList.get(peer));
            replicaSendSocket.send(DpSend);
            byte[] receive = new byte[65535];                      
            DatagramPacket DpReceive = null;
            while(true){
                receive = new byte[65535];
                DpReceive = new DatagramPacket(receive, receive.length);
                replicaSendSocket.receive(DpReceive);
                System.out.println("Client:-" + data(receive));
                String receivedData = data(receive).toString();
                if(!receivedData.startsWith("done")){
                    fileData.add(receivedData);
                }
                else
                    break;
                receive = null;
            }
            return fileData;
        }
        catch(Exception e){
            e.printStackTrace();
        }
        return null;
    }
    
    public static void encrypt(File inputFile, File outputFile)
           {
        doCrypto(Cipher.ENCRYPT_MODE, key, inputFile, outputFile);
    }
 
    public static void decrypt(File inputFile, File outputFile)
            {
        doCrypto(Cipher.DECRYPT_MODE, key, inputFile, outputFile);
    }
 
    private static void doCrypto(int cipherMode, String key, File inputFile,
            File outputFile)  {
        try {
            Key secretKey = new SecretKeySpec(key.getBytes(), ALGORITHM);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(cipherMode, secretKey);
             
            FileInputStream inputStream = new FileInputStream(inputFile);
            byte[] inputBytes = new byte[(int) inputFile.length()];
            inputStream.read(inputBytes);
             
            byte[] outputBytes = cipher.doFinal(inputBytes);
             
            FileOutputStream outputStream = new FileOutputStream(outputFile);
            outputStream.write(outputBytes);
             
            inputStream.close();
            outputStream.close();
             
        } catch (NoSuchPaddingException | NoSuchAlgorithmException
                | InvalidKeyException | BadPaddingException
                | IllegalBlockSizeException | IOException ex) {
            ex.printStackTrace();
        }
    } 
    public static String encrypt(String strToEncrypt)
    {
        try {
  
            // Create default byte array
            byte[] iv = { 0, 0, 0, 0, 0, 0, 0, 0,
                          0, 0, 0, 0, 0, 0, 0, 0 };
            IvParameterSpec ivspec
                = new IvParameterSpec(iv);
  
            // Create SecretKeyFactory object
            SecretKeyFactory factory
                = SecretKeyFactory.getInstance(
                    "PBKDF2WithHmacSHA256");
            
            // Create KeySpec object and assign with
            // constructor
            KeySpec spec = new PBEKeySpec(
                SECRET_KEY.toCharArray(), SALT.getBytes(),
                65536, 256);
            SecretKey tmp = factory.generateSecret(spec);
            SecretKeySpec secretKey = new SecretKeySpec(
                tmp.getEncoded(), "AES");
  
            Cipher cipher = Cipher.getInstance(
                "AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey,
                        ivspec);
            // Return encrypted string
            return Base64.getEncoder().encodeToString(
                cipher.doFinal(strToEncrypt.getBytes(
                    StandardCharsets.UTF_8)));
        }
        catch (Exception e) {
            System.out.println("Error while encrypting: "
                               + e.toString());
        }
        return null;
    }
  
    // This method use to decrypt to string
    public static String decrypt(String strToDecrypt)
    {
        try {
  
            // Default byte array
            byte[] iv = { 0, 0, 0, 0, 0, 0, 0, 0,
                          0, 0, 0, 0, 0, 0, 0, 0 };
            // Create IvParameterSpec object and assign with
            // constructor
            IvParameterSpec ivspec
                = new IvParameterSpec(iv);
  
            // Create SecretKeyFactory Object
            SecretKeyFactory factory
                = SecretKeyFactory.getInstance(
                    "PBKDF2WithHmacSHA256");
  
            // Create KeySpec object and assign with
            // constructor
            KeySpec spec = new PBEKeySpec(
                SECRET_KEY.toCharArray(), SALT.getBytes(),
                65536, 256);
            SecretKey tmp = factory.generateSecret(spec);
            SecretKeySpec secretKey = new SecretKeySpec(
                tmp.getEncoded(), "AES");
  
            Cipher cipher = Cipher.getInstance(
                "AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, secretKey,
                        ivspec);
            // Return decrypted string
            return new String(cipher.doFinal(
                Base64.getDecoder().decode(strToDecrypt)));
        }
        catch (Exception e) {
            System.out.println("Error while decrypting: "
                               + e.toString());
        }
        return null;
    }
}