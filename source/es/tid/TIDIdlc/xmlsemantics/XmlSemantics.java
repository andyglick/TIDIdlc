/*
* MORFEO Project
* http://www.morfeo-project.org
*
* Component: TIDIdlc
* Programming Language: Java
*
* File: $Source$
* Version: $Revision: 242 $
* Date: $Date: 2008-03-03 15:29:05 +0100 (Mon, 03 Mar 2008) $
* Last modified by: $Author: avega $
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

package es.tid.TIDIdlc.xmlsemantics;

import es.tid.TIDIdlc.idl2xml.*;
import es.tid.TIDIdlc.xml2java.*;
import es.tid.TIDIdlc.*;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.HashSet;
import java.util.Arrays;
import java.io.IOException;

import org.w3c.dom.*;

/**
 * Redecorates the xml document (associated to the idl file), and generates type
 * information, for semantic error detection and easier code generation.
 */
public class XmlSemantics
    implements Idl2XmlNames
{

    private XmlType m_current_xml_type;

    private Document m_doc;

    private String m_gen_package;

    //private Vector _modulePackagedList;
    //private boolean _orbIncluded;
    //private Vector _packageToList;

    // Attributes
    private Scope m_scope;

    private TypeHandler m_type_handler;

    public XmlSemantics(Document doc, TypeHandler typeHandler, String genPackage)
    /*
     * , Vector modulePackagedList, Vector packageToList, boolean orbIncluded)
     */
    {

        try {
            m_scope = new Scope(""); // root node
            m_doc = doc;
            m_type_handler = typeHandler;
            m_gen_package = genPackage;
            //_modulePackagedList = modulePackagedList;
            //_packageToList = packageToList;
            //_orbIncluded = orbIncluded;
            // Solucion +o- limpia a la inserccion de cosas del codigo final en
            // el semantico.
            if (CompilerConf.getCompilerType().equals("Java")) {
                m_current_xml_type = new XmlJavaSemanticType();
            } else {
                m_current_xml_type = new XmlCppSemanticType();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void checkScopes()
        throws SemanticException,
        IOException
    {

        Element idl = m_doc.getDocumentElement();
        checkScopes(idl, m_scope);
        removePragmas(idl);
    }

    private void addTypeDef(Element doc)
    {
        // El contenido con el que se rellena TypedefManager no tiene ni
        // el m�s m�nimo inter�s para
        //          C++; si bien en Java los typedef se reducen hasta el �ltimo tipo
        // simple o clase posible
        //          porque no existe sentencia de definici�n de tipos en el lenguaje, en
        // C++ a cada typedef IDL
        //          le corresponde un typedef en C++, por lo que no hay que mantener
        // ninguna tabla a este respecto.
        //          Por otra parte, lo que s� es necesario en C++ es conocer el tipo de
        // definici�n (struct, union,
        //          tipo b�sico...) original de un typedef, que en un 'retypedef' no
        // estar� disponible si no es
        //          volviendo a recorrer el �rbol. Por eso adaptaremos el contenido del
        // TypedefManager para que
        //          sirva a este prop�sito.

        NodeList nodes = doc.getChildNodes();
        Element type = (Element) nodes.item(0);

        for (int i = 1; i < nodes.getLength(); i++) {
            Element decl = (Element) nodes.item(i);
            String scopedTypeName = decl.getAttribute(OMG_scoped_name);

            if (CompilerConf.getCompilerType().equals("Java")) { // Java only -
                                                                 // old common
                                                                 // method

                // DAVV - getAbsoluteTypedefType no aporta diferencias con
                // respecto a getTypedefType
                //String typeString =
                // _currentXmlType.getAbsoluteTypedefType(type);
                String typeString = ((XmlJavaSemanticType) m_current_xml_type)
                    .getTypedefType(type);
                String holderType = m_current_xml_type
                    .basicOutMapping(typeString);

                if (decl.getTagName().equals(OMG_array)) {

                    //generateArrayJava(type, decl, outputDirectory,
                    // genPackage);
                    // Un array es como una secuencia: el tipo que se guarda en
                    // el TypedefManager ya es un tipo Java valido. No hay que
                    // convertirlo a posteriori.
                    typeString = m_current_xml_type.basicMapping(typeString);
                    holderType = m_current_xml_type.basicOutMapping(typeString);
                    String bounds = "";
                    for (int k = 0; k < decl.getChildNodes().getLength(); k++)
                        bounds += "[]";

                    TypedefManager.getInstance().typedef(
                                        scopedTypeName,
                                        typeString + bounds,
                                        TypeManager.convert(scopedTypeName),
                                        scopedTypeName, null, null); /* holderType */
                } else {

                    // Si no es un array se almacena lo que nos devolvio
                    // _currentXmlType.getAbsoluteTypedefType
                    // que para el caso de secuencias es un tipo Java ya
                    // convertido,
                    // pero en el resto de los casos es todavia un tipo IDL.
                    String helperType = "";

                    if (type.getTagName().equals(OMG_scoped_name)) {

                        // helperType =
                        // _currentXmlType.getTypedefScopedHelper(type);
                        helperType = scopedTypeName;

                        if (holderType == null) {

                            // if we have a typedef mapped to a scoped name, we
                            // use the
                            // holder of the originary typedef, if exists
                            String type_name = type.getAttribute(OMG_name);
                            holderType = TypedefManager
                                .getInstance().getUnrolledHolderType(type_name);

                            if (holderType == null) {

                                // If not exists, the original type is not a
                                // typedef, so we
                                // use the holder of the original type
                                holderType = typeString;
                            } else {
                                holderType = TypeManager.convert(holderType);
                            }
                        }
                    }

                    TypedefManager
                        .getInstance().typedef(scopedTypeName, typeString,
                                               holderType, helperType, null,
                                               null);
                }
            } else { // C++ only
                String definition, kind = null;
                if (decl.getTagName().equals(OMG_array))
                    definition = OMG_array;
                else if (type.getTagName().equals(OMG_type)) {
                    definition = OMG_kind; // raro, pero es como se ha
                                           // estado usando hasta ahora
                    kind = type.getAttribute(OMG_kind);
                } else
                    definition = type.getTagName();
                if (definition.equals(OMG_scoped_name)) {
                    definition = TypedefManager
                        .getInstance()
                        .getDefinitionType(type.getAttribute(OMG_name));
                    if (definition != null && definition.equals(OMG_kind))
                        kind = TypedefManager
                            .getInstance().getKind(type.getAttribute(OMG_name));
                }
                TypedefManager.getInstance().typedef(scopedTypeName, null,
                                                     null, null, definition,
                                                     kind);
            }
        }
    }

    private void checkScopes(Element el, Scope scope)
        throws SemanticException,
        IOException
    {

        /*
         * System.out.println("----"); System.out.println("Elemento:"+el);
         * System.out.println("Scope"+scope+"\n El Volcado:"); _scope.dump();
         * System.out.println("----");
         */
        String tag = el.getTagName();
        Scope newScope = scope;
        
        
//         if (el.getAttribute(OMG_name).compareTo("ExceptionHolder") == 0) {
//         	System.out.println("encontrado");
//         }
        
        
        

        try {            
           
            if (tag.equals(OMG_specification)) { // para evitar errores
                                                 // con definiciones en ambito
                                                 // global
                scope.setElement(el);
            }
            // ScopedName
            else if (tag.equals(OMG_scoped_name)) {
                String scopedName = el.getAttribute(OMG_name);

                Element parent = (Element) el.getParentNode();
                String parentTag = parent.getTagName();
                if (!parentTag.equals(OMG_typedef)) {
                    // introducido para controlar identificadores
                    // reservados por el mapeo de IDL a Java
                    String modScopedName = checkReservedNames(scopedName, false);
                    String completeScopedName = "";
                    try {
                        completeScopedName = scope
                            .getCompleteName(modScopedName);
                    }
                    catch (SemanticException e) {
                        modScopedName = checkReservedNames(scopedName, true);
                        completeScopedName = scope
                            .getCompleteName(modScopedName);
                    }

                    // la siguiente comprobacion se explica en el
                    // interior del bucle
                    if (parentTag.equals(OMG_struct)
                        || parentTag.equals(OMG_exception)
                        || parentTag.equals(OMG_union)) {
                        NodeList nodes = parent.getChildNodes();
                        for (int i = 0; i < nodes.getLength(); i++) {
                            if (((Element) nodes.item(i))
                                .getAttribute(OMG_name).equals(scopedName)
                                && !((Element) nodes.item(i))
                                    .getTagName().equals(OMG_scoped_name)) {
                                // si se cumple esto tenemos una
                                // declaracion en IDL del tipo 'lalo lalo',
                                // donde el segundo lalo es el
                                // identificador de un elemento de un tipo
                                // compuesto y el primero un tipo definido en el
                                // scope que
                                // contiene al compuesto (aqui estamos manejando
                                // el primer 'lalo'); la gram�tica lo permite
                                scope = scope.getParent();
                                completeScopedName = scope
                                    .getCompleteName(modScopedName);
                                break;
                            }
                        }
                    }

                    scopedName = completeScopedName;
                }

                if (m_gen_package.equals("org.omg")) {

                    if (scopedName.startsWith("::org::omg::org::omg")) {
                        scopedName = scopedName.substring(10);
                    }
                }

                el.setAttribute(OMG_name, scopedName);

            } // Declaraciones
            else if (tag.equals(OMG_simple_declarator) || tag.equals(OMG_array)) {

                String scopeName = el.getAttribute(OMG_name);
                String modScopedName = scopeName;
                try {
                    scopeName = scope.getCompleteName(modScopedName);
                }
                catch (SemanticException e) { // para comprobacion de
                                              // identificadores reservados por
                                              // mapping
                    modScopedName = checkReservedNames(el
                        .getAttribute(OMG_name), true);
                    scopeName = scope.getCompleteName(modScopedName);
                }

                el.setAttribute(OMG_scoped_name, scopeName);
                el.setAttribute(OMG_variable_size_type, getVariableSizeType(el));
                // Module (defines scope)

            } else if (tag.equals(OMG_module)) {

                String scopeName = el.getAttribute(OMG_name);

                // El nombre de un m�dulo no puede repetirse para otro
                // m�dulo en su interior; ej: A::A
                if (scopeName.equalsIgnoreCase(scope.getName()))
                    throw new SemanticException("Invalid use of: " + scopeName);

                if (scope.isInThisScope(scopeName))
                    // un m�dulo puede ser reabierto; en ese caso, su
                    // �mbito (scope) ya se habr�a definido anteriormente
                    newScope = Scope.getGlobalScope(scope
                        .getCompleteName(scopeName));
                else {
                    // se salva el nombre del m�dulo
                    m_type_handler.saveMapping(scope, scopeName, new Integer(el
                        .getAttribute("line")).intValue());
                    // un m�dulo define su propio scope
                    newScope = new Scope(scopeName, scope, Scope.KIND_MODULE,
                                         el);
                }

                // ajustes para cuando hay que generar c�digo CORBA (se
                // puede mejorar)
                if (m_gen_package.equals("org.omg")) {
                    if (scopeName.equals("org") || scopeName.equals("omg")) {
                        el.removeAttribute(OMG_Do_Not_Generate_Code);
                    }
                } else {
                    if ((scopeName.equals("java") || scopeName.equals("io")
                         || scopeName.equals("lang") || scopeName.equals("rmi"))
                        && !IncludeORB.isIncludedModule("CORBA")) {
                        el.removeAttribute(OMG_Do_Not_Generate_Code);
                    }
                }
                if (IncludeORB.isHardCodedModule(scopeName)
                    && !IncludeORB.isIncludedModule(scopeName)) {
                    el.removeAttribute(OMG_Do_Not_Generate_Code);
                } 
                
                
                Integer line = new Integer(el.getAttribute("line"));
                String module_file = 
                    Preprocessor.getInstance().locateFile(line.intValue());
                
                boolean generateCodesss = CompilerConf.getNotGenerateCode();
                boolean moduleCoincidencia = module_file.equals(CompilerConf.getFile());
                                    
                if(CompilerConf.getNotGenerateCode()&&!module_file.equals(CompilerConf.getFile()))
                {
                    el.setAttribute(OMG_Do_Not_Generate_Code, "TRUE");
                }
                
                
            } // INTERFACE - define scope
            else if (tag.equals(OMG_interface)) {

                String scopeName = el.getAttribute(OMG_name);
                scopeName = checkReservedNames(scopeName, true);
                el.setAttribute(OMG_name, scopeName);

                m_type_handler.saveMapping(scope, scopeName, new Integer(el
                    .getAttribute("line")).intValue());

                String prefix = el.getAttribute(OMG_prefix);
                RepositoryIdManager.getInstance().setName(el, scope, scopeName,
                                                          prefix);

                String isAbstractS = el.getAttribute(OMG_abstract);
                boolean isAbstract = (isAbstractS != null)
                                     && (isAbstractS.equals(OMG_true));
                String isForwardS = el.getAttribute(OMG_fwd);
                boolean isForward = (isForwardS != null)
                                    && (isForwardS.equals(OMG_true));
                String isLocalS = el.getAttribute(OMG_local);
                boolean isLocal = (isLocalS != null)
                                  && (isLocalS.equals(OMG_true));
                if (isForward) {
                    if (isAbstract) {
                        newScope = new Scope(scopeName, scope,
                                             Scope.KIND_INTERFACE_FWD_ABS, el);
                    } else {
                        newScope = new Scope(scopeName, scope,
                                             Scope.KIND_INTERFACE_FWD, el);
                    }
                } else {
                    if (isAbstract) {
                        newScope = new Scope(scopeName, scope,
                                             Scope.KIND_INTERFACE_ABS, el);
                    } else {
                        newScope = new Scope(scopeName, scope,
                                             Scope.KIND_INTERFACE, el);
                    }
                }

                NodeList list = el.getElementsByTagName(OMG_inheritance_spec);

                if (list.getLength() == 1) {
                    Element inheritance = (Element) list.item(0);
                    NodeList inherits = inheritance.getChildNodes();
                    for (int k = 0; k < inherits.getLength(); k++) {
                        Element inheritedScopeEl = (Element) inherits.item(k);
                        String inheritedScope = checkReservedNames(inheritedScopeEl.getAttribute(OMG_name), true);
                        //Scope inhScope =
                        // _scope.getScope(scope.getCompleteName(inheritedScope));
                        Scope inhScope = m_scope.getGlobalScopeInterface(scope.getCompleteName(inheritedScope));
                        if (inhScope == null) {
                            throw new SemanticException(
                                          "It's not allowed to inherit from a forward-declared interface whose definition has not yet been seen:\nDerived interface: '"
                                          + el.getAttribute(OMG_name)
                                          + "'.\nBase interface: '"
                                          + inheritedScope
                                          + "'.");
                        }
                        String localInheritance = inhScope
                            .getElement().getAttribute(OMG_local);
                        if (localInheritance != null) {
                            if (localInheritance.equals(OMG_true) && !isLocal)
                                // Si hereda de local y no se ha definido
                                // como local
                                throw new SemanticException(
                                              "It's not allowed to inherit from a local interface whithout explicit local declaration:\nDerived interface: '"
                                              + el.getAttribute(OMG_name)
                                              + "'.\nBase interface: '"
                                              + inheritedScope
                                              + "'.");
                        }
                        newScope.addInheritance(inhScope);
                    }

                    // Check the redefinition of operations and attributes in
                    // the derived interface
                    Vector inheritance_scopes = newScope.getInheritance();
                    InterfaceInheritance.checkRedefinitions(el,
                                                            inheritance_scopes);
                }

                NodeList listAttrDcl = el.getElementsByTagName(OMG_attr_dcl);

                for (int i = 0; i < listAttrDcl.getLength(); i++) {
                    Element attrEl = (Element) listAttrDcl.item(i);
                    list = attrEl.getElementsByTagName(OMG_attribute);
                    for (int j = 0; j < list.getLength(); j++) {
                        String name = ((Element) list.item(j))
                            .getAttribute(OMG_name);
                        newScope.add(name, Scope.KIND_ATTRIBUTE);
                    }
                }

                NodeList listOpsDcl = el.getElementsByTagName(OMG_op_dcl);

                for (int i = 0; i < listOpsDcl.getLength(); i++) {
                    Element opEl = (Element) listOpsDcl.item(i);
                    String name = opEl.getAttribute(OMG_name);
                    newScope.add(name, Scope.KIND_OPERATION);
                }

                String scopedName = newScope.getCompleteName(scopeName);
                el.setAttribute(OMG_scoped_name, scopedName);
                el
                    .setAttribute(OMG_variable_size_type,
                                  getVariableSizeType(el));
                // DAVV - para la nueva sem�ntica en C++ del TypedefManager
                if (CompilerConf.getCompilerType().equals("Cpp"))
                    TypedefManager
                        .getInstance().typedef(
                                   newScope.getCompleteName(scopeName),
                                   null, null, null, OMG_interface,null);

            } // Native - �define scope?
            else if (tag.equals(OMG_native)) {

                String scopeName = el.getAttribute(OMG_name);
                scopeName = checkReservedNames(scopeName, false);
                el.setAttribute(OMG_name, scopeName);

                m_type_handler.saveMapping(scope, scopeName, new Integer(el
                    .getAttribute("line")).intValue());

                String prefix = el.getAttribute(OMG_prefix);

                //System.out.println("El prefijo:"+prefix+", el
                // nombre"+scopeName);
                RepositoryIdManager.getInstance().setName(el, scope, scopeName,
                                                          prefix);
                newScope = new Scope(scopeName, scope,
                                     Scope.KIND_INTERFACE_FWD_ABS, el);
                el
                    .setAttribute(OMG_variable_size_type,
                                  getVariableSizeType(el));

                // para la nueva sem�ntica en C++ del TypedefManager
                if (CompilerConf.getCompilerType().equals("Cpp"))
                    TypedefManager
                        .getInstance()
                        .typedef(scope.getCompleteName(scopeName), null, null,
                                 null, OMG_native, null);

            } // TYPEDEF
            else if (tag.equals(OMG_typedef)) {

                NodeList list = el.getChildNodes();
                //System.out.println("Dentro de un
                // typedefSemantics:"+el.getAttribute(OMG_name));
                for (int i = 0; i < list.getLength(); i++) {
                    Element itemi = (Element) list.item(i);

                    if (i == 0) { // DAVV - tipo
                        /*
                         * no tengo muy claro por que se hace esto; salvo
                         * que el typedef sea de una secuencia, itemi no va a
                         * tener hijos OMG_scoped_name ni por asomo
                         */
                        NodeList scopelist = itemi
                            .getElementsByTagName(OMG_scoped_name);

                        for (int j = 0; j < scopelist.getLength(); j++) {
                            Element scoped_name_el = (Element) scopelist
                                .item(j);
                            String scoped_name = scoped_name_el
                                .getAttribute(OMG_name);
                            String completeScopeName = scope
                                .getCompleteName(scoped_name);
                            scoped_name_el.setAttribute(OMG_scoped_name,
                                                        completeScopeName);
                        }
                        // por adelantado, para no tener problemas con el
                        // addtypedef m�s abajo
                        if (itemi.getTagName().equals(OMG_scoped_name)) {
                            String modScopedName = checkReservedNames(itemi
                                .getAttribute(OMG_name), false);
                            String completeScopedName = "";
                            try {
                                completeScopedName = scope
                                    .getCompleteName(modScopedName);
                            }
                            catch (SemanticException e) {
                                modScopedName = checkReservedNames(itemi
                                    .getAttribute(OMG_name), true);
                                completeScopedName = scope
                                    .getCompleteName(modScopedName);
                            }
                            itemi.setAttribute(OMG_name, completeScopedName);
                        }

                    } else { // declaracion(es)
                        String scopeName = itemi.getAttribute(OMG_name);
                        scopeName = checkReservedNames(scopeName, false);
                        itemi.setAttribute(OMG_name, scopeName);
                        m_type_handler.saveMapping(scope, scopeName,
                                                   new Integer(itemi
                                                       .getAttribute("line"))
                                                       .intValue());

                        String prefix = el.getAttribute(OMG_prefix);
                        RepositoryIdManager.getInstance().setName(itemi, scope,
                                                                  scopeName,
                                                                  prefix);

                        newScope.add(scopeName, Scope.KIND_TYPE);

                        String completeScopeName = scope
                            .getCompleteName(scopeName);
                        itemi.setAttribute(OMG_scoped_name, completeScopeName);
                    }
                }

                addTypeDef(el);
                el.setAttribute(OMG_variable_size_type,
                                getVariableSizeType(el));

            } // STRUCT (define scope)
            else if (tag.equals(OMG_struct)) {
                String scopeName = el.getAttribute(OMG_name);
                scopeName = checkReservedNames(scopeName, false);
                el.setAttribute(OMG_name, scopeName);

                m_type_handler.saveMapping(scope, scopeName, new Integer(el
                    .getAttribute("line")).intValue());

                String prefix = el.getAttribute(OMG_prefix);
                RepositoryIdManager.getInstance().setName(el, scope, scopeName,
                                                          prefix);

                String isForwardS = el.getAttribute(OMG_fwd);
                boolean isForward = (isForwardS != null)
                                    && (isForwardS.equals(OMG_true));

                if (isForward)
                    newScope = new Scope(scopeName, scope,
                                         Scope.KIND_STRUCT_FWD, el);
                else
                    newScope = new Scope(scopeName, scope, Scope.KIND_STRUCT,
                                         el);

                // DAVV - NO SIRVE - getElementsByTagName() busca RECURSIVAMENTE
                // dentro del �rbol, no s�lo en primer nivel
                //       as� que ante un struct uno { struct internal {long lalo;}; };
                // registraria 'lalo' en el scope de uno
                /*
                 * NodeList list; list =
                 * el.getElementsByTagName(OMG_simple_declarator);
                 * addToScope(newScope, list); list =
                 * el.getElementsByTagName(OMG_array); addToScope(newScope,
                 * list);
                 */

                NodeList children = el.getChildNodes();
                for (int i = 0; i < children.getLength(); i++) {
                    Element child = (Element) children.item(i);
                    if (child.getTagName().equals(OMG_simple_declarator)
                        || child.getTagName().equals(OMG_array))
                        newScope.add(child.getAttribute(OMG_name),
                                     Scope.KIND_ELEMENT);
                }

                // Added to support the declaration of structs into a Union body
                String scopedName = newScope.getCompleteName(scopeName);
                el.setAttribute(OMG_scoped_name, scopedName);
                el
                    .setAttribute(OMG_variable_size_type,
                                  getVariableSizeType(el));

                // para la nueva sem�ntica en C++ del TypedefManager
                if (CompilerConf.getCompilerType().equals("Cpp"))
                    TypedefManager.getInstance().typedef(scopedName, null,
                                                         null, null,
                                                         OMG_struct, null);

            } // UNION - define scope
            else if (tag.equals(OMG_union)) {

                String scopeName = el.getAttribute(OMG_name);
                scopeName = checkReservedNames(scopeName, false);
                el.setAttribute(OMG_name, scopeName);
                m_type_handler.saveMapping(scope, scopeName, new Integer(el
                    .getAttribute("line")).intValue());

                String prefix = el.getAttribute(OMG_prefix);
                RepositoryIdManager.getInstance().setName(el, scope, scopeName,
                                                          prefix);

                String isForwardS = el.getAttribute(OMG_fwd);
                boolean isForward = (isForwardS != null)
                                    && (isForwardS.equals(OMG_true));

                if (isForward)
                    newScope = new Scope(scopeName, scope,
                                         Scope.KIND_UNION_FWD, el);
                else
                    newScope = new Scope(scopeName, scope, Scope.KIND_UNION, el);

                // DAVV - NO SIRVE - getElementsByTagName() busca RECURSIVAMENTE
                // dentro del �rbol, no s�lo en primer nivel
                //       as� que ante un union uno switch(short) { case 1: struct
                // internal {long lalo;}; }; registraria 'lalo' en el scope de
                // uno
                /*
                 * NodeList list; /*list =
                 * el.getElementsByTagName(OMG_enumerator); // DAVV - se
                 * a�adir�n cuando se procese el enum que los contiene...
                 * addToScope(newScope, list);*-/ list =
                 * el.getElementsByTagName(OMG_simple_declarator);
                 * addToScope(newScope, list); list =
                 * el.getElementsByTagName(OMG_array); addToScope(newScope,
                 * list);
                 */

                NodeList children = el.getChildNodes();
                for (int i = 1; i < children.getLength(); i++) { //  i==0,
                                                                 // switch
                    Element caseLabel = (Element) children.item(i);
                    Element value = (Element) caseLabel.getLastChild();
                    Element decl = (Element) value.getLastChild(); // simple_declarator
                                                                   // or
                                                                   // array_declarator
                    newScope.add(decl.getAttribute(OMG_name),
                                 Scope.KIND_ELEMENT);
                }

                // Added to support the declaration of unions into a Union body
                String scopedName = newScope.getCompleteName(scopeName);
                el.setAttribute(OMG_scoped_name, scopedName);
                el.setAttribute(OMG_variable_size_type, getVariableSizeType(el));


                // para la nueva sem�ntica en C++ del TypedefManager
                if (CompilerConf.getCompilerType().equals("Cpp"))
                    TypedefManager.getInstance().typedef(scopedName, null,
                                                         null, null, OMG_union,
                                                         null);

                if (!isForward) {
                    Element switch_el = (Element) el.getFirstChild();
                    //NodeList scopelist =
                    // switch_el.getElementsByTagName(OMG_scoped_name); 
                    // - solo puede haber uno
                    Element switch_type = (Element) switch_el.getFirstChild();

                    Union union;
                    if (CompilerConf.getCompilerType().equals("Java"))
                        union = new UnionJava(scope, el);
                    else
                        union = new UnionCpp(scope, el);

                    //if (scopelist.getLength() > 0)
                    if (switch_type.getTagName().equals(OMG_scoped_name)
                        || switch_type.getTagName().equals(OMG_enum)) {

                        //Element scoped_name_el = (Element)scopelist.item(0);
                        //String scoped_name =
                        // scoped_name_el.getAttribute(OMG_name);
                        String scoped_name = switch_type.getAttribute(OMG_name);
                        if (switch_type.getTagName().equals(OMG_enum)) {
                            scoped_name = newScope
                                .getCompleteName(scopeName).substring(2)
                                          + "::" + scoped_name;
                            NodeList list = switch_type
                                .getElementsByTagName(OMG_enumerator);
                            for (int i = 0; i < list.getLength(); i++) {
                                newScope
                                    .add(((Element) list.item(i))
                                        .getAttribute(OMG_name),
                                         Scope.KIND_ELEMENT);
                                // para registrar los nombres de
                                // los valores del 'enum' antes de que se haya
                                // procesado �ste
                                //      - facilita los chequeos de las etiquetas de
                                // la uni�n que vienen a continuaci�n
                                //      - y evita que se detecte una falsa
                                // redefinici�n de identificadores cuando se
                                // procesa el 'enum' (ver m�s abajo, "else if
                                // (tag.equals(OMG_enum)) {")
                                ((Element) list.item(i))
                                    .setAttribute("preScoped", "true");
                            }
                        } else
                            scoped_name = newScope.getCompleteName(scoped_name);

                        union.setScopedDiscriminator(scoped_name);

                        // We have a scoped_name discriminator. We must check
                        // its type.
                        if (switch_type.getTagName().equals(OMG_scoped_name)) {
                            try {
                                union.checkSwitchType();
                            }
                            catch (SemanticException e) {
                                e.locate(switch_type);
                                throw e;
                            }
                        }
                    }

                    union.checkCaseLabel(switch_el, newScope);
                    union.fillSwitchBody();
                    union.checkCaseLabelValues();
                    UnionManager.getInstance().put(el, union);
                }
                

            } // ENUM - NO define scope
            else if (tag.equals(OMG_enum)) {

                String scopeName = el.getAttribute(OMG_name);
                scopeName = checkReservedNames(scopeName, false);
                el.setAttribute(OMG_name, scopeName);
                m_type_handler.saveMapping(scope, scopeName, new Integer(el
                    .getAttribute("line")).intValue());

                String prefix = el.getAttribute(OMG_prefix);
                RepositoryIdManager.getInstance().setName(el, scope, scopeName,
                                                          prefix);
                // los enums no definen nuevo scope!!
                //newScope = new Scope(scopeName, scope, Scope.KIND_TYPE, el);

                scope.add(scopeName, Scope.KIND_TYPE);
                // DAVV - los valores definidos para el enum pertenecen al mismo
                // scope que �l
                //addToScope(newScope, list);
                NodeList list;
                list = el.getElementsByTagName(OMG_enumerator);
                for (int i = 0; i < list.getLength(); i++) {
                    Element item = (Element) list.item(i);
                    if (item.getAttribute("preScoped").equals("")) // para casos
                                                                   // en que se
                                                                   // define el
                                                                   // 'enum' en
                                                                   // el
                                                                   // discriminante
                                                                   // de un
                                                                   // 'union'
                        scope.add(item.getAttribute(OMG_name),
                                  Scope.KIND_ELEMENT);
                    else
                        item.removeAttribute("preScoped");
                    m_type_handler.saveMapping(scope, item
                        .getAttribute(OMG_name), new Integer(item
                        .getAttribute("line")).intValue());
                }

                // Added to support the declaration of enumerations into a Union
                // body
                String scopedName = newScope.getCompleteName(scopeName);
                el.setAttribute(OMG_scoped_name, scopedName);
                el.setAttribute(OMG_variable_size_type,
                                  getVariableSizeType(el));

                // para la nueva sem�ntica en C++ del TypedefManager
                if (CompilerConf.getCompilerType().equals("Cpp"))
                    TypedefManager.getInstance().typedef(scopedName, null,
                                                         null, null, OMG_enum,
                                                         null);

            } // CONSTANT
            else if (tag.equals(OMG_const_dcl)) {

                String scopeName = el.getAttribute(OMG_name);
                m_type_handler.saveMapping(scope, scopeName, new Integer(el
                    .getAttribute("line")).intValue());
                scope.add(scopeName, Scope.KIND_CONST);

                String scopedName;

                scopedName = scope.getCompleteName(scopeName);
                el.setAttribute(OMG_scoped_name, scopedName);

                // Scoped id in a constant value
                NodeList scopelist = el.getElementsByTagName(OMG_scoped_name);

                for (int i = 0; i < scopelist.getLength(); i++) {
                    Element scoped_name_el = (Element) scopelist.item(i);
                    String scoped_name = scoped_name_el.getAttribute(OMG_name);
                    String scoped_Name2 = scope.getCompleteName(scoped_name);
                    scoped_name_el.setAttribute(OMG_scoped_name, scoped_Name2);
                    scoped_name_el.setAttribute(OMG_name, scoped_Name2); // adelantandonos
                                                                         // para
                                                                         // no
                                                                         // tener
                                                                         // problemas
                                                                         // mas
                                                                         // abajo
                }

                if ((scopedName != null) && !scopedName.equals("")) {
                    NodeList nodes = el.getChildNodes();
                    Element typeEl = (Element) nodes.item(0);
                    //String type = _currentXmlType.getType(typeEl);
                    //String deepType = _currentXmlType.getDeepType(typeEl);
                    String type, deepType;
                    Element exprEl = (Element) nodes.item(1);
                    Object expr;

                    if (CompilerConf.getCompilerType().equals("Java")) {
                        type = m_current_xml_type.getType(typeEl);
                        deepType = ((XmlJavaSemanticType) m_current_xml_type)
                            .getDeepType(typeEl);
                        if (deepType.equals(OMG_enum))
                            expr = XmlExpr2Java.getEnumExpr(exprEl, type);
                        //else if (type.indexOf("::") < 0)
                        else
                            expr = XmlExpr2Java.getExpr(exprEl, type);
                        //else
                        //    expr = XmlExpr2Java.getExpr(exprEl, deepType);
                    } else {
                        type = m_current_xml_type.getType(typeEl);
                        deepType = ((XmlCppSemanticType) m_current_xml_type)
                            .getDeepKind(typeEl);
                        String definition = ((XmlCppSemanticType) m_current_xml_type)
                            .getDefinitionType(typeEl);
                        //if (deepType.equals(OMG_enum))
                        if (definition.equals(OMG_enum))
                            expr = es.tid.TIDIdlc.xml2cpp.XmlExpr2Cpp
                                .getEnumExpr(exprEl);
                        else
                            expr = es.tid.TIDIdlc.xml2cpp.XmlExpr2Cpp
                                .getExpr(exprEl, m_current_xml_type
                                    .basicMapping(deepType));
                        /*
                         * else if ((type.indexOf("::") < 0) ||
                         * (type.indexOf("CORBA") == 0)) expr =
                         * es.tid.TIDIdlc.xml2cpp.XmlExpr2Cpp.getExpr(exprEl,
                         * type); else ISP expr =
                         * es.tid.TIDIdlc.xml2cpp.XmlExpr2Cpp.getExpr(exprEl,
                         * deepType);
                         */
                    }
                    IdlConstants.getInstance().add(scopedName, type, expr);
                }
                el
                    .setAttribute(OMG_variable_size_type,
                                  getVariableSizeType(el));

            } // EXCEPTION (defines scope)
            else if (tag.equals(OMG_exception)) {

                String scopeName = el.getAttribute(OMG_name);
                scopeName = checkReservedNames(scopeName, false);
                el.setAttribute(OMG_name, scopeName);
                m_type_handler.saveMapping(scope, scopeName, new Integer(el
                    .getAttribute("line")).intValue());

                String prefix = el.getAttribute(OMG_prefix);
                RepositoryIdManager.getInstance().setName(el, scope, scopeName,
                                                          prefix);
                newScope = new Scope(scopeName, scope, Scope.KIND_TYPE, el);

                // DAVV - NO SIRVE - getElementsByTagName() busca RECURSIVAMENTE
                // dentro del �rbol, no s�lo en primer nivel
                //       as� que ante un struct uno { struct internal {long lalo;}; };
                // registraria 'lalo' en el scope de uno
                /*
                 * NodeList list; list =
                 * el.getElementsByTagName(OMG_simple_declarator);
                 * addToScope(newScope, list); list =
                 * el.getElementsByTagName(OMG_array); addToScope(newScope,
                 * list);
                 */

                NodeList children = el.getChildNodes();
                for (int i = 0; i < children.getLength(); i++) {
                    Element child = (Element) children.item(i);
                    if (child.getTagName().equals(OMG_simple_declarator)
                        || child.getTagName().equals(OMG_array))
                        newScope.add(child.getAttribute(OMG_name),
                                     Scope.KIND_ELEMENT);
                }

                String scopedName = newScope.getCompleteName(scopeName);
                el.setAttribute(OMG_scoped_name, scopedName);
                el
                    .setAttribute(OMG_variable_size_type,
                                  getVariableSizeType(el));
                // DAVV - para la nueva sem�ntica en C++ del TypedefManager
                if (CompilerConf.getCompilerType().equals("Cpp"))
                    TypedefManager.getInstance().typedef(scopedName, null,
                                                         null, null,
                                                         OMG_exception, null);

            } // Pragma
            else if (tag.equals(OMG_pragma)) {

                String pragmaText = el.getAttribute(OMG_pragma_value);

                try {

                    StringTokenizer tok = new StringTokenizer(pragmaText, " ",
                                                              false);
                    String pragmaName = tok.nextToken();
                    String objName = tok.nextToken();

                    if (pragmaName.equals("version")) {
                        String versionNumber = tok.nextToken();
                        RepositoryIdManager
                            .getInstance().setVersion(scope, objName,
                                                      versionNumber);
                    } else if (pragmaName.equals("ID")) {
                        String objId = tok.nextToken();
                        RepositoryIdManager.getInstance().setId(scope, objName,
                                                                objId);
                    } else {
                        throw new SemanticException(
                                                    "Invalid #pragma directive: "
                                                                                                                                                                                                                + pragmaText);
                    }
                }
                catch (SemanticException e) {
                    throw e;
                }
                catch (Exception e) {
                    e.printStackTrace();
                    throw new SemanticException("Invalid #pragma directive: "
                                                + pragmaText);
                }

            } // VALUETYPE - defines scope
            else if (tag.equals(OMG_valuetype)) {
                String scopeName = el.getAttribute(OMG_name);
                scopeName = checkReservedNames(scopeName, false);
                el.setAttribute(OMG_name, scopeName);
                m_type_handler.saveMapping(scope, scopeName, new Integer(el
                    .getAttribute("line")).intValue());

                String prefix = el.getAttribute(OMG_prefix);
                RepositoryIdManager.getInstance().setName(el, scope, scopeName,
                                                          prefix);

                String isAbstractS = el.getAttribute(OMG_abstract);
                boolean isAbstract = (isAbstractS != null)
                                     && (isAbstractS.equals(OMG_true));
                String isForwardS = el.getAttribute(OMG_fwd);
                boolean isForward = (isForwardS != null)
                                    && (isForwardS.equals(OMG_true));

                if (isForward) {
                    if (isAbstract) {
                        newScope = new Scope(scopeName, scope,
                                             Scope.KIND_VALUETYPE_FWD_ABS, el);
                    } else {
                        newScope = new Scope(scopeName, scope,
                                             Scope.KIND_VALUETYPE_FWD, el);
                    }
                } else {
                    if (isAbstract) {
                        newScope = new Scope(scopeName, scope,
                                             Scope.KIND_VALUETYPE_ABS, el);
                    } else {
                        newScope = new Scope(scopeName, scope,
                                             Scope.KIND_VALUETYPE, el);
                    }
                }

                NodeList list = el
                    .getElementsByTagName(OMG_value_inheritance_spec);

                if (list.getLength() == 1) {

                    Element inheritance = (Element) list.item(0);
                    NodeList inherits = inheritance.getChildNodes();

                    for (int k = 0; k < inherits.getLength(); k++) {

                        Element inheritedScopeEl = (Element) inherits.item(k);
                        String inherited_tag = inheritedScopeEl.getTagName();

                        if (inherited_tag.equals(OMG_scoped_name)) {

                            String inheritedScope = checkReservedNames(inheritedScopeEl.getAttribute(OMG_name), false);
                            Scope inhScope = m_scope.getGlobalScopeInterface(scope.getCompleteName(inheritedScope));
                            Element elfather = inhScope.getElement();
                            String father_tag = elfather.getTagName();

                            if (father_tag.equals(OMG_valuetype)) {
                                newScope.addInheritance(inhScope);
                            } else {
                                throw new SemanticException(
                                                            "Invalid inheritance in '"
                                                                                                                                                                                                                                                + scopeName
                                                                                                                                                                                                                                                + "': '"
                                                                                                                                                                                                                                                + inheritedScope
                                                                                                                                                                                                                                                + "' is not a valuetype");
                            }
                        } else if (inherited_tag.equals(OMG_supports)) {
                            NodeList supports = inheritedScopeEl
                                .getChildNodes();
                            for (int j = 0; j < supports.getLength(); j++) {
                                Element supportedScopeEl = (Element) supports
                                    .item(j);
                                String supported_tag = supportedScopeEl
                                    .getTagName();
                                if (supported_tag.equals(OMG_scoped_name)) {
                                    String supportedScope = checkReservedNames(supportedScopeEl.getAttribute(OMG_name), false);
                                    Scope inhScope = m_scope.getGlobalScopeInterface(scope.getCompleteName(supportedScope));
                                    Element elfather = inhScope.getElement();
                                    String father_tag = elfather.getTagName();

                                    if (father_tag.equals(OMG_interface)) {
                                        newScope.addInheritance(inhScope);
                                    } else {
                                        throw new SemanticException(
                                                      "Invalid support in '"
                                                      + scopeName
                                                      + "': '"
                                                      + supportedScope
                                                      + "' is not an interface");
                                    }	
                                } else {
                                    throw new SemanticException(
                                                  "'"
                                                  + scopeName
                                                  + " can�t support '"
                                                  + supportedScopeEl.getAttribute(OMG_name)
                                                  + "'");
                                }	
                            }
                        } else {
                            throw new SemanticException(
                                          "'"
                                          + scopeName
                                          + " can�t inherit from '"
                                          + inheritedScopeEl.getAttribute(OMG_name)
                                          + "'");
                        }
                    }

                    //Check the allowed inheritances
                    ValuetypeInheritance.checkInheritance(el, newScope
                        .getValuetypeInheritance(), inheritance
                        .getAttribute(OMG_truncatable));

                    // Check the redefinition of operations and attributes in
                    // the derived interface
                    ValuetypeInheritance.checkRedefinitions(el, newScope
                        .getInheritance());
                }

                NodeList listStateMember = el
                    .getElementsByTagName(OMG_state_member);

                for (int i = 0; i < listStateMember.getLength(); i++) {
                    Element stateMemberEl = (Element) listStateMember.item(i);
                    list = stateMemberEl
                        .getElementsByTagName(OMG_simple_declarator);
                    for (int j = 0; j < list.getLength(); j++) {
                        String name = ((Element) list.item(j))
                            .getAttribute(OMG_name);
                        newScope.add(name, Scope.KIND_STATE_MEMBER);
                    }

                    // los arrays tambi�n tienen derecho a la vida...
                    list = stateMemberEl.getElementsByTagName(OMG_array);
                    for (int j = 0; j < list.getLength(); j++) {
                        String name = ((Element) list.item(j))
                            .getAttribute(OMG_name);
                        newScope.add(name, Scope.KIND_STATE_MEMBER);
                    }
                }

                NodeList listAttrDcl = el.getElementsByTagName(OMG_attr_dcl);

                for (int i = 0; i < listAttrDcl.getLength(); i++) {
                    Element attrEl = (Element) listAttrDcl.item(i);
                    list = attrEl.getElementsByTagName(OMG_attribute);
                    for (int j = 0; j < list.getLength(); j++) {
                        String name = ((Element) list.item(j))
                            .getAttribute(OMG_name);
                        newScope.add(name, Scope.KIND_ATTRIBUTE);
                    }
                }
                // en el mapping de Java, los 'value boxes' de tipos no
                // basicos son parecidos a typedef:
                // DAVV - no se construye una clase nueva para ellos, si no que
                // se utiliza la ya existente para
                // DAVV - el elemento que contienen
                String isBoxedS = el.getAttribute(OMG_boxed);
                boolean isBoxed = (isBoxedS != null)
                                  && (isBoxedS.equals(OMG_true));
                if (CompilerConf.getCompilerType().equals("Java")) {
                    if (isBoxed) {
                        NodeList nodes = el.getChildNodes();
                        Element typeEl = (Element) nodes.item(0);
                        String type = m_current_xml_type.getType(typeEl);
                        if (!(type.equals("boolean") || type.equals("char")
                              || type.equals("byte") || type.equals("short")
                              || type.equals("int") || type.equals("long")
                              || type.equals("float") || type.equals("double"))) {
                            String completeScopeName = scope
                                .getCompleteName(scopeName);
                            // getAbsoluteTyupedefType no aporta
                            // diferencias con respecto a getTypedefType
                            //String typeString =
                            // _currentXmlType.getAbsoluteTypedefType(typeEl);
                            String typeString = ((XmlJavaSemanticType) m_current_xml_type)
                                .getTypedefType(typeEl);
                            TypedefManager
                                .getInstance().typedef(completeScopeName,
                                                       typeString,
                                                       completeScopeName,
                                                       completeScopeName, null,
                                                       null);
                        }
                    }
                } else
                    // para la nueva sem�ntica en C++ del TypedefManager
                    TypedefManager.getInstance()
                        .typedef(scope.getCompleteName(scopeName), null, null,
                                 null, OMG_valuetype, null);

            }

        }
        catch (SemanticException e) { // DAVV - localizando excepciones
            if (!e.isLocated())
                e.locate(el);
            throw e;
        }       
        

        NodeList nodes = el.getChildNodes();

        for (int i = 0; i < nodes.getLength(); i++) {
            Element subEl = (Element) nodes.item(i);
            if (tag.equals(OMG_inheritance_spec)
                || tag.equals(OMG_value_inheritance_spec))
                // para interfaces y valuetypes, el scope nuevo no
                // empieza hasta despues del '{'
                checkScopes(subEl, newScope.getParent());
            else
                checkScopes(subEl, newScope);
        }

    }

    private void removePragmas(Element el)
    //throws SemanticException - nunca lo lanza
    {

        NodeList pragmas = el.getElementsByTagName(OMG_pragma);

        while (pragmas.getLength() > 0) {

            Element pragma = (Element) pragmas.item(0);

            // Remove pragma from xml document
            Node superNode = pragma.getParentNode();
            superNode.removeChild(pragma);
        }
    }

    private String getVariableSizeType(Element el)
    {
        String vl_type = el.getAttribute(OMG_variable_size_type);
        if (vl_type != null && !vl_type.equals("")) {//suponemos que ya ha sido
                                                     // definido y el valor es
                                                     // su valor,
            return vl_type;
        }
        String tag = el.getTagName();
        if (tag.equals(OMG_module)) {
            return "false";
        } else if (tag.equals(OMG_interface)) {
            return "true";
        } else if (tag.equals(OMG_const_dcl)) {
            return "false";
        } else if (tag.equals(OMG_enum)) {
            return "false";
        } else if (tag.equals(OMG_struct) || tag.equals(OMG_exception)) {
            // Varaible size elements should belong to Structs od
            NodeList list;
            list = el.getChildNodes();
            for (int i = 0; i < list.getLength(); i++) {
                if (getVariableSizeType(((Element) list.item(i)))
                    .equals("true"))
                    return "true";
            }
            //if (tag.equals(OMG_struct)) // DAVV - incluido para generar
            // operadores de insercion
            //	return "true"; // en Any adecuados

        } else if (tag.equals(OMG_union)) { // Se considera siempre de tama�o
                                            // variable
            // (es Any) - DAVV 30/12/02
        	      	
	   	    NodeList list;
	        NodeList casesList;
	        NodeList typesList;
	        Element element, value, type, simple;
	        	
	      	list = el.getElementsByTagName("case");
	        for (int i = 0; i < list.getLength(); i++)
	        {
	           element = (Element)list.item(i);
	           casesList = element.getElementsByTagName("value");
	           value = (Element)casesList.item(0);
	           typesList = value.getElementsByTagName("type");
	           type = (Element)typesList.item(0);
	           
	           if (type!=null) {
	           	  //Si es un tipo simple (etiqueta type), devuelvo su valor
	           	  if (getVariableSizeType(type).equals("true"))
	                 return "true";
	           }
	           else {
	           	  // Si no es un tipo simple (etiqueta simple), veo su valor
	           	  simple = (Element)value.getElementsByTagName("simple").item(0);
	           	  if (simple!=null&&getVariableSizeType(simple).equals("true"))
	           	  	return "true";
	           }
	        }             	
	        return "false";
        	
        } else if (tag.equals(OMG_typedef)) {
            // El primer elemento es la definicion del tipo y segundo el nombre
            // del hijo.
            vl_type = getVariableSizeType((Element) el.getFirstChild());
            ((Element) el.getLastChild()).setAttribute(OMG_variable_size_type,
                                                       vl_type);
            ((Element) el.getFirstChild()).setAttribute(OMG_variable_size_type,
                                                        vl_type); // Para que
                                                                  // sea
                                                                  // accesible
            // en cualquiera de los dos hijos
            // DAVV - 30/12/02
            return vl_type;
        } else if (tag.equals(OMG_sequence)) {
            //((Element)el.getLastChild()).setAttribute(OMG_variable_size_type,"true");
            if (!((Element) el.getLastChild())
                .getTagName().equals(OMG_scoped_name))
                ((Element) el.getLastChild())
                    .setAttribute(OMG_variable_size_type,
                                  getVariableSizeType(((Element) el
                                      .getLastChild())));
            return "true";
        } else if (tag.equals(OMG_valuetype)) {
            return "true";
        } else if (tag.equals(OMG_type)) {
            String type = el.getAttribute(OMG_kind);
            if (type.equals(OMG_string) || type.equals(OMG_wstring)
                || type.equals(OMG_any))
                return "true";

            return "false";
        } else if (tag.equals(OMG_scoped_name)) {
            Element tenode = getDefinitionNode(el);
            if (tenode == null)
                return "false";
            return getVariableSizeType(tenode);
        } else if (tag.equals(OMG_simple_declarator) || tag.equals(OMG_array)) { // - le
                                                                                 // corresponde
                                                                                 // el
                                                                                 // valor
                                                                                 // de
                                                                                 // su
                                                                                 // 'hermano'
                                                                                 // a la
                                                                                 // izquierda
            Element definition = (Element) el.getPreviousSibling();
            return getVariableSizeType(definition);
        }

        return "false";
    }

    private static Element getDefinitionNode(Element el)
    {
        String name = el.getAttribute(OMG_name);           
        
        if(!name.startsWith("::")) { // search in parent context
                      
            Element parent = (Element) el.getParentNode().getParentNode();
            
            Element theNode = findFirstOccurrence(name, parent);
            
            if(theNode != null) {
                return theNode;
            }            
        }
        
        // Special case for valuetypes as member of structs
        if(!name.startsWith("::")) { // search in parent context
        	
            try{
            	Element parent = (Element) el.getParentNode().getParentNode().getParentNode();
            
            	Element theNode = findFirstOccurrence(name, parent);
            
            	if(theNode != null) {
            		//System.out.println("getDefinitionNode: encontrado en 4 " + 
                	//		theNode.getAttribute(OMG_name) + "\n");
            		return theNode;
            	}
            }catch(Exception e){
            }
            
        }
        
        // Special case for unions
        if(!name.startsWith("::")) { // search in parent context
        	
            try{
            	Element parent = (Element) el.getParentNode().getParentNode().getParentNode().getParentNode();
            
            	Element theNode = findFirstOccurrence(name, parent);
            
            	if(theNode != null) {
            		return theNode;
            	}
            }catch(Exception e){
            }
            
        }
        
        // search y document contest
          return findFirstOccurrence(name, el.getOwnerDocument().getDocumentElement());     
       
        
    }

    private static Element findFirstOccurrence(String name, Element doc)
    {    
            
        if (name.startsWith("::"))
            name = name.substring(2);
        
        String currentName = null;
        
        int pos = name.indexOf("::");
        
        if( pos== -1) { // the name is in this scope  
            currentName = name;
        } else {
            currentName = name.substring(0,pos);
        }
               

        NodeList nl = doc.getChildNodes(); // Coincide el nombre actual
                                           // con el comienzo del scoped_name
                                          // buscado
        
        if (nl == null)
            return null;
        

        Element temp;
        Element aux;
        
        for (int i = 0; i < nl.getLength(); i++) { // seguimos la
                                                   // b�squeda en cada uno de
                                                   // los hijos
            aux = (Element) nl.item(i);
            
            if(aux.getAttribute(OMG_name).equals(currentName)) {
                if(pos == -1) {
                    return(Element) aux;
                } else {
                    return findFirstOccurrence(name.substring(pos), (Element)aux);
                }
            } else if(aux.getNodeName().equals(OMG_typedef)) {
                
                temp = findFirstOccurrence(name, (Element)aux);
                
                if(temp != null) {
                    return aux;
                }
            }
        }
        return null;

    }
      

    public static String checkReservedNames(String originalName, boolean isInterface)
    {
        // modifica los identificadores que 'chocan' con los nombres
        // reservados por el mapping de Java
        //      - (exportando algunos casos a C++);
        //      - para mas info vease la documentacion oficial de Java mapping,
        // apartado 1.2.1 'Reserved Names'

        String scope = "";
        String name = originalName;

        if (originalName.lastIndexOf("::") >= 0) {
            scope = originalName.substring(0, name.lastIndexOf("::"));
            name = originalName.substring(name.lastIndexOf("::") + 2);
        }

        if (CompilerConf.getCompilerType().equals("Java")) {
            String rest = name;
            boolean repeat;
            do {
                repeat = false;
                if (rest.endsWith("Helper")) {
                    name = "_" + name;
                    rest = rest.substring(0, rest.length() - 6);
                    repeat = true;
                } else if (rest.endsWith("Holder")) {
                    name = "_" + name;
                    rest = rest.substring(0, rest.length() - 6);
                    repeat = true;
                } else if (isInterface) {
                    if (rest.endsWith("Operations")) {
                        name = "_" + name;
                        rest = rest.substring(0, rest.length() - 10);
                        repeat = true;
                    } else if (rest.endsWith("POA") && !name.equals("POA")) {
                        name = "_" + name;
                        rest = rest.substring(0, rest.length() - 3);
                        repeat = true;
                    } else if (rest.endsWith("POATie")) {
                        name = "_" + name;
                        rest = rest.substring(0, rest.length() - 6);
                        repeat = true;
                    }
                } else if (rest.endsWith("Holder")) {
                    String[] java_basic_types = { "boolean", "byte", "char",
                                                 "float", "int", "long",
                                                 "short" };
                    HashSet javawords = new HashSet(Arrays
                        .asList(java_basic_types));
                    if (javawords
                        .contains(rest.substring(0, rest.length() - 6)))
                        name = "_" + name;
                } else if (rest.endsWith("Package")) {
                    name = "_" + name;
                    rest = rest.substring(0, rest.length() - 7);
                    repeat = true;
                }
            } while (repeat);

        } else { // en el mapping de C++ no existen realmente las clases
                 // Helper y Holder,
            //       y por tanto tampoco las restricciones de nombre que acaben por
            // 'Helper' y 'Holder';
            //      sin embargo, nosotros las utilizamos tambi�n en C++, y por ello
            // nos llevamos tambi�n
            //      la reserva de identificadores a este lenguaje, as� como la forma
            // de resolverla
            String rest = name;
            while (rest.endsWith("Helper") || rest.endsWith("Holder")) {
                name = "_" + name;
                rest = rest.substring(0, rest.length() - 6);
            }
        }

        return (!scope.equals("") ? scope + "::" : scope) + name;
    }

}
