/*
 * Copyright 2012-2014, First Three LLC
 *
 * This file is a part of Viz.
 *
 * Viz is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * Viz is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Viz.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.first3.viz.utils;

public class StringBuffer {
    final String buffer;

    private StringBuffer(String s) {
        this.buffer = s;
    }

    public static StringBuffer fromString(String s) {
        return new StringBuffer(s);
    }

    /*
     * Return the index of the next character in the StringBuffer
     * after s or -1 if s is not found in StringBuffer.
     */
    public int nextIndex(String s) {
        int start = buffer.indexOf(s);
        if (start == -1) {
            return -1;
        }
        // there is not a valid index after s, so return -1
        if (start + s.length() == buffer.length()) {
            return -1;
        }
        return (start + s.length());
    }

    /*
     * Return the remaining string after s in StringBuffer.
     */
    public StringBuffer after(String s) {
        int start = nextIndex(s);
        if (start == -1) {
            return null;
        }
        return new StringBuffer(buffer.substring(start));
    }

    /*
     * Starting at the beginning of StringBuffer, locate the String
     * that occurs before String s.
     *
     * For example, before(".") on StringBuffer one.two.mp4 would
     * return "one".
     */
    public StringBuffer before(String s) {
        int start = buffer.indexOf(s);
        if (start == -1 || start == 0) {
            return null;
        }
        return new StringBuffer(buffer.substring(0, start));
    }

    public String stringBefore(String s) {
        StringBuffer sb = before(s);
        if (sb != null) {
            return sb.toString();
        }
        return null;
    }

    public String stringAfter(String a) {
        StringBuffer sb = after(a);
        if (sb != null) {
            return sb.toString();
        }
        return null;
    }

    public int indexOf(String s) {
        return buffer.indexOf(s);
    }

    /*
     * Return a string that contains the contents between a and b,
     * but not including either.
     */
    public String stringBetween(String a, String b) {
        StringBuffer afterA = this.after(a);
        if (afterA == null) {
            return null;
        }
        return afterA.stringBefore(b);
    }

    public StringBuffer between(String a, String b) {
        StringBuffer afterA = this.after(a);
        if (afterA == null) {
            return null;
        }
        return afterA.before(b);
    }

    /**
     * Return a substring of the StringBuffer such that param a resides
     * at the end of the StringBuffer.  Returns null if a is not found
     * in StringBuffer.
     */
    public String stringEndsWith(String a) {
        int aStartsAt = buffer.indexOf(a);
        if (aStartsAt == -1) {
            return null;
        }
        return buffer.substring(0, aStartsAt + a.length());
    }

    /**
     *  Return a substring of the StringBuffer such that param a resides
     *  at the beginning of the StringBuffer.  The search begins at the
     *  beginning of the StringBuffer.  Returns null if a is not found in
     *  StringBuffer.
     */
    public String stringStartsWith(String a) {
        int aStartsAt = buffer.indexOf(a);
        if (aStartsAt == -1) {
            return null;
        }
        return buffer.substring(aStartsAt, buffer.length());
    }

    public StringBuffer startsWith(String a) {
        String data = stringStartsWith(a);
        if (data == null) {
            return null;
        }
        return new StringBuffer(data);
    }

    /**
     * Return the string that contains all of String a at its beginning and
     * all of String b at its end, i.e., such that both a and b exist in
     * the StringBuffer.
     */
    public String stringAfterEndsWith(String a, String b) {
        StringBuffer afterA = this.after(a);
        if (afterA == null) {
            return null;
        }
        return afterA.stringEndsWith(b);
    }

    public String toString() {
        return buffer.toString();
    }

    /**
     * Return the last instance of param a in StringBuffer.
     *
     * For example, with a StringBuffer of "one.two.mp4"
     *  lastStringEndsWith(".") would return ".mp4"
     */
    public String lastStringEndsWith(String a) {
        int loc = buffer.lastIndexOf(a);
        if (loc == -1) {
            return null;
        }
        return buffer.substring(loc);
    }

    /*
     * Just like stringAfter, but starts looking for matching
     * string at the end of the StringBuffer.
     */
    public String lastStringAfter(String a) {
        int loc = buffer.lastIndexOf(a);
        if (loc == -1) {
            return null;
        }
        // no content after matching string
        if (loc == buffer.length()) {
            return null;
        }
        return buffer.substring(loc+1, buffer.length());
    }

    public StringBuffer lastAfter(String a) {
        String found = lastStringAfter(a);
        if (found == null) {
            return null;
        }
        return StringBuffer.fromString(found);
    }

    /**
     * Just like stringBefore except starts looking for string a
     * at the end of the StringBuffer.
     *
     * For example, with a StringBuffer of "one.two.mp4"
     *  lastStringEndsWith(".") would return "one.two".
     */
    public String lastStringBefore(String a) {
        int loc = buffer.lastIndexOf(a);
        if (loc == -1) {
            return null;
        }
        return buffer.substring(0, loc);
    }

    /**
     *  Return a new string buffer that is at most length long.
     *  If maxLength is longer than the length of the StringBuffer,
     *  do nothing.  I.e., a non-barfing version of substring.
     */
    public StringBuffer trimTo(int maxLength) {
        if (buffer.length() <= maxLength) {
            return this;
        }
        return new StringBuffer(buffer.substring(0, maxLength));
    }

    public String stringTrimTo(int maxLength) {
        if (buffer.length() <= maxLength) {
            return buffer;
        }
        return buffer.substring(0, maxLength);
    }
}
