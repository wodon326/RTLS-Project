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
	static InputStream  is = null;
	static ObjectInputStream ois;
	
	static byte[] buf = new byte[512];
	static Socket socket;
	
	private static JPanel contentPane;
	ImageIcon icon;
	
	static final byte STX = (byte)0x02;
	static final byte ETX = (byte)0x03;
	static final byte CMD_RTDATA = (byte)0x00;
	static final byte CMD_ALLSTAT = (byte)0x01;
	static final byte CMD_MSG = (byte)0x02;
	static final byte CMD_LOGIN = (byte)0x10;
	static final byte CMD_LIST = (byte)0x11;
	static int id;
	static int x;
	static int y;
	static byte state = (byte)0x00;
	static byte normal = (byte)0x00;
	static byte danger = (byte)0xFF;
	static HashMap<Integer,JLabel> client_location = new HashMap<Integer,JLabel>();
	
	static JMenu Client_path;
	static JMenuItem MenuItem_Client_path;
	
	static Connection conn;
	static Statement stmt = null;
	static ResultSet srs; 
	
	static int line_num=0;
	static int xArray[] = null; 
	static int yArray[] = null; 
	static Queue<Pair> queue = new LinkedList<>();
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
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 500, 350);
		setTitle("monitoring");
		
		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);
		
		Client_path = new JMenu("Client_path");
		menuBar.add(Client_path);
		
		//배경 그리기
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
		   public monitor() {
			   setName("monitor");
		   }
		   @Override
		   public void run() {
			   while(true)
			   {
				   int client_num=0;
				   
				   try {
					buf = (byte[])ois.readObject();
					if(buf[0]==STX&&buf[buf.length-1]==ETX)
					{
						switch(buf[1]){
		                   case CMD_ALLSTAT:
		                	   client_num = (int)buf[2];
		                	   for(int i=0;i<client_num;i++)
		                	   {
		                		   byte[]byte_recode = new byte[10];
		                		   byte[]byte_int = new byte[4];
		                		   System.arraycopy(buf, 3+i*10, byte_recode, 0, 10);
		                		   id = (int)byte_recode[0];
		                		   state = byte_recode[1];
		                		   System.arraycopy(byte_recode, 2, byte_int, 0, 4);
		                		   x=ByteBuffer.wrap(byte_int).getInt();
		                           System.arraycopy(byte_recode, 6, byte_int, 0, 4);
		                           y=ByteBuffer.wrap(byte_int).getInt();
		                           if(client_location.containsKey(id))
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
		                           else {
		                        	   JLabel lblNewLabel = new JLabel(Integer.toString(id));
			                           lblNewLabel.setBounds(x, y, 57, 15);
			                           if(state == danger)
		                        	   {
			                        	   lblNewLabel.setForeground(Color.RED);
		                        	   }
		                        	   else
		                        	   {
		                        		   lblNewLabel.setForeground(Color.BLACK);
		                        	   }
			                           contentPane.add(lblNewLabel);
			                           client_location.put(id, lblNewLabel);
			                           MenuItem_Client_path = new JMenuItem( Integer.toString(id));
			                           MenuItem_Client_path.addActionListener(new ActionListener() {
				               				public void actionPerformed(ActionEvent e) {
				               					try {
				               						int path_id = Integer.parseInt(e.getActionCommand());
													srs = stmt.executeQuery("select * from rtls");
									            	line_num=0;
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
													xArray = new int[line_num]; 
									            	yArray = new int[line_num]; 
									            	int n=0;
													while(!queue.isEmpty())
													{
														Pair pair = (Pair)queue.poll();
														xArray[n] = pair.getX();
														yArray[n] = pair.getY();
														n++;
													}
													Client_path client_path = new Client_path(xArray,yArray,line_num,(JLabel)client_location.get(path_id));
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
		setBounds(100, 100, 500, 324);
		icon = new ImageIcon("C:\\Users\\마상균\\eclipse-workspace\\Clientgui\\src\\RTLS map.png");
		setTitle("ID : "+ id.getText()+" Path");
		
		contentPane_path = new JPanel(){
            public void paintComponent(Graphics g) {
                Dimension d = getSize();
                g.drawImage(icon.getImage(), 0, 0, d.width, d.height, null);
            	g.drawPolyline(X_Array, Y_Array, Line_Num);
            }
        };
        contentPane_path.add(id);
        contentPane_path.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane_path);
		contentPane_path.setLayout(null);
	}
}
