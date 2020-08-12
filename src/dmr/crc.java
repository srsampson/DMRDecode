package dmr;

public class crc {

    private int crc8Value, crc16Value;

    public void setCrc8Value(int crc8Value) {
        this.crc8Value = crc8Value;
    }

    public int getCrc8Value() {
        return crc8Value;
    }

    // The CRC8 routine //
    public void crc8(boolean bit) {
        boolean shiftBit = (crc8Value & 0x01) > 0;
        
        crc8Value = crc8Value >> 1;
        
        if ((bit ^ shiftBit) == true) {
            crc8Value = crc8Value ^ 0xe0;
        }
    }

    // The CCITT CRC16 routine //
    private void ccitt_crc16(int in) {
        byte c = (byte) in;
        
        for (int i = 0; i < 8; i++) {
            boolean c15 = ((crc16Value >> 15 & 1) == 1);
            boolean bit = ((c >> (7 - i) & 1) == 1);
            crc16Value <<= 1;
            if (c15 ^ bit) {
                crc16Value ^= 0x1021;
            }
        }
        
        crc16Value = crc16Value & 0xffff;
    }

    // CSBK CRC check
    public boolean crcCSBK(boolean in[]) {
        crc16Value = 0;
        // Run through all 96 bits
        for (int a = 0; a < 96; a = a + 8) {
            int val = 0;
            for (int b = 0; b < 8; b++) {
                if (in[a + b] == true) {
                    val = val + (int) Math.pow(2.0, (7.0 - b));
                }
            }
            // Allow for the CSBK CRC mask
            if (a >= 80) {
                val = val ^ 0xA5;
            }
            ccitt_crc16(val);
        }
        return crc16Value == 0x1D0F;
    }

    // Data Header CRC check
    public boolean crcDataHeader(boolean in[]) {
        crc16Value = 0;
        // Run through all 96 bits
        for (int a = 0; a < 96; a = a + 8) {
            int val = 0;
            for (int b = 0; b < 8; b++) {
                if (in[a + b] == true) {
                    val = val + (int) Math.pow(2.0, (7.0 - b));
                }
            }
            // Allow for the Data Header CRC mask
            if (a >= 80) {
                val = val ^ 0xCC;
            }
            ccitt_crc16(val);
        }
        return crc16Value == 0x1D0F;
    }

    // Reed-Solomon (12,9) check
    // TODO : Get the Reed Solomon (12,9) check routine working
    public boolean RS129(boolean in[]) {
        int byteCount = 0;
        int inBytes[] = new int[12];
        // Convert from binary to an array of integers
        for (int a = 0; a < 96; a = a + 8) {
            inBytes[byteCount] = 0;
            for (int b = 0; b < 8; b++) {
                int d = (int) Math.pow(2.0, ((8 - b) - 1));
                if (in[a + b] == true) {
                    inBytes[byteCount] = inBytes[byteCount] + d;
                }
            }
            byteCount++;
        }

        return false;
    }

    // CRC 5 check
    public boolean crcFiveBit(boolean in[], int tcrc) {
        int b = 0, oct = 0, total = 0;
        // Convert the boolean array into an array of ints
        for (int a = 0; a < 72; a++) {
            if (in[a] == true) {
                oct = oct + (int) Math.pow(2.0, (int) b);
            }
            b++;
            if (b == 8) {
                b = 0;
                total = total + oct;
                oct = 0;
            }
        }
        total = total % 31;
        return total == tcrc;
    }
}
