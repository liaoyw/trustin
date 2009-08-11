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
 * @(#) $Id: ByteBuffers.java 19 2005-04-19 15:29:55Z trustin $
 */
package net.gleamynode.netty2;

import java.nio.ByteBuffer;


/**
 * @author Trustin Lee (http://gleamynode.net/)
 * @version $Rev: 19 $, $Date: 2005-04-20 00:29:55 +0900 $
 */
class ByteBuffers {
    private static final byte[] highDigits;
    private static final byte[] lowDigits;

    // initialize lookup tables
    static {
        final byte[] digits =
            { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

        int i;
        byte[] high = new byte[256];
        byte[] low = new byte[256];

        for (i = 0; i < 256; i++) {
            high[i] = digits[i >>> 4];
            low[i] = digits[i & 0x0F];
        }

        highDigits = high;
        lowDigits = low;
    }

    public static String getHexdump(ByteBuffer in) {
        int size = in.remaining();

        if (size == 0) {
            return "empty";
        }

        StringBuffer out = new StringBuffer((in.remaining() * 3) - 1);

        in.mark();

        // fill the first
        int byteValue = in.get() & 0xFF;
        out.append((char) highDigits[byteValue]);
        out.append((char) lowDigits[byteValue]);
        size--;

        // and the others, too
        for (; size > 0; size--) {
            out.append(' ');
            byteValue = in.get() & 0xFF;
            out.append((char) highDigits[byteValue]);
            out.append((char) lowDigits[byteValue]);
        }

        in.reset();

        return out.toString();
    }
}
