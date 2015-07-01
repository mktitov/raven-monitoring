/*
 *  Copyright 2008 Mikhail Titov.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */

package org.raven.net.impl;

import org.raven.net.*;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Mikhail Titov
 */
public class IpRange
{
    private final static Pattern ipPattern = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)\\.(\\d+)");

    private final long fromIp;
    private final long toIp;

    public IpRange(String fromStrIp, String toStrIp) throws InvalidIpRangeException, InvalidIpException
    {
        fromIp = parseIp(fromStrIp);
        toIp = parseIp(toStrIp);
        if (fromIp>toIp)
            throw new InvalidIpRangeException(fromStrIp, toStrIp);
    }

    public Iterator<String> getIterator()
    {
        return new Iterator<String>() {
            private long currentIp = fromIp;

            public boolean hasNext()
            {
                return currentIp<=toIp;
            }

            public String next()
            {
                return unparseIp(currentIp++);
            }

            public void remove() {
                throw new UnsupportedOperationException("Not supported operation.");
            }
        };
    }

    private static long parseIp(String ipStr) throws InvalidIpException
    {
        long ip=0;
        Matcher ipMatcher = ipPattern.matcher(ipStr);
        if (!ipMatcher.matches())
            throw new InvalidIpException(ipStr);
        for (int i=1; i<=4; ++i)
        {
            try{
                short octet = Short.parseShort(ipMatcher.group(i));
                if (octet>255) throw new InvalidIpException(ipStr);
                if (i!=1) ip<<=8;
                ip|=octet;
            }catch(NumberFormatException e){
                throw new InvalidIpException(ipStr);
            }
        }
        return ip;
    }

    private static String unparseIp(long ip)
    {
        long mask=255l;
        StringBuffer strIp = new StringBuffer();
        for (int i=0; i<4; ++i)
        {
            if (i!=0) ip>>=8;
            strIp.insert(0, (i!=3? ".":"")+(ip&mask));
        }
        return strIp.toString();
    }
}
