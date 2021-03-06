/*
* MORFEO Project
* http://www.morfeo-project.org
*
* Component: TIDIdlc
* Programming Language: Java
*
* File: $Source$
* Version: $Revision: 199 $
* Date: $Date: 2007-05-10 12:16:01 +0200 (Thu, 10 May 2007) $
* Last modified by: $Author: avega $
*
* (C) Copyright 2004 Telefónica Investigación y Desarrollo
*     S.A.Unipersonal (Telefónica I+D)
*
* Info about members and contributors of the MORFEO project
* is available at:
*
*   http://www.morfeo-project.org/TIDIdlc/CREDITS
*
* This program is free software; you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation; either version 2 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program; if not, write to the Free Software
* Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
*
* If you want to use this software an plan to distribute a
* proprietary application in any way, and you are not licensing and
* distributing your source code under GPL, you probably need to
* purchase a commercial license of the product.  More info about
* licensing options is available at:
*
*   http://www.morfeo-project.org/TIDIdlc/Licensing
*/ 

package es.tid.TIDIdlc.xml2java;

import es.tid.TIDIdlc.idl2xml.Idl2XmlNames;
import es.tid.TIDIdlc.xml2java.structures.*;
import es.tid.TIDIdlc.util.Traces;

import java.io.*;
import java.util.StringTokenizer;
import java.util.Vector;

import org.w3c.dom.*;

/**
 * Generates Java for structure declarations.
 */
class XmlStruct2Java
    implements Idl2XmlNames
{

    /** Generate Java */
    public void generateJava(Element doc, String outputDirectory,
                             String genPackage, boolean generateCode)
        throws Exception
    {

        // Forward declaration
        if (doc.getAttribute(OMG_fwd).equals(OMG_true))
            return;

        // Get package components
        String targetDirName = outputDirectory;
        if (targetDirName.charAt(targetDirName.length() - 1) == File.separatorChar) {
            targetDirName = targetDirName.substring(0,targetDirName.length() - 1);
        }
        StringTokenizer tok = new StringTokenizer(genPackage, ".");
        while (tok.hasMoreTokens()) {
            targetDirName = targetDirName + File.separatorChar+ tok.nextToken();
        }

        if (generateCode) {
            // Make target directory
            File targetDir = new File(targetDirName);
            if (!targetDir.exists()) {
                targetDir.mkdirs();
            }
        }

        FileWriter writer;
        BufferedWriter buf_writer;
        String fileName, contents;

        // Struct generation
        fileName = doc.getAttribute(OMG_name) + ".java";
        if (generateCode) {
            Traces.println("XmlStruct2Java:->", Traces.DEEP_DEBUG);
            Traces.println("Generating : " + targetDirName + File.separatorChar
                           + fileName + "...", Traces.USER);
        }
        contents = generateJavaStructDef(doc, genPackage);
        if (generateCode) {
            writer = new FileWriter(targetDirName + File.separatorChar
                                    + fileName);
            buf_writer = new BufferedWriter(writer);
            buf_writer.write(contents);
            buf_writer.close();
        }

        // StructHolder generation
        fileName = doc.getAttribute(OMG_name) + "Holder" + ".java";
        if (generateCode) {
            Traces.println("XmlStruct2Java:->", Traces.DEEP_DEBUG);
            Traces.println("Generating : " + targetDirName + File.separatorChar
                           + fileName + "...", Traces.USER);
        }
        String name = doc.getAttribute(OMG_name);
        contents = XmlJavaHolderGenerator.generate(genPackage, name, name);
        if (generateCode) {
            writer = new FileWriter(targetDirName + File.separatorChar
                                    + fileName);
            buf_writer = new BufferedWriter(writer);
            buf_writer.write(contents);
            buf_writer.close();
        }

        // StructHelper generation
        fileName = doc.getAttribute(OMG_name) + "Helper" + ".java";
        if (generateCode) {
            Traces.println("XmlStruct2Java:->", Traces.DEEP_DEBUG);
            Traces.println("Generating : " + targetDirName + File.separatorChar
                           + fileName + "...", Traces.USER);
        }
        contents = generateJavaHelperDef(doc, genPackage);
        if (generateCode) {
            writer = new FileWriter(targetDirName + File.separatorChar
                                    + fileName);
            buf_writer = new BufferedWriter(writer);
            buf_writer.write(contents);
            buf_writer.close();
        }

        generateJavaSubPackageDef(doc, outputDirectory, name, genPackage,
                                  generateCode);

    }

    private String generateJavaStructDef(Element doc, String genPackage)
    {
        StringBuffer buffer = new StringBuffer();
        String name = doc.getAttribute(OMG_name);
        // Package header
        XmlJavaHeaderGenerator.generate(buffer, "struct", name, genPackage);

        // Class header
        buffer.append("public class ");
        buffer.append(name);
        buffer.append("\n   implements org.omg.CORBA.portable.IDLEntity {\n\n");

        // Items definition
        NodeList nodes = doc.getChildNodes();
        String objectName = null, classType = null;
        for (int i = 0; i < nodes.getLength(); i++) {
            Element el = (Element) nodes.item(i);
            String tag = el.getTagName();
            if (tag.equals(OMG_simple_declarator)) {
                objectName = el.getAttribute(OMG_name);
            } else if (tag.equals(OMG_array)) {
                objectName = el.getAttribute(OMG_name);
                for (int k = 0; k < el.getChildNodes().getLength(); k++) {
                    objectName += "[]";
                }
            } else {
                classType = XmlType2Java.getType(el);
                objectName = null;
            }
            if (objectName != null)
                buffer.append("  public " + classType + " " + objectName
                              + ";\n");
        }
        buffer.append("\n");

        // Constructors
        // buffer.append("  public " + name + "() {}\n\n");
        // Initialize all string members to "" conform to 1.8 Mapping for Struct (Java Mapping 02-08-05) 
        buffer.append("  public " + name + "() {\n");              
        for (int i = 0; i < nodes.getLength(); i++) {
        	Element el = (Element) nodes.item(i);
            String tag = el.getTagName();
            if (tag.equals(OMG_simple_declarator)) {
                objectName = el.getAttribute(OMG_name);
            } else if (tag.equals(OMG_array)) {
                objectName = el.getAttribute(OMG_name);
                for (int k = 0; k < el.getChildNodes().getLength(); k++) {
                    objectName += "[]";
                }
            } else {
                classType = XmlType2Java.getType(el);
                objectName = null;
            }
            if (objectName != null && classType.equals("java.lang.String")){
                buffer.append("    " + objectName + " = \"\";\n");                     
            } else if (objectName != null && classType.equals("java.lang.String[]")){
                buffer.append("    " + objectName + " = new String[0];\n");                     
            }
        }
        buffer.append("  }\n\n");
        
        
        buffer.append("  public " + name + "(");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element el = (Element) nodes.item(i);
            String tag = el.getTagName();
            if (tag.equals(OMG_simple_declarator)) {
                objectName = el.getAttribute(OMG_name);
            } else if (tag.equals(OMG_array)) {
                objectName = el.getAttribute(OMG_name);
                for (int k = 0; k < el.getChildNodes().getLength(); k++) {
                    objectName += "[]";
                }
            } else {
                classType = XmlType2Java.getType(el);
                objectName = null;
            }
            if (objectName != null) {
                if (i > 1)
                    buffer.append(", ");
                buffer.append(classType + " " + objectName);
            }
        }
        buffer.append(") {\n");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element el = (Element) nodes.item(i);
            String tag = el.getTagName();
            if (tag.equals(OMG_simple_declarator) || (tag.equals(OMG_array))) {
                objectName = el.getAttribute(OMG_name);
            } else {
                objectName = null;
            }
            if (objectName != null) {
                buffer.append("    this." + objectName + " = " + objectName
                              + ";\n");
            }
        }
        buffer.append("  }\n\n");

        buffer.append("}\n");

        return buffer.toString();
    }

    private String generateJavaHelperDef(Element doc, String genPackage)
        throws Exception
    {
        StringBuffer buffer = new StringBuffer();
        NodeList nodes = doc.getChildNodes();

        // Header
        String name = doc.getAttribute(OMG_name);
        XmlJavaHeaderGenerator.generate(buffer, "helper", name + "Helper",
                                        genPackage);

        // Class header
        buffer.append("abstract public class ");
        buffer.append(name);
        buffer.append("Helper {\n\n");
        buffer.append("  private static org.omg.CORBA.ORB _orb() {\n");
        buffer.append("    return org.omg.CORBA.ORB.init();\n");
        buffer.append("  }\n\n");

        XmlJavaHelperGenerator.generateInsertExtract(buffer, name, name
                                                                   + "Holder");

        buffer.append("  private static org.omg.CORBA.TypeCode _type = null;\n");
        buffer.append("  public static org.omg.CORBA.TypeCode type() {\n");
        buffer.append("    if (_type == null) {\n");
        NodeList dec1 = doc.getElementsByTagName(OMG_simple_declarator);
        NodeList dec2 = doc.getElementsByTagName(OMG_array);
        int numMembers = dec1.getLength() + dec2.getLength();
        buffer.append("      org.omg.CORBA.StructMember[] members = new org.omg.CORBA.StructMember["
                    + numMembers + "];\n");
        String st_name = genPackage + '.' + name;

        generateJavaStructProcess(st_name, buffer, doc, new StructType());
        buffer.append("      _type = _orb().create_struct_tc(id(), \"");
        buffer.append(name);
        buffer.append("\", members);\n");
        buffer.append("    }\n");
        buffer.append("    return _type;\n");
        buffer.append("  };\n\n");

        XmlJavaHelperGenerator.generateRepositoryId(buffer, doc);

        buffer.append("  public static ");
        buffer.append(name);
        buffer.append(" read(org.omg.CORBA.portable.InputStream is) {\n");
        buffer.append("    ");
        buffer.append(name);
        buffer.append(" result = new ");
        buffer.append(name);
        buffer.append("();\n");
        generateJavaStructProcess(st_name, buffer, doc, new StructReader());
        buffer.append("    return result;\n");
        buffer.append("  };\n\n");

        buffer.append("  public static void write(org.omg.CORBA.portable.OutputStream os, ");
        buffer.append(name);
        buffer.append(" val) {\n");
        generateJavaStructProcess(st_name, buffer, doc, new StructWriter());
        buffer.append("  };\n\n");

        buffer.append("}\n");

        return buffer.toString();
    }

    private void generateJavaStructProcess(String structName,
                                           StringBuffer buffer, Element doc,
                                           StructProcessor processor)
        throws Exception
    {
        String objectName = null;
        Element classType = null;
        NodeList nodes = doc.getChildNodes();
        Vector indexes = new Vector();
        Vector isArray = new Vector();

        for (int i = 0; i < nodes.getLength(); i++) {
            Element el = (Element) nodes.item(i);
            String tag = el.getTagName();

            // Simple declarator
            if (tag.equals(OMG_simple_declarator)) {
                objectName = el.getAttribute(OMG_name);
            }
            // Array declarator
            else if (tag.equals(OMG_array)) {
                objectName = el.getAttribute(OMG_name);
                NodeList indexChilds = el.getChildNodes();
                for (int k = 0; k < indexChilds.getLength(); k++) {
                    Element indexChild = (Element) indexChilds.item(k);
                    if (indexChild != null) {
                        indexes.insertElementAt(new Long(XmlExpr2Java.getIntExpr(indexChild)), k);
                        isArray.insertElementAt(new Boolean(true), k);
                    }
                }
            }
            // Sequence
            else if (tag.equals(OMG_sequence)) {
                indexes.removeAllElements();
                isArray.removeAllElements();
                int val = i;
                while (tag.equals(OMG_sequence)) {
                    el = (Element) el.getFirstChild();
                    Element expr = (Element) el.getNextSibling();
                    if (expr != null) {
                        indexes.addElement(new Long(XmlExpr2Java.getIntExpr(expr)));
                    } else {
                        indexes.addElement(new String("length" + val));
                    }
                    isArray.addElement(new Boolean(false));
                    tag = el.getTagName();
                    val++;
                }
                classType = el;
                objectName = null;
            }
            // Bounded Strings
            else if ((XmlType2Java.getTypedefType(el).equals(Idl2XmlNames.OMG_string) || 
				XmlType2Java.getTypedefType(el).equals(Idl2XmlNames.OMG_wstring)) &&
				(el.getFirstChild() != null)) {
                	indexes.removeAllElements();
                	isArray.removeAllElements();
                	int val = 0;
                	Element expr = (Element) el.getFirstChild();
                	if (expr != null) {
                		indexes.addElement(new Long(XmlExpr2Java.getIntExpr(expr)));
                	} else {
                		indexes.addElement(new String("length" + val));
                	}
                	isArray.addElement(new Boolean(false));
                	classType = el;
                	objectName = null;
            }
            // Other type
            else {
                indexes.removeAllElements();
                isArray.removeAllElements();
                classType = el;
                objectName = null;
            }

            if (objectName != null) {
                processor.generateJava(structName, buffer, objectName,
                                       classType, indexes, isArray);
            }
        }
    }

    private void generateJavaSubPackageDef(Element doc, String outputDir,
                                           String structName,
                                           String genPackage,
                                           boolean generateCode)
        throws Exception
    {

        String newPackage;
        if (!genPackage.equals("")) {
            newPackage = genPackage + "." + structName + "Package";
        } else {
            newPackage = structName + "Package";
        }

        NodeList nodes = doc.getChildNodes();

        for (int i = 0; i < nodes.getLength(); i++) {

            Element el = (Element) nodes.item(i);
            String tag = el.getTagName();

            if (tag.equals(OMG_enum)) {
                XmlEnum2Java gen = new XmlEnum2Java();
                gen.generateJava(el, outputDir, newPackage, generateCode);
            } else if (tag.equals(OMG_struct)) {
                XmlStruct2Java gen = new XmlStruct2Java();
                gen.generateJava(el, outputDir, newPackage, generateCode);
            } else if (tag.equals(OMG_union)) {
                XmlUnion2Java gen = new XmlUnion2Java();
                gen.generateJava(el, outputDir, newPackage, generateCode);
            }
        }
    }

}