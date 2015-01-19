package edu.uj.lesson2.utilities;

import android.opengl.GLES20;
import android.opengl.Matrix;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

// import org.lwjgl.opengl.GL11;

public class MD2Model {
    protected RandomAccessFile in;
    protected byte b[] = new byte[4];
    protected MD2Header Head;
    protected MD2Frame[] frame;
    protected MD2Vtx[] vtx;
    protected MD2Face[] face;
    protected Mesh_UV[] UV;
    protected MD2TexCoord[] MD2_UV;
    protected MD2FrameInfo[] FrameInfo;
    protected String textureName;
    protected int nFrames, nTri, nVtx, nUV;
    protected long ItemLoop;
    protected int TexID; //this should be type GLuint
    protected float width;
    protected float height;
    protected float depth;

    protected FloatBuffer[] mFrameBuffers;

    // shader variables
    protected int mMVPMatrixHandle;
    protected int mPositionHandle;

//    private ShaderProgram identityProgram;

    public MD2Model(int positionHandle, int mvpMatrixHandle) {
        face = null;
        frame = null;
        UV = null;
        Head = new MD2Header();
        TexID = 0;

//        identityProgram = new ShaderProgram();
//        identityProgram.attach(new IdentityFragmentShader());
//        identityProgram.attach(new IdentityVertexShader());
//        identityProgram.use();
//
//        mPositionHandle = GLES20.glGetAttribLocation(identityProgram.getHandle(), "vPosition");
//        mMVPMatrixHandle = GLES20.glGetAttribLocation(identityProgram.getHandle(), "u_MVPMatrix");

        mPositionHandle = positionHandle;
        mMVPMatrixHandle = mvpMatrixHandle;
    }

    private void LoadHeader() {
        try {
            for (int i = 0; i < 4; i++)
                Head.ID[i] = (int) in.read();

            Head.Version = readInt();
            Head.TexWidth = readInt();
            Head.TexHeight = readInt();
            Head.FrameSize = readInt();
            Head.nTextures = readInt();
            Head.nVertices = readInt();
            Head.nTexCoords = readInt();
            Head.nTriangles = readInt();
            Head.nGLCmd = readInt();
            Head.nFrames = readInt();
            Head.TexOffset = readInt();
            Head.UVOffset = readInt();
            Head.FaceOffset = readInt();
            Head.FrameOffset = readInt();
            Head.GLCmdOffset = readInt();
            Head.EOFOffset = readInt();
        } catch (IOException e) {
            System.out.println("Error reading md2 file.");
        }

        nFrames = Head.nFrames;
        nTri = Head.nTriangles;
        nVtx = Head.nVertices;
        nUV = Head.nTexCoords;

    }

    private void LoadFrames() {
        try {
            frame = new MD2Frame[nFrames];    //array is the size of nFrames that we get form header
            for (int FrameLoop = 0; FrameLoop != nFrames; ++FrameLoop) {
                frame[FrameLoop] = new MD2Frame();
                frame[FrameLoop].Vtx = new Mesh_Vtx[nVtx];
                frame[FrameLoop].Norm = new Mesh_Vtx[nVtx];
            }
            vtx = new MD2Vtx[Head.nVertices];
            face = new MD2Face[Head.nTriangles];
            UV = new Mesh_UV[Head.nTriangles];
            MD2_UV = new MD2TexCoord[Head.nTexCoords];

            //read first texture name
            int[] texName = new int[64];
            if (Head.nTextures > 0) {
                in.seek(Head.TexOffset);
                textureName = "";
                for (int i = 0; i < 64; i++) {
                    texName[i] = (int) in.read();
                    textureName += (char) texName[i];
                }
                System.out.println("Texture Name:" + textureName);
            }

            //read face data
            in.seek(Head.FaceOffset);
            for (int i = 0; i < Head.nTriangles; i++) {
                face[i] = new MD2Face();
                face[i].p1 = readShort();
                face[i].p2 = readShort();
                face[i].p3 = readShort();
                face[i].uv1 = readShort();
                face[i].uv2 = readShort();
                face[i].uv3 = readShort();
            }

            //read UV data
            in.seek(Head.UVOffset);
            for (int i = 0; i < Head.nTexCoords; i++) {
                MD2_UV[i] = new MD2TexCoord();
                MD2_UV[i].u = readShort();
                MD2_UV[i].v = readShort();
            }

            // Convert into regular UVs
            for (int i = 0; i != nUV; ++i) {
                UV[i] = new Mesh_UV();
                UV[i].u = ((float) MD2_UV[i].u) / Head.TexWidth;
                UV[i].v = ((float) MD2_UV[i].v) / Head.TexHeight;
            }

            float[] mins = { 0, 0, 0 },
                    maxs = { 0, 0, 0 };

            // Load frame vertex info
            FrameInfo = new MD2FrameInfo[nFrames];
            for (int i = 0; i != nFrames; ++i) {
                // Get frame conversion data
                in.seek(Head.FrameOffset + (Head.FrameSize * i));
                FrameInfo[i] = new MD2FrameInfo();
                FrameInfo[i].Scale[0] = readFloat();
                FrameInfo[i].Scale[1] = readFloat();
                FrameInfo[i].Scale[2] = readFloat();

                FrameInfo[i].Translate[0] = readFloat();
                FrameInfo[i].Translate[1] = readFloat();
                FrameInfo[i].Translate[2] = readFloat();

                //read frame info (frame name)
                int c;
                for (int k = 0; k < 16; k++) {
                    c = (int) in.read();
                    FrameInfo[i].Name[k] = (char) c;
                }

                // Read MD2 style vertex data
                //in.seek(Head.FrameOffset + (Head.FrameSize * i)+ _size of this shit up_);
                for (int m = 0; m < nVtx; m++) {
                    vtx[m] = new MD2Vtx();
                    for (int k = 0; k < 3; k++) {
                        vtx[m].Vtx[k] = (int) in.read();
                    }

                    vtx[m].lNorm = (int) in.read();
                }

                // Convert vertices
                for (int j = 0; j != nVtx; ++j) {
                    frame[i].Vtx[j] = new Mesh_Vtx();
                    frame[i].Vtx[j].x = (vtx[j].Vtx[0] * FrameInfo[i].Scale[0]) + FrameInfo[i].Translate[0];
                    frame[i].Vtx[j].y = (vtx[j].Vtx[1] * FrameInfo[i].Scale[0]) + FrameInfo[i].Translate[1];
                    frame[i].Vtx[j].z = (vtx[j].Vtx[2] * FrameInfo[i].Scale[0]) + FrameInfo[i].Translate[2];

                    if (frame[i].Vtx[j].x < mins[0])
                        mins[0] = frame[i].Vtx[j].x;

                    if (frame[i].Vtx[j].x > maxs[0])
                        maxs[0] = frame[i].Vtx[j].x;

                    if (frame[i].Vtx[j].y < mins[1])
                        mins[1] = frame[i].Vtx[j].y;

                    if (frame[i].Vtx[j].y > maxs[1])
                        maxs[1] = frame[i].Vtx[j].y;

                    if (frame[i].Vtx[j].z < mins[2])
                        mins[2] = frame[i].Vtx[j].z;

                    if (frame[i].Vtx[j].z > maxs[2])
                        maxs[2] = frame[i].Vtx[j].z;

                    if (frame[i].Vtx[j].x < mins[0])
                        mins[0] = frame[i].Vtx[j].x;

                    if (frame[i].Vtx[j].x > maxs[0])
                        maxs[0] = frame[i].Vtx[j].x;
                }
            }

            width = maxs[0] - mins[0];
            height = maxs[1] - mins[1];
            depth = maxs[2] - mins[2];

            // Calc normals for each frame
            // TODO: Index out of bounds exception here
//            for (int i = 0; i < nFrames; i++) {
//                // Calc face normal
//                for (int j = 0; j < nTri; j++) {
//                    calcNormal(frame[i].Vtx[face[j].p1], frame[i].Vtx[face[j].p2], frame[i].Vtx[face[j].p3], frame[i].Norm[j] = new Mesh_Vtx());
//                }
//            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error reading md2 file.");
        }
    }

    public float getDepth() {
        return depth;
    }

    public float getHeight() {
        return height;
    }

    public float getWidth() {
        return width;
    }

    //Read all data form file
    //We dont check if this is real md2 file - version, id, length (todo)
    public void Load(String filename) {
        try {
            in = new RandomAccessFile(filename, "rw");

            LoadHeader();
            Head.printHeader();
            readShort();
            LoadFrames();
            PrepareFrameBuffers();

            in.close();
        } catch (FileNotFoundException e) {
            System.out.println("File " + filename + " not found.");
        } catch (IOException e) {
            System.out.println("Error reading md2 file.");
        }
    }

    //Read all data form file
    //We dont check if this is real md2 file - version, id, length (todo)
    public void Load(RandomAccessFile input) {
        try {
            // in = new RandomAccessFile(in, "rw");
            in = input;

            LoadHeader();
            Head.printHeader();
            readShort();
            LoadFrames();
            PrepareFrameBuffers();

            in.close();
        } catch (IOException e) {
            System.out.println("Error reading md2 file.");
        }
    }

    private void PrepareFrameBuffers() {
        final int mBytesPerFloat = 4;
        mFrameBuffers = new FloatBuffer[nFrames];

        for (int Frame = 0; Frame < nFrames; Frame++) {
            FloatBuffer modelVertices = ByteBuffer.allocateDirect(nTri * 3 * 3 * mBytesPerFloat)
                    .order(ByteOrder.nativeOrder()).asFloatBuffer();

            for (int part = 0; part < nTri; ++part) {
                modelVertices.put(frame[Frame].Vtx[face[part].p1].x);
                modelVertices.put(frame[Frame].Vtx[face[part].p1].y);
                modelVertices.put(frame[Frame].Vtx[face[part].p1].z);

                modelVertices.put(frame[Frame].Vtx[face[part].p2].x);
                modelVertices.put(frame[Frame].Vtx[face[part].p2].y);
                modelVertices.put(frame[Frame].Vtx[face[part].p2].z);

                modelVertices.put(frame[Frame].Vtx[face[part].p3].x);
                modelVertices.put(frame[Frame].Vtx[face[part].p3].y);
                modelVertices.put(frame[Frame].Vtx[face[part].p3].z);
            }

            mFrameBuffers[Frame] = modelVertices;
        }
    }

    public void Draw(int Frame, float[] mMVPMatrix, float[] mViewMatrix, float[] mModelMatrix, float[] mProjectionMatrix) {
        //limit frame range
        if (Frame >= nFrames)
            Frame = 0;

        final int mPositionOffset = 0;
        final int mElementsPerVertex = 3;
        final int mBytesPerFloat = 4;
        final int mStrideBytes = mElementsPerVertex * mBytesPerFloat; // elements per vertex

//        Matrix.setIdentityM(mModelMatrix, 0);

        FloatBuffer modelVertices = mFrameBuffers[Frame];
        modelVertices.position(mPositionOffset);

        GLES20.glEnableVertexAttribArray(mPositionHandle);

        GLES20.glVertexAttribPointer(mPositionHandle, mElementsPerVertex, GLES20.GL_FLOAT, false,
                mStrideBytes, modelVertices);

//        Matrix.setIdentityM(mModelMatrix, 0);
//        Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);
//        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);
//
//        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, modelVertices.limit() / mElementsPerVertex);

        GLES20.glDisableVertexAttribArray(mPositionHandle);
    }

    public int getFrameCount() {
        return nFrames;
    }

    public String getTexName() {
        return textureName;
    }

    public void setTexture(int TexNum) {    //TexNum should be type GLuint
        TexID = TexNum;
    }

    public void calcNormal(Mesh_Vtx v1, Mesh_Vtx v2, Mesh_Vtx v3, Mesh_Vtx Result) {
        double v1x, v1y, v1z, v2x, v2y, v2z;
        double nx, ny, nz;
        double vLen;

        // Calculate vectors
        v1x = v1.x - v2.x;
        v1y = v1.y - v2.y;
        v1z = v1.z - v2.z;

        v2x = v2.x - v3.x;
        v2y = v2.y - v3.y;
        v2z = v2.z - v3.z;

        // Get cross product of vectors

        nx = (v1y * v2z) - (v1z * v2y);
        ny = (v1z * v2x) - (v1x * v2z);
        nz = (v1x * v2y) - (v1y * v2x);

        // Normalise final vector
        vLen = Math.sqrt((nx * nx) + (ny * ny) + (nz * nz));

        Result.x = (float) (nx / vLen);
        Result.y = (float) (ny / vLen);
        Result.z = (float) (nz / vLen);
    }

    public int readInt() throws IOException {
        for (int i = 0; i < 4; i++)
            b[i] = (byte) in.readUnsignedByte();
        //java uses big endian, but in .bsp files
        long l = (long) b[0] & 0xFF;                    //the little endian is used
        l += ((long) b[1] & 0xFF) << 8;                //therefore the switch of an endian
        l += ((long) b[2] & 0xFF) << 16;                //must be performed
        l += ((long) b[3] & 0xFF) << 24;

        return (int) l;
    }

    public short readShort() throws IOException {
        for (int i = 0; i < 2; i++)
            b[i] = (byte) in.readUnsignedByte();

        short returnNumber = (short) ((b[0]) & 0xFF);
        returnNumber += (short) ((b[1]) & 0xFF) << 8;

        return returnNumber;
    }

    public float readFloat() throws IOException {
        for (int i = 0; i < 4; i++)
            b[i] = (byte) in.readUnsignedByte();

        int i = (int) b[0] & 0xFF;
        i |= ((int) b[1] & 0xFF) << 8;
        i |= ((int) b[2] & 0xFF) << 16;
        i |= ((int) b[3] & 0xFF) << 24;

        return Float.intBitsToFloat(i);
    }

    public IntBuffer createIntBuffer(int size) {
        ByteBuffer temp = ByteBuffer.allocateDirect(4 * size);
        temp.order(ByteOrder.nativeOrder());

        return temp.asIntBuffer();
    }

    //supported structures
    protected class MD2Header {
        int[] ID;      // File Type - Normally 'IPD2'
        int Version;
        int TexWidth;   // Texture width
        int TexHeight;  // Texture height
        int FrameSize;  // Size for frames in bytes
        int nTextures;  // Number of textures
        int nVertices;  // Number of vertices
        int nTexCoords; // Number of UVs
        int nTriangles; // Number of polys
        int nGLCmd;     // Number of GL Commmands
        int nFrames;    // Number of frames
        int TexOffset;  // Offset to texture name(s)
        int UVOffset;   // Offset to UV data
        int FaceOffset; // Offset to poly data
        int FrameOffset;// Offset to first frame
        int GLCmdOffset;// Offset to GL Cmds
        int EOFOffset;  // Size of file

        MD2Header() {
            ID = new int[4];
        }

        public void printHeader() {
            String id = "";
            for (int i = 0; i < 4; i++) {
                id += (char) ID[i];
            }
            System.out.println("Id:" + id);
            System.out.println("Version:" + Version);
            System.out.println("TexWidth:" + TexWidth);
            System.out.println("TexHeight:" + TexHeight);
            System.out.println("FrameSize:" + FrameSize);
            System.out.println("nTextures:" + nTextures);
            System.out.println("nVertices:" + nVertices);
            System.out.println("nTexCoords:" + nTexCoords);
            System.out.println("nTriangles:" + nTriangles);
            System.out.println("nGLCmd:" + nGLCmd);
            System.out.println("nFrames:" + nFrames);
            System.out.println("TexOffset:" + TexOffset);
            System.out.println("UVOffset:" + UVOffset);
            System.out.println("FaceOffset:" + FaceOffset);
            System.out.println("FrameOffset:" + FrameOffset);
            System.out.println("GLCmdOffset:" + GLCmdOffset);
            System.out.println("EOFOffset:" + EOFOffset);
        }
    }

    protected class MD2FrameInfo {
        float[] Scale;
        float[] Translate;
        char[] Name;

        public MD2FrameInfo() {
            Scale = new float[3];
            Translate = new float[3];
            Name = new char[16];
        }
    }

    protected class MD2Face {
        short p1, p2, p3;
        short uv1, uv2, uv3;
    }

    protected class MD2Vtx {
        int[] Vtx = new int[3];    //in c file is here both char
        int lNorm;
    }

    protected class Mesh_Vtx {
        float x, y, z;
    }

    protected class Mesh_UV {
        float u, v;
    }

    protected class MD2Frame {
        Mesh_Vtx[] Vtx = new Mesh_Vtx[nVtx];
        Mesh_Vtx[] Norm = new Mesh_Vtx[nVtx];
    }

    protected class MD2TexCoord {
        short u, v;
    }
}
