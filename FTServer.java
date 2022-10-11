import java.net.*;
import java.io.*;
import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import java.util.*;

class RequestProcessor extends Thread
{
private Socket socket;
private String id;
private FTServerFrame fsf;
RequestProcessor(Socket socket,String id,FTServerFrame fsf)
{
this.socket=socket;
this.id=id;
this.fsf=fsf;
start();
}
public void run()
{
try
{
SwingUtilities.invokeLater(new Runnable(){
public void run()
{
fsf.updateLog("Client connected and id alloted is : "+id);
}
});
InputStream is=socket.getInputStream();
OutputStream os=socket.getOutputStream();

int i,j;
j=0;
i=0;
byte header[]=new  byte[1024];
byte tmp[]=new byte[1024];

int bytesToReceive=1024;
int byteReadCount;
while(j<bytesToReceive)
{
byteReadCount=is.read(tmp);
if(byteReadCount==-1)continue;
for(int k=0;k<byteReadCount;k++)
{
header[i]=tmp[k];
i++;
}
j+=byteReadCount;
}

long lengthOfFile=0;

i=0;
j=1;
while(header[i]!=',')
{
lengthOfFile+=header[i]*j;
i++;
j*=10;
}
i++;

StringBuffer sb=new StringBuffer();

while(i<=1023)
{
sb.append((char)header[i]);
i++;
}
String fileName=sb.toString().trim();
long lof=lengthOfFile;

SwingUtilities.invokeLater(()->{
fsf.updateLog("Receiving File : "+fileName+" of length : "+lof);
});


File file=new File("upload"+File.separator+fileName);
if(file.exists())file.delete();


byte ack[]=new byte[1];
ack[0]=1;
os.write(ack,0,1);
os.flush();



FileOutputStream fos=new FileOutputStream(file); 
int chunkSize=4096;
byte bytes[]=new byte[chunkSize];
long m=0;

while(m<lengthOfFile)
{
byteReadCount=is.read(bytes);
if(byteReadCount==-1)continue;
fos.write(bytes,0,byteReadCount);
fos.flush();
m+=byteReadCount;
}
fos.close();

ack[0]=1;
os.write(ack,0,1);
os.flush();

socket.close();

SwingUtilities.invokeLater(()->{
fsf.updateLog("File saved to "+file.getAbsolutePath());
fsf.updateLog("Connection with client whose id is : "+id+" close");
});

}catch(Exception e)
{
System.out.println(e);
}
}
}


class FTServer extends Thread
{
private ServerSocket serverSocket;
private FTServerFrame fsf;
FTServer(FTServerFrame fsf)
{
this.fsf=fsf;
}


public void run()
{
try
{
serverSocket=new ServerSocket(5500);
startListening();
}catch(Exception e)
{
System.out.println(e);
}
}
public void shutDown()
{
try
{
serverSocket.close();
}catch(Exception e)
{
System.out.println(e); // romove after testing
}
}


private void startListening()
{
try
{
Socket socket;
RequestProcessor requestProcessor;
while(true)
{
SwingUtilities.invokeLater(new Thread(){
public void run()
{
fsf.updateLog("Server is ready to accept request on port 5500");
}
});
socket=serverSocket.accept();
requestProcessor=new RequestProcessor(socket,UUID.randomUUID().	toString(),fsf);
}
}catch(Exception e)
{
System.out.println(e);
}
}
}

class FTServerFrame extends JFrame implements ActionListener
{
private FTServer server;
private JButton button;
private JTextArea jta;
private JScrollPane jsp;
private Container container;
private boolean serverState=false;
FTServerFrame()
{
container=getContentPane();
container.setLayout(new BorderLayout());
button=new JButton("Start");
jta=new JTextArea();
jsp=new JScrollPane(jta,ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
container.add(jsp,BorderLayout.CENTER);
container.add(button,BorderLayout.SOUTH);
button.addActionListener(this);
setLocation(400,100);
setSize(500,500);
setVisible(true);
}

public void updateLog(String message)
{
jta.append(message+"\n");
}
public void actionPerformed(ActionEvent ae)
{
if(serverState==false)
{
server=new FTServer(this);
server.start();
serverState=true;
button.setText("Stop");
}
else
{
server.shutDown();
serverState=false;
jta.append("Server stopped\n");
button.setText("Start");

}
}

public static void main(String gg[])
{
FTServerFrame fsf=new FTServerFrame();
}
}