package student_player;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

import com.thoughtworks.xstream.XStream;

public class MyTools {
	
	private static XStream xstream = new XStream();
	private static String filename = "data"+File.separator+"data.xml";
	
	public static boolean saveToXMLwithXStream(Object obj) {
		initXStream();
		String xml = xstream.toXML(obj);
		
		try {
			FileWriter writer = new FileWriter(filename);
			writer.write(xml);
			writer.close();
			return true;
		} catch (IOException e){
			e.printStackTrace();
			return false;
		}
	}
	
	public static Object loadFromXMLwithXStream(){
		initXStream();
		try {
			FileReader fileReader = new FileReader(filename);
			return xstream.fromXML(fileReader);
		} catch (IOException e){
			e.printStackTrace();
			return null;
		}
	}
	
	private static void initXStream(){
		xstream.setMode(XStream.ID_REFERENCES);
		Class<?>[] classes = new Class[] {Map.class};
		XStream xStream = new XStream();
		XStream.setupDefaultSecurity(xStream);
		xStream.allowTypes(classes);
	}
	
	public static void setAlias(String xmlTagName, Class<?> className) {
		xstream.alias(xmlTagName, className);
	}
	
	public static void setFilename(String fn) {
		filename = fn;
	}
}
