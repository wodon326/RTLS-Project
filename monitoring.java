import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.awt.event.*;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.JMenuBar;


public class monitoring extends JFrame {
	//통신할 때 데이터를 받을 때 필요한 변수
	static InputStream  is = null;
	static ObjectInputStream ois;
	static Socket socket;
	static JPanel contentPane;
	static final byte STX = (byte)0x02;
	static final byte ETX = (byte)0x03;
	static final byte CMD_RTDATA = (byte)0x00;
	static final byte CMD_ALLSTAT = (byte)0x01;
	static final byte CMD_MSG = (byte)0x02;
	static final byte CMD_LOGIN = (byte)0x10;
	static final byte CMD_LIST = (byte)0x11;
	static final byte normal = (byte)0x00;
	static final byte danger = (byte)0xFF;
	static HashMap<Integer,JLabel> client_location = new HashMap<Integer,JLabel>();
	static JMenu Client_path;
	//데이터베이스 변수
	static Connection conn;
	static Statement stmt = null;
	static ResultSet srs; 
	
	/**
	 * Launch the application.
	 * @throws IOException 
	 * @throws UnknownHostException 
	 */
	public static void main(String[] args) throws UnknownHostException, IOException {
		socket = new Socket("localhost",3001);
		is = socket.getInputStream();
	    ois = new ObjectInputStream(is);
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					monitoring frame = new monitoring();
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
		monitor thread = new monitor();
		thread.start();
	}

	/**
	 * Create the frame.
	 */
	public monitoring() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);//monitoring 모두 종료
		setBounds(100, 100, 500, 350);
		setTitle("monitoring");
		
		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);
		
		Client_path = new JMenu("Client_path");
		menuBar.add(Client_path);
		
		//배경 그리기
		ImageIcon icon;
		icon = new ImageIcon("C:\\Users\\마상균\\eclipse-workspace\\Clientgui\\src\\RTLS map.png");
		contentPane = new JPanel(){
            public void paintComponent(Graphics g) {
                Dimension d = getSize();
                g.drawImage(icon.getImage(), 0, 0, d.width, d.height, null);
            }
        };
		
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);
	}
	
	static class monitor extends Thread{
			byte[] buf = new byte[512];
			JLabel Client_Label;
			int client_num=0;
			byte[]byte_recode = new byte[10];
			byte[]byte_int = new byte[4];
			int line_num=0;
			int xArray[] = null; 
			int yArray[] = null; 
			int id;
			int x;
			int y;
			byte state = (byte)0x00;
			Queue<Pair> queue = new LinkedList<>();
			JMenuItem MenuItem_Client_path;
			
			public monitor() {
			   setName("monitor");
			}
			@Override
			public void run() {
				while(true)
				{
				   client_num=0;
				   
				   try {
					buf = (byte[])ois.readObject();
					if(buf[0]==STX&&buf[buf.length-1]==ETX)
					{
						switch(buf[1]){
		                   case CMD_ALLSTAT://클라이언트들의 모든 상태와 위치가 담긴 패킷을 분석
		                	   client_num = (int)buf[2];
		                	   //클라이언트 수만큼 상태와 위치 분석
		                	   for(int i=0;i<client_num;i++)
		                	   {
		                		   System.arraycopy(buf, 3+i*10, byte_recode, 0, 10);
		                		   id = (int)byte_recode[0];
		                		   state = byte_recode[1];
		                		   System.arraycopy(byte_recode, 2, byte_int, 0, 4);
		                		   x=ByteBuffer.wrap(byte_int).getInt();
		                           System.arraycopy(byte_recode, 6, byte_int, 0, 4);
		                           y=ByteBuffer.wrap(byte_int).getInt();
		                           
		                           //클라이언트의 상태와 위치가 담긴 JLabel을 hashmap에 넣어서 관리
		                           if(client_location.containsKey(id)) //기존에 모니터링되고있던 클라이언트면 hashmap에서 가져와 상태와 위치 교체
		                           {
		                        	   ((JLabel)client_location.get(id)).setLocation(x, y);
		                        	   if(state == danger)
		                        	   {
		                        		   ((JLabel)client_location.get(id)).setForeground(Color.RED);
		                        	   }
		                        	   else
		                        	   {
		                        		   ((JLabel)client_location.get(id)).setForeground(Color.BLACK);
		                        	   }
		                           }
		                           else { //기존에 모니터링되고있던 클라이언트가 아니면 JLabel을 만들어 hashmap에 넣고 클라이언트의 경로를 구하는 메뉴 추가 
		                        	   Client_Label = new JLabel(Integer.toString(id));
			                           Client_Label.setBounds(x, y, 57, 15);
			                           if(state == danger)
		                        	   {
			                        	   Client_Label.setForeground(Color.RED);
		                        	   }
		                        	   else
		                        	   {
		                        		   Client_Label.setForeground(Color.BLACK);
		                        	   }
			                           contentPane.add(Client_Label);
			                           client_location.put(id, Client_Label);
			                           
			                           //클라이언트의 경로를 구하는 메뉴 추가 
			                           MenuItem_Client_path = new JMenuItem( Integer.toString(id));
			                           //메뉴 클릭했을 때 데이터베이스에서 클라이언트의 위치를 가져오고 Client_path에 경로와 현재 위치와 현재 상태를 띄움
			                           MenuItem_Client_path.addActionListener(new ActionListener() {
				               				public void actionPerformed(ActionEvent e) {
				               					try {
				               						int path_id = Integer.parseInt(e.getActionCommand());
													srs = stmt.executeQuery("select * from rtls");
									            	line_num=0;
									            	//데이터베이스에서 가져온 데이터들의 x,y값을 Pair로 묶어 queue에 넣으며 데이터 개수를 확인
													while (srs.next()) {
														int id_db = srs.getInt("id");
														int x_db = srs.getInt("x");
														int y_db = srs.getInt("y");
														if(id_db==path_id)
														{
															queue.add(new Pair(x_db,y_db));
															line_num++;
														}
						               				}
													//데이터 개수만큼 배열을 생성하고 큐에 쌓인 데이터를 xArray, yArray에 집어넣음
													xArray = new int[line_num]; 
									            	yArray = new int[line_num]; 
									            	int n=0;
													while(!queue.isEmpty())	{
														Pair pair = (Pair)queue.poll();
														xArray[n] = pair.getX();
														yArray[n] = pair.getY();
														n++;
													}
													//클라이언트의 현재상태를 Client_path에 복사하기 위해 새로운 JLabel에 복사 후 Client_path 띄움
													JLabel Client_Path_Label = new JLabel(Integer.toString(path_id));
													Client_Path_Label.setBounds(x, y, 57, 15);
													JLabel Copy_Client = (JLabel)client_location.get(path_id);
													int x_copy=Copy_Client.getX();
										        	int y_copy=Copy_Client.getY();
										        	Client_Path_Label.setLocation(x_copy, y_copy);
										        	Client_Path_Label.setForeground(Copy_Client.getForeground());
													Client_path client_path = new Client_path(xArray,yArray,line_num,Client_Path_Label);
													client_path.setVisible(true);
												} catch (SQLException e1) {
													// TODO Auto-generated catch block
													e1.printStackTrace();
												}
					               			}
				               			});
			                           Client_path.add(MenuItem_Client_path);
		                           }
		                           
		                	   }
		                	   break;
		                   default:
		                	   break;
						}
					}
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			   }
		   }
	   }
	
	//Pair 구현 클래스
	static class Pair {
	    private int x;
	    private int y;
	    Pair(int x, int y) {
	        this.x = x;
	        this.y = y;
	    }
	    public int getX(){
	        return x;
	    }
	    public int getY(){
	        return y;
	    }
	}
}
class Client_path extends JFrame {
	JPanel contentPane_path;
	ImageIcon icon;
	public Client_path(int[]X_Array,int[]Y_Array,int Line_Num,JLabel id) {
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); //Client_path 하나만 종료
		setBounds(100, 100, 500, 324);
		icon = new ImageIcon("C:\\Users\\마상균\\eclipse-workspace\\Clientgui\\src\\RTLS map.png");
		setTitle("ID : "+ id.getText()+" Path");
		//배경을 띄우고 경로를 그림
		contentPane_path = new JPanel(){
            public void paintComponent(Graphics g) {
                Dimension d = getSize();
                g.drawImage(icon.getImage(), 0, 0, d.width, d.height, null);//배경 그리기
            	g.drawPolyline(X_Array, Y_Array, Line_Num);//Client의 경로를 그림
            }
        };
        contentPane_path.add(id);
        contentPane_path.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane_path);
		contentPane_path.setLayout(null);
	}
}
