package org.gluster.test;

import java.io.File;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.util.TreeMap;

import org.apache.hadoop.fs.glusterfs.GlusterFSBrickClass;
import org.apache.hadoop.fs.glusterfs.GlusterFUSEInputStream;
import org.junit.Test;

public class TestGlusterFuseInputStream{

    @Test
    public void testDoubleClose() throws IOException{
    	GlusterFUSEInputStream gfi=null;
    	for (int i = 1 ; i < 8 ; i=i*2){
    		int reads=0;
    		try {
				GlusterFUSEInputStream.LENGTH = 65536*i;//128m
				gfi= new GlusterFUSEInputStream ( new
						File("/mnt/glusterfs/sqlite2"), null, "localhost") ; 
				while(gfi.read()!=-1){
					reads++;
				}
				System.out.println(" pos " + gfi.getPos());
				gfi.close();
				System.out.println(" pass " + i);
			} 
    		catch (BufferUnderflowException e) {
    			e.printStackTrace();
    		}
    		finally{
    			System.out.println(reads);
    			gfi.close();
    		}
    	}
    }

}
