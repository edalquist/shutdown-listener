/**
 * Copyright (c) 2000-2010, Jasig, Inc.
 * See license distributed with this file and available online at
 * https://www.ja-sig.org/svn/jasig-parent/tags/rel-10/license-header.txt
 */

package com.googlecode.shutdownlistener.spring;

import org.springframework.core.Ordered;

import com.googlecode.shutdownlistener.ShutdownListener;

/**
 * @author Eric Dalquist
 * @version $Revision$
 */
public interface OrderedShutdownListener extends ShutdownListener, Ordered {

}
