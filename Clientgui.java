import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.BadLocationException;
import java.io.*;
import java.net.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.nio.ByteBuffer;

public class Clientgui extends JFrame {
	//통신할 때 데이터를 보내고 받을 때 필요한 변수
	static InputStream  is = null;
	static OutputStream  os;
	static ObjectInputStream ois;
	static ObjectOutputStream oos;
	static Socket socket;
	static byte client_ID;
	static final byte STX = (byte)0x02;
	static final byte ETX = (byte)0x03;
	static final byte CMD_RTDATA = (byte)0x00;
	static final byte CMD_ALLSTAT = (byte)0x01;
	static final byte CMD_MSG = (byte)0x02;
	static final byte CMD_LOGIN = (byte)0x10;
	static final byte CMD_LIST = (byte)0x11;
	static byte state = (byte)0x00;
	static byte normal = (byte)0x00;
	static byte danger = (byte)0xFF;
	static int x;
	static int y;
	int FLYING_UNIT = 10;//키보드 한번 클릭할때 움직이는 크기
	JPanel contentPane;
	static JMenu Menu;
	static JLabel location = new JLabel(" ");
	//데이터베이스 변수
	static Connection conn;
	static Statement stmt = null;
	static ResultSet srs; 
	
	public Clientgui(int ID) {
		ImageIcon icon;//배경
		icon = new ImageIcon("C:\\Users\\마상균\\eclipse-workspace\\Clientgui\\src\\RTLS map.png");
		
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 500, 350);
		setTitle("ID : "+ ID);
		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);
		
		Menu = new JMenu("Chat Other Client");
		menuBar.add(Menu);
		contentPane = new JPanel(){
            public void paintComponent(Graphics g) {
                Dimension d = getSize();
                g.drawImage(icon.getImage(), 0, 0, d.width, d.height, null);
            }
        };
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);
		contentPane.addKeyListener(new MyKeyListener());//키보드 입력 이벤트 추가
		
		//초기 위치를 room 7에 배치
		x = 394;
		y = 225;
		location.setLocation(x, y);
		location.setSize(100, 20);
		contentPane.add(location);
		contentPane.setFocusable(true);
		contentPane.requestFocus();
	}
	
	//키보드의 입력은 인식하여 방향키대로 클라이언트를 움직이며 상태를 체크하는 클래스
	class MyKeyListener extends KeyAdapter{
	    public void keyPressed(KeyEvent e){
	        int keyCode = e.getKeyCode();
	        switch (keyCode) {
	        case KeyEvent.VK_UP:
	        	x=location.getX();
	        	y=location.getY() - FLYING_UNIT;
	        	location.setLocation(x, y);
	        	state = state_check(x,y);
	            break;
	        case KeyEvent.VK_DOWN:
	        	x=location.getX();
	        	y=location.getY() + FLYING_UNIT;
	        	location.setLocation(x, y);
	        	state = state_check(x,y);
	            break;
	        case KeyEvent.VK_LEFT:
	        	x=location.getX() - FLYING_UNIT;
	        	y=location.getY();
	        	location.setLocation(x, y);
	        	state = state_check(x,y);
	            break;
	        case KeyEvent.VK_RIGHT:
	        	x=location.getX() + FLYING_UNIT;
	        	y=location.getY();
	        	location.setLocation(x, y);
	        	state = state_check(x,y);
	            break;
	        }
	    }
	}
	
	//클라이언트의 상태를 체크하는 함수 (danger room : room 1,room 4, room 6)
	public static byte state_check(int x,int y){
		if(0<=x&&x<=100&&0<=y&&y<=110)//room 1
		{
			location.setForeground(Color.RED);//state가 danger일때 빨간색으로 교체
			return danger;
		}
		else if(360<=x&&x<=500&&0<=y&&y<=110)//room 4
		{
			location.setForeground(Color.RED);//state가 danger일때 빨간색으로 교체
			return danger;
		}
		else if(160<=x&&x<=310&&160<=y&&y<=270)//room 6
		{
			location.setForeground(Color.RED);//state가 danger일때 빨간색으로 교체
			return danger;
		}
		location.setForeground(Color.BLACK);//앞에 if문을 모두 지나치면 안전하므로 검은색으로 교체
		return normal;
	}
	/**
	 * Launch the application.
	 * @throws IOException 
	 * @throws UnknownHostException 
	 */
	
	public static void main(String[] args) throws UnknownHostException, IOException {
		socket = new Socket("localhost",3000); // 서버에 접속
		
	    client_ID = (byte)Integer.parseInt(JOptionPane.showInputDialog("로그인 : 양의 정수를 입력해주세요 : ")); // 로그인 화면
	    
	    //로그인 후 배경화면 띄우기
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					Clientgui frame = new Clientgui(client_ID);
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		
		//데이터베이스 연결
		try {
			Class.forName("com.mysql.jdbc.Driver"); // MySQL 드라이버 로드
			conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/rtls_db", "root","0221"); 
			// JDBC 연결, password는 root 계정 패스워드 입력
			System.out.println("DB 연결 완료");
			stmt = conn.createStatement(); // SQL문 처리용 Statement 객체 생성
			
		} catch (ClassNotFoundException e) {
			System.out.println("JDBC 드라이버 로드 에러");
		} catch (SQLException e) {
			System.out.println("SQL 실행 에러");
		}
		
		//통신할 때 데이터를 보내고 받을 때 필요한 변수를 설정
	    os = socket.getOutputStream();
	    oos = new ObjectOutputStream(os);
	    is = socket.getInputStream();
	    ois = new ObjectInputStream(is);
	    location.setText( Integer.toString((int)client_ID));
		
	    
	    
	    //데이터를 받는 쓰레드와 실시간 위치 전송 쓰레드 실행
	    Receiver thread_receiver = new Receiver();
	    thread_receiver.start(); 
	    RTLS thread_rtls = new RTLS();
	    thread_rtls.start();
	    
	    //서버에 로그인 패킷 전송
	    byte[] buf_login = new byte[4];
	    buf_login[0] = STX;
	    buf_login[1] = CMD_LOGIN;
	    buf_login[2] = client_ID;
	    buf_login[3] = ETX;
	    oos.writeObject(buf_login);
	}
	
	//서버에서 데이터를 받았을 때 받은 데이터를 처리하는 쓰레드
	static class Receiver extends Thread{
		byte[] buf = new byte[512];
		JMenuItem NewMenuItem;
		public Receiver() {
			setName("Receiver");
		}
		@Override
		public void run() {
			try {
				while(true) {
					buf = (byte[])ois.readObject(); // 데이터 받기 (데이터를 받을 때까지 대기)
					
					int sender = 0;
					byte[]msg_byte = null;
					if(buf[0]==STX&&buf[buf.length-1]==ETX)
					{
						switch(buf[1]){
						case CMD_MSG: //CMD_MSG일때 데이터를 분석한 후 채팅 gui에 추가
							sender = (int)buf[2];
							msg_byte = new byte[buf.length-5];
	                	   	System.arraycopy(buf, 4, msg_byte, 0, buf.length-5);
	                	   	String msg_string = new String(msg_byte);
	                	   	String msg = "클라이언트"+sender+" : "+msg_string;
	                	   	
	                	   	//채팅 gui에 추가
	                	   	Clientgui_Chat.textArea.append(msg + "\n");
	                	   	Clientgui_Chat.textArea.setCaretPosition(Clientgui_Chat.textArea.getText().length());
	                	   	break;
	                   case CMD_LOGIN://CMD_LOGIN일때 새로운 클라이언트가 로그인했다는 팝업창을 띄우고 메뉴에 새로운 클라이언트와 채팅할 수 있게 Chat Other Client메뉴에 추가
	                	   	int new_client = (int)buf[2];
	                	   	
	                	   	//새로운 클라이언트가 로그인했다는 팝업창을 띄우기
                	   		String msg_login = "ID:"+new_client+"가 로그인하였습니다.";
	                	   	JOptionPane.showMessageDialog(null, msg_login);
	                	   	
	                	   	//새로운 클라이언트와 채팅할 수 있게 Chat Other Client메뉴에 추가
	                	   	NewMenuItem = new JMenuItem( Integer.toString(new_client));
	                	   	
	                	   	//메뉴 클릭했을때 데이터베이스에서 채팅기록을 가져오고 채팅할수있는 Clientgui_Chat을 띄움
	                	   	NewMenuItem.addActionListener(new ActionListener() { 
	               				public void actionPerformed(ActionEvent e) {
	               					//다른 클라이언트와 채팅한 기록을 전부 날림
	               					Clientgui_Chat.textArea.setText("");
	               					
	               					int to = Integer.parseInt(e.getActionCommand());
	               					Clientgui_Chat chat = new Clientgui_Chat(to,(int)client_ID);
	               					try {
	               						//데이터베이스에서 채팅기록을 가져옴
	               						srs = stmt.executeQuery("select * from chat");
	               						while (srs.next()) {
											int sender_db = srs.getInt("sender");
											int receiver_db = srs.getInt("receiver");
											String msg_db = srs.getString("msg");
											if((sender_db == (int)client_ID||sender_db==to)&&(receiver_db == (int)client_ID||receiver_db==to))
											{
												String msg_db_gui = "클라이언트"+sender_db+" : "+msg_db;
												Clientgui_Chat.textArea.append(msg_db_gui + "\n");
												Clientgui_Chat.textArea.setCaretPosition(Clientgui_Chat.textArea.getText().length());
											}
			               				}
									} catch (SQLException e1) {
										// TODO Auto-generated catch block
										e1.printStackTrace();
									}
		               				chat.setVisible(true);
		               			}
	               			});
	                	   	Menu.add(NewMenuItem);
	               			break;
	                   case CMD_LIST:
	                	   	for(int i = 2;i<buf.length-1;i++)
	                	   	{
	                	   		if((int)buf[i]!=(int)client_ID)
	                	   		{
	                	   			//CMD_LOGIN에서 메뉴 추가하는것과 같음
	                	   			NewMenuItem = new JMenuItem(Integer.toString((int)buf[i]));
	                	   			NewMenuItem.addActionListener(new ActionListener() {
	                	   				public void actionPerformed(ActionEvent e) {
	                	   					Clientgui_Chat.textArea.setText("");
	                	   					int to = Integer.parseInt(e.getActionCommand());
	                	   					Clientgui_Chat chat = new Clientgui_Chat(to,(int)client_ID);
	                	   					try {
	                	   						srs = stmt.executeQuery("select * from chat");
	                	   						while (srs.next()) {
	                	   							int sender_db = srs.getInt("sender");
	        										int receiver_db = srs.getInt("receiver");
	        										String msg_db = srs.getString("msg");
	        										if((sender_db == (int)client_ID||sender_db==to)&&(receiver_db == (int)client_ID||receiver_db==to))
	        										{
	        											String msg_db_gui = "클라이언트"+sender_db+" : "+msg_db;
	        											Clientgui_Chat.textArea.append(msg_db_gui + "\n");
	        											Clientgui_Chat.textArea.setCaretPosition(Clientgui_Chat.textArea.getText().length());
	        										}
	        		               				}
	        								} catch (SQLException e1) {
	        									// TODO Auto-generated catch block
	        									e1.printStackTrace();
	        								}
	        	               				chat.setVisible(true);
	        	               			}
	                	   			});
	                	   			Menu.add(NewMenuItem);
	                	   		}
	                	   	}
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
	         }catch(IOException e){
	        	 System.out.println(e.getMessage());
	         } catch (ClassNotFoundException e) {
	        	 // TODO Auto-generated catch block
	        	 e.printStackTrace();
	         }finally {
				try {
	            	if(socket != null) {
	            	   socket.close();      
	               }
	            }
	            catch(IOException e){
	            	System.out.println("서버와 채팅 중 오류갸 발생했습니다.");
	            }
			}
	      }
	}
	
	//1초마다 실시간 위치를 서버로 전송하는 쓰레드
	static class RTLS extends Thread{
		byte[] buf_RTLS = new byte[13];
		public RTLS() {
			setName("RTLS");
		}
		@Override
		public void run() {
			while(true)
			{
				//1초마다 CMD_RTDATA 패킷을 만들어 서버로 전송
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				byte []data_RTLS = new byte[10];
				byte []int_byte = new byte[4];
				data_RTLS[0] = client_ID;
				data_RTLS[1] = state;
				int_byte = intToBytes(x);
				System.arraycopy(int_byte, 0, data_RTLS, 2, 4);
				int_byte = intToBytes(y);
				System.arraycopy(int_byte, 0, data_RTLS, 6, 4);
				buf_RTLS= makepacket(CMD_RTDATA,data_RTLS);
				try {
					oos.writeObject(buf_RTLS);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	    	  }
	      }
	}   
	
	//패킷 만드는 함수
	public static byte[] makepacket(byte cmd, byte[]data){
       byte[]pack = new byte[data.length+3];
       pack[0] = STX;
       pack[1] = cmd;
       System.arraycopy(data, 0, pack, 2, data.length);
       pack[pack.length-1] = ETX;
       return pack;
   	}
	
	//int -> byte[] 함수
	public static byte[] intToBytes( final int i ) {
	    ByteBuffer bytebuffer = ByteBuffer.allocate(4); 
	    bytebuffer.putInt(i); 
	    return bytebuffer.array();
	}
}

//채팅할 수 있는 화면 구성하는 클래스
class Clientgui_Chat extends JFrame {
	byte[] buf = new byte[512];
	static JTextField textField;
	static JTextArea textArea = new JTextArea();
	
	public Clientgui_Chat(int to,int ID) {
		setTitle("ID : "+ ID+" <-> ID : "+to);
		setBounds(100, 100, 300, 500);
		getContentPane().setLayout(null);
		textArea.setEditable(false); // 채팅 기록이 쌓이는 곳이므로 입력 못하게 막아둔 코드
		textField = new JTextField();
		textField.setBounds(12, 430, 179, 21);
		getContentPane().add(textField);
		textField.setColumns(10);
		textArea.setBounds(12, 10, 260, 410);
		getContentPane().add(textArea);
		JButton Send_Button = new JButton("Send");
		//Send버튼을 눌렀을 때 CMD_MSG패킷을 만들어 서버로 전송
		Send_Button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				//textField에서 String을 가져오고 textField을 비움
				String outputMessage = textField.getText();
				textField.setText("");
				//textField에서 가져온 String을 textArea에 추가
				textArea.append("클라이언트"+(int)Clientgui.client_ID+" : " + outputMessage + "\n");
				textArea.setCaretPosition(textArea.getText().length());
				//textField에서 가져온 String을 CMD_MSG패킷으로 만들어 서버로 전송
				byte[]msg = outputMessage.getBytes();
				byte[]data = new byte[msg.length+3];
				data[0] = Clientgui.client_ID;
				data[1] = (byte) to;
	            System.arraycopy(msg, 0, data, 2, msg.length);
	            buf = Clientgui.makepacket(Clientgui.CMD_MSG,data);
	            try {
	            	Clientgui.oos.writeObject(buf);
				}
	            catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		});
		Send_Button.setBounds(198, 429, 74, 23);
		getContentPane().add(Send_Button);
	}
}

