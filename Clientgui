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
	public Clientgui(int to,int ID) {
		setTitle("ID : "+ ID+" <-> ID : "+to);
		
		setBounds(100, 100, 300, 500);
		getContentPane().setLayout(null);
		textArea.setEditable(false); 
		textField = new JTextField();
		textField.setBounds(12, 430, 179, 21);
		getContentPane().add(textField);
		textField.setColumns(10);
		
		
		textArea.setBounds(12, 10, 260, 410);
		getContentPane().add(textArea);
		
		JButton btnNewButton = new JButton("Send");
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
   				String outputMessage = textField.getText();
   				textField.setText("");
   				textArea.append("클라이언트"+(int)client_ID+" : " + outputMessage + "\n");
   				textArea.setCaretPosition(textArea.getText().length());
   				byte[]msg = outputMessage.getBytes();
   				byte[]data = new byte[msg.length+3];
   				data[0] = client_ID;
   				data[1] = (byte) to;
	            System.arraycopy(msg, 0, data, 2, msg.length);
	            buf = makepacket(CMD_MSG,data);
	            try {
					oos.writeObject(buf);
				}
	            catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		});
		btnNewButton.setBounds(198, 429, 74, 23);
		getContentPane().add(btnNewButton);
		
		
	}
	static InputStream  is = null;
	static byte[] buf = new byte[512];
	static byte[] buf_RTLS = new byte[13];
	static byte[] packet = new byte[512];
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
	static JMenu mnNewMenu;
	static JMenuItem mntmNewMenuItem;
	public static JPanel contentPane;
	public static JLabel location = new JLabel(" ");
	private JTextField textField;
	static JTextArea textArea = new JTextArea();
	static Connection conn;
	static Statement stmt = null;
	static ResultSet srs; 
	static int x;
	static int y;
	static byte state = (byte)0x00;
	static byte normal = (byte)0x00;
	static byte danger = (byte)0xFF;
	/**
	 * Launch the application.
	 * @throws IOException 
	 * @throws UnknownHostException 
	 */
	public static void main(String[] args) throws UnknownHostException, IOException {
		socket = new Socket("localhost",3000);
	    client_ID = (byte)Integer.parseInt(JOptionPane.showInputDialog("로그인 : 양의 정수를 입력해주세요 : "));
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					Clientgui_1 frame = new Clientgui_1(client_ID);
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
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
	    os = socket.getOutputStream();
	    oos = new ObjectOutputStream(os);
	    is = socket.getInputStream();
	    ois = new ObjectInputStream(is);
	    location.setText( Integer.toString((int)client_ID));
		
	    byte[] buf_login = new byte[4];
	    buf_login[0] = STX;
	    buf_login[1] = CMD_LOGIN;
	    buf_login[2] = client_ID;
	    buf_login[3] = ETX;
	    oos.writeObject(buf_login);
	    Receiver t1 = new Receiver();
	    t1.start(); 
	    RTLS t2 = new RTLS();
	    t2.start();
	  	}

	static class Receiver extends Thread{
	      public Receiver() {
	         setName("Receiver");
	      }
	      @Override
	      public void run() {

	         try {
	            
	            while(true)
	            {
	               buf = (byte[])ois.readObject();
	               
	               int sender = 0;
	               byte[]msg_byte = null;
	               if(buf[0]==STX&&buf[buf.length-1]==ETX)
	               {
	            	   switch(buf[1]){
	                   case CMD_MSG:
	                	   	sender = (int)buf[2];
	                	   	msg_byte = new byte[buf.length-5];
	                	   	System.arraycopy(buf, 4, msg_byte, 0, buf.length-5);
	                	   	String msg_string = new String(msg_byte);
	                	   	String msg = "클라이언트"+sender+" : "+msg_string;
	                	   	textArea.append(msg + "\n");
	           				textArea.setCaretPosition(textArea.getText().length());
	                	   	break;
	                   case CMD_LOGIN:
	                	   	int new_client = (int)buf[2];
                	   	
                	   		String msg_login = "ID:"+new_client+"가 로그인하였습니다.";
	                	   	JOptionPane.showMessageDialog(null, msg_login);
	                	   	
	               			mntmNewMenuItem = new JMenuItem( Integer.toString(new_client));
	               			mntmNewMenuItem.addActionListener(new ActionListener() {
	               				public void actionPerformed(ActionEvent e) {
	               					textArea.setText("");
	               					int to = Integer.parseInt(e.getActionCommand());
	               					Clientgui chat = new Clientgui(to,(int)client_ID);
	               					try {
	               						srs = stmt.executeQuery("select * from chat");
	               						while (srs.next()) {
											int sender_db = srs.getInt("sender");
											int receiver_db = srs.getInt("receiver");
											String msg_db = srs.getString("msg");
											if((sender_db == (int)client_ID||sender_db==to)&&(receiver_db == (int)client_ID||receiver_db==to))
											{
												String msg_db_gui = "클라이언트"+sender_db+" : "+msg_db;
												textArea.append(msg_db_gui + "\n");
								   				textArea.setCaretPosition(textArea.getText().length());
											}
			               				}
									} catch (SQLException e1) {
										// TODO Auto-generated catch block
										e1.printStackTrace();
									}
		               				chat.setVisible(true);
		               			}
	               			});
	               			mnNewMenu.add(mntmNewMenuItem);
	               			break;
	                   case CMD_LIST:
	                	   	for(int i = 2;i<buf.length-1;i++)
	                	   	{
	                	   		if((int)buf[i]!=(int)client_ID)
	                	   		{
	                	   			mntmNewMenuItem = new JMenuItem(Integer.toString((int)buf[i]));
	                	   			mntmNewMenuItem.addActionListener(new ActionListener() {
	                	   				public void actionPerformed(ActionEvent e) {
	                	   					textArea.setText("");
	                	   					int to = Integer.parseInt(e.getActionCommand());
	                	   					Clientgui chat = new Clientgui(to,(int)client_ID);
	                	   					try {
	                	   						srs = stmt.executeQuery("select * from chat");
	                	   						while (srs.next()) {
	                	   							int sender_db = srs.getInt("sender");
	        										int receiver_db = srs.getInt("receiver");
	        										String msg_db = srs.getString("msg");
	        										if((sender_db == (int)client_ID||sender_db==to)&&(receiver_db == (int)client_ID||receiver_db==to))
	        										{
	        											String msg_db_gui = "클라이언트"+sender_db+" : "+msg_db;
	        											textArea.append(msg_db_gui + "\n");
	        							   				textArea.setCaretPosition(textArea.getText().length());
	        										}
	        		               				}
	        								} catch (SQLException e1) {
	        									// TODO Auto-generated catch block
	        									e1.printStackTrace();
	        								}
	        	               				chat.setVisible(true);
	        	               			}
	                	   			});
			               			mnNewMenu.add(mntmNewMenuItem);
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
	static class RTLS extends Thread{
	      public RTLS() {
	         setName("RTLS");
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
	public static byte[] makepacket(byte cmd, byte[]data){
       byte[]pack = new byte[data.length+3];
       pack[0] = STX;
       pack[1] = cmd;
       System.arraycopy(data, 0, pack, 2, data.length);
       pack[pack.length-1] = ETX;
       return pack;
   	}
	public static byte[] intToBytes( final int i ) {
	    ByteBuffer bb = ByteBuffer.allocate(4); 
	    bb.putInt(i); 
	    return bb.array();
	}
}

class Clientgui_1 extends JFrame {
	int FLYING_UNIT = 10;
	ImageIcon icon;
	public Clientgui_1(int ID) {
		icon = new ImageIcon("C:\\Users\\마상균\\eclipse-workspace\\Clientgui\\src\\RTLS map.png");
		
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 500, 350);
		setTitle("ID : "+ ID);
		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);
		
		Clientgui.mnNewMenu = new JMenu("Chat Other Client");
		menuBar.add(Clientgui.mnNewMenu);
		Clientgui.contentPane = new JPanel(){
            public void paintComponent(Graphics g) {
                Dimension d = getSize();
                g.drawImage(icon.getImage(), 0, 0, d.width, d.height, null);
            }
        };
        
		Clientgui.contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(Clientgui.contentPane);
		Clientgui.contentPane.setLayout(null);
		
		Clientgui.contentPane.addKeyListener(new MyKeyListener());
		Clientgui.x = 394;
		Clientgui.y = 225;
		Clientgui.location.setLocation(Clientgui.x, Clientgui.y);
		
		
        
		Clientgui.location.setSize(100, 20);
		Clientgui.contentPane.add(Clientgui.location);
		Clientgui.contentPane.setFocusable(true);
		Clientgui.contentPane.requestFocus();
	}
	class MyKeyListener extends KeyAdapter{
	    public void keyPressed(KeyEvent e){
	        int keyCode = e.getKeyCode();
	        switch (keyCode) {
	        case KeyEvent.VK_UP:
	        	Clientgui.x=Clientgui.location.getX();
	        	Clientgui.y=Clientgui.location.getY() - FLYING_UNIT;
	        	Clientgui.location.setLocation(Clientgui.x, Clientgui.y);
	        	Clientgui.state = state_check(Clientgui.x,Clientgui.y);
	            break;
	        case KeyEvent.VK_DOWN:
	        	Clientgui.x=Clientgui.location.getX();
	        	Clientgui.y=Clientgui.location.getY() + FLYING_UNIT;
	        	Clientgui.location.setLocation(Clientgui.x, Clientgui.y);
	        	Clientgui.state = state_check(Clientgui.x,Clientgui.y);
	            break;
	        case KeyEvent.VK_LEFT:
	        	Clientgui.x=Clientgui.location.getX() - FLYING_UNIT;
	        	Clientgui.y=Clientgui.location.getY();
	        	Clientgui.location.setLocation(Clientgui.x, Clientgui.y);
	        	Clientgui.state = state_check(Clientgui.x,Clientgui.y);
	            break;
	        case KeyEvent.VK_RIGHT:
	        	Clientgui.x=Clientgui.location.getX() + FLYING_UNIT;
	        	Clientgui.y=Clientgui.location.getY();
	        	Clientgui.location.setLocation(Clientgui.x, Clientgui.y);
	        	Clientgui.state = state_check(Clientgui.x,Clientgui.y);
	            break;
	        }
	    }
	}
	public static byte state_check(int x,int y){
		if(0<=x&&x<=100&&0<=y&&y<=110)
		{
			Clientgui.location.setForeground(Color.RED);//state가 danger일때 빨간색으로 교체
			return Clientgui.danger;
		}
		else if(360<=x&&x<=500&&0<=y&&y<=110)
		{
			Clientgui.location.setForeground(Color.RED);//state가 danger일때 빨간색으로 교체
			return Clientgui.danger;
		}
		else if(160<=x&&x<=310&&160<=y&&y<=270)
		{
			Clientgui.location.setForeground(Color.RED);//state가 danger일때 빨간색으로 교체
			return Clientgui.danger;
		}
		Clientgui.location.setForeground(Color.BLACK);//state가 danger일때 빨간색으로 교체
		return Clientgui.normal;
	}
}
