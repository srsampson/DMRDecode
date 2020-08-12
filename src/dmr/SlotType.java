package dmr;

public class SlotType {

    private int dataType;
    private String line;
    private boolean passErrorCheck;
    private DMRDecode theApp;

    // Decode a SLOT TYPE field given a int array of dibit values
    public String decode(DMRDecode ttheApp, byte[] dibit_buf) {
        theApp = ttheApp;
        passErrorCheck = mainDecode(dibit_buf);
        return line;
    }

    // The main decode and display method
    private boolean mainDecode(byte[] dibit_buf) {
        int a, r, colourCode;
        boolean dataSLOT[] = new boolean[20];
        StringBuilder sb = new StringBuilder(250);
        // Convert from dibits into boolean
        // DATA SLOT is broken into 2 parts either side of the sync burst
        // these need reuniting into a single 20 bit boolean array
        r = 0;
        for (a = 61; a < 66; a++) {
            if (dibit_buf[a] == 0) {
                dataSLOT[r] = false;
                dataSLOT[r + 1] = false;
            } else if (dibit_buf[a] == 1) {
                dataSLOT[r] = false;
                dataSLOT[r + 1] = true;
            } else if (dibit_buf[a] == 2) {
                dataSLOT[r] = true;
                dataSLOT[r + 1] = false;
            } else if (dibit_buf[a] == 3) {
                dataSLOT[r] = true;
                dataSLOT[r + 1] = true;
            }
            r = r + 2;
        }
        for (a = 90; a < 95; a++) {
            if (dibit_buf[a] == 0) {
                dataSLOT[r] = false;
                dataSLOT[r + 1] = false;
            } else if (dibit_buf[a] == 1) {
                dataSLOT[r] = false;
                dataSLOT[r + 1] = true;
            } else if (dibit_buf[a] == 2) {
                dataSLOT[r] = true;
                dataSLOT[r + 1] = false;
            } else if (dibit_buf[a] == 3) {
                dataSLOT[r] = true;
                dataSLOT[r + 1] = true;
            }
            r = r + 2;
        }
        // Error check SLOT TYPE
        if (checkGolay208(dataSLOT) == false) {
            return false;
        }
        // Colour code
        if (dataSLOT[0] == true) {
            colourCode = 8;
        } else {
            colourCode = 0;
        }
        if (dataSLOT[1] == true) {
            colourCode = colourCode + 4;
        }
        if (dataSLOT[2] == true) {
            colourCode = colourCode + 2;
        }
        if (dataSLOT[3] == true) {
            colourCode++;
        }
        // If this has changed then set the main colourCode variable
        if (theApp != null) {
            theApp.setColourCode(colourCode);
        }
        // Data Type
        if (dataSLOT[4] == true) {
            dataType = 8;
        } else {
            dataType = 0;
        }
        if (dataSLOT[5] == true) {
            dataType = dataType + 4;
        }
        if (dataSLOT[6] == true) {
            dataType = dataType + 2;
        }
        if (dataSLOT[7] == true) {
            dataType++;
        }
        // Display this info
        sb.append("Slot Type : Color Code " + Integer.toString(colourCode));
        if (dataType == 0) {
            sb.append(" PI Header");
        } else if (dataType == 1) {
            sb.append(" Voice LC Header");
        } else if (dataType == 2) {
            sb.append(" Terminator with LC");
        } else if (dataType == 3) {
            sb.append(" CSBK");
        } else if (dataType == 4) {
            sb.append(" MBC Header");
        } else if (dataType == 5) {
            sb.append(" MBC Continuation");
        } else if (dataType == 6) {
            sb.append(" Data Header");
        } else if (dataType == 7) {
            sb.append(" Rate 1/2 Data Continuation");
        } else if (dataType == 8) {
            sb.append(" Rate 3/4 Data Continuation");
        } else if (dataType == 9) {
            sb.append(" Idle");
        } else {
            sb.append(" Reserved for future use");
        }
        // Convert from StringBuilder to a String
        line = sb.toString();
        return true;
    }

    // Code to calculate all valid values for Golay (20,8)
    boolean calcGolay208() {
        boolean d[] = new boolean[8];
        boolean p[] = new boolean[12];
        int value[] = new int[256];
        int a;
        // Run through all possible 8 bit values
        for (a = 0; a < 256; a++) {
            // Convert to binary
            if ((a & 128) > 0) {
                d[0] = true;
            } else {
                d[0] = false;
            }
            if ((a & 64) > 0) {
                d[1] = true;
            } else {
                d[1] = false;
            }
            if ((a & 32) > 0) {
                d[2] = true;
            } else {
                d[2] = false;
            }
            if ((a & 16) > 0) {
                d[3] = true;
            } else {
                d[3] = false;
            }
            if ((a & 8) > 0) {
                d[4] = true;
            } else {
                d[4] = false;
            }
            if ((a & 4) > 0) {
                d[5] = true;
            } else {
                d[5] = false;
            }
            if ((a & 2) > 0) {
                d[6] = true;
            } else {
                d[6] = false;
            }
            if ((a & 1) > 0) {
                d[7] = true;
            } else {
                d[7] = false;
            }
            // Shift the value 12 times to the left
            value[a] = a << 12;
            // Calculate the parity bits
            p[0] = d[1] ^ d[4] ^ d[5] ^ d[6] ^ d[7];
            p[1] = d[1] ^ d[2] ^ d[4];
            p[2] = d[0] ^ d[2] ^ d[3] ^ d[5];
            p[3] = d[0] ^ d[1] ^ d[3] ^ d[4] ^ d[6];
            p[4] = d[0] ^ d[1] ^ d[2] ^ d[4] ^ d[5] ^ d[7];
            p[5] = d[0] ^ d[2] ^ d[3] ^ d[4] ^ d[7];
            p[6] = d[3] ^ d[6] ^ d[7];
            p[7] = d[0] ^ d[1] ^ d[5] ^ d[6];
            p[8] = d[0] ^ d[1] ^ d[2] ^ d[6] ^ d[7];
            p[9] = d[2] ^ d[3] ^ d[4] ^ d[5] ^ d[6];
            p[10] = d[0] ^ d[3] ^ d[4] ^ d[5] ^ d[6] ^ d[7];
            p[11] = d[1] ^ d[2] ^ d[3] ^ d[5] ^ d[7];
            // Add these to the lower bits of the valid words
            if (p[0] == true) {
                value[a] = value[a] + 2048;
            }
            if (p[1] == true) {
                value[a] = value[a] + 1024;
            }
            if (p[2] == true) {
                value[a] = value[a] + 512;
            }
            if (p[3] == true) {
                value[a] = value[a] + 256;
            }
            if (p[4] == true) {
                value[a] = value[a] + 128;
            }
            if (p[5] == true) {
                value[a] = value[a] + 64;
            }
            if (p[6] == true) {
                value[a] = value[a] + 32;
            }
            if (p[7] == true) {
                value[a] = value[a] + 16;
            }
            if (p[8] == true) {
                value[a] = value[a] + 8;
            }
            if (p[9] == true) {
                value[a] = value[a] + 4;
            }
            if (p[10] == true) {
                value[a] = value[a] + 2;
            }
            if (p[11] == true) {
                value[a] = value[a] + 1;
            }
        }
        // Just something to break on !
        return true;
    }

    // Check if a 20 bit boolean array has the collect Golay (20,8) coding
    private boolean checkGolay208(boolean[] word) {
        int a, golayValue;
        // A complete list of valid slot type words
        // This was generated by the calcGolay208 () method
        final int[] GolayNums = {0, 6379, 10558, 12757, 19095, 21116, 25513, 31554, 36294, 38189,
            42232, 48147, 51025, 57274, 61039, 63108, 66407, 72588, 76377, 78514, 84464,
            86299, 90318, 96293, 102049, 104010, 108447, 114548, 115766, 122077, 126216,
            128483, 132813, 138790, 143347, 145176, 150618, 152753, 157028, 163215, 166667,
            168928, 172597, 178910, 180636, 186743, 190626, 192585, 198058, 204097, 208020,
            210047, 216893, 219094, 222723, 229096, 231532, 233607, 237906, 244153, 246523,
            252432, 256965, 258862, 265625, 267634, 271527, 277580, 280334, 286693, 290352,
            292571, 295007, 301236, 305505, 307594, 314056, 315939, 320502, 326429, 331518,
            333333, 337856, 343851, 345193, 351362, 355671, 357820, 361272, 367571, 371206,
            373485, 379311, 381252, 385169, 391290, 396116, 398271, 402026, 408193, 410051,
            416040, 420093, 421910, 427666, 433785, 438188, 440135, 445445, 447726, 451899,
            458192, 460851, 463064, 467213, 473574, 475812, 481871, 486298, 488305, 493045,
            498974, 502987, 504864, 511842, 513929, 517724, 523959, 525274, 531249, 535268,
            537103, 543053, 545190, 548979, 555160, 560668, 562935, 567074, 573385, 574603,
            580704, 585141, 587102, 590013, 596054, 600451, 602472, 608810, 611009, 615188,
            621567, 626043, 628112, 631877, 638126, 641004, 646919, 650962, 652857, 656663,
            663036, 666665, 668866, 675712, 677739, 681662, 687701, 690385, 692282, 696815,
            702724, 705094, 711341, 715640, 717715, 722544, 728731, 733006, 735141, 740583,
            742412, 746969, 752946, 756662, 758621, 762504, 768611, 770337, 776650, 780319,
            782580, 790083, 792232, 796541, 802710, 804052, 810047, 814570, 816385, 820101,
            826222, 830139, 832080, 837906, 840185, 843820, 850119, 855332, 857551, 861210,
            867569, 870323, 876376, 880269, 882278, 884962, 890889, 895452, 897335, 903797,
            905886, 910155, 916384, 919694, 921701, 926128, 932187, 934425, 940786, 944935,
            947148, 951624, 957859, 961654, 963741, 970719, 972596, 976609, 982538, 986089,
            987906, 991959, 997948, 999806, 1005973, 1009728, 1011883, 1017391, 1023684,
            1027857, 1030138, 1035448, 1037395, 1041798, 1047917};
        // Convert the boolean array into an integer
        if (word[19] == true) {
            golayValue = 1;
        } else {
            golayValue = 0;
        }
        if (word[18] == true) {
            golayValue = golayValue + 2;
        }
        if (word[17] == true) {
            golayValue = golayValue + 4;
        }
        if (word[16] == true) {
            golayValue = golayValue + 8;
        }
        if (word[15] == true) {
            golayValue = golayValue + 16;
        }
        if (word[14] == true) {
            golayValue = golayValue + 32;
        }
        if (word[13] == true) {
            golayValue = golayValue + 64;
        }
        if (word[12] == true) {
            golayValue = golayValue + 128;
        }
        if (word[11] == true) {
            golayValue = golayValue + 256;
        }
        if (word[10] == true) {
            golayValue = golayValue + 512;
        }
        if (word[9] == true) {
            golayValue = golayValue + 1024;
        }
        if (word[8] == true) {
            golayValue = golayValue + 2048;
        }
        if (word[7] == true) {
            golayValue = golayValue + 4096;
        }
        if (word[6] == true) {
            golayValue = golayValue + 8192;
        }
        if (word[5] == true) {
            golayValue = golayValue + 16384;
        }
        if (word[4] == true) {
            golayValue = golayValue + 32768;
        }
        if (word[3] == true) {
            golayValue = golayValue + 65536;
        }
        if (word[2] == true) {
            golayValue = golayValue + 131072;
        }
        if (word[1] == true) {
            golayValue = golayValue + 262144;
        }
        if (word[0] == true) {
            golayValue = golayValue + 524288;
        }
        // Run through the possible values and we have a match return true
        for (a = 0; a < 256; a++) {
            if (golayValue == GolayNums[a]) {
                return true;
            }
        }
        // No matches so we must have a problem and so should return false
        return false;
    }

    // Let the main program know if there is an error in the frame
    public boolean isPassErrorCheck() {
        return passErrorCheck;
    }

    // Return the data type
    public int returnDataType() {
        return dataType;
    }
}