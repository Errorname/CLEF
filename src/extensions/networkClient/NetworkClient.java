package extensions.networkClient;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

import javax.sql.rowset.serial.SerialArray;

import framework.Framework;
import framework.plugin.IGUI;
import framework.plugin.INetworkClient;

public class NetworkClient implements INetworkClient {
	
	private int port;
	
	private Socket socket;
	private Scanner input;
	private PrintWriter output;
	
	private IGUI gui;
	
	
	public NetworkClient() throws IOException {
		gui = (IGUI) Framework.getExtension(IGUI.class);
	}

	public void send(String message) {
		output.println(message);
	}
	
	public String read() throws IOException {
		return input.nextLine();
	}

	@Override
	public String getClientName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setClientName(String clientName) {
		// TODO Auto-generated method stub
		
	}
	
	public void setPort(int port){
		this.port = port;
	}
	
	public int getPort(){
		return this.port;
	}

	public void setServer(String server) {
		try {
			socket = new Socket(server,getPort());
			input = new Scanner(new InputStreamReader(socket.getInputStream()));
			output = new PrintWriter(socket.getOutputStream(),true);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		Thread thread = new Thread() {
			public void run () {
				while(true) {
					if (input != null) {
						try {
							String message = read();
							gui.receiveMessage(message);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}
		};
		thread.start();
		
	}

	@Override
	public void handleEvent(String name, Object event) {
		// TODO Auto-generated method stub
		
	}
	
	public void stopThread() {
		try {
			socket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}
