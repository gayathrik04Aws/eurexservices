package com.prisma.eurex.util;
import java.text.SimpleDateFormat;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;
import com.prisma.eurex.pojo.State;


public class XMLResponseUtil {

	
	
	public  static String getStateResponse(String method,State state) throws Exception{


		
		Element elem = new Element("Response");
		Document document = new Document(elem);
		document.setRootElement(elem);
		elem.setAttribute("commandType", method);
		elem.setAttribute("status", "success");
		elem.setAttribute("code", "0");
		elem.setAttribute("message", "success");
		Element stateElem = new Element("State");
		elem.addContent(stateElem);
		stateElem.setAttribute("state", state.getStatus());
		Element dataSetDateElem = new Element("DatasetDate");
		if( state.getDatasetDate() != null ){
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
			
			dataSetDateElem.setText(dateFormat.format(state.getDatasetDate()));
		}else{
			dataSetDateElem.setText("");
		}
		stateElem.addContent(dataSetDateElem);
		
		XMLOutputter xmOut = new XMLOutputter(); 
		return xmOut.outputString(document);

	}
	
	public  static String getLoadMarginDataResponse(String method,String status,String statusCode, String message) throws Exception{
		Element elem = new Element("Response");
		Document document = new Document(elem);
		document.setRootElement(elem);
		elem.setAttribute("commandType", method);
		elem.setAttribute("status", status);
		elem.setAttribute("code", statusCode);
		elem.setAttribute("message", message);

		XMLOutputter xmOut = new XMLOutputter(); 
		return xmOut.outputString(document);

	}
	
	public static Document getResponseElement(String method) throws Exception{
		Element elem = new Element("Response");
		Document document = new Document(elem);
		document.setRootElement(elem);
		elem.setAttribute("commandType", method);
		elem.setAttribute("status", "success");
		elem.setAttribute("code", "0");
		elem.setAttribute("message", "success");
		return document;
	}
	
	public static String getResponseAsString(Document document,String status,String statusCode, String message)throws Exception{
		Element elem = document.getRootElement();
		elem.setAttribute("status", status);
		elem.setAttribute("code", statusCode);
		elem.setAttribute("message", message);
		XMLOutputter xmOut = new XMLOutputter(); 
		return xmOut.outputString(document);
	}
}
