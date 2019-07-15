//package distributed.MyWebServer;
/*--------------------------------------------------------

1. Name / Date: Yi Zheng / 2.13.2019

2. Java version used, if not the official version for the class:

java se-10

3. Precise command-line compilation examples / instructions:


> javac MyWebServer.java


4. Precise examples / instructions to run this program:

> java MyWebServer

5. List of files needed for running the program.

MyWebServer.java


----------------------------------------------------------*/


import java.io.*;  
import java.net.*; 
import java.util.Scanner;

class WebServerWorker extends Thread {    
	Socket sock;                   
	WebServerWorker (Socket s) {sock = s;} 
	
	String link1 = "<a href=\"";//maybe i should call them wrappers instead, but they are http that will be wrapped around file names
	String link2 = "\">";
	String link3 = "</a> <br>";
	
	public void run(){
		// Get I/O streams from the socket:
		PrintStream out = null;
		BufferedReader in = null;
		try {
			out = new PrintStream(sock.getOutputStream());
			in = new BufferedReader
					(new InputStreamReader(sock.getInputStream()));
			String sockdata;
			String file_name;

			sockdata = in.readLine();
			if(sockdata!=null) {//check for null, otherwise would get null pointer exception
				
				if (sockdata.indexOf("..")!=-1 || sockdata.endsWith("~")) {//this condition added for security reason. cannot allow user to go beyond root directory. However, it seems firefox disallows .. anyway.
					System.out.println("not allowed!");
				}
				
				else if(sockdata.indexOf("favicon") != -1) {//ignore favicon
					//sock.close();
				}
				
				else {
					System.out.println("request string is "+sockdata);
					int end_index = sockdata.indexOf("HTTP") - 1;//slice string to extract file name
					file_name = sockdata.substring(5, end_index);
					System.out.println("file or directory to be returned is "+file_name);

					if (file_name.equals("") || file_name.charAt(file_name.length()-1) == '/') { //file_name.indexOf("/") != -1
						//open file in directory
						display_file_list(out, "./"+file_name);
					}
					else if(file_name.indexOf("addnums.fake-cgi")!=-1) {//case of cgi
						addnums(out, file_name);
					}
					else {
						Scanner scanner = new Scanner(new BufferedInputStream(new FileInputStream(file_name)));//"src/distributed/MyWebServer/"+
						if (file_name.indexOf(".") != -1) {
							String[] split = file_name.split("\\.");
							if (split[1].equals("txt") || split[1].equals("java")) {//txt or java displayed as plain text
								displayTxt(out, scanner);
							}
							else if (split[1].equals("html")) {//case of html
								displayHtml(out, scanner);
							}
						}
						scanner.close();
					}
					
				}
				sock.close();
			}
		} catch (IOException x) {
			System.out.println("Connetion reset. Listening again...");
			System.out.println(x);
		}
	}
	
	private void addnums(PrintStream out, String file_name) {
		int i1 = file_name.indexOf("person=")+7;//get positions of values in the string
		int i2 = file_name.indexOf("&num1=")+6;
		int i3 = file_name.indexOf("&num2=")+6;
		String person = file_name.substring(i1, i2-6);//extract values
		int num1 = Integer.parseInt(file_name.substring(i2,i3-6));
		int num2 = Integer.parseInt(file_name.substring(i3));
		int total = num1 +num2;
		out.println("HTTP/1.1 200 OK");//wrap http around output
		out.println("Content-Length: 16384");
		out.println("Content-Type: text/html");
		out.println("\r\n\r\n");
		out.println("<html>");
		out.println("<pre>");
		out.printf("Dear %s, the sum of %d and %d is %d\n", person, num1, num2, total);
		out.println("</pre>");
		out.println("</html>");
	}
	
	private void displayTxt(PrintStream out, Scanner scanner) {
		out.println("HTTP/1.1 200 OK");
		out.println("Content-Length: 16384");
		out.println("Content-Type: text/plain");
		out.println("\r\n\r\n");
		while(scanner.hasNext()) {
			out.println(scanner.nextLine());//send output from the file to the client
		}
	}
	
	private void displayHtml(PrintStream out, Scanner scanner) {
		out.println("HTTP/1.1 200 OK");
		out.println("Content-Length: 16384");
		out.println("Content-Type: text/html");
		out.println("\r\n\r\n");
		while(scanner.hasNext()) {
			out.println(scanner.nextLine());
		}
	}
	
	private void display_file_list(PrintStream out, String dir) {
		File f1 = new File(dir);//"src/distributed/MyWebServer"
		File[] strFilesDirs = f1.listFiles();
		out.println("HTTP/1.1 200 OK");
		out.println("Content-Length: 16384");
		out.println("Content-Type: text/html");
		out.println("\r\n\r\n");
		if(!dir.equals("./")) {
			out.println(link1+"../"+link2+"Parent Directory"+link3);//case of parent directory
		}
		for (int i=0; i<strFilesDirs.length; i++) {
			String name = strFilesDirs[i].toString();
			String label = strFilesDirs[i].toString();
			if(name.length()-name.replace("/", "").length()>0) {
				String[] temp = name.split("/");
				label = temp[temp.length-1];	//just want the last part of the path so there won't be problem when concatenating
			}
			if (strFilesDirs[i].isDirectory()) {
				out.println(link1+label+"/"+link2+label+"/"+link3);
			}
			else if (strFilesDirs[i].isFile()) {
				out.println(link1+label+link2+label+link3);
			}
		}
	}
}

public class MyWebServer {
	public static boolean controlSwitch = true;

	public static void main(String a[]) throws IOException {
		int q_len = 6; /* Number of requests for OpSys to queue */
		int port = 2540;
		Socket sock;

		ServerSocket servsock = new ServerSocket(port, q_len);
		
		System.out.println("Yi's web server running at 2540.\n");

		while (controlSwitch) {
			// wait for the next client connection:
			sock = servsock.accept();
			new WebServerWorker (sock).start(); // Uncomment to see shutdown bug:
			// try{Thread.sleep(10000);} catch(InterruptedException ex) {}
		}
	}
}
