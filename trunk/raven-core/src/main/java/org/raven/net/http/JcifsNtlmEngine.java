/*
 *  Copyright 2009 Mikhail Titov.
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

package org.raven.net.http;

import java.io.IOException;
import jcifs.ntlmssp.Type1Message;
import jcifs.ntlmssp.Type2Message;
import jcifs.ntlmssp.Type3Message;
import org.apache.http.impl.auth.NTLMEngine;
import org.apache.http.impl.auth.NTLMEngineException;

/**
 *
 * @author Mikhail Titov
 */
public class JcifsNtlmEngine implements NTLMEngine
{
    public String generateType1Msg(String domain, String workstation) throws NTLMEngineException
    {
        Type1Message t1m = new Type1Message(0x00088207, domain, workstation);
        return jcifs.util.Base64.encode(t1m.toByteArray());
    }

    public String generateType3Msg(
            String username, String password, String domain, String workstation, String challenge)
        throws NTLMEngineException
    {
        Type2Message t2m;
        try {
            t2m = new Type2Message(jcifs.util.Base64.decode(challenge));
        } catch (IOException ex) {
            throw new NTLMEngineException("Invalid Type2 message", ex);
        }
        Type3Message t3m = new Type3Message(t2m, password, domain, username, workstation, 0x00088205);
        return jcifs.util.Base64.encode(t3m.toByteArray());
    }
}
