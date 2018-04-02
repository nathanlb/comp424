package student_player;

import java.util.Map;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

import student_player.StudentPlayer.Node;
import tablut.TablutBoardState;

public class MyTools {
	
    public static double getSomething() {
        return Math.random();
    }
    
    public static String treeToXML(Map<TablutBoardState, Node> m){
    	
    	XStream xstream = new XStream(new DomDriver());
    	String xml = xstream.toXML(m);
    	return xml;
    }
    
    @SuppressWarnings("unchecked")
	public static Map<TablutBoardState, Node> XMLToTree(String xml){
    	XStream xstream = new XStream(new DomDriver());
    	Map<TablutBoardState, Node> m = (Map<TablutBoardState, Node>)xstream.fromXML(xml);
    	return m;
    }
    
    
}
