/*
 * MORFEO Project
 * http://www.morfeo-project.org*
 * Component: TIDIdlc
 * Programming Language: Java
 *
 * File: $Source$
 * Version: $Revision: 330 $
 * Date: $Date: 2012-02-27 18:02:15 +0100 (Mon, 27 Feb 2012) $
 * Last modified by: $Author: avega $
 *
 * (C) Copyright 2004 Telef?nica Investigaci?n y Desarrollo
 *     S.A.Unipersonal (Telef?nica I+D)
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

package es.tid.TIDIdlc;

import java.util.Vector;
import java.util.Hashtable;

import es.tid.TIDIdlc.async.ExceptionHolderIdl;
import es.tid.TIDIdlc.async.ReplyHandlerIdl;

/**
 * CompilerConf is used to reduce direct class interaction as before it.
 */

public class CompilerConf {
	// From Xml2Java.java
	/**
	 * @attribute lang for choose which language is going to be generated
	 */
	public static String lang = "Java";

	/**
	 * @attribute just_expand for expanding includes in IDLs and finish
	 */
	public static boolean st_just_expand = false;

	/**
	 * @attribute no_stub for server compilation no stub is needed.
	 */
	public static boolean st_no_stub = false;

	/**
	 * @attribute no_skel for client compilation no skeleton is needed.
	 */
	public static boolean st_no_skel = false;

	/**
	 * @attribute no_tie.
	 */
	public static boolean st_no_tie = false;

	/**
	 * @attribute portable for POA generation.
	 */
	public static boolean st_portable = false;

	// From Idl2Java.
	protected static String st_file = null;

	protected static String st_filename = null;

	/**
	 * @attribute expanded for expanded C++ code generation.
	 */
	public static boolean st_expanded = false;

	/**
	 * @attribute header_extension.
	 */
	public static String st_h_ext = ".h";

	/**
	 * @attribute source_extension.
	 */
	public static String st_c_ext = ".C";

	/**
	 * @attribute compiler version.
	 */
	public static String st_compiler_version = "1.3.13";

	/**
	 * @attribute async for client compilation, asynchronous invocations needed.
	 */
	public static boolean st_asynchronous = false;
	
	/**
	 * @attribute non_copying_operators for internal use of non-copying any insert/extract operators from C++ Stub and Skeletons
	 */
	public static boolean st_non_copying_operators = false;

      	/**
	 * @attribute enum_check for enable or disable enum values checking
	 */
	public static boolean st_enum_check = true;
	
	public static void setLang(String val) {
		CompilerConf.lang = val;
	}

	public static String getLang() {
		return CompilerConf.lang;
	}

	public static void setJustExpand(boolean val) {
		CompilerConf.st_just_expand = val;
	}

	public static boolean getJust_Expand() {
		return CompilerConf.st_just_expand;
	}

	public static void setNoStub(boolean val) {
		CompilerConf.st_no_stub = val;
	}

	public static boolean getNoStub() {
		return CompilerConf.st_no_stub;
	}

	public static void setNoSkel(boolean val) {
		CompilerConf.st_no_skel = val;
	}

	public static boolean getNoSkel() {
		return CompilerConf.st_no_skel;
	}

	public static void setNoTie(boolean val) {
		CompilerConf.st_no_tie = val;
	}

	public static boolean getNoTie() {
		return CompilerConf.st_no_tie;
	}

	public static void setPortable(boolean val) {
		CompilerConf.st_portable = val;
	}

	public static boolean getPortable() {
		return CompilerConf.st_portable;
	}

	public static void setFile(String val) {
		CompilerConf.st_file = val;
	}

	public static void setFileName(String val) {
		CompilerConf.st_filename = val;
	}

	public static String getFile() {
		return CompilerConf.st_file;
	}

	public static String getFileName() {
		return CompilerConf.st_filename;
	}

	protected static Vector searchPath = null;

	public static void setSearchPath(Vector val) {
		CompilerConf.searchPath = val;
	}

	public static Vector getSearchPath() {
		if (CompilerConf.searchPath == null) {
			CompilerConf.searchPath = new Vector();
		}
		return new Vector(CompilerConf.searchPath);
	}

	protected static String st_output_path = ".";

	public static void setOutputPath(String val) {
		CompilerConf.st_output_path = val;
	}

	public static String getOutputPath() {
		return CompilerConf.st_output_path;
	}

	protected static String st_output_header_file_dir = "";

	public static void setOutputHeaderDir(String val) {
		CompilerConf.st_output_header_file_dir = val;
	}

	public static String getOutputHeaderDir() {
		return CompilerConf.st_output_header_file_dir;
	}

	public static String st_package_used = "";

	public static void setPackageUsed(String val) {
		CompilerConf.st_package_used = val;
	}

	public static String getPackageUsed() {
		return CompilerConf.st_package_used;
	}

	public static Vector st_module_packaged = new Vector();

	public static void setModulePackaged(Vector val) {
		CompilerConf.st_module_packaged = val;
	}

	public static Vector getModule_Packaged() {
		if (CompilerConf.st_module_packaged == null) {
			CompilerConf.st_module_packaged = new Vector();
		}
		return CompilerConf.st_module_packaged;
	}

	public static String st_package_to_error = "WARNING";

	public static void setPackageToError(String val) {
		CompilerConf.st_package_to_error = val;
	}

	public static String getPackageToError() {
		return CompilerConf.st_package_to_error;
	}

	public static Hashtable st_package_to_table = new Hashtable();

	public static void setPackageToTable(Hashtable val) {
		CompilerConf.st_package_to_table = val;
	}

	public static Hashtable getPackageToTable() {
		if (CompilerConf.st_package_to_table == null) {
			CompilerConf.st_package_to_table = new Hashtable();
		}
		return CompilerConf.st_package_to_table;
	}

	public static Vector st_file_packaged = new Vector();

	public static void setFilePackaged(Vector val) {
		CompilerConf.st_file_packaged = val;
	}

	public static Vector getFilePackaged() {
		if (CompilerConf.st_file_packaged == null) {
			CompilerConf.st_file_packaged = new Vector();
		}
		return CompilerConf.st_file_packaged;
	}

	public static String st_compile_to;

	public static void setCompilerType(String pcompileTo) {
		st_compile_to = pcompileTo;
	}

	public static String getCompilerType() {
		return st_compile_to;
	}

	public static boolean st_corba_idl = false;

	public static boolean st_minimun = false;

	private static boolean st_notGenerateCode;

	public static void setCORBA_IDL(boolean val) {
		st_corba_idl = val;
	}

	public static boolean getCORBA_IDL() {
		return st_corba_idl;
	}

	public static void setMinimun(boolean val) {
		st_minimun = val;
	}

	public static boolean getMinimun() {
		return st_minimun;
	}

	public static void setHeaderExtension(String val) {
		CompilerConf.st_h_ext = "." + val;
	}

	public static String getHeaderExtension() {
		return CompilerConf.st_h_ext;
	}

	public static void setSourceExtension(String val) {
		CompilerConf.st_c_ext = "." + val;
	}

	public static String getSourceExtension() {
		return CompilerConf.st_c_ext;
	}

	public static void setExpanded(boolean val) {
		CompilerConf.st_expanded = val;
	}

	public static boolean getExpanded() {
		return CompilerConf.st_expanded;
	}

	public static void setAsynchronous(boolean val) {
		CompilerConf.st_asynchronous = val;
	}

	public static boolean getAsynchronous() {
		return CompilerConf.st_asynchronous;
	}

	public static void setNonCopyingOperators(boolean val) {
		CompilerConf.st_non_copying_operators = val;
	}
	
	public static boolean getNonCopyingOperators() {
		return CompilerConf.st_non_copying_operators;
	}

	public static void setEnumCheck(boolean val) {
		CompilerConf.st_enum_check = val;
	}
	
	public static boolean getEnumCheck() {
		return CompilerConf.st_enum_check;
	}
	
	/**
	 * @attribute generateCode for code geneting control
	 */
	public static void setNotGenerateCode(boolean b) {
		CompilerConf.st_notGenerateCode = b;
	}
	
	public static boolean getNotGenerateCode() {
		return CompilerConf.st_notGenerateCode;
	}


	/**
	 * Reset Compiler configuration to default values
         * Usefull to be called from Ant tasks
	 */
	public static void reset() {

            lang = "Java";
            st_just_expand = false;
            st_no_stub = false;
            st_no_skel = false;
            st_no_tie = false;
            st_portable = false;
            st_file = null;
            st_filename = null;
            st_expanded = false;
            st_h_ext = ".h";
            st_c_ext = ".C";
            st_asynchronous = false;
            st_non_copying_operators = false;
            st_enum_check = true;
            searchPath = null;
            st_output_path = ".";
            st_output_header_file_dir = "";
            st_package_used = "";
            st_package_to_error = "WARNING";
            st_corba_idl = false;

	}

      

}// end of class CompilerConf
