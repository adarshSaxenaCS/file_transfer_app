import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;

class FileUploadEvent
{
private String uploaderId;
private File file;
private long numberOfBytesUploaded;
public FileUploadEvent()
{
this.uploaderId=null;
this.file=null;
this.numberOfBytesUploaded=0;
}

public void setUploaderId(String uploaderId)
{
this.uploaderId=uploaderId;
}
public String getUploaderId()
{
return this.uploaderId;
}

public void setFile(File file)
{
this.file=file;
}
public File getFile()
{
return this.file;
}

public void setNumberOfBytesUploaded(long numberOfBytesUploaded)
{
this.numberOfBytesUploaded=numberOfBytesUploaded;
}
public long getNumberOfBytesUploaded()
{
return this.numberOfBytesUploaded;
}

}

interface FileUploadListener
{
public void fileUploadStatesChanged(FileUploadEvent fileUploadEvent);
}



class FileModel extends AbstractTableModel
{
private ArrayList<File> files;
FileModel()
{
files=new ArrayList<>();
}
public int getRowCount()
{
return this.files.size();
}
public int getColumnCount()
{
return 2;
}
public String getColumnName(int index)
{
if(index==0)return "S.No";
return "Files";
}
public Class getColumnClass(int index)
{
if(index==0)return Integer.class;
return String.class;
}
public boolean isEditable(int row ,int column)
{
return false;
}
public Object getValueAt(int row,int column)
{
if(column==0)return row+1;
return this.files.get(row).getAbsolutePath();
}
public void add(File file)
{
this.files.add(file);
fireTableDataChanged();
}
public ArrayList<File> getFiles()
{
return files;
}
}

class FTClientFrame extends JFrame
{ 
private String host;
private int portNumber;
private FileSelectionPanel fileSelectionPanel;
private FileUploadViewPanel fileUploadViewPanel;
private Container container;


FTClientFrame(String host,int portNumber)
{
this.host=host;
this.portNumber=portNumber;
this.fileSelectionPanel=new FileSelectionPanel();
this.fileUploadViewPanel=new FileUploadViewPanel();
container=getContentPane();
container.setLayout(new GridLayout(1,2));
container.add(fileSelectionPanel);
container.add(fileUploadViewPanel);
Dimension d=Toolkit.getDefaultToolkit().getScreenSize();
int width=900;
int height=600;
int x=(d.width/2)-(width/2);
int y=(d.height/2)-(height/2);
setLocation(x,y);
setSize(width,height);
setVisible(true);
setDefaultCloseOperation(EXIT_ON_CLOSE);
}

class FileSelectionPanel extends JPanel implements ActionListener
{
private JLabel titleLabel;
private FileModel model;
private JTable table;
private JScrollPane jsp;
private JButton addFileButton;
private Set<File> files;
FileSelectionPanel()
{
setLayout(new BorderLayout());
titleLabel=new JLabel("Selected Files");
model=new FileModel();
table=new JTable(model);
jsp=new JScrollPane(table,ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
addFileButton=new JButton("Add Files");

Font titleFont=new Font("TIMES NEW ROMAN",23,24);
Font tableHeaderFont=new Font("TIMES NEW ROMAN",Font.BOLD,18);
Font dataFont=new Font("TIMES NEW ROMAN",Font.PLAIN,16);

titleLabel.setFont(titleFont);
table.getColumnModel().getColumn(0).setPreferredWidth(40);
table.getColumnModel().getColumn(1).setPreferredWidth(300);

JTableHeader tableHeader=table.getTableHeader();
tableHeader.setFont(tableHeaderFont);
tableHeader.setResizingAllowed(false);
tableHeader.setReorderingAllowed(false);
table.setFont(dataFont);
addFileButton.addActionListener(this);
add(titleLabel,BorderLayout.NORTH);
add(jsp,BorderLayout.CENTER);
add(addFileButton,BorderLayout.SOUTH);
}
public void actionPerformed(ActionEvent ae)
{             
JFileChooser jfc=new JFileChooser();
jfc.setCurrentDirectory(new File("."));
int selectedOption=jfc.showOpenDialog(this);
if(selectedOption==jfc.APPROVE_OPTION)
{
File selectedFile=jfc.getSelectedFile();
model.add(selectedFile);
}
}
public ArrayList<File> getFiles()
{
return model.getFiles();
}
}// ends inner class FileSelectionPanel

class FileUploadViewPanel extends JPanel implements ActionListener,FileUploadListener
{
private JButton uploadFilesButton;
private JPanel progressPanelsContainer;
private JScrollPane jsp;
private ArrayList<ProgressPanel> progressPanels;
private ArrayList<File> files;
private ArrayList<FileUploadThread> fileUploaders;
FileUploadViewPanel()
{
uploadFilesButton=new JButton("Upload Files");
setLayout(new BorderLayout());
add(uploadFilesButton,BorderLayout.NORTH);
uploadFilesButton.addActionListener(this);


}
public void actionPerformed(ActionEvent ev)
{
files=fileSelectionPanel.getFiles();
if(files.size()==0)
{
JOptionPane.showMessageDialog(FTClientFrame.this,"No files selected to upload");
return;
}
progressPanelsContainer=new JPanel();
progressPanelsContainer.setLayout(new GridLayout(files.size(),1));
ProgressPanel progressPanel;
progressPanels=new ArrayList<>();
fileUploaders=new ArrayList<>();
FileUploadThread fut;
String uploaderId;
for(File file:files)
{
uploaderId=UUID.randomUUID().toString();
progressPanel=new ProgressPanel(uploaderId,file);
progressPanels.add(progressPanel);
progressPanelsContainer.add(progressPanel);
fut=new FileUploadThread(this,uploaderId,file,host,portNumber);
fileUploaders.add(fut);
}
jsp=new JScrollPane(progressPanelsContainer,ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
add(jsp,BorderLayout.CENTER);
this.revalidate();
this.repaint();
for(FileUploadThread fileUploadThread:fileUploaders)
{
fileUploadThread.start();
}
}
public void fileUploadStatesChanged(FileUploadEvent fileUploadEvent)
{
String uploaderId=fileUploadEvent.getUploaderId();
long numberOfBytesUploaded=fileUploadEvent.getNumberOfBytesUploaded();
File file=fileUploadEvent.getFile();
for(ProgressPanel progressPanel:progressPanels)
{
if(progressPanel.getId().equals(uploaderId))
{
progressPanel.updateProgressBar(numberOfBytesUploaded);
break;
}
}
}

class ProgressPanel extends JPanel
{
private String id;
private File file;
private JLabel fileNameLabel;
private JProgressBar progressBar;
private long fileLength;
public ProgressPanel(String id,File file)
{
this.id=id;
this.file=file;
this.fileLength=file.length();
fileNameLabel=new JLabel("Uploading "+file.getAbsolutePath());
progressBar=new JProgressBar(1,100);
setLayout(new GridLayout(2,1));
add(fileNameLabel);
add(progressBar);
}

public String getId()
{
return id;
}

public void updateProgressBar(long bytesUploaded)
{
int percentage;
if(bytesUploaded==fileLength)percentage=100;
else percentage=(int)((bytesUploaded*100)/fileLength);
progressBar.setValue(percentage);
if(percentage==100)
{
fileNameLabel.setText("Uploaded : "+file.getAbsolutePath());
}
}



}// ends inner class ProgressPanel

} //  ends inner class FileUploadViewPanel
public static void main(String gg[])
{
FTClientFrame fcf=new FTClientFrame("localhost",5500);
}
}// ends outer class 

class FileUploadThread extends Thread
{
private FileUploadListener fileUploadListener;
private String id;
private File file;
private String host;
private int portNumber;

FileUploadThread(FileUploadListener fileUploadListener,String id,File file,String host,int portNumber)
{
this.fileUploadListener=fileUploadListener;
this.id=id;
this.file=file;
this.host=host;
this.portNumber=portNumber;
}

public void run()
{
try
{
Socket socket=new Socket(host,portNumber);
InputStream is=socket.getInputStream();
OutputStream os=socket.getOutputStream();

long lengthOfFile=file.length();
String name=file.getName();

int i,j;
long x,k;

i=0;
x=lengthOfFile;

byte header[]=new byte[1024];
while(x>0)
{
header[i]=(byte)(x%10);
x=x/10;
i++;
}
header[i]=(byte)',';
i++;
k=name.length();
j=0;
while(j<k)
{
header[i]=(byte)name.charAt(j);
j++;
i++;
}
while(i<=1023)
{
header[i]=(byte)32;
i++;
}
os.write(header,0,1024);
os.flush();

byte ack[]=new byte[1];
int byteReadCount;
while(true)
{
byteReadCount=is.read(ack);
if(byteReadCount==-1)continue;
break;
}

FileInputStream fis=new FileInputStream(file);
int chunkSize=4096;
byte bytes[]=new byte[chunkSize];
k=0;
while(k<lengthOfFile)
{
byteReadCount=fis.read(bytes);
if(byteReadCount==-1)continue;
os.write(bytes,0,byteReadCount);
os.flush();
k+=byteReadCount;
long brc=k;
SwingUtilities.invokeLater(()->{
FileUploadEvent fue=new FileUploadEvent();
fue.setUploaderId(id);
fue.setFile(file);
fue.setNumberOfBytesUploaded(brc);
fileUploadListener.fileUploadStatesChanged(fue);
});
}
fis.close();
while(true)
{
byteReadCount=is.read(ack);
if(byteReadCount==-1)continue;
break;
}
socket.close();
}catch(Exception e)
{
System.out.println(e);
}
}
}