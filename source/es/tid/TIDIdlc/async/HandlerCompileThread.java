/*
* MORFEO Project
* http://www.morfeo-project.org
*
* Component: TIDorbJ
* Programming Language: Java
*
* File: $Source$
* Version: $Revision: 307 $
* Date: $Date: 2009-06-01 10:48:05 +0200 (Mon, 01 Jun 2009) $
* Last modified by: $Author: avega $
*
* (C) Copyright 2004 Telefónica Investigación y Desarrollo
*     S.A.Unipersonal (Telefónica I+D)
*
* Info about members and contributors of the MORFEO project
* is available at:
*
*   http://www.morfeo-project.org/TIDorbJ/CREDITS
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
*   http://www.morfeo-project.org/TIDorbJ/Licensing
*/    
package es.tid.TIDIdlc.async;

import es.tid.TIDIdlc.CompilerConf;
import es.tid.TIDIdlc.Idl2Java;
import es.tid.TIDIdlc.util.Traces;


/**
 * Execution thread. Gets request from the request queue and executes them.
 * 
 * @autor Irenka Redondo Granados
 * @version 1.0
 */
public class HandlerCompileThread extends Thread
{
	public HandlerCompileThread(String handlerPath)
    {
        CompilerConf.setFile(handlerPath);
        CompilerConf.setAsynchronous(false);
    }

    public void run()
    {
    	Traces.println ("output dir " + CompilerConf.getOutputPath(), Traces.USER );
    	Traces.println ("async " + CompilerConf.getAsynchronous(), Traces.USER );
    	try {
    		//Idl2Java.compile();
    	} catch (Exception e) {
    		if (Traces.getLevel() >= Traces.DEBUG) {
    			e.printStackTrace();
    		} else {
    			System.err.println(e.toString());
    		}
    		System.exit(1);
    	}
    }
}
