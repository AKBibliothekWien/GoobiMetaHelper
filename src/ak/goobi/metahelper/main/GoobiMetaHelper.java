package ak.goobi.metahelper.main;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import betullam.goobi.oaihelper.main.GoobiOaiHelper;
import betullam.xmlhelper.XmlParser;

/**
 * Helps to process meta.xml and meta_anchor.xml files from Goobi.
 * 
 * @author mbirkner
 *
 */
public class GoobiMetaHelper extends GoobiOaiHelper {

	XmlParser xmlParser = new XmlParser();
	GoobiOaiHelper goh = new GoobiOaiHelper();
	String pathToMetadataFoder;
	boolean isAnchorMets;
	Document document;

	public GoobiMetaHelper(String pathToMetadataFolder, boolean isAnchorMets) {
		this.pathToMetadataFoder = stripFileSeperatorFromPath(pathToMetadataFolder);
		this.isAnchorMets = isAnchorMets;
		this.document = parseDocument(this.pathToMetadataFoder);
	}


	private Document parseDocument(String pathToMetadataFolder) {
		Document document = null;

		try {
			String fileName = (this.isAnchorMets) ? "meta_anchor.xml" : "meta.xml" ;
			File metaxmlFile = new File(pathToMetadataFolder + File.separator + fileName);
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			document = db.parse(metaxmlFile);
		} catch (SAXException | IOException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return document;
	}

	public Document getDocument() {
		return this.document;
	}

	@Override
	public List<String> getUrnsByPhysIds(Document document, List<String> physIds) {
		document = this.document;
		List<String> urns = new ArrayList<String>();
		List<String> dmdphysIds = getDmdphysIdsByPhysIds(physIds);
		for (String dmdphysId : dmdphysIds) {
			String urn = null;
			try {
				urn = xmlParser.getTextValue(document, "//mets/dmdSec[@ID='" + dmdphysId + "']/mdWrap/xmlData/mods/extension/goobi/metadata[@name='_urn']");
			} catch (XPathExpressionException e) {
				e.printStackTrace();
			}
			urns.add(urn);
		}

		return urns;
	}


	@Override
	public List<String> getAuthorsByDmdlogId(Document document, String dmdlogId) {
		document = this.document;
		List<String> authorNames = new ArrayList<String>();
		String xPathName				= "//mets/dmdSec[@ID='" + dmdlogId + "']/mdWrap/xmlData/mods/extension/goobi/metadata[@type='person']";

		try {
			XPath xPath =  XPathFactory.newInstance().newXPath();
			NodeList nameNodes = (NodeList)xPath.compile(xPathName).evaluate(document, XPathConstants.NODESET);

			for (int i = 0; i < nameNodes.getLength(); i++) {

				if (nameNodes.item(i).getNodeType() == org.w3c.dom.Node.ELEMENT_NODE ) {
					Element nameNodeElement = (Element)nameNodes.item(i);
					NodeList nameNodeChilds = nameNodeElement.getChildNodes();

					String firstName = null;
					String lastName = null;

					for (int j = 0; j < nameNodeChilds.getLength(); j++) {

						if (nameNodeChilds.item(j).getNodeType() == org.w3c.dom.Node.ELEMENT_NODE ) {
							Element childElement = (Element)nameNodeChilds.item(j);

							if (childElement.getTagName().equals("goobi:firstName")) {
								firstName = childElement.getTextContent();
							}

							if (childElement.getTagName().equals("goobi:lastName")) {
								lastName = childElement.getTextContent();
							} 
						}
					}
					
					if ((firstName != null && !firstName.isEmpty()) && (lastName != null && !lastName.isEmpty())) {
						authorNames.add(firstName + " " + lastName);
					}
				}
			}
		} catch (XPathExpressionException e) {
			e.printStackTrace();
		}

		authorNames = (authorNames.isEmpty() == false) ? authorNames : null;
		return authorNames;
	}



	public List<String> getDmdphysIdsByPhysIds(List<String> physIds) {
		List<String> dmdphysIds = new ArrayList<String>();
		for (String physId : physIds) {
			try {
				String dmdphysId = xmlParser.getAttributeValue(this.document, "//mets/structMap//div[@ID='" + physId + "']", "DMDID");
				dmdphysIds.add(dmdphysId);
			} catch (XPathExpressionException e) {
				e.printStackTrace();
			}
		}
		return dmdphysIds;
	}

	public String getPi() {
		String pi = null;

		try {
			pi = xmlParser.getTextValue(this.document, "//mets/dmdSec/mdWrap/xmlData/mods/extension/goobi/metadata[@name='CatalogIDDigital' and not(@anchorId='true')]");
		} catch (XPathExpressionException e) {
			e.printStackTrace();
		}

		return pi;
	}


	private String stripFileSeperatorFromPath (String path) {
		if ((path.length() > 0) && (path.charAt(path.length()-1) == File.separatorChar)) {
			path = path.substring(0, path.length()-1);
		}
		return path;
	}



}
