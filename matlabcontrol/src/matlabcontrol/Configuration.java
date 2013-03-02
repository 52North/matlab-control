package matlabcontrol;

/*
 * Copyright (c) 2013, Joshua Kaplan
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *  - Redistributions of source code must retain the above copyright notice, this list of conditions and the following
 *    disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 *    following disclaimer in the documentation and/or other materials provided with the distribution.
 *  - Neither the name of matlabcontrol nor the names of its contributors may be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;

/**
 * Contains important configuration information regarding the setup of MATLAB and matlabcontrol.
 * 
 * @since 3.1.0
 * 
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
class Configuration
{   
    private Configuration() { }
    
    /**
     * If running on OS X.
     * 
     * @return 
     */
    static boolean isOSX()
    {
        return getOperatingSystem().startsWith("Mac OS X");
    }
    
    /**
     * If running on Windows.
     * 
     * @return 
     */
    static boolean isWindows()
    {
        return getOperatingSystem().startsWith("Windows");
    }
    
    /**
     * If running on Linux.
     * 
     * @return
     */
    static boolean isLinux()
    {
        return getOperatingSystem().startsWith("Linux");
    }
    
    /**
     * Gets a string naming the operating system.
     * 
     * @return 
     */
    static String getOperatingSystem()
    {
        return System.getProperty("os.name");
    }

    /**
     * Returns the location or alias of MATLAB on an operating system specific basis.
     * <br><br>
     * For OS X this will be the location, for Windows or Linux this will be an alias. For any other operating system an 
     * exception will be thrown.
     * 
     * @return
     * @throws MatlabConnectionException thrown if the location of MATLAB cannot be determined on OS X, or the alias
     *                                       cannot be determined because the operating system is not Windows or Linux
     */
    static String getMatlabLocation() throws MatlabConnectionException
    {
        //Determine the location of MATLAB
        String matlabLoc;

        //OS X
        if(isOSX())
        {
            matlabLoc = getOSXMatlabLocation();
        }
        //Windows or Linux
        else if(isWindows() || isLinux())
        {
            matlabLoc = "matlab";
        }
        //Other unsupported operating system
        else
        {
            throw new MatlabConnectionException("MATLAB's location or alias can only be determined for OS X, " +
                    "Windows, & Linux. For this operating system the location or alias must be specified " +
                    "explicitly.");
        }
        
        return matlabLoc;
    }
    
    /**
     * Determines the location of the MATLAB executable on OS X. If multiple versions are found, the last one
     * encountered will be used.
     * 
     * @return MATLAB's location on OS X
     * 
     * @throws MatlabConnectionException if the location cannot be determined
     */
    private static String getOSXMatlabLocation() throws MatlabConnectionException
    {
        //Search for MATLAB in the Applications directory
        String matlabName = null;
        for(String fileName : new File("/Applications/").list())
        {
            if(fileName.startsWith("MATLAB"))
            {
                matlabName = fileName;
            }
        }
        
        //If no installation is found
        if(matlabName == null)
        {
            throw new MatlabConnectionException("No installation of MATLAB on OS X can be found");
        }
        
        //Build path to the executable location
        String matlabLocation = "/Applications/" + matlabName + "/bin/matlab";
        
        //Check the path actually exists
        if(!new File(matlabLocation).exists())
        {
            throw new MatlabConnectionException("An installation of MATLAB on OS X was found but the main " +
                    "executable file was not found in the anticipated location: " + matlabLocation);
        }
        
        return matlabLocation;
    }
    
    /**
     * Converts the classpath into RMI's codebase format. The codebase format is a list of URL formatted strings
     * separated by spaces. As the application may be running in a different directory, paths are made absolute.
     * 
     * @return codebase
     * @throws MatlabConnectionException
     */
    static String getClassPathAsRMICodebase() throws MatlabConnectionException
    {
        try
        {
            StringBuilder codebaseBuilder = new StringBuilder();
            String[] paths = System.getProperty("java.class.path", "").split(File.pathSeparator);
            
            for(int i = 0; i < paths.length; i++)
            {
                codebaseBuilder.append("file://");
                codebaseBuilder.append(new File(paths[i]).getCanonicalFile().toURI().toURL().getPath());

                if(i != paths.length - 1)
                {
                    codebaseBuilder.append(" ");
                }
            }
        
            return codebaseBuilder.toString();
        }
        catch(IOException e)
        {
            throw new MatlabConnectionException("Unable to resolve classpath entry", e);
        }
    }
    
    /**
     * Converts the classpath into individual canonical entries.
     * 
     * @return
     * @throws MatlabConnectionException 
     */
    static String[] getClassPathAsCanonicalPaths() throws MatlabConnectionException
    {
        try
        {
            String[] paths = System.getProperty("java.class.path", "").split(File.pathSeparator);
            
            for(int i = 0; i < paths.length; i++)
            {
                paths[i] = new File(paths[i]).getCanonicalPath();
            }
        
            return paths;
        }
        catch(IOException e)
        {
            throw new MatlabConnectionException("Unable to resolve classpath entry", e);
        }
    }
    
    /**
     * Determines the location of this code at runtime. Either it will be the directory or jar this .class file is in.
     * (That is, the .class file built from compiling this .java file.) Returned as a string so that it may be used by
     * MATLAB.
     * <br><br>
     * It may not be possible to determine the location of the code due to the class loader for this class. The class
     * loader will determine if a code source was set and the format of its location. Due to the problematic nature
     * of determining code location this method has been designed to throw exceptions with very detailed exceptions to
     * aid in improving this method further based on anticipated bug reports.
     * 
     * @return
     * @throws MatlabConnectionException
     */
    static String getSupportCodeLocation() throws MatlabConnectionException
    {
        //domain should never be null
        ProtectionDomain domain = Configuration.class.getProtectionDomain();
        
        //codeSource can be null
        CodeSource codeSource = domain.getCodeSource();
        if(codeSource != null)
        {
            //url can be null
            URL url = codeSource.getLocation();
            if(url != null)
            {
                //Convert from url to absolute path
                try
                {
                    //uri cannot be null, but this method can throw a URISyntaxException
                    URI uri = url.toURI();
                    
                    //path could be null
                    String path = uri.getPath();
                    if(path != null)
                    {
                        try
                        {
                            File file = new File(path).getCanonicalFile();
                            if(file.exists())
                            {
                                return file.getAbsolutePath();
                            }
                            else
                            {
                                ClassLoader loader = Configuration.class.getClassLoader();
                                throw new MatlabConnectionException("Support code location was determined improperly." +
                                        " Location does not exist.\n" +
                                        "Location determined as: " + file.getAbsolutePath() + "\n" +
                                        "Path: " + path + "\n" + 
                                        "URI Location: " + uri + "\n" +
                                        "URL Location: " + url + "\n" +
                                        "Code Source: " + codeSource + "\n" +
                                        "Protection Domain: " + domain + "\n" +
                                        "Class Loader: " + loader +
                                        (loader == null ? "" : "\nClass Loader Class: " +loader.getClass()));
                            }
                        }
                        //Unable to resolve canconical path
                        catch(IOException e)
                        {
                            ClassLoader loader = Configuration.class.getClassLoader();
                            throw new MatlabConnectionException("Support code location could not be determined. " +
                                    "Could not resolve canonical path.\n" +
                                    "Path: " + path + "\n" +
                                    "URI Location: " + uri + "\n" +
                                    "URL Location: " + url + "\n" +
                                    "Code Source: " + codeSource + "\n" +
                                    "Protection Domain: " + domain + "\n" +
                                    "Class Loader: " + loader +
                                    (loader == null ? "" : "\nClass Loader Class: " +loader.getClass()), e);
                        }
                    }
                    //path was null
                    else
                    {
                        ClassLoader loader = Configuration.class.getClassLoader();
                        throw new MatlabConnectionException("Support code location could not be determined. " +
                                "Could not get path from URI location.\n" +
                                "URI Location: " + uri + "\n" +
                                "URL Location: " + url + "\n" +
                                "Code Source: " + codeSource + "\n" +
                                "Protection Domain: " + domain + "\n" +
                                "Class Loader: " + loader +
                                (loader == null ? "" : "\nClass Loader Class: " +loader.getClass()));
                    }
                }
                //Unable to convert URL to URI
                catch(URISyntaxException e)
                {
                    ClassLoader loader = Configuration.class.getClassLoader();
                    throw new MatlabConnectionException("Support code location could not be determined. " +
                            "Could not convert from URL to URI location.\n" +
                            "URL Location: " + url + "\n" +
                            "Code Source: " + codeSource + "\n" +
                            "Protection Domain: " + domain + "\n" +
                            "Class Loader: " + loader +
                            (loader == null ? "" : "\nClass Loader Class: " +loader.getClass()), e);
                }
            }
            //url was null
            else
            {
                ClassLoader loader = Configuration.class.getClassLoader();
                throw new MatlabConnectionException("Support code location could not be determined. " +
                        "Could not get URL from CodeSource.\n" +
                        "Code Source: " + codeSource + "\n" +
                        "Protection Domain: " + domain + "\n" +
                        "Class Loader: " + loader +
                        (loader == null ? "" : "\nClass Loader Class: " +loader.getClass()));
            }
        }
        //code source was null
        else
        {  
            ClassLoader loader = Configuration.class.getClassLoader();
            throw new MatlabConnectionException("Support code location could not be determined. " +
                    "Could not get CodeSource from ProtectionDomain.\n" +
                    "Protection Domain: " + domain + "\n" +
                    "Class Loader: " + loader +
                    (loader == null ? "" : "\nClass Loader Class: " +loader.getClass()));
        }
    }
    
    /**
     * Whether this code is running inside of MATLAB.
     * 
     * @return 
     */
    static boolean isRunningInsideMatlab()
    {
        boolean available;
        try
        {
            //Load the class com.mathworks.jmi.Matlab and then calls its static method isMatlabAvailable()
            //All of this is done with reflection so that this class does not cause the class loader to attempt
            //to load JMI classes (and if not running inside of MATLAB - fail)
            Class<?> matlabClass = Class.forName("com.mathworks.jmi.Matlab");
            Method isAvailableMethod = matlabClass.getMethod("isMatlabAvailable");
            available = (Boolean) isAvailableMethod.invoke(null);
        }
        catch(Throwable t)
        {
            available = false;
        }
        
        return available;
    }
}