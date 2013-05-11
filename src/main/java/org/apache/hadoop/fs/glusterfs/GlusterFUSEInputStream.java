/**
 *
 * Copyright (c) 2011 Gluster, Inc. <http://www.gluster.com>
 * This file is part of GlusterFS.
 *
 * Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License.
 *
 */

package org.apache.hadoop.fs.glusterfs;

import java.io.*;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.TreeMap;

import org.apache.hadoop.fs.FSInputStream;
import org.apache.hadoop.fs.FileSystem;

public class GlusterFUSEInputStream extends FSInputStream{
    File f;
    boolean lastActive;
    long pos;
    boolean closed;
    String thisHost;
    MappedByteBuffer fuseInputStream;
    RandomAccessFile fsInputStream;
    GlusterFSBrickClass thisBrick;
    int nodeLocation;
    TreeMap<Integer, GlusterFSBrickClass> hnts;
    public static int LENGTH= 65536;//128m
    public GlusterFUSEInputStream(File f, TreeMap<Integer, GlusterFSBrickClass> hnts, String hostname) throws IOException{
        this.f=f;
        this.pos=0;
        this.closed=false;
        this.hnts=hnts;
        this.thisHost=hostname;
        this.fsInputStream=null;
        this.fuseInputStream=new RandomAccessFile(f, "rw").getChannel().map(
                FileChannel.MapMode.READ_ONLY, 0, LENGTH);
        this.lastActive=true; // true == FUSE, false == backed file

        String directFilePath=null;
        if(this.hnts!=null){
            directFilePath=findLocalFile(f.getPath(), this.hnts);
            if(directFilePath!=null){
                this.fsInputStream=new RandomAccessFile(directFilePath, "r");
                this.lastActive=!this.lastActive;
            }
        }
    }

    public String findLocalFile(String path,TreeMap<Integer, GlusterFSBrickClass> hnts){
        int i=0;
        String actFilePath=null;
        GlusterFSBrickClass gfsBrick=null;

        gfsBrick=hnts.get(0);

        /*
         * do a linear search for the matching host not worrying about file
         * stripes
         */
        for(i=0;i<hnts.size();i++){
            gfsBrick=hnts.get(i);
            actFilePath=gfsBrick.brickIsLocal(this.thisHost);
            if(actFilePath!=null){
                this.thisBrick=gfsBrick;
                this.nodeLocation=i;
                break;
            }
        }

        return actFilePath;
    }

    public long getPos() throws IOException{
        return pos;
    }

    public synchronized int available() throws IOException{
        return (int) ((f.length())-getPos());
    }

    public void seek(long pos) throws IOException{
        fuseInputStream.asReadOnlyBuffer().position((int)pos);
        if(fsInputStream!=null)
            fsInputStream.seek(pos);
    }

    public boolean seekToNewSource(long pos) throws IOException{
        return false;
    }

    public MappedByteBuffer chooseStream(long start,int[] nlen) throws IOException{
        GlusterFSBrickClass gfsBrick=null;
        MappedByteBuffer in=fuseInputStream;
        return in;
    }

    public synchronized int read() throws IOException{
        int byteRead=0;
        MappedByteBuffer in=null;

        if(closed)
            throw new IOException("Stream Closed.");

        int[] nlen={1};

        in=chooseStream(getPos(), nlen);
        byteRead=in.get();
        if(byteRead>=0){
            pos++;
        }

        return byteRead;
    }

    public synchronized int read(byte buff[],int off,int len) throws IOException{

        if(closed)
            throw new IOException("Stream Closed.");

        int[] nlen={len}; // hack to make len mutable
        int result= fuseInputStream.get(buff, off, len).remaining();
        if(result>0){
            pos+=result;
        }
        return result;
    }
 

    public synchronized void close() throws IOException{
        super.close();

        if(closed)
            return;

        if(fsInputStream!=null){
            fsInputStream.close();
        }

        fuseInputStream.clear();
        closed=true;
    }

    // Not supported - mark () and reset ()

    public boolean markSupported(){
        return false;
    }

    public void mark(int readLimit){
    }

    public void reset() throws IOException{
        throw new IOException("Mark/Reset not supported.");
    }
}
