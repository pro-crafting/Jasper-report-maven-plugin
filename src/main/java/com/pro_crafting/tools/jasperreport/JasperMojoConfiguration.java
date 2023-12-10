package com.pro_crafting.tools.jasperreport;

import org.codehaus.plexus.compiler.util.scan.StaleSourceScanner;

import java.io.File;
import java.util.List;
import java.util.Map;

public class JasperMojoConfiguration {
    public String compiler;

    public File outputDirectory;

    public File sourceDirectory;

    public String sourceFileExt;

    public String outputFileExt;

    public boolean xmlValidation;

    public boolean skip;

    public boolean verbose;

    public int numberOfThreads;

    public List<String> classpathElements;

    public Map<String, String> additionalProperties;
    
    public boolean failOnMissingSourceDirectory = true;

    public String sourceScanner = StaleSourceScanner.class.getName();

    public String additionalClasspath;
}
