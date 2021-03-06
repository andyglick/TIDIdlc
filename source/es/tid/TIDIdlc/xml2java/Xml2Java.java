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

package es.tid.TIDIdlc.xml2java;

import es.tid.TIDIdlc.idl2xml.Idl2XmlNames;
import es.tid.TIDIdlc.idl2xml.Preprocessor;
import es.tid.TIDIdlc.xmlsemantics.*;
import es.tid.TIDIdlc.util.Traces;
import es.tid.TIDIdlc.CompilerConf;

import java.util.StringTokenizer;
import java.util.Vector;
import java.util.Enumeration;
import java.util.Hashtable;
import java.io.File;
import java.io.IOException;

import org.w3c.dom.*;

/**
 * Generates Java for the Document node. This is the main class of the
 * Xml-to-Java translation.
 */
public class Xml2Java
    implements Idl2XmlNames
{

    /**
     * @param dom
     *            Document node.
     */
    public Xml2Java(Document dom)
    {
        m_dom = dom;
    }

    /**
     * @param outputDir
     *            directory where the Java code is generated.
     * @param genPackage
     *            package to which generated classes belong.
     */
    public void generateJava(String outputDir, String genPackage,
                             Vector modulePackagedList,
                             Vector filePackagedList,
                             Hashtable packageToTable, String packageToError)
        throws Exception
    {

        // Generating of Java code for the idl file
        Traces.println("Generating: IDL.", Traces.USER);

        Element specification = m_dom.getDocumentElement();

        if (Traces.getLevel() == Traces.DEEP_DEBUG) {
            (new es.tid.TIDIdlc.idl2xml.XMLWriter(m_dom)).write(System.out);
        }

        // Check & resolve scopes
        XmlSemantics sem = new XmlSemantics(m_dom, new TypeManager(),
                                            genPackage);
        // Attention,
        //* Language Semantic Dependence.

        sem.checkScopes();

        // DAVV - comprobacion e implementacion de parametros -package y
        // -package_to
        rebuildNodesForPackageToParams(specification, genPackage,
                                       modulePackagedList, filePackagedList,
                                       packageToTable, packageToError);

        if ((outputDir == null) || (outputDir.length() == 0)) {
            outputDir = ".";
        }
        genPackage = "";

        // Do it!!
        XmlModule2Java gen = new XmlModule2Java();
        gen.generateJava(specification, outputDir, genPackage, true);

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
        }

		// Ant Tasks
        // Shutdown
        TypedefManager.Shutdown();
        TypeManager.Shutdown();
        RepositoryIdManager.Shutdown();
        IdlConstants.Shutdown();
        UnionManager.Shutdown();
        //Preprocessor.ReInit();
    }

    private Document m_dom;

    // Find the name of the module given in the descendants of the parent
    // Element.
    // If founded, returns the module. If not, returns null
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
                found = child.getTagName().equals(OMG_module) && child.getAttribute(OMG_name).equals(module_part);
            }
            if (found)
                theNode = child;
            else
                break;
        }
        if (!found)
            return null;
        else
            return theNode;
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

        // DAVV - comprobacion e implementacion de -package_to (primero solo
        // para modulos)
        Enumeration modules_pck = modulePackagedList.elements();
        String module_packaged = null;
        String package_to = null;
        Element theModule;
        while (modules_pck.hasMoreElements()) { 
        	// DAVV - para cada m�dulo del
        	// package_to...
            module_packaged = (String) modules_pck.nextElement();
            package_to = (String) packageToTable.get(module_packaged);
            theModule = findModule(specification, module_packaged);
            if (theModule == null) { 
            	// DAVV ... comprobamos que existe en el IDL
                if (packageToError == null || packageToError.equals("STOP"))
                    throw new SemanticException("-package_to parameter: The module was not found: '"
                    		+ module_packaged
							+ "'.");
                else if (packageToError.equals("WARNING"))
                    System.err.println("** WARNING: -package_to parameter: The module was not found: '"
                                 + module_packaged + "'.");
            } else {
                if (module_packaged.lastIndexOf("::") >= 0)
                    	package_to += "::"
                    		+ module_packaged.substring(0, module_packaged.lastIndexOf("::"));
                insertModuleToPackage(theModule, package_to);
            }
        }

        // DAVV - comprobacion e implementacion de -package_to (ahora para
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
                fileFound = fileFound || checkFileForParamTo(specification, completeFile,
                                                   package_to);
            }
            if (!fileFound) {
                if (packageToError == null || packageToError.equals("STOP"))
                    throw new SemanticException(
                    		"-package_to parameter: The file is not included: '"
                    		+ file_packaged
							+ "'.");
                else if (packageToError.equals("WARNING"))
                    System.err.println("** WARNING: -package_to parameter: The file is not included: '"
                                 + file_packaged + "'.");
            }
        }

        // DAVV - comprobacion e implementacion de -package
        if (genPackage != null && !genPackage.equals("")) {
            NodeList childNodes = specification.getChildNodes();
            int i = 0;
            while (i < childNodes.getLength()) {
                Element child = (Element) childNodes.item(i);
                if (!(child.getTagName().equals(OMG_module) && (child.getAttribute("line") == null ||
                		child.getAttribute("line").equals(""))))
                    // DAVV solo para aquellos hijos que no se acaben de
                    // insertar debido a -package_to (los cuales no tienen
                    // atributo "line" y son m�dulos)
                    insertModuleToPackage(child, genPackage);
                // DAVV al insertar, movemos el nodo evaluado de su puesto; si
                // se incrementara el valor del indice i,
                // nos saltariamos el siguiente nodo
                else
                    i++;
            }
        }

    }

    // DAVV - inserta en el �rbol m�dulos adicionales para los par�metros
    // package_to
    //          'theModule' es el nodo del �rbol que hay que mover bajo el nuevo
    //          'package_to' es el nombre completo del nuevo m�dulo

    private void insertModuleToPackage(Element theModule, String package_to)
    {
        Element specification = m_dom.getDocumentElement();
        Element theNew = findModule(specification, package_to);
        Element topModuleOverTheModule = theModule;
        while (topModuleOverTheModule.getParentNode().getNodeName() != OMG_specification)
            topModuleOverTheModule = (Element) topModuleOverTheModule.getParentNode();
        if (theNew == null) { 
        	// no existe a�n en el �rbol
            if (!package_to.equals("")) {
                StringTokenizer tok = new StringTokenizer(package_to, "::");
                Element theFather = specification;
                while (tok.hasMoreTokens()) {
                    String token = tok.nextToken();
                    theNew = m_dom.createElement(OMG_module);
                    theNew.setAttribute(OMG_name, token);
                    if (theFather.getTagName().equals(OMG_specification)) 
                    	// DAVV primer nivel
                        theFather.insertBefore(theNew, topModuleOverTheModule); 
                    // DAVV posicion es importante para generaci�n de includes de primer nivel
                    else
                        theFather.appendChild(theNew);
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
                    found = found || checkFileForParamTo(child, targetFile,
                                                   package_to);
                    i++;
                } else
                    i++;
            } else
                i++;
        }
        return found;
    }

}