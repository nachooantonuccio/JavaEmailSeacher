import java.io.IOException;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.NoSuchProviderException;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.InternetAddress;
import javax.mail.search.SearchTerm;
 
/**
 * Este programa demuestra como buscar e-mails en un correo de Gmail que satisfagan
 * un criterio de búsqueda específico, en este caso la palabra "DevOps" como lo solicita
 * el ejercicio, tanto en el asunto como en el cuerpo del e-mail.
 * 
 * @author IgnacioAntonuccio
 *
 */
public class EmailSearcher {
 
    /**
     * Busca los e-mails que contengan la palabra clave "DevOps" en
     * el campo asunto o cuerpo del mensaje.
     * @param host
     * @param port
     * @param userName
     * @param password
     * @param keyword
     */
    public void searchEmail(String host, String port, String userName, String password, final String keyword) {
        
    	Properties properties = new Properties();
 
        // configuración del servidor
        properties.put("mail.imap.host", host);
        properties.put("mail.imap.port", port);
 
        // configuración de SSL
        properties.setProperty("mail.imap.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        properties.setProperty("mail.imap.socketFactory.fallback", "false");
        properties.setProperty("mail.imap.socketFactory.port", String.valueOf(port));
        
        properties.put("mail.imap.ssl.checkserveridentity", "false");
        properties.put("mail.imap.ssl.trust", "*");
 
        Session session = Session.getDefaultInstance(properties);
        
        
 
        try {
            // nos conectamos al message store
            Store store = session.getStore("imap");
            store.connect(host, userName, password);
 
            // abrimos la carpeta inbox de mails
            final Folder folderInbox = store.getDefaultFolder().getFolder("Inbox");
            folderInbox.open(Folder.READ_ONLY);
 
            // creamos el criterio de busqueda
            SearchTerm searchCondition = new SearchTerm() {
                @Override
                public boolean match(Message message) {
                    try {
                        if (message.getSubject().contains(keyword) || getText(message).contains(keyword)) {
                            return true;
                        }
                    } catch (MessagingException | IOException ex) {
                        ex.printStackTrace();
                    }
                    return false;
                }

				
				private String getText(Part p) throws MessagingException, IOException {
					
			        if (p.isMimeType("text/*")) {
			            String s = (String) p.getContent();
			 
			            return s;
			        }
			 
			        if (p.isMimeType("multipart/alternative")) {
			        	
			            Multipart mp = (Multipart) p.getContent();
			            String text = null;
			 
			            for (int i = 0; i < mp.getCount(); i++) {
			                Part bp = mp.getBodyPart(i);
			 
			                if (bp.isMimeType("text/plain")) {
			                    if (text == null) {
			                        text = getText(bp);
			                    }
			                } else if (bp.isMimeType("text/html")) {
			                    String s = getText(bp);
			 
			                    if (s != null) {
			                        return s;
			                    }
			                } else {
			                    return getText(bp);
			                }
			            }
			 
			            return text;
			        } else if (p.isMimeType("multipart/*")) {
			            Multipart mp = (Multipart) p.getContent();
			 
			            for (int i = 0; i < mp.getCount(); i++) {
			                String s = getText(mp.getBodyPart(i));
			 
			                if (s != null) {
			                    return s;
			                }
			            }
			        }
			 
			        return null;
			    }
            };
 
            
            Message[] foundMessages = folderInbox.search(searchCondition);

            for (int i = 0; i < foundMessages.length; i++) {
            	
                Message message = foundMessages[i];
                String subject = message.getSubject();
                InternetAddress add = (InternetAddress)message.getFrom()[0];
              	guardarEnDB(message.getReceivedDate(), add.getAddress().toString(), message.getSubject());
                System.out.println("Mail encontrado #" + i + ": " + subject);
            }
 
            // desconectar
            folderInbox.close(false);
            store.close();
        } catch (NoSuchProviderException ex) {
            System.out.println("No provider.");
            ex.printStackTrace();
        } catch (MessagingException ex) {
            System.out.println("No se pudo conectar al message store.");
            ex.printStackTrace();
        }
    }
 

    private static void guardarEnDB(Date receivedDate, String from, String subject) {
    	
    	DateFormat fecha = new SimpleDateFormat("dd/MM/yyyy");
		String fechaConvertida = fecha.format(receivedDate).toString();
    	
    	try
        {
		
            // Se obtiene una conexión con la base de datos. Hay que
            // cambiar el usuario "root" y la clave "la_clave" por las
            // adecuadas a la base de datos que estemos usando.
            Connection conexion = DriverManager.getConnection ("jdbc:mysql://localhost:3306/meli","root", "la_clave");
            
            // Se crea un Statement, para realizar el insert
            Statement s = conexion.createStatement();
                 
            // Se realiza el insert en la base de datos
            String insert = "INSERT INTO `meli`.`EmailsEncontrados` (`FECHA`, `FROM`, `SUBJECT`) VALUES ('"+fechaConvertida.toString()+"', '"+from+"', '"+subject.toString()+"')";
            s.executeUpdate (insert);
            
            
            // Se cierra la conexión con la base de datos.
            conexion.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
		
	}


	public static void main(String[] args) {
        String host = "imap.gmail.com";
        String port = "993";
        String userName = "tumail@gmail.com"; //modificarlo segun cada usuario
        String password = "tucontraseña"; //modificarlo segun cada usuario
        EmailSearcher searcher = new EmailSearcher();
        String keyword = "DevOps";
        searcher.searchEmail(host, port, userName, password, keyword);
    }
 
}
