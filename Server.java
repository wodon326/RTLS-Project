import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.ByteBuffer;

import java.sql.*;


public class Server {
   static Scanner scanner = new Scanner(System.in);
   static Queue<Socket> queue = new LinkedList<>();
   static HashMap<Integer,Thread> client_map = new HashMap<Integer,Thread>();
   static ServerSocket listener = null;
   static Socket socket = null;
   static final byte STX = (byte)0x02;
   static final byte ETX = (byte)0x03;
   static final byte CMD_RTDATA = (byte)0x00;
   static final byte CMD_ALLSTAT = (byte)0x01;
   static final byte CMD_MSG = (byte)0x02;
   static final byte CMD_LOGIN = (byte)0x10;
   static final byte CMD_LIST = (byte)0x11;
   static int Client_num = 0;
   static Connection conn;
   static Statement stmt = null;
   static PreparedStatement pstmt_chat = null;
   static PreparedStatement pstmt_rtls = null;
   static String SQL_chat = "insert into chat(sender,receiver,msg) values(?, ?, ?)";
   static String SQL_RTLS = "insert into rtls(id,stat,x,y) values(?, ?, ?,?)";
   public static void main(String[] args) throws IOException, ClassNotFoundException, SQLException
   {
	  try {
		  Class.forName("com.mysql.jdbc.Driver"); // MySQL 드라이버 로드
		  conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/rtls_db", "root","0221"); 
		  // JDBC 연결, password는 root 계정 패스워드 입력
		  System.out.println("DB 연결 완료");
		  stmt = conn.createStatement(); // SQL문 처리용 Statement 객체 생성
		  pstmt_chat = conn.prepareStatement(SQL_chat);	
		  pstmt_rtls = conn.prepareStatement(SQL_RTLS);	
	  } catch (ClassNotFoundException e) {
		  System.out.println("JDBC 드라이버 로드 에러");
	  } catch (SQLException e) {
		  System.out.println("SQL 실행 에러");
	  }
      accepter Accepter = new accepter();
      worker Worker = new worker();
      Accepter.start(); 
      System.out.println("Accepter 시작");
      Worker.start(); 
      System.out.println("Worker 시작");
      monitor_thread Monitor = new monitor_thread();
      Monitor.start();
   }
   static class accepter extends Thread{
      public accepter() {
         setName("accepter");
      }
      @Override
      public void run() {
         try {
            listener = new ServerSocket(3000);
            while(true)
            {
               System.out.println("연결을 기다리고있습니다....");
               socket = listener.accept();
               System.out.println("연결되었습니다.");
               queue.add(socket);
            }
         } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
         }
      }
   }
   static class worker extends Thread{
      public worker() {
         setName("worker");
      }
      @Override
      public void run() {
         while(true)
         {
            try {
               Thread.sleep(1000);
            } catch (InterruptedException e1) {
               // TODO Auto-generated catch block
               e1.printStackTrace();
            }
            if(!queue.isEmpty())
            {
               Socket socket = (Socket)queue.poll();
               try {
                  Client_Thread thread = new Client_Thread(socket);
                  thread.start();
                  System.out.println("쓰레드 시작.");
               } catch (IOException e) {
                  // TODO Auto-generated catch block
                  e.printStackTrace();
               } catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            }
         }
      }
   }
   //PrintWriter pw = ((Client_Thread)client_map.get(socket)).pw; <- pw얻어서 데이터 전송
   static class Client_Thread extends Thread{
      int Id;
      Socket socket;
      InputStream  is = null;
      OutputStream  os;
      ObjectInputStream ois;
      byte[] buf = new byte[512];
      public ObjectOutputStream oos;
      public int RTLS_ID=0;
      public int RTLS_STATE=0;
      public int RTLS_X=0;
      public int RTLS_Y=0;
      public Client_Thread(Socket socket1) throws IOException, ClassNotFoundException {
         socket = socket1;
         os = socket.getOutputStream();
         oos = new ObjectOutputStream(os);
         is = socket.getInputStream();
         ois = new ObjectInputStream(is);
         buf = (byte[])ois.readObject();
         if(buf[0]==STX&&buf[3]==ETX)
         {
        	 switch(buf[1]){
             case CMD_LOGIN:
            	 Id = (int)buf[2];
            	 for(int key : client_map.keySet()) {
            		 if(key != Id)
            		 {
            			 ObjectOutputStream send_out = ((Client_Thread)client_map.get(key)).oos;
            			 send_out.writeObject(buf);
            		 }
                 }
            	 break;
             default:
            	 break;
        	 }
         }
         byte [] buf_list = new byte[Client_num+3];
         if(Client_num>0)
         {
        	 int n = 2;
        	 buf_list[0] = STX;
        	 buf_list[1] = CMD_LIST;
        	 for(int key : client_map.keySet()) {
            	 buf_list[n++] = (byte)key;
             }
        	 buf_list[n] = ETX;
        	 oos.writeObject(buf_list);
         }
         Client_num++;
         client_map.put(Id, this);
      }
      @Override
      public void run() {
         try {
            while(true)
            {
               buf  = (byte[])ois.readObject();
               int sender = 0;
               int receiver = 0;
               byte[]int_byte = new byte[4];
               byte[]msg_byte = null;
               if(buf[0]==STX&&buf[buf.length-1]==ETX)
               {
            	   switch(buf[1]){
                   case CMD_MSG:
                	   sender = (int)buf[2];
                	   receiver = (int)buf[3];
                	   msg_byte = new byte[buf.length-5];
                	   System.arraycopy(buf, 4, msg_byte, 0, buf.length-5);
                	   
                	   if(receiver==Id)
                       {
                          System.out.println("error num");
                       }
                       else
                       {
                    	  pstmt_chat.setInt(1, sender); 
                    	  pstmt_chat.setInt(2, receiver); 
                    	  pstmt_chat.setString(3, new String(msg_byte));
                    	  int r = pstmt_chat.executeUpdate();
                    	  ObjectOutputStream send_out = ((Client_Thread)client_map.get(receiver)).oos;
                    	  send_out.writeObject(buf);
                       }
                	   break;
                   case CMD_RTDATA:
                	   RTLS_ID=(int)buf[2];
                       RTLS_STATE=(int)buf[3];
                       System.arraycopy(buf, 4, int_byte, 0, 4);
                       RTLS_X=ByteBuffer.wrap(int_byte).getInt();
                       System.arraycopy(buf, 8, int_byte, 0, 4);
                       RTLS_Y=ByteBuffer.wrap(int_byte).getInt();
                       pstmt_rtls.setInt(1, RTLS_ID);
                       pstmt_rtls.setInt(2, RTLS_STATE);
                       pstmt_rtls.setInt(3, RTLS_X);
                       pstmt_rtls.setInt(4, RTLS_Y);
                       int r = pstmt_rtls.executeUpdate();
                	   break;
                   default:
                	   break;
                   }
               }
               else
               {
            	   continue;
               }
               
            }
         }
         catch(IOException e){
            System.out.println(e.getMessage());
         } catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
         finally
         {
            try {
               scanner.close();
               socket.close();
            }
            catch(IOException e){
               System.out.println("클라이언트와 채팅 중 오류갸 발생했습니다.");
            }
         }
      }
   }
   static class monitor_thread extends Thread{
	   public monitor_thread() {
		   setName("monitor_thread");
	   }
	   @Override
	   public void run() {
		   try {
			   ServerSocket listener_monitor = new ServerSocket(3001);
			   System.out.println("monitor의 연결을 기다리고있습니다....");
			   Socket socket_monitor = listener_monitor.accept();
               System.out.println("monitor와 연결되었습니다.");
               OutputStream os_monitor = socket_monitor.getOutputStream();
               ObjectOutputStream oos_monitor = new ObjectOutputStream(os_monitor);
			   while(true)
			   {
				   try {
		               Thread.sleep(1000);
				   } catch (InterruptedException e1) {
					   // TODO Auto-generated catch block
					   e1.printStackTrace();
				   }
				   if(Client_num>0)
				   {
					   int i = 0;
		               byte[] buf_monitor = new byte[Client_num*10+4];
		               buf_monitor[0]=STX;
		               buf_monitor[1]=CMD_ALLSTAT;
		               buf_monitor[2]=(byte)Client_num;
		               for(int key : client_map.keySet()) {
		            	   byte[] buf_monitor_recode = new byte[10];
		            	   byte[]int_byte = new byte[4];
		            	   buf_monitor_recode[0] = (byte)key;
		            	   buf_monitor_recode[1] = (byte)((Client_Thread)client_map.get(key)).RTLS_STATE;
		            	   int_byte = intToBytes(((Client_Thread)client_map.get(key)).RTLS_X);
		            	   System.arraycopy(int_byte, 0, buf_monitor_recode, 2, 4);
		            	   int_byte = intToBytes(((Client_Thread)client_map.get(key)).RTLS_Y);
		            	   System.arraycopy(int_byte, 0, buf_monitor_recode, 6, 4);
		            	   System.arraycopy(buf_monitor_recode, 0, buf_monitor, 3+i, 10);
		            	   i+=10;
		               }
		               buf_monitor[Client_num*10+3]=ETX;
		               oos_monitor.writeObject(buf_monitor);
				   }
			   }
		   } catch (IOException e1) {
			   // TODO Auto-generated catch block
			   e1.printStackTrace();
		   }
	   }
	   public static byte[] intToBytes( final int i ) {
		    ByteBuffer bb = ByteBuffer.allocate(4); 
		    bb.putInt(i); 
		    return bb.array();
		}
   }
}


