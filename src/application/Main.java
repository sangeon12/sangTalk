
package application;
	
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import views.Client;


public class Main extends Application {
	
	public Button startAndStop;
	public Label state;
	
	public static ExecutorService threadPool;
	public static Vector<Client> clients = new Vector<Client>();
	
	ServerSocket serverSocket;
	private int currentId = 0;
	
	public static int status = 0; //게임상태를 저장 0이면 중지 1이면 게임시작
	public static int turn = 0; //현재 메시지를 쓸 수 있는 클라이언
	public static int time = 0;
	public static boolean timeSeve = true;//시간 안에 썻으면 쓰레드 종료
	
	public static String endWord = "";
	
	public static ArrayList<String> arrWord = new ArrayList<>();//이미 사용한 단어 저장
	public static ArrayList<String> userList = new ArrayList<>();//현재 접속한 사람 이름
	
	public static long sec = 0;
	public static String overUser;
	public static boolean no = false;
	
	String IP = "127.0.0.1";
	
	//서버를 시작하는 매소드
	public void startServer(String IP, int port) {
		String ip = "127.0.0.1";
		try {
			DatagramSocket soc = new DatagramSocket();
			soc.connect(InetAddress.getByName("8.8.8.8"), 10002);
			ip = soc.getLocalAddress().getHostAddress();
		} catch (Exception e) {
			ip = "127.0.0.1";
		}
		IP = ip;
		try {
			serverSocket = new ServerSocket();
			serverSocket.bind(new InetSocketAddress(IP, port));
		} catch (Exception e) {
			e.printStackTrace();
			if(!serverSocket.isClosed()) {
				stopServer();
			}
			return;
		}
		
		//클라이언트가 접속할때까지 존버
		Runnable thread = new Runnable() {
			@Override
			public void run() {
				while(true) {
					try {
						Socket socket = serverSocket.accept();
						currentId++;
						clients.add(new Client(socket, currentId));
//						System.out.println("[클라이언트 접속]"+socket.getRemoteSocketAddress()+": "
//								+Thread.currentThread().getName());
					} catch (Exception e) {
						if(!serverSocket.isClosed()) {
							stopServer();
						}
						break;
					}
				}
			}
		};
		threadPool = Executors.newCachedThreadPool();
		threadPool.submit(thread);
	}
	
	//서버를 중지하는 매소드
	public void stopServer() {
		try {
			//현재 작동중인 모든 소캣 닫기
			Iterator<Client> iterator = clients.iterator();
			while(iterator.hasNext()) {
				Client client = iterator.next();
				client.socket.close();
				iterator.remove();
			}
			
			//서버 소켓 닫기
			if(serverSocket != null && !serverSocket.isClosed()) {
				serverSocket.close();
			}
			
			//쓰레드 종료
			if(threadPool != null && !threadPool.isShutdown()) {
				threadPool.shutdown();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void start(Stage primaryStage) {
		try {
			FXMLLoader loader = new FXMLLoader();
			loader.setLocation(getClass().getResource("/application/ClientLayout.fxml"));
			AnchorPane root = loader.load();
			
			Scene scene = new Scene(root);
			scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
			primaryStage.setScene(scene);
			primaryStage.show();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		launch(args);
	}
	 
	
	
	public int port = 9910;
	public boolean first = false;
	public void bStart() {
		String a = startAndStop.getText();
		String b = state.getText();
		if(a.equals("서버 시작")) {
			if(!first) {
				startServer(IP, port);
				state.setText("서버 시작");
				b = state.getText();
				startAndStop.setText("서버 중지");
				first = true;
			}else if(first){
				startServer(IP, port);
				state.setText(b+"\n서버 시작");
				b = state.getText();
				startAndStop.setText("서버 중지");
			}
		}else if(a.equals("서버 중지")) {
			stopServer();
			state.setText(b+"\n서버 중지");
			b = state.getText();
			startAndStop.setText("서버 시작");
		}
	}
	
}
