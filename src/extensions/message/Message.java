package extensions.message;

import java.net.InetAddress;

import framework.plugin.IMessage;

public class Message implements IMessage {
	private String author;
	private String plainText;
	private InetAddress address;
	
	public InetAddress getAddress() {
		return address;
	}

	public void setAddress(InetAddress address) {
		this.address = address;
	}

	public String getAuthor() {
		return author;
	}
	
	public void setAuthor(String author) {
		this.author = author;
	}
	
	public String getPlainText() {
		return plainText;
	}
	
	public void setPlainText(String plainText) {
		this.plainText = plainText;
	}
	
	
}