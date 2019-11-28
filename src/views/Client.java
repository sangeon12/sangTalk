package views;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URLEncoder;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import application.Main;

public class Client {
	
	public Socket socket;
	public int id;
	public String name;
	
	public Client(Socket socket, int id) {
		this.socket = socket;
		this.id = id;
		receive();
	}
	
	//메세지를 받는 매소드
	public void receive() {
		Runnable thread = new Runnable() {
			@Override
			public void run() {
				try {
					while(true) {
						InputStream in = socket.getInputStream();
						byte[] buffer = new byte[512];
						int length = in.read(buffer);
						while(length == -1) throw new IOException();
//						System.out.println("[메세지 수신 성공]"+socket.getRemoteSocketAddress()+": "
//								+Thread.currentThread().getName());
						String message = new String(buffer, 0, length, "UTF-8");
						
						String[] cmdArr = message.split("::");
						
						switch(cmdArr[0]) {
						case "SYSTEM":
							if(cmdArr[1].equals("LOGIN")) {
								send("SYSTEM::LOGIN::" + id);
								for(Client  mainController : Main.clients) {
									mainController.send("MSG::[" + cmdArr[2] + "]님이 로그인하셨습니다.\n");
								}
								name = cmdArr[2];
								Main.userList.add(cmdArr[2]);
							}
							
							if(cmdArr[1].equals("LISTDEL")) {
								for(Client  mainController : Main.clients) {
									mainController.send("MSG::[" + cmdArr[2] + "]님이 퇴장하셨습니다.\n");
								}
								send("SYSTEM::STOPSERVER");
								listDel(cmdArr[2]);
							}
							
							if(Main.status == 0) {
								if(cmdArr[1].equals("ENDWORDGAME")) {
									if(Main.userList.size() <= 1) {
										send("MSG::[서버 메세지]: 접속자가 2명 이상일때만 게임을 시작할 수 있습니다\n");
									}else {
										Main.status = 1;
										Main.turn = 0;
										Main.endWord = "";
										Main.sec = 0;
										Main.arrWord.clear();
										Main.timeSeve = true;
										time();
										for(Client  mainController : Main.clients) {
											mainController.send("MSG::[" + name + "]님이 게임을 시작하셨습니다.\n[서버 메세지]: 게임 순서는 접속한 순서대로 입니다.\n");
											mainController.send("SYSTEM::START");
										}
										Main.overUser = name;
									}
								}else if(cmdArr[1].equals("COMMAND")) {
									send("MSG::[서버 메세지]: /끝말잇기, /사용자\n");
								}else if(cmdArr[1].equals("USER")) {
									user();
								}
							}else{
							}
							
							break;
						case "MSG":
							if(Main.status == 0) {
								for(Client  mainController : Main.clients) {
									mainController.send("MSG::[" + name +"]: " + cmdArr[1]+"\n");
								}
							}else {
								for(int i = 0; i < Main.clients.size(); i++) {
									if(Main.clients.get(i).id == id && i == Main.turn) {
										if(Main.endWord.isEmpty()) {
											if(dictionary(cmdArr[1])) {
												Main.endWord = cmdArr[1].substring(cmdArr[1].length()-1, 
														cmdArr[1].length());
												for(Client  mainController : Main.clients) {
													mainController.send("MSG::[" + name +"]: " + cmdArr[1]+"\n");
													mainController.send("SYSTEM::RETURN");
												}
												Main.arrWord.add(cmdArr[1]);
												Main.sec = 0;
												Main.turn = (Main.turn+1) % Main.clients.size();
												Main.overUser = Main.userList.get(Main.turn);
											}else {
												send("MSG::[서버 메세지]: 없는 단어입니다.\n");
											}
										}else {
											if(cmdArr[1].startsWith(Main.endWord)) {
												if(no(cmdArr[1])) {
													send("MSG::[서버 메세지]: 이미 사용된 단어 입니다.\n");
												}else if(!no(cmdArr[1])) {
													if(dictionary(cmdArr[1])) {
														for(Client  mainController : Main.clients) {
															mainController.send("MSG::[" + name +"]: " + cmdArr[1]+"\n");
															mainController.send("SYSTEM::RETURN");
														}
														Main.endWord = cmdArr[1].substring(cmdArr[1].length()-1, 
																cmdArr[1].length());
														Main.arrWord.add(cmdArr[1]);
														Main.no = false;
														Main.sec = 0;
														Main.turn = (Main.turn+1) % Main.clients.size();
														Main.overUser = Main.userList.get(Main.turn);
													}else {
														send("MSG::[서버 메세지]: 없는 단어입니다.\n");
													}
												}
											}else {
												send("MSG::[서버 메세지]: "+Main.endWord+"로 시작하는 단어를 사용해주세요\n");
											}
										}
										
										
									}else {
										//null
									}
								}
							}
							
							break;
						}
						
						
					}
				} catch (Exception e) {
					try {
//						System.out.println("[메세지 수신 오류]"+socket.getRemoteSocketAddress()+": "
//								+Thread.currentThread().getName());
					} catch (Exception e2) {
						e2.printStackTrace();
					}
				}
			}
		};
		Main.threadPool.submit(thread);
	}
	//메세지를 보내는 메소드
	public void send(String message) {
		Runnable thread = new Runnable() {
			@Override
			public void run() {
				try {
					OutputStream out = socket.getOutputStream();
					byte[] buffer = message.getBytes("UTF-8");
					out.write(buffer);
					out.flush();
				} catch (Exception e) {
					try {
//						System.out.println("[메세지 송신 오류]"+socket.getRemoteSocketAddress()+": "
//								+Thread.currentThread().getName());
						Main.clients.remove(Client.this);
						socket.close();
					} catch (Exception e2) {
						e2.printStackTrace();
					}
				}
			}	
		};
		Main.threadPool.submit(thread);
	}
	
	//제한시간
	public void time() {
		Thread thread = new Thread() {
			public void run() {
				while(true) {
					try {
						Thread.sleep(1000);
						Main.sec++;
					} catch (Exception e) {
						e.printStackTrace();
						System.out.println("쓰레드 오류 발생");
					}
					if(Main.sec==10) {
						for(int j = 0; j < Main.userList.size(); j++) {
							if(Main.overUser.equals(Main.userList.get(j))) {
								Main.overUser = Main.userList.get(j);
							}
						}
						for(Client  mainController : Main.clients) {
							mainController.send("MSG::[서버 메세지]: "+Main.overUser+"님의 패배입니다\n");
						}
						Main.status = 0;
						break;
					}
				}
			}
		};
		thread.start();
	}
		//이미 사용한 단어인지 판별하는 메소드
		public boolean no(String a) {
			for(int j = 0; j < Main.arrWord.size(); j++) {
				if(a.equals(Main.arrWord.get(j))) {
					return true;
				}
			}
			return false;
		}
		//접속한 유저 확인
		public void user() {
			for(int i = 0; i < Main.userList.size(); i++) {
				send("MSG::"+Main.userList.get(i)+"님\n ");
			}
		}
		
		//접속을 종료한 유저를 리스트에서 삭제하는 메소드
		public void listDel(String a) {
			for(int i = 0; i < Main.userList.size(); i++) {
				if(a.equals(Main.userList.get(i))) {
					Main.userList.remove(i);
				}
			}
		}
		//단어가 국어사전에 있는지 판별하는 메소드
		public boolean dictionary(String a) {
			try {
				String dicUrl = "https://dict.naver.com/search.nhn?dicQuery=" + URLEncoder.encode(a, "UTF-8"); 
				Document doc = Jsoup.connect(dicUrl).get(); 
				Element dicWord = doc.selectFirst(".lst_krdic > li .c_b > strong"); // 실제 단어 

				if (dicWord != null && dicWord.text().equals(a)) {
					return true;					
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("국어사전 검사중 오류 발생");
			}
			return false;	
		}
}
