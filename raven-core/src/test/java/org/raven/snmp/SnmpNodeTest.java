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

package org.raven.snmp;

import org.junit.Assert;
import org.junit.Test;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;

/**
 *
 * @author Mikhail Titov
 */
public class SnmpNodeTest extends Assert
{
    @Test
    public void get() throws Exception
    {
        UdpAddress addr = new UdpAddress("127.0.0.1/161");
        
        CommunityTarget target = new CommunityTarget(addr, new OctetString("public"));
        target.setRetries(1);
        target.setTimeout(1500);
        target.setVersion(SnmpConstants.version1);
        
        PDU pdu = new PDU();
        pdu.add(new VariableBinding(new OID("1.3.6.1.2.1.1.3.0")));
        pdu.setType(PDU.GET);
        
        TransportMapping transport = new DefaultUdpTransportMapping();
        transport.listen();
        try
        {
            Snmp snmp = new Snmp(transport);
            ResponseEvent res = snmp.send(pdu, target);
            if (res.getError()!=null)
                throw res.getError();
            assertNotNull(res.getResponse());
        }finally
        {
            transport.close();
        }
    }
}
