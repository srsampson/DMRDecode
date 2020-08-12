package dmr;

public class ShortLC {

    private boolean dataReady;
    private String line;
    private boolean rawData[] = new boolean[69];
    private boolean crcResult = false;
    private int currentState = -1;
    private DMRDecode TtheApp;

    // Add data to the Short LC data buffer
    // Type 0 if First fragment of LC
    // Type 1 if Continuation fragment of LC
    // Type 2 if Last fragment of LC
    public void addData(boolean[] CACHbuf, int type) {
        int a, b = 7;
        int rawCounter = 0;
        dataReady = false;
        // First fragment ?
        // If so reset the the counters
        switch (type) {
            // Continuation fragments
            case 0:
                rawCounter = 0;
                // Ensure nothing else has arrived before
                if (currentState != -1) {
                    return;
                }   // Set the current state to 0 to indicate a first fragment has arrived
                currentState = 0;
                // Clear the display line
                line = "";
                break;
            // Last fragment
            case 1:
                switch (currentState) {
                    case 0:
                        rawCounter = 17;
                        currentState = 1;
                        break;
                    case 1:
                        rawCounter = 34;
                        currentState = 2;
                        break;
                    case -1:
                        rawCounter = 0;
                        return;
                    default:
                        break;
                }
                break;
            case 2:
                // Ensure that a first fragment and two continuation fragments have arrived
                if (currentState != 2) {
                    currentState = -1;
                    rawCounter = 0;
                    return;
                } else {
                    rawCounter = 51;
                }
                break;
            default:
                break;
        }
        // Add the data
        for (a = rawCounter; a < (rawCounter + 17); a++) {
            // Ignore the TACT
            rawData[a] = CACHbuf[b];
            b++;
        }
        // Has the fragment ended ?
        if (type == 2) {
            decode();
            currentState = -1;
            rawCounter = 0;
        }
    }

    // Make the text string available
    public String getLine() {
        return line;
    }

    // Tell the main object if decoded data is available
    public boolean isDataReady() {
        return dataReady;
    }

    // Tell the main object if the CRC and Hamming checks are OK
    public boolean isCRCgood() {
        return crcResult;
    }

    // Clear the data ready boolean
    public void clrDataReady() {
        dataReady = false;
    }

    // Deinterleave and error check the short LC
    public void decode() {
        crcResult = false;
        if (shortLCHamming(rawData) == true) {
            boolean shortLC[] = deInterleaveShortLC(rawData);
            if (shortLCcrc(shortLC) == true) {
                line = decodeShortLC(shortLC);
                crcResult = true;
            }
        } else {
            line = "";
        }
        dataReady = true;
    }

    // Deinterleave a Short LC from 4 CACH bursts
    private boolean[] deInterleaveShortLC(boolean raw[]) {
        int a, pos;
        final int sequence[] = {
            0, 4, 8, 12, 16, 20, 24, 28, 32, 36, 40, 44,
            1, 5, 9, 13, 17, 21, 25, 29, 33, 37, 41, 45,
            2, 6, 10, 14, 18, 22, 26, 30, 34, 38, 42, 46};
        boolean[] deinter = new boolean[36];
        for (a = 0; a < 36; a++) {
            pos = sequence[a];
            deinter[a] = raw[pos];
        }
        return deinter;
    }

    // Hamming check 3 of the 4 CACH rows
    private boolean shortLCHamming(boolean raw[]) {
        int a, pos;
        final int sequence1[] = {0, 4, 8, 12, 16, 20, 24, 28, 32, 36, 40, 44};
        final int sequence2[] = {1, 5, 9, 13, 17, 21, 25, 29, 33, 37, 41, 45};
        final int sequence3[] = {2, 6, 10, 14, 18, 22, 26, 30, 34, 38, 42, 46};
        final int ham1[] = {48, 52, 56, 60, 64};
        final int ham2[] = {49, 53, 57, 61, 65};
        final int ham3[] = {50, 54, 58, 62, 66};
        boolean[] d = new boolean[12];
        boolean[] p = new boolean[5];
        boolean[] c = new boolean[5];
        // Row 1
        for (a = 0; a < 12; a++) {
            pos = sequence1[a];
            d[a] = raw[pos];
            if (a < 5) {
                pos = ham1[a];
                p[a] = raw[pos];
            }
        }
        c[0] = d[0] ^ d[1] ^ d[2] ^ d[3] ^ d[6] ^ d[7] ^ d[9];
        c[1] = d[0] ^ d[1] ^ d[2] ^ d[3] ^ d[4] ^ d[7] ^ d[8] ^ d[10];
        c[2] = d[1] ^ d[2] ^ d[3] ^ d[4] ^ d[5] ^ d[8] ^ d[9] ^ d[11];
        c[3] = d[0] ^ d[1] ^ d[4] ^ d[5] ^ d[7] ^ d[10];
        c[4] = d[0] ^ d[1] ^ d[2] ^ d[5] ^ d[6] ^ d[8] ^ d[11];
        for (a = 0; a < 5; a++) {
            if (c[a] != p[a]) {
                return false;
            }
        }
        // Row 2
        for (a = 0; a < 12; a++) {
            pos = sequence2[a];
            d[a] = raw[pos];
            if (a < 5) {
                pos = ham2[a];
                p[a] = raw[pos];
            }
        }
        c[0] = d[0] ^ d[1] ^ d[2] ^ d[3] ^ d[6] ^ d[7] ^ d[9];
        c[1] = d[0] ^ d[1] ^ d[2] ^ d[3] ^ d[4] ^ d[7] ^ d[8] ^ d[10];
        c[2] = d[1] ^ d[2] ^ d[3] ^ d[4] ^ d[5] ^ d[8] ^ d[9] ^ d[11];
        c[3] = d[0] ^ d[1] ^ d[4] ^ d[5] ^ d[7] ^ d[10];
        c[4] = d[0] ^ d[1] ^ d[2] ^ d[5] ^ d[6] ^ d[8] ^ d[11];
        for (a = 0; a < 5; a++) {
            if (c[a] != p[a]) {
                return false;
            }
        }
        // Row 3
        for (a = 0; a < 12; a++) {
            pos = sequence3[a];
            d[a] = raw[pos];
            if (a < 5) {
                pos = ham3[a];
                p[a] = raw[pos];
            }
        }
        c[0] = d[0] ^ d[1] ^ d[2] ^ d[3] ^ d[6] ^ d[7] ^ d[9];
        c[1] = d[0] ^ d[1] ^ d[2] ^ d[3] ^ d[4] ^ d[7] ^ d[8] ^ d[10];
        c[2] = d[1] ^ d[2] ^ d[3] ^ d[4] ^ d[5] ^ d[8] ^ d[9] ^ d[11];
        c[3] = d[0] ^ d[1] ^ d[4] ^ d[5] ^ d[7] ^ d[10];
        c[4] = d[0] ^ d[1] ^ d[2] ^ d[5] ^ d[6] ^ d[8] ^ d[11];
        for (a = 0; a < 5; a++) {
            if (c[a] != p[a]) {
                return false;
            }
        }
        // All done so must have passed
        return true;
    }

    // Test if the short LC passes its CRC8 test
    private boolean shortLCcrc(boolean dataBits[]) {
        int a;
        crc tCRC = new crc();
        tCRC.setCrc8Value(0);
        for (a = 0; a < dataBits.length; a++) {
            tCRC.crc8(dataBits[a]);
        }
        return tCRC.getCrc8Value() == 0;
    }

    // Decode and display the info in SHORT LC PDUs
    private String decodeShortLC(boolean db[]) {
        int slco, a;
        StringBuilder dline = new StringBuilder(250);
        // Calculate the SLCO
        if (db[0] == true) {
            slco = 8;
        } else {
            slco = 0;
        }
        if (db[1] == true) {
            slco = slco + 4;
        }
        if (db[2] == true) {
            slco = slco + 2;
        }
        if (db[3] == true) {
            slco++;
        }
        // Short LC Types
        switch (slco) {
            case 0:
                dline.append("Nul_Msg");
                break;
            // Tier III SYS_Parms
            case 1:
                int addr1,
                 addr2,
                 inf;
                dline.append("Act_Updt - ");
                // Slot 1
                if (db[4] == true) {
                    inf = 8;
                } else {
                    inf = 0;
                }
                if (db[5] == true) {
                    inf = inf + 4;
                }
                if (db[6] == true) {
                    inf = inf + 2;
                }
                if (db[7] == true) {
                    inf++;
                }
                dline.append(decodeAct_Updt(inf, 1));
                // Hashed Address
                if (inf != 0) {
                    if (db[12] == true) {
                        addr1 = 128;
                    } else {
                        addr1 = 0;
                    }
                    if (db[13] == true) {
                        addr1 = addr1 + 64;
                    }
                    if (db[14] == true) {
                        addr1 = addr1 + 32;
                    }
                    if (db[15] == true) {
                        addr1 = addr1 + 16;
                    }
                    if (db[16] == true) {
                        addr1 = addr1 + 8;
                    }
                    if (db[17] == true) {
                        addr1 = addr1 + 4;
                    }
                    if (db[18] == true) {
                        addr1 = addr1 + 2;
                    }
                    if (db[19] == true) {
                        addr1++;
                    }
                    dline.append(" Hashed Addr ").append(Integer.toString(addr1));
                }
                dline.append(" : ");
                // Slot 2
                if (db[8] == true) {
                    inf = 8;
                } else {
                    inf = 0;
                }
                if (db[9] == true) {
                    inf = inf + 4;
                }
                if (db[10] == true) {
                    inf = inf + 2;
                }
                if (db[11] == true) {
                    inf++;
                }
                dline.append(decodeAct_Updt(inf, 2));
                if (inf != 0) {
                    // Hashed Address
                    if (db[20] == true) {
                        addr2 = 128;
                    } else {
                        addr2 = 0;
                    }
                    if (db[21] == true) {
                        addr2 = addr2 + 64;
                    }
                    if (db[22] == true) {
                        addr2 = addr2 + 32;
                    }
                    if (db[23] == true) {
                        addr2 = addr2 + 16;
                    }
                    if (db[24] == true) {
                        addr2 = addr2 + 8;
                    }
                    if (db[25] == true) {
                        addr2 = addr2 + 4;
                    }
                    if (db[26] == true) {
                        addr2 = addr2 + 2;
                    }
                    if (db[27] == true) {
                        addr2++;
                    }
                    dline.append(" Hashed Addr ").append(Integer.toString(addr2));
                }
                break;
            // Connect Plus SLCO 9
            case 2:
                int model,
                 net = 0,
                 site = 0,
                 par;
                // Start to make up the display
                dline.append("SYS_Parms : ");
                // Model
                if (db[4] == true) {
                    model = 2;
                } else {
                    model = 0;
                }
                if (db[5] == true) {
                    model++;
                }   // Par
                if (db[18] == true) {
                    par = 2;
                } else {
                    par = 0;
                }
                if (db[19] == true) {
                    par++;
                }   // Tiny
                switch (model) {
                    // Small
                    case 0:
                        dline.append("Tiny Network");
                        // Net
                        if (db[6] == true) {
                            net = 256;
                        }
                        if (db[7] == true) {
                            net = net + 128;
                        }
                        if (db[8] == true) {
                            net = net + 64;
                        }
                        if (db[9] == true) {
                            net = net + 32;
                        }
                        if (db[10] == true) {
                            net = net + 16;
                        }
                        if (db[11] == true) {
                            net = net + 8;
                        }
                        if (db[12] == true) {
                            net = net + 4;
                        }
                        if (db[13] == true) {
                            net = net + 2;
                        }
                        if (db[14] == true) {
                            net++;
                        }
                        // Site
                        if (db[15] == true) {
                            site = 4;
                        }
                        if (db[16] == true) {
                            site = site + 2;
                        }
                        if (db[17] == true) {
                            site++;
                        }
                        break;
                    // Large
                    case 1:
                        dline.append("Small Network");
                        // Net
                        if (db[6] == true) {
                            net = 64;
                        }
                        if (db[7] == true) {
                            net = net + 32;
                        }
                        if (db[8] == true) {
                            net = net + 16;
                        }
                        if (db[9] == true) {
                            net = net + 8;
                        }
                        if (db[10] == true) {
                            net = net + 4;
                        }
                        if (db[11] == true) {
                            net = net + 2;
                        }
                        if (db[12] == true) {
                            net++;
                        }
                        // Site
                        if (db[13] == true) {
                            site = 16;
                        }
                        if (db[14] == true) {
                            site = site + 8;
                        }
                        if (db[15] == true) {
                            site = site + 4;
                        }
                        if (db[16] == true) {
                            site = site + 2;
                        }
                        if (db[17] == true) {
                            site++;
                        }
                        break;
                    // Huge
                    case 2:
                        dline.append("Large Network");
                        // Net
                        if (db[6] == true) {
                            net = 8;
                        }
                        if (db[7] == true) {
                            net = net + 4;
                        }
                        if (db[8] == true) {
                            net = net + 2;
                        }
                        if (db[9] == true) {
                            net++;
                        }
                        // Site
                        if (db[10] == true) {
                            site = 128;
                        }
                        if (db[11] == true) {
                            site = site + 64;
                        }
                        if (db[12] == true) {
                            site = site + 32;
                        }
                        if (db[13] == true) {
                            site = site + 16;
                        }
                        if (db[14] == true) {
                            site = site + 8;
                        }
                        if (db[15] == true) {
                            site = site + 4;
                        }
                        if (db[16] == true) {
                            site = site + 2;
                        }
                        if (db[17] == true) {
                            site++;
                        }
                        break;
                    case 3:
                        dline.append("Huge Network");
                        // Net
                        if (db[6] == true) {
                            net = 2;
                        }
                        if (db[7] == true) {
                            net++;
                        }
                        // Site
                        if (db[8] == true) {
                            site = 512;
                        }
                        if (db[9] == true) {
                            site = site + 256;
                        }
                        if (db[10] == true) {
                            site = site + 128;
                        }
                        if (db[11] == true) {
                            site = site + 64;
                        }
                        if (db[12] == true) {
                            site = site + 32;
                        }
                        if (db[13] == true) {
                            site = site + 16;
                        }
                        if (db[14] == true) {
                            site = site + 8;
                        }
                        if (db[15] == true) {
                            site = site + 4;
                        }
                        if (db[16] == true) {
                            site = site + 2;
                        }
                        if (db[17] == true) {
                            site++;
                        }
                        break;
                    default:
                        break;
                }
                dline.append(" NET=").append(Integer.toString(net)).append(" SITE=").append(Integer.toString(site));
                // PAR
                switch (par) {
                    case 1:
                        dline.append(" Category A MSs only permitted");
                        break;
                    case 2:
                        dline.append(" Category B MSs only permitted");
                        break;
                    // Reg
                    case 3:
                        dline.append(" Category A MSs and B MSs permitted");
                        break;
                    default:
                        break;
                }
                boolean reg = db[20];
                int counter = 0;
                // Counter
                if (db[21] == true) {
                    counter = 256;
                }
                if (db[22] == true) {
                    counter = counter + 128;
                }
                if (db[23] == true) {
                    counter = counter + 64;
                }
                if (db[24] == true) {
                    counter = counter + 32;
                }
                if (db[25] == true) {
                    counter = counter + 16;
                }
                if (db[26] == true) {
                    counter = counter + 8;
                }
                if (db[27] == true) {
                    counter = counter + 4;
                }
                if (db[28] == true) {
                    counter = counter + 2;
                }
                if (db[29] == true) {
                    counter++;
                }
                dline.append(" Common_Slot_Counter=").append(Integer.toString(counter));
                if (reg == true) {
                    dline.append(" Reg=1");
                } else {
                    dline.append(" Reg=0");
                }   // Display the network/site in the system label
                String ldip = Integer.toString(net) + "/" + Integer.toString(site);
                TtheApp.setSystemLabel(ldip);
                break;
            case 9: {
                int netID, siteID;
                dline.append("Connect Plus Voice Channel SLCO=").append(Integer.toString(slco)).append(" Network: ");
                // Network ID
                if (db[4] == true) {
                    netID = 2048;
                } else {
                    netID = 0;
                }
                if (db[5] == true) {
                    netID = netID + 1024;
                }
                if (db[6] == true) {
                    netID = netID + 512;
                }
                if (db[7] == true) {
                    netID = netID + 256;
                }
                if (db[8] == true) {
                    netID = netID + 128;
                }
                if (db[9] == true) {
                    netID = netID + 64;
                }
                if (db[10] == true) {
                    netID = netID + 32;
                }
                if (db[11] == true) {
                    netID = netID + 16;
                }
                if (db[12] == true) {
                    netID = netID + 8;
                }
                if (db[13] == true) {
                    netID = netID + 4;
                }
                if (db[14] == true) {
                    netID = netID + 2;
                }
                if (db[15] == true) {
                    netID++;
                }       // Bits 16,17,18,19,20,21,22,23 appear to be the site ID
                // Site ID
                if (db[16] == true) {
                    siteID = 128;
                } else {
                    siteID = 0;
                }
                if (db[17] == true) {
                    siteID = siteID + 64;
                }
                if (db[18] == true) {
                    siteID = siteID + 32;
                }
                if (db[19] == true) {
                    siteID = siteID + 16;
                }
                if (db[20] == true) {
                    siteID = siteID + 8;
                }
                if (db[21] == true) {
                    siteID = siteID + 4;
                }
                if (db[22] == true) {
                    siteID = siteID + 2;
                }
                if (db[23] == true) {
                    siteID++;
                }       // Bits 24,25,26,27 have an unknown purpose
                dline.append(netID);
                dline.append(" Site: ");
                dline.append(siteID);
                // Make up a status bar system label display
                if (TtheApp != null) {
                    TtheApp.setSystemLabel("System : Connect Plus (Network " + Integer.toString(netID) + ") (Site " + Integer.toString(siteID) + ")");
                }
                break;
            } // Connect Plus SLCO 10
            case 10: {
                int netID, siteID;
                dline.append("Connect Plus Control Channel SLCO=").append(Integer.toString(slco)).append(" Network: ");
                // Network ID
                if (db[4] == true) {
                    netID = 2048;
                } else {
                    netID = 0;
                }
                if (db[5] == true) {
                    netID = netID + 1024;
                }
                if (db[6] == true) {
                    netID = netID + 512;
                }
                if (db[7] == true) {
                    netID = netID + 256;
                }
                if (db[8] == true) {
                    netID = netID + 128;
                }
                if (db[9] == true) {
                    netID = netID + 64;
                }
                if (db[10] == true) {
                    netID = netID + 32;
                }
                if (db[11] == true) {
                    netID = netID + 16;
                }
                if (db[12] == true) {
                    netID = netID + 8;
                }
                if (db[13] == true) {
                    netID = netID + 4;
                }
                if (db[14] == true) {
                    netID = netID + 2;
                }
                if (db[15] == true) {
                    netID++;
                }       // Bits 16,17,18,19,20,21,22,23 appear to be the site ID
                // Site ID
                if (db[16] == true) {
                    siteID = 128;
                } else {
                    siteID = 0;
                }
                if (db[17] == true) {
                    siteID = siteID + 64;
                }
                if (db[18] == true) {
                    siteID = siteID + 32;
                }
                if (db[19] == true) {
                    siteID = siteID + 16;
                }
                if (db[20] == true) {
                    siteID = siteID + 8;
                }
                if (db[21] == true) {
                    siteID = siteID + 4;
                }
                if (db[22] == true) {
                    siteID = siteID + 2;
                }
                if (db[23] == true) {
                    siteID++;
                }       // Bits 24,25,26,27 have an unknown purpose
                dline.append(netID);
                dline.append(" Site: ");
                dline.append(siteID);
                // Make up a status bar system label display
                if (TtheApp != null) {
                    TtheApp.setSystemLabel("System : Connect Plus (Network " + Integer.toString(netID) + ") (Site " + Integer.toString(siteID) + ")");
                }
                break;
            } // Capacity Plus SLCO 15
            case 15:
                int lcn;
                if (db[16] == true) {
                    lcn = 8;
                } else {
                    lcn = 0;
                }
                if (db[17] == true) {
                    lcn = lcn + 4;
                }
                if (db[18] == true) {
                    lcn = lcn + 2;
                }
                if (db[19] == true) {
                    lcn++;
                }
                dline.append(" Capacity Plus Act_Updt - Rest Channel is LCN ").append(Integer.toString(lcn));
                break;
            default:
                dline.append("Unknown SLCO=").append(Integer.toString(slco)).append(" ");
                for (a = 4; a < 28; a++) {
                    if (db[a] == true) {
                        dline.append("1");
                    } else {
                        dline.append("0");
                    }
                }
                break;
        }
        return dline.toString();
    }

    // Decode a 4 bit section of an Act_Updt
    private String decodeAct_Updt(int inf, int channel) {
        String l;
        switch (inf) {
            case 0:
                l = "No activity on BS time slot " + Integer.toString(channel);
                if (channel == 1) {
                    TtheApp.setCh1Label("Unused", TtheApp.labelQuiteColour);
                } else if (channel == 2) {
                    TtheApp.setCh2Label("Unused", TtheApp.labelQuiteColour);
                }
                break;
            case 2:
                l = "Group CSBK activity on BS time slot " + Integer.toString(channel);
                break;
            case 3:
                l = "Individual CSBK activity on BS time slot " + Integer.toString(channel);
                break;
            case 8:
                l = "Group voice activity on BS time slot " + Integer.toString(channel);
                break;
            case 9:
                l = "Individual voice activity on BS time slot " + Integer.toString(channel);
                break;
            case 10:
                l = "Individual data activity on BS time slot " + Integer.toString(channel);
                break;
            case 11:
                l = "Group data activity on BS time slot " + Integer.toString(channel);
                break;
            case 12:
                l = "Emergency group activity on BS time slot " + Integer.toString(channel);
                break;
            case 13:
                l = "Emergency individual voice activity on BS time slot " + Integer.toString(channel);
                break;
            default:
                l = "Reserved";
                break;
        }
        return l;
    }

    public void setApp(DMRDecode theApp) {
        TtheApp = theApp;
    }
}
