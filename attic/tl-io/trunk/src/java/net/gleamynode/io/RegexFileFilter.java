/*
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
/*
 * @(#) $Id$
 */
package net.gleamynode.io;

import java.io.File;
import java.io.FileFilter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * A {@link FileFilter} which matches the specified regular expression with the file name.
 *
 * @author Trustin Lee
 * @version $Revision$, $Date$
 */
public class RegexFileFilter implements FileFilter {
    private Pattern pattern;

    /**
     * Constructs a new instance with the specified regular expression.
     */
    public RegexFileFilter(Pattern pattern) {
        if (pattern == null) {
            throw new NullPointerException("pattern is null");
        }

        this.pattern = pattern;
    }

    /**
     * Returns <code>true</code> if the name of the specified file matches the regular expression
     * which was specified in the constructor.
     */
    public boolean accept(File pathname) {
        Matcher matcher = pattern.matcher(pathname.getName());
        return matcher.matches();
    }
}
