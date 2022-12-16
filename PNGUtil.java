import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import java.util.zip.DataFormatException;

public class PNGUtil {
    private static final byte[] PNG_HEADER = new byte[] { (byte) 0x89, (byte) 0x50, (byte) 0x4E, (byte) 0x47, (byte) 0x0D, (byte) 0x0A, (byte) 0x1A, (byte) 0x0A };
    private static final byte[] IHDR = new byte[] { (byte) 0x49, (byte) 0x48, (byte) 0x44, (byte) 0x52 };
    private static final byte[] IDAT = new byte[] { (byte) 0x49, (byte) 0x44, (byte) 0x41, (byte) 0x54 };
    private static final byte[] IEND = new byte[] {(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x49, (byte) 0x45, (byte) 0x4E, (byte) 0x44, (byte) 0xAE, (byte) 0x42, (byte) 0x60, (byte) 0x82};

    public static byte[][][] getImgPixels(String imgPath){
        byte[] ihdr = getChunk(imgPath, IHDR);
        if(ihdr == null){
            return null;
        }
        int width = (int) uInt32FromByteArray(new byte[] { ihdr[0], ihdr[1], ihdr[2], ihdr[3] });
        int height = (int) uInt32FromByteArray(new byte[] { ihdr[4], ihdr[5], ihdr[6], ihdr[7] });
        byte bitDepth = ihdr[8];
        byte colourType = ihdr[9];
        byte compressionMethod = ihdr[10];
        byte filterMethod = ihdr[11];
        byte interlaceMethod = ihdr[12];

        byte[] idat = inflate(getChunk(imgPath, IDAT));
        if(idat == null){
            return null;
        }
        byte[][][] res = new byte[height][width][3];
        byte filter;
        byte channel = 4; //Por defecto suponemos que hay alpha (R G B a)
        int rowBeginning, offset;
        byte[] leftPixel, upperLeftPixel;

        switch(colourType){
            case 2: //TrueColor
                channel = 3;
            case 6: //TrueColor with alpha
                for(int i = 0; i < height; i++){
                    rowBeginning = i * (width*channel + 1);
                    filter = idat[rowBeginning];
                    switch(filter){
                        case 0: //None
                            for (int j = 0; j < width; j++) {
                                offset = j * channel + rowBeginning;
                                res[i][j][0] = idat[offset + 1];
                                res[i][j][1] = idat[offset + 2];
                                res[i][j][2] = idat[offset + 3];
                            }
                            break;
                        case 1: //Sub
                            leftPixel = new byte[3]; // Inicial 0 0 0
                            for (int j = 0; j < width; j++) {
                                offset = j * channel + rowBeginning;
                                res[i][j][0] = (byte) (idat[offset + 1] + leftPixel[0]);
                                res[i][j][1] = (byte) (idat[offset + 2] + leftPixel[1]);
                                res[i][j][2] = (byte) (idat[offset + 3] + leftPixel[2]);
                                leftPixel[0] = res[i][j][0];
                                leftPixel[1] = res[i][j][1];
                                leftPixel[2] = res[i][j][2];
                            }
                            break;
                        case 2: //Up
                            for (int j = 0; j < width; j++) {
                                offset = j * channel + rowBeginning;
                                res[i][j][0] = (byte) (idat[offset + 1] + res[i - 1][j][0]);
                                res[i][j][1] = (byte) (idat[offset + 2] + res[i - 1][j][1]);
                                res[i][j][2] = (byte) (idat[offset + 3] + res[i - 1][j][2]);
                            }
                            break;
                        case 3: //Average
                            leftPixel = new byte[3];
                            for (int j = 0; j < width; j++) {
                                offset = j * channel + rowBeginning;
                                res[i][j][0] = (byte) (idat[offset + 1] + ((res[i - 1][j][0] + leftPixel[0]) >>> 1));
                                res[i][j][1] = (byte) (idat[offset + 2] + ((res[i - 1][j][1] + leftPixel[1]) >>> 1));
                                res[i][j][2] = (byte) (idat[offset + 3] + ((res[i - 1][j][2] + leftPixel[2]) >>> 1));
                                leftPixel[0] = res[i][j][0];
                                leftPixel[1] = res[i][j][1];
                                leftPixel[2] = res[i][j][2];
                            }
                            break;
                        case 4: //Paeth
                            leftPixel = new byte[3];
                            upperLeftPixel = new byte[3];
                            for (int j = 0; j < width;) {
                                offset = j * channel + rowBeginning;
                                res[i][j][0] = (byte) (idat[offset + 1] + paethPredictor(leftPixel[0], res[i - 1][j][0], upperLeftPixel[0]));
                                res[i][j][1] = (byte) (idat[offset + 2] + paethPredictor(leftPixel[1], res[i - 1][j][1], upperLeftPixel[1]));
                                res[i][j][2] = (byte) (idat[offset + 3] + paethPredictor(leftPixel[2], res[i - 1][j][2], upperLeftPixel[2]));
                                leftPixel[0] = res[i][j][0];
                                leftPixel[1] = res[i][j][1];
                                leftPixel[2] = res[i][j][2];
                                j++;
                                upperLeftPixel[0] = res[i - 1][j - 1][0];
                                upperLeftPixel[1] = res[i - 1][j - 1][1];
                                upperLeftPixel[2] = res[i - 1][j - 1][2];
                            }
                            break;
                        default: //no se puede llegar aqui xd
                            return null;
                    }
                }
                return res;
            default:
                System.err.println("Colour type " + colourType + " not supported");
                return null;
        }
    }

    public static void saveRgbPng(String imgPath, byte[][][] pixels){
        try{
            FileOutputStream fos = new FileOutputStream(new File(imgPath));
            CRC32 crc32 = new CRC32();

            //Guardamos el header de PNG
            fos.write(PNG_HEADER);

            int width = pixels[0].length;
            int height = pixels.length;

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write(IHDR);
            baos.write(byteArrayFromUInt32(width));
            baos.write(byteArrayFromUInt32(height));
            baos.write(new byte[]{8, 2, 0, 0, 0});
            byte[] ihdr = baos.toByteArray();
            crc32.update(ihdr);
            fos.write(byteArrayFromUInt32(ihdr.length - 4));
            fos.write(ihdr);
            fos.write(byteArrayFromUInt32(crc32.getValue()));

            baos.reset();
            int rowBeginning, offset;
            byte[] idat = new byte[width*height*3 + height];
            for(int i = 0; i < height; i++){
                rowBeginning = i * (width * 3 + 1);
                for (int j = 0; j < width; j++) {
                    offset = j * 3 + rowBeginning;
                    idat[offset + 1] = pixels[i][j][0];
                    idat[offset + 2] = pixels[i][j][1];
                    idat[offset + 3] = pixels[i][j][2];
                }
            }
            baos.write(IDAT);
            idat = deflate(idat);
            baos.write(idat);
            idat = baos.toByteArray();
            crc32.update(idat);
            fos.write(byteArrayFromUInt32(idat.length - 4));
            fos.write(idat);
            fos.write(byteArrayFromUInt32(crc32.getValue()));

            fos.write(IEND);
           
            baos.close();
            fos.close();
        }
        catch(FileNotFoundException e){
            e.printStackTrace();
        }
        catch(IOException e){
            e.printStackTrace();
        }
    }

    private static byte[] getChunk(String imgPath, byte[] chunk) {
        if (chunk.length != 4) {
            return null;
        }
        boolean isIdat = cmpArray(chunk, IDAT);
        boolean found = false;
        File f = new File(imgPath);
        try {
            FileInputStream fis = new FileInputStream(f);
            int fSize = (int) f.length() - 3;
            byte[] readBuffer = new byte[4];
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] chunkBuffer;
            int chunkSize;
            for (int i = 12; i < fSize; i++) {
                fis.getChannel().position(i);
                fis.read(readBuffer);
                if (cmpArray(readBuffer, chunk)) {
                    fis.getChannel().position(i - 4);
                    fis.read(readBuffer);
                    chunkSize = (int) uInt32FromByteArray(readBuffer);
                    chunkBuffer = new byte[chunkSize];
                    fis.getChannel().position(i + 4);
                    fis.read(chunkBuffer);
                    baos.write(chunkBuffer);
                    found = true;
                    if (!isIdat) {
                        break;
                    }
                    i += chunkSize + 8;
                }
            }
            baos.close();
            fis.close();
            if(!found){
                return null;
            }
            return baos.toByteArray();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.exit(-1);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static boolean cmpArray(byte[] a1, byte[] a2){
        if(a1.length != a2.length){
            return false;
        }
        for(int i = 0; i < a1.length; i++){
            if(a1[i] != a2[i]){
                return false;
            }
        }
        return true;
    }

    private static long uInt32FromByteArray(byte[] a){
        return ((a[0] & 0xFFL) << 24) | ((a[1] & 0xFFL) << 16) | ((a[2] & 0xFFL) << 8) | a[3] & 0xFFL;
    }

    private static byte[] byteArrayFromUInt32(long n){
        return new byte[]{ (byte) ((n >>> 24) & 0xFF), (byte) ((n >>> 16) & 0xFF), (byte) ((n >>> 8) & 0xFF) ,(byte) (n&0xFF) };
    }

    private static byte[] deflate(byte[] input) {
        try {
            Deflater deflater = new Deflater();
            deflater.setInput(input);

            ByteArrayOutputStream baos = new ByteArrayOutputStream(input.length);
            deflater.finish();
            byte[] buffer = new byte[128];
            int len;
            while (!deflater.finished()) {
                len = deflater.deflate(buffer);
                baos.write(buffer, 0, len);
            }
            baos.close();
            return baos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        } 
        return null;
    }

    private static byte[] inflate(byte[] input) {
        try{
            Inflater inflater = new Inflater();
            inflater.setInput(input);

            ByteArrayOutputStream baos = new ByteArrayOutputStream(input.length);
            byte[] buffer = new byte[128];
            int len;
            while(!inflater.finished()){
                len = inflater.inflate(buffer);
                baos.write(buffer, 0, len);
            }
            baos.close();
            return baos.toByteArray();
        }
        catch(IOException e){
            e.printStackTrace();
        }
        catch(DataFormatException e){
            e.printStackTrace();
        }
        return null;
    }

    private static byte paethPredictor(byte a, byte b, byte c){
        byte p = (byte) (a + b - c);

        byte pa = myAbsByte((byte) (p - a));
        byte pb = myAbsByte((byte) (p - b));
        byte pc = myAbsByte((byte) (p - c));

        if(pa <= pb && pa <= pc){
            return a;
        }
        else if(pb <= pc){
            return b;
        }
        else{
            return c;
        }
    }

    private static byte myAbsByte(byte b){
        return b >= 0 ? (byte) b : (byte) -b;
    }
}