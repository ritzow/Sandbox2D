/*
 * Copyright 2014 Ruediger Moeller.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.nustaq.serialization.simpleapi;

import org.nustaq.offheap.bytez.malloc.MallocBytez;
import org.nustaq.serialization.*;
import org.nustaq.serialization.coders.FSTBytezDecoder;
import org.nustaq.serialization.coders.FSTBytezEncoder;

import java.io.IOException;

/**
 * Created by ruedi on 09.11.14.
 *
 * enables zero copy encoding to offheap memory. The encoding is platform dependent (endianess) and
 * no attemps on compression are made.
 *
 * Use case: messaging, offheap en/decoding, tmp preservation of state
 * NOT thread safe
 *
 * Future version may choose to operate on DirectByteBuffer in case unsafe class vanishes
 *
 * Do not confuse this with a stream. Each single writeObject is an isolated operation,
 * so restoring of references inside an object graph only happens for refs inside the object graph
 * given to writeObject.
 *
 * ***********************************************************************
 * USE ONLY IF DEFAULTCODER (no unsafe) HAS BEEN PROVEN TO SLOW YOU DOWN.
 * ***********************************************************************
 *
 * Note this does not satisfy the FSTCoder interface. Its purely targeted to directly read/write native memory
 * allocates using unsafe.
 */
public class OffHeapCoder {

    protected FSTConfiguration conf;
    MallocBytez writeTarget;
    MallocBytez readTarget;
    FSTObjectOutput out;
    FSTObjectInput in;

    public OffHeapCoder() {
        this(true);
    }
    public OffHeapCoder(boolean sharedRefs) {
        conf = FSTConfiguration.createUnsafeBinaryConfiguration();
        conf.setShareReferences(sharedRefs);
        writeTarget = new MallocBytez(0l,0);
        readTarget = new MallocBytez(0l,0);
        conf.setStreamCoderFactory(new FSTConfiguration.StreamCoderFactory() {
            @Override
            public FSTEncoder createStreamEncoder() {
                FSTBytezEncoder fstBytezEncoder = new FSTBytezEncoder(conf, writeTarget);
                fstBytezEncoder.setAutoResize(false);
                return fstBytezEncoder;
            }

            @Override
            public FSTDecoder createStreamDecoder() {
                return new FSTBytezDecoder(conf,readTarget);
            }

            ThreadLocal input = new ThreadLocal();
            ThreadLocal output = new ThreadLocal();
            @Override
            public ThreadLocal getInput() {
                return input;
            }

            @Override
            public ThreadLocal getOutput() {
                return output;
            }

        });
        if (sharedRefs) {
            out = conf.getObjectOutput();
            in = conf.getObjectInput();
        } else {
            out = new FSTObjectOutputNoShared(conf);
            in = new FSTObjectInputNoShared(conf);
        }
    }

    /**
     * throw
     * @param preregister
     */
    public OffHeapCoder( boolean sharedRefs, Class ... preregister ) {
        this(sharedRefs);
        conf.registerClass(preregister);
    }

    public OffHeapCoder( Class ... preregister ) {
        this(true);
        conf.registerClass(preregister);
    }

    /**
     * throws FSTBufferTooSmallException in case object does not fit into given range
     *
     * @param o
     * @param address
     * @param availableSize
     * @throws IOException
     * @return number of bytes written to the memory region
     */
    public int toMemory(Object o, long address, int availableSize) throws IOException {
        out.resetForReUse();
        writeTarget.setBase(address, availableSize);
        out.writeObject(o);
        int written = out.getWritten();
        return written;
    }

    public Object toObject(long address, int availableSize) throws IOException, ClassNotFoundException {
        in.resetForReuse(null);
        readTarget.setBase(address,availableSize);
        Object o = in.readObject();
        return o;
    }

}
