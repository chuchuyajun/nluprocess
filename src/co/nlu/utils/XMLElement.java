package co.nlu.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;


public class XMLElement {
	private Element el;

	public XMLElement(Element el) {
		this.el = el;
	}

	public String getChildText(String path) {
		Element leafChild = getLeafElement(path);
		if(leafChild == null){
			return null;
		}
		return leafChild.getText();
	}

	public String getChildTextWithOutEx(String path) {
		try {
			Element leafChild = getLeafElement(path);
			if(leafChild==null){
				return "";
			}
			return leafChild.getText();
		} catch (RuntimeException e) {
			//e.printStackTrace();
			return "";
		}
	}
	private Element getLeafElement(String path) {
		String[] sps = path.split("/");
		Element leafChild = el;
		for (int i = 0; i < sps.length; i++) {
			String sc = sps[i];
			leafChild = leafChild.getChild(sc);
			if (leafChild == null) {
				return null;
			}
		}
		return leafChild;
	}
	
//	public XMLElement getElement(String path) {
//		Element leafChild = getLeafElement(path);
//		return new XMLElement(leafChild);
//	}
	public String getChildTextByXpath(String xpath) {
		XMLElement el = getElement(xpath);
		if (el == null) {
			return null;
		}
		return el.getText();
	}
	
	public XMLElement getElement(String path) {
		Element element = null;
		try {
			element = (Element)XPath.selectSingleNode(el, path);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		if(element == null)
			return null;
		return new XMLElement(element);
	}
	
	//Liuhui 2012-09-18
	public XMLElement getElementWithNS(String path, Namespace ns) {
		Element element = null;
		try {
//			element = (Element)XPath.selectSingleNode(el, path);
			element = el.getChild(path, ns);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		if(element == null)
			return null;
		return new XMLElement(element);
	}
	
	public List getElements(String path) {
		List allNotes = null;
		try {
			allNotes = XPath.selectNodes(el, path);
		} catch (Exception e) {
			e.printStackTrace();
			return new ArrayList();
		}
		if (allNotes == null) {
			return new ArrayList();
		}
		List result = new ArrayList();
		for (Iterator iter = allNotes.iterator(); iter.hasNext();) {
			Element e = (Element) iter.next();
			result.add(new XMLElement(e));
		}
		return result;
	}

	public String getValueAttribute() {
		if(el.getAttributeValue("VALUE") == null){
			return el.getAttributeValue("value");
		}
		return el.getAttributeValue("VALUE");
	}
	
	public String getAttribute(String name) {
		return el.getAttributeValue(name);
	}

	public String getText() {
		return el.getText();
	}
	
	//Update by Derick to create same function with String that don't get null value in vm file.
	public String toString() {
		return el.getText();
	}
	
	public List getChildren(){
		List children=el.getChildren();
		List resultList=new ArrayList();
		for (Iterator iter = children.iterator(); iter.hasNext();) {
			Element tel = (Element) iter.next();
			resultList.add(new XMLElement(tel));
		}
		return resultList;
	}
	
	public List getChildrenByName(String name){
		List children=el.getChildren();
		List resultList=new ArrayList();
		for (Iterator iter = children.iterator(); iter.hasNext();) {
			Element tel = (Element) iter.next();
			if (tel.getName().equalsIgnoreCase(name)){
				resultList.add(new XMLElement(tel));
			}
		}
		return resultList;
	}
	
	public static XMLElement parseXML(String xmlString) {
		Reader reader = new StringReader(xmlString);
		try {
			org.jdom.input.SAXBuilder builder = new SAXBuilder();
			Document doc = builder.build(new BufferedReader(reader));

			Element root = doc.getRootElement();
			return new XMLElement(root);
		} catch (JDOMException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				reader.close();
			} catch (IOException e) {
			}
		}
		return null;

	}

	public static XMLElement parseXML(File f) {
		try {
			org.jdom.input.SAXBuilder builder = new SAXBuilder();
			Document doc = builder.build(f);
			Element root = doc.getRootElement();
			return new XMLElement(root);
		} catch (JDOMException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	//Update by Derick to parse XML format to String.	
	public String parseXMLtoString(){
		XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
		return outputter.outputString(el.getDocument());
	}

	public String getName() {
		return el.getName();
	}
	
	
	public static void main(String args[]){
//		XMLElement xml = XMLElement.parseXML(new java.io.File("test2.xml"));
		
//		XMLElement x = xml.getElement("/adapter/output[2]/fact[3]/tn");
//		XMLElement x = xml.getElement("SOAP-ENV:Header");
//		Namespace ns = Namespace.getNamespace("ns2","http://csi.cingular.com/CSI/Namespaces/Types/Public/CingularDataModel.xsd");
//		XMLElement x = xml.getElementWithNS("PortDetails/currentSPID", ns);
 /*
		XMLElement xml = XMLElement.parseXML(new java.io.File("LNP_output.txt"));
		Namespace ns = Namespace.getNamespace("SOAP-ENV","http://schemas.xmlsoap.org/soap/envelope/");
		Namespace ns2 = Namespace.getNamespace("PortDetailsResponse","http://csi.cingular.com/CSI/Namespaces/Container/Public/InquireWirelinePortDetailsResponse.xsd");
		
		XMLElement x = xml.getElementWithNS("Body", ns);
		XMLElement x2 = x.getElementWithNS("InquireWirelinePortDetailsResponse", ns2);
		
		XMLElement x3 = x2.getElementWithNS("Response", ns2);
		
		List list = x3.getChildren();
		XMLElement xman = null;
		for (Iterator iter = list.iterator(); iter.hasNext();){
			xman = (XMLElement)iter.next();
			System.out.println(xman.getName() + " == " + xman.getText());
		}
*/		
		/*
		//LNP ERROR
		XMLElement xml = XMLElement.parseXML(new java.io.File("error300"));
		
		Namespace ns = Namespace.getNamespace("SOAP-ENV","http://schemas.xmlsoap.org/soap/envelope/");
		Namespace ns2 = Namespace.getNamespace("Fault","http://csi.cingular.com/CSI/Namespaces/Types/Public/SoapFaultDetails.xsd");
		Namespace ns3 = Namespace.getNamespace("Error","http://csi.cingular.com/CSI/Namespaces/Types/Public/ErrorResponse.xsd");
		
		XMLElement x = xml.getElementWithNS("Body", ns);
		XMLElement x2 = x.getElementWithNS("Fault", ns);
		XMLElement x3 = x2.getElement("detail");
		XMLElement x4 = x3.getElementWithNS("CSIApplicationException",ns2);
		XMLElement x5 = x4.getElementWithNS("Response",ns3);
		
		List list = x5.getChildren();
		XMLElement xman = null;
		for (Iterator iter = list.iterator(); iter.hasNext();){
			xman = (XMLElement)iter.next();
			System.out.println(xman.getName() + " == " + xman.getText());
		}
		*/
		
		XMLElement xml = XMLElement.parseXML(new java.io.File("output.txt"));
		
//		System.out.println(GeneralFunction.getLNP_ReturnCode(xml));
//		System.out.println(GeneralFunction.getLNP_NewCurrentSP(xml));
//		System.out.println(GeneralFunction.getLNP_SPName(xml));
//		System.out.println(GeneralFunction.getLNP_LRN(xml));
	}
}
