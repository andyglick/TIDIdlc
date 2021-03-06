/*
* MORFEO Project
* http://www.morfeo-project.org
*
* Component: TIDIdlc
* Programming Language: Java
*
* File: $Source$
* Version: $Revision: 31 $
* Date: $Date: 2005-05-17 13:22:05 +0200 (Tue, 17 May 2005) $
* Last modified by: $Author: gsanjuan $
*
* (C) Copyright 2004 Telef�nica Investigaci�n y Desarrollo
*     S.A.Unipersonal (Telef�nica I+D)
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

package es.tid.TIDIdlc.xml2cpp;

import es.tid.TIDIdlc.CompilerConf;
import es.tid.TIDIdlc.idl2xml.Idl2XmlNames;
import es.tid.TIDIdlc.idl2xml.Preprocessor;
import es.tid.TIDIdlc.util.FileManager;
import es.tid.TIDIdlc.util.Traces;
import es.tid.TIDIdlc.xmlsemantics.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
//import java.io.FileWriter;
//import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.Hashtable;

/**
 * Generates Cpp for the Document node. This is the main class of the
 * Xml-to-Java translation.
 * <p>
 */
public class Xml2Cpp
    implements Idl2XmlNames
{

    /**
     * @param dom
     *            Document node.
     */
    public Xml2Cpp(Document dom)
    {
        m_dom = dom;
    }

    private Document m_dom;

    /**
     * @param outputDir
     *            directory where the Java code is generated.
     * @param genPackage
     *            package to which generated classes belong.
     */
    public void generateCpp(String outputDir, String genPackage,
                            Vector modulePackagedList, Vector filePackagedList,
                            /* Vector packageToList */
                            Hashtable packageToTable, String packageToError
                            /*, boolean orb_included */)
        throws Exception
    {
    	// Gets the File Manager
    	FileManager fm = FileManager.getInstance();

    	String h_ext = CompilerConf.getHeaderExtension();
    	String c_ext = CompilerConf.getSourceExtension();
    	boolean expanded = CompilerConf.getExpanded();
    	
        Element specification = m_dom.getDocumentElement();

        if (Traces.getLevel() == Traces.DEEP_DEBUG) {
            (new es.tid.TIDIdlc.idl2xml.XMLWriter(m_dom)).write(System.out);
        }

        // Check & resolve scopes
        XmlSemantics sem = new XmlSemantics(m_dom, new TypeManager(),
                                            genPackage);
        /*
         * , modulePackagedList, packageToList, orb_included);// Attention,
         * Language Semantic Dependence.
         */
        sem.checkScopes();

        // comprobacion e implementacion de parametros -package y
        // -package_to
        rebuildNodesForPackageToParams(specification, genPackage,
                                       modulePackagedList, filePackagedList,
                                       packageToTable, packageToError);

        // directorio(s) de salida
        if ((outputDir == null) || (outputDir.length() == 0))
            outputDir = ".";

        genPackage = "";

        String targetDirName = outputDir;// directory without final separator.
        if (targetDirName.charAt(targetDirName.length() - 1) == File.separatorChar) {
            targetDirName = targetDirName.substring(0, targetDirName.length() - 1);
        }

        // Make target directory
        //File targetDir = new File(targetDirName);
        //if (!targetDir.exists()) {
        //    targetDir.mkdirs();
        //}

        String sourceDir = targetDirName;
        String headerDir = targetDirName;
        if (specification.getTagName().equals(Idl2XmlNames.OMG_specification)) {
            // header Directory.
            if (!CompilerConf.getOutputHeaderDir().equals("")) {
                headerDir = CompilerConf.getOutputHeaderDir();
                if (headerDir.charAt(headerDir.length() - 1) == File.separatorChar) {
                    headerDir = headerDir.substring(0, headerDir.length() - 1);
                    CompilerConf.setOutputHeaderDir(headerDir);
                }
            }
            
            // This snippet doesn't make sense. It adds "./" to an absolute path.
            //if (!headerDir.startsWith(new Character(File.separatorChar).toString()) &&
            //    !headerDir.startsWith(".")) {
            //    headerDir = "." + File.separatorChar + headerDir;
            //}
            //targetDir = new File(headerDir);
            //if (!targetDir.exists()) {
            //    targetDir.mkdirs();
            //}
        }

        // Get package components

        if ((genPackage == null) || (genPackage.length() == 0)) {
            genPackage = "";
        }

        // Generation of Cpp code for the idl file
        Traces.println("Generating: IDL.", Traces.USER);

        // DAVV - para definiciones en �mbito global
        String globalForwardDeclarations = "_global_includes_for_"
                   + CompilerConf.getFileName().substring(0, CompilerConf.getFileName().length() - 4)
                   + "_idl";

        if (specification.getAttributeNode(OMG_Do_Not_Generate_Code) == null) {
            StringBuffer buffer = new StringBuffer();
            //Xml2Cpp.getDir(genPackage, headerDir, true);
            Traces.println("XmlModule2Cpp:->", Traces.DEEP_DEBUG);
            Traces.println("Generating : " + headerDir + File.separatorChar
                           + globalForwardDeclarations + h_ext +"...", Traces.USER);
            //FileWriter writer = new FileWriter(headerDir + File.separatorChar
            //                                   + globalForwardDeclarations
            //                                   + h_ext);
            //BufferedWriter buf_writer = new BufferedWriter(writer);
            XmlHppHeaderGenerator.generate(specification, buffer,
                                           OMG_specification,
                                           globalForwardDeclarations, "");
            XmlHppHeaderGenerator.includeForwardDeclarations(specification,
                                                             buffer,
                                                             OMG_specification,
                                                             "", "");
            XmlHppHeaderGenerator.includeChildrenHeaderFiles(specification,
                                                             buffer,
                                                             OMG_specification,
                                                             "", "");
            XmlHppHeaderGenerator.generateFoot(buffer, OMG_specification,
                                               globalForwardDeclarations, "");
            
            fm.addFile(buffer, globalForwardDeclarations+h_ext, headerDir, "GLOBAL", FileManager.TYPE_MAIN_HEADER);
            //buf_writer.write(buffer.toString());
            //buf_writer.close();
        }

        // Do it!!
        XmlModule2Cpp gen = new XmlModule2Cpp();
        gen.generateCpp(specification, sourceDir, headerDir, genPackage, true, expanded, h_ext, c_ext);

        Traces.println("End of code generation: IDL.", Traces.USER);

        // Tracing
        if (Traces.getLevel() == Traces.DEEP_DEBUG) {
            Traces.println("-------------------------\n", Traces.DEEP_DEBUG);

            Traces.println("Typedef Manager contents: ", Traces.DEEP_DEBUG);
            TypedefManager.getInstance().dump();
            Traces.println("-------------------------\n", Traces.DEEP_DEBUG);

            Traces.println("Type Mapping contents: ", Traces.DEEP_DEBUG);
            TypeManager.dump();
            Traces.println("-------------------------\n", Traces.DEEP_DEBUG);

            Traces.println("RepositoryId Manager contents: ", Traces.DEEP_DEBUG);
            RepositoryIdManager.getInstance().dump();
            Traces.println("-------------------------\n", Traces.DEEP_DEBUG);
            Traces.println("Final Dom Contents: ", Traces.DEEP_DEBUG);
            (new es.tid.TIDIdlc.idl2xml.XMLWriter(m_dom)).write(System.out);

        }
    }

    // Find the name of the module given in the descendants of the parent
    // Element.
    // If found, returns the module. If not, returns null

    private Element findModule(Element parent, String module_packaged)
    {
        StringTokenizer tok = new StringTokenizer(module_packaged, "::");
        String module_part = null;
        Element theNode = parent;
        boolean found = false;
        while (tok.hasMoreTokens()) {
            module_part = tok.nextToken();
            NodeList children = theNode.getChildNodes();
            found = false;
            Element child = null;
            for (int j = 0; j < children.getLength() && !found; j++) {
                child = (Element) children.item(j);
                found = child.getTagName().equals(OMG_module)
                        && child.getAttribute(OMG_name).equals(module_part);
            }
            if (found) {
                theNode = child;
            } else {
                break;
            }
        }
        if (!found) {
            return null;
        }
        else {
            return theNode;
        }
    }

    // inserta en el �rbol m�dulos adicionales para los par�metros
    // package_to
    //          'theModule' es el nodo del �rbol que hay que mover bajo el nuevo
    //          'package_to' es el nombre completo del nuevo m�dulo

    private void insertModuleToPackage(Element theModule, String package_to)
    {
        Element specification = m_dom.getDocumentElement();
        Element theNew = findModule(specification, package_to);
        Element topModuleOverTheModule = theModule;
        while (topModuleOverTheModule.getParentNode().getNodeName() != OMG_specification)
            topModuleOverTheModule = (Element) topModuleOverTheModule
                .getParentNode();
        if (theNew == null) { // no existe a�n en el �rbol
            if (!package_to.equals("")) {
                StringTokenizer tok = new StringTokenizer(package_to, "::");
                Element theFather = specification;
                while (tok.hasMoreTokens()) {
                    String token = tok.nextToken();
                    theNew = m_dom.createElement(OMG_module);
                    theNew.setAttribute(OMG_name, token);
                    if (theFather.getTagName().equals(OMG_specification)) {
                        // primer nivel
                        theFather.insertBefore(theNew, topModuleOverTheModule); 
                        // posicion es importante para generaci�n de includes
                        // de primer nivel
                    } else {
                        theFather.appendChild(theNew);
                    }
                    theFather = theNew;
                    theNew.appendChild(theModule);
                }
            } else {
                theNew = specification;
                if (topModuleOverTheModule != theModule)
                    theNew.insertBefore(theModule, topModuleOverTheModule);
            }
        } else
            theNew.appendChild(theModule);
    }

    // Accesible members. Compiler configuration data.
    public static String getDir(String genPackage, String directory,
                                 boolean generateCode)
    {// This is a code reduction, because, this hapens two times in every file
     // XXX2Cpp.
        // Destiny Directory
        String targetDirName = directory;
        if (targetDirName.charAt(targetDirName.length() - 1) == File.separatorChar) {
            targetDirName = targetDirName.substring(0, targetDirName.length() - 1);
        }
        StringTokenizer tok = new StringTokenizer(genPackage, "::");
        while (tok.hasMoreTokens()) {
            targetDirName = targetDirName + File.separatorChar
                            + tok.nextToken();
        }

        if (generateCode) {
            // Make target directory
            //File targetDir = new File(targetDirName);
            //if (!targetDir.exists()) {
            //    targetDir.mkdirs();
            //}
        }
        return targetDirName;
    }

 
    private void rebuildNodesForPackageToParams(Element specification,
                                                String genPackage,
                                                Vector modulePackagedList,
                                                Vector filePackagedList,
                                                Hashtable packageToTable,
                                                String packageToError)
        throws SemanticException,
        IOException
    {

        // comprobacion e implementacion de -package_to (primero solo
        // para modulos)
        Enumeration modules_pck = modulePackagedList.elements();
        //Enumeration packages_to = packageToList.elements();
        String module_packaged = null;
        String package_to = null;
        Element theModule;
        while (modules_pck.hasMoreElements()) { // para cada m�dulo del
                                                // package_to...
            module_packaged = (String) modules_pck.nextElement();
            //package_to = (String) packages_to.nextElement();
            package_to = (String) packageToTable.get(module_packaged);
            theModule = findModule(specification, module_packaged);
            if (theModule == null) { // ... comprobamos que existe en el IDL
                if (packageToError == null || packageToError.equals("STOP"))
                    throw new SemanticException("-package_to parameter: The module was not found: '"
                                                + module_packaged
                                                + "'.");
                else if (packageToError.equals("WARNING"))
                    System.err.println("** WARNING: -package_to parameter: The module was not found: '"
                                       + module_packaged + "'.");
            } else {
                if (module_packaged.lastIndexOf("::") >= 0) {
                    package_to += "::"
                        + module_packaged.substring(0, module_packaged.lastIndexOf("::"));
                }
                insertModuleToPackage(theModule, package_to);
            }
        }

        // comprobacion e implementacion de -package_to (ahora para
        // ficheros IDL)
        Enumeration files_pack = filePackagedList.elements();
        String file_packaged = null;
        while (files_pack.hasMoreElements()) {
            file_packaged = (String) files_pack.nextElement();
            package_to = (String) packageToTable.get(file_packaged);
            Vector searchPath = CompilerConf.getSearchPath();
            searchPath.add(".");
            boolean fileFound = false;
            for (int k = 0; k < searchPath.size(); k++) {
                String completeFile = (new File(searchPath.elementAt(k)
                                                + File.separator
                                                + file_packaged)).getCanonicalPath();
                fileFound = fileFound
                            || checkFileForParamTo(specification, completeFile,
                                                   package_to);
            }
            if (!fileFound) {
                if (packageToError == null || packageToError.equals("STOP")) {
                    throw new SemanticException("-package_to parameter: The file is not included: '"
                                                + file_packaged
                                                + "'.");
                } else if (packageToError.equals("WARNING")) {
                    System.err.println("** WARNING: -package_to parameter: The file is not included: '"
                                       + file_packaged + "'.");
                }
            }
        }

        // comprobacion e implementacion de -package
        if (genPackage != null && !genPackage.equals("")) {
            NodeList childNodes = specification.getChildNodes();
            int i = 0;
            while (i < childNodes.getLength()) {
                Element child = (Element) childNodes.item(i);
                if (!(child.getTagName().equals(OMG_module) && (child
                    .getAttribute("line") == null || child
                    .getAttribute("line").equals("")))) {
                    // solo para aquellos hijos que no se acaben de
                    // insertar debido a -package_to (los cuales no tienen
                    // atributo "line" y son m�dulos)
                    insertModuleToPackage(child, genPackage);
                // al insertar, movemos el nodo evaluado de su puesto; si
                // se incrementara el valor del indice i,
                // nos saltariamos el siguiente nodo
                } else {
                    i++;
                }
            }
        }

    }

    private boolean checkFileForParamTo(Element doc, String targetFile,
                                        String package_to)
    {
        NodeList children = doc.getChildNodes();
        boolean found = false;
        int i = 0;
        while (i < children.getLength()) {
            Element child = (Element) children.item(i);
            String line = child.getAttribute("line");
            if (line.length() > 0) {
                String actualFile = Preprocessor
                    .getInstance().locateFile((new Integer(line)).intValue());
                if (actualFile.equals(targetFile)) {
                    insertModuleToPackage(child, package_to);
                    found = true;
                } else if (child.getTagName().equals(OMG_module)) {
                    found = found
                            || checkFileForParamTo(child, targetFile,
                                                   package_to);
                    i++;
                } else {
                    i++;
                }
            } else {
                i++;
            }
        }
        return found;
    }

}