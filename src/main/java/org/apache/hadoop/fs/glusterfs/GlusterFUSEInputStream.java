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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.hadoop.fs.FSError;
import org.apache.hadoop.fs.FSInputStream;
import org.apache.hadoop.fs.Path;

/*******************************************************
 * For open()'s FSInputStream
 *******************************************************/
class GlusterFUSEInputStream extends FSInputStream {
  FileInputStream fis;
  private long position;
  class TrackingFileInputStream extends FileInputStream {
	    public TrackingFileInputStream(File f) throws IOException {
	      super(f);
	    }
	    
	    public int read() throws IOException {
	      int result = super.read();
	      if (result != -1) {
	      }
	      return result;
	    }
	    
	    public int read(byte[] data) throws IOException {
	      int result = super.read(data);
	      if (result != -1) {
	      }
	      return result;
	    }
	    
	    public int read(byte[] data, int offset, int length) throws IOException {
	      int result = super.read(data, offset, length);
	      if (result != -1) {
	      }
	      return result;
	    }
	  }

  public GlusterFUSEInputStream(File f) throws IOException {
    this.fis = new TrackingFileInputStream(f);
  }
  
  public void seek(long pos) throws IOException {
    fis.getChannel().position(pos);
    this.position = pos;
  }
  
  public long getPos() throws IOException {
    return this.position;
  }
  
  public boolean seekToNewSource(long targetPos) throws IOException {
    return false;
  }
  
  /*
   * Just forward to the fis
   */
  public int available() throws IOException { return fis.available(); }
  public void close() throws IOException { fis.close(); }
  public boolean markSupport() { return false; }
  
  public int read() throws IOException {
    try {
      int value = fis.read();
      if (value >= 0) {
        this.position++;
      }
      return value;
    } catch (IOException e) {                 // unexpected exception
      throw new RuntimeException(e);                   // assume native fs error
    }
  }
  
  public int read(byte[] b, int off, int len) throws IOException {
    try {
      int value = fis.read(b, off, len);
      if (value > 0) {
        this.position += value;
      }
      return value;
    } catch (IOException e) {                 // unexpected exception
      throw new RuntimeException(e);                   // assume native fs error
    }
  }
  
  public int read(long position, byte[] b, int off, int len)
    throws IOException {
    ByteBuffer bb = ByteBuffer.wrap(b, off, len);
    try {
      return fis.getChannel().read(bb, position);
    } catch (IOException e) {
    	throw new RuntimeException(e); 
    }
  }
  
  public long skip(long n) throws IOException {
    long value = fis.skip(n);
    if (value > 0) {
      this.position += value;
    }
    return value;
  }
}