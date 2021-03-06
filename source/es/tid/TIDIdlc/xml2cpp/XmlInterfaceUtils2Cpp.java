/*
* MORFEO Project
* http://www.morfeo-project.org
*
* Component: TIDIdlc
* Programming Language: Java
*
* File: $Source$
* Version: $Revision: 310 $
* Date: $Date: 2009-06-09 09:37:19 +0200 (Tue, 09 Jun 2009) $
* Last modified by: $Author: avega $
*
* (C) Copyright 2004 Telef???nica Investigaci???n y Desarrollo
*     S.A.Unipersonal (Telef???nica I+D)
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
import es.tid.TIDIdlc.xmlsemantics.*;
//import es.tid.TIDIdlc.util.Traces;
//import java.io.*;
//import java.util.StringTokenizer;

import org.w3c.dom.*;

/**
 * Generates Cpp for interfaces.
 */
class XmlInterfaceUtils2Cpp
    implements Idl2XmlNames
{

    private java.util.Hashtable m_util_interface_parents = new java.util.Hashtable();

    protected int generateInterfacesSupported(StringBuffer buffer, Element doc,
                                              int indice)
    {
        String name = RepositoryIdManager.getInstance().get(doc);
        //_ids[0] = CORBA::String_dup(<<rep. id. 0>>);
        buffer.append("\t(*ids)[" + indice + "]=CORBA::string_dup(\"");
        buffer.append(name);
        buffer.append("\");\n");
        Element el1 = (Element) doc.getFirstChild();
        if (el1 != null) {
            if (el1.getTagName().equals(OMG_inheritance_spec)) {
                NodeList nodes = el1.getChildNodes();
                for (int i = 0; i < nodes.getLength(); i++) {
                    Element el = (Element) nodes.item(i);
                    //String clase = XmlType2Cpp.getType(el);

                    //String clase = XmlType2Cpp.getType(el);
                    //Scope scope = Scope.getGlobalScopeInterface(clase,"::");
                    Scope scope = Scope.getGlobalScopeInterface(el
                        .getAttribute(OMG_name));
                    Element inheritedElement = scope.getElement();
                    //String claseRepositoryId =
                    // RepositoryIdManager.getInstance().get(inheritedElement);

                    // Generate the Repository Id of all the interface parents
                    if (!m_util_interface_parents.containsKey(inheritedElement)) {
                        // This is to avoid the duplication of the RepositoryId
                        // when there's multiple
                        // inheritance and one of the father inherits from the
                        // other
                        m_util_interface_parents.put(inheritedElement, "void");
                        indice = generateInterfacesSupported(buffer,
                                                             inheritedElement,
                                                             indice + 1);
                    }
                }
            }
        }
        return indice;
    }

    protected void generateHppMethodHeader(StringBuffer buffer, Element doc,
                                           boolean is_virtual, boolean is_virtual_pure, String identation)
        throws Exception
    {
        // type == true Generate pure virtual methods
        // type == false Generate methods without virtual.
        boolean isOneWay = ((doc.getAttribute(OMG_oneway) != null) 
            && doc.getAttribute(OMG_oneway).equals("true")); // DAVV
        String isLocalS = 
            ((Element) doc.getParentNode()).getAttribute(OMG_local);
        boolean isLocal = (isLocalS != null && isLocalS.equals("true"))
                          || ((Element) doc.getParentNode()).getTagName().equals(OMG_valuetype);
        NodeList nodes = doc.getChildNodes();
        // Return type
        Element returnType = (Element) nodes.item(0);
        NodeList returnTypeL = returnType.getChildNodes();
        buffer.append("\n" + identation);
        if (is_virtual)
            buffer.append("virtual ");
        if (returnTypeL.getLength() > 0) {
            Element ret = (Element) returnTypeL.item(0);
            if (isOneWay) 
                throw new SemanticException(
                            "Return value for oneway operation must be 'void'",
                            doc);
            if (ret.getTagName().equals(OMG_scoped_name)
                && XmlType2Cpp.getDefinitionType(ret).equals(OMG_native))
                if (!isLocal)
                    throw new SemanticException(
                                "Native types are only allowed in local interfaces",
                                ret);
            String retType = XmlType2Cpp.getReturnType(ret);
            buffer.append(retType);
            buffer.append(" ");
        } else {
            buffer.append("void ");
        }

        // Method name
        String nombre = doc.getAttribute(OMG_name);
        buffer.append(nombre + "(");

        // Parameters
        for (int i = 1; i < nodes.getLength(); i++) {
            Element el = (Element) nodes.item(i);
            if (el.getTagName().equals(OMG_parameter)) {
                Element paramType = (Element) el.getChildNodes().item(0);
                String paramName = el.getAttribute(OMG_name);
                //boolean in = el.getAttribute(OMG_kind).equals("in");
                // In C++ are different inout parameters and out parameters so
                // the result type must also be different.
                if (!el.getAttribute(OMG_kind).equals("in") && isOneWay) 
                    throw new SemanticException(
                                "Parameters for oneway operation must be 'in'",
                                el);
                if (el.getTagName().equals(OMG_scoped_name)
                    && XmlType2Cpp.getDefinitionType(el).equals(OMG_native))
                    if (!isLocal)
                        throw new SemanticException(
                                    "Native types are only allowed in local interfaces",
                                    el);
                if (i > 1)
                    buffer.append(", ");
                String paramTypeS = XmlType2Cpp.getParamType(paramType, 
                                                    el.getAttribute(OMG_kind));
                buffer.append(paramTypeS);
                buffer.append(" ");
                buffer.append(paramName);
            }
        }
        buffer.append(")");

        if (is_virtual_pure)
            buffer.append(" = 0");
    }

    protected void generateHppAttributeDecl(StringBuffer buffer, Element doc,
                                            boolean isVirtual, String identation)
    {
        // Get type
        NodeList nodes = doc.getChildNodes();
        String returntype = XmlType2Cpp.getReturnType((Element) nodes.item(0));
        String type = XmlType2Cpp.getParamType((Element) nodes.item(0), "in");
        String readonly = doc.getAttribute(OMG_readonly);

        // Accessors generation
        for (int i = 1; i < nodes.getLength(); i++) {
            Element el = (Element) nodes.item(i);
            String name = el.getAttribute(OMG_name);
            buffer.append("\n" + identation);
            if (isVirtual)
                buffer.append("virtual ");
            buffer.append(returntype);
            buffer.append(" " + name + "()"); //getter
            if (isVirtual)
                buffer.append(" = 0");
            buffer.append(";\n");
            if ((readonly == null) || !readonly.equals(OMG_true)) {
                buffer.append(identation);
                if (isVirtual)
                    buffer.append("virtual ");
                buffer.append("void " + name + "(");
                buffer.append(type);
                buffer.append(" value)");
                if (isVirtual)
                    buffer.append(" = 0");
                buffer.append(";\n"); //setter
            }
            buffer.append("\n");
        }
    }

    protected void generateCppMethodHeader(StringBuffer buffer, Element doc,
                                           String className)
    {
        NodeList nodes = doc.getChildNodes();
        // Return type
        Element returnType = (Element) nodes.item(0);
        NodeList returnTypeL = returnType.getChildNodes();
        if (returnTypeL.getLength() > 0) {
            Element ret = (Element) returnTypeL.item(0);
            String retType = XmlType2Cpp.getReturnType(ret);
            buffer.append(retType);
            buffer.append(" ");
        } else {
            buffer.append("void ");
        }

        // Method name
        String nombre = doc.getAttribute(OMG_name);
        buffer.append(className);
        buffer.append("::" + nombre + "(");

        // Parameters
        for (int i = 1; i < nodes.getLength(); i++) {
            Element el = (Element) nodes.item(i);
            if (el.getTagName().equals(OMG_parameter)) {
                Element paramType = (Element) el.getChildNodes().item(0);
                String paramName = el.getAttribute(OMG_name);
                //boolean in = el.getAttribute(OMG_kind).equals("in");
                if (i > 1) {
                    buffer.append(", ");
                }
                String paramTypeS = 
                    XmlType2Cpp.getParamType(paramType, 
                                             el.getAttribute(OMG_kind));
                buffer.append(paramTypeS);
                buffer.append(" ");
                buffer.append(paramName);
            }
        }
        buffer.append(")\n");

    }

    protected void generateInArgumentExtraction(StringBuffer buffer,
                                                Element type, String paramName,
                                                String anyName, String ident)
        throws SemanticException
    {
        //String paramType=type.getAttribute(OMG_name);
        //if(paramType.equals("")) paramType=
        // XmlType2Cpp.basicMapping(type.getAttribute(OMG_kind));
        String paramType = XmlType2Cpp.getType(type);
        String definition = XmlType2Cpp.getDefinitionType(type);

        if (definition.equals(OMG_array)) {
            buffer.append(ident + paramType + "_forany " + paramName + ";\n");
            buffer.append(ident + anyName + " >>= " + paramName + ";\n");

        } else if (definition.equals(OMG_interface)) {
            buffer.append(ident + paramType + "_ptr " + paramName + ";\n");
            buffer.append(ident + anyName + ">>= " + paramName + ";\n");

        } else if (definition.equals(OMG_struct)){ 
        	if (CompilerConf.getNonCopyingOperators()) { // Performance improvements
        		buffer.append(ident + paramType + "* " + paramName + ";\n");        		
        	} else {
        		buffer.append(ident + "const " + paramType + "* " + paramName + ";\n");
        	}        	 
            buffer.append(ident + anyName + ">>= " + paramName + ";\n");
            
        } else if (definition.equals(OMG_union)) { // Performance improvements
        	if (CompilerConf.getNonCopyingOperators()) {
        		buffer.append(ident + paramType + "* " + paramName + ";\n");	
        	} else {
                buffer.append(ident + "const " + paramType + "* " + paramName + ";\n");        		
        	}        		        	
            buffer.append(ident + anyName + ">>= " + paramName + ";\n");
            
        } else if (definition.equals(OMG_sequence)) { // Performance improvements
        	if (CompilerConf.getNonCopyingOperators()) {
        		buffer.append(ident + paramType + "* " + paramName + ";\n");	
        	} else {
        		buffer.append(ident + "const " + paramType + "* " + paramName + ";\n");
        	}
            buffer.append(ident + anyName + ">>= " + paramName + ";\n");

        } else if (definition.equals(OMG_valuetype)) {
            buffer.append(ident + paramType + "* " + paramName + ";\n");
            buffer.append(ident + anyName + ">>= " + paramName + ";\n");

        } else if (definition.equals(OMG_enum)) {
            buffer.append(ident + paramType + " " + paramName + ";\n");
            buffer.append(ident + anyName + ">>= " + paramName + ";\n");

        } else if (definition.equals(OMG_kind)) {
            if (XmlType2Cpp.isAString(type)) {
                long bounds = 0;
                Element expr = (Element) type.getFirstChild();
                if (expr != null) {
                    bounds = XmlExpr2Cpp.getIntExpr(expr);
                    buffer.append("\t\t\t" + "const " + paramType + " "
                                  + paramName + ";\n");
                    buffer.append("\t\t\t" + anyName
                                  + ">>= CORBA::Any::to_string(" + paramName
                                  + ", " + bounds + ");\n");
                } else {
                    buffer.append("\t\t\t const char* " + paramName + ";\n");
                    buffer.append("\t\t\t" + anyName + " >>= " + paramName
                                  + ";\n");
                }
            } else if (XmlType2Cpp.isAWString(type)) {
                long bounds = 0;
                Element expr = (Element) type.getFirstChild();
                if (expr != null) {
                    bounds = XmlExpr2Cpp.getIntExpr(expr);
                    buffer.append(ident + "const " + paramType + " "
                                  + paramName + ";\n");
                    buffer.append(ident + anyName
                                  + ">>= CORBA::Any::to_wstring(" + paramName
                                  + ", " + bounds + ");\n");
                } else {
                    buffer.append("\t\t\t const CORBA::WChar* " + paramName
                                  + ";\n");
                    buffer.append("\t\t\t" + anyName + " >>= " + paramName
                                  + ";\n");
                }

            } else if (XmlType2Cpp.getDeepKind(type).equals(OMG_any)) {
                buffer.append(ident + "const " + paramType + "* " + paramName
                              + ";\n");
                buffer.append(ident + anyName + ">>= " + paramName + ";\n");
            } else if (XmlType2Cpp.getDeepKind(type).equals(OMG_Object)            		 
            		&& !type.getAttribute(OMG_kind).equals(OMG_Object)) {
                buffer.append(ident + paramType + "_ptr "+ paramName + ";\n");
                buffer.append(ident + anyName + ">>= "
                              + XmlType2Cpp.getAnyExtraction(type, paramName)
                              + ";\n");
            } else {
                // esto es en otro caso.
                buffer.append(ident + paramType + " " + paramName + ";\n");
                buffer.append(ident + anyName + ">>= "
                              + XmlType2Cpp.getAnyExtraction(type, paramName)
                              + ";\n");
            }
        }
    }

    protected void generateOutArgumentInsertion(StringBuffer buffer,
                                                Element type, String paramName,
                                                String anyName)
        throws SemanticException
    {       
        if (XmlType2Cpp.isAString(type) || XmlType2Cpp.isAWString(type))
            buffer.append("\t\t\t// Any value takes ownership of " + paramName + "\n");  
        
        buffer.append("\t\t\t" + anyName + " <<= "
                      + XmlType2Cpp.getAnyInsertion(type, paramName, true)
                      + ";\n");
             
    }

    protected void generateOutArgumentDefinition(StringBuffer buffer,
                                                 Element type,
                                                 String paramName, String ident)
    {
        //String paramType=type.getAttribute(OMG_name);
        //if(paramType.equals("")) paramType=
        // XmlType2Cpp.basicMapping(type.getAttribute(OMG_kind));
        String paramType = XmlType2Cpp.getType(type);
        String definition = XmlType2Cpp.getDefinitionType(type);
        if (definition.equals(OMG_interface))
            buffer.append(ident + paramType + "_ptr " + paramName + ";\n");
        else if (definition.equals(OMG_array))
            if (XmlType2Cpp.isVariableSizeType(type))
                buffer.append(ident + paramType + "_slice* " + paramName
                              + ";\n");
            else
                buffer.append(ident + paramType + " " + paramName + ";\n");
        else if (definition.equals(OMG_struct) || definition.equals(OMG_union))
            if (XmlType2Cpp.isVariableSizeType(type))
                buffer.append(ident + paramType + "* " + paramName + ";\n");
            else
                buffer.append(ident + paramType + " " + paramName + ";\n");
        else if (definition.equals(OMG_sequence)
                 || definition.equals(OMG_valuetype)
                 || (definition.equals(OMG_kind) 
                 && XmlType2Cpp.getDeepKind(type).equals(OMG_any)))
            buffer.append(ident + paramType + "* " + paramName + ";\n");
        else if (definition.equals(OMG_kind)  
                && (!type.getAttribute(OMG_kind).equals(OMG_Object))                 
                && XmlType2Cpp.getDeepKind(type).equals(OMG_Object))  
            buffer.append(ident + paramType + "_ptr " + paramName + ";\n");
        else
            // resto de OMG_kind y OMG_enum
            buffer.append(ident + paramType + " " + paramName + ";\n");
    }

    protected void generateInoutArgumentExtraction(StringBuffer buffer,
                                                   Element type,
                                                   String paramName,
                                                   String anyName,
                                                   String ident, boolean inout, boolean poa)
        throws SemanticException
    {
        //String paramType=type.getAttribute(OMG_name);
        String paramType = XmlType2Cpp.getType(type);
        if (paramType.equals(""))
            paramType = XmlType2Cpp.basicMapping(type.getAttribute(OMG_kind));
        String definition = XmlType2Cpp.getDefinitionType(type);

        if (definition.equals(OMG_array)) {
            buffer.append(ident + paramType + "_forany " + paramName
                          + "_forany;\n");
            buffer.append(ident + anyName + " >>= " + paramName + "_forany;\n");
            if (poa){
            	buffer.append(ident + paramName + " = (" + paramType + "_slice*) "
            				+ paramName + "_forany;\n");
            } else if (!inout && XmlType2Cpp.isVariableSizeType(type)) {
            	buffer.append(ident + paramName + " = " + ident + paramType + "_dup("
        				    + paramName + "_forany);\n");
            } else {
            	buffer.append(ident + paramType + "_copy("
            				+ paramName + ", " + paramName + "_forany);\n");
            }
            
        } else if (definition.equals(OMG_interface)) {
        	buffer.append(ident + anyName + ">>= " + paramName + ";\n");
        } else if (definition.equals(OMG_valuetype)) {
            buffer.append(ident + anyName + ">>= " + paramName + ";\n");        
        } else if (definition.equals(OMG_sequence)) {
	        if (!CompilerConf.getNonCopyingOperators()) {
            	buffer.append(ident + "const " + paramType + "* const_" + paramName + ";\n");
           	}
            //jagd  
            //buffer.append(ident + anyName + ">>= const_" + paramName + ";\n");
            buffer.append(ident + anyName + ">>= " + paramName + ";\n");
            // hay que distinguir entre parametros out (se pasa por
            // referencia a puntero) e inout (se pasa por referencia)
            /* jagd  
            if (!inout)
                buffer.append(ident + paramName + " =new " + paramType
                              + "(*const_" + paramName + ");\n");
            else
                buffer.append(ident + paramName + " = *const_" + paramName
                              + ";\t//Deep copy\n"); // DAVV - el operador '='
                                                     // esta sobrecargado
            */
        } else if (XmlType2Cpp.isAString(type)) {
            NodeList children = type.getChildNodes();
            long bounds = 0;
            if (children.getLength() > 0) {
                bounds = XmlExpr2Cpp.getIntExpr(children.item(0));
                buffer.append(ident + "const char* const_" + paramName
                              + "= NULL;\n");
                buffer.append(ident + anyName
                              + ">>=  CORBA::Any::to_string(const_" + paramName
                              + ", " + bounds + ");\n");
                buffer.append(ident + paramName + " = CORBA::string_dup(const_"
                              + paramName + ");\n");
            } else {
                buffer.append(ident + "const char* const_" + paramName
                              + " = NULL;\n");
                buffer.append(ident + anyName + ">>=  const_" + paramName
                              + ";\n");
                buffer.append(ident + paramName
                              + " =::CORBA::string_dup(const_" + paramName
                              + ");\n");
            }
        } else if (XmlType2Cpp.isAWString(type)) {
            NodeList children = type.getChildNodes();
            long bounds = 0;
            if (children.getLength() > 0) {
                buffer.append(ident + "const CORBA::WChar* const_" + paramName
                              + "= NULL;\n");
                buffer.append(ident + anyName
                              + ">>=  CORBA::Any::to_wstring(const_"
                              + paramName + ", " + bounds + ");\n");
                buffer.append(ident + paramName
                              + " = CORBA::wstring_dup(const_" + paramName
                              + ");\n");
            } else {
                buffer.append(ident + "const CORBA::WChar* const_" + paramName
                              + " = NULL;\n");
                buffer.append(ident + anyName + ">>=  const_" + paramName
                              + ";\n");
                buffer.append(ident + paramName
                              + " =::CORBA::wstring_dup(const_" + paramName
                              + ");\n");
            }
        } else if (definition.equals(OMG_kind)) {
            if (XmlType2Cpp.getDeepKind(type).equals(OMG_any)) {
 
                //buffer.append(ident + "const CORBA::Any* const_" + paramName + ";\n");
                //buffer.append(ident + anyName + " >>= const_" + paramName + ";\n");
                if (inout) {
                	buffer.append(ident + "const CORBA::Any* const_" + paramName + ";\n");
                    buffer.append(ident + anyName + " >>= const_" + paramName + ";\n");
                    buffer.append(ident + paramName + "=(*const_" + paramName + ");\n");
                }
                else {
                    buffer.append("("+ident + anyName + ".delegate()).extract_any(" + paramName + ");\n");
                    //buffer.append(ident + paramName + "=new CORBA::Any(*const_" + paramName + ");\n");
                }

            } else
                buffer.append(ident + anyName + ">>= "
                              + XmlType2Cpp.getAnyExtraction(type, paramName)
                              + ";\n");
        } else if (definition.equals(OMG_enum)) {
            buffer.append(ident + anyName + ">>= "
                          + XmlType2Cpp.getAnyExtraction(type, paramName)
                          + ";\n");
        } else if (definition.equals(OMG_struct)) {
			if (!CompilerConf.getNonCopyingOperators()) { // Performance improvements
				buffer.append(ident + "const " + paramType + "* const_" + paramName
                        + ";\n");
            }	
            buffer.append(ident + anyName + ">>= " + paramName + ";\n");                
        } else if (definition.equals(OMG_union)) {
        	if (CompilerConf.getNonCopyingOperators()) { // Performance improvements
        		if (XmlType2Cpp.isVariableSizeType(type)) { // out e inout
        			buffer.append(ident + anyName + ">>= " + paramName + ";\n");
        			
	        	} else {
    	    		if (inout) { // inoout
        				buffer.append(ident + anyName + ">>= " + paramName + ";\n");
	        		}
    	    		else { // out
        				buffer.append(ident + "const " + paramType + "* const_" + paramName + ";\n");
        				buffer.append(ident + anyName + ">>= const_" + paramName + ";\n");
        				buffer.append(ident + paramName + " = *const_" + paramName + ";\n");
        			}
        		}
        	} else {                	        	
	            if (XmlType2Cpp.isVariableSizeType(type)) {
    	            buffer.append(ident + "const " + paramType + "* const_" + paramName + ";\n");
        	        buffer.append(ident + anyName + ">>= const_" + paramName + ";\n");
            	    // hay que distinguir entre parametros out (se pasa por
                	// referencia a puntero) e inout (se pasa por referencia)
	                if (!inout)
    	                buffer.append(ident + paramName + " =new " + paramType
                                  + "(*const_" + paramName + ");\n");                    
                	else
                    	buffer.append(ident + paramName + " = *const_" + paramName
                                  + ";\t//Deep copy\n"); // el operador
                                                         // '=' esta
                                                         // sobrecargado
	            } else {
    	            buffer.append(ident + "const " + paramType + "* const_" + paramName + ";\n");
        	        buffer.append(ident + anyName + ">>= const_" + paramName + ";\n");
            	    buffer.append(ident + paramName + " = *const_" + paramName + ";\n");
	            }
	        }
        }
    }

    protected void generateInoutArgumentDefinition(StringBuffer buffer,
                                                   Element type,
                                                   String paramName,
                                                   String anyName, String index)
    {
        //String paramType=type.getAttribute(OMG_name);
        //if(paramType.equals(""))
        // paramType=XmlType2Cpp.basicMapping(type.getAttribute(OMG_kind));
        String paramType = XmlType2Cpp.getType(type);
        String definition = XmlType2Cpp.getDefinitionType(type);

        if (definition.equals(OMG_array))
            buffer.append(index + paramType + "_slice* " + paramName + ";\n");
        else if (definition.equals(OMG_interface))
            buffer.append(index + paramType + "_ptr " + paramName + ";\n");
        else if (definition.equals(OMG_valuetype))
            buffer.append(index + paramType + "* " + paramName + ";\n");
        else if(definition.equals(OMG_kind)        		
        		&& !type.getAttribute(OMG_kind).equals(OMG_Object) 
                && XmlType2Cpp.getDeepKind(type).equals(OMG_Object))
            buffer.append(index + paramType + "_ptr " + paramName + ";\n");
        else if (definition.equals(OMG_struct)){
	      	if(CompilerConf.getNonCopyingOperators())
	    		buffer.append(index + paramType + "* " + paramName + ";\n");
	        else
		        buffer.append(index + paramType + " " + paramName + ";\n");
        } else if (definition.equals(OMG_union)){
           	if(CompilerConf.getNonCopyingOperators())
	        	buffer.append(index + paramType + "* " + paramName + ";\n");
	        else
		        buffer.append(index + paramType + " " + paramName + ";\n");
        } else if (definition.equals(OMG_sequence)){ 
           	if(CompilerConf.getNonCopyingOperators())
        		buffer.append(index + paramType + "* " + paramName + ";\n");
        	else
        		buffer.append(index + paramType + " " + paramName + ";\n");
        } else          	
            buffer.append(index + paramType + " " + paramName + ";\n");
        // DAVV - asi de sencillo...
    }

    protected void generateInArgumentParameterUsing(StringBuffer buffer,
                                                    Element type,
                                                    String paramName)
    {
        String definition = XmlType2Cpp.getDefinitionType(type);
        if (definition.equals(OMG_sequence)
                  || definition.equals(OMG_struct)
                  || definition.equals(OMG_union))
            buffer.append("*");
        else if (definition.equals(OMG_kind)
                 && XmlType2Cpp.getDeepKind(type).equals(OMG_any))
            buffer.append("*");
        else if (definition.equals(OMG_array))
            buffer.append("(" + XmlType2Cpp.getType(type) + "_slice*)");
        buffer.append(paramName);
    }

    protected void generateOutArgumentParameterUsing(StringBuffer buffer,
                                                     Element type,
                                                     String paramName)
    {
        buffer.append(paramName);
    }

    protected void generateInoutArgumentParameterUsing(StringBuffer buffer,
                                                       Element type,
                                                       String paramName)
    {
        String definition = XmlType2Cpp.getDefinitionType(type);
        if (XmlType2Cpp.isVariableSizeType(type)) {// Los tipos de tama?o
                                                   // variable se pasan por *&

            if (definition.equals(OMG_interface)) {

                buffer.append(paramName);

            } else if (definition.equals(OMG_struct)) {
            	if(CompilerConf.getNonCopyingOperators())
	            	buffer.append("(*"+paramName+")");
	            else
	            	buffer.append(paramName);
            } else if (definition.equals(OMG_union)) {
               	if(CompilerConf.getNonCopyingOperators())
               		buffer.append("(*"+paramName+")");
               	else
               		buffer.append(paramName);
            } else if (definition.equals(OMG_sequence)) {
               	if(CompilerConf.getNonCopyingOperators())            
                	buffer.append("(*"+paramName+")"); 
                else
                	buffer.append(paramName);
            } else
                buffer.append(paramName);

            return;

        } else {
        	if (definition.equals(OMG_struct)) {
              	if(CompilerConf.getNonCopyingOperators())
            		buffer.append("(*"+paramName+")");
            	else
                	buffer.append(paramName);
        	} else if (definition.equals(OMG_union)) {
              	if(CompilerConf.getNonCopyingOperators())
            		buffer.append("(*"+paramName+")");
            	else
            		buffer.append(paramName);                          
            } else if (type.getAttribute(OMG_kind) != null
                       || definition.equals(OMG_kind)) {// Tipos de tama?o fijo se pasa
                                                        // la referencia.
                if (type.getAttribute(OMG_kind).equals(OMG_string)) {
                    buffer.append(paramName);

                    return;
                }
                // No es un String pero si es un Kind
                buffer.append(paramName);
                return;
            }                    
        }
    }



    protected void generateReturnTypeExtraction(StringBuffer buffer,
                                                Element type, String paramName,
                                                String anyName, String ident)
        throws SemanticException
    {
        //String paramType=type.getAttribute(OMG_name);
        String paramType = XmlType2Cpp.getType(type);
        if (paramType.equals(""))
            paramType = XmlType2Cpp.basicMapping(type.getAttribute(OMG_kind));
        String definition = XmlType2Cpp.getDefinitionType(type);

        if (definition.equals(OMG_array)) {
            buffer.append(ident + paramType + "_forany " + paramName
                          + "_forany;\n");
            buffer.append(ident + "if (" + anyName + " >>= " + paramName
                          + "_forany) {\n");
            //buffer.append(ident + anyName + " >>= " + paramName +
            // "_forany\n");
            buffer.append(ident + "\t" + paramName + " = (" + paramType
                          + "_slice*) " + paramName + "_forany;\n");
        } else if (definition.equals(OMG_interface)) {            
            buffer.append(ident + "if (" + anyName + ">>= " + paramName
                          + ") {\n");            
        } else if (definition.equals(OMG_valuetype)) {
	        if(CompilerConf.getNonCopyingOperators()){
        		buffer.append(ident + "if (" + anyName + ">>= " + paramName + ") {\n");
        		buffer.append(ident + "\tCORBA::add_ref(" + paramName + ");\n");
        	} else {
            	buffer.append(ident + "const " + paramType + "* const_" + paramName + ";\n");
            	buffer.append(ident + "if (" + anyName + ">>= const_" + paramName + ") {\n");            
            	buffer.append(ident + "\t" + paramName + " = (" + paramType + "*) const_" + paramName+ ";\n");
            	buffer.append(ident + "\tCORBA::add_ref(" + paramName + ");\n");
            }            
        } else if (definition.equals(OMG_sequence)) {
        	if(CompilerConf.getNonCopyingOperators())
        		buffer.append(ident + "if (" + anyName + ">>= " + paramName + ") {\n");
        	else {            
            	//buffer.append(ident + "const " + paramType + "* const_" + paramName
            	buffer.append(ident + " " + paramType + "* const_" + paramName  + ";\n");
            	//buffer.append(ident+anyName+">>= const_"+paramName+";\n");
            	buffer.append(ident + "if (" + anyName + ">>= const_" + paramName + ") {\n");
             
            	//buffer.append(ident + "\t" + paramName + " =new " + paramType
            	//              + "(*const_" + paramName + ");\n");
            	buffer.append(ident + "\t" + paramName + " = const_"+ paramName + ";\n");            
            }
        } else if (XmlType2Cpp.isAString(type)) {
            long bounds = 0;
            Element expr = (Element) type.getFirstChild();
            if (expr != null) {
                bounds = XmlExpr2Cpp.getIntExpr(type);
                buffer.append(ident + "const char*  const_" 
                              + paramName + ";\n");
                buffer.append(ident + "if (" + anyName
                              + ">>= CORBA::Any::to_string(const_" + paramName
                              + ", " + bounds + ")) {\n");
                buffer.append(ident + "\t" + paramName
                              + " =::CORBA::string_dup(const_" + paramName
                              + ");\n");
            } else {
                buffer.append(ident + "const char* const_" + paramName + ";\n");
                buffer.append(ident + "if (" + anyName + ">>=  const_"
                              + paramName + ") {\n");
                buffer.append(ident + "\t" + paramName
                              + " =::CORBA::string_dup(const_" + paramName
                              + ");\n");
            }
        } else if (XmlType2Cpp.isAWString(type)) {
            long bounds = 0;
            Element expr = (Element) type.getFirstChild();
            if (expr != null) {
                buffer.append(ident + "const CORBA::WChar*  const_" + paramName
                              + ";\n");
                buffer.append(ident + "if (" + anyName
                              + ">>= CORBA::Any::to_wstring(const_" + paramName
                              + ", " + bounds + ")) {\n");
                buffer.append(ident + "\t" + paramName
                              + " =::CORBA::wstring_dup(const_" + paramName
                              + ");\n");
            } else {
                buffer.append(ident + "const CORBA::WChar* const_" + paramName
                              + ";\n");
                buffer.append(ident + "if (" + anyName + ">>=  const_"
                              + paramName + ") {\n");
                buffer.append(ident + "\t" + paramName
                              + " =::CORBA::wstring_dup(const_" + paramName
                              + ");\n");
            }
        } else if (definition.equals(OMG_struct)) {
        	if(CompilerConf.getNonCopyingOperators()){
        		buffer.append(ident + "if (" + anyName + ">>= " + paramName + ") {\n");        	
        	} else {
        		if (XmlType2Cpp.isVariableSizeType(type)){
        			buffer.append(ident + " " + paramType + "* const_" + paramName + ";\n");
        			buffer.append(ident + "if (" + anyName + ">>= const_" + paramName + ") {\n");
        			buffer.append(ident + "\t" + paramName + " = " + "const_" + paramName + ";\n");
        		} else {
        			buffer.append(ident + "const " + paramType + "* const_" + paramName + ";\n");
        			buffer.append(ident + "if (" + anyName + ">>= " + paramName + ") {\n");
        		}
        	}
        	
        } else if (definition.equals(OMG_union)) {
            if(CompilerConf.getNonCopyingOperators()){
              	buffer.append(ident + "if (" + anyName + ">>= " + paramName + ") {\n");
            } else {
        		if (XmlType2Cpp.isVariableSizeType(type)){
	        		buffer.append(ident + " " + paramType + "* const_" + paramName + ";\n");
                	buffer.append(ident + "if (" + anyName + ">>= const_" + paramName + ") {\n");
                	buffer.append(ident + "\t" + paramName + " = " + "const_" + paramName + ";\n");
        		} else {
        			buffer.append(ident + "const " + paramType + "* const_" + paramName + ";\n");
        			buffer.append(ident + "if (" + anyName + ">>= " + paramName + ") {\n");
        		}
        	}
            
        } else if (definition.equals(OMG_enum)) {
            buffer.append(ident + "if (" + anyName + ">>= "
                          + XmlType2Cpp.getAnyExtraction(type, paramName)
                          + ") {\n");
        } else if (definition.equals(OMG_kind)) {
            if (XmlType2Cpp.getDeepKind(type).equals(OMG_any)) {
             	if(!CompilerConf.getNonCopyingOperators()){
                	buffer.append(ident + "const " + paramType + "* const_" + paramName + ";\n");
              	}
                //buffer.append(ident + "if (" + anyName + ">>= const_"
                //              + paramName + ") {\n");
                buffer.append(ident + "if ((" + anyName +".delegate()).extract_any( "
                              + paramName + ")) {\n");
                //buffer.append(ident + "\t" + paramName + "= new " + paramType
                //              + "(*const_" + paramName + ");\n");
            } else
                buffer.append(ident + "if (" + anyName + ">>= "
                              + XmlType2Cpp.getAnyExtraction(type, paramName)
                              + ") {\n");
        }
    }
}