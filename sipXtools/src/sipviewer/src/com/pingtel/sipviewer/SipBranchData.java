package com.pingtel.sipviewer;


import java.util.Vector;
import java.util.List;
import java.util.Enumeration;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import org.jdom.Element;
import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import org.jdom.JDOMException;


public class SipBranchData
{
    String method;
    String responseCode;
    String responseText;
    String sourceEntity;
    String destinationEntity;
    String sourceAddress;
    String destinationAddress;
    String timeStamp;
    String transactionId;
    String frameId;
    String message;
    Vector branchIds;
    
    // contains JDOM items from the XML file parse operation
    static Element nodeContainer = null;
    static Document traceDoc = null;

    public SipBranchData(Element xmlBranchNode)
    {
        method = xmlBranchNode.getChildText("method");


        responseCode = xmlBranchNode.getChildText("responseCode");


        responseText = xmlBranchNode.getChildText("responseText");


        sourceEntity = xmlBranchNode.getChildText("source");


        destinationEntity = xmlBranchNode.getChildText("destination");


        sourceAddress = xmlBranchNode.getChildText("sourceAddress");


        destinationAddress = xmlBranchNode.getChildText("destinationAddress");


        timeStamp  = xmlBranchNode.getChildText("time");


        transactionId  = xmlBranchNode.getChildText("transactionId");
        int c = transactionId.charAt(0);
        if (c == 'C' || c == 'A')
        {
            transactionId = transactionId.substring(1);
        }

        frameId  = xmlBranchNode.getChildText("frameId");


        message = xmlBranchNode.getChildText("message");


        Element branchSet = xmlBranchNode.getChild("branchIdSet");
        branchIds = new Vector();
        List elementList = branchSet.getChildren("branchId");
        Element branchIdNode;
        int count = elementList.size();
        for(int i = 0; i < count; i++)
        {
            branchIdNode = (Element) elementList.get(i);
            branchIds.add(branchIdNode.getText());
        }


    }

    // input is the root container of the input file, it contains individual
    // XML elements that are SIP messages
    public static Vector getSipBranchDataElements(Element branchContainer)
    {
        Vector nodes = new Vector();
        
        // puts all the <branchNode></branchNode> sections into their own
        // individual element on the list
        List elementList = branchContainer.getChildren("branchNode");
        Element xmlNode;
        
        // list containing all the "branchNode" elements
        int count = elementList.size();
        
        // loop through all the elements
        for(int i = 0; i < count; i++)
        {
        	// get the JDOM Element object from the data
            xmlNode = (Element) elementList.get(i);
            
            // convert the JDOM object to a SipBranchData object
            // and add it to the vector that will be used as a source
            // to store ChartDescriptor elements
            nodes.add(new SipBranchData(xmlNode));
        }


        return(nodes);
    }

    // parses the input file and stores SIP data elements in a Vector
    // which is later processes to reorder the SIP messages (in case
    // they are not in the proper chronological sequence), then each
    // vector element in added to SIP Model
    // Note: This is an overloaded method so don't get confused when
    // its called again with the container object as the input
    public static Vector getSipBranchDataElements(URL traceFilename)
    {    	    	
    	// JDOM structure
    	SAXBuilder builder = new SAXBuilder();
        System.out.println("reading: " + traceFilename);
        Vector nodes = null;

        try
        {
        	// open the file and create an input stream
            URLConnection uc = traceFilename.openConnection();
            InputStreamReader input = new InputStreamReader(uc.getInputStream());
            
            // feed the stream through the JDOM builder and store it in the JDOM document
            traceDoc = builder.build(input);
            
            // get the root container from the JDOM document
            nodeContainer = traceDoc.getRootElement();

            // get the individual sip elements and store them in a vector which 
            // will be returned as part of this method
            nodes = SipBranchData.getSipBranchDataElements(nodeContainer);
            input.close();
        }
        catch(JDOMException je)
        {
            je.printStackTrace();
        }
        catch(IOException ioe)
        {
            ioe.printStackTrace();
        }


        return(nodes);
    }


    public static void main(String argv[]) throws Exception {
        String filename = argv[0];
        URL url = new URL( "file:" + filename);
        Vector nodes = SipBranchData.getSipBranchDataElements(url);
        int count = nodes.size();
        for(int i = 0; i < count; i++)
        {
            System.out.println((nodes.elementAt(i)));
        }
    }


    public String getLabel()
    {
        String label;
        if(isRequest()) label = method;
        else label = responseCode + " " + responseText;


        return(label);
    }


    public boolean isRequest()
    {
        return(method != null);
    }


    public String getMethod()
    {
        return(method);
    }


    public String getResponseCode()
    {
        return(responseCode);
    }


    public String getResponseText()
    {
        return(responseText);
    }



    public String getSourceEntity()
    {
        return(sourceEntity);
    }



    public String getDestinationEntity()
    {
        return(destinationEntity);
    }


    public String getSourceAddress()
    {
        return(sourceAddress);
    }


    public String getDestinationAddress()
    {
        return(destinationAddress);
    }


    public String getTimeStamp()
    {
        return(timeStamp);
    }



    public String getTransactionId()
    {
        return(transactionId);
    }


    // Returns the CSeq number, the Call-Id, and the from-tag, which
    // identifies the (end-to-end) transaction (assuming the to-tag is
    // different from the from-tag).
    public String getCSeqCallId()
    {
        int i = transactionId.lastIndexOf(",");
        String s = (i == -1) ? transactionId : transactionId.substring(0, i);
        return(s);
    }


    // Returns the dialog identifier, the Call-Id, the to-tag, and the
    // from-tag, which identifies the dialog.
    public String getDialogId()
    {
        int i = transactionId.indexOf(",");
        String s = (i == -1) ? transactionId : transactionId.substring(i+1);
        return(s);
    }


    public String getCallId()
    {
        int i = transactionId.indexOf(",");
        int j = (i == -1) ? -1 : transactionId.indexOf(",", i+1);
        String s = (j == -1) ? transactionId : transactionId.substring(i+1, j);
        return(s);
    }


    public String getFrameId()
    {
        return(frameId);
    }


    public String getMessage()
    {
       return(message);
    }


    public String getThisBranchId()
    {
        return(branchIds.size() > 0 ? (String)branchIds.elementAt(0) : null);
    }


    public Vector getBranchIds()
    {
       return(branchIds);
    }


    public String toString()
    {
        StringBuffer buffer = new StringBuffer();


        if(method != null) buffer.append("method: " + method + "\n");
        if(responseCode != null) buffer.append("responseCode: " + responseCode + "\n");
        if(responseText != null) buffer.append("responseText: " + responseText + "\n");
        if(sourceEntity != null) buffer.append("sourceEntity: " + sourceEntity + "\n");
        if(destinationEntity != null) buffer.append("destinationEntity: " + destinationEntity + "\n");
        if(sourceAddress != null) buffer.append("sourceAddress: " + sourceAddress + "\n");
        if(destinationAddress != null) buffer.append("destinationAddress: " + destinationAddress + "\n");
        if(timeStamp  != null) buffer.append("timeStamp: " + timeStamp + "\n");
        if(transactionId != null) buffer.append("transactionId: " + transactionId + "\n");
        if(frameId != null) buffer.append("frameId: " + frameId + "\n");
        if(message != null) buffer.append("message: " + message + "\n");


        if(branchIds != null)
        {
            for(Enumeration enumerator = branchIds.elements() ; enumerator.hasMoreElements() ;)
            {
                buffer.append("   branchId: " + (String) enumerator.nextElement() + "\n");
            }
        }


        return(buffer.toString());
    }
}
