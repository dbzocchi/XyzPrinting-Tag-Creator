/**
 ReelTool 2, A tool to reset or create filament reel NFC tags
 Copyright (C) 2018 madman907

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published
 by the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.

 contains nfckey-library by Jack Fagner,
 see https://github.com/jackfagner/NfcKey
 */
package org.madman907.reeltool;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareUltralight;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ImporterTopLevel;

public class MainActivity extends Activity implements View.OnClickListener{

    private static final DateFormat TIME_FORMAT = SimpleDateFormat.getDateTimeInstance();
    private LinearLayout mTagContent;

    private NfcAdapter mAdapter;
    private PendingIntent mPendingIntent;

    private AlertDialog mDialog;

    private TextView tv = null;
    private TextView uid = null;
    private Button resetReelBt = null;
    private Button newReelBt = null;

    private LinearLayout dataLayout = null;
    private RadioGroup rgTemp = null;
    private TextView tmpTitle = null;
    private TextView FLTitle = null;
    private RadioButton radio190 = null;
    private RadioButton radio200 = null;
    private RadioButton radio210 = null;

    private TextView tagtype = null;
    private RadioGroup rgLength = null;
    private RadioButton radio200m = null;
    private RadioButton radio300m = null;
    private RadioButton radio100m = null;

    private Context c = null;
    private ImporterTopLevel scope = null;

    private MifareUltralight currentTag=null;

    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        //Taken from https://github.com/jackfagner/NfcKey with slight modifcations to make the code run Rhino....
        String strCode = "importPackage(org.mozilla.javascript.typedarrays); \nvar NfcKey = function () { };\n" +
                "\n" +
                "NfcKey.prototype = (function () {\n" +
                "    var intConst = [\n" +
                "        0x6D835AFC, 0x7D15CD97, 0x0942B409, 0x32F9C923, 0xA811FB02, 0x64F121E8,\n" +
                "        0xD1CC8B4E, 0xE8873E6F, 0x61399BBB, 0xF1B91926, 0xAC661520, 0xA21A31C9,\n" +
                "        0xD424808D, 0xFE118E07, 0xD18E728D, 0xABAC9E17, 0x18066433, 0x00E18E79,\n" +
                "        0x65A77305, 0x5AE9E297, 0x11FC628C, 0x7BB3431F, 0x942A8308, 0xB2F8FD20,\n" +
                "        0x5728B869, 0x30726D5A\n" +
                "    ];\n" +
                "\n" +
                "    var transform = function (rotUid) {\n" +
                "        var intPos = 0, tmp1, tmp2;\n" +
                "        var v1 = ((((rotUid[3] << 24) >>> 0) | ((rotUid[2] << 16) >>> 0) | ((rotUid[1] << 8) >>> 0) | rotUid[0]) >>> 0) + intConst[intPos++];\n" +
                "        var v2 = ((((rotUid[7] << 24) >>> 0) | ((rotUid[6] << 16) >>> 0) | ((rotUid[5] << 8) >>> 0) | rotUid[4]) >>> 0) + intConst[intPos++];\n" +
                "\n" +
                "        for (var i = 0; i < 12; i += 2) {\n" +
                "            tmp1 = rotateLeft((v1 ^ v2) >>> 0, v2 & 0x1F) + intConst[intPos++];\n" +
                "            tmp2 = rotateLeft((v2 ^ tmp1) >>> 0, tmp1 & 0x1F) + intConst[intPos++];\n" +
                "            v1 = rotateLeft((tmp1 ^ tmp2) >>> 0, tmp2 & 0x1F) + intConst[intPos++];\n" +
                "            v2 = rotateLeft((tmp2 ^ v1) >>> 0, v1 & 0x1F) + intConst[intPos++];\n" +
                "        }\n" +
                "        \n" +
                "        var r = new NativeUint8Array(8);\n" +
                "        r[0] = v1 & 0xFF;\n" +
                "        r[1] = (v1 >>> 8) & 0xFF;\n" +
                "        r[2] = (v1 >>> 16) & 0xFF;\n" +
                "        r[3] = (v1 >> 24) & 0xFF;\n" +
                "        r[4] = v2 & 0xFF;\n" +
                "        r[5] = (v2 >>> 8) & 0xFF;\n" +
                "        r[6] = (v2 >>> 16) & 0xFF;\n" +
                "        r[7] = (v2 >>> 24) & 0xFF;\n" +
                "        return r;\n" +
                "    };\n" +
                "\n" +
                "    var rotateLeft = function (x, n) {\n" +
                "        return (((x << n) >>> 0) | (x >>> (32 - n))) >>> 0;\n" +
                "    };\n" +
                "\n" +
                "    var parseHexUid = function (hexUid) {\n" +
                "        var r = new NativeUint8Array(8);\n" +
                "        if (!isValidHexUid(hexUid))\n" +
                "            return r;\n" +
                "        for (var i = 0; i < hexUid.length / 2; i++)\n" +
                "            r[i] = parseInt(hexUid.substr(i * 2, 2), 16);\n" +
                "        return r;\n" +
                "    };\n" +
                "\n" +
                "    var isValidHexUid = function (hexUid) {\n" +
                "        return typeof hexUid === \"string\" && hexUid.length === 14;\n" +
                "    };\n" +
                "\n" +
                "    var swap16 = function (val) {\n" +
                "        return ((((val & 0xFF) << 8) >>> 0)\n" +
                "            | ((val >>> 8) & 0xFF)) >>> 0;\n" +
                "    };\n" +
                "\n" +
                "    var swap32 = function (val) {\n" +
                "        return ((((val & 0xFF) << 24) >>> 0)\n" +
                "            | (((val & 0xFF00) << 8) >>> 0)\n" +
                "            | ((val >>> 8) & 0xFF00)\n" +
                "            | ((val >>> 24) & 0xFF)) >>> 0;\n" +
                "    };\n" +
                "\n" +
                "    var tohex = function (b, l) {\n" +
                "        var h = b.toString(16).toUpperCase();\n" +
                "        while (l && h.length < l / 4)\n" +
                "            h = '0' + h;\n" +
                "        return h;\n" +
                "    };\n" +
                "\n" +
                "    return {\n" +
                "\n" +
                "        constructor: NfcKey,\n" +
                "\n" +
                "        getKey: function (uid) {\n" +
                "            if (!isValidHexUid(uid))\n" +
                "                return;\n" +
                "            var i;\n" +
                "            var uid8 = parseHexUid(uid);\n" +
                "            var rotUid = new NativeUint8Array(8);\n" +
                "            var rotation = ((uid8[1] + uid8[3] + uid8[5]) & 7) >>> 0;\n" +
                "            for (i = 0; i < 7; i++)\n" +
                "                rotUid[((i + rotation) & 7) >>> 0] = uid8[i];\n" +
                "\n" +
                "            var transfUid = transform(rotUid);\n" +
                "\n" +
                "            var intKey = 0;\n" +
                "            var offset = (transfUid[0] + transfUid[2] + transfUid[4] + transfUid[6]) & 3;\n" +
                "            for (i = 0; i < 4; i++)\n" +
                "                intKey = transfUid[i + offset] + ((intKey << 8) >>> 0);\n" +
                "\n" +
                "            return tohex(swap32(intKey), 32);\n" +
                "        },\n" +
                "\n" +
                "        getPack: function (uid) {\n" +
                "            if (!isValidHexUid(uid))\n" +
                "                return;\n" +
                "            var i;\n" +
                "            var uid8 = parseHexUid(uid);\n" +
                "            var rotUid = new NativeUint8Array(8);\n" +
                "            var rotation = ((uid8[2] + uid8[5]) & 7) >>> 0;\n" +
                "            for (i = 0; i < 7; i++)\n" +
                "                rotUid[((i + rotation) & 7) >>> 0] = uid8[i];\n" +
                "\n" +
                "            var transfUid = transform(rotUid);\n" +
                "\n" +
                "            var intPack = 0;\n" +
                "            for (i = 0; i < 8; i++)\n" +
                "                intPack += transfUid[i] * 13;\n" +
                "\n" +
                "            var res = ((intPack ^ 0x5555) >>> 0) & 0xFFFF;\n" +
                "            return tohex(swap16(res), 16);\n" +
                "        }\n" +
                "\n" +
                "    };\n" +
                "})();var nfckey=new NfcKey();";


        //Setup Rhino-Context and load code
        c =Context.enter();
        scope = new ImporterTopLevel(c);
        c.setOptimizationLevel(-1);
        c.evaluateString(scope,strCode,null,0,null);

        setContentView(R.layout.activity_main);
        mTagContent = findViewById(R.id.list);
        resolveIntent(getIntent());

        tv = findViewById(R.id.tag_viewer_text);
        uid = findViewById(R.id.UIDText);
        tagtype = findViewById(R.id.TypeText);

        resetReelBt = findViewById(R.id.reset_reel);
        newReelBt = findViewById(R.id.set_PWD);
        radio190= findViewById(R.id.radio190);
        radio200= findViewById(R.id.radio200);
        radio210= findViewById(R.id.radio210);

        rgTemp = findViewById(R.id.rgTemp);
        rgTemp.setVisibility(View.INVISIBLE);

        dataLayout = findViewById(R.id.dataLayout);
        dataLayout.setVisibility(View.INVISIBLE);

        tmpTitle = findViewById(R.id.tmpTitle);
        tmpTitle.setVisibility(View.INVISIBLE);

        tmpTitle = findViewById(R.id.tmpTitle);
        tmpTitle.setVisibility(View.INVISIBLE);

        FLTitle = findViewById(R.id.FLTitle);
        FLTitle.setVisibility(View.INVISIBLE);

        rgLength = findViewById(R.id.rgLength);
        rgLength.setVisibility(View.INVISIBLE);

        radio100m = findViewById(R.id.radio100m);

        radio200m = findViewById(R.id.radio200m);

        radio300m = findViewById(R.id.radio300m);

        resetReelBt.setVisibility(View.INVISIBLE);
        newReelBt.setVisibility(View.INVISIBLE);

        tv.setText("Scan a Tag to begin..");

        ((Button)findViewById(R.id.set_PWD)).setOnClickListener(this);
        ((Button)findViewById(R.id.reset_reel)).setOnClickListener(this);


        mDialog = new AlertDialog.Builder(this).setNeutralButton("Ok", null).create();

        mAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mAdapter == null) {
            showMessage(R.string.error, R.string.no_nfc);
            finish();
            return;
        }

        mPendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
    }

    private void showMessage(int title, int message) {
        mDialog.setTitle(title);
        mDialog.setMessage(getText(message));
        mDialog.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mAdapter != null) {
            if (!mAdapter.isEnabled()) {
                showWirelessSettingsDialog();
            }
            mAdapter.enableForegroundDispatch(this, mPendingIntent, null, null);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mAdapter != null) {
            mAdapter.disableForegroundDispatch(this);
        }
    }

    private void showWirelessSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.nfc_disabled);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
                Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
                startActivity(intent);
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
                finish();
            }
        });
        builder.create().show();
        return;
    }

    private void resolveIntent(Intent intent) {
        String action = intent.getAction();
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {

            // Unknown tag type
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

            MifareUltralight mful = MifareUltralight.get(tag);

            currentTag = mful;

            try {
                mful.connect();

                resetReelBt.setVisibility(View.INVISIBLE);
                newReelBt.setVisibility(View.INVISIBLE);
                rgTemp.setVisibility(View.INVISIBLE);
                dataLayout.setVisibility((View.INVISIBLE));
                tmpTitle.setVisibility(View.INVISIBLE);
                FLTitle.setVisibility(View.INVISIBLE);
                rgLength.setVisibility(View.INVISIBLE);
                uid.setText(toReversedHex(tag.getId()));


                try {
                    sendPasswordAuth(mful);
                    resetReelBt.setVisibility(View.VISIBLE);
                }
                catch(Exception e){
                    tv.setVisibility(View.INVISIBLE);
                    dataLayout.setVisibility(View.VISIBLE);
                    tagtype.setText("New Tag (Blank)");
                    newReelBt.setVisibility(View.VISIBLE);
                    rgTemp.setVisibility(View.VISIBLE);
                    tmpTitle.setVisibility(View.VISIBLE);
                   FLTitle.setVisibility(View.VISIBLE);
                    radio210.setChecked(true);

                    rgLength.setVisibility(View.VISIBLE);
                    radio200m.setChecked(true);

                    mful.close();
                    return;
                }

                byte[] page10 = mful.readPages(10);
                String strOrigLen = firstPageHex(page10);
                int nReelSize = (int) (toDec(hexStringToByteArray(firstPageHex(page10)))/1000);

                byte[] page20 = mful.readPages(20);
                mful.close();
                String strCurrentLen = firstPageHex(page20);
                int nReelRemain = (int) (toDec(hexStringToByteArray(firstPageHex(page20)))/1000);

                tv.setText(tv.getText()+"\n"+"Filament remaining: "+nReelRemain+"/"+nReelSize+"m");

                System.out.println(nReelSize);
                mful.close();

            }catch (Exception e){
                newReelBt.setEnabled(false);
                resetReelBt.setEnabled(false);
                e.printStackTrace();
            }
        }
    }

    private String findPassword(byte[] uid){

        String strHexKey= toReversedHex(uid).replaceAll(" ","");

        c.evaluateString(scope,"var erg = nfckey.getKey(\""+strHexKey+"\")",null,0,null);
        Object o2 =scope.get("erg");
        return o2.toString();
    }

    private String findPack(byte[] uid){

        String strHexKey= toReversedHex(uid).replaceAll(" ","");

        c.evaluateString(scope,"var erg = nfckey.getPack(\""+strHexKey+"\")",null,0,null);
        Object o2 =scope.get("erg");
        return o2.toString();
    }


    private void sendPasswordAuth(MifareUltralight mful) throws Exception
    {
        String pwd = findPassword(mful.getTag().getId());

        byte[] pwdcmd = hexStringToByteArray("1B"+pwd);

        byte[] ans = mful.transceive(pwdcmd);
        //Pack auswerten!

    }

    private void resetReel()
    {

        if(null==currentTag)
            return;

        MifareUltralight mful = currentTag;


        try {
            mful.connect();


            sendPasswordAuth(mful);

            byte[] page10 = mful.readPages(10);
            String strOrigLen = firstPageHex(page10);
            System.out.println(strOrigLen);
            byte[] newval = hexStringToByteArray(strOrigLen);
            mful.writePage(20,newval);

            int nReelSize = (int) (toDec(hexStringToByteArray(firstPageHex(page10)))/1000);

            tv.setText("Tag found, ID: "+toReversedHex(mful.getTag().getId()));
            tv.setText(tv.getText()+"\n"+"Remaining Filament reset to "+nReelSize+"m!");

            mful.close();
        }
        catch(Exception e){
            e.printStackTrace();
        }

    }

    private void createCard(){

        try {
            MifareUltralight mifare = currentTag;
            mifare.connect();
            while (!mifare.isConnected()) {
                Thread.sleep(100);
            };
            byte[] response;

            String strPass = findPassword(mifare.getTag().getId());
            String strPack = findPack(mifare.getTag().getId());
            byte[] pwd = hexStringToByteArray(strPass);
            byte[] pack = hexStringToByteArray(strPack);

            byte[] page10 = mifare.readPages(10);
            String strOrigLen = firstPageHex(page10);

            /*
            // Authenticate with the tag first
            // In case it's already been locked
            try {
                response = mifare.transceive(new byte[]{
                        (byte) 0x1B, // PWD_AUTH
                        pwd[0], pwd[1], pwd[2], pwd[3]
                });
                // Check if PACK is matching expected PACK
                // This is a (not that) secure method to check if tag is genuine
                if ((response != null) && (response.length >= 2)) {
                    byte[] packResponse = Arrays.copyOf(response, 2);
                    if (!(pack[0] == packResponse[0] && pack[1] == packResponse[1])) {
                        tv.setText(tv.getText()+"\n"+"Tag could not be authenticated:\n" + packResponse.toString() + "≠" + pack.toString());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            */


            mifare.writePage(0x04,hexStringToByteArray("0103A00C"));
            mifare.writePage(0x05,hexStringToByteArray("340300FE"));
            mifare.writePage(0x06,hexStringToByteArray("00000000"));
            mifare.writePage(0x07,hexStringToByteArray("00000000"));

            if(radio190.isChecked()) {
                //190deg
                mifare.writePage(0x08, hexStringToByteArray("5A504000"));
            } else
            if(radio200.isChecked()) {
                //200deg
                mifare.writePage(0x08, hexStringToByteArray("5A504500"));
            }
            else {
                //210deg
                mifare.writePage(0x08, hexStringToByteArray("5A505000"));
            }

            mifare.writePage(0x09,hexStringToByteArray("00353454"));

            if(radio200m.isChecked()) {
                mifare.writePage(0x0A, hexStringToByteArray("400D0300"));    //200m
                mifare.writePage(0x0B, hexStringToByteArray("400D0300"));   //200m
            }
            else if(radio300m.isChecked())
            {
                mifare.writePage(0x0A, hexStringToByteArray("E0930400"));    //300m
                mifare.writePage(0x0B, hexStringToByteArray("E0930400"));   //300m
            }
            else {

                mifare.writePage(0x0A, hexStringToByteArray("A0860100"));    //100m
                mifare.writePage(0x0B, hexStringToByteArray("A0860100"));   //100m

            }

            mifare.writePage(0x0C,hexStringToByteArray("D2002D00"));
            mifare.writePage(0x0D,hexStringToByteArray("54484742"));
            mifare.writePage(0x0E,hexStringToByteArray("30333338"));
            mifare.writePage(0x0F,hexStringToByteArray("00000000"));
            mifare.writePage(0x10,hexStringToByteArray("00000000"));
            mifare.writePage(0x11,hexStringToByteArray("34000000"));
            mifare.writePage(0x12,hexStringToByteArray("00000000"));
            mifare.writePage(0x13,hexStringToByteArray("00000000"));

            if(radio200m.isChecked()) {
                mifare.writePage(0x14,hexStringToByteArray("400D0300"));   //200m
                mifare.writePage(0x15,hexStringToByteArray("081F3154"));   //checksum
                mifare.writePage(0x16,hexStringToByteArray("50B1E0CE"));   //checksum
                mifare.writePage(0x17,hexStringToByteArray("52E74F76"));   //checksum
            }
            else
            {
                mifare.writePage(0x14,hexStringToByteArray("E0930400"));   //300m
                mifare.writePage(0x15,hexStringToByteArray("A8813654"));   //checksum
                mifare.writePage(0x16,hexStringToByteArray("F03FEECE"));   //checksum
                mifare.writePage(0x17,hexStringToByteArray("F26E4D76"));   //checksum
            }

            //... bis 27h sollte alles 0 sein, ab 28h config-Bytes


            // Get Page 2Ah
            response = mifare.transceive(new byte[]{
                    (byte) 0x30, // READ
                    (byte) 0x2A  // page address
            });


            // configure tag as write-protected with unlimited authentication tries
            if ((response != null) && (response.length >= 16)) {    // read always returns 4 pages
                boolean prot = true;                               // false = PWD_AUTH for write only, true = PWD_AUTH for read and write
                int authlim = 0;                                    // 0 = unlimited tries
                mifare.transceive(new byte[]{
                        (byte) 0xA2, // WRITE
                        (byte) 0x2A, // page address
                        (byte) ((response[0] & 0x078) | (prot ? 0x080 : 0x000) | (authlim & 0x007)),    // set ACCESS byte according to our settings
                        0, 0, 0                                                                         // fill rest as zeros as stated in datasheet (RFUI must be set as 0b)
                });
            }

            // Get page 29h
            response = mifare.transceive(new byte[]{
                    (byte) 0x30, // READ
                    (byte) 0x29  // page address
            });


            // Configure tag to protect storage starting at  Page 8
            if ((response != null) && (response.length >= 16)) {  // read always returns 4 pages
                int auth0 = 8;                                    // first page to be protected
                mifare.transceive(new byte[]{
                        (byte) 0xA2, // WRITE
                        (byte) 0x29, // page address
                        response[0], 0, response[2],              // Keep old mirror values and write 0 in RFUI byte as stated in datasheet
                        (byte) (auth0 & 0x0ff)
                });
            }


            // Send PACK and PWD
            // set PACK:
            mifare.transceive(new byte[]{
                    (byte) 0xA2,
                    (byte) 0x2C,
                    pack[0], pack[1], 0, 0  // Write PACK into first 2 Bytes and 0 in RFUI bytes
            });
            // set PWD:
            mifare.transceive(new byte[]{
                    (byte) 0xA2,
                    (byte) 0x2B,
                    pwd[0], pwd[1], pwd[2], pwd[3] // Write all 4 PWD bytes into Page 43
            });

            mifare.close();

            tv.setText(tv.getText()+"\n"+"Successfully created Tag !");
        }
        catch(Exception e){
            e.printStackTrace();
            e.getMessage();
            new AlertDialog.Builder(this).setMessage("Failed:"+e.getMessage()).setNeutralButton("Ok", null).create().show();

        }
    }

    private String firstPageHex(byte[] dat)
    {
        String page = toReversedHex(dat).substring(0,11);
        return page.replaceAll(" ","");
    }

    private String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = bytes.length - 1; i >= 0; --i) {
            int b = bytes[i] & 0xff;
            if (b < 0x10)
                sb.append('0');
            sb.append(Integer.toHexString(b));
            if (i > 0) {
                sb.append(" ");
            }
        }
        return sb.toString();
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    private String toReversedHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; ++i) {
            if (i > 0) {
                sb.append(" ");
            }
            int b = bytes[i] & 0xff;
            if (b < 0x10)
                sb.append('0');
            sb.append(Integer.toHexString(b));
        }
        return sb.toString();
    }

    private long toDec(byte[] bytes) {
        long result = 0;
        long factor = 1;
        for (int i = 0; i < bytes.length; ++i) {
            long value = bytes[i] & 0xffl;
            result += value * factor;
            factor *= 256l;
        }
        return result;
    }

    // Implement the OnClickListener callback
    public void onClick(View v) {
        // do something when the button is clicked

        switch (v.getId()) {
            case R.id.reset_reel:
                resetReel();
                return;

            case R.id.set_PWD:
                createCard();
                return;
        }

    }
    @Override
    public void onNewIntent(Intent intent) {
        setIntent(intent);
        resolveIntent(intent);
    }
}