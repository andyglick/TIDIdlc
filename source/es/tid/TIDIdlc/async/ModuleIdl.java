/*
* MORFEO Project
* http://www.morfeo-project.org
*
* Component: TIDIdlc
* Programming Language: Java
*
* File: $Source$
* Version: $Revision: 253 $
* Date: $Date: 2008-05-16 11:02:54 +0200 (Fri, 16 May 2008) $
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

package es.tid.TIDIdlc.async;

import java.util.ArrayList;

/**
 * @author iredondo
 */
public class ModuleIdl {
	
	/**
	 * The name of the module.
	 */
	private String m_name;

	/**
	 * The list of definitions.
	 */
	private ArrayList m_definitions_list;

	/**
	 * The list of exceptions.
	 */
//	private ArrayList m_exceptions_list;

	/**
	 * The actual exception.
	 */
//	private ExceptionIdl m_exception_act;

	/**
	 * The list of interfaces.
	 */
	private ArrayList m_interfaces_list;
	
	/**
	 * The actual valuetype.
	 */
	private InterfaceIdl m_interface_act;

	/**
	 * The list of valuetypes.
	 */
	private ArrayList m_valuetypes_list;
	
	/**
	 * The actual valuetype.
	 */
	private ValuetypeIdl m_valuetype_act;

	/**
	 * Constructor
	 */
	public ModuleIdl (String name){
		this.m_name = name;
		this.m_definitions_list = new ArrayList();
//		this.m_exceptions_list = new ArrayList();
//		this.m_exception_act = null;
		this.m_interfaces_list = new ArrayList();
		this.m_interface_act = null;
		this.m_valuetypes_list = new ArrayList(); 
		this.m_valuetype_act = null;
	}

	public void addDefinition (String definition) {
		m_definitions_list.add(definition);
	}

/*	public void addException (String name) {
		m_exception_act = new ExceptionIdl(name);
		m_exceptions_list.add(m_exception_act);
	}

	public void addMemberException (String member) {
		m_exception_act.addMember(member);
	}*/

	public InterfaceIdl getInterfaceAct() {
		return m_interface_act;
	}

	public void addInterface (String name, boolean abstractx, boolean local) {
		m_interface_act = new InterfaceIdl(name, false, abstractx, local);
		m_interfaces_list.add(m_interface_act);
	}

	public void addInterfaceForward (String name, boolean abstractx, boolean local) {
		InterfaceIdl ifaceFw = new InterfaceIdl(name, true, abstractx, local);
		m_interfaces_list.add(ifaceFw);
	}

	public void addOperationInterface (String name, String returnType) {
		if (m_interface_act != null)
			m_interface_act.addOperation(name, returnType);
	}
	
	public void addExceptionDclValuetype (String name) {
		if (m_valuetype_act != null)
			m_valuetype_act.addExceptionDcl(name);
	}

	public void addParameterOper (String name, String type, String modif) {
		if (m_interface_act != null)
			m_interface_act.addParameterOper(name, type, modif);
	}

	public void addExceptionOperInterface (String name) {
		if (m_interface_act != null)
			m_interface_act.addExceptionOper(name);
	}

	public void addInheritanceInterface (String inheritance) {
		if (m_interface_act != null)
			m_interface_act.addInheritance (inheritance);
	}

	public ValuetypeIdl getValuetypeAct() {
		return m_valuetype_act;
	}

	public void addValuetype (String name, String ifaceName, boolean abstractx, boolean local) {
		m_valuetype_act = new ValuetypeIdl(name, ifaceName, false, abstractx, local);
		m_valuetypes_list.add(m_valuetype_act);
	}

	public void addValuetypeForward (String name, String ifaceName, boolean abstractx, boolean local) {
		ValuetypeIdl ivalueFw = new ValuetypeIdl(name, ifaceName, true, abstractx, local);
		m_valuetypes_list.add(ivalueFw);
	}

	public void addOperationValuetype (String name, String returnType) {
		if (m_valuetype_act != null)
			m_valuetype_act.addOperation(name, returnType);
	}

	public void addExceptionOperValuetype (String name) {
		if (m_valuetype_act != null)
			m_valuetype_act.addExceptionOper(name);
	}

	public void addInheritanceValuetype (String inheritance) {
		if (m_valuetype_act != null)
			m_valuetype_act.addInheritance (inheritance);
	}

	public String getString() {
		if (m_definitions_list.size()==0 && m_interfaces_list.size() == 0 && m_valuetypes_list.size() == 0) 
			return null;
		
		String contentModule = "module " + m_name + "{\n";
		for (int i=0; i<m_definitions_list.size(); i++) {
			contentModule = contentModule + "\tvaluetype " + m_definitions_list.get(i) + ";\n\n";
		}
		/*for (int i=0; i<m_exceptions_list.size(); i++) {
			contentModule = contentModule + ((ExceptionIdl)m_exceptions_list.get(i)).getString() + "\n";
		}*/
		for (int i=0; i<m_interfaces_list.size(); i++) {
			contentModule = contentModule + ((InterfaceIdl)m_interfaces_list.get(i)).getString() + "\n";
		}
		for (int i=0; i<m_valuetypes_list.size(); i++) {
			contentModule = contentModule + ((ValuetypeIdl)m_valuetypes_list.get(i)).getString() + "\n";
		}
		contentModule = contentModule + "};";
		return contentModule;
	}
}
