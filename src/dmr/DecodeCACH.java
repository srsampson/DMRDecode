package dmr;

public class DecodeCACH {

    private StringBuilder line = new StringBuilder(250);
    private String shortLCline;
    private boolean at;
    private boolean channel;
    private int lcss;
    private boolean passErrorCheck = false;
    private boolean haveShortLC = false;
    private boolean shortLCError = false;
    private int errorRes;
    private DMRDecode theApp;

    public String decode(DMRDecode TtheApp, byte[] dibit_buf) {
        theApp = TtheApp;
        line.append("CACH : TACT ");
        // CACH decode
        passErrorCheck = mainDecode(dibit_buf);
        // Only pass the decoded data back if the user wants to display CACH info
        if (theApp.isDisplayCACH() == true) {
            return line.toString();
        } else {
            return null;
        }
    }

    // De-interleave , CRC check and decode the CACH
    // With code added to work out which interleave sequence to use
    private boolean mainDecode(byte[] dibit_buf) {
        int a, r = 0, fragType = -1;
        boolean rawdataCACH[] = new boolean[24];
        boolean dataCACH[] = new boolean[24];
        final int[] interleaveCACH = {0, 4, 8, 12, 14, 18, 22, 1, 2, 3, 5, 6, 7, 9, 10, 11, 13, 15, 16, 17, 19, 20, 21, 23};
        // Convert from dibits into boolean
        for (a = 0; a < 12; a++) {
            switch (dibit_buf[a]) {
                case 0:
                    rawdataCACH[r] = false;
                    rawdataCACH[r + 1] = false;
                    break;
                case 1:
                    rawdataCACH[r] = false;
                    rawdataCACH[r + 1] = true;
                    break;
                case 2:
                    rawdataCACH[r] = true;
                    rawdataCACH[r + 1] = false;
                    break;
                case 3:
                    rawdataCACH[r] = true;
                    rawdataCACH[r + 1] = true;
                    break;
                default:
                    break;
            }
            r = r + 2;
        }
        // De-interleave
        for (a = 0; a < 24; a++) {
            r = interleaveCACH[a];
            dataCACH[a] = rawdataCACH[r];
        }
        //  Convert the first 7 bits (TACT + 3 parity bits) into an integer
        if (dataCACH[0] == true) {
            errorRes = 64;
        } else {
            errorRes = 0;
        }
        if (dataCACH[1] == true) {
            errorRes = errorRes + 32;
        }
        if (dataCACH[2] == true) {
            errorRes = errorRes + 16;
        }
        if (dataCACH[3] == true) {
            errorRes = errorRes + 8;
        }
        if (dataCACH[4] == true) {
            errorRes = errorRes + 4;
        }
        if (dataCACH[5] == true) {
            errorRes = errorRes + 2;
        }
        if (dataCACH[6] == true) {
            errorRes++;
        }
        if (errorCheckHamming743(errorRes) == false) {
            return false;
        }
        // Decode the TACT
        at = dataCACH[0];
        channel = dataCACH[1];
        if (dataCACH[2] == true) {
            lcss = 2;
        } else {
            lcss = 0;
        }
        if (dataCACH[3] == true) {
            lcss++;
        }
        // Display TACT info
        if (at == true) {
            line.append(" AT=1");
        }
        if (channel == false) {
            line.append(" Ch 1");
            theApp.currentChannel = 1;
        } else {
            line.append(" Ch 2");
            theApp.currentChannel = 2;
        }
        switch (lcss) {
            case 0:
                line.append(" First fragment of CSBK ");
                break;
            case 1:
                line.append(" First fragment of LC ");
                break;
            case 2:
                line.append(" Last fragment of LC ");
                break;
            case 3:
                line.append(" Continuation fragment of LC ");
                break;
            default:
                break;
        }
        // If this is an short LC message pass the data on to the ShortLC object
        switch (lcss) {
            case 3:
                fragType = 1;
                break;
            case 2:
                fragType = 2;
                break;
            case 1:
                fragType = 0;
                break;
            default:
                break;
        }
        // Below is commented out as the code contains a known bug
        // Also other things need fixing first
        if (fragType != -1) {
            theApp.short_lc.addData(dataCACH, fragType);
        }
        // Is short LC data ready ?
        if (theApp.short_lc.isDataReady() == true) {
            // See if the short LC passed its error checks
            if (theApp.short_lc.isCRCgood() == true) {
                shortLCError = false;
                shortLCline = theApp.getTimeStamp() + " Short LC : " + theApp.short_lc.getLine();
            } else {
                shortLCError = true;
                shortLCline = theApp.getTimeStamp() + " Bad Short LC !";
            }
            theApp.short_lc.clrDataReady();
            haveShortLC = true;
        } else {
            haveShortLC = false;
        }
        return true;
    }

    // Error check the CACH TACT
    public boolean errorCheckHamming743(int tact) {
        // An array of valid Hamming words which was generated by calcHamming()
        final int[] Hamming743 = {0, 11, 22, 29, 39, 44, 49, 58, 69, 78, 83, 88, 98, 105, 116, 127};
        int a;
        for (a = 0; a < 16; a++) {
            // If we have a match return true
            if (tact == Hamming743[a]) {
                return true;
            }
        }
        // If no match there is a problem so return a false
        return false;
    }

    // Generate a list of valid Hamming words
    // Isn't normally called but leave in for now
    public boolean calcHamming() {
        boolean d1, d2, d3, d4, h2, h1, h0;
        int a;
        int valid[] = new int[16];
        // Run through all possible values
        for (a = 0; a < 16; a++) {
            // Covert from an integer to boolean
            d1 = (a & 8) > 0;
            d2 = (a & 4) > 0;
            d3 = (a & 2) > 0;
            d4 = (a & 1) > 0;
            // Calculate the parity bits
            h2 = d1 ^ d2 ^ d3;
            h1 = d2 ^ d3 ^ d4;
            h0 = d1 ^ d2 ^ d4;
            // Convert this back into a 7 bit integer
            if (d1 == true) {
                valid[a] = 64;
            } else {
                valid[a] = 0;
            }
            if (d2 == true) {
                valid[a] = valid[a] + 32;
            }
            if (d3 == true) {
                valid[a] = valid[a] + 16;
            }
            if (d4 == true) {
                valid[a] = valid[a] + 8;
            }
            if (h2 == true) {
                valid[a] = valid[a] + 4;
            }
            if (h1 == true) {
                valid[a] = valid[a] + 2;
            }
            if (h0 == true) {
                valid[a] = valid[a] + 1;
            }
        }
        // Just something to break on
        return true;
    }

    // Let the main program know if there is an error in the frame
    public boolean isPassErrorCheck() {
        return passErrorCheck;
    }

    // Let the main program know the hamming word in case of an error
    public int getErrorRes() {
        return errorRes;
    }

    // Tell the user we have a Short LC
    public boolean getShortLC() {
        return haveShortLC;
    }

    // Clear the Short LC variables
    public void clearShortLC() {
        haveShortLC = false;
    }

    // Return the decoded short LC
    public String getShortLCline() {
        return shortLCline;
    }

    public boolean getshortLCError() {
        return shortLCError;
    }
}
