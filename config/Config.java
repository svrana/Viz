package com.first3.viz;

/**
 * Do not change this file - changes will be overwritten with the contents of
 * the build.properties file at build time (when compiled with ant, i.e., ant
 * debug).
 */
public class Config
{
    /**
     * The string used by the Provider to locate its database.  This is
     * always com.first.viz unless it's the free Amazon version then it's
     * com.first.vizfree;
     */
    public static final String CONTENT_AUTHORITY = @CONFIG.CONTENT_AUTHORITY@;

    /** Whether or not this is an Amazon build */
    public static final boolean isAmazonVersion() {
        return @CONFIG.IS_AMZ_VERSION@;
    }

    public static final boolean forceVimeoSearchFix() {
        return @CONFIG.FORCE_VIMEO_SEARCH_FIX@;
    }
}
