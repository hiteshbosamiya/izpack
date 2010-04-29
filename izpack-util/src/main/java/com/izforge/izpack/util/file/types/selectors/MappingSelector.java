/*
 * Copyright  2000-2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.izforge.izpack.util.file.types.selectors;

import com.izforge.izpack.api.data.AutomatedInstallData;
import com.izforge.izpack.util.file.FileNameMapper;
import com.izforge.izpack.util.file.FileUtils;
import com.izforge.izpack.util.file.IdentityMapper;
import com.izforge.izpack.util.file.types.Mapper;
import org.apache.tools.ant.BuildException;

import java.io.File;

/**
 * A mapping selector is an abstract class adding mapping support to the base
 * selector
 */
public abstract class MappingSelector extends BaseSelector
{
    protected File targetdir = null;
    protected Mapper mapperElement = null;
    protected FileNameMapper map = null;
    protected int granularity = 0;

    /**
     * Creates a new <code>MappingSelector</code> instance.
     */
    public MappingSelector()
    {
        granularity = (int) FileUtils.newFileUtils().getFileTimestampGranularity();
    }


    /**
     * The name of the file or directory which is checked for out-of-date
     * files.
     *
     * @param targetdir the directory to scan looking for files.
     */
    public void setTargetdir(File targetdir)
    {
        this.targetdir = targetdir;
    }

    /**
     * Defines the FileNameMapper to use (nested mapper element).
     *
     * @return a mapper to be configured
     * @throws BuildException if more that one mapper defined
     */
    public Mapper createMapper() throws Exception
    {
        if (mapperElement != null)
        {
            throw new Exception("Cannot define more than one mapper");
        }
        mapperElement = new Mapper();
        return mapperElement;
    }

    /**
     * Checks to make sure all settings are kosher. In this case, it
     * means that the dest attribute has been set and we have a mapper.
     */
    public void verifySettings() throws Exception
    {
        if (targetdir == null)
        {
            setError("The targetdir attribute is required.");
        }
        if (mapperElement == null)
        {
            map = new IdentityMapper();
        }
        else
        {
            map = mapperElement.getImplementation();
        }
        if (map == null)
        {
            setError("Could not set <mapper> element.");
        }
    }

    /**
     * The heart of the matter. This is where the selector gets to decide
     * on the inclusion of a file in a particular fileset.
     *
     * @param basedir  the base directory the scan is being done from
     * @param filename is the name of the file to check
     * @param file     is a java.io.File object the selector can use
     * @return whether the file should be selected or not
     */
    public boolean isSelected(AutomatedInstallData idata, File basedir, String filename, File file) throws Exception
    {

        // throw BuildException on error
        validate();

        // Determine file whose out-of-dateness is to be checked
        String[] destfiles = map.mapFileName(filename);
        // If filename does not match the To attribute of the mapper
        // then filter it out of the files we are considering
        if (destfiles == null)
        {
            return false;
        }
        // Sanity check
        if (destfiles.length != 1 || destfiles[0] == null)
        {
            throw new Exception("Invalid destination file results for "
                    + targetdir.getName() + " with filename " + filename);
        }
        String destname = destfiles[0];
        File destfile = new File(targetdir, destname);

        boolean selected = selectionTest(file, destfile);
        return selected;
    }

    /**
     * this test is our selection test that compared the file with the destfile
     *
     * @param srcfile  file to test; may be null
     * @param destfile destination file
     * @return true if source file compares with destination file
     */
    protected abstract boolean selectionTest(File srcfile, File destfile);

    /**
     * Sets the number of milliseconds leeway we will give before we consider
     * a file out of date. Defaults to 2000 on MS-DOS derivatives as the FAT
     * file system.
     *
     * @param granularity the leeway in milliseconds
     */
    public void setGranularity(int granularity)
    {
        this.granularity = granularity;
    }

}
