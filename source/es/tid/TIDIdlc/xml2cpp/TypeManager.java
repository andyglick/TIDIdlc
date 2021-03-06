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

import es.tid.TIDIdlc.idl2xml.Idl2XmlNames;
import es.tid.TIDIdlc.idl2xml.Preprocessor;
import es.tid.TIDIdlc.xmlsemantics.*;
import es.tid.TIDIdlc.CompilerConf;

import java.util.*;
import java.io.File;
import java.io.IOException;

/**
 * Holds the mapping of IDL defined types. In the context of this class, "Cpp
 * mapping for IDL type" stands for the translation of IDL scope where the type
 * is defined to Cpp module, so the type itself is not converted. For example:
 * ::module1::itf1::mytype -> module1::itf1Package::mytype
 * <p>
*/
public class TypeManager
    implements TypeHandler, Idl2XmlNames
{

    /**
     * Converts IDL types to Cpp types (if the mapping was previousy saved)
     * 
     * @param idlType
     *            The IDL type to be converted. Complete scoped name is
     *            required.
     * @return The previously saved mapping for
     * @param idlType.
     */
    public static String convert(String idlType)
    {
    	String var = TypedefManager.getInstance().getTypedefKind(idlType);
    	if (var!=null&&var.equals(OMG_string)){
    		return "char*";
    		//return convertName(idlType);
    	}
		else {
			//if ()
			String res = (String) st_types.get(idlType);
			if (res != null) {
	            return res;
	        }
	        else {
	        	return (idlType.startsWith("::") ? idlType.substring(2) : idlType);
	        }
		}  
    }
    
    /**
     * Converts IDL types to Cpp types (if the mapping was previousy saved)
     * strings are mapped to the typedef type not to char*
     * @param idlType
     *            The IDL type to be converted. Complete scoped name is
     *            required.
     * @return The previously saved mapping for
     * @param idlType.
     */
    public static String convertName(String idlType)
    {
        String res = (String) st_types.get(idlType);
        if (res != null) {
            return res;
        }
        else {
            return (idlType.startsWith("::") ? idlType.substring(2) : idlType);
        }
    }

    /**
     * Save the mapping to Cpp of an IDL type
     * 
     * @param idlType
     *            The IDL type to be converted.
     * @param scope
     *            The scope where the IDL type is defined.
     * @param line
     *            The line in the IDL file where the type is defined
     */
    public void saveMapping(Scope scope, String idlType, int line)
        throws SemanticException,
        IOException
    {

        boolean package_to_found = false;

        // nombre completo del scope (versi�n IDL)
        String scopedName = scope.getCompleteName();
        // nombre completo del tipo (versi�n IDL)
        String completeScopeName = scopedName + Scope.SEP + idlType;

        // en C++ el nombre completo coincide con el nombre en IDL
        // s�lo quitamos el '::' inicial
        String converted = completeScopeName.substring(2);

        // comprobaci�n y ajuste de par�metro -package_to
        Enumeration elem = CompilerConf.getModule_Packaged().elements();
        while (elem.hasMoreElements()) {
            String module = (String) elem.nextElement();
            if (converted.startsWith(module)) {
                String modulePackagedTo = 
                    (String) CompilerConf.getPackageToTable().get(module);
                converted = modulePackagedTo + "::" + converted;
                package_to_found = true;
            }
        }

        // comprobaci�n y ajuste de par�metro -package_to (ahora para
        // ficheros)
        elem = CompilerConf.getFilePackaged().elements();
        while (elem.hasMoreElements() && !package_to_found) {
            String file = (String) elem.nextElement();
            String actual = Preprocessor.getInstance().locateFile(line);
            Vector searchPath = CompilerConf.getSearchPath();
            searchPath.add(".");
            for (int k = 0; k < searchPath.size(); k++) {
                String completeFile = 
                    (new File(searchPath.elementAt(k)+ File.separator + file)).getCanonicalPath();
                if (actual.equals(completeFile)) {
                    String modulePackagedTo = 
                        (String) CompilerConf.getPackageToTable().get(file);
                    converted = modulePackagedTo + "::" + converted;
                    package_to_found = true;
                    break;
                }
            }
        }

        // comprobaci�n de par�metro -package
        if (!CompilerConf.getPackageUsed().equals("") && !package_to_found) {
            converted = CompilerConf.getPackageUsed() + "::" + converted;
        }

        // pareja: (IDL_scoped_name, C++_scoped_name)
        st_types.put(completeScopeName, converted);
    }

    public static void dump()
    {
        Enumeration keys = st_types.keys();
        Enumeration elements = st_types.elements();
        for (; keys.hasMoreElements();) {
            System.out.print(keys.nextElement());
            String info = (String) elements.nextElement();
            System.out.print(" -> ");
            System.out.println(info);
        }
    }

    private String removePrefix(String s)
        throws SemanticException
    {
        if (s == null)
            return s;
        StringTokenizer tokenizer = new StringTokenizer(s);
        return s;
    }

    private static Hashtable st_types;

    static {
        st_types = new Hashtable();
        /*
         * // This data is not used. It's kept here for further use.
         * _types.put(OMG_char, "char"); 
         * _types.put(OMG_octet, "byte");
         * _types.put(OMG_string, "string"); 
         * _types.put(OMG_wstring, "wstring");
         * _types.put(OMG_unsignedshort, "short"); 
         * _types.put(OMG_long, "int");
         * _types.put(OMG_unsignedlong, "int"); 
         * _types.put(OMG_longlong, "long"); 
         * _types.put(OMG_unsignedlonglong, "long");
         * _types.put(OMG_fixed, "java.math.BigDecimal"); 
         * _types.put(OMG_any, "org.omg.CORBA.Any"); 
         * _types.put(OMG_boolean, "boolean");
         */
    }

}