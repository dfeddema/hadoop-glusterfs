package org.apache.hadoop.fs.glusterfs;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

public class BufferedRandomAccessFile extends RandomAccessFile {
        // BUF映射在当前文件首的偏移地址
        private long bufstartpos;
        // BUF映射在当前文件尾的偏移地址
        private long bufendpos;
        // 当前类文件指针的便宜地址
        private long curpos = 0;
        // 当改值为真的时候，把buf[]中尚未写入磁盘的数据写入磁盘
        private boolean bufdirty;
        // 已经使用的字节
        private int bufusedsize;
        // 指示当前文件的尾偏移地址，主要考虑到追加因素
        private long fileendpos;
        // 缓冲区字节长度
        private long bufbitlen;
        // 缓冲区字节大小
        private long bufsize;
        // 设置的需要的缓冲区
        private byte[] buf;
        // 
        private int initfilelen;

        public BufferedRandomAccessFile(String name, String mode)
                        throws FileNotFoundException {
                super(name, mode);
        }

        /**
         * 读取当前文件POS位置所在的字节
         * 
         * @param pos
         * @return
         * @throws IOException
         */
        public byte read(long pos) throws IOException {
                if (pos < this.bufstartpos || pos > this.bufendpos) {
                        this.flushbuf();
                        this.seek(pos);
                        if ((pos < this.bufstartpos) || (pos > this.bufendpos))
                                throw new IOException();
                }
                this.curpos = pos;
                return this.buf[(int) (pos - this.bufstartpos)];
        }

        /**
         * 刷新缓冲区
         * 
         * @throws IOException
         */
        private void flushbuf() throws IOException {
                if (this.bufdirty == true) {
                        if (super.getFilePointer() != this.bufstartpos) {
                                super.seek(this.bufstartpos);
                        }
                        super.write(this.buf, 0, this.bufusedsize);
                        this.bufdirty = false;
                }
        }

        /**
         * 移动指针到pos位置，并且把buf[]映射填充到POS所在的文件块
         */
        public void seek(long pos) throws IOException {
                if ((pos < this.bufstartpos) || (pos > this.bufendpos)) {
                        this.flushbuf();
                        if ((pos >= 0) && (pos <= this.fileendpos)
                                        && (this.fileendpos != 0)) {
                                this.bufstartpos = pos * this.bufbitlen / this.bufbitlen;
                                this.bufusedsize = this.fillbuf();
                        } else if ((pos == 0) && (this.fileendpos == 0)
                                        || (pos == this.fileendpos + 1)) {
                                this.bufstartpos = pos;
                                this.bufusedsize = 0;
                        }
                        this.bufendpos = this.bufstartpos + this.bufsize - 1;
                }
                this.curpos = pos;
        }

        /**
         * 根据bufstartpos，填充buf[]
         * 
         * @return
         * @throws IOException
         */
        private int fillbuf() throws IOException {
                super.seek(this.bufstartpos);
                this.bufdirty = false;
                return super.read(this.buf);
        }

        /**
         * 根据POS的不同以及BUF的位置：存在修改、追加、BUF中、BUF外等情况。 在逻辑判断时，把最可能出现的情况，最先判断可以提高速度
         * 
         * @param bw
         * @param pos
         * @return
         * @throws IOException
         */
        public boolean write(byte bw, long pos) throws IOException {
                if ((pos >= this.bufstartpos) && (pos <= this.bufendpos)) {
                        this.buf[(int) (pos - this.bufstartpos)] = bw;
                        this.bufdirty = true;
                        if (pos == this.fileendpos + 1) {
                                this.fileendpos++;
                                this.bufusedsize++;
                        }
                } else {
                        this.seek(pos);
                        if ((pos >= 0) && (pos <= this.fileendpos)
                                        && (this.fileendpos != 0)) {
                                this.buf[(int) (pos - this.bufstartpos)] = bw;
                        } else if (((pos == 0) && (this.fileendpos == 0))
                                        || (pos == this.fileendpos + 1)) {
                                this.buf[0] = bw;
                                this.fileendpos++;
                                this.bufusedsize = 1;
                        } else {
                                throw new IndexOutOfBoundsException();
                        }
                        this.bufdirty = true;
                }
                this.curpos = pos;
                return true;
        }

        public boolean append(byte bw) throws IOException {
                return this.write(bw, this.fileendpos + 1);
        }

        public boolean write(byte bw) throws IOException {
                return this.write(bw, this.curpos);
        }

        public long length() throws IOException {
                return this.max(this.fileendpos + 1, this.initfilelen);
        }

        public long getFilePointer() throws IOException {
                return this.curpos;
        }

        private long max(long l, int initfilelen2) {
                // TODO Auto-generated method stub
                return 0;
        }

        public void write(byte b[], int off, int len) throws IOException {
                long writeendpos = this.curpos + len - 1;
                if (writeendpos <= this.bufendpos) { // b[] in cur buf
                        System.arraycopy(b, off, this.buf,
                                        (int) (this.curpos - this.bufstartpos), len);
                        this.bufdirty = true;
                        this.bufusedsize = (int) (writeendpos - this.bufstartpos + 1);
                } else { // b[] not in cur buf
                        super.seek(this.curpos);
                        super.write(b, off, len);
                }
                if (writeendpos > this.fileendpos)
                        this.fileendpos = writeendpos;
                this.seek(writeendpos + 1);
        }

        public void write(byte b[]) throws IOException {
                this.write(b, 0, b.length);
        }

        public int read(byte b[], int off, int len) throws IOException {
                long readendpos = this.curpos + len - 1;
                if (readendpos <= this.bufendpos && readendpos <= this.fileendpos) {
                        System.arraycopy(this.buf, (int) (this.curpos - this.bufstartpos),
                                        b, off, len);
                } else {
                        if (readendpos > this.fileendpos) {
                                len = (int) (this.length() - this.curpos + 1);
                        }
                        super.seek(this.curpos);
                        len = super.read(b, off, len);
                        readendpos = this.curpos + len - 1;
                }
                this.seek(readendpos + 1);
                return len;
        }

        public int read(byte b[]) throws IOException {
                return this.read(b, 0, b.length);
        }

        public void setLength(long newLength) throws IOException {
                if (newLength > 0) {
                        this.fileendpos = newLength - 1;
                } else {
                        this.fileendpos = 0;
                }
                super.setLength(newLength);
        }

        public void close() throws IOException {
                this.flushbuf();
                super.close();
        }
}
