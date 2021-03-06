/*
* MORFEO Project
* http://www.morfeo-project.org
*
* Component: TIDIdlc
* Programming Language: Java
*
* File: $Source$
* Version: $Revision: 2 $
* Date: $Date: 2005-04-15 14:20:45 +0200 (Fri, 15 Apr 2005) $
* Last modified by: $Author: rafa $
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

package es.tid.TIDIdlc.xmlsemantics;

import org.w3c.dom.Element;
import es.tid.TIDIdlc.xml2cpp.XmlType2Cpp;

public class XmlCppSemanticType extends XmlType
{

    public XmlCppSemanticType()
    {}

    /*
     * public String getTypedefType(Element doc) { return
     * XmlType2Cpp.getTypedefType(doc); }
     */
    /*
     * en los contextos en que es usado, no aporta ninguna diferencia con
     * respecto a getTypedefType public String getAbsoluteTypedefType(Element
     * doc) { return XmlType2Cpp.getAbsoluteTypedefType(doc) ; }
     */

    public String getType(Element doc)
    {
        return XmlType2Cpp.getType(doc);
    }

    /*
     * public String getTypeWithoutPackage(Element doc) { return
     * XmlType2Cpp.getTypeWithoutPackage(doc); }
     */
    public String getParamType(Element doc, String kind)
    {
        return XmlType2Cpp.getParamType(doc, kind);
    }

    public String getHelperType(Element doc)
    {
        return XmlType2Cpp.getHelperType(doc);
    }

    public String getTypecode(Element doc)
        throws Exception
    {
        return XmlType2Cpp.getTypecode(doc);
    }

    public String getTypeReader(Element doc, String inputStreamName)
        throws Exception
    {
        return XmlType2Cpp.getTypeReader(doc, inputStreamName);
    }

    public String getTypeWriter(Element doc, String outputStreamName,
                                String outputData)
        throws Exception
    {
        return XmlType2Cpp.getTypeWriter(doc, outputStreamName, outputData);
    }

    public String basicMapping(String type)
    {
        return XmlType2Cpp.basicMapping(type);
    }

    public String basicOutMapping(String type)
    {
        return XmlType2Cpp.basicOutMapping(type);
    }

    public String basicORBTypeMapping(Element el)
    {
        return XmlType2Cpp.basicORBTypeMapping(el);
    }

    public String basicORBTcKindMapping(Element el)
    {
        return XmlType2Cpp.basicORBTcKindMapping(el);
    }

    /*
     * public String getUnrolledName(String scopedName) { return
     * XmlType2Cpp.getUnrolledName(scopedName) ; } public String
     * getUnrolledName(Element doc) { return XmlType2Cpp.getUnrolledName(doc) ; }
     */
    /*
     * no se usa nunca y no aporta nada con respecto a getUnrolledName!!
     * public String getAbsoluteUnrolledName(Element doc) { return
     * XmlType2Cpp.getAbsoluteUnrolledName(doc) ; }
     */
    /*
     * public String getUnrolledNameWithoutPackage(String
     * scopedName) { return
     * XmlType2Cpp.getUnrolledNameWithoutPackage(scopedName) ; } public String
     * getUnrolledNameWithoutPackage(Element doc) { return
     * XmlType2Cpp.getUnrolledNameWithoutPackage(doc) ; }
     */
    public String getDefaultConstructor(String type)
    {
        return "";//XmlType2Cpp.getDefaultConstructor(type);
    }

    public String getDefinitionType(Element doc)
    {
        return XmlType2Cpp.getDefinitionType(doc);
    }

    public String getDeepKind(Element doc)
    {
        return XmlType2Cpp.getDeepKind(doc);
    }
}